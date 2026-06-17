package br.com.adaneinstein.wheresmymoney.tui.screen;

import br.com.adaneinstein.wheresmymoney.domain.repository.CategoryTotal;
import br.com.adaneinstein.wheresmymoney.service.ReportService;
import br.com.adaneinstein.wheresmymoney.service.report.FinancialSummary;
import br.com.adaneinstein.wheresmymoney.tui.component.Bars;
import br.com.adaneinstein.wheresmymoney.tui.component.EscClose;
import br.com.adaneinstein.wheresmymoney.tui.component.Layouts;
import br.com.adaneinstein.wheresmymoney.util.CurrencyUtil;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Borders;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class DashboardScreen {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private final ReportService reportService;

    public void open(WindowBasedTextGUI gui) {
        YearMonth now = YearMonth.now();
        LocalDate start = now.atDay(1);
        LocalDate end = now.atEndOfMonth();

        FinancialSummary summary = reportService.summary(start, end);
        List<CategoryTotal> spending = reportService.spendingByCategory(start, end);

        BasicWindow window = Layouts.fullScreen("Dashboard");

        Panel root = new Panel(new LinearLayout(Direction.VERTICAL));

        String monthName = now.getMonth().getDisplayName(TextStyle.FULL, PT_BR);
        root.addComponent(new Label("Resumo de " + capitalize(monthName) + "/" + now.getYear()));
        root.addComponent(new EmptySpace());

        // Coluna esquerda: totais do mês.
        Panel totals = new Panel(new LinearLayout(Direction.VERTICAL));
        totals.addComponent(colored("Receitas:  " + CurrencyUtil.format(summary.income()), TextColor.ANSI.GREEN_BRIGHT));
        totals.addComponent(colored("Despesas:  " + CurrencyUtil.format(summary.expense()), TextColor.ANSI.RED_BRIGHT));
        TextColor balanceColor = summary.balance().signum() >= 0 ? TextColor.ANSI.GREEN_BRIGHT : TextColor.ANSI.RED_BRIGHT;
        totals.addComponent(colored("Saldo:     " + CurrencyUtil.format(summary.balance()), balanceColor));

        // Coluna direita: maiores gastos, ocupando a largura restante.
        int barWidth = Math.max(10, Layouts.cols(gui) / 3);
        int maxRows = Math.max(5, Layouts.rows(gui) - 8);
        Panel top = new Panel(new LinearLayout(Direction.VERTICAL));
        if (spending.isEmpty()) {
            top.addComponent(new Label("Sem despesas neste mês."));
        } else {
            BigDecimal max = spending.get(0).total();
            spending.stream().limit(maxRows).forEach(ct ->
                    top.addComponent(new Label(String.format("%-16s %18s  %s",
                            truncate(ct.categoryName()),
                            CurrencyUtil.format(ct.total()),
                            Bars.bar(ct.total(), max, barWidth)))));
        }

        Panel body = new Panel(new LinearLayout(Direction.HORIZONTAL));
        body.addComponent(totals.withBorder(Borders.singleLine("Totais")));
        body.addComponent(top.withBorder(Borders.singleLine("Maiores gastos do mês")), Layouts.GROW);
        root.addComponent(body, Layouts.GROW);
        root.addComponent(new EmptySpace());

        Button close = new Button("Fechar (Esc)", window::close);
        root.addComponent(close);
        window.setComponent(root);
        window.addWindowListener(EscClose.of(window));
        gui.addWindowAndWait(window);
    }

    private static Label colored(String text, TextColor color) {
        Label label = new Label(text);
        label.setForegroundColor(color);
        return label;
    }

    private static String truncate(String s) {
        return s.length() > 16 ? s.substring(0, 15) + "…" : s;
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
