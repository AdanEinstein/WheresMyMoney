package br.com.adaneinstein.wheresmymoney.service;

import br.com.adaneinstein.wheresmymoney.domain.model.Transaction;
import br.com.adaneinstein.wheresmymoney.domain.model.TransactionType;
import br.com.adaneinstein.wheresmymoney.domain.repository.CategoryTotal;
import br.com.adaneinstein.wheresmymoney.domain.repository.SubcategoryTotal;
import br.com.adaneinstein.wheresmymoney.domain.repository.TransactionRepository;
import br.com.adaneinstein.wheresmymoney.service.report.FinancialSummary;
import br.com.adaneinstein.wheresmymoney.service.report.SubcategoryTotalNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;
import br.com.adaneinstein.wheresmymoney.service.report.MonthlyPoint;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final TransactionRepository transactionRepository;

    /** Gastos (EXPENSE) por categoria no período, ordenados do maior para o menor. */
    @Transactional(readOnly = true)
    public List<CategoryTotal> spendingByCategory(LocalDate start, LocalDate end) {
        return transactionRepository.totalsByCategory(start, end).stream()
                .filter(ct -> ct.type() == TransactionType.EXPENSE)
                .toList();
    }

    /** Gastos (EXPENSE) por subcategoria no período, ordenados do maior para o menor. */
@Transactional(readOnly = true)
public List<SubcategoryTotalNode> spendingBySubcategory(LocalDate start, LocalDate end) {
    List<SubcategoryTotal> flatList =
        transactionRepository.totalsBySubcategory(start, end).stream()
            .filter(st -> st.type() == TransactionType.EXPENSE)
            .toList();

    Map<Long, SubcategoryTotalNode> nodes =
        flatList.stream()
            .map(
                st ->
                    new SubcategoryTotalNode(
                        st.categoryName(),
                        st.subcategoryId(),
                        st.subcategoryName(),
                        st.parentSubcategoryId(),
                        st.type(),
                        st.total()))
            .collect(Collectors.toMap(SubcategoryTotalNode::subcategoryId, n -> n));

    List<SubcategoryTotalNode> tree = new ArrayList<>();
    for (SubcategoryTotalNode node : nodes.values()) {
      if (node.parentSubcategoryId() != null) {
        nodes.get(node.parentSubcategoryId()).children().add(node);
      } else {
        tree.add(node);
      }
    }
    return tree;
}

    /** Receitas x despesas do período. */
    @Transactional(readOnly = true)
    public FinancialSummary summary(LocalDate start, LocalDate end) {
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        for (Transaction t : transactionRepository.findByDateBetweenOrderByDateDesc(start, end)) {
            if (t.getType() == TransactionType.INCOME) {
                income = income.add(t.getAmount());
            } else {
                expense = expense.add(t.getAmount());
            }
        }
        return new FinancialSummary(income, expense);
    }

    /**
     * Histórico dos últimos {@code monthsBack} meses (incluindo o mês de
     * referência). Agregação feita em Java para não depender de funções de
     * data específicas do dialeto SQLite.
     */
    @Transactional(readOnly = true)
    public List<MonthlyPoint> monthlyHistory(YearMonth reference, int monthsBack) {
        YearMonth first = reference.minusMonths(monthsBack - 1L);
        LocalDate start = first.atDay(1);
        LocalDate end = reference.atEndOfMonth();

        List<Transaction> all = transactionRepository.findByDateBetweenOrderByDateDesc(start, end);

        List<MonthlyPoint> points = new ArrayList<>();
        for (int i = 0; i < monthsBack; i++) {
            YearMonth ym = first.plusMonths(i);
            BigDecimal income = BigDecimal.ZERO;
            BigDecimal expense = BigDecimal.ZERO;
            for (Transaction t : all) {
                if (YearMonth.from(t.getDate()).equals(ym)) {
                    if (t.getType() == TransactionType.INCOME) {
                        income = income.add(t.getAmount());
                    } else {
                        expense = expense.add(t.getAmount());
                    }
                }
            }
            points.add(new MonthlyPoint(ym, income, expense));
        }
        return points;
    }
}
