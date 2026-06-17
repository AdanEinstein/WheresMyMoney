package br.com.adaneinstein.wheresmymoney.domain.repository;

import br.com.adaneinstein.wheresmymoney.domain.model.Category;
import br.com.adaneinstein.wheresmymoney.domain.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByDateBetweenOrderByDateDesc(LocalDate start, LocalDate end);

    List<Transaction> findAllByOrderByDateDesc();

    /** Fallback de busca quando o Ollama está indisponível. */
    List<Transaction> findByDescriptionContainingIgnoreCaseOrNotesContainingIgnoreCaseOrderByDateDesc(
            String description, String notes);

    List<Transaction> findByCategory(Category category);

    long countByCategory(Category category);

    List<Transaction> findByEmbeddingIsNull();

    List<Transaction> findByEmbeddingIsNotNull();

    /**
     * Total por categoria num período. Sem funções de data no SELECT (só no WHERE)
     * para evitar atrito com o dialeto community do SQLite.
     */
    @Query("""
            select new br.com.adaneinstein.wheresmymoney.domain.repository.CategoryTotal(
                t.category.name, t.type, sum(t.amount))
            from Transaction t
            where t.date between :start and :end
            group by t.category.name, t.type
            order by sum(t.amount) desc
            """)
    List<CategoryTotal> totalsByCategory(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
