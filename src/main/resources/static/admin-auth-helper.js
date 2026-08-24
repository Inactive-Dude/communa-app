// admin-auth-helper.js
// Manages the admin JWT token stored in sessionStorage after admin login.

function setAdminToken(token) {
    sessionStorage.setItem('adminToken', token);
}

function getAdminToken() {
    return sessionStorage.getItem('adminToken');
}

function removeAdminToken() {
    sessionStorage.removeItem('adminToken');
}

function isAdminAuthenticated() {
    return getAdminToken() !== null;
}

/** Returns headers including the admin Bearer token. */
function getAdminAuthHeaders() {
    const token = getAdminToken();
    return {
        'Content-Type': 'application/json',
        ...(token ? { 'Authorization': `Bearer ${token}` } : {})
    };
}

function adminLogout() {
    removeAdminToken();
    sessionStorage.removeItem('adminData');
    window.location.replace('index.html');
}
