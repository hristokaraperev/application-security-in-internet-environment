'use strict';

// Tokens are kept only in memory (module scope) — never in localStorage — so
// they vanish on tab close and are not reachable via persisted storage.
let accessToken = null;
let refreshToken = null;
let currentUser = null;

const $ = (id) => document.getElementById(id);

function log(message, payload) {
    const time = new Date().toLocaleTimeString();
    const text = payload !== undefined ? `${message} ${JSON.stringify(payload)}` : message;
    $('log').textContent = `[${time}] ${text}\n` + $('log').textContent;
}

function setStatus(status) {
    const badge = $('last-status');
    badge.textContent = status;
    badge.className = 'badge ' + (status >= 200 && status < 300 ? 'ok' : 'err');
}

function showToken() {
    $('token-display').textContent = accessToken
        ? accessToken.slice(0, 24) + '…' + accessToken.slice(-12)
        : 'няма';
}

// Single choke point for all calls. The gateway is same-origin, so paths are relative.
async function api(path, { method = 'GET', body = null, auth = false } = {}) {
    const headers = { 'Content-Type': 'application/json' };
    if (auth && accessToken) {
        headers['Authorization'] = 'Bearer ' + accessToken;
    }
    const res = await fetch(path, {
        method,
        headers,
        body: body ? JSON.stringify(body) : null
    });
    let data = null;
    try { data = await res.json(); } catch (_) { /* empty body */ }
    setStatus(res.status);
    log(`${method} ${path} →`, data ?? res.status);
    return { status: res.status, ok: res.ok, data };
}

function enterWallet(username) {
    currentUser = username;
    $('current-user').textContent = username;
    $('auth-panel').classList.add('hidden');
    $('wallet-panel').classList.remove('hidden');
    showToken();
    getBalance();
}

function leaveWallet() {
    accessToken = refreshToken = currentUser = null;
    $('wallet-panel').classList.add('hidden');
    $('auth-panel').classList.remove('hidden');
    $('balance').textContent = '—';
    $('transactions').replaceChildren();
    showToken();
}

async function register() {
    const username = $('username').value.trim();
    const password = $('password').value;
    const { ok } = await api('/auth/register', { method: 'POST', body: { username, password } });
    if (ok) log(`Регистриран потребител '${username}'. Вече може да влезете.`);
}

async function login() {
    const username = $('username').value.trim();
    const password = $('password').value;
    const { ok, data } = await api('/auth/login', { method: 'POST', body: { username, password } });
    if (ok) {
        accessToken = data.accessToken;
        refreshToken = data.refreshToken;
        enterWallet(username);
    }
}

async function getBalance() {
    const { ok, data } = await api('/api/wallet/balance', { auth: true });
    if (ok) $('balance').textContent = Number(data.balance).toFixed(2) + ' лв.';
}

async function transfer() {
    const toUsername = $('to-username').value.trim();
    const amount = $('amount').value;
    const { ok } = await api('/api/wallet/transfer', {
        method: 'POST', auth: true, body: { toUsername, amount }
    });
    if (ok) { getBalance(); getTransactions(); }
}

async function getTransactions() {
    const { ok, data } = await api('/api/wallet/transactions', { auth: true });
    if (!ok || !Array.isArray(data)) return;
    const list = $('transactions');
    list.replaceChildren();
    if (data.length === 0) {
        const li = document.createElement('li');
        li.textContent = 'няма транзакции';
        list.appendChild(li);
        return;
    }
    for (const t of data) {
        const li = document.createElement('li');
        li.className = t.direction === 'DEBIT' ? 'debit' : 'credit';
        // textContent (not innerHTML) — untrusted values are never parsed as HTML (anti-XSS).
        const sign = t.direction === 'DEBIT' ? '−' : '+';
        li.textContent = `${sign}${Number(t.amount).toFixed(2)} лв.  ${t.direction === 'DEBIT' ? 'към' : 'от'} ${t.counterparty}`;
        list.appendChild(li);
    }
}

async function refresh() {
    if (!refreshToken) { log('Няма refresh токен'); return; }
    const { ok, data } = await api('/auth/refresh', { method: 'POST', body: { refreshToken } });
    if (ok) {
        accessToken = data.accessToken;
        refreshToken = data.refreshToken;
        showToken();
        log('Издаден нов access токен (старият refresh е ротиран).');
    }
}

async function logout() {
    if (refreshToken) {
        // Logout invalidates ALL of the user's access tokens server-side (derived
        // from the refresh token), so the access token stops working at the wallet
        // immediately — no need to send it here.
        await api('/auth/logout', { method: 'POST', body: { refreshToken } });
    }
    leaveWallet();
}

document.addEventListener('DOMContentLoaded', () => {
    $('btn-login').addEventListener('click', login);
    $('btn-register').addEventListener('click', register);
    $('btn-logout').addEventListener('click', logout);
    $('btn-balance').addEventListener('click', getBalance);
    $('btn-transfer').addEventListener('click', transfer);
    $('btn-transactions').addEventListener('click', getTransactions);
    $('btn-refresh').addEventListener('click', refresh);
});
