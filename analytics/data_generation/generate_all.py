"""
Master script to generate all sample data for MegaBank Payment Gateway

This script orchestrates the generation of:
1. Merchants (businesses accepting payments)
2. Customers (people making payments)
3. Transactions (payment records)

Usage:
    python generate_all.py --preset small    # 100 customers, 1K transactions
    python generate_all.py --preset medium   # 1K customers, 10K transactions
    python generate_all.py --preset large    # 10K customers, 100K transactions
    python generate_all.py --custom --customers 5000 --merchants 200 --transactions 50000
"""

import sys
import os
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import argparse
import logging
from datetime import datetime
from middleware.db_connector import get_db
from generate_merchants import MerchantGenerator
from generate_customers import CustomerGenerator
from generate_transactions import TransactionGenerator

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


# Predefined presets for different use cases
PRESETS = {
    'demo': {
        'name': 'Demo (Quick Test)',
        'merchants': 10,
        'customers': 50,
        'transactions': 200,
        'months_back': 3
    },
    'small': {
        'name': 'Small Dataset',
        'merchants': 50,
        'customers': 500,
        'transactions': 2000,
        'months_back': 6
    },
    'medium': {
        'name': 'Medium Dataset (Recommended)',
        'merchants': 100,
        'customers': 2000,
        'transactions': 20000,
        'months_back': 12
    },
    'large': {
        'name': 'Large Dataset',
        'merchants': 200,
        'customers': 10000,
        'transactions': 100000,
        'months_back': 24
    },
    'xlarge': {
        'name': 'Extra Large Dataset (Warning: Slow)',
        'merchants': 500,
        'customers': 50000,
        'transactions': 500000,
        'months_back': 36
    }
}


class DataGenerationOrchestrator:
    """Orchestrates the generation of all sample data."""

    def __init__(self):
        self.db = get_db()
        self.merchant_gen = MerchantGenerator()
        self.customer_gen = CustomerGenerator()
        self.transaction_gen = TransactionGenerator()

    def check_database_connection(self):
        """Verify database connectivity."""
        try:
            tables = self.db.list_tables()
            logger.info(f"✓ Database connected: {self.db.database}")
            logger.info(f"✓ Found {len(tables)} tables: {', '.join(tables)}")
            return True
        except Exception as e:
            logger.error(f"✗ Database connection failed: {e}")
            return False

    def show_current_state(self):
        """Display current row counts."""
        logger.info("=" * 70)
        logger.info("CURRENT DATABASE STATE")
        logger.info("=" * 70)

        tables = ['merchants', 'customers', 'transactions']
        for table in tables:
            try:
                count = self.db.get_row_count(table)
                logger.info(f"  {table:20s}: {count:>10,} rows")
            except Exception as e:
                logger.error(f"  {table:20s}: Error - {e}")

        logger.info("=" * 70)

    def confirm_generation(self, config):
        """Ask user to confirm data generation."""
        logger.info("")
        logger.info("=" * 70)
        logger.info("DATA GENERATION PLAN")
        logger.info("=" * 70)
        logger.info(f"  Merchants:     {config['merchants']:>10,}")
        logger.info(f"  Customers:     {config['customers']:>10,}")
        logger.info(f"  Transactions:  {config['transactions']:>10,}")
        logger.info(f"  Time Period:   {config['months_back']:>10} months")
        logger.info(f"  Fraud Rate:    {config.get('fraud_rate', 0.02)*100:>10.1f}%")
        logger.info("=" * 70)

        # Estimate time
        estimated_minutes = (
            (config['merchants'] / 1000) +
            (config['customers'] / 500) +
            (config['transactions'] / 2000)
        )
        logger.info(f"  Estimated time: ~{estimated_minutes:.1f} minutes")
        logger.info("=" * 70)

        if not config.get('skip_confirmation', False):
            response = input("\nProceed with data generation? (yes/no): ")
            return response.lower() in ['yes', 'y']
        return True

    def generate_all_data(self, config):
        """
        Generate all sample data in the correct order.

        Args:
            config: Dictionary with generation parameters

        Returns:
            Dictionary with generation statistics
        """
        start_time = datetime.now()
        stats = {
            'merchants': 0,
            'customers': 0,
            'transactions': {},
            'duration_seconds': 0
        }

        try:
            # Step 1: Generate Merchants
            logger.info("")
            logger.info("=" * 70)
            logger.info("STEP 1/3: Generating Merchants")
            logger.info("=" * 70)
            stats['merchants'] = self.merchant_gen.generate_and_insert_merchants(
                count=config['merchants']
            )

            # Step 2: Generate Customers
            logger.info("")
            logger.info("=" * 70)
            logger.info("STEP 2/3: Generating Customers")
            logger.info("=" * 70)
            stats['customers'] = self.customer_gen.generate_and_insert_customers(
                count=config['customers'],
                vip_percentage=config.get('vip_percentage', 0.15)
            )

            # Step 3: Generate Transactions
            logger.info("")
            logger.info("=" * 70)
            logger.info("STEP 3/3: Generating Transactions")
            logger.info("=" * 70)
            stats['transactions'] = self.transaction_gen.generate_and_insert_transactions(
                count=config['transactions'],
                months_back=config['months_back'],
                fraud_rate=config.get('fraud_rate', 0.02)
            )

        except Exception as e:
            logger.error(f"Error during data generation: {e}")
            raise

        # Calculate duration
        end_time = datetime.now()
        stats['duration_seconds'] = (end_time - start_time).total_seconds()

        return stats

    def show_final_summary(self, stats):
        """Display final generation summary."""
        logger.info("")
        logger.info("=" * 70)
        logger.info("GENERATION COMPLETE!")
        logger.info("=" * 70)
        logger.info(f"  Merchants Created:     {stats['merchants']:>10,}")
        logger.info(f"  Customers Created:     {stats['customers']:>10,}")
        logger.info(f"  Transactions Created:  {stats['transactions']['total_inserted']:>10,}")
        logger.info(f"    - Approved:          {stats['transactions']['approved']:>10,}")
        logger.info(f"    - Declined:          {stats['transactions']['declined']:>10,}")
        logger.info(f"    - Pending:           {stats['transactions']['pending']:>10,}")
        logger.info(f"    - Fraud Detected:    {stats['transactions']['fraud_detected']:>10,}")
        logger.info(f"  Duration:              {stats['duration_seconds']:>10.1f} seconds")
        logger.info("=" * 70)

        logger.info("\n✓ Sample data generation successful!")
        logger.info("  You can now run Jupyter notebooks for analysis.")
        logger.info("  Access Jupyter at: http://localhost:8888")


def main():
    """Main execution function."""
    parser = argparse.ArgumentParser(
        description='Generate sample data for MegaBank Payment Gateway',
        formatter_class=argparse.RawDescriptionHelpFormatter
    )

    # Preset or custom
    parser.add_argument(
        '--preset',
        choices=list(PRESETS.keys()),
        default='medium',
        help='Use predefined dataset size'
    )

    # Custom options
    parser.add_argument('--merchants', type=int, help='Number of merchants (overrides preset)')
    parser.add_argument('--customers', type=int, help='Number of customers (overrides preset)')
    parser.add_argument('--transactions', type=int, help='Number of transactions (overrides preset)')
    parser.add_argument('--months', type=int, help='Months of historical data (overrides preset)')
    parser.add_argument('--fraud-rate', type=float, default=0.02, help='Fraud rate (0.0-1.0)')
    parser.add_argument('--vip-pct', type=float, default=0.15, help='VIP customer percentage (0.0-1.0)')

    # Flags
    parser.add_argument('--yes', '-y', action='store_true', help='Skip confirmation prompt')
    parser.add_argument('--show-presets', action='store_true', help='Show available presets and exit')

    args = parser.parse_args()

    # Show presets if requested
    if args.show_presets:
        print("\nAvailable Presets:")
        print("=" * 70)
        for key, preset in PRESETS.items():
            print(f"\n{key.upper()}: {preset['name']}")
            print(f"  Merchants:     {preset['merchants']:>10,}")
            print(f"  Customers:     {preset['customers']:>10,}")
            print(f"  Transactions:  {preset['transactions']:>10,}")
            print(f"  Months Back:   {preset['months_back']:>10}")
        print("\n" + "=" * 70)
        return

    # Build configuration
    preset_config = PRESETS[args.preset]
    config = {
        'merchants': args.merchants or preset_config['merchants'],
        'customers': args.customers or preset_config['customers'],
        'transactions': args.transactions or preset_config['transactions'],
        'months_back': args.months or preset_config['months_back'],
        'fraud_rate': args.fraud_rate,
        'vip_percentage': args.vip_pct,
        'skip_confirmation': args.yes
    }

    # Initialize orchestrator
    orchestrator = DataGenerationOrchestrator()

    # Check database
    if not orchestrator.check_database_connection():
        logger.error("Cannot proceed without database connection")
        sys.exit(1)

    # Show current state
    orchestrator.show_current_state()

    # Confirm
    if not orchestrator.confirm_generation(config):
        logger.info("Data generation cancelled by user")
        return

    # Generate data
    logger.info("\nStarting data generation...")
    stats = orchestrator.generate_all_data(config)

    # Show summary
    orchestrator.show_final_summary(stats)


if __name__ == "__main__":
    main()
