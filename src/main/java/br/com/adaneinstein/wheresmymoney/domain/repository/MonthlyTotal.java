package br.com.adaneinstein.wheresmymoney.domain.repository;

import br.com.adaneinstein.wheresmymoney.domain.model.TransactionType;

import java.math.BigDecimal;

/** Projeção: total por mês e tipo (histórico comparativo). */
public record MonthlyTotal(int year, int month, TransactionType type, BigDecimal total) {
}
