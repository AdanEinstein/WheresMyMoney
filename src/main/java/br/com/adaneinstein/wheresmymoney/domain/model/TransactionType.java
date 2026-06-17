package br.com.adaneinstein.wheresmymoney.domain.model;

public enum TransactionType {
    INCOME("Receita"),
    EXPENSE("Despesa");

    private final String label;

    TransactionType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
