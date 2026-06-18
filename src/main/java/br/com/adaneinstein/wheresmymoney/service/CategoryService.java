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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final TransactionRepository transactionRepository;

    /** Nó da árvore hierárquica de subcategorias (recursivo). */
    public record SubTree(Subcategory sub, List<SubTree> children) {}

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
        return subcategoryRepository.findByCategoryIdOrderByNameAsc(categoryId);
    }

    /**
     * Árvore recursiva de subcategorias de uma categoria. Monta a hierarquia em
     * memória a partir da lista plana (todos os nós carregam a categoria raiz),
     * evitando {@code LazyInitializationException} e recursão em JPA.
     */
    @Transactional(readOnly = true)
    public List<SubTree> subcategoryForest(Long categoryId) {
        List<Subcategory> all = subcategoryRepository.findByCategoryIdOrderByNameAsc(categoryId);
        Map<Long, List<Subcategory>> byParent = new LinkedHashMap<>();
        for (Subcategory s : all) {
            Long parentId = s.getParentSubcategory() != null ? s.getParentSubcategory().getId() : null;
            byParent.computeIfAbsent(parentId, k -> new ArrayList<>()).add(s);
        }
        return buildTrees(byParent, null);
    }

    private List<SubTree> buildTrees(Map<Long, List<Subcategory>> byParent, Long parentId) {
        List<SubTree> result = new ArrayList<>();
        for (Subcategory s : byParent.getOrDefault(parentId, List.of())) {
            result.add(new SubTree(s, buildTrees(byParent, s.getId())));
        }
        return result;
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
    public Subcategory addChildSubcategory(Long parentSubcategoryId, String name) {
        Subcategory parent = subcategoryRepository.findById(parentSubcategoryId)
                .orElseThrow(() -> new IllegalArgumentException("Subcategoria inexistente: " + parentSubcategoryId));
        Subcategory child = new Subcategory(name);
        parent.addChild(child);
        return subcategoryRepository.save(child);
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
        Subcategory sub = subcategoryRepository.findById(subcategoryId)
                .orElseThrow(() -> new IllegalArgumentException("Subcategoria inexistente: " + subcategoryId));
        List<Long> ids = collectDescendantIds(sub);
        if (transactionRepository.existsBySubcategoryIdIn(ids)) {
            throw new IllegalStateException("Subcategoria (ou descendente) em uso por transações");
        }
        // orphanRemoval em childSubcategories cascateia a exclusão da subárvore.
        subcategoryRepository.deleteById(subcategoryId);
    }

    /** IDs da subcategoria e de todos os seus descendentes (DFS). */
    private List<Long> collectDescendantIds(Subcategory root) {
        List<Long> ids = new ArrayList<>();
        Deque<Subcategory> stack = new ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            Subcategory s = stack.pop();
            ids.add(s.getId());
            s.getChildSubcategories().forEach(stack::push);
        }
        return ids;
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
