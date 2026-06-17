package br.com.adaneinstein.wheresmymoney.tui.screen;

import br.com.adaneinstein.wheresmymoney.domain.repository.CategoryTotal;
import br.com.adaneinstein.wheresmymoney.service.ReportService;
import br.com.adaneinstein.wheresmymoney.service.report.FinancialSummary;
import br.com.adaneinstein.wheresmymoney.tui.component.Bars;
import br.com.adaneinstein.wheresmymoney.tui.component.EscClose;
import br.com.adaneinstein.wheresmymoney.util.CurrencyUtil;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Borders;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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

        Panel root = new Panel();
        root.setLayoutManager(new LinearLayout(com.googlecode.lanterna.gui2.Direction.VERTICAL));

        String monthName = now.getMonth().getDisplayName(TextStyle.FULL, PT_BR);
        root.addComponent(new Label("Resumo de " + capitalize(monthName) + "/" + now.getYear()));
        root.addComponent(new EmptySpace());

        root.addComponent(colored("Receitas:  " + CurrencyUtil.format(summary.income()), TextColor.ANSI.GREEN_BRIGHT));
        root.addComponent(colored("Despesas:  " + CurrencyUtil.format(summary.expense()), TextColor.ANSI.RED_BRIGHT));
        TextColor balanceColor = summary.balance().signum() >= 0 ? TextColor.ANSI.GREEN_BRIGHT : TextColor.ANSI.RED_BRIGHT;
        root.addComponent(colored("Saldo:     " + CurrencyUtil.format(summary.balance()), balanceColor));

        Panel top = new Panel();
        top.setLayoutManager(new LinearLayout(com.googlecode.lanterna.gui2.Direction.VERTICAL));
        if (spending.isEmpty()) {
            top.addComponent(new Label("Sem despesas neste mês."));
        } else {
            BigDecimal max = spending.get(0).total();
            spending.stream().limit(5).forEach(ct ->
                    top.addComponent(new Label(String.format("%-16s %18s  %s",
                            truncate(ct.categoryName()),
                            CurrencyUtil.format(ct.total()),
                            Bars.bar(ct.total(), max, 20)))));
        }

        root.addComponent(new EmptySpace());
        root.addComponent(top.withBorder(Borders.singleLine("Maiores gastos do mês")));
        root.addComponent(new EmptySpace());

        BasicWindow window = new BasicWindow("Dashboard");
        window.setHints(Set.of(Window.Hint.CENTERED));
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
