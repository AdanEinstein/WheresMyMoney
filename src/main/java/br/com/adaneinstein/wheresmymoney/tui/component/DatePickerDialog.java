package br.com.adaneinstein.wheresmymoney.tui.component;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowListenerAdapter;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DatePickerDialog {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final String[] WEEKDAYS = {"Seg", "Ter", "Qua", "Qui", "Sex", "Sab", "Dom"};

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
        Label helpLabel = new Label("H/J/K/L: mover  PgUp/PgDn: mês  Home/End: início/fim  T: hoje  Enter: confirmar");
        Label weekHeader = new Label(weekHeaderLine());
        Panel weeksPanel = new Panel(new LinearLayout(Direction.VERTICAL));
        Label[] weekLines = new Label[6];
        for (int i = 0; i < weekLines.length; i++) {
            weekLines[i] = new Label("");
            weeksPanel.addComponent(weekLines[i]);
        }
        final Runnable[] refresh = new Runnable[1];

        refresh[0] = () -> {
            LocalDate firstOfMonth = monthView[0].withDayOfMonth(1);
            String monthName = firstOfMonth.getMonth().getDisplayName(TextStyle.FULL, PT_BR);
            monthLabel.setText(capitalize(monthName) + " " + firstOfMonth.getYear());

            int shift = firstOfMonth.getDayOfWeek().getValue() - 1;
            LocalDate start = firstOfMonth.minusDays(shift);

            for (int week = 0; week < weekLines.length; week++) {
                LocalDate weekStart = start.plusDays((long) week * 7);
                weekLines[week].setText(weekLine(weekStart, selected[0], today, firstOfMonth.getMonthValue()));
            }
        };

        Button prevMonth = new Button("< Mês", () -> {
            selected[0] = selected[0].minusMonths(1);
            monthView[0] = monthView[0].minusMonths(1).withDayOfMonth(1);
            refresh[0].run();
        });
        Button nextMonth = new Button("Mês >", () -> {
            selected[0] = selected[0].plusMonths(1);
            monthView[0] = monthView[0].plusMonths(1).withDayOfMonth(1);
            refresh[0].run();
        });
        Button goToday = new Button("Hoje", () -> {
            selected[0] = today;
            monthView[0] = today.withDayOfMonth(1);
            refresh[0].run();
        });

        Button confirm = new Button("Selecionar", () -> {
            pickedDate[0] = selected[0];
            dialog.close();
        });
        Button cancel = new Button("Cancelar", dialog::close);

        Panel topBar = new Panel(new LinearLayout(Direction.HORIZONTAL));
        topBar.addComponent(prevMonth);
        topBar.addComponent(new EmptySpace(new TerminalSize(2, 1)));
        topBar.addComponent(monthLabel);
        topBar.addComponent(new EmptySpace(new TerminalSize(2, 1)));
        topBar.addComponent(nextMonth);
        topBar.addComponent(new EmptySpace(new TerminalSize(2, 1)));
        topBar.addComponent(goToday);

        Panel actions = new Panel(new LinearLayout(Direction.HORIZONTAL));
        actions.addComponent(confirm);
        actions.addComponent(cancel);

        Panel calendar = new Panel(new LinearLayout(Direction.VERTICAL));
        calendar.addComponent(centerHorizontally(topBar));
        calendar.addComponent(new EmptySpace(new TerminalSize(1, 1)));
        calendar.addComponent(centerHorizontally(weekHeader));
        calendar.addComponent(centerHorizontally(weeksPanel));
        calendar.addComponent(new EmptySpace(new TerminalSize(1, 1)));
        calendar.addComponent(centerHorizontally(helpLabel));
        calendar.addComponent(new EmptySpace(new TerminalSize(1, 1)));
        calendar.addComponent(centerHorizontally(actions));

        Panel centered = new Panel(new LinearLayout(Direction.HORIZONTAL));
        centered.addComponent(new EmptySpace(), Layouts.GROW);
        centered.addComponent(calendar);
        centered.addComponent(new EmptySpace(), Layouts.GROW);

        Panel root = new Panel(new LinearLayout(Direction.VERTICAL));
        root.addComponent(centered, Layouts.GROW);

        dialog.addWindowListener(new WindowListenerAdapter() {
            @Override
            public void onUnhandledInput(Window basePane, KeyStroke key, AtomicBoolean handled) {
                LocalDate next = selected[0];
                if (key.getKeyType() == KeyType.PageUp) {
                    next = selected[0].minusMonths(1);
                } else if (key.getKeyType() == KeyType.PageDown) {
                    next = selected[0].plusMonths(1);
                } else if (key.getKeyType() == KeyType.Home) {
                    next = selected[0].withDayOfMonth(1);
                } else if (key.getKeyType() == KeyType.End) {
                    next = selected[0].withDayOfMonth(selected[0].lengthOfMonth());
                } else if (key.getKeyType() == KeyType.Enter) {
                    handled.set(true);
                    pickedDate[0] = selected[0];
                    dialog.close();
                    return;
                } else if (key.getKeyType() == KeyType.Character && key.getCharacter() != null
                        && Character.toLowerCase(key.getCharacter()) == 't') {
                    next = today;
                } else if (key.getKeyType() == KeyType.Character && key.getCharacter() != null
                        && Character.toLowerCase(key.getCharacter()) == 'h') {
                    next = selected[0].minusDays(1);
                } else if (key.getKeyType() == KeyType.Character && key.getCharacter() != null
                        && Character.toLowerCase(key.getCharacter()) == 'l') {
                    next = selected[0].plusDays(1);
                } else if (key.getKeyType() == KeyType.Character && key.getCharacter() != null
                        && Character.toLowerCase(key.getCharacter()) == 'k') {
                    next = selected[0].minusDays(7);
                } else if (key.getKeyType() == KeyType.Character && key.getCharacter() != null
                        && Character.toLowerCase(key.getCharacter()) == 'j') {
                    next = selected[0].plusDays(7);
                } else {
                    return;
                }

                handled.set(true);
                selected[0] = next;
                monthView[0] = next.withDayOfMonth(1);
                refresh[0].run();
            }
        });

        dialog.setComponent(root);
        dialog.addWindowListener(EscClose.of(dialog));
        refresh[0].run();
        gui.addWindowAndWait(dialog);
        return pickedDate[0];
    }

    private static String weekHeaderLine() {
        return String.join(" ", Arrays.stream(WEEKDAYS)
                .map(day -> String.format(" %-3s", day))
                .toArray(String[]::new));
    }

    private static String weekLine(LocalDate start, LocalDate selected, LocalDate today, int currentMonth) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(dayCell(start.plusDays(i), selected, today, currentMonth));
        }
        return sb.toString();
    }

    private static String dayCell(LocalDate date, LocalDate selected, LocalDate today, int currentMonth) {
        String day = String.format("%2d", date.getDayOfMonth());
        if (date.equals(selected)) {
            return "[" + day + "]";
        }
        if (date.equals(today)) {
            return "*" + day + "*";
        }
        if (date.getMonthValue() != currentMonth) {
            return "(" + day + ")";
        }
        return " " + day + " ";
    }

    private static String capitalize(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private static Panel centerHorizontally(com.googlecode.lanterna.gui2.Component content) {
        Panel row = new Panel(new LinearLayout(Direction.HORIZONTAL));
        row.addComponent(new EmptySpace(), Layouts.GROW);
        row.addComponent(content);
        row.addComponent(new EmptySpace(), Layouts.GROW);
        return row;
    }
}
