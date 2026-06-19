package br.com.adaneinstein.wheresmymoney.tui.component;

import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * Seletor de range de datas reutilizável: dois botões (Início/Fim) que abrem o
 * {@link DatePickerDialog} e um botão Atualizar. Default: mês atual.
 */
// ponytail: range = dois DatePickerDialog reaproveitados; inverte se start>end, sem mais validação.
public final class PeriodSelector extends Panel {

    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private LocalDate start;
    private LocalDate end;
    private Runnable onChange;

    public PeriodSelector(WindowBasedTextGUI gui, Runnable onChange) {
        super(new LinearLayout(Direction.HORIZONTAL));
        this.onChange = onChange != null ? onChange : () -> {};
        YearMonth now = YearMonth.now();
        this.start = now.atDay(1);
        this.end = now.atEndOfMonth();

        Button startButton = new Button(start.format(BR));
        Button endButton = new Button(end.format(BR));

        startButton.addListener(b -> {
            LocalDate picked = DatePickerDialog.show(gui, "Data inicial", start);
            if (picked != null) {
                start = picked;
                normalize();
                startButton.setLabel(start.format(BR));
                endButton.setLabel(end.format(BR));
                this.onChange.run();
            }
        });
        endButton.addListener(b -> {
            LocalDate picked = DatePickerDialog.show(gui, "Data final", end);
            if (picked != null) {
                end = picked;
                normalize();
                startButton.setLabel(start.format(BR));
                endButton.setLabel(end.format(BR));
                this.onChange.run();
            }
        });

        addComponent(new Label("Período:"));
        addComponent(startButton);
        addComponent(new Label("até"));
        addComponent(endButton);
        addComponent(new Button("Atualizar", () -> this.onChange.run()));
    }

    /** Permite definir o callback após a construção (telas que referenciam o próprio selector). */
    public void setOnChange(Runnable onChange) {
        this.onChange = onChange != null ? onChange : () -> {};
    }

    private void normalize() {
        if (start.isAfter(end)) {
            LocalDate tmp = start;
            start = end;
            end = tmp;
        }
    }

    public LocalDate start() {
        return start;
    }

    public LocalDate end() {
        return end;
    }
}
