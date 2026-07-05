package com.finguard.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class TransactionRequest {

    @NotBlank(message = "Card number is required")
    @Pattern(regexp = "\\d{13,19}", message = "Card number must be 13-19 digits")
    private String cardNumber;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    @Digits(integer = 15, fraction = 2, message = "Amount format is invalid")
    private BigDecimal amount;

    @NotBlank(message = "Merchant is required")
    @Size(max = 100, message = "Merchant name is too long")
    private String merchant;

    @NotBlank(message = "Location is required")
    @Size(max = 100, message = "Location is too long")
    private String location;

    public TransactionRequest() {}

    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getMerchant() { return merchant; }
    public void setMerchant(String merchant) { this.merchant = merchant; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}
