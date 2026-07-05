package com.finguard.repository;

import com.finguard.model.FraudAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FraudAlertRepository extends JpaRepository<FraudAlert, Long> {
    List<FraudAlert> findTop50ByOrderByCreatedAtDesc();
    List<FraudAlert> findTop50ByUsernameOrderByCreatedAtDesc(String username);
}
