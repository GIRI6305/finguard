package com.finguard.dto;

public class TransactionResponse {
    private String transactionId;
    private String status;

    public TransactionResponse() {}

    public TransactionResponse(String transactionId, String status) {
        this.transactionId = transactionId;
        this.status = status;
    }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
