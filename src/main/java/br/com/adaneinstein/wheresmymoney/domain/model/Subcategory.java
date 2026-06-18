package br.com.adaneinstein.wheresmymoney.domain.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "subcategory")
@Getter
@Setter
@NoArgsConstructor
public class Subcategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /** Categoria raiz à qual esta subcategoria pertence (preenchida em todos os níveis). */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    /** Subcategoria pai; {@code null} quando é filho direto da categoria raiz. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_subcategory_id")
    private Subcategory parentSubcategory;

    @OneToMany(mappedBy = "parentSubcategory", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Subcategory> childSubcategories = new ArrayList<>();

    public Subcategory(String name) {
        this.name = name;
    }

    /** Adiciona uma subcategoria filha mantendo o vínculo bidirecional e a categoria raiz. */
    public void addChild(Subcategory child) {
        child.setParentSubcategory(this);
        child.setCategory(this.category);
        this.childSubcategories.add(child);
    }

    @Override
    public String toString() {
        return name;
    }
}
