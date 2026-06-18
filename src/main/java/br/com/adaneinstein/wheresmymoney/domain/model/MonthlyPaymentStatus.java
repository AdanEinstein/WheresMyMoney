package br.com.adaneinstein.wheresmymoney.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Marca que um {@link MonthlyPayment} foi pago num determinado mês/ano e
 * referencia a {@link Transaction} lançada. A ausência de um registro para o mês
 * significa "pendente".
 */
@Entity
@Table(name = "monthly_payment_status",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"monthly_payment_id", "year", "month"}))
@Getter
@Setter
@NoArgsConstructor
public class MonthlyPaymentStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "monthly_payment_id", nullable = false)
    private MonthlyPayment monthlyPayment;

    @Column(name = "year", nullable = false)
    private int year;

    @Column(name = "month", nullable = false)
    private int month;

    /** Transação lançada ao marcar como pago; pode ficar nula se a transação foi removida externamente. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @Column(name = "paid_at", nullable = false)
    private LocalDateTime paidAt;
}
