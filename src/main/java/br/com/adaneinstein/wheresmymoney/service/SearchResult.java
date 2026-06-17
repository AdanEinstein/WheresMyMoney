package br.com.adaneinstein.wheresmymoney.service;

import br.com.adaneinstein.wheresmymoney.domain.model.Transaction;

/** Resultado de busca com pontuação e origem (semântica vs textual). */
public record SearchResult(Transaction transaction, double score, Origin origin) {

    public enum Origin {
        SEMANTIC,
        TEXTUAL
    }
}
