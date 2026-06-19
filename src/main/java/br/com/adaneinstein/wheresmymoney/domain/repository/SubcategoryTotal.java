package br.com.adaneinstein.wheresmymoney.domain.repository;

import br.com.adaneinstein.wheresmymoney.domain.model.TransactionType;

import java.math.BigDecimal;

/** Projeção: total por subcategoria (dentro de uma categoria) num período. */
public record SubcategoryTotal(
    String categoryName,
    Long subcategoryId,
    String subcategoryName,
    Long parentSubcategoryId,
    TransactionType type,
    BigDecimal total) {
}
