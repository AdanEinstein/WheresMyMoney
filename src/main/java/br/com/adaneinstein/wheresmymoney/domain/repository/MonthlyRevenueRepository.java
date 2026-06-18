package br.com.adaneinstein.wheresmymoney.domain.repository;

import br.com.adaneinstein.wheresmymoney.domain.model.MonthlyRevenue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MonthlyRevenueRepository extends JpaRepository<MonthlyRevenue, Long> {

    List<MonthlyRevenue> findByActiveTrueOrderByDueDayAscDescriptionAsc();
}
