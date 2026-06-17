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
            List<SearchResult> semantic = semanticSearch(queryVector, topN);
            if (!semantic.isEmpty()) {
                return semantic;
            }
        }
        return textualSearch(query, topN);
    }

    private List<SearchResult> semanticSearch(float[] queryVector, int topN) {
        return transactionRepository.findByEmbeddingIsNotNull().stream()
                .map(t -> new SearchResult(
                        t,
                        CosineSimilarity.between(queryVector, FloatBytes.toFloats(t.getEmbedding())),
                        SearchResult.Origin.SEMANTIC))
                .filter(r -> r.score() > 0.0)
                .sorted(Comparator.comparingDouble(SearchResult::score).reversed())
                .limit(topN)
                .toList();
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
