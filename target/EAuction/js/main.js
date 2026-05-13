/* ============================================================
   E-Auction — main.js  (v2: all features)
   ============================================================ */
'use strict';

// ── Global Bid State (persistent per product) ───────────────────────────────
// Key: productId, Value: currentBid (never re-read from DOM)
const bidState = {};

function initializeBidState(productId, basePrice) {
    if (!bidState[productId]) {
        bidState[productId] = basePrice;
    }
}

function getCurrentBidState(productId) {
    return bidState[productId] || 0;
}

function incrementBidState(productId, step) {
    const current = getCurrentBidState(productId);
    bidState[productId] = current + step;
    return bidState[productId];
}

function resetBidState(productId, basePrice) {
    bidState[productId] = basePrice;
}

// ── Toast ───────────────────────────────────────────────────────────────────
function showToast(message, type = 'success') {
    let c = document.getElementById('toast-container');
    if (!c) { c = document.createElement('div'); c.id = 'toast-container'; document.body.appendChild(c); }
    const t = document.createElement('div');
    t.className = 'toast toast-' + type;
    t.textContent = message;
    c.appendChild(t);
    requestAnimationFrame(() => { t.style.opacity = '1'; });
    setTimeout(() => { t.style.opacity = '0'; setTimeout(() => t.remove(), 400); }, 3800);
}

// ── Modal ───────────────────────────────────────────────────────────────────
function openModal(id)  { const e = document.getElementById(id); if (e) e.classList.add('open'); }
function closeModal(id) { const e = document.getElementById(id); if (e) e.classList.remove('open'); }
document.addEventListener('click', e => { if (e.target.classList.contains('modal-overlay')) e.target.classList.remove('open'); });
document.addEventListener('keydown', e => { if (e.key === 'Escape') document.querySelectorAll('.modal-overlay.open').forEach(m => m.classList.remove('open')); });

// ── Tabs ────────────────────────────────────────────────────────────────────
function switchTab(tabId, btn) {
    document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
    const t = document.getElementById(tabId); if (t) t.classList.add('active');
    if (btn) { document.querySelectorAll('.nav-item,.tab-btn').forEach(b => b.classList.remove('active')); btn.classList.add('active'); }
}

// ── Countdown timer ─────────────────────────────────────────────────────────
// endTimeStr can be updated externally (anti-snipe extension) via window._auctionEndTimes[productId]
window._auctionEndTimes = window._auctionEndTimes || {};

function startCountdown(endTimeStr, elementId, productId) {
    const el = document.getElementById(elementId);
    if (!el) return;
    if (productId) window._auctionEndTimes[productId] = endTimeStr;

    function update() {
        const ets  = productId ? (window._auctionEndTimes[productId] || endTimeStr) : endTimeStr;
        const diff = new Date(ets).getTime() - Date.now();
        if (diff <= 0) {
            el.innerHTML = '<span class="timer-ended">Auction Ended</span>';
            clearInterval(iv);
            // Disable any bid buttons for this product
            if (productId) disableBidControls(productId);
            return;
        }
        const d = Math.floor(diff/86400000), h = Math.floor((diff%86400000)/3600000),
              m = Math.floor((diff%3600000)/60000),  s = Math.floor((diff%60000)/1000);
        let txt = '';
        if (d > 0) txt += d + 'd ';
        if (h > 0) txt += h + 'h ';
        txt += m + 'm ' + s + 's';
        const color = diff < 60000 ? '#E74C3C' : diff < 3600000 ? '#E67E22' : '#C9A84C';
        // Flash red in last 10 seconds (anti-snipe window indicator)
        const cls = diff < 10000 ? 'timer-snipe' : '';
        el.innerHTML = `<span style="color:${color};font-weight:600;" class="${cls}">${txt}</span> remaining`;
    }
    update();
    const iv = setInterval(update, 1000);
}

function disableBidControls(productId) {
    const btn = document.getElementById('bidBtn_' + productId);
    if (btn) { btn.disabled = true; btn.textContent = 'Auction Ended'; }
    const inp = document.getElementById('bidAmount_' + productId);
    if (inp) inp.disabled = true;
    const panel = document.getElementById('incrementPanel_' + productId);
    if (panel) panel.style.opacity = '0.4';
}

// ── Feature 1 & 2: Increment buttons ────────────────────────────────────────
/**
 * Renders increment buttons inside #incrementPanel_{productId}.
 * Called on page load AND after each successful bid / poll.
 * buttons: [{label:"+₹500", amount:15500}, ...]
 */
function parseAmount(value) {
    if (!value && value !== 0) return NaN;
    if (typeof value === 'number') return value;
    const cleaned = String(value).trim().replace(/[^0-9.-]/g, '');
    return cleaned === '' ? NaN : Number(cleaned);
}

function updateBidDisplay(productId, bidValue) {
    const bidEl = document.getElementById('currentBid_' + productId);
    if (bidEl) {
        bidEl.textContent = '₹' + Number(bidValue).toLocaleString('en-IN', {minimumFractionDigits:2, maximumFractionDigits:2});
    }
    const inp = document.getElementById('bidAmount_' + productId);
    if (inp) {
        inp.value = Number(bidValue).toFixed(2);
    }
}

function renderIncrementButtons(productId, buttons, contextPath) {
    const panel = document.getElementById('incrementPanel_' + productId);
    if (!panel) return;
    panel.innerHTML = '';
    if (!buttons || buttons.length === 0) return;

    buttons.forEach(btn => {
        const b = document.createElement('button');
        b.className = 'btn btn-increment';
        b.textContent = btn.label;
        const step = Number(btn.step || 0);
        b.title = 'Add ₹' + Number(step).toLocaleString('en-IN', {minimumFractionDigits:0});
        b.onclick = () => {
            const newBid = incrementBidState(productId, step);
            updateBidDisplay(productId, newBid);
            showToast(`Bid updated to ₹${Number(newBid).toLocaleString('en-IN')}`, 'info');
        };
        panel.appendChild(b);
    });
}

// ── Feature 1: Place bid via AJAX ────────────────────────────────────────────
function placeBid(productId, contextPath) {
    const bidAmount = getCurrentBidState(productId);
    if (isNaN(bidAmount) || bidAmount <= 0) {
        showToast('Bid amount is invalid. Please try again.', 'error');
        return;
    }

    const btn = document.getElementById('bidBtn_' + productId);
    if (btn) { btn.disabled = true; btn.textContent = 'Placing…'; }

    const params = new URLSearchParams();
    params.append('productId', productId);
    params.append('bidAmount', bidAmount.toFixed(2));

    fetch(contextPath + '/buyer/placeBid', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: params
    })
        .then(r => { if (!r.ok) throw new Error('HTTP ' + r.status); return r.json(); })
        .then(data => {
            if (data.success) {
                showToast(data.message, 'success');
                refreshBidStatus(productId, contextPath);
            } else {
                showToast(data.message || 'Bid failed.', 'error');
            }
        })
        .catch(err => { console.error(err); showToast('Network error. Please try again.', 'error'); })
        .finally(() => { if (btn) { btn.disabled = false; btn.textContent = 'Bid Now'; } });
}

// ── Helper: Increment and place bid in one click ──────────────────────────────
function incrementBidAndPlace(productId, increment, contextPath) {
    const newBid = incrementBidState(productId, increment);
    updateBidDisplay(productId, newBid);
    showToast(`Bid updated to ₹${Number(newBid).toLocaleString('en-IN')}`, 'info');
    setTimeout(() => placeBid(productId, contextPath), 300);
}

// ── Feature 4 & 2: AJAX poll — refresh bid + rebuild increment buttons ────────
function refreshBidStatus(productId, contextPath) {
    fetch(contextPath + '/auction/status?productId=' + productId)
        .then(r => r.json())
        .then(data => {
            if (data.error) return;

            // Update current bid display
            const bidEl = document.getElementById('currentBid_' + productId);
            if (bidEl && data.currentBid !== undefined) {
                bidEl.textContent = '₹' + parseFloat(data.currentBid)
                    .toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
            }

            // Update minimum on free-form input
            const inp = document.getElementById('bidAmount_' + productId);
            if (inp && data.minIncrement) {
                inp.min         = parseFloat(data.currentBid) + parseFloat(data.minIncrement);
                inp.placeholder = '> ₹' + parseFloat(data.currentBid).toLocaleString('en-IN');
            }

            // Re-render increment buttons with fresh amounts
            if (data.buttons) {
                renderIncrementButtons(productId, data.buttons, contextPath);
            }

            // If auction ended, disable controls
            if (data.status !== 'active') disableBidControls(productId);

            // Update timer end time in case of anti-snipe extension
            if (data.endTime && window._auctionEndTimes) {
                window._auctionEndTimes[productId] = data.endTime;
            }
        })
        .catch(() => { /* silent on network error */ });
}

// Poll all visible products every 8 seconds
function startAutoRefresh(productIds, contextPath) {
    if (!productIds || productIds.length === 0) return;
    setInterval(() => productIds.forEach(id => refreshBidStatus(id, contextPath)), 8000);
}

// ── Feature 3: Auto-bid (proxy bid) ──────────────────────────────────────────
function saveAutoBid(productId, contextPath) {
    const inp = document.getElementById('autoBidMax_' + productId);
    const max = inp ? inp.value.trim() : '';
    if (!max || isNaN(max) || parseFloat(max) <= 0) {
        showToast('Enter a valid maximum bid amount.', 'error'); return;
    }
    const btn = document.getElementById('autoBidBtn_' + productId);
    if (btn) { btn.disabled = true; btn.textContent = 'Saving…'; }

    const params = new URLSearchParams();
    params.append('productId', productId);
    params.append('maxAmount', max);

    fetch(contextPath + '/buyer/autoBid', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: params
    })
        .then(r => r.json())
        .then(data => {
            showToast(data.message, data.success ? 'success' : 'error');
            if (data.success) {
                if (inp) inp.value = '';
                refreshBidStatus(productId, contextPath);
                closeModal('autoBidModal_' + productId);
            }
        })
        .catch(err => { console.error(err); showToast('Network error.', 'error'); })
        .finally(() => { if (btn) { btn.disabled = false; btn.textContent = 'Set Auto-Bid'; } });
}

// ── Feature 9: Notifications bell ────────────────────────────────────────────
let _notifInterval = null;

function initNotifications(contextPath) {
    updateNotificationBell(contextPath);
    _notifInterval = setInterval(() => updateNotificationBell(contextPath), 20000);
}

function updateNotificationBell(contextPath) {
    fetch(contextPath + '/notifications')
        .then(r => r.json())
        .then(data => {
            const badge = document.getElementById('notifBadge');
            if (badge) {
                badge.textContent = data.unread > 0 ? data.unread : '';
                badge.style.display = data.unread > 0 ? 'flex' : 'none';
            }
            renderNotificationDropdown(data.notifications || [], contextPath);
        })
        .catch(() => {});
}

function renderNotificationDropdown(notifications, contextPath) {
    const list = document.getElementById('notifList');
    if (!list) return;
    if (notifications.length === 0) {
        list.innerHTML = '<div class="notif-empty">No notifications</div>'; return;
    }
    list.innerHTML = notifications.slice(0, 8).map(n => `
        <div class="notif-item${n.read ? '' : ' notif-unread'}" onclick="markNotifRead(${n.id},'${contextPath}')">
            <span class="notif-icon">${n.icon}</span>
            <div class="notif-body">
                <div class="notif-msg">${escapeHtml(n.msg)}</div>
                <div class="notif-time">${n.time}</div>
            </div>
        </div>`).join('');
}

function toggleNotifPanel() {
    const panel = document.getElementById('notifPanel');
    if (panel) panel.classList.toggle('open');
}

function markAllNotifRead(contextPath) {
    fetch(contextPath + '/notifications', {
        method: 'POST',
        body: new URLSearchParams({ action: 'markAllRead' })
    }).then(() => updateNotificationBell(contextPath));
}

function markNotifRead(id, contextPath) {
    fetch(contextPath + '/notifications', {
        method: 'POST',
        body: new URLSearchParams({ action: 'markRead', id })
    }).then(() => updateNotificationBell(contextPath));
}

// Close notification panel when clicking outside
document.addEventListener('click', e => {
    const panel = document.getElementById('notifPanel');
    const bell  = document.getElementById('notifBell');
    if (panel && bell && !bell.contains(e.target) && !panel.contains(e.target)) {
        panel.classList.remove('open');
    }
});

// ── Seller form validation ────────────────────────────────────────────────────
function validateProductForm() {
    const s = document.getElementById('auctionStartTime');
    const e = document.getElementById('auctionEndTime');
    const p = document.getElementById('basePrice');
    if (!s.value || !e.value) { showToast('Fill in auction start and end times.', 'error'); return false; }
    if (new Date(e.value) <= new Date(s.value)) { showToast('End time must be after start time.', 'error'); return false; }
    if (parseFloat(p.value) <= 0 || isNaN(parseFloat(p.value))) { showToast('Base price must be > 0.', 'error'); return false; }
    return true;
}

// ── Image preview ─────────────────────────────────────────────────────────────
function previewImage(input) {
    const preview = document.getElementById('imagePreview');
    if (!preview) return;
    if (input.files && input.files[0]) {
        const reader = new FileReader();
        reader.onload = e => {
            preview.src = e.target.result;
            preview.style.display = 'block';
        };
        reader.readAsDataURL(input.files[0]);
    }
}

// ── Search form: prevent empty submit ────────────────────────────────────────
function submitSearch(formId) {
    const form = document.getElementById(formId);
    const q    = form ? form.querySelector('[name="q"]') : null;
    if (q && q.value.trim() === '') { q.focus(); showToast('Enter a search term.', 'error'); return false; }
    return true;
}

// ── Utilities ─────────────────────────────────────────────────────────────────
function escapeHtml(s) {
    return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}
