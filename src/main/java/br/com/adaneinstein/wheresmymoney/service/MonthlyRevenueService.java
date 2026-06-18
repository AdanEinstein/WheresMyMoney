package br.com.adaneinstein.wheresmymoney.service;

import br.com.adaneinstein.wheresmymoney.domain.model.Category;
import br.com.adaneinstein.wheresmymoney.domain.model.MonthlyRevenue;
import br.com.adaneinstein.wheresmymoney.domain.model.MonthlyRevenueStatus;
import br.com.adaneinstein.wheresmymoney.domain.model.Subcategory;
import br.com.adaneinstein.wheresmymoney.domain.model.Transaction;
import br.com.adaneinstein.wheresmymoney.domain.repository.MonthlyRevenueRepository;
import br.com.adaneinstein.wheresmymoney.domain.repository.MonthlyRevenueStatusRepository;
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
 * Gerencia as receitas previstas mensais ({@link MonthlyRevenue}) e o checklist do
 * que já foi recebido em cada mês. Ao marcar como recebida, lança a transação correspondente
 * via {@link TransactionService}; ao desmarcar, remove essa transação.
 */
@Service
@RequiredArgsConstructor
public class MonthlyRevenueService {

    private final MonthlyRevenueRepository revenueRepository;
    private final MonthlyRevenueStatusRepository statusRepository;
    private final TransactionService transactionService;

    /** Linha do checklist: o template, se já foi recebida no mês e a transação vinculada. */
    public record MonthlyRevenueView(MonthlyRevenue revenue, boolean received, Long transactionId) {}

    /** Totais do mês: previsto (todos os ativos) e recebido (somente marcados). */
    public record Totals(BigDecimal expected, BigDecimal received) {
        public BigDecimal toReceive() {
            return expected.subtract(received);
        }
    }

    // ── CRUD de templates ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<MonthlyRevenue> listActive() {
        return revenueRepository.findByActiveTrueOrderByDueDayAscDescriptionAsc();
    }

    @Transactional
    public MonthlyRevenue create(String description, BigDecimal amount, int dueDay,
                                 Category category, Subcategory subcategory) {
        MonthlyRevenue r = new MonthlyRevenue();
        r.setDescription(description);
        r.setAmount(amount);
        r.setDueDay(clampDay(dueDay));
        r.setCategory(category);
        r.setSubcategory(subcategory);
        r.setActive(true);
        r.setCreatedAt(LocalDateTime.now());
        return revenueRepository.save(r);
    }

    @Transactional
    public MonthlyRevenue update(Long id, String description, BigDecimal amount, int dueDay,
                                 Category category, Subcategory subcategory) {
        MonthlyRevenue r = revenueRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Receita inexistente: " + id));
        r.setDescription(description);
        r.setAmount(amount);
        r.setDueDay(clampDay(dueDay));
        r.setCategory(category);
        r.setSubcategory(subcategory);
        return revenueRepository.save(r);
    }

    /** Exclui o template e seus registros de status. As transações já lançadas são preservadas. */
    @Transactional
    public void delete(Long id) {
        statusRepository.deleteAll(statusRepository.findByMonthlyRevenueId(id));
        revenueRepository.deleteById(id);
    }

    // ── Checklist do mês ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<MonthlyRevenueView> view(YearMonth ym) {
        Map<Long, MonthlyRevenueStatus> statusByRevenue = statusIndex(ym);
        List<MonthlyRevenue> revenues = revenueRepository.findByActiveTrueOrderByDueDayAscDescriptionAsc();
        return revenues.stream()
                .map(r -> {
                    MonthlyRevenueStatus st = statusByRevenue.get(r.getId());
                    Long txId = st != null && st.getTransaction() != null ? st.getTransaction().getId() : null;
                    return new MonthlyRevenueView(r, st != null, txId);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public Totals totals(YearMonth ym) {
        Map<Long, MonthlyRevenueStatus> statusByRevenue = statusIndex(ym);
        BigDecimal expected = BigDecimal.ZERO;
        BigDecimal received = BigDecimal.ZERO;
        for (MonthlyRevenue r : revenueRepository.findByActiveTrueOrderByDueDayAscDescriptionAsc()) {
            expected = expected.add(r.getAmount());
            if (statusByRevenue.containsKey(r.getId())) {
                received = received.add(r.getAmount());
            }
        }
        return new Totals(expected, received);
    }

    /**
     * Marca a receita como recebida no mês: lança a transação (tipo definido pela categoria,
     * data = hoje) e grava o status. Idempotente — não duplica se já recebida.
     */
    @Transactional
    public void markReceived(Long revenueId, YearMonth ym) {
        if (statusRepository.findByMonthlyRevenueIdAndYearAndMonth(revenueId, ym.getYear(), ym.getMonthValue())
                .isPresent()) {
            return;
        }
        MonthlyRevenue r = revenueRepository.findById(revenueId)
                .orElseThrow(() -> new IllegalArgumentException("Receita inexistente: " + revenueId));

        String notes = "Receita mensal: " + r.getDescription()
                + String.format(" (%02d/%d)", ym.getMonthValue(), ym.getYear());
        Transaction tx = transactionService.create(
                r.getDescription(), r.getAmount(), LocalDate.now(),
                r.getCategory(), r.getSubcategory(), notes);

        MonthlyRevenueStatus status = new MonthlyRevenueStatus();
        status.setMonthlyRevenue(r);
        status.setYear(ym.getYear());
        status.setMonth(ym.getMonthValue());
        status.setTransaction(tx);
        status.setReceivedAt(LocalDateTime.now());
        statusRepository.save(status);
    }

    /** Desmarca: remove o status do mês e a transação vinculada (se existir). */
    @Transactional
    public void unmarkReceived(Long revenueId, YearMonth ym) {
        statusRepository.findByMonthlyRevenueIdAndYearAndMonth(revenueId, ym.getYear(), ym.getMonthValue())
                .ifPresent(status -> {
                    Long txId = status.getTransaction() != null ? status.getTransaction().getId() : null;
                    statusRepository.delete(status);
                    if (txId != null) {
                        transactionService.delete(txId);
                    }
                });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    private Map<Long, MonthlyRevenueStatus> statusIndex(YearMonth ym) {
        Map<Long, MonthlyRevenueStatus> map = new HashMap<>();
        for (MonthlyRevenueStatus st : statusRepository.findByYearAndMonth(ym.getYear(), ym.getMonthValue())) {
            map.put(st.getMonthlyRevenue().getId(), st);
        }
        return map;
    }

    private static int clampDay(int day) {
        return Math.max(1, Math.min(31, day));
    }
}
