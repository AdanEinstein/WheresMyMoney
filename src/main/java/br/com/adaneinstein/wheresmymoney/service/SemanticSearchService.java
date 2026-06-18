package br.com.adaneinstein.wheresmymoney.service;

import br.com.adaneinstein.wheresmymoney.domain.model.Transaction;
import br.com.adaneinstein.wheresmymoney.domain.repository.TransactionRepository;
import br.com.adaneinstein.wheresmymoney.util.CosineSimilarity;
import br.com.adaneinstein.wheresmymoney.util.FloatBytes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * Busca inteligente sobre as transações. Usa embeddings do Ollama quando
 * disponível (similaridade de cosseno em memória — o dataset pessoal é
 * pequeno) e cai para busca textual (LIKE) quando o Ollama está offline.
 */
@Service
@RequiredArgsConstructor
public class SemanticSearchService {

    private final TransactionRepository transactionRepository;
    private final EmbeddingService embeddingService;

    @Transactional(readOnly = true)
    public List<SearchResult> search(String query, int topN) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        float[] queryVector = embeddingService.embed(query);
        if (queryVector != null) {
            List<SearchResult> semantic = semanticSearch(queryVector, query, topN);
            if (!semantic.isEmpty()) {
                return semantic;
            }
        }
        return textualSearch(query, topN);
    }

    private List<SearchResult> semanticSearch(float[] queryVector, String query, int topN) {
        String q = query.toLowerCase();
        return transactionRepository.findByEmbeddingIsNotNull().stream()
                .map(t -> {
                    double semantic = CosineSimilarity.between(queryVector, FloatBytes.toFloats(t.getEmbedding()));
                    double hybrid = 0.7 * semantic + 0.3 * lexicalScore(t, q);
                    return new SearchResult(t, hybrid, SearchResult.Origin.SEMANTIC);
                })
                .filter(r -> r.score() > 0.0)
                .sorted(Comparator.comparingDouble(SearchResult::score).reversed())
                .limit(topN)
                .toList();
    }

    private double lexicalScore(Transaction t, String queryLower) {
        double desc = fieldLexicalScore(t.getDescription(), queryLower);
        double notes = fieldLexicalScore(t.getNotes(), queryLower);
        return Math.max(desc, notes);
    }

    private double fieldLexicalScore(String field, String queryLower) {
        if (field == null) return 0.0;
        String f = field.toLowerCase();
        if (f.equals(queryLower)) return 1.0;
        if (f.startsWith(queryLower)) return 0.7;
        if (f.matches(".*\\b" + java.util.regex.Pattern.quote(queryLower) + "\\b.*")) return 0.5;
        if (f.contains(queryLower)) return 0.3;
        return 0.0;
    }

    private List<SearchResult> textualSearch(String query, int topN) {
        List<Transaction> matches = transactionRepository
                .findByDescriptionContainingIgnoreCaseOrNotesContainingIgnoreCaseOrderByDateDesc(query, query);
        return matches.stream()
                .limit(topN)
                .map(t -> new SearchResult(t, 1.0, SearchResult.Origin.TEXTUAL))
                .toList();
    }
}
