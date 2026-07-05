package com.finguard.repository;

import com.finguard.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findTop50ByOrderByTimestampDesc();
    List<Transaction> findTop50ByUsernameOrderByTimestampDesc(String username);
}
