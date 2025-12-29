"""
Generate realistic transaction data for MegaBank Payment Gateway

This script generates synthetic transaction records with realistic patterns:
- Time-based patterns (business hours, weekends, holidays)
- Amount distributions
- Approval/decline patterns
- Fraudulent transaction patterns
- Customer behavior patterns
"""

import sys
import os
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from faker import Faker
import random
import uuid
from datetime import datetime, timedelta
from middleware.db_connector import get_db
import logging
import pandas as pd

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Initialize Faker
fake = Faker()
Faker.seed(42)
random.seed(42)


class TransactionGenerator:
    """Generate synthetic transaction data with realistic patterns."""

    def __init__(self):
        self.db = get_db()
        self.customers = None
        self.merchants = None
        self.blacklisted_cards = set()

    def load_reference_data(self):
        """Load customers and merchants from database."""
        logger.info("Loading reference data (customers and merchants)...")

        # Load customers
        self.customers = self.db.execute_query("SELECT * FROM customers")
        if len(self.customers) == 0:
            raise ValueError("No customers found in database. Run generate_customers.py first.")

        # Load merchants
        self.merchants = self.db.execute_query("SELECT * FROM merchants")
        if len(self.merchants) == 0:
            raise ValueError("No merchants found in database. Run generate_merchants.py first.")

        # Load blacklisted cards
        blacklisted = self.db.execute_query("SELECT card_number FROM blacklisted_cards")
        self.blacklisted_cards = set(blacklisted['card_number'].tolist())

        logger.info(f"Loaded {len(self.customers)} customers and {len(self.merchants)} merchants")

    @staticmethod
    def generate_transaction_amount(merchant_category):
        """
        Generate realistic transaction amount based on merchant category.

        Args:
            merchant_category: Type of merchant

        Returns:
            Transaction amount (float)
        """
        # Different categories have different typical amounts
        amount_ranges = {
            'Restaurant - Fast Food': (5, 50),
            'Restaurant - Fine Dining': (50, 300),
            'Restaurant - Cafe': (3, 25),
            'Retail - Grocery': (20, 200),
            'Retail - Electronics': (100, 2000),
            'Retail - Clothing': (30, 300),
            'Gas Stations': (20, 100),
            'E-commerce - General': (15, 500),
            'Travel - Hotels': (100, 800),
            'Travel - Airlines': (200, 1500),
            'Subscription Services': (5, 100),
        }

        # Get range or default
        min_amt, max_amt = amount_ranges.get(merchant_category, (10, 500))

        # Use log-normal distribution for realistic amount spread
        # Most transactions are low, few are high
        base_amount = random.uniform(min_amt, max_amt)

        # Occasional high-value transactions
        if random.random() < 0.05:  # 5% chance of high-value
            base_amount *= random.uniform(2, 5)

        # Round to 2 decimal places
        return round(base_amount, 2)

    @staticmethod
    def generate_transaction_timestamp(start_date, end_date):
        """
        Generate realistic transaction timestamp.
        More transactions during business hours, weekdays.

        Args:
            start_date: Start of time range
            end_date: End of time range

        Returns:
            datetime object
        """
        # Random day between start and end
        time_between = end_date - start_date
        random_days = random.randint(0, time_between.days)
        transaction_date = start_date + timedelta(days=random_days)

        # Weight towards weekdays vs weekends (80% weekday, 20% weekend)
        if transaction_date.weekday() >= 5:  # Weekend
            if random.random() > 0.3:  # 70% chance to move to weekday
                days_to_add = (7 - transaction_date.weekday()) % 7
                transaction_date += timedelta(days=days_to_add)

        # Business hours (8 AM - 10 PM) are more common
        # Weight distribution: 10% before 8 AM, 70% 8 AM - 10 PM, 20% after 10 PM
        rand_hour = random.random()
        if rand_hour < 0.10:
            hour = random.randint(0, 7)  # Late night / early morning
        elif rand_hour < 0.80:
            hour = random.randint(8, 21)  # Business hours
        else:
            hour = random.randint(22, 23)  # Late evening

        minute = random.randint(0, 59)
        second = random.randint(0, 59)

        return transaction_date.replace(hour=hour, minute=minute, second=second)

    def determine_transaction_status(self, customer, merchant, amount, is_fraud_attempt=False):
        """
        Determine if transaction should be approved, declined, or pending.

        Args:
            customer: Customer record (dict)
            merchant: Merchant record (dict)
            amount: Transaction amount
            is_fraud_attempt: Whether this is a fraudulent transaction

        Returns:
            Tuple of (status_code, risk_score, decline_reason)
            Status: 0=pending, 1=approved, 2=declined
        """
        risk_score = 0
        decline_reason = None

        # Check blacklist
        if customer['card_number'] in self.blacklisted_cards:
            return 2, 100, "Blacklisted card"

        # Fraud attempts have high chance of decline
        if is_fraud_attempt:
            risk_score += 50
            if random.random() < 0.7:  # 70% of fraud attempts are caught
                return 2, risk_score + 30, "Suspected fraud"

        # Risk level affects approval
        risk_score += customer['risk_level'] * 15

        # High amounts increase risk
        if amount > 5000:
            risk_score += 20
        elif amount > 2000:
            risk_score += 10

        # Check credit limit (if available)
        if amount > customer.get('credit_limit', 50000):
            return 2, risk_score, "Exceeds credit limit"

        # Random declines (represent insufficient funds, expired cards, etc.)
        if random.random() < 0.12:  # 12% decline rate
            decline_reasons = [
                "Insufficient funds",
                "Card expired",
                "Incorrect CVV",
                "Card restricted",
                "Do not honor"
            ]
            return 2, risk_score, random.choice(decline_reasons)

        # Small chance of pending status
        if random.random() < 0.03:  # 3% pending
            return 0, risk_score, None

        # Approved
        return 1, risk_score, None

    def generate_transaction(self, start_date, end_date, fraud_rate=0.02):
        """
        Generate a single transaction.

        Args:
            start_date: Earliest transaction date
            end_date: Latest transaction date
            fraud_rate: Probability of generating fraudulent transaction

        Returns:
            Dictionary with transaction data
        """
        # Select random customer and merchant
        customer = self.customers.sample(n=1).iloc[0]
        merchant = self.merchants.sample(n=1).iloc[0]

        # Determine if this is a fraud attempt
        is_fraud = random.random() < fraud_rate

        # Generate amount
        amount = self.generate_transaction_amount(merchant['category'])

        # Fraud transactions often have unusual amounts
        if is_fraud:
            if random.random() < 0.5:
                amount = random.uniform(1000, 10000)  # High amount fraud
            else:
                amount = random.uniform(0.01, 5.00)  # Testing fraud

        # Generate timestamp
        timestamp = self.generate_transaction_timestamp(start_date, end_date)

        # Determine status
        status, risk_score, decline_reason = self.determine_transaction_status(
            customer, merchant, amount, is_fraud
        )

        # Generate IP address (realistic distribution)
        ip_address = fake.ipv4()

        transaction = {
            'id': str(uuid.uuid4()),
            'card_number': customer['card_number'],
            'cvv': customer['cvv'],
            'expiry_date': customer['card_expiry'],
            'amount': amount,
            'currency': 'USD',
            'status': status,
            'merchant_id': merchant['merchant_id'],
            'merchant_name': merchant['name'],
            'customer_id': customer['customer_id'],
            'customer_email': customer['email'],
            'customer_ssn': customer['ssn'],
            'ip_address': ip_address,
            'risk_score': risk_score,
            'fraud_detected': is_fraud and status == 2,
            'decline_reason': decline_reason,
            'authorization_code': fake.bothify(text='AUTH######') if status == 1 else None,
            'created_date': timestamp.strftime('%Y-%m-%d %H:%M:%S'),
            'notes': 'Suspicious pattern detected' if is_fraud else None
        }

        return transaction

    def insert_transaction(self, transaction_data):
        """Insert transaction into database."""
        columns = ', '.join(transaction_data.keys())
        placeholders = ', '.join([f":{key}" for key in transaction_data.keys()])
        sql = f"INSERT INTO transactions ({columns}) VALUES ({placeholders})"

        try:
            self.db.execute_raw_sql(sql, transaction_data)
            return True
        except Exception as e:
            logger.error(f"Error inserting transaction {transaction_data['id']}: {e}")
            return False

    def generate_and_insert_transactions(
        self,
        count=10000,
        months_back=12,
        fraud_rate=0.02
    ):
        """
        Generate and insert multiple transactions.

        Args:
            count: Number of transactions to generate
            months_back: How many months back to generate data
            fraud_rate: Percentage of fraudulent transactions (0.0 to 1.0)

        Returns:
            Dictionary with generation statistics
        """
        logger.info(f"Generating {count} transactions over {months_back} months...")

        # Load reference data
        self.load_reference_data()

        # Define time range
        end_date = datetime.now()
        start_date = end_date - timedelta(days=months_back * 30)

        inserted = 0
        approved = 0
        declined = 0
        pending = 0
        fraud_attempts = 0

        for i in range(count):
            transaction = self.generate_transaction(start_date, end_date, fraud_rate)

            if self.insert_transaction(transaction):
                inserted += 1

                # Track statistics
                if transaction['status'] == 1:
                    approved += 1
                elif transaction['status'] == 2:
                    declined += 1
                else:
                    pending += 1

                if transaction.get('fraud_detected'):
                    fraud_attempts += 1

            if (i + 1) % 1000 == 0:
                logger.info(f"Progress: {i + 1}/{count} transactions generated")

        stats = {
            'total_inserted': inserted,
            'approved': approved,
            'declined': declined,
            'pending': pending,
            'fraud_detected': fraud_attempts,
            'approval_rate': (approved / inserted * 100) if inserted > 0 else 0
        }

        logger.info("=" * 60)
        logger.info(f"Transaction Generation Complete")
        logger.info(f"Total Inserted: {stats['total_inserted']}")
        logger.info(f"Approved: {stats['approved']} ({stats['approval_rate']:.1f}%)")
        logger.info(f"Declined: {stats['declined']}")
        logger.info(f"Pending: {stats['pending']}")
        logger.info(f"Fraud Detected: {stats['fraud_detected']}")
        logger.info("=" * 60)

        return stats


def main():
    """Main execution function."""
    import argparse

    parser = argparse.ArgumentParser(description='Generate synthetic transaction data')
    parser.add_argument('--count', type=int, default=10000, help='Number of transactions to generate')
    parser.add_argument('--months', type=int, default=12, help='Number of months of historical data')
    parser.add_argument('--fraud-rate', type=float, default=0.02, help='Fraud rate (0.0-1.0)')
    args = parser.parse_args()

    generator = TransactionGenerator()

    logger.info("=" * 60)
    logger.info("MegaBank Transaction Data Generator")
    logger.info("=" * 60)

    # Check current transaction count
    current_count = generator.db.get_row_count('transactions')
    logger.info(f"Current transactions in database: {current_count}")

    # Generate transactions
    stats = generator.generate_and_insert_transactions(
        count=args.count,
        months_back=args.months,
        fraud_rate=args.fraud_rate
    )

    # Report final count
    final_count = generator.db.get_row_count('transactions')
    logger.info(f"Final transaction count: {final_count}")


if __name__ == "__main__":
    main()
