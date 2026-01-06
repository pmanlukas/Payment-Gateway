#!/bin/bash
# Cleanup script to reset repository to clean state before demo

set -e  # Exit on error

echo "🧹 Starting repository cleanup..."

# Navigate to Payment-Gateway directory
cd ~/Dev/Payment-Gateway

# Switch to main branch
echo "📌 Switching to main branch..."
git checkout main

# Pull latest changes
echo "⬇️  Pulling latest changes..."
git pull origin main

# Hard reset to match remote
echo "🔄 Resetting to match remote..."
git reset --hard origin/main

# Delete local branch if it exists
echo "🗑️  Removing local branch fix/issue-7-sql-injection..."
git branch -D fix/issue-7-sql-injection 2>/dev/null || true

# Delete remote branch if it exists
echo "🗑️  Removing remote branch fix/issue-7-sql-injection..."
git push origin --delete fix/issue-7-sql-injection 2>/dev/null || true

# Optional: Delete any other demo-related branches
git branch -D security/fix-sql-injection-customer-search 2>/dev/null || true
git push origin --delete security/fix-sql-injection-customer-search 2>/dev/null || true

echo ""
echo "✅ Clean state ready for demo!"
echo "📊 Current branch: $(git branch --show-current)"
echo "📝 Latest commit: $(git log -1 --oneline)"
