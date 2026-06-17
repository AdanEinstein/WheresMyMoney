package br.com.adaneinstein.wheresmymoney.domain.repository;

import br.com.adaneinstein.wheresmymoney.domain.model.TransactionType;

import java.math.BigDecimal;

/** Projeção: total por categoria num período (usada nos relatórios). */
public record CategoryTotal(String categoryName, TransactionType type, BigDecimal total) {
}
