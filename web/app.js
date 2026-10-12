// SpendWise Frontend Logic (INR & Clean Onboarding)
let categoryChartInstance = null;
let monthlyChartInstance = null;
let currentExpenses = [];
let hasCheckedInitialBudget = false;

document.addEventListener('DOMContentLoaded', () => {
  initEventListeners();
  loadAll();
});

function initEventListeners() {
  // Modal buttons
  document.getElementById('btnOpenAddModal').addEventListener('click', () => openExpenseModal());
  document.getElementById('btnCloseModal').addEventListener('click', () => closeExpenseModal());
  document.getElementById('btnCancelModal').addEventListener('click', () => closeExpenseModal());

  document.getElementById('btnOpenBudgetModal').addEventListener('click', () => openBudgetModal(false));
  document.getElementById('btnCloseBudgetModal').addEventListener('click', () => closeBudgetModal());
  document.getElementById('btnCancelBudgetModal').addEventListener('click', () => closeBudgetModal());

  // Reset button
  document.getElementById('btnResetData').addEventListener('click', handleResetData);

  // Forms
  document.getElementById('expenseForm').addEventListener('submit', handleSaveExpense);
  document.getElementById('budgetForm').addEventListener('submit', handleSaveBudget);

  // Filters
  const searchInput = document.getElementById('searchInput');
  searchInput.addEventListener('input', debounce(() => fetchExpenses(), 250));

  document.getElementById('clearSearch').addEventListener('click', () => {
    searchInput.value = '';
    fetchExpenses();
  });

  document.getElementById('categoryFilter').addEventListener('change', () => fetchExpenses());
  document.getElementById('timeFilter').addEventListener('change', () => fetchExpenses());

  // Close modals on clicking overlay
  window.addEventListener('click', (e) => {
    if (e.target.classList.contains('modal-overlay')) {
      closeExpenseModal();
      closeBudgetModal();
    }
  });
}

async function loadAll() {
  await Promise.all([fetchSummary(), fetchExpenses()]);
}

async function fetchSummary() {
  try {
    const res = await fetch('/api/summary');
    if (!res.ok) throw new Error('Failed to fetch summary');
    const data = await res.json();
    renderSummary(data);

    // If fresh state (budgetLimit is 0) and we haven't prompted yet, ask user to set budget!
    if (!hasCheckedInitialBudget && (!data.budgetLimit || data.budgetLimit === 0)) {
      hasCheckedInitialBudget = true;
      openBudgetModal(true);
    }
  } catch (err) {
    console.error('Error fetching summary:', err);
  }
}

function renderSummary(data) {
  // Stat cards
  document.getElementById('cardTotalSpent').textContent = formatCurrency(data.totalAllTime);
  document.getElementById('cardTxCount').textContent = `${data.txCount} transactions recorded`;

  document.getElementById('cardMonthSpent').textContent = formatCurrency(data.monthTotal);
  document.getElementById('cardDailyAvg').textContent = `Daily avg: ${formatCurrency(data.dailyAverage)}`;

  document.getElementById('cardBudgetRemaining').textContent = formatCurrency(data.budgetRemaining);
  document.getElementById('cardBudgetLimit').textContent = `Limit: ${formatCurrency(data.budgetLimit)}`;

  // Budget status badge
  const badge = document.getElementById('budgetStatusBadge');
  badge.textContent = data.budgetStatusLabel || 'On Track';
  badge.className = 'stat-badge';
  if (data.budgetStatus === 'EXCEEDED') {
    badge.classList.add('badge-red');
  } else if (data.budgetStatus === 'WARNING') {
    badge.classList.add('badge-yellow');
  } else {
    badge.classList.add('badge-green');
  }

  // Highest Expense
  if (data.highestExpense) {
    document.getElementById('cardHighestAmt').textContent = formatCurrency(data.highestExpense.amount);
    document.getElementById('cardHighestTitle').textContent = data.highestExpense.title;
  } else {
    document.getElementById('cardHighestAmt').textContent = '₹0.00';
    document.getElementById('cardHighestTitle').textContent = 'None yet';
  }

  // Budget Progress Bar
  const pct = Math.min(100, Math.max(0, data.budgetPercentage || 0));
  document.getElementById('progressPctText').textContent = `${(data.budgetPercentage || 0).toFixed(1)}%`;
  document.getElementById('progressSpentDetails').textContent = `${formatCurrency(data.monthTotal)} of ${formatCurrency(data.budgetLimit)}`;
  
  const pBar = document.getElementById('budgetProgressBar');
  pBar.style.width = `${pct}%`;
  if (data.budgetStatus === 'EXCEEDED') {
    pBar.style.backgroundColor = '#ef4444';
  } else if (data.budgetStatus === 'WARNING') {
    pBar.style.backgroundColor = '#f59e0b';
  } else {
    pBar.style.backgroundColor = '#2563eb';
  }

  // Charts
  renderCategoryChart(data.categories || []);
  renderMonthlyChart(data.monthly || []);
}

async function fetchExpenses() {
  const search = document.getElementById('searchInput').value.trim();
  const category = document.getElementById('categoryFilter').value;
  const timePeriod = document.getElementById('timeFilter').value;

  const params = new URLSearchParams();
  if (search) params.append('search', search);
  if (category) params.append('category', category);
  if (timePeriod) params.append('timePeriod', timePeriod);

  try {
    const res = await fetch(`/api/expenses?${params.toString()}`);
    if (!res.ok) throw new Error('Failed to fetch expenses');
    const expenses = await res.json();
    currentExpenses = expenses;
    renderExpenseTable(expenses);
  } catch (err) {
    console.error('Error fetching expenses:', err);
    showToast('Failed to load expenses', 'error');
  }
}

function renderExpenseTable(expenses) {
  const tbody = document.getElementById('expenseTableBody');
  const emptyState = document.getElementById('emptyState');
  tbody.innerHTML = '';

  if (!expenses || expenses.length === 0) {
    emptyState.style.display = 'block';
    return;
  }
  emptyState.style.display = 'none';

  expenses.forEach((e) => {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td><strong>${escapeHtml(e.id)}</strong></td>
      <td>${escapeHtml(e.date)}</td>
      <td><strong>${escapeHtml(e.title)}</strong></td>
      <td>
        <span class="category-tag">
          <span>${e.categoryEmoji || '🏷️'}</span>
          <span>${escapeHtml(e.categoryName || e.category)}</span>
        </span>
      </td>
      <td>${escapeHtml(e.paymentMethodName || e.paymentMethod)}</td>
      <td class="text-right expense-amount">${formatCurrency(e.amount)}</td>
      <td><small style="color: #64748b;">${escapeHtml(e.notes || '-')}</small></td>
      <td class="text-center">
        <div class="action-buttons">
          <button class="btn-icon" title="Edit Expense" onclick="openExpenseModal('${e.id}')">✏️</button>
          <button class="btn-icon delete" title="Delete Expense" onclick="deleteExpense('${e.id}')">🗑️</button>
        </div>
      </td>
    `;
    tbody.appendChild(tr);
  });
}

function renderCategoryChart(categories) {
  if (typeof Chart === 'undefined') return;
  const canvas = document.getElementById('categoryChart');
  if (!canvas) return;
  const ctx = canvas.getContext('2d');

  if (categoryChartInstance) {
    categoryChartInstance.destroy();
  }

  if (categories.length === 0) {
    return;
  }

  const labels = categories.map(c => `${c.emoji} ${c.displayName}`);
  const data = categories.map(c => c.total);
  const colors = [
    '#2563eb', '#10b981', '#f59e0b', '#8b5cf6', '#ec4899', 
    '#06b6d4', '#64748b', '#ef4444', '#14b8a6', '#f97316'
  ];

  categoryChartInstance = new Chart(ctx, {
    type: 'doughnut',
    data: {
      labels: labels,
      datasets: [{
        data: data,
        backgroundColor: colors.slice(0, categories.length),
        borderWidth: 2,
        borderColor: '#ffffff'
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          position: 'right',
          labels: {
            boxWidth: 12,
            font: { size: 11, family: 'Inter' }
          }
        },
        tooltip: {
          callbacks: {
            label: function(ctx) {
              const val = ctx.raw || 0;
              return ` ₹${val.toLocaleString('en-IN', { minimumFractionDigits: 2 })}`;
            }
          }
        }
      }
    }
  });
}

function renderMonthlyChart(monthly) {
  if (typeof Chart === 'undefined') return;
  const canvas = document.getElementById('monthlyChart');
  if (!canvas) return;
  const ctx = canvas.getContext('2d');

  if (monthlyChartInstance) {
    monthlyChartInstance.destroy();
  }

  const filtered = monthly.filter(m => m.total > 0 || m.month.endsWith('-10') || m.month.endsWith('-09'));
  const labels = (filtered.length > 0 ? filtered : monthly).map(m => m.label);
  const data = (filtered.length > 0 ? filtered : monthly).map(m => m.total);

  monthlyChartInstance = new Chart(ctx, {
    type: 'bar',
    data: {
      labels: labels,
      datasets: [{
        label: 'Spent (₹)',
        data: data,
        backgroundColor: '#8b5cf6',
        borderRadius: 6
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false },
        tooltip: {
          callbacks: {
            label: function(ctx) {
              return ` ₹${(ctx.raw || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}`;
            }
          }
        }
      },
      scales: {
        y: {
          beginAtZero: true,
          grid: { color: '#f1f5f9' },
          ticks: { font: { size: 11 } }
        },
        x: {
          grid: { display: false },
          ticks: { font: { size: 11 } }
        }
      }
    }
  });
}

// Modal logic
function openExpenseModal(id = null) {
  const modal = document.getElementById('expenseModal');
  const title = document.getElementById('modalTitle');
  const form = document.getElementById('expenseForm');
  form.reset();

  if (id) {
    const exp = currentExpenses.find(e => e.id === id);
    if (!exp) return;
    title.textContent = 'Edit Expense';
    document.getElementById('expenseId').value = exp.id;
    document.getElementById('expenseTitle').value = exp.title;
    document.getElementById('expenseAmount').value = exp.amount;
    document.getElementById('expenseDate').value = exp.date;
    document.getElementById('expenseCategory').value = exp.category;
    document.getElementById('expensePayment').value = exp.paymentMethod;
    document.getElementById('expenseNotes').value = exp.notes || '';
  } else {
    title.textContent = 'Add New Expense';
    document.getElementById('expenseId').value = '';
    document.getElementById('expenseDate').value = new Date().toISOString().split('T')[0];
  }

  modal.classList.add('active');
}

function closeExpenseModal() {
  document.getElementById('expenseModal').classList.remove('active');
}

function openBudgetModal(isInitial = false) {
  const title = document.getElementById('budgetModalTitle');
  if (isInitial) {
    title.textContent = '🎯 Welcome! Set Your Monthly Budget (₹)';
  } else {
    title.textContent = '🎯 Set Monthly Budget';
  }

  fetch('/api/budget')
    .then(r => r.json())
    .then(b => {
      document.getElementById('budgetLimitInput').value = b.limit > 0 ? b.limit : '';
      document.getElementById('budgetModal').classList.add('active');
    })
    .catch(() => {
      document.getElementById('budgetLimitInput').value = '25000';
      document.getElementById('budgetModal').classList.add('active');
    });
}

function closeBudgetModal() {
  document.getElementById('budgetModal').classList.remove('active');
}

async function handleSaveExpense(e) {
  e.preventDefault();
  const id = document.getElementById('expenseId').value;
  const expenseData = {
    title: document.getElementById('expenseTitle').value.trim(),
    amount: parseFloat(document.getElementById('expenseAmount').value),
    date: document.getElementById('expenseDate').value,
    category: document.getElementById('expenseCategory').value,
    paymentMethod: document.getElementById('expensePayment').value,
    notes: document.getElementById('expenseNotes').value.trim()
  };

  try {
    let res;
    if (id) {
      expenseData.id = id;
      res = await fetch(`/api/expenses/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(expenseData)
      });
    } else {
      res = await fetch('/api/expenses', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(expenseData)
      });
    }

    if (!res.ok) throw new Error('Save failed');

    closeExpenseModal();
    showToast(id ? 'Expense updated successfully!' : 'Expense added successfully!', 'success');
    loadAll();
  } catch (err) {
    showToast(err.message, 'error');
  }
}

async function deleteExpense(id) {
  if (!confirm(`Are you sure you want to delete expense ${id}?`)) return;

  try {
    const res = await fetch(`/api/expenses/${id}`, { method: 'DELETE' });
    if (!res.ok) throw new Error('Delete failed');
    showToast(`Expense ${id} deleted`, 'success');
    loadAll();
  } catch (err) {
    showToast(err.message, 'error');
  }
}

async function handleSaveBudget(e) {
  e.preventDefault();
  const limit = parseFloat(document.getElementById('budgetLimitInput').value);
  if (isNaN(limit) || limit < 0) {
    alert('Please enter a valid budget amount in ₹.');
    return;
  }

  try {
    const res = await fetch('/api/budget', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ limit: limit })
    });
    if (!res.ok) throw new Error('Budget update failed');
    closeBudgetModal();
    showToast(`Monthly budget set to ${formatCurrency(limit)}!`, 'success');
    loadAll();
  } catch (err) {
    showToast(err.message, 'error');
  }
}

async function handleResetData() {
  const confirm1 = confirm("⚠️ Are you sure you want to wipe all expenses and start freshly?");
  if (!confirm1) return;

  try {
    const res = await fetch('/api/reset', { method: 'POST' });
    if (!res.ok) throw new Error('Reset failed');
    showToast('Clean slate initialized! Set your budget to begin.', 'success');
    await loadAll();
    openBudgetModal(true);
  } catch (err) {
    showToast('Failed to reset data', 'error');
  }
}

// Helpers
function formatCurrency(val) {
  return `₹${Number(val || 0).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

function escapeHtml(str) {
  if (!str) return '';
  return str.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}

function showToast(msg, type = 'success') {
  const container = document.getElementById('toastContainer');
  const toast = document.createElement('div');
  toast.className = `toast ${type}`;
  toast.innerHTML = `<span>${type === 'success' ? '✔' : '⚠️'}</span><span>${escapeHtml(msg)}</span>`;
  container.appendChild(toast);
  setTimeout(() => toast.remove(), 3500);
}

function debounce(func, delay) {
  let timer;
  return function(...args) {
    clearTimeout(timer);
    timer = setTimeout(() => func.apply(this, args), delay);
  };
}
