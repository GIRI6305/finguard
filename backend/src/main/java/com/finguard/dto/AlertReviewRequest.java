package com.finguard.dto;

import jakarta.validation.constraints.NotBlank;

public class AlertReviewRequest {
    @NotBlank
    private String status;

    public AlertReviewRequest() {}

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
