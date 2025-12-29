"""
Data Generation Package for MegaBank Payment Gateway

This package provides synthetic data generators for:
- Merchants (businesses)
- Customers (cardholders)
- Transactions (payments)

Use generate_all.py as the main entry point.
"""

from .generate_merchants import MerchantGenerator
from .generate_customers import CustomerGenerator
from .generate_transactions import TransactionGenerator

__all__ = [
    'MerchantGenerator',
    'CustomerGenerator',
    'TransactionGenerator'
]
