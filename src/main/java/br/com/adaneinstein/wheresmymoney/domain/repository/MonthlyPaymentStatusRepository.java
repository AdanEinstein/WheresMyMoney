package br.com.adaneinstein.wheresmymoney.domain.repository;

import br.com.adaneinstein.wheresmymoney.domain.model.MonthlyPaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MonthlyPaymentStatusRepository extends JpaRepository<MonthlyPaymentStatus, Long> {

    List<MonthlyPaymentStatus> findByYearAndMonth(int year, int month);

    Optional<MonthlyPaymentStatus> findByMonthlyPaymentIdAndYearAndMonth(Long monthlyPaymentId, int year, int month);

    List<MonthlyPaymentStatus> findByMonthlyPaymentId(Long monthlyPaymentId);
}
