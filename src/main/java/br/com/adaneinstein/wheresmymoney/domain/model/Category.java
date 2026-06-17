package br.com.adaneinstein.wheresmymoney.domain.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "category")
@Getter
@Setter
@NoArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    /** Dica de cor para a TUI (nome simples: "red", "green", "cyan"...). */
    private String colorHint;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Subcategory> subcategories = new ArrayList<>();

    public Category(String name, TransactionType type, String colorHint) {
        this.name = name;
        this.type = type;
        this.colorHint = colorHint;
    }

    public void addSubcategory(Subcategory sub) {
        sub.setCategory(this);
        this.subcategories.add(sub);
    }

    @Override
    public String toString() {
        return name;
    }
}
