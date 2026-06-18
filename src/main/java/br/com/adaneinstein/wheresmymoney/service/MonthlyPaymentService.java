package br.com.adaneinstein.wheresmymoney.service;

import br.com.adaneinstein.wheresmymoney.domain.model.Category;
import br.com.adaneinstein.wheresmymoney.domain.model.MonthlyPayment;
import br.com.adaneinstein.wheresmymoney.domain.model.MonthlyPaymentStatus;
import br.com.adaneinstein.wheresmymoney.domain.model.Subcategory;
import br.com.adaneinstein.wheresmymoney.domain.model.Transaction;
import br.com.adaneinstein.wheresmymoney.domain.repository.MonthlyPaymentRepository;
import br.com.adaneinstein.wheresmymoney.domain.repository.MonthlyPaymentStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gerencia os gastos previstos mensais ({@link MonthlyPayment}) e o checklist do
 * que já foi pago em cada mês. Ao marcar como pago, lança a transação correspondente
 * via {@link TransactionService}; ao desmarcar, remove essa transação.
 */
@Service
@RequiredArgsConstructor
public class MonthlyPaymentService {

    private final MonthlyPaymentRepository paymentRepository;
    private final MonthlyPaymentStatusRepository statusRepository;
    private final TransactionService transactionService;

    /** Linha do checklist: o template, se já foi pago no mês e a transação vinculada. */
    public record MonthlyPaymentView(MonthlyPayment payment, boolean paid, Long transactionId) {}

    /** Totais do mês: previsto (todos os ativos) e pago (somente marcados). */
    public record Totals(BigDecimal expected, BigDecimal paid) {
        public BigDecimal remaining() {
            return expected.subtract(paid);
        }
    }

    // ── CRUD de templates ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<MonthlyPayment> listActive() {
        return paymentRepository.findByActiveTrueOrderByDueDayAscDescriptionAsc();
    }

    @Transactional
    public MonthlyPayment create(String description, BigDecimal amount, int dueDay,
                                 Category category, Subcategory subcategory) {
        MonthlyPayment p = new MonthlyPayment();
        p.setDescription(description);
        p.setAmount(amount);
        p.setDueDay(clampDay(dueDay));
        p.setCategory(category);
        p.setSubcategory(subcategory);
        p.setActive(true);
        p.setCreatedAt(LocalDateTime.now());
        return paymentRepository.save(p);
    }

    @Transactional
    public MonthlyPayment update(Long id, String description, BigDecimal amount, int dueDay,
                                 Category category, Subcategory subcategory) {
        MonthlyPayment p = paymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pagamento inexistente: " + id));
        p.setDescription(description);
        p.setAmount(amount);
        p.setDueDay(clampDay(dueDay));
        p.setCategory(category);
        p.setSubcategory(subcategory);
        return paymentRepository.save(p);
    }

    /** Exclui o template e seus registros de status. As transações já lançadas são preservadas. */
    @Transactional
    public void delete(Long id) {
        statusRepository.deleteAll(statusRepository.findByMonthlyPaymentId(id));
        paymentRepository.deleteById(id);
    }

    // ── Checklist do mês ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<MonthlyPaymentView> view(YearMonth ym) {
        Map<Long, MonthlyPaymentStatus> statusByPayment = statusIndex(ym);
        List<MonthlyPayment> payments = paymentRepository.findByActiveTrueOrderByDueDayAscDescriptionAsc();
        return payments.stream()
                .map(p -> {
                    MonthlyPaymentStatus st = statusByPayment.get(p.getId());
                    Long txId = st != null && st.getTransaction() != null ? st.getTransaction().getId() : null;
                    return new MonthlyPaymentView(p, st != null, txId);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public Totals totals(YearMonth ym) {
        Map<Long, MonthlyPaymentStatus> statusByPayment = statusIndex(ym);
        BigDecimal expected = BigDecimal.ZERO;
        BigDecimal paid = BigDecimal.ZERO;
        for (MonthlyPayment p : paymentRepository.findByActiveTrueOrderByDueDayAscDescriptionAsc()) {
            expected = expected.add(p.getAmount());
            if (statusByPayment.containsKey(p.getId())) {
                paid = paid.add(p.getAmount());
            }
        }
        return new Totals(expected, paid);
    }

    /**
     * Marca o pagamento como pago no mês: lança a transação (EXPENSE/INCOME conforme a
     * categoria, data = hoje) e grava o status. Idempotente — não duplica se já pago.
     */
    @Transactional
    public void markPaid(Long paymentId, YearMonth ym) {
        if (statusRepository.findByMonthlyPaymentIdAndYearAndMonth(paymentId, ym.getYear(), ym.getMonthValue())
                .isPresent()) {
            return;
        }
        MonthlyPayment p = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Pagamento inexistente: " + paymentId));

        String notes = "Pagamento mensal: " + p.getDescription()
                + String.format(" (%02d/%d)", ym.getMonthValue(), ym.getYear());
        Transaction tx = transactionService.create(
                p.getDescription(), p.getAmount(), LocalDate.now(),
                p.getCategory(), p.getSubcategory(), notes);

        MonthlyPaymentStatus status = new MonthlyPaymentStatus();
        status.setMonthlyPayment(p);
        status.setYear(ym.getYear());
        status.setMonth(ym.getMonthValue());
        status.setTransaction(tx);
        status.setPaidAt(LocalDateTime.now());
        statusRepository.save(status);
    }

    /** Desmarca: remove o status do mês e a transação vinculada (se existir). */
    @Transactional
    public void unmarkPaid(Long paymentId, YearMonth ym) {
        statusRepository.findByMonthlyPaymentIdAndYearAndMonth(paymentId, ym.getYear(), ym.getMonthValue())
                .ifPresent(status -> {
                    Long txId = status.getTransaction() != null ? status.getTransaction().getId() : null;
                    statusRepository.delete(status);
                    if (txId != null) {
                        transactionService.delete(txId);
                    }
                });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    private Map<Long, MonthlyPaymentStatus> statusIndex(YearMonth ym) {
        Map<Long, MonthlyPaymentStatus> map = new HashMap<>();
        for (MonthlyPaymentStatus st : statusRepository.findByYearAndMonth(ym.getYear(), ym.getMonthValue())) {
            map.put(st.getMonthlyPayment().getId(), st);
        }
        return map;
    }

    private static int clampDay(int day) {
        return Math.max(1, Math.min(31, day));
    }
}
