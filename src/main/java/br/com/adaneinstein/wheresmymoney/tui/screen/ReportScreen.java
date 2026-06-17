package br.com.adaneinstein.wheresmymoney.tui.screen;

import br.com.adaneinstein.wheresmymoney.domain.repository.CategoryTotal;
import br.com.adaneinstein.wheresmymoney.service.ReportService;
import br.com.adaneinstein.wheresmymoney.service.report.FinancialSummary;
import br.com.adaneinstein.wheresmymoney.service.report.MonthlyPoint;
import br.com.adaneinstein.wheresmymoney.tui.component.Bars;
import br.com.adaneinstein.wheresmymoney.tui.component.EscClose;
import br.com.adaneinstein.wheresmymoney.tui.component.Layouts;
import br.com.adaneinstein.wheresmymoney.util.CurrencyUtil;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Borders;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.ComboBox;
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
import java.util.List;

@Component
@RequiredArgsConstructor
public class ReportScreen {

    private final ReportService reportService;

    public void open(WindowBasedTextGUI gui) {
        BasicWindow window = Layouts.fullScreen("Relatórios");

        ComboBox<String> periodCombo = new ComboBox<>("Mês atual", "Mês anterior", "Ano atual");

        Panel content = new Panel(new LinearLayout(Direction.VERTICAL));
        Runnable refresh = () -> renderContent(gui, content, periodCombo.getSelectedItem());

        Panel header = new Panel(new LinearLayout(Direction.HORIZONTAL));
        header.addComponent(new Label("Período:"));
        header.addComponent(periodCombo);
        header.addComponent(new Button("Atualizar", refresh));
        periodCombo.addListener((sel, prev, byUser) -> refresh.run());

        Panel root = new Panel(new LinearLayout(Direction.VERTICAL));
        root.addComponent(header);
        root.addComponent(new EmptySpace());
        root.addComponent(content, Layouts.GROW);
        root.addComponent(new EmptySpace());
        root.addComponent(new Button("Fechar (Esc)", window::close));

        renderContent(gui, content, periodCombo.getSelectedItem());

        window.setComponent(root);
        window.addWindowListener(EscClose.of(window));
        gui.addWindowAndWait(window);
    }

    private void renderContent(WindowBasedTextGUI gui, Panel content, String period) {
        content.removeAllComponents();
        YearMonth ref = YearMonth.now();
        LocalDate start;
        LocalDate end;
        switch (period == null ? "Mês atual" : period) {
            case "Mês anterior" -> {
                ref = ref.minusMonths(1);
                start = ref.atDay(1);
                end = ref.atEndOfMonth();
            }
            case "Ano atual" -> {
                start = LocalDate.of(ref.getYear(), 1, 1);
                end = LocalDate.of(ref.getYear(), 12, 31);
            }
            default -> {
                start = ref.atDay(1);
                end = ref.atEndOfMonth();
            }
        }

        FinancialSummary summary = reportService.summary(start, end);
        content.addComponent(colored("Receitas: " + CurrencyUtil.format(summary.income()), TextColor.ANSI.GREEN_BRIGHT));
        content.addComponent(colored("Despesas: " + CurrencyUtil.format(summary.expense()), TextColor.ANSI.RED_BRIGHT));
        content.addComponent(colored("Saldo:    " + CurrencyUtil.format(summary.balance()),
                summary.balance().signum() >= 0 ? TextColor.ANSI.GREEN_BRIGHT : TextColor.ANSI.RED_BRIGHT));

        int barWidth = Math.max(10, Layouts.cols(gui) / 4);
        Panel byCat = new Panel(new LinearLayout(Direction.VERTICAL));
        List<CategoryTotal> spending = reportService.spendingByCategory(start, end);
        if (spending.isEmpty()) {
            byCat.addComponent(new Label("Sem despesas no período."));
        } else {
            BigDecimal max = spending.get(0).total();
            for (CategoryTotal ct : spending) {
                byCat.addComponent(new Label(String.format("%-16s %18s  %s",
                        truncate(ct.categoryName()), CurrencyUtil.format(ct.total()), Bars.bar(ct.total(), max, barWidth))));
            }
        }

        Panel history = new Panel(new LinearLayout(Direction.VERTICAL));
        for (MonthlyPoint p : reportService.monthlyHistory(YearMonth.now(), 6)) {
            history.addComponent(new Label(String.format("%s  receitas %14s  despesas %14s  saldo %14s",
                    p.month(), CurrencyUtil.format(p.income()),
                    CurrencyUtil.format(p.expense()), CurrencyUtil.format(p.balance()))));
        }

        // Gastos por categoria e histórico lado-a-lado, usando a largura disponível.
        Panel columns = new Panel(new LinearLayout(Direction.HORIZONTAL));
        columns.addComponent(byCat.withBorder(Borders.singleLine("Gastos por categoria")), Layouts.GROW);
        columns.addComponent(history.withBorder(Borders.singleLine("Histórico (6 meses)")), Layouts.GROW);
        content.addComponent(new EmptySpace());
        content.addComponent(columns, Layouts.GROW);
    }

    private static Label colored(String text, TextColor color) {
        Label label = new Label(text);
        label.setForegroundColor(color);
        return label;
    }

    private static String truncate(String s) {
        return s.length() > 16 ? s.substring(0, 15) + "…" : s;
    }
}
