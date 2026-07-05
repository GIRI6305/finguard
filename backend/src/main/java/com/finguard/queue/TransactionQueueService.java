package com.finguard.queue;

import com.finguard.model.Transaction;
import com.finguard.repository.TransactionRepository;
import com.finguard.service.FraudDetectionService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Simulates an event-streaming pipeline (the role Kafka would play) using
 * Java's own concurrency primitives: a BlockingQueue feeding a dedicated
 * consumer thread. This decouples "accept the transaction" (fast, returns
 * to the caller immediately) from "score it for fraud" (the slower work),
 * exactly like a producer/consumer message queue would -- just in-process,
 * so no external broker is required to run this project.
 */
@Component
public class TransactionQueueService {

    private final BlockingQueue<Transaction> queue = new LinkedBlockingQueue<>();
    private volatile boolean running = true;
    private Thread consumerThread;

    @Autowired
    private FraudDetectionService fraudDetectionService;

    @Autowired
    private TransactionRepository transactionRepository;

    @PostConstruct
    public void startConsumer() {
        consumerThread = new Thread(this::consumeLoop, "transaction-consumer");
        consumerThread.setDaemon(true);
        consumerThread.start();
    }

    @PreDestroy
    public void stopConsumer() {
        running = false;
        if (consumerThread != null) {
            consumerThread.interrupt();
        }
    }

    /** Producer side: called by the REST controller to enqueue a new transaction. */
    public void publish(Transaction transaction) {
        queue.offer(transaction);
    }

    /** Consumer side: runs continuously on its own thread, scoring and saving transactions. */
    private void consumeLoop() {
        while (running) {
            try {
                Transaction tx = queue.take();
                fraudDetectionService.evaluate(tx);
                transactionRepository.save(tx);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                System.err.println("Failed to process transaction: " + e.getMessage());
            }
        }
    }
}
