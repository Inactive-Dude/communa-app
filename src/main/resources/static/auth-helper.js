// auth-helper.js - Token management helper

function setAuthToken(token) {
    sessionStorage.setItem('authToken', token);
}

function getAuthToken() {
    return sessionStorage.getItem('authToken');
}

function removeAuthToken() {
    sessionStorage.removeItem('authToken');
}

function setUserData(userData) {
    sessionStorage.setItem('userData', JSON.stringify(userData));
}

function getUserData() {
    const data = sessionStorage.getItem('userData');
    return data ? JSON.parse(data) : null;
}

function clearUserData() {
    sessionStorage.removeItem('userData');
}

function isAuthenticated() {
    return getAuthToken() !== null;
}

function getAuthHeaders() {
    const token = getAuthToken();
    return token ? { 'Authorization': `Bearer ${token}` } : {};
}

function logout() {
    removeAuthToken();
    clearUserData();
    window.location.replace('index.html');
}