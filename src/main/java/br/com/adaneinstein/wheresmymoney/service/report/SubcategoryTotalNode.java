package br.com.adaneinstein.wheresmymoney.service.report;

import br.com.adaneinstein.wheresmymoney.domain.model.TransactionType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public record SubcategoryTotalNode(
    String categoryName,
    Long subcategoryId,
    String subcategoryName,
    Long parentSubcategoryId,
    TransactionType type,
    BigDecimal total,
    List<SubcategoryTotalNode> children) {

  public SubcategoryTotalNode(
      String categoryName,
      Long subcategoryId,
      String subcategoryName,
      Long parentSubcategoryId,
      TransactionType type,
      BigDecimal total) {
    this(
        categoryName,
        subcategoryId,
        subcategoryName,
        parentSubcategoryId,
        type,
        total,
        new ArrayList<>());
  }
}
