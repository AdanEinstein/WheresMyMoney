package br.com.adaneinstein.wheresmymoney.domain.repository;

import br.com.adaneinstein.wheresmymoney.domain.model.Category;
import br.com.adaneinstein.wheresmymoney.domain.model.TransactionType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    @EntityGraph(attributePaths = "subcategories")
    List<Category> findByType(TransactionType type);

    @EntityGraph(attributePaths = "subcategories")
    List<Category> findAllByOrderByTypeAscNameAsc();

    boolean existsByNameIgnoreCase(String name);
}
