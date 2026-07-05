package com.finguard.service;

import com.finguard.model.Transaction;
import com.finguard.model.TransactionStatus;
import com.finguard.repository.FraudAlertRepository;
import com.finguard.websocket.AlertSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the fraud-scoring engine. Runs with `mvn test` --
 * no Spring context, no database, no MySQL required, since the
 * dependencies are mocked directly.
 */
@ExtendWith(MockitoExtension.class)
class FraudDetectionServiceTest {

    @Mock
    private VelocityCheckService velocityCheckService;

    @Mock
    private FraudAlertRepository fraudAlertRepository;

    @Mock
    private AlertSocketHandler alertSocketHandler;

    @InjectMocks
    private FraudDetectionService fraudDetectionService;

    private Transaction buildTransaction(String amount, String location) {
        Transaction tx = new Transaction();
        tx.setTransactionId("test-tx-1");
        tx.setCardNumber("4111111111111111");
        tx.setAmount(new BigDecimal(amount));
        tx.setMerchant("TestMerchant");
        tx.setLocation(location);
        tx.setTimestamp(LocalDateTime.now());
        tx.setStatus(TransactionStatus.PENDING);
        tx.setUsername("testuser");
        return tx;
    }

    @Test
    void lowAmountLowVelocity_isApproved() {
        when(velocityCheckService.recordAndCount("4111111111111111")).thenReturn(1L);

        Transaction tx = buildTransaction("500", "IN");
        Transaction result = fraudDetectionService.evaluate(tx);

        assertEquals(TransactionStatus.APPROVED, result.getStatus());
        assertEquals(0.0, result.getRiskScore(), 0.01);
    }

    @Test
    void veryHighAmount_isBlocked() {
        when(velocityCheckService.recordAndCount("4111111111111111")).thenReturn(1L);

        // 10000 base + 60000 over threshold at (amount-10000)/1000 = 60 points, capped at 40
        Transaction tx = buildTransaction("70000", "IN");
        Transaction result = fraudDetectionService.evaluate(tx);

        assertEquals(40.0, result.getRiskScore(), 0.01);
        assertEquals(TransactionStatus.FLAGGED, result.getStatus());
    }

    @Test
    void highRiskLocation_addsExpectedScore() {
        when(velocityCheckService.recordAndCount("4111111111111111")).thenReturn(1L);

        Transaction tx = buildTransaction("500", "UNKNOWN");
        Transaction result = fraudDetectionService.evaluate(tx);

        assertEquals(25.0, result.getRiskScore(), 0.01);
        assertEquals(TransactionStatus.APPROVED, result.getStatus());
    }

    @Test
    void highAmountPlusHighRiskLocation_isBlocked() {
        when(velocityCheckService.recordAndCount("4111111111111111")).thenReturn(1L);

        Transaction tx = buildTransaction("60000", "UNKNOWN");
        Transaction result = fraudDetectionService.evaluate(tx);

        // amountScore = min(40, (60000-10000)/1000) = 40, + 25 location = 65
        assertEquals(65.0, result.getRiskScore(), 0.01);
        assertEquals(TransactionStatus.FLAGGED, result.getStatus());
    }

    @Test
    void highVelocity_increasesScoreProportionally() {
        when(velocityCheckService.recordAndCount("4111111111111111")).thenReturn(5L);

        Transaction tx = buildTransaction("500", "IN");
        Transaction result = fraudDetectionService.evaluate(tx);

        // velocityScore = min(30, (5-1)*10) = 30
        assertEquals(30.0, result.getRiskScore(), 0.01);
        assertEquals(TransactionStatus.FLAGGED, result.getStatus());
    }

    @Test
    void scoreNeverExceeds100() {
        when(velocityCheckService.recordAndCount("4111111111111111")).thenReturn(10L);

        Transaction tx = buildTransaction("200000", "UNKNOWN");
        Transaction result = fraudDetectionService.evaluate(tx);

        assertTrue(result.getRiskScore() <= 100.0);
        assertEquals(TransactionStatus.BLOCKED, result.getStatus());
    }
}
