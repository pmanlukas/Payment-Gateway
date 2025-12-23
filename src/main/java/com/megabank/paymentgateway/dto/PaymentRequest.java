package com.megabank.paymentgateway.dto;

import java.io.Serializable;

/**
 * Payment Request DTO
 *
 * Used for incoming payment requests. Validation? What's that?
 */
public class PaymentRequest implements Serializable {

    // All fields public for "convenience"
    public String cardNumber;
    public String cvv;
    public String expiryMonth;
    public String expiryYear;
    public String cardholderName;
    public double amount;  // double for money - precision issues!
    public String currency;
    public String merchantId;
    public String orderId;
    public String customerId;
    public String customerEmail;
    public String customerIp;
    public String description;

    // Billing address
    public String billingStreet;
    public String billingCity;
    public String billingState;
    public String billingZip;
    public String billingCountry;

    // 3D Secure (never implemented)
    public boolean use3DSecure;
    public String threeDSecureToken;

    // Default constructor
    public PaymentRequest() {
    }

    // Constructor with all fields - way too many parameters!
    public PaymentRequest(String cardNumber, String cvv, String expiryMonth, String expiryYear,
                         String cardholderName, double amount, String currency, String merchantId,
                         String orderId, String customerId, String customerEmail, String customerIp,
                         String description, String billingStreet, String billingCity,
                         String billingState, String billingZip, String billingCountry) {
        this.cardNumber = cardNumber;
        this.cvv = cvv;
        this.expiryMonth = expiryMonth;
        this.expiryYear = expiryYear;
        this.cardholderName = cardholderName;
        this.amount = amount;
        this.currency = currency;
        this.merchantId = merchantId;
        this.orderId = orderId;
        this.customerId = customerId;
        this.customerEmail = customerEmail;
        this.customerIp = customerIp;
        this.description = description;
        this.billingStreet = billingStreet;
        this.billingCity = billingCity;
        this.billingState = billingState;
        this.billingZip = billingZip;
        this.billingCountry = billingCountry;
    }

    // No validation whatsoever!

    // toString exposes all sensitive data
    @Override
    public String toString() {
        return "PaymentRequest{" +
                "cardNumber='" + cardNumber + '\'' +
                ", cvv='" + cvv + '\'' +
                ", amount=" + amount +
                ", customerEmail='" + customerEmail + '\'' +
                '}';
    }

    // No equals or hashCode
}
