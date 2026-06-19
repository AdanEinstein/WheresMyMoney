package br.com.adaneinstein.wheresmymoney.tui.screen;

import br.com.adaneinstein.wheresmymoney.domain.model.Category;
import br.com.adaneinstein.wheresmymoney.domain.model.TransactionType;
import br.com.adaneinstein.wheresmymoney.domain.repository.CategoryTotal;
import br.com.adaneinstein.wheresmymoney.service.CategoryService;
import br.com.adaneinstein.wheresmymoney.service.ReportService;
import br.com.adaneinstein.wheresmymoney.service.report.FinancialSummary;
import br.com.adaneinstein.wheresmymoney.service.report.MonthlyPoint;
import br.com.adaneinstein.wheresmymoney.tui.AppTheme;
import br.com.adaneinstein.wheresmymoney.tui.component.EscClose;
import br.com.adaneinstein.wheresmymoney.tui.component.Layouts;
import br.com.adaneinstein.wheresmymoney.tui.component.LineChart;
import br.com.adaneinstein.wheresmymoney.util.CurrencyUtil;
import com.googlecode.lanterna.TerminalSize;
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
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.WindowListenerAdapter;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
public class ReportScreen {

    private static final String[] MONTHS = {
        "Jan", "Fev", "Mar", "Abr", "Mai", "Jun",
        "Jul", "Ago", "Set", "Out", "Nov", "Dez"
    };

    private final ReportService reportService;
    private final CategoryService categoryService;

    public void open(WindowBasedTextGUI gui) {
        BasicWindow window = Layouts.fullScreen("Relatórios");
        State state = new State();

        Panel content = new Panel(new LinearLayout(Direction.HORIZONTAL));

        ComboBox<Integer> yearCombo = new ComboBox<>();
        int currentYear = LocalDate.now().getYear();
        for (int y = currentYear - 4; y <= currentYear + 1; y++) {
            yearCombo.addItem(y);
        }
        yearCombo.setSelectedItem(currentYear);
        yearCombo.addListener((sel, prev, byUser) ->
            renderContent(gui, content, yearCombo.getSelectedItem(), state));

        ComboBox<String> monthCombo = new ComboBox<>();
        for (String m : MONTHS) {
            monthCombo.addItem(m);
        }
        state.monthCombo = monthCombo;
        monthCombo.addListener((sel, prev, byUser) -> selectMonth(state, sel));

        Panel header = new Panel(new LinearLayout(Direction.HORIZONTAL));
        header.addComponent(new Label("Ano:"));
        header.addComponent(yearCombo);
        header.addComponent(new Label("  Mês:"));
        header.addComponent(monthCombo);

        Panel legend = new Panel(new LinearLayout(Direction.HORIZONTAL));
        legend.addComponent(colored("● Despesas", AppTheme.EXPENSE));
        legend.addComponent(colored("   ● Receitas", AppTheme.INCOME));
        legend.addComponent(Layouts.hint("    (use ← → para escolher o mês)"));

        Panel root = new Panel(new LinearLayout(Direction.VERTICAL));
        root.addComponent(header);
        root.addComponent(legend);
        root.addComponent(new EmptySpace());
        root.addComponent(content, Layouts.GROW);
        root.addComponent(new EmptySpace());
        root.addComponent(new Button("Fechar (Esc)", window::close));

        renderContent(gui, content, currentYear, state);

        window.setComponent(root);
        window.addWindowListener(EscClose.of(window));
        window.addWindowListener(new MonthKeys(state));
        gui.addWindowAndWait(window);
    }

    private void renderContent(WindowBasedTextGUI gui, Panel content, int year, State state) {
        content.removeAllComponents();

        List<MonthlyPoint> points = reportService.monthlyHistory(YearMonth.of(year, 12), 12);
        double[] expenses = new double[points.size()];
        double[] incomes = new double[points.size()];
        for (int i = 0; i < points.size(); i++) {
            expenses[i] = points.get(i).expense().doubleValue();
            incomes[i] = points.get(i).income().doubleValue();
        }

        LineChart chart = new LineChart(expenses, incomes, MONTHS, AppTheme.EXPENSE, AppTheme.INCOME);
        state.chart = chart;
        state.points = points;

        Panel info = new Panel(new LinearLayout(Direction.VERTICAL));
        info.setPreferredSize(new TerminalSize(32, 1));
        state.info = info;

        content.addComponent(
            chart.withBorder(Borders.singleLine("Despesas × Receitas " + year)),
            Layouts.GROW);
        content.addComponent(info.withBorder(Borders.singleLine("Categorias")), Layouts.FILL);

        // Posiciona no mês mais recente; o listener do combo dispara selectMonth.
        int last = points.isEmpty() ? 0 : points.size() - 1;
        if (state.monthCombo.getSelectedIndex() == last) {
            selectMonth(state, last); // mesmo índice não dispara o listener
        } else {
            state.monthCombo.setSelectedIndex(last);
        }
    }

    /** Move o mês selecionado, redesenha o destaque e atualiza as categorias. */
    private void selectMonth(State state, int month) {
        if (state.points.isEmpty()) {
            return;
        }
        month = Math.max(0, Math.min(month, state.points.size() - 1));
        state.selected = month;
        state.chart.setSelected(month);

        YearMonth ym = state.points.get(month).month();
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        Panel info = state.info;
        info.removeAllComponents();
        info.addComponent(Layouts.title(MONTHS[ym.getMonthValue() - 1] + "/" + ym.getYear()));
        info.addComponent(new EmptySpace());
        section(info, "Receitas", reportService.incomeByCategory(start, end),
                TransactionType.INCOME, AppTheme.INCOME);
        info.addComponent(new EmptySpace());
        section(info, "Despesas", reportService.spendingByCategory(start, end),
                TransactionType.EXPENSE, AppTheme.EXPENSE);
        info.addComponent(new EmptySpace());
        FinancialSummary summary = reportService.summary(start, end);
        info.addComponent(Layouts.title("Resumo"));
        info.addComponent(colored(
                String.format("%-16s %s", "Receitas", CurrencyUtil.format(summary.income())),
                AppTheme.INCOME));
        info.addComponent(colored(
                String.format("%-16s %s", "Gastos", CurrencyUtil.format(summary.expense())),
                AppTheme.EXPENSE));
        info.addComponent(colored(
                String.format("%-16s %s", "Saldo", CurrencyUtil.format(summary.balance())),
                summary.balance().signum() >= 0 ? AppTheme.INCOME : AppTheme.EXPENSE));
        info.invalidate(); // força redraw ao navegar com as setas
    }

    /** Renderiza uma seção (Receitas/Despesas); mês sem lançamentos vira categorias zeradas. */
    private void section(Panel info, String header, List<CategoryTotal> totals,
                         TransactionType type, TextColor color) {
        info.addComponent(Layouts.title(header));
        if (totals.isEmpty()) {
            for (Category c : categoryService.findByType(type)) {
                info.addComponent(colored(
                    String.format("%-16s %s", c.getName(), CurrencyUtil.format(BigDecimal.ZERO)),
                    color));
            }
        } else {
            for (CategoryTotal ct : totals) {
                info.addComponent(colored(
                    String.format("%-16s %s", ct.categoryName(), CurrencyUtil.format(ct.total())),
                    color));
            }
        }
    }

    private static Label colored(String text, TextColor color) {
        Label l = new Label(text);
        l.setForegroundColor(color);
        return l;
    }

    /** Estado mutável de uma sessão da tela (TUI é single-thread, um relatório por vez). */
    private static final class State {
        LineChart chart;
        Panel info;
        ComboBox<String> monthCombo;
        List<MonthlyPoint> points = List.of();
        int selected = -1;
    }

    /** Navega o mês selecionado com as setas ← →. */
    private final class MonthKeys extends WindowListenerAdapter {
        private final State state;

        MonthKeys(State state) {
            this.state = state;
        }

        @Override
        public void onInput(Window window, KeyStroke key, AtomicBoolean deliverEvent) {
            if (state.chart == null || state.points.isEmpty()) {
                return;
            }
            KeyType type = key.getKeyType();
            int idx = state.monthCombo.getSelectedIndex();
            if (type == KeyType.ArrowLeft) {
                state.monthCombo.setSelectedIndex(Math.max(0, idx - 1)); // listener atualiza tudo
                deliverEvent.set(false); // consome p/ não mover o foco horizontal
            } else if (type == KeyType.ArrowRight) {
                state.monthCombo.setSelectedIndex(Math.min(MONTHS.length - 1, idx + 1));
                deliverEvent.set(false);
            }
        }
    }
}
