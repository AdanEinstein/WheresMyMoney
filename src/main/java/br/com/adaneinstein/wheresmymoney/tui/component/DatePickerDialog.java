package br.com.adaneinstein.wheresmymoney.tui.component;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.ComboBox;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Set;

public final class DatePickerDialog {

    private DatePickerDialog() {
    }

    public static LocalDate show(WindowBasedTextGUI gui, String title, LocalDate initialDate) {
        LocalDate initial = initialDate != null ? initialDate : LocalDate.now();

        BasicWindow dialog = new BasicWindow(title);
        dialog.setHints(Set.of(Window.Hint.CENTERED));

        ComboBox<Integer> dayCombo = new ComboBox<>();
        ComboBox<Integer> monthCombo = new ComboBox<>();
        ComboBox<Integer> yearCombo = new ComboBox<>();

        for (int month = 1; month <= 12; month++) {
            monthCombo.addItem(month);
        }

        int currentYear = LocalDate.now().getYear();
        int minYear = Math.min(currentYear, initial.getYear()) - 20;
        int maxYear = Math.max(currentYear, initial.getYear()) + 20;
        for (int year = minYear; year <= maxYear; year++) {
            yearCombo.addItem(year);
        }

        final int[] desiredDay = {initial.getDayOfMonth()};
        final Runnable refreshDays = () -> {
            Integer year = yearCombo.getSelectedItem();
            Integer month = monthCombo.getSelectedItem();
            if (year == null || month == null) {
                return;
            }
            int maxDay = YearMonth.of(year, month).lengthOfMonth();
            int selectedDay = Math.min(desiredDay[0], maxDay);

            dayCombo.clearItems();
            for (int day = 1; day <= maxDay; day++) {
                dayCombo.addItem(day);
            }
            dayCombo.setSelectedItem(selectedDay);
        };

        yearCombo.addListener((selected, previousSelection, changedByUserInteraction) -> refreshDays.run());
        monthCombo.addListener((selected, previousSelection, changedByUserInteraction) -> refreshDays.run());
        dayCombo.addListener((selected, previousSelection, changedByUserInteraction) -> desiredDay[0] = selected);

        yearCombo.setSelectedItem(initial.getYear());
        monthCombo.setSelectedItem(initial.getMonthValue());
        refreshDays.run();

        Panel grid = new Panel(new GridLayout(2));
        grid.addComponent(new Label("Dia"));
        grid.addComponent(dayCombo);
        grid.addComponent(new Label("Mês"));
        grid.addComponent(monthCombo);
        grid.addComponent(new Label("Ano"));
        grid.addComponent(yearCombo);

        final LocalDate[] selectedDate = {null};

        Panel actions = new Panel(new LinearLayout(Direction.HORIZONTAL));
        actions.addComponent(new Button("Hoje", () -> {
            LocalDate today = LocalDate.now();
            desiredDay[0] = today.getDayOfMonth();
            yearCombo.setSelectedItem(today.getYear());
            monthCombo.setSelectedItem(today.getMonthValue());
            refreshDays.run();
        }));
        actions.addComponent(new Button("Selecionar", () -> {
            Integer day = dayCombo.getSelectedItem();
            Integer month = monthCombo.getSelectedItem();
            Integer year = yearCombo.getSelectedItem();
            if (day != null && month != null && year != null) {
                selectedDate[0] = LocalDate.of(year, month, day);
                dialog.close();
            }
        }));
        actions.addComponent(new Button("Cancelar", dialog::close));

        Panel root = new Panel(new LinearLayout(Direction.VERTICAL));
        root.addComponent(grid);
        root.addComponent(new EmptySpace(new TerminalSize(1, 1)));
        root.addComponent(actions);

        dialog.setComponent(root);
        dialog.addWindowListener(EscClose.of(dialog));
        gui.addWindowAndWait(dialog);
        return selectedDate[0];
    }
}
