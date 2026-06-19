package br.com.adaneinstein.wheresmymoney.tui.component;

import br.com.adaneinstein.wheresmymoney.util.CurrencyUtil;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.WindowListenerAdapter;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/** Diálogo simples para cálculos aritméticos e retorno formatado para o campo monetário. */
public final class CalculatorDialog {

    private static final MathContext DIVISION_CONTEXT = new MathContext(16, RoundingMode.HALF_UP);

    private CalculatorDialog() {
    }

    public static void installShortcut(Window window, WindowBasedTextGUI gui, TextBox... targets) {
        window.addWindowListener(new WindowListenerAdapter() {
            @Override
            public void onUnhandledInput(Window basePane, KeyStroke key, AtomicBoolean handled) {
                if (key.getKeyType() != KeyType.F4) {
                    return;
                }
                TextBox target = findFocusedTarget(targets);
                if (target == null) {
                    return;
                }
                handled.set(true);
                String value = show(gui, "Calculadora", target.getText());
                if (value != null) {
                    target.setText(value);
                    target.setCaretPosition(0, value.length());
                }
            }
        });
    }

    public static String show(WindowBasedTextGUI gui, String title, String initialValue) {
        BasicWindow dialog = new BasicWindow((title == null || title.isBlank()) ? "Calculadora" : title);
        dialog.setHints(Set.of(Window.Hint.CENTERED));

        TextBox expressionBox = new TextBox(new TerminalSize(32, 1));
        Label help = new Label("Digite a expressão e pressione Enter para aplicar. Esc cancela.");
        help.setForegroundColor(TextColor.ANSI.BLACK_BRIGHT);

        String initialExpression = normalizeInitialExpression(initialValue);
        if (!initialExpression.isEmpty()) {
            expressionBox.setText(initialExpression);
        }

        final String[] selectedValue = {null};
        Runnable apply = () -> {
            try {
                BigDecimal value = evaluateExpression(expressionBox.getText());
                selectedValue[0] = CurrencyUtil.format(value);
                dialog.close();
            } catch (RuntimeException e) {
                MessageDialog.showMessageDialog(gui, "Erro", "Expressão inválida", MessageDialogButton.OK);
            }
        };

        Panel root = new Panel(new LinearLayout(Direction.VERTICAL));
        root.addComponent(new Label("Expressão"));
        root.addComponent(expressionBox);
        root.addComponent(help);

        dialog.setComponent(root);
        dialog.addWindowListener(new WindowListenerAdapter() {
            @Override
            public void onUnhandledInput(Window basePane, KeyStroke key, AtomicBoolean handled) {
                if (key.getKeyType() == KeyType.Enter) {
                    handled.set(true);
                    apply.run();
                }
            }
        });
        dialog.addWindowListener(EscClose.of(dialog));
        gui.addWindowAndWait(dialog);
        return selectedValue[0];
    }

    static BigDecimal evaluateExpression(String expression) {
        Parser parser = new Parser(expression);
        BigDecimal value = parser.parseExpression();
        parser.ensureEnd();
        return value;
    }

    private static String normalizeInitialExpression(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        try {
            return CurrencyUtil.parse(raw).stripTrailingZeros().toPlainString();
        } catch (RuntimeException e) {
            return "";
        }
    }

    private static BigDecimal parseNumberToken(String token) {
        String normalized = token.trim();
        if (normalized.contains(",")) {
            normalized = normalized.replace(".", "").replace(",", ".");
        }
        return new BigDecimal(normalized);
    }

    private static TextBox findFocusedTarget(TextBox... targets) {
        if (targets == null) {
            return null;
        }
        return Arrays.stream(targets)
                .filter(target -> target != null && target.isFocused())
                .findFirst()
                .orElse(null);
    }

    private static final class Parser {
        private final String source;
        private int index = 0;

        private Parser(String source) {
            this.source = source == null ? "" : source;
        }

        private BigDecimal parseExpression() {
            BigDecimal value = parseTerm();
            while (true) {
                skipSpaces();
                if (consume('+')) {
                    value = value.add(parseTerm());
                } else if (consume('-')) {
                    value = value.subtract(parseTerm());
                } else {
                    return value;
                }
            }
        }

        private BigDecimal parseTerm() {
            BigDecimal value = parseFactor();
            while (true) {
                skipSpaces();
                if (consume('*')) {
                    value = value.multiply(parseFactor());
                } else if (consume('/')) {
                    BigDecimal divisor = parseFactor();
                    if (BigDecimal.ZERO.compareTo(divisor) == 0) {
                        throw error("Divisão por zero");
                    }
                    value = value.divide(divisor, DIVISION_CONTEXT);
                } else {
                    return value;
                }
            }
        }

        private BigDecimal parseFactor() {
            skipSpaces();
            if (consume('+')) {
                return parseFactor();
            }
            if (consume('-')) {
                return parseFactor().negate();
            }
            if (consume('(')) {
                BigDecimal nested = parseExpression();
                skipSpaces();
                if (!consume(')')) {
                    throw error("')' esperado");
                }
                return nested;
            }
            return parseNumber();
        }

        private BigDecimal parseNumber() {
            skipSpaces();
            int start = index;
            boolean hasDigit = false;
            while (index < source.length()) {
                char c = source.charAt(index);
                if (Character.isDigit(c)) {
                    hasDigit = true;
                    index++;
                } else if (c == '.' || c == ',') {
                    index++;
                } else {
                    break;
                }
            }
            if (!hasDigit) {
                throw error("Número esperado");
            }

            String token = source.substring(start, index);
            try {
                return parseNumberToken(token);
            } catch (NumberFormatException e) {
                throw error("Número inválido");
            }
        }

        private void ensureEnd() {
            skipSpaces();
            if (index != source.length()) {
                throw error("Fim da expressão esperado");
            }
        }

        private void skipSpaces() {
            while (index < source.length() && Character.isWhitespace(source.charAt(index))) {
                index++;
            }
        }

        private boolean consume(char expected) {
            if (index < source.length() && source.charAt(index) == expected) {
                index++;
                return true;
            }
            return false;
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " na posição " + (index + 1));
        }
    }
}
