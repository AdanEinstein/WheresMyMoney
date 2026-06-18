package br.com.adaneinstein.wheresmymoney.tui.component;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Borders;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.WindowListenerAdapter;
import com.googlecode.lanterna.input.KeyStroke;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Diálogo de seleção de data: calendário visual em grade (7 colunas × 6 semanas),
 * com borda, dia selecionado destacado e navegação por setas. Retorna a data
 * escolhida ou {@code null} se cancelado.
 */
public final class DatePickerDialog {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final String[] WEEKDAYS = {"Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom"};

    // Paleta de cores das células.
    private static final TextColor BG = TextColor.ANSI.BLACK;
    private static final TextColor NORMAL_FG = TextColor.ANSI.WHITE;
    private static final TextColor OTHER_MONTH_FG = TextColor.ANSI.BLACK_BRIGHT;
    private static final TextColor TODAY_FG = TextColor.ANSI.CYAN_BRIGHT;
    private static final TextColor SELECTED_FG = TextColor.ANSI.BLACK;
    private static final TextColor SELECTED_BG = TextColor.ANSI.WHITE;

    private DatePickerDialog() {
    }

    public static LocalDate show(WindowBasedTextGUI gui, String title, LocalDate initialDate) {
        LocalDate initial = initialDate != null ? initialDate : LocalDate.now();
        LocalDate today = LocalDate.now();

        BasicWindow dialog = new BasicWindow(title);
        dialog.setHints(Set.of(Window.Hint.CENTERED));

        final LocalDate[] selected = {initial};
        final LocalDate[] monthView = {initial.withDayOfMonth(1)};
        final LocalDate[] pickedDate = {null};

        Label monthLabel = new Label("");
        monthLabel.setForegroundColor(TextColor.ANSI.CYAN_BRIGHT);

        // Grade: 7 colunas (cabeçalho dos dias + 6 semanas de células).
        Panel grid = new Panel(new GridLayout(7));
        for (String wd : WEEKDAYS) {
            Label header = new Label(String.format(" %-3s", wd));
            header.setForegroundColor(TextColor.ANSI.YELLOW);
            grid.addComponent(header);
        }
        Label[] cells = new Label[42];
        for (int i = 0; i < cells.length; i++) {
            cells[i] = new Label("    ");
            grid.addComponent(cells[i]);
        }

        final Runnable refresh = () -> {
            LocalDate firstOfMonth = monthView[0].withDayOfMonth(1);
            String monthName = firstOfMonth.getMonth().getDisplayName(TextStyle.FULL, PT_BR);
            monthLabel.setText(capitalize(monthName) + " " + firstOfMonth.getYear());

            int shift = firstOfMonth.getDayOfWeek().getValue() - 1; // segunda = 0
            LocalDate start = firstOfMonth.minusDays(shift);
            for (int i = 0; i < cells.length; i++) {
                LocalDate day = start.plusDays(i);
                Label cell = cells[i];
                cell.setText(String.format(" %2d ", day.getDayOfMonth()));
                if (day.equals(selected[0])) {
                    cell.setForegroundColor(SELECTED_FG);
                    cell.setBackgroundColor(SELECTED_BG);
                } else {
                    cell.setBackgroundColor(BG);
                    if (day.equals(today)) {
                        cell.setForegroundColor(TODAY_FG);
                    } else if (day.getMonthValue() != firstOfMonth.getMonthValue()) {
                        cell.setForegroundColor(OTHER_MONTH_FG);
                    } else {
                        cell.setForegroundColor(NORMAL_FG);
                    }
                }
            }
        };

        Button prevMonth = new Button("◀", () -> {
            selected[0] = selected[0].minusMonths(1);
            monthView[0] = monthView[0].minusMonths(1).withDayOfMonth(1);
            refresh.run();
        });
        Button nextMonth = new Button("▶", () -> {
            selected[0] = selected[0].plusMonths(1);
            monthView[0] = monthView[0].plusMonths(1).withDayOfMonth(1);
            refresh.run();
        });
        Button confirm = new Button("Selecionar", () -> {
            pickedDate[0] = selected[0];
            dialog.close();
        });
        Button cancel = new Button("Cancelar", dialog::close);

        Panel topBar = new Panel(new LinearLayout(Direction.HORIZONTAL));
        topBar.addComponent(prevMonth);
        topBar.addComponent(new EmptySpace());
        topBar.addComponent(monthLabel);
        topBar.addComponent(new EmptySpace());
        topBar.addComponent(nextMonth);

        Panel actions = new Panel(new LinearLayout(Direction.HORIZONTAL));
        actions.addComponent(confirm);
        actions.addComponent(cancel);

        Label help = new Label("Setas: mover · ◀ ▶: mês · T: hoje · Enter: ok · Esc: cancela");
        help.setForegroundColor(TextColor.ANSI.BLACK_BRIGHT);

        Panel content = new Panel(new LinearLayout(Direction.VERTICAL));
        content.addComponent(center(topBar));
        content.addComponent(new EmptySpace());
        content.addComponent(grid.withBorder(Borders.singleLine()));
        content.addComponent(new EmptySpace());
        content.addComponent(center(help));
        content.addComponent(new EmptySpace());
        content.addComponent(center(actions));

        dialog.addWindowListener(new WindowListenerAdapter() {
            @Override
            public void onUnhandledInput(Window basePane, KeyStroke key, AtomicBoolean handled) {
                LocalDate next = selected[0];
                switch (key.getKeyType()) {
                    case ArrowLeft -> next = selected[0].minusDays(1);
                    case ArrowRight -> next = selected[0].plusDays(1);
                    case ArrowUp -> next = selected[0].minusDays(7);
                    case ArrowDown -> next = selected[0].plusDays(7);
                    case PageUp -> next = selected[0].minusMonths(1);
                    case PageDown -> next = selected[0].plusMonths(1);
                    case Home -> next = selected[0].withDayOfMonth(1);
                    case End -> next = selected[0].withDayOfMonth(selected[0].lengthOfMonth());
                    case Enter -> {
                        handled.set(true);
                        pickedDate[0] = selected[0];
                        dialog.close();
                        return;
                    }
                    case Character -> {
                        Character ch = key.getCharacter();
                        if (ch == null) {
                            return;
                        }
                        switch (Character.toLowerCase(ch)) {
                            case 't' -> next = today;
                            case 'h' -> next = selected[0].minusDays(1);
                            case 'l' -> next = selected[0].plusDays(1);
                            case 'k' -> next = selected[0].minusDays(7);
                            case 'j' -> next = selected[0].plusDays(7);
                            default -> { return; }
                        }
                    }
                    default -> { return; }
                }

                handled.set(true);
                selected[0] = next;
                monthView[0] = next.withDayOfMonth(1);
                refresh.run();
            }
        });

        dialog.setComponent(content);
        dialog.addWindowListener(EscClose.of(dialog));
        refresh.run();
        gui.addWindowAndWait(dialog);
        return pickedDate[0];
    }

    private static String capitalize(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private static Panel center(com.googlecode.lanterna.gui2.Component content) {
        Panel row = new Panel(new LinearLayout(Direction.HORIZONTAL));
        row.addComponent(new EmptySpace(), Layouts.GROW);
        row.addComponent(content);
        row.addComponent(new EmptySpace(), Layouts.GROW);
        return row;
    }
}
