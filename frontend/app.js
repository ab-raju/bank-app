// ── API base (Java server runs on 8080)
const BASE = '';

// ── Fetch helper
async function api(endpoint, method = 'GET', body = null) {
  try {
    const opts = { method, headers: { 'Content-Type': 'application/json' } };
    if (body) opts.body = JSON.stringify(body);
    const res  = await fetch(BASE + endpoint, opts);
    return await res.json();
  } catch (e) {
    return { success: false, message: 'Cannot reach server. Is the Java server running?' };
  }
}

// ── Session helpers
function saveSession(data)  { sessionStorage.setItem('bank', JSON.stringify(data)); }
function getSession()       { const d = sessionStorage.getItem('bank'); return d ? JSON.parse(d) : null; }
function clearSession()     { sessionStorage.removeItem('bank'); }

// ── Message helpers
function showMsg(id, text, type) {
  const el = document.getElementById(id);
  if (!el) return;
  el.textContent = text;
  el.className = 'msg ' + type;
}
function hideMsg(id) {
  const el = document.getElementById(id);
  if (el) el.className = 'msg hidden';
}
