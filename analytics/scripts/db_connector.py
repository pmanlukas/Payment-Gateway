"""
Database Connection Middleware for MegaBank Payment Gateway Analytics

This module provides database connectivity using SQLAlchemy to the existing
MySQL payment gateway database. Supports both direct SQL queries and ORM access.
"""

import os
from sqlalchemy import create_engine, text, MetaData, Table
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import QueuePool
import pandas as pd
from typing import Optional, Dict, Any
import logging

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class DatabaseConnector:
    """
    Handles database connections to MegaBank Payment Gateway MySQL database.

    Environment variables (set by Docker or .env):
        DB_HOST: MySQL host (default: mysql)
        DB_PORT: MySQL port (default: 3306)
        DB_NAME: Database name (default: payments)
        DB_USER: Database user (default: payment_admin)
        DB_PASSWORD: Database password
    """

    def __init__(self):
        self.host = os.getenv('DB_HOST', 'mysql')
        self.port = os.getenv('DB_PORT', '3306')
        self.database = os.getenv('DB_NAME', 'payments')
        self.user = os.getenv('DB_USER', 'payment_admin')
        self.password = os.getenv('DB_PASSWORD', 'Payment123!')

        # Build connection string
        self.connection_string = (
            f"mysql+pymysql://{self.user}:{self.password}"
            f"@{self.host}:{self.port}/{self.database}"
        )

        # Create engine with connection pooling
        self.engine = create_engine(
            self.connection_string,
            poolclass=QueuePool,
            pool_size=5,
            max_overflow=10,
            pool_pre_ping=True,  # Verify connections before using
            echo=False  # Set to True for SQL debugging
        )

        # Create session factory
        self.Session = sessionmaker(bind=self.engine)

        # Reflect database metadata
        self.metadata = MetaData()
        self.metadata.reflect(bind=self.engine)

        logger.info(f"Connected to database: {self.database}@{self.host}:{self.port}")

    def get_session(self):
        """Get a new database session (for ORM operations)."""
        return self.Session()

    def execute_query(self, query: str, params: Optional[Dict[str, Any]] = None) -> pd.DataFrame:
        """
        Execute a SQL query and return results as pandas DataFrame.

        Args:
            query: SQL query string
            params: Optional dictionary of query parameters

        Returns:
            pandas DataFrame with query results
        """
        try:
            with self.engine.connect() as conn:
                result = pd.read_sql(text(query), conn, params=params)
            logger.info(f"Query executed successfully, returned {len(result)} rows")
            return result
        except Exception as e:
            logger.error(f"Error executing query: {e}")
            raise

    def execute_raw_sql(self, sql: str, params: Optional[Dict[str, Any]] = None):
        """
        Execute raw SQL (INSERT, UPDATE, DELETE, etc.) without returning results.

        Args:
            sql: SQL statement
            params: Optional dictionary of parameters
        """
        try:
            with self.engine.connect() as conn:
                conn.execute(text(sql), params or {})
                conn.commit()
            logger.info("SQL executed successfully")
        except Exception as e:
            logger.error(f"Error executing SQL: {e}")
            raise

    def get_table(self, table_name: str) -> Table:
        """
        Get a SQLAlchemy Table object for the specified table.

        Args:
            table_name: Name of the database table

        Returns:
            SQLAlchemy Table object
        """
        if table_name not in self.metadata.tables:
            raise ValueError(f"Table '{table_name}' not found in database")
        return self.metadata.tables[table_name]

    def get_table_data(self, table_name: str, limit: Optional[int] = None) -> pd.DataFrame:
        """
        Read entire table into pandas DataFrame.

        Args:
            table_name: Name of the table
            limit: Optional row limit

        Returns:
            pandas DataFrame with table data
        """
        query = f"SELECT * FROM {table_name}"
        if limit:
            query += f" LIMIT {limit}"
        return self.execute_query(query)

    def get_table_info(self, table_name: str) -> Dict[str, Any]:
        """
        Get metadata information about a table.

        Args:
            table_name: Name of the table

        Returns:
            Dictionary with table metadata (columns, types, etc.)
        """
        table = self.get_table(table_name)
        return {
            'name': table.name,
            'columns': [
                {
                    'name': col.name,
                    'type': str(col.type),
                    'nullable': col.nullable,
                    'primary_key': col.primary_key
                }
                for col in table.columns
            ],
            'primary_key': [col.name for col in table.primary_key],
            'indexes': [idx.name for idx in table.indexes]
        }

    def list_tables(self):
        """List all tables in the database."""
        return list(self.metadata.tables.keys())

    def get_row_count(self, table_name: str) -> int:
        """Get the number of rows in a table."""
        query = f"SELECT COUNT(*) as count FROM {table_name}"
        result = self.execute_query(query)
        return int(result.iloc[0]['count'])

    def close(self):
        """Close database connections."""
        self.engine.dispose()
        logger.info("Database connections closed")


# Global singleton instance
_db_instance: Optional[DatabaseConnector] = None


def get_db() -> DatabaseConnector:
    """
    Get or create the global DatabaseConnector singleton instance.

    Returns:
        DatabaseConnector instance
    """
    global _db_instance
    if _db_instance is None:
        _db_instance = DatabaseConnector()
    return _db_instance


# Convenience functions for quick database access
def query(sql: str, params: Optional[Dict[str, Any]] = None) -> pd.DataFrame:
    """Execute SQL query and return DataFrame."""
    return get_db().execute_query(sql, params)


def execute(sql: str, params: Optional[Dict[str, Any]] = None):
    """Execute SQL statement without returning results."""
    return get_db().execute_raw_sql(sql, params)


def get_table_df(table_name: str, limit: Optional[int] = None) -> pd.DataFrame:
    """Read table into DataFrame."""
    return get_db().get_table_data(table_name, limit)


if __name__ == "__main__":
    # Test database connection
    db = get_db()
    print(f"Connected to database: {db.database}")
    print(f"\nAvailable tables: {', '.join(db.list_tables())}")

    # Show table row counts
    print("\nTable row counts:")
    for table in db.list_tables():
        count = db.get_row_count(table)
        print(f"  {table}: {count} rows")
