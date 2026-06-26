// ── Gestión de sesión y helpers compartidos ──

const API = '';  // mismo origen

function saveSession(data) {
    localStorage.setItem('token', data.token);
    localStorage.setItem('email', data.email);
    localStorage.setItem('nombre', data.nombre);
    localStorage.setItem('roles', JSON.stringify(data.roles || []));
}

function getToken()   { return localStorage.getItem('token'); }
function getNombre()  { return localStorage.getItem('nombre'); }
function getEmail()   { return localStorage.getItem('email'); }
function getRoles()   { return JSON.parse(localStorage.getItem('roles') || '[]'); }
function isAdmin()    { return getRoles().includes('ADMIN'); }

function logout() {
    localStorage.clear();
    window.location.href = 'login.html';
}

// Redirige a login si no hay token
function requireAuth() {
    if (!getToken()) {
        window.location.href = 'login.html';
    }
}

// fetch con token incluido
async function apiFetch(url, options = {}) {
    options.headers = options.headers || {};
    options.headers['Content-Type'] = 'application/json';
    const token = getToken();
    if (token) options.headers['Authorization'] = 'Bearer ' + token;

    const res = await fetch(API + url, options);

    // Token expirado o inválido
    if (res.status === 401) {
        logout();
        throw new Error('Sesión expirada');
    }
    return res;
}

// ── Toast ──
function showToast(ok, msg) {
    let t = document.getElementById('toast');
    if (!t) {
        t = document.createElement('div');
        t.id = 'toast';
        document.body.appendChild(t);
    }
    t.className = ok ? 'success' : 'error';
    t.textContent = (ok ? 'OK' : 'NO') + msg;
    t.style.display = 'block';
    setTimeout(() => { t.style.display = 'none'; }, 3500);
}
