package com.finguard.service;

import com.finguard.model.*;
import com.finguard.repository.FraudAlertRepository;
import com.finguard.websocket.AlertSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Set;

@Service
public class FraudDetectionService {

    @Autowired
    private VelocityCheckService velocityCheckService;

    @Autowired
    private FraudAlertRepository fraudAlertRepository;

    @Autowired
    private AlertSocketHandler alertSocketHandler;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private static final Set<String> HIGH_RISK_LOCATIONS = Set.of("UNKNOWN", "SANCTIONED_REGION");

    /**
     * Rule-based risk scoring with continuous scaling (not flat jumps), so scores
     * vary smoothly with how extreme the amount/velocity are, not just on/off.
     * Score bands: >=70 BLOCKED, 40-69 FLAGGED, otherwise APPROVED.
     */
    public Transaction evaluate(Transaction tx) {
        double score = 0;
        StringBuilder reasons = new StringBuilder();

        double amountValue = tx.getAmount().doubleValue();
        if (amountValue > 10000) {
            double amountScore = Math.min(40, (amountValue - 10000) / 1000.0);
            score += amountScore;
            if (amountScore >= 5) {
                reasons.append(String.format("Elevated amount (+%.0f). ", amountScore));
            }
        }

        long velocity = velocityCheckService.recordAndCount(tx.getCardNumber());
        if (velocity > 1) {
            double velocityScore = Math.min(30, (velocity - 1) * 10.0);
            score += velocityScore;
            reasons.append("High velocity (" + velocity + " tx/60s). ");
        }

        if (HIGH_RISK_LOCATIONS.contains(tx.getLocation().toUpperCase())) {
            score += 25;
            reasons.append("High-risk location. ");
        }

        if (tx.getAmount().compareTo(new BigDecimal("1000")) < 0 && velocity > 1) {
            score += 10;
            reasons.append("Repeated small-value probing pattern. ");
        }

        score = Math.min(score, 100);
        tx.setRiskScore(score);

        if (score >= 70) {
            tx.setStatus(TransactionStatus.BLOCKED);
        } else if (score >= 40) {
            tx.setStatus(TransactionStatus.FLAGGED);
        } else {
            tx.setStatus(TransactionStatus.APPROVED);
        }

        if (score >= 40) {
            FraudAlert alert = new FraudAlert();
            alert.setTransactionId(tx.getTransactionId());
            alert.setUsername(tx.getUsername());
            alert.setRiskScore(score);
            alert.setReason(reasons.length() == 0 ? "Elevated risk" : reasons.toString());
            alert.setStatus(AlertStatus.OPEN);
            alert.setCreatedAt(LocalDateTime.now());
            fraudAlertRepository.save(alert);

            broadcastAlert(alert, tx);
        }

        return tx;
    }

    private void broadcastAlert(FraudAlert alert, Transaction tx) {
        try {
            HashMap<String, Object> payload = new HashMap<>();
            payload.put("type", "FRAUD_ALERT");
            payload.put("transactionId", tx.getTransactionId());
            payload.put("riskScore", alert.getRiskScore());
            payload.put("reason", alert.getReason());
            payload.put("status", tx.getStatus().name());
            alertSocketHandler.broadcast(objectMapper.writeValueAsString(payload));
        } catch (Exception ignored) {
            // broadcasting failures should never break the fraud pipeline
        }
    }
}
