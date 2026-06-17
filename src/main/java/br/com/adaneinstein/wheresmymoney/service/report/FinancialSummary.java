package br.com.adaneinstein.wheresmymoney.service.report;

import java.math.BigDecimal;

/** Resumo de receitas x despesas de um período. */
public record FinancialSummary(BigDecimal income, BigDecimal expense) {

    public BigDecimal balance() {
        return income.subtract(expense);
    }
}
