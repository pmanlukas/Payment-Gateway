package com.megabank.paymentgateway.core;

import java.util.Date;
import java.util.ArrayList;
import java.util.List;

/**
 * Customer entity
 *
 * TODO: Merge with CustomerDTO, CustomerVO, CustomerBean, CustomerData classes
 * - Mike (2009)
 */
public class Customer {

    public String customerId;
    public String firstName;
    public String lastName;
    public String email;
    public String phone;
    public String ssn;  // Storing SSN in plain text!
    public String dateOfBirth;  // Should be Date type
    public String password;  // Plain text password stored here!
    public String passwordHint;  // "What is your password?" as hint option
    public String securityQuestion;
    public String securityAnswer;  // Stored in plain text
    public String pin;  // 4-digit PIN stored as string
    public String creditLimit;  // Should be numeric
    public String address1;
    public String address2;
    public String city;
    public String state;
    public String zip;
    public String country;

    // Risk assessment data
    public int riskLevel;  // 1-5, but some records have 0 or 99
    public boolean isBlacklisted;
    public String blacklistReason;
    public Date blacklistDate;

    // VIP customer handling - added in 2015
    public boolean isVIP;
    public double vipDiscount;  // Percent, but sometimes stored as decimal

    // These are duplicated from Address class
    public String billingStreet;
    public String billingCity;
    public String billingZip;
    public String shippingStreet;
    public String shippingCity;
    public String shippingZip;

    // Card data stored directly on customer - PCI violation!
    public String primaryCardNumber;
    public String primaryCardCvv;
    public String primaryCardExpiry;

    // Timestamps
    public Date createdDate;
    public Date lastLoginDate;
    public Date lastTransactionDate;
    public int failedLoginAttempts;  // Never reset!

    // Preferences stored as concatenated string (format: "key1=val1;key2=val2")
    public String preferences;

    // Raw notes field - can contain anything
    public String notes;

    // Internal tracking
    public String createdBy;
    public String modifiedBy;
    public String internalCode;  // Nobody knows what this is for

    // Collection with raw type
    public List transactions = new ArrayList();  // Raw type - no generics

    // Default constructor
    public Customer() {
    }

    // Constructor that sets ID
    public Customer(String id) {
        this.customerId = id;
    }

    // Utility method in entity - bad practice
    public String getFullName() {
        return firstName + " " + lastName;
    }

    // Business logic in entity
    public boolean canMakeTransaction(double amount) {
        try {
            double limit = Double.parseDouble(creditLimit);
            return amount <= limit;
        } catch (NumberFormatException e) {
            // Swallowing exception
            return false;
        }
    }

    // GDPR violation: no way to export/delete customer data

    // Validation logic in entity - and it's incomplete
    public boolean isValid() {
        return email != null && email.contains("@");
        // Missing validation for SSN format, phone, etc.
    }

    // Password check in entity - should be in service layer
    public boolean checkPassword(String inputPassword) {
        return password.equals(inputPassword);  // Plain text comparison!
    }

    // toString with sensitive data
    @Override
    public String toString() {
        return "Customer{" +
                "customerId='" + customerId + '\'' +
                ", email='" + email + '\'' +
                ", ssn='" + ssn + '\'' +  // Logging SSN!
                ", password='" + password + '\'' +  // Logging password!
                ", primaryCardNumber='" + primaryCardNumber + '\'' +  // Logging card!
                '}';
    }
}
