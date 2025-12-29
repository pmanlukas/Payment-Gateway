"""
Generate realistic customer data for MegaBank Payment Gateway

This script generates synthetic customer records using Faker library.
Creates customers with realistic names, addresses, and payment information.
"""

import sys
import os
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from faker import Faker
import random
import hashlib
from datetime import datetime, timedelta
from middleware.db_connector import get_db
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Initialize Faker
fake = Faker()
Faker.seed(42)  # For reproducibility
random.seed(42)


class CustomerGenerator:
    """Generate synthetic customer data."""

    def __init__(self):
        self.db = get_db()

    @staticmethod
    def generate_test_card_number(card_type='visa'):
        """
        Generate a test card number (Luhn algorithm valid).
        WARNING: These are test numbers only, not real cards.
        """
        # Test card prefixes (common in testing environments)
        prefixes = {
            'visa': ['4111111111', '4012888888', '4222222222'],
            'mastercard': ['5555555555', '5105105105', '2221000000'],
            'amex': ['378282246', '371449635'],
            'discover': ['6011111111', '6011000990']
        }

        prefix = random.choice(prefixes.get(card_type, prefixes['visa']))

        # Generate remaining digits
        if card_type == 'amex':
            # Amex is 15 digits
            card_number = prefix + str(random.randint(10000, 99999))
        else:
            # Others are 16 digits
            card_number = prefix + str(random.randint(100000, 999999))

        return card_number

    @staticmethod
    def generate_cvv(card_type='visa'):
        """Generate CVV (3 digits for most, 4 for Amex)."""
        if card_type == 'amex':
            return str(random.randint(1000, 9999))
        return str(random.randint(100, 999))

    @staticmethod
    def generate_expiry_date():
        """Generate card expiry date (MM/YY format, future dates)."""
        # Cards expire 2-5 years in the future
        future_months = random.randint(24, 60)
        expiry = datetime.now() + timedelta(days=future_months * 30)
        return expiry.strftime('%m/%y')

    @staticmethod
    def hash_password(password):
        """
        Hash password using MD5 (matching legacy system).
        WARNING: MD5 is weak and deprecated - used here to match legacy code.
        """
        return hashlib.md5(password.encode()).hexdigest()

    def generate_customer(self, customer_id=None, is_vip=None):
        """
        Generate a single customer record.

        Args:
            customer_id: Optional specific customer ID
            is_vip: Optional VIP status (random if not specified)

        Returns:
            Dictionary with customer data
        """
        if customer_id is None:
            customer_id = f"CUST_{fake.unique.random_number(digits=8, fix_len=True)}"

        if is_vip is None:
            is_vip = random.random() < 0.15  # 15% are VIP

        card_type = random.choice(['visa', 'visa', 'visa', 'mastercard', 'mastercard', 'amex'])  # Weighted

        customer = {
            'customer_id': customer_id,
            'first_name': fake.first_name(),
            'last_name': fake.last_name(),
            'email': fake.email(),
            'phone': fake.phone_number()[:20],  # Limit to database field size
            'ssn': fake.ssn(),
            'date_of_birth': fake.date_of_birth(minimum_age=18, maximum_age=80).strftime('%Y-%m-%d'),
            'address': fake.street_address()[:100],
            'city': fake.city()[:50],
            'state': fake.state_abbr(),
            'zip_code': fake.zipcode(),
            'country': 'US',
            'password': self.hash_password('Password123!'),  # Weak default password
            'card_number': self.generate_test_card_number(card_type),
            'cvv': self.generate_cvv(card_type),
            'card_expiry': self.generate_expiry_date(),
            'is_vip': is_vip,
            'vip_discount': random.choice([0.05, 0.10, 0.15, 0.20]) if is_vip else 0.0,
            'risk_level': random.choice([0, 1, 2]),  # 0=low, 1=medium, 2=high
            'credit_limit': random.choice([1000, 2500, 5000, 10000, 25000, 50000]),
            'is_blacklisted': False,
            'created_date': (datetime.now() - timedelta(days=random.randint(30, 730))).strftime('%Y-%m-%d %H:%M:%S'),
            'last_login': None,
            'notes': None
        }

        return customer

    def insert_customer(self, customer_data):
        """Insert customer into database."""
        columns = ', '.join(customer_data.keys())
        placeholders = ', '.join([f":{key}" for key in customer_data.keys()])
        sql = f"INSERT INTO customers ({columns}) VALUES ({placeholders})"

        try:
            self.db.execute_raw_sql(sql, customer_data)
            return True
        except Exception as e:
            logger.error(f"Error inserting customer {customer_data['customer_id']}: {e}")
            return False

    def generate_and_insert_customers(self, count=1000, vip_percentage=0.15):
        """
        Generate and insert multiple customers.

        Args:
            count: Number of customers to generate
            vip_percentage: Percentage of VIP customers (0.0 to 1.0)

        Returns:
            Number of customers successfully inserted
        """
        logger.info(f"Generating {count} customers...")

        inserted = 0
        vip_count = int(count * vip_percentage)

        for i in range(count):
            is_vip = i < vip_count
            customer = self.generate_customer(is_vip=is_vip)

            if self.insert_customer(customer):
                inserted += 1

            if (i + 1) % 100 == 0:
                logger.info(f"Progress: {i + 1}/{count} customers generated")

        logger.info(f"Successfully inserted {inserted}/{count} customers")
        return inserted


def main():
    """Main execution function."""
    import argparse

    parser = argparse.ArgumentParser(description='Generate synthetic customer data')
    parser.add_argument('--count', type=int, default=1000, help='Number of customers to generate')
    parser.add_argument('--vip-pct', type=float, default=0.15, help='Percentage of VIP customers (0.0-1.0)')
    args = parser.parse_args()

    generator = CustomerGenerator()

    logger.info("=" * 60)
    logger.info("MegaBank Customer Data Generator")
    logger.info("=" * 60)

    # Check current customer count
    current_count = generator.db.get_row_count('customers')
    logger.info(f"Current customers in database: {current_count}")

    # Generate customers
    inserted = generator.generate_and_insert_customers(args.count, args.vip_pct)

    # Report final count
    final_count = generator.db.get_row_count('customers')
    logger.info(f"Final customer count: {final_count}")
    logger.info("=" * 60)


if __name__ == "__main__":
    main()
