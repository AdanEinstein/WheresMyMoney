package br.com.adaneinstein.wheresmymoney.service;

import br.com.adaneinstein.wheresmymoney.domain.model.Category;
import br.com.adaneinstein.wheresmymoney.domain.model.Subcategory;
import br.com.adaneinstein.wheresmymoney.domain.model.TransactionType;
import br.com.adaneinstein.wheresmymoney.domain.repository.CategoryRepository;
import br.com.adaneinstein.wheresmymoney.domain.repository.SubcategoryRepository;
import br.com.adaneinstein.wheresmymoney.domain.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public List<Category> findAll() {
        return categoryRepository.findAllByOrderByTypeAscNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Category> findByType(TransactionType type) {
        return categoryRepository.findByType(type);
    }

    @Transactional(readOnly = true)
    public List<Subcategory> subcategoriesOf(Long categoryId) {
        return subcategoryRepository.findByCategoryId(categoryId);
    }

    @Transactional
    public Category create(String name, TransactionType type, String colorHint) {
        return categoryRepository.save(new Category(name, type, colorHint));
    }

    @Transactional
    public Category save(Category category) {
        return categoryRepository.save(category);
    }

    @Transactional
    public Subcategory addSubcategory(Long categoryId, String name) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Categoria inexistente: " + categoryId));
        Subcategory sub = new Subcategory(name);
        category.addSubcategory(sub);
        categoryRepository.save(category);
        return sub;
    }

    @Transactional
    public Subcategory renameSubcategory(Long subcategoryId, String newName) {
        Subcategory sub = subcategoryRepository.findById(subcategoryId)
                .orElseThrow(() -> new IllegalArgumentException("Subcategoria inexistente: " + subcategoryId));
        sub.setName(newName);
        return subcategoryRepository.save(sub);
    }

    @Transactional
    public void deleteSubcategory(Long subcategoryId) {
        if (transactionRepository.existsBySubcategoryId(subcategoryId)) {
            throw new IllegalStateException("Subcategoria em uso por transações");
        }
        subcategoryRepository.deleteById(subcategoryId);
    }

    @Transactional
    public void delete(Long categoryId) {
        categoryRepository.deleteById(categoryId);
    }

    @Transactional(readOnly = true)
    public long count() {
        return categoryRepository.count();
    }
}
