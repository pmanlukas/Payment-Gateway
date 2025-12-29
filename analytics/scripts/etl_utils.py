"""
ETL Utilities for MegaBank Payment Gateway Analytics

Helper functions for Extract, Transform, Load operations, data quality checks,
and common data transformations for banking analytics.
"""

import pandas as pd
import numpy as np
from datetime import datetime, timedelta
from typing import List, Dict, Any, Optional, Tuple
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class DataQualityChecker:
    """Data quality validation and profiling utilities."""

    @staticmethod
    def profile_dataframe(df: pd.DataFrame, name: str = "DataFrame") -> Dict[str, Any]:
        """
        Generate comprehensive data quality profile for a DataFrame.

        Args:
            df: pandas DataFrame to profile
            name: Name of the dataset

        Returns:
            Dictionary with quality metrics
        """
        profile = {
            'name': name,
            'row_count': len(df),
            'column_count': len(df.columns),
            'memory_usage_mb': df.memory_usage(deep=True).sum() / 1024**2,
            'duplicate_rows': df.duplicated().sum(),
            'columns': {}
        }

        for col in df.columns:
            col_profile = {
                'dtype': str(df[col].dtype),
                'null_count': df[col].isnull().sum(),
                'null_percentage': (df[col].isnull().sum() / len(df) * 100) if len(df) > 0 else 0,
                'unique_count': df[col].nunique(),
                'unique_percentage': (df[col].nunique() / len(df) * 100) if len(df) > 0 else 0
            }

            # Add statistics for numeric columns
            if pd.api.types.is_numeric_dtype(df[col]):
                col_profile['min'] = df[col].min()
                col_profile['max'] = df[col].max()
                col_profile['mean'] = df[col].mean()
                col_profile['median'] = df[col].median()
                col_profile['std'] = df[col].std()

            # Sample values
            col_profile['sample_values'] = df[col].dropna().head(5).tolist()

            profile['columns'][col] = col_profile

        return profile

    @staticmethod
    def check_nulls(df: pd.DataFrame, critical_columns: List[str]) -> List[str]:
        """
        Check for null values in critical columns.

        Args:
            df: DataFrame to check
            critical_columns: List of columns that should not have nulls

        Returns:
            List of columns with null violations
        """
        violations = []
        for col in critical_columns:
            if col in df.columns and df[col].isnull().any():
                null_count = df[col].isnull().sum()
                violations.append(f"{col}: {null_count} null values")
        return violations

    @staticmethod
    def check_duplicates(df: pd.DataFrame, key_columns: List[str]) -> pd.DataFrame:
        """
        Find duplicate records based on key columns.

        Args:
            df: DataFrame to check
            key_columns: Columns that should be unique

        Returns:
            DataFrame containing duplicate rows
        """
        return df[df.duplicated(subset=key_columns, keep=False)].sort_values(key_columns)

    @staticmethod
    def check_referential_integrity(
        df_child: pd.DataFrame,
        df_parent: pd.DataFrame,
        child_fk: str,
        parent_pk: str
    ) -> pd.DataFrame:
        """
        Check for orphaned records (foreign key violations).

        Args:
            df_child: Child table DataFrame
            df_parent: Parent table DataFrame
            child_fk: Foreign key column in child table
            parent_pk: Primary key column in parent table

        Returns:
            DataFrame with orphaned records
        """
        parent_keys = set(df_parent[parent_pk].dropna())
        orphaned = df_child[~df_child[child_fk].isin(parent_keys)]
        return orphaned


class TransactionAnalytics:
    """Banking-specific analytics utilities for transactions."""

    @staticmethod
    def classify_transaction_status(status_code: int) -> str:
        """
        Convert numeric status code to human-readable status.

        Status codes from legacy system:
            0 = Pending
            1 = Approved
            2 = Declined
            3 = Refunded
            4 = Cancelled
        """
        status_map = {
            0: 'Pending',
            1: 'Approved',
            2: 'Declined',
            3: 'Refunded',
            4: 'Cancelled'
        }
        return status_map.get(status_code, 'Unknown')

    @staticmethod
    def detect_card_type(card_number: str) -> str:
        """
        Detect card type from card number prefix.

        Returns:
            Card type (Visa, Mastercard, Amex, Discover, etc.)
        """
        if pd.isna(card_number):
            return 'Unknown'

        card_str = str(card_number).replace(' ', '').replace('-', '')

        if card_str.startswith('4'):
            return 'Visa'
        elif card_str.startswith(('51', '52', '53', '54', '55')):
            return 'Mastercard'
        elif card_str.startswith(('34', '37')):
            return 'Amex'
        elif card_str.startswith('6011') or card_str.startswith(('644', '645', '646', '647', '648', '649', '65')):
            return 'Discover'
        else:
            return 'Unknown'

    @staticmethod
    def calculate_approval_rate(df: pd.DataFrame, status_col: str = 'status') -> float:
        """Calculate approval rate (approved / total transactions)."""
        if len(df) == 0:
            return 0.0
        approved = (df[status_col] == 1).sum()
        return (approved / len(df)) * 100

    @staticmethod
    def detect_suspicious_patterns(df: pd.DataFrame) -> pd.DataFrame:
        """
        Flag potentially fraudulent transactions based on simple rules.

        Rules:
            - Multiple transactions from same card in short time
            - Unusually high amount
            - Multiple declined attempts
            - Unusual time patterns (late night, etc.)

        Returns:
            DataFrame with suspicious transactions and flags
        """
        suspicious = df.copy()
        suspicious['fraud_flags'] = ''

        # High amount threshold (e.g., > $5000)
        high_amount_mask = suspicious['amount'] > 5000
        suspicious.loc[high_amount_mask, 'fraud_flags'] += 'HIGH_AMOUNT;'

        # Multiple declined attempts from same card
        if 'card_number' in suspicious.columns and 'status' in suspicious.columns:
            declined_counts = suspicious[suspicious['status'] == 2].groupby('card_number').size()
            multiple_declines = declined_counts[declined_counts >= 3].index
            suspicious.loc[suspicious['card_number'].isin(multiple_declines), 'fraud_flags'] += 'MULTIPLE_DECLINES;'

        # Late night transactions (11 PM - 5 AM)
        if 'created_date' in suspicious.columns:
            suspicious['hour'] = pd.to_datetime(suspicious['created_date']).dt.hour
            late_night = (suspicious['hour'] >= 23) | (suspicious['hour'] < 5)
            suspicious.loc[late_night, 'fraud_flags'] += 'LATE_NIGHT;'
            suspicious.drop('hour', axis=1, inplace=True)

        # Filter to only suspicious records
        suspicious = suspicious[suspicious['fraud_flags'] != ''].copy()
        suspicious['fraud_flags'] = suspicious['fraud_flags'].str.rstrip(';')

        return suspicious

    @staticmethod
    def calculate_customer_metrics(transactions_df: pd.DataFrame, customer_id_col: str = 'customer_id') -> pd.DataFrame:
        """
        Calculate customer-level metrics from transactions.

        Metrics:
            - Total transactions
            - Total amount spent
            - Average transaction amount
            - Approval rate
            - Days since last transaction
            - Transaction frequency

        Returns:
            DataFrame with one row per customer
        """
        metrics = transactions_df.groupby(customer_id_col).agg({
            'id': 'count',  # Transaction count
            'amount': ['sum', 'mean', 'median', 'std'],
            'status': lambda x: (x == 1).sum(),  # Approved count
            'created_date': ['min', 'max']
        })

        metrics.columns = ['_'.join(col).strip() for col in metrics.columns.values]
        metrics.rename(columns={
            'id_count': 'total_transactions',
            'amount_sum': 'total_spent',
            'amount_mean': 'avg_transaction',
            'amount_median': 'median_transaction',
            'amount_std': 'std_transaction',
            'status_<lambda>': 'approved_count',
            'created_date_min': 'first_transaction_date',
            'created_date_max': 'last_transaction_date'
        }, inplace=True)

        # Calculate approval rate
        metrics['approval_rate'] = (metrics['approved_count'] / metrics['total_transactions'] * 100)

        # Days since last transaction
        now = pd.Timestamp.now()
        metrics['days_since_last_transaction'] = (now - pd.to_datetime(metrics['last_transaction_date'])).dt.days

        # Customer lifetime (days)
        metrics['customer_lifetime_days'] = (
            pd.to_datetime(metrics['last_transaction_date']) -
            pd.to_datetime(metrics['first_transaction_date'])
        ).dt.days

        return metrics.reset_index()


class DataTransformers:
    """Common data transformation utilities."""

    @staticmethod
    def mask_sensitive_data(df: pd.DataFrame, sensitive_cols: List[str]) -> pd.DataFrame:
        """
        Mask sensitive data (PII) for safe display.

        Args:
            df: DataFrame to mask
            sensitive_cols: Columns containing sensitive data

        Returns:
            DataFrame with masked values
        """
        masked = df.copy()

        for col in sensitive_cols:
            if col not in masked.columns:
                continue

            if 'card' in col.lower() or 'number' in col.lower():
                # Mask card numbers: show last 4 digits only
                masked[col] = masked[col].astype(str).apply(
                    lambda x: '****' + x[-4:] if len(x) >= 4 else '****'
                )
            elif 'ssn' in col.lower():
                # Mask SSN: show last 4 digits only
                masked[col] = masked[col].astype(str).apply(
                    lambda x: '***-**-' + x[-4:] if len(x) >= 4 else '***-**-****'
                )
            elif 'email' in col.lower():
                # Mask email: show first char and domain
                masked[col] = masked[col].astype(str).apply(
                    lambda x: x[0] + '***@' + x.split('@')[1] if '@' in x else '***'
                )
            elif 'cvv' in col.lower():
                # Completely mask CVV
                masked[col] = '***'
            else:
                # Generic masking
                masked[col] = '****'

        return masked

    @staticmethod
    def aggregate_by_period(
        df: pd.DataFrame,
        date_col: str,
        value_col: str,
        period: str = 'D',
        agg_func: str = 'sum'
    ) -> pd.DataFrame:
        """
        Aggregate data by time period.

        Args:
            df: DataFrame to aggregate
            date_col: Date column name
            value_col: Value column to aggregate
            period: Pandas period alias ('D'=day, 'W'=week, 'M'=month, 'Q'=quarter, 'Y'=year)
            agg_func: Aggregation function ('sum', 'mean', 'count', etc.)

        Returns:
            Aggregated DataFrame
        """
        df_copy = df.copy()
        df_copy[date_col] = pd.to_datetime(df_copy[date_col])
        df_copy = df_copy.set_index(date_col)

        aggregated = df_copy[value_col].resample(period).agg(agg_func)
        return aggregated.reset_index()

    @staticmethod
    def create_customer_segments(metrics_df: pd.DataFrame) -> pd.DataFrame:
        """
        Segment customers based on RFM (Recency, Frequency, Monetary) analysis.

        Segments:
            - Champions: Recent, frequent, high-value
            - Loyal: Frequent, high-value
            - At Risk: Not recent, previously high-value
            - Lost: Not recent, low frequency

        Args:
            metrics_df: DataFrame with customer metrics (must have days_since_last_transaction,
                       total_transactions, total_spent)

        Returns:
            DataFrame with segment labels
        """
        segmented = metrics_df.copy()

        # Calculate RFM quintiles
        segmented['recency_score'] = pd.qcut(segmented['days_since_last_transaction'], 5, labels=[5, 4, 3, 2, 1], duplicates='drop')
        segmented['frequency_score'] = pd.qcut(segmented['total_transactions'], 5, labels=[1, 2, 3, 4, 5], duplicates='drop')
        segmented['monetary_score'] = pd.qcut(segmented['total_spent'], 5, labels=[1, 2, 3, 4, 5], duplicates='drop')

        # Combine scores
        segmented['rfm_score'] = (
            segmented['recency_score'].astype(int) +
            segmented['frequency_score'].astype(int) +
            segmented['monetary_score'].astype(int)
        )

        # Assign segments
        def assign_segment(row):
            score = row['rfm_score']
            recency = row['recency_score']

            if score >= 13:
                return 'Champions'
            elif score >= 10:
                return 'Loyal Customers'
            elif score >= 7:
                return 'Potential Loyalists'
            elif recency <= 2 and score >= 5:
                return 'At Risk'
            elif recency <= 2:
                return 'Lost Customers'
            else:
                return 'New Customers'

        segmented['segment'] = segmented.apply(assign_segment, axis=1)

        return segmented


def export_to_csv(df: pd.DataFrame, filename: str, mask_sensitive: bool = True):
    """
    Export DataFrame to CSV with optional PII masking.

    Args:
        df: DataFrame to export
        filename: Output filename
        mask_sensitive: Whether to mask sensitive columns
    """
    if mask_sensitive:
        sensitive_cols = [col for col in df.columns if any(
            keyword in col.lower() for keyword in ['card', 'cvv', 'ssn', 'password']
        )]
        df = DataTransformers.mask_sensitive_data(df, sensitive_cols)

    df.to_csv(filename, index=False)
    logger.info(f"Exported {len(df)} rows to {filename}")


if __name__ == "__main__":
    # Example usage
    print("ETL Utilities loaded successfully")
    print("\nAvailable utilities:")
    print("  - DataQualityChecker: profile_dataframe, check_nulls, check_duplicates, check_referential_integrity")
    print("  - TransactionAnalytics: classify_transaction_status, detect_card_type, calculate_approval_rate, detect_suspicious_patterns")
    print("  - DataTransformers: mask_sensitive_data, aggregate_by_period, create_customer_segments")
