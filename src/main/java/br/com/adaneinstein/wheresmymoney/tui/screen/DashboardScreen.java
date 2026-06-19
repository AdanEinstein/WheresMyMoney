package br.com.adaneinstein.wheresmymoney.tui.screen;

import br.com.adaneinstein.wheresmymoney.domain.repository.CategoryTotal;
import br.com.adaneinstein.wheresmymoney.service.report.SubcategoryTotalNode;
import br.com.adaneinstein.wheresmymoney.service.ReportService;
import br.com.adaneinstein.wheresmymoney.service.report.FinancialSummary;
import br.com.adaneinstein.wheresmymoney.tui.component.Bars;
import br.com.adaneinstein.wheresmymoney.tui.component.EscClose;
import br.com.adaneinstein.wheresmymoney.tui.component.Layouts;
import br.com.adaneinstein.wheresmymoney.tui.component.PeriodSelector;
import br.com.adaneinstein.wheresmymoney.tui.AppTheme;
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
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DashboardScreen {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private final ReportService reportService;

    public void open(WindowBasedTextGUI gui) {
        BasicWindow window = Layouts.fullScreen("Dashboard");

        Panel content = new Panel(new LinearLayout(Direction.VERTICAL));
        PeriodSelector period = new PeriodSelector(gui, () -> {});

        Panel root = new Panel(new LinearLayout(Direction.VERTICAL));
        root.addComponent(period);
        root.addComponent(new EmptySpace());
        root.addComponent(content, Layouts.GROW);
        root.addComponent(new EmptySpace());
        root.addComponent(new Button("Fechar (Esc)", window::close));

        period.setOnChange(() -> renderContent(gui, content, period.start(), period.end()));
        renderContent(gui, content, period.start(), period.end());

        window.setComponent(root);
        window.addWindowListener(EscClose.of(window));
        gui.addWindowAndWait(window);
    }

    private void renderContent(WindowBasedTextGUI gui, Panel content, LocalDate start, LocalDate end) {
    content.removeAllComponents();

    FinancialSummary summary = reportService.summary(start, end);
    List<CategoryTotal> spending = reportService.spendingByCategory(start, end);
    Map<String, List<SubcategoryTotalNode>> subsByCategory =
        reportService.spendingBySubcategory(start, end).stream()
            .collect(Collectors.groupingBy(SubcategoryTotalNode::categoryName));

    YearMonth refMonth = YearMonth.from(start);
    String monthName = refMonth.getMonth().getDisplayName(TextStyle.FULL, PT_BR);
    content.addComponent(Layouts.title("Resumo de " + capitalize(monthName) + "/" + refMonth.getYear()));
    content.addComponent(new EmptySpace());

    // Faixa de totais no topo, em linha — libera toda a largura para as barras abaixo.
    Panel totals = new Panel(new LinearLayout(Direction.HORIZONTAL));
    totals.addComponent(
        colored("Receitas:  " + CurrencyUtil.format(summary.income()), AppTheme.INCOME));
    totals.addComponent(new Label("    "));
    totals.addComponent(
        colored("Despesas:  " + CurrencyUtil.format(summary.expense()), AppTheme.EXPENSE));
    totals.addComponent(new Label("    "));
    TextColor balanceColor =
        summary.balance().signum() >= 0 ? AppTheme.INCOME : AppTheme.EXPENSE;
    totals.addComponent(colored("Saldo:     " + CurrencyUtil.format(summary.balance()), balanceColor));
    content.addComponent(totals.withBorder(Borders.singleLine("Totais")));

    // Hierarquia completa de gastos, ocupando a largura inteira.
    int barWidth = Math.max(10, Layouts.cols(gui) - 45);
    Panel top = new Panel(new LinearLayout(Direction.VERTICAL));
    if (spending.isEmpty()) {
      top.addComponent(new Label("Sem despesas no período."));
    } else {
      BigDecimal max = spending.get(0).total();
      // ponytail: sem scroll — hierarquia muito longa pode exceder a altura e ser cortada.
      spending.forEach(
          ct -> {
            top.addComponent(
                new Label(
                    String.format(
                        "%-16s %18s  %s",
                        truncate(ct.categoryName()),
                        CurrencyUtil.format(ct.total()),
                        Bars.bar(ct.total(), max, barWidth))));
            for (SubcategoryTotalNode st :
                subsByCategory.getOrDefault(ct.categoryName(), List.of())) {
              renderSubcategory(top, st, max, barWidth, 1);
            }
          });
    }
    content.addComponent(top.withBorder(Borders.singleLine("Gastos por categoria")), Layouts.GROW);
  }

  private void renderSubcategory(
      Panel top, SubcategoryTotalNode st, BigDecimal max, int barWidth, int level) {
    String prefix = " ".repeat(level * 2) + "› ";
    // ponytail: coluna esquerda fixa em 16 (= categorias) → valor/barra alinham em qualquer
    // profundidade; prefix >= 13 satura nameRoom em 3, mas o alinhamento se mantém.
    int nameRoom = Math.max(3, 16 - prefix.length());
    String left = prefix + truncate(st.subcategoryName(), nameRoom);
    top.addComponent(
        new Label(
            String.format(
                "%-16s %18s  %s",
                left,
                CurrencyUtil.format(st.total()),
                Bars.bar(st.total(), max, barWidth))));
    for (SubcategoryTotalNode child : st.children()) {
      renderSubcategory(top, child, max, barWidth, level + 1);
    }
  }

    private static Label colored(String text, TextColor color) {
        Label label = new Label(text);
        label.setForegroundColor(color);
        return label;
    }

    private static String truncate(String s, int width) {
        return s.length() > width ? s.substring(0, width - 1) + "…" : s;
    }

    private static String truncate(String s) {
        return truncate(s, 16);
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
