package br.com.adaneinstein.wheresmymoney.service.report;

import java.math.BigDecimal;
import java.time.YearMonth;

/** Ponto do histórico mensal: receitas e despesas de um mês. */
public record MonthlyPoint(YearMonth month, BigDecimal income, BigDecimal expense) {

    public BigDecimal balance() {
        return income.subtract(expense);
    }
}
