package br.com.adaneinstein.wheresmymoney.service;

import br.com.adaneinstein.wheresmymoney.domain.model.Category;
import br.com.adaneinstein.wheresmymoney.domain.model.Subcategory;
import br.com.adaneinstein.wheresmymoney.domain.model.Transaction;
import br.com.adaneinstein.wheresmymoney.domain.repository.TransactionRepository;
import br.com.adaneinstein.wheresmymoney.util.FloatBytes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final EmbeddingService embeddingService;

    @Transactional(readOnly = true)
    public List<Transaction> findAll() {
        return transactionRepository.findAllByOrderByDateDesc();
    }

    @Transactional(readOnly = true)
    public List<Transaction> findBetween(LocalDate start, LocalDate end) {
        return transactionRepository.findByDateBetweenOrderByDateDesc(start, end);
    }

    @Transactional
    public Transaction create(String description, BigDecimal amount, LocalDate date,
                              Category category, Subcategory subcategory, String notes) {
        Transaction t = new Transaction();
        t.setDescription(description);
        t.setAmount(amount);
        t.setDate(date);
        t.setCategory(category);
        t.setType(category.getType());
        t.setSubcategory(subcategory);
        t.setNotes(notes);
        t.setCreatedAt(LocalDateTime.now());
        applyEmbedding(t);
        return transactionRepository.save(t);
    }

    @Transactional
    public Transaction update(Transaction t) {
        applyEmbedding(t);
        return transactionRepository.save(t);
    }

    @Transactional
    public void delete(Long id) {
        transactionRepository.deleteById(id);
    }

    /** Gera (ou regenera) o embedding da transação a partir do texto canônico. */
    private void applyEmbedding(Transaction t) {
        float[] vector = embeddingService.embed(t.toEmbeddingText());
        if (vector != null) {
            t.setEmbedding(FloatBytes.toBytes(vector));
            t.setEmbeddingModel(embeddingService.getModel());
        }
    }

    /**
     * Recalcula embeddings das transações sem vetor (ou de todas se {@code force}).
     * Usado quando o Ollama sobe depois ou o modelo muda. Retorna quantas indexou.
     */
    @Transactional
    public int reindexAll(boolean force) {
        if (!embeddingService.refreshAvailability()) {
            return 0;
        }
        List<Transaction> pending = force
                ? transactionRepository.findAll()
                : transactionRepository.findByEmbeddingIsNull();
        int count = 0;
        for (Transaction t : pending) {
            float[] vector = embeddingService.embed(t.toEmbeddingText());
            if (vector != null) {
                t.setEmbedding(FloatBytes.toBytes(vector));
                t.setEmbeddingModel(embeddingService.getModel());
                transactionRepository.save(t);
                count++;
            }
        }
        log.info("Reindexação concluída: {} transações", count);
        return count;
    }
}
