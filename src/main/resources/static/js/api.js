// src/main/resources/static/js/api.js

const API_BASE = "http://localhost:8080/api";

function getToken() {
    return localStorage.getItem("vaultToken");
}

function setToken(token) {
    localStorage.setItem("vaultToken", token);
}

function removeToken() {
    localStorage.removeItem("vaultToken");
}

function getHeaders() {
    return {
        "Content-Type": "application/json",
        "Authorization": "Bearer " + getToken()
    };
}

/**
 * Core API caller utility
 */
async function apiCall(method, endpoint, body = null) {
    const url = API_BASE + endpoint;
    const options = {
        method: method,
        headers: getHeaders()
    };

    if (body) {
        options.body = JSON.stringify(body);
    }

    try {
        const response = await fetch(url, options);

        if (response.status === 401) {
            // Unauthorized, clear token and redirect to login
            removeToken();
            window.location.href = "/login.html";
            return null;
        }

        // Try to parse JSON. Some endpoints might return empty body (204)
        const text = await response.text();
        const data = text ? JSON.parse(text) : {};

        if (!response.ok) {
            // Include status code so callers can handle specific errors
            data.status = response.status;
            throw data;
        }

        return data;
    } catch (error) {
        console.error(`API Error on ${method} ${endpoint}:`, error);
        throw error;
    }
}

// -------------------------------------------------------------
// Auth
// -------------------------------------------------------------

function login(email, password) {
    return apiCall("POST", "/auth/login", { email, password });
}

function register(firstName, lastName, email, password) {
    return apiCall("POST", "/auth/register", { firstName, lastName, email, password });
}

function changePassword(currentPassword, newPassword, confirmPassword) {
    return apiCall("PUT", "/auth/change-password", { currentPassword, newPassword, confirmPassword });
}

// -------------------------------------------------------------
// Documents
// -------------------------------------------------------------

function getAllDocuments() {
    return apiCall("GET", "/documents");
}

function getDocument(id) {
    return apiCall("GET", "/documents/" + id);
}

function createDocument(data) {
    return apiCall("POST", "/documents", data);
}

function updateDocument(id, data) {
    return apiCall("PUT", "/documents/" + id, data);
}

function deleteDocument(id) {
    return apiCall("DELETE", "/documents/" + id);
}

function getExpiryDashboard() {
    return apiCall("GET", "/documents/expiry/summary");
}

function getDocumentsByStatus(status) {
    return apiCall("GET", "/documents/status/" + status);
}

function shareDocument(id, email, hours) {
    return apiCall("POST", "/documents/" + id + "/share", { sharedWithEmail: email, expiresInHours: hours });
}

// -------------------------------------------------------------
// Categories
// -------------------------------------------------------------

async function getAllCategories() {
    try {
        const response = await fetch(
            API_BASE + '/categories', {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
                // No Authorization header needed
                // GET categories is public
            }
        });
        return await response.json();
    } catch (error) {
        console.error('Categories fetch error:', error);
        return [];
    }
}

function createCategory(name, description) {
    return apiCall("POST", "/categories", { name, description });
}

// -------------------------------------------------------------
// Notifications
// -------------------------------------------------------------

function getNotifications() {
    return apiCall("GET", "/notifications");
}

function triggerExpiryCheck() {
    return apiCall("POST", "/notifications/trigger-check");
}
