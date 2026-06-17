package br.com.adaneinstein.wheresmymoney.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_entry")
@Getter
@Setter
@NoArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "entry_date", nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "subcategory_id")
    private Subcategory subcategory;

    @Column(length = 2000)
    private String notes;

    /**
     * Vetor de embedding serializado (float[] -> byte[]); nulo se ainda não
     * indexado. Sem {@code @Lob}: o driver sqlite-jdbc não suporta streaming de
     * LOB (SQLFeatureNotSupported), então mapeamos como binário simples.
     */
    @Column(name = "embedding", columnDefinition = "BLOB")
    private byte[] embedding;

    @Column(name = "embedding_model")
    private String embeddingModel;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** Texto canônico usado para gerar o embedding e para busca textual. */
    public String toEmbeddingText() {
        StringBuilder sb = new StringBuilder();
        sb.append(description);
        if (category != null) {
            sb.append(' ').append(category.getName());
        }
        if (subcategory != null) {
            sb.append(' ').append(subcategory.getName());
        }
        if (notes != null && !notes.isBlank()) {
            sb.append(' ').append(notes);
        }
        return sb.toString();
    }
}
