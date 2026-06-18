package br.com.adaneinstein.wheresmymoney.domain.repository;

import br.com.adaneinstein.wheresmymoney.domain.model.MonthlyPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MonthlyPaymentRepository extends JpaRepository<MonthlyPayment, Long> {

    List<MonthlyPayment> findByActiveTrueOrderByDueDayAscDescriptionAsc();
}
