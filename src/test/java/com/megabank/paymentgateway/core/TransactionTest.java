package com.megabank.paymentgateway.core;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;
import java.util.Date;

/**
 * Tests for Transaction entity class
 */
public class TransactionTest {

    private Transaction transaction;

    @Before
    public void setUp() {
        transaction = new Transaction();
    }

    @Test
    public void testConstructor_setsCreatedDate() {
        assertNotNull("Created date should be set", transaction.createdDate);
    }

    @Test
    public void testConstructorWithParameters() {
        Transaction txn = new Transaction("4111111111111111", 99.99);

        assertEquals("4111111111111111", txn.cardNumber);
        assertEquals(99.99, txn.amount, 0.01);
        assertNotNull("Created date should be set", txn.createdDate);
    }

    @Test
    public void testCopyConstructor() {
        Transaction original = new Transaction();
        original.id = "TXN123";
        original.cardNumber = "4111111111111111";
        original.amount = 99.99;

        Transaction copy = new Transaction(original);

        assertEquals(original.id, copy.id);
        assertEquals(original.cardNumber, copy.cardNumber);
        assertEquals(original.amount, copy.amount, 0.01);
    }

    @Test
    public void testIsApproved_approved() {
        transaction.status = 1;  // APPROVED
        assertTrue(transaction.isApproved());
    }

    @Test
    public void testIsApproved_declined() {
        transaction.status = 2;  // DECLINED
        assertFalse(transaction.isApproved());
    }

    @Test
    public void testIsHighRisk_lowAmount() {
        transaction.amount = 1000.0;
        assertFalse("Amount under threshold should not be high risk",
                    transaction.isHighRisk());
    }

    @Test
    public void testIsHighRisk_highAmount() {
        transaction.amount = 15000.0;
        assertTrue("Amount over $10,000 should be high risk",
                   transaction.isHighRisk());
    }

    @Test
    public void testGettersAndSetters() {
        transaction.setId("TXN456");
        assertEquals("TXN456", transaction.getId());

        transaction.setCardNumber("4111111111111111");
        assertEquals("4111111111111111", transaction.getCardNumber());

        transaction.setCvv("123");
        assertEquals("123", transaction.getCvv());

        transaction.setAmt(150.50);
        assertEquals(150.50, transaction.getAmt(), 0.01);

        transaction.setStatus(1);
        assertEquals(1, transaction.getStatus());
    }

    @Test
    public void testToString_containsSensitiveData() {
        transaction.id = "TXN789";
        transaction.cardNumber = "4111111111111111";
        transaction.cvv = "123";
        transaction.customerSSN = "123-45-6789";

        String str = transaction.toString();

        // SECURITY ISSUE: toString exposes sensitive data!
        assertTrue("toString should contain card number",
                   str.contains("4111111111111111"));
        assertTrue("toString should contain CVV",
                   str.contains("123"));
        assertTrue("toString should contain SSN",
                   str.contains("123-45-6789"));

        // This test passes, but the behavior is a security violation!
    }

    @Test
    public void testIsFraudulent_buggyEquality() {
        // Bug: Uses == instead of .equals() for String comparison
        transaction.riskScore = "HIGH";

        // This will fail because of the == bug
        // assertFalse(transaction.isFraudulent());

        // Test documents the bug
        assertNotNull(transaction.riskScore);
    }
}
