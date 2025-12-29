// MegaBank Payment Gateway - Web UI JavaScript

const API_BASE_URL = '/payment-gateway/api';
const DEFAULT_API_KEY = 'test_key_12345';

// Tab Navigation
document.addEventListener('DOMContentLoaded', function() {
    // Tab switching
    const navTabs = document.querySelectorAll('.nav-tab');
    const tabContents = document.querySelectorAll('.tab-content');

    navTabs.forEach(tab => {
        tab.addEventListener('click', () => {
            const targetTab = tab.dataset.tab;

            // Update active states
            navTabs.forEach(t => t.classList.remove('active'));
            tabContents.forEach(c => c.classList.remove('active'));

            tab.classList.add('active');
            document.getElementById(targetTab).classList.add('active');

            // Load data when switching to certain tabs
            if (targetTab === 'dashboard') {
                loadDashboard();
            } else if (targetTab === 'transactions') {
                loadTransactions();
            } else if (targetTab === 'customers') {
                loadCustomers();
            } else if (targetTab === 'merchants') {
                loadMerchants();
            }
        });
    });

    // Initialize dashboard on load
    loadDashboard();
    loadMerchantDropdown();
    checkSystemStatus();

    // Payment form submission
    document.getElementById('payment-form').addEventListener('submit', handlePaymentSubmit);

    // Auto-refresh system status every 30 seconds
    setInterval(checkSystemStatus, 30000);
});

// Dashboard Functions
async function loadDashboard() {
    try {
        // Load statistics
        const transactions = await apiRequest('/transactions');

        if (transactions && Array.isArray(transactions)) {
            // Calculate statistics
            const totalTransactions = transactions.length;
            const approvedTransactions = transactions.filter(t => t.status === 1).length;
            const totalRevenue = transactions
                .filter(t => t.status === 1)
                .reduce((sum, t) => sum + (parseFloat(t.amount) || 0), 0);
            const approvalRate = totalTransactions > 0
                ? ((approvedTransactions / totalTransactions) * 100).toFixed(1)
                : '0.0';

            // Get unique customers
            const uniqueCustomers = new Set(transactions.map(t => t.customer_id).filter(Boolean));
            const activeCustomers = uniqueCustomers.size;

            // Update dashboard
            document.getElementById('total-transactions').textContent = totalTransactions.toLocaleString();
            document.getElementById('total-revenue').textContent = '$' + totalRevenue.toLocaleString('en-US', {minimumFractionDigits: 2, maximumFractionDigits: 2});
            document.getElementById('approval-rate').textContent = approvalRate + '%';
            document.getElementById('active-customers').textContent = activeCustomers.toLocaleString();

            // Load recent transactions table
            loadRecentTransactions(transactions.slice(0, 5));
        }

        // Load system health
        await loadSystemHealth();

    } catch (error) {
        console.error('Error loading dashboard:', error);
        showError('dashboard', 'Failed to load dashboard data');
    }
}

async function loadRecentTransactions(transactions) {
    const container = document.getElementById('recent-transactions-table');

    if (!transactions || transactions.length === 0) {
        container.innerHTML = '<p>No recent transactions</p>';
        return;
    }

    let html = `
        <table>
            <thead>
                <tr>
                    <th>Transaction ID</th>
                    <th>Amount</th>
                    <th>Status</th>
                    <th>Customer</th>
                    <th>Date</th>
                </tr>
            </thead>
            <tbody>
    `;

    transactions.forEach(txn => {
        const status = txn.status === 1 ? 'success' : 'error';
        const statusText = txn.status === 1 ? 'Approved' : 'Declined';
        const cardDisplay = txn.card_number ? maskCardNumber(txn.card_number) : 'N/A';
        const date = txn.created_date ? new Date(txn.created_date).toLocaleString() : 'N/A';

        html += `
            <tr>
                <td>${escapeHtml(txn.id || 'N/A')}</td>
                <td>$${parseFloat(txn.amount || 0).toFixed(2)}</td>
                <td><span class="badge ${status}">${statusText}</span></td>
                <td>${escapeHtml(txn.customer_email || txn.customer_id || 'N/A')}</td>
                <td>${date}</td>
            </tr>
        `;
    });

    html += '</tbody></table>';
    container.innerHTML = html;
}

async function loadSystemHealth() {
    try {
        const health = await apiRequest('/health', 'GET', null, true);
        const container = document.getElementById('system-health');

        if (health && (health.status === 'UP' || health.status === 'healthy')) {
            container.innerHTML = `
                <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px;">
                    <div class="stat-card">
                        <div class="stat-icon approval"><i class="fas fa-check-circle"></i></div>
                        <div class="stat-info">
                            <div class="stat-label">Database</div>
                            <div class="stat-value" style="font-size: 1.2rem;">${health.database || 'Connected'}</div>
                        </div>
                    </div>
                    <div class="stat-card">
                        <div class="stat-icon customers"><i class="fas fa-server"></i></div>
                        <div class="stat-info">
                            <div class="stat-label">API Status</div>
                            <div class="stat-value" style="font-size: 1.2rem;">${health.status || 'Healthy'}</div>
                        </div>
                    </div>
                    <div class="stat-card">
                        <div class="stat-icon transactions"><i class="fas fa-clock"></i></div>
                        <div class="stat-info">
                            <div class="stat-label">Memory Used</div>
                            <div class="stat-value" style="font-size: 1.2rem;">${health.memory_used ? Math.round(health.memory_used / 1024 / 1024) + 'MB' : 'N/A'}</div>
                        </div>
                    </div>
                </div>
            `;
        } else {
            container.innerHTML = '<p style="color: var(--danger-color);">System health check failed</p>';
        }
    } catch (error) {
        document.getElementById('system-health').innerHTML = '<p style="color: var(--warning-color);">Health check unavailable</p>';
    }
}

async function checkSystemStatus() {
    try {
        const health = await apiRequest('/health', 'GET', null, true);
        const statusElement = document.getElementById('system-status');

        if (health && (health.status === 'UP' || health.status === 'healthy')) {
            statusElement.innerHTML = '<i class="fas fa-circle" style="color: var(--success-color);"></i> System Online';
        } else {
            statusElement.innerHTML = '<i class="fas fa-circle" style="color: var(--warning-color);"></i> System Degraded';
        }
    } catch (error) {
        document.getElementById('system-status').innerHTML = '<i class="fas fa-circle" style="color: var(--danger-color);"></i> System Offline';
    }
}

// Payment Processing
async function handlePaymentSubmit(event) {
    event.preventDefault();

    const resultBox = document.getElementById('payment-result');
    resultBox.className = 'result-box';
    resultBox.textContent = '';

    const formData = {
        card_number: document.getElementById('card-number').value,
        cvv: document.getElementById('cvv').value,
        expiry: document.getElementById('expiry').value,
        amount: parseFloat(document.getElementById('amount').value),
        currency: 'USD',
        merchant_id: document.getElementById('merchant-id').value || null,
        customer_id: document.getElementById('customer-id').value || null
    };

    const apiKey = document.getElementById('api-key').value || DEFAULT_API_KEY;

    try {
        const response = await apiRequest('/payments', 'POST', formData, true, apiKey);

        if (response && response.status === 'approved') {
            resultBox.className = 'result-box success';
            resultBox.innerHTML = `
                <h3><i class="fas fa-check-circle"></i> Payment Approved</h3>
                <p><strong>Transaction ID:</strong> ${response.transaction_id}</p>
                <p><strong>Amount:</strong> $${response.amount}</p>
                <p><strong>Card:</strong> ${maskCardNumber(formData.card_number)}</p>
                <p><strong>Message:</strong> ${response.message || 'Payment processed successfully'}</p>
            `;
            document.getElementById('payment-form').reset();
            document.getElementById('api-key').value = apiKey; // Restore API key
        } else {
            resultBox.className = 'result-box error';
            resultBox.innerHTML = `
                <h3><i class="fas fa-times-circle"></i> Payment Declined</h3>
                <p><strong>Reason:</strong> ${response.message || 'Payment could not be processed'}</p>
            `;
        }
    } catch (error) {
        resultBox.className = 'result-box error';
        resultBox.innerHTML = `
            <h3><i class="fas fa-exclamation-triangle"></i> Error</h3>
            <p>${error.message || 'Failed to process payment'}</p>
        `;
    }
}

// Transactions Tab
async function loadTransactions() {
    const apiKey = document.getElementById('txn-api-key').value || DEFAULT_API_KEY;
    const container = document.getElementById('transactions-table');

    try {
        container.innerHTML = '<div class="loading"><i class="fas fa-spinner"></i> Loading transactions...</div>';

        const transactions = await apiRequest('/transactions', 'GET', null, true, apiKey);

        if (!transactions || transactions.length === 0) {
            container.innerHTML = '<p>No transactions found</p>';
            return;
        }

        let html = `
            <table>
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Card Number</th>
                        <th>Amount</th>
                        <th>Status</th>
                        <th>Customer</th>
                        <th>Merchant</th>
                        <th>Risk Score</th>
                        <th>Date</th>
                    </tr>
                </thead>
                <tbody>
        `;

        transactions.forEach(txn => {
            const status = txn.status === 1 ? 'success' : 'error';
            const statusText = txn.status === 1 ? 'Approved' : 'Declined';
            const cardDisplay = txn.card_number ? maskCardNumber(txn.card_number) : 'N/A';
            const date = txn.created_date ? new Date(txn.created_date).toLocaleString() : 'N/A';
            // risk_score is a string ('LOW', 'MEDIUM', 'HIGH')
            const riskScore = txn.risk_score || 'UNKNOWN';
            let riskClass = 'success';
            if (riskScore === 'HIGH') riskClass = 'error';
            else if (riskScore === 'MEDIUM') riskClass = 'pending';

            html += `
                <tr>
                    <td>${escapeHtml(txn.id || 'N/A')}</td>
                    <td><code>${cardDisplay}</code></td>
                    <td>$${parseFloat(txn.amount || 0).toFixed(2)} ${txn.currency || 'USD'}</td>
                    <td><span class="badge ${status}">${statusText}</span></td>
                    <td>${escapeHtml(txn.customer_email || txn.customer_id || 'N/A')}</td>
                    <td>${escapeHtml(txn.merchant_id || 'N/A')}</td>
                    <td><span class="badge ${riskClass}">${riskScore}</span></td>
                    <td>${date}</td>
                </tr>
            `;
        });

        html += '</tbody></table>';
        container.innerHTML = html;

    } catch (error) {
        container.innerHTML = `<div class="result-box error">Failed to load transactions: ${error.message}</div>`;
    }
}

// Customers Tab
async function loadCustomers() {
    const apiKey = document.getElementById('cust-api-key').value || DEFAULT_API_KEY;
    const container = document.getElementById('customers-table');

    try {
        container.innerHTML = '<div class="loading"><i class="fas fa-spinner"></i> Loading customers...</div>';

        const customers = await apiRequest('/customers', 'GET', null, true, apiKey);

        if (!customers || customers.length === 0) {
            container.innerHTML = '<p>No customers found</p>';
            return;
        }

        let html = `
            <table>
                <thead>
                    <tr>
                        <th>Customer ID</th>
                        <th>Name</th>
                        <th>Email</th>
                        <th>Phone</th>
                        <th>VIP</th>
                        <th>Risk Level</th>
                        <th>Credit Limit</th>
                    </tr>
                </thead>
                <tbody>
        `;

        customers.forEach(customer => {
            const vipBadge = customer.is_vip ? '<span class="badge success">VIP</span>' : '<span class="badge">Regular</span>';
            const riskClass = customer.risk_level > 5 ? 'error' : customer.risk_level > 3 ? 'pending' : 'success';

            html += `
                <tr>
                    <td><code>${escapeHtml(customer.customer_id || 'N/A')}</code></td>
                    <td>${escapeHtml(customer.first_name || '')} ${escapeHtml(customer.last_name || '')}</td>
                    <td>${escapeHtml(customer.email || 'N/A')}</td>
                    <td>${escapeHtml(customer.phone || 'N/A')}</td>
                    <td>${vipBadge}</td>
                    <td><span class="badge ${riskClass}">${customer.risk_level || 0}</span></td>
                    <td>$${parseFloat(customer.credit_limit || 0).toLocaleString()}</td>
                </tr>
            `;
        });

        html += '</tbody></table>';
        container.innerHTML = html;

    } catch (error) {
        container.innerHTML = `<div class="result-box error">Failed to load customers: ${error.message}</div>`;
    }
}

// Merchants Tab
async function loadMerchants() {
    const container = document.getElementById('merchants-list');

    try {
        container.innerHTML = '<div class="loading"><i class="fas fa-spinner"></i> Loading merchants...</div>';

        const merchants = await apiRequest('/merchants', 'GET', null, true);

        if (!merchants || merchants.length === 0) {
            container.innerHTML = '<p>No merchants found</p>';
            return;
        }

        let html = `
            <table>
                <thead>
                    <tr>
                        <th>Merchant ID</th>
                        <th>Name</th>
                        <th>Contact Email</th>
                        <th>Fee (%)</th>
                        <th>Status</th>
                    </tr>
                </thead>
                <tbody>
        `;

        merchants.forEach(merchant => {
            const statusBadge = merchant.is_active ? '<span class="badge success">Active</span>' : '<span class="badge error">Inactive</span>';

            html += `
                <tr>
                    <td><code>${escapeHtml(merchant.merchant_id || 'N/A')}</code></td>
                    <td>${escapeHtml(merchant.name || 'N/A')}</td>
                    <td>${escapeHtml(merchant.contact_email || 'N/A')}</td>
                    <td>${merchant.fee_percentage ? merchant.fee_percentage.toFixed(2) + '%' : 'N/A'}</td>
                    <td>${statusBadge}</td>
                </tr>
            `;
        });

        html += '</tbody></table>';
        container.innerHTML = html;

    } catch (error) {
        container.innerHTML = `<div class="result-box error">Failed to load merchants: ${error.message}</div>`;
    }
}

// Load merchants for dropdown
async function loadMerchantDropdown() {
    try {
        const merchants = await apiRequest('/merchants', 'GET', null, true);
        const dropdown = document.getElementById('merchant-id');

        if (merchants && merchants.length > 0) {
            merchants.forEach(merchant => {
                const option = document.createElement('option');
                option.value = merchant.merchant_id;
                option.textContent = `${merchant.name} (${merchant.merchant_id})`;
                dropdown.appendChild(option);
            });
        }
    } catch (error) {
        console.error('Failed to load merchants for dropdown:', error);
    }
}

// API Request Helper
async function apiRequest(endpoint, method = 'GET', data = null, requireAuth = true, apiKey = null) {
    const url = `${API_BASE_URL}${endpoint}`;
    const headers = {
        'Content-Type': 'application/json'
    };

    if (requireAuth) {
        headers['X-API-Key'] = apiKey || DEFAULT_API_KEY;
    }

    const options = {
        method: method,
        headers: headers
    };

    if (data && (method === 'POST' || method === 'PUT')) {
        options.body = JSON.stringify(data);
    }

    const response = await fetch(url, options);

    if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`API request failed: ${response.status} - ${errorText}`);
    }

    const contentType = response.headers.get('content-type');
    if (contentType && contentType.includes('application/json')) {
        return await response.json();
    } else {
        return await response.text();
    }
}

// Utility Functions
function maskCardNumber(cardNumber) {
    if (!cardNumber || cardNumber.length < 4) {
        return '****';
    }
    const last4 = cardNumber.slice(-4);
    return `**** **** **** ${last4}`;
}

function escapeHtml(text) {
    if (text === null || text === undefined) {
        return '';
    }
    const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };
    return String(text).replace(/[&<>"']/g, m => map[m]);
}

function showError(containerId, message) {
    const container = document.getElementById(containerId);
    if (container) {
        container.innerHTML = `<div class="result-box error">${escapeHtml(message)}</div>`;
    }
}
