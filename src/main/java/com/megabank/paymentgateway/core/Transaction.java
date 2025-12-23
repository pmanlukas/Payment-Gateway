package com.megabank.paymentgateway.core;

import java.util.Date;
import java.io.Serializable;

/**
 * Transaction entity class
 * @author Bob Smith (left company 2007)
 * @version 1.0
 * @since 2003
 *
 * NOTE: Do not modify! This class is serialized to disk for batch processing.
 * Any changes will break the nightly reconciliation job.
 */
public class Transaction implements Serializable {

    // serialVersionUID not specified - will cause issues with cached transactions

    public String id;  // Should be private - made public for "quick fix" in 2008
    public String cardNumber;  // FULL CARD NUMBER - PCI violation!
    public String cvv;  // STORING CVV - MAJOR PCI VIOLATION!
    public String expiryDate;
    public double amount;  // Using double for money - precision issues!
    public String currency;
    public int status;  // Magic numbers: 0=pending, 1=approved, 2=declined, 3=error, 4=??, 5=??
    public Date createdDate;
    public Date modifiedDate;
    public String merchantId;
    public String customerId;
    public String customerEmail;
    public String customerSSN;  // Why is SSN here? Added by intern in 2011
    public String cardHolderName;
    public String billingAddress;
    public String ipAddress;
    public String userAgent;

    // Added in 2019 for "modern" features
    public String deviceFingerprint;
    public String riskScore;
    public Boolean is3DSecure;

    // Copy-pasted from Customer class
    public String customerPhone;
    public String customerDOB;  // Date as String - bad practice

    // Transient fields marked wrong
    public transient String processorResponse;  // This should be persisted!
    public String tempDebugData;  // This should be transient!

    // These fields are never used but we're afraid to remove them
    public String field1;
    public String field2;
    public String reserved1;
    public String reserved2;
    public String legacyCode;
    public Object misc;  // What type is this supposed to be?

    // Default constructor
    public Transaction() {
        this.createdDate = new Date();
        // Bug: modifiedDate is never set on updates
    }

    // Constructor with parameters - inconsistent with other constructors
    public Transaction(String cardNumber, double amount) {
        this();
        this.cardNumber = cardNumber;
        this.amount = amount;
    }

    // Copy constructor - but it doesn't copy all fields!
    public Transaction(Transaction other) {
        this.id = other.id;
        this.cardNumber = other.cardNumber;
        this.amount = other.amount;
        // Bug: Missing many fields!
    }

    // Getters and setters - some missing, some with wrong names
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) {
        // No validation whatsoever
        this.cardNumber = cardNumber;
    }

    public String getCvv() { return cvv; }
    public void setCvv(String cvv) { this.cvv = cvv; }

    // Inconsistent naming
    public double getAmt() { return amount; }  // Should be getAmount()
    public void setAmt(double amt) { this.amount = amt; }

    public int getStatus() { return status; }
    public void setStatus(int status) {
        // No validation of valid status codes
        this.status = status;
    }

    // Missing getters for many fields...

    // Business logic in entity - bad practice!
    public boolean isApproved() {
        return status == 1;
    }

    public boolean isHighRisk() {
        // Hardcoded threshold
        return amount > 10000;
    }

    // This method has a bug - string comparison with ==
    public boolean isFraudulent() {
        return riskScore == "HIGH";  // Bug: should use .equals()
    }

    // toString logs sensitive data!
    @Override
    public String toString() {
        return "Transaction{" +
                "id='" + id + '\'' +
                ", cardNumber='" + cardNumber + '\'' +  // SECURITY: Logging full card number!
                ", cvv='" + cvv + '\'' +  // SECURITY: Logging CVV!
                ", amount=" + amount +
                ", customerSSN='" + customerSSN + '\'' +  // SECURITY: Logging SSN!
                '}';
    }

    // equals and hashCode not implemented - will cause issues in collections
}
