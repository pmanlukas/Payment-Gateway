"""
Generate realistic merchant data for MegaBank Payment Gateway

This script generates synthetic merchant/business records.
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
Faker.seed(42)
random.seed(42)


class MerchantGenerator:
    """Generate synthetic merchant data."""

    # Realistic merchant categories
    MERCHANT_CATEGORIES = [
        'Retail - Electronics',
        'Retail - Clothing',
        'Retail - Grocery',
        'Retail - Home & Garden',
        'Restaurant - Fast Food',
        'Restaurant - Fine Dining',
        'Restaurant - Cafe',
        'E-commerce - General',
        'E-commerce - Specialty',
        'Travel - Airlines',
        'Travel - Hotels',
        'Entertainment - Movies',
        'Entertainment - Gaming',
        'Services - Professional',
        'Services - Healthcare',
        'Subscription Services',
        'Digital Goods',
        'Gas Stations',
        'Department Stores',
        'Specialty Retail'
    ]

    def __init__(self):
        self.db = get_db()

    @staticmethod
    def generate_api_key():
        """Generate a random API key."""
        random_string = f"{fake.uuid4()}{datetime.now().timestamp()}"
        return hashlib.sha256(random_string.encode()).hexdigest()

    @staticmethod
    def generate_secret_key():
        """Generate a random secret key."""
        random_string = f"{fake.uuid4()}{datetime.now().timestamp()}{random.random()}"
        return hashlib.sha256(random_string.encode()).hexdigest()

    def generate_merchant(self, merchant_id=None):
        """
        Generate a single merchant record.

        Args:
            merchant_id: Optional specific merchant ID

        Returns:
            Dictionary with merchant data
        """
        if merchant_id is None:
            merchant_id = f"MERCH_{fake.unique.random_number(digits=6, fix_len=True)}"

        # Generate business name
        company_name = fake.company()

        # Fee structure varies by business type
        fee_percentage = random.choice([1.5, 1.75, 2.0, 2.25, 2.5, 2.75, 3.0, 3.5])

        # Settlement timing (days)
        settlement_days = random.choice([1, 2, 3, 5, 7])

        merchant = {
            'merchant_id': merchant_id,
            'name': company_name,
            'category': random.choice(self.MERCHANT_CATEGORIES),
            'api_key': self.generate_api_key(),
            'secret_key': self.generate_secret_key(),
            'webhook_url': f"https://{company_name.lower().replace(' ', '').replace(',', '')}.example.com/webhook",
            'fee_percentage': fee_percentage,
            'settlement_account': fake.iban(),
            'settlement_days': settlement_days,
            'contact_name': fake.name(),
            'contact_email': fake.company_email(),
            'contact_phone': fake.phone_number()[:20],
            'address': fake.street_address()[:100],
            'city': fake.city()[:50],
            'state': fake.state_abbr(),
            'zip_code': fake.zipcode(),
            'country': 'US',
            'is_active': random.random() < 0.95,  # 95% active
            'monthly_volume_limit': random.choice([50000, 100000, 250000, 500000, 1000000]),
            'created_date': (datetime.now() - timedelta(days=random.randint(30, 1095))).strftime('%Y-%m-%d %H:%M:%S'),
            'notes': None
        }

        return merchant

    def insert_merchant(self, merchant_data):
        """Insert merchant into database."""
        columns = ', '.join(merchant_data.keys())
        placeholders = ', '.join([f":{key}" for key in merchant_data.keys()])
        sql = f"INSERT INTO merchants ({columns}) VALUES ({placeholders})"

        try:
            self.db.execute_raw_sql(sql, merchant_data)
            return True
        except Exception as e:
            logger.error(f"Error inserting merchant {merchant_data['merchant_id']}: {e}")
            return False

    def generate_and_insert_merchants(self, count=100):
        """
        Generate and insert multiple merchants.

        Args:
            count: Number of merchants to generate

        Returns:
            Number of merchants successfully inserted
        """
        logger.info(f"Generating {count} merchants...")

        inserted = 0

        for i in range(count):
            merchant = self.generate_merchant()

            if self.insert_merchant(merchant):
                inserted += 1

            if (i + 1) % 25 == 0:
                logger.info(f"Progress: {i + 1}/{count} merchants generated")

        logger.info(f"Successfully inserted {inserted}/{count} merchants")
        return inserted


def main():
    """Main execution function."""
    import argparse

    parser = argparse.ArgumentParser(description='Generate synthetic merchant data')
    parser.add_argument('--count', type=int, default=100, help='Number of merchants to generate')
    args = parser.parse_args()

    generator = MerchantGenerator()

    logger.info("=" * 60)
    logger.info("MegaBank Merchant Data Generator")
    logger.info("=" * 60)

    # Check current merchant count
    current_count = generator.db.get_row_count('merchants')
    logger.info(f"Current merchants in database: {current_count}")

    # Generate merchants
    inserted = generator.generate_and_insert_merchants(args.count)

    # Report final count
    final_count = generator.db.get_row_count('merchants')
    logger.info(f"Final merchant count: {final_count}")
    logger.info("=" * 60)


if __name__ == "__main__":
    main()
