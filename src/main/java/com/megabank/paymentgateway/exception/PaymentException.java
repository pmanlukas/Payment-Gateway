package com.megabank.paymentgateway.exception;

/**
 * Custom exception for payment processing errors
 *
 * This exception was supposed to be the base for a hierarchy
 * of specific exceptions, but we never got around to it.
 *
 * @author Bob (2003)
 */
public class PaymentException extends RuntimeException {

    // Error codes - some are duplicates, some are unused
    public static final int ERROR_UNKNOWN = 0;
    public static final int ERROR_INVALID_CARD = 1;
    public static final int ERROR_EXPIRED_CARD = 2;
    public static final int ERROR_INSUFFICIENT_FUNDS = 3;
    public static final int ERROR_DECLINED = 4;
    public static final int ERROR_FRAUD = 5;
    public static final int ERROR_NETWORK = 6;
    public static final int ERROR_DATABASE = 7;
    public static final int ERROR_TIMEOUT = 8;
    public static final int ERROR_DUPLICATE = 9;
    public static final int ERROR_INVALID_AMOUNT = 10;
    public static final int ERROR_INVALID_CURRENCY = 11;
    public static final int ERROR_MERCHANT_DISABLED = 12;
    public static final int ERROR_RATE_LIMITED = 13;
    public static final int ERROR_CARD_NOT_FOUND = 1;  // Duplicate of ERROR_INVALID_CARD!
    public static final int ERROR_PROCESSING = 99;

    private int errorCode;
    private String cardNumber;  // Storing card number in exception!
    private String cvv;  // Storing CVV in exception!
    private double amount;

    // Default constructor
    public PaymentException() {
        super();
        this.errorCode = ERROR_UNKNOWN;
    }

    // Constructor with message
    public PaymentException(String message) {
        super(message);
        this.errorCode = ERROR_UNKNOWN;
    }

    // Constructor with code and message
    public PaymentException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    // Constructor that logs sensitive data!
    public PaymentException(String message, String cardNumber, String cvv, double amount) {
        super(message + " - Card: " + cardNumber + ", CVV: " + cvv + ", Amount: " + amount);
        this.cardNumber = cardNumber;
        this.cvv = cvv;
        this.amount = amount;

        // Log the exception with sensitive data!
        System.out.println("PaymentException created: " + this.getMessage());
    }

    // Constructor with cause
    public PaymentException(String message, Throwable cause) {
        super(message, cause);
        // Logs full stack trace including any sensitive data
        cause.printStackTrace();
    }

    // Getters that expose sensitive data
    public int getErrorCode() {
        return errorCode;
    }

    public String getCardNumber() {
        return cardNumber;  // Returns full card number!
    }

    public String getCvv() {
        return cvv;  // Returns CVV!
    }

    public double getAmount() {
        return amount;
    }

    // toString includes all sensitive data!
    @Override
    public String toString() {
        return "PaymentException{" +
                "errorCode=" + errorCode +
                ", message='" + getMessage() + '\'' +
                ", cardNumber='" + cardNumber + '\'' +
                ", cvv='" + cvv + '\'' +
                ", amount=" + amount +
                '}';
    }

    // Helper to get user-friendly message - but doesn't hide technical details
    public String getUserMessage() {
        switch (errorCode) {
            case ERROR_INVALID_CARD:
                return "Invalid card number: " + cardNumber;  // Exposes card!
            case ERROR_DECLINED:
                return "Transaction declined for card: " + cardNumber;
            case ERROR_FRAUD:
                return "Fraud detected on card: " + cardNumber + " - investigation pending";
            default:
                return "Error processing payment: " + getMessage();
        }
    }
}
