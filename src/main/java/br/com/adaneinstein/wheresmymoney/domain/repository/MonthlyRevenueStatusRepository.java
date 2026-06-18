package br.com.adaneinstein.wheresmymoney.domain.repository;

import br.com.adaneinstein.wheresmymoney.domain.model.MonthlyRevenueStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MonthlyRevenueStatusRepository extends JpaRepository<MonthlyRevenueStatus, Long> {

    List<MonthlyRevenueStatus> findByYearAndMonth(int year, int month);

    Optional<MonthlyRevenueStatus> findByMonthlyRevenueIdAndYearAndMonth(Long monthlyRevenueId, int year, int month);

    List<MonthlyRevenueStatus> findByMonthlyRevenueId(Long monthlyRevenueId);
}
