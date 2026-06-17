package br.com.adaneinstein.wheresmymoney.service;

import br.com.adaneinstein.wheresmymoney.domain.model.Category;
import br.com.adaneinstein.wheresmymoney.domain.model.TransactionType;
import br.com.adaneinstein.wheresmymoney.domain.repository.CategoryTotal;
import br.com.adaneinstein.wheresmymoney.service.report.FinancialSummary;
import br.com.adaneinstein.wheresmymoney.service.report.MonthlyPoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReportServiceTest {

    @Autowired
    private ReportService reportService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private TransactionService transactionService;

    @Test
    void summaryComputesIncomeExpenseAndBalance() {
        Category salario = categoryService.create("T_Salário", TransactionType.INCOME, "green");
        Category mercado = categoryService.create("T_Mercado", TransactionType.EXPENSE, "red");
        LocalDate today = LocalDate.now();

        transactionService.create("Salário", new BigDecimal("5000.00"), today, salario, null, null);
        transactionService.create("Compras", new BigDecimal("300.00"), today, mercado, null, null);
        transactionService.create("Compras 2", new BigDecimal("200.00"), today, mercado, null, null);

        YearMonth ym = YearMonth.from(today);
        FinancialSummary summary = reportService.summary(ym.atDay(1), ym.atEndOfMonth());

        assertThat(summary.income()).isEqualByComparingTo("5000.00");
        assertThat(summary.expense()).isEqualByComparingTo("500.00");
        assertThat(summary.balance()).isEqualByComparingTo("4500.00");
    }

    @Test
    void spendingByCategoryReturnsOnlyExpensesSortedDesc() {
        Category mercado = categoryService.create("T_Cat_A", TransactionType.EXPENSE, "red");
        Category transporte = categoryService.create("T_Cat_B", TransactionType.EXPENSE, "yellow");
        LocalDate today = LocalDate.now();

        transactionService.create("a", new BigDecimal("100.00"), today, mercado, null, null);
        transactionService.create("b", new BigDecimal("400.00"), today, transporte, null, null);

        YearMonth ym = YearMonth.from(today);
        List<CategoryTotal> spending = reportService.spendingByCategory(ym.atDay(1), ym.atEndOfMonth());

        assertThat(spending).allMatch(ct -> ct.type() == TransactionType.EXPENSE);
        // maior gasto primeiro
        assertThat(spending.get(0).total()).isGreaterThanOrEqualTo(spending.get(spending.size() - 1).total());
    }

    @Test
    void monthlyHistoryReturnsRequestedNumberOfMonths() {
        List<MonthlyPoint> points = reportService.monthlyHistory(YearMonth.now(), 6);
        assertThat(points).hasSize(6);
        assertThat(points.get(5).month()).isEqualTo(YearMonth.now());
    }
}
