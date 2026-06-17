package br.com.adaneinstein.wheresmymoney.domain.repository;

import br.com.adaneinstein.wheresmymoney.domain.model.Category;
import br.com.adaneinstein.wheresmymoney.domain.model.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByType(TransactionType type);

    List<Category> findAllByOrderByTypeAscNameAsc();

    boolean existsByNameIgnoreCase(String name);
}
