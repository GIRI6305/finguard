package com.finguard.controller;

import com.finguard.dto.TransactionRequest;
import com.finguard.dto.TransactionResponse;
import com.finguard.model.Transaction;
import com.finguard.model.TransactionStatus;
import com.finguard.queue.TransactionQueueService;
import com.finguard.repository.TransactionRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @Autowired
    private TransactionQueueService transactionQueueService;

    @Autowired
    private TransactionRepository transactionRepository;

    @PostMapping
    public ResponseEntity<TransactionResponse> submit(@Valid @RequestBody TransactionRequest request,
                                                        Authentication authentication) {
        Transaction tx = new Transaction();
        tx.setTransactionId(UUID.randomUUID().toString());
        tx.setCardNumber(request.getCardNumber());
        tx.setAmount(request.getAmount());
        tx.setMerchant(request.getMerchant());
        tx.setLocation(request.getLocation());
        tx.setTimestamp(LocalDateTime.now());
        tx.setStatus(TransactionStatus.PENDING);
        tx.setUsername(authentication.getName());

        transactionQueueService.publish(tx);

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new TransactionResponse(tx.getTransactionId(), "PENDING"));
    }

    @GetMapping
    public ResponseEntity<List<Transaction>> recent(Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN"));

        if (isAdmin) {
            return ResponseEntity.ok(transactionRepository.findTop50ByOrderByTimestampDesc());
        }
        return ResponseEntity.ok(transactionRepository.findTop50ByUsernameOrderByTimestampDesc(authentication.getName()));
    }
}
