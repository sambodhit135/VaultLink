// src/main/resources/static/js/utils.js

/**
 * Creates and displays a Bootstrap alert that auto-dismisses
 * @param {string} message - The message to display
 * @param {string} type - "success", "danger", "warning", or "info"
 */
function showAlert(message, type = "success") {
    const alertContainer = document.getElementById("alertContainer");
    
    // Create container if it doesn't exist
    if (!alertContainer) {
        const div = document.createElement("div");
        div.id = "alertContainer";
        document.body.appendChild(div);
    }

    const alertId = "alert-" + Date.now();
    const alertHtml = `
        <div id="${alertId}" class="alert alert-${type} alert-dismissible fade show shadow-sm" role="alert">
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
    `;

    document.getElementById("alertContainer").insertAdjacentHTML('beforeend', alertHtml);

    // Auto dismiss after 4 seconds
    setTimeout(() => {
        const alertElement = document.getElementById(alertId);
        if (alertElement) {
            alertElement.classList.remove('show');
            setTimeout(() => alertElement.remove(), 150); // wait for fade transition
        }
    }, 4000);
}

/**
 * Toggles a full-screen loading overlay
 * @param {boolean} show 
 */
function showLoading(show = true) {
    let overlay = document.getElementById("loadingOverlay");
    
    // Create if it doesn't exist
    if (!overlay && show) {
        const overlayHtml = `
            <div id="loadingOverlay">
                <div class="loading-spinner"></div>
                <h4 class="mt-3 text-primary">Loading...</h4>
            </div>
        `;
        document.body.insertAdjacentHTML('beforeend', overlayHtml);
        overlay = document.getElementById("loadingOverlay");
    }

    if (overlay) {
        overlay.style.display = show ? "flex" : "none";
    }
}

/**
 * Formats "YYYY-MM-DD" into "DD MMM YYYY"
 * Example: "2025-01-15" -> "15 Jan 2025"
 */
function formatDate(dateString) {
    if (!dateString) return "N/A";
    const options = { day: 'numeric', month: 'short', year: 'numeric' };
    const date = new Date(dateString);
    // Adjust for timezone issues if dateString is ISO
    const userTimezoneOffset = date.getTimezoneOffset() * 60000;
    const adjustedDate = new Date(date.getTime() + userTimezoneOffset);
    return adjustedDate.toLocaleDateString('en-GB', options);
}

/**
 * Converts numeric days left into a human-readable string
 */
function daysUntilText(days) {
    if (days < 0) return "Expired";
    if (days === 0) return "Expires Today!";
    if (days === 1) return "1 day left";
    return `${days} days left`;
}

/**
 * Returns the HTML badge markup based on status
 */
function getStatusBadge(status) {
    if (!status) return "";
    const upperStatus = status.toUpperCase();
    
    switch (upperStatus) {
        case 'CRITICAL':
            return '<span class="badge badge-critical">Critical</span>';
        case 'WARNING':
            return '<span class="badge badge-warning">Warning</span>';
        case 'SAFE':
            return '<span class="badge badge-safe">Safe</span>';
        case 'EXPIRED':
            return '<span class="badge badge-expired">Expired</span>';
        default:
            return `<span class="badge bg-secondary">${status}</span>`;
    }
}

/**
 * Returns the CSS class name for a card border based on status
 */
function getStatusCardClass(status) {
    if (!status) return "";
    const upperStatus = status.toUpperCase();
    
    switch (upperStatus) {
        case 'CRITICAL': return 'status-card-critical';
        case 'WARNING':  return 'status-card-warning';
        case 'SAFE':     return 'status-card-safe';
        case 'EXPIRED':  return 'status-card-expired';
        default:         return '';
    }
}

/**
 * Checks if user has a token
 */
function isLoggedIn() {
    return getToken() !== null;
}

/**
 * Clears local storage and redirects to login
 */
function logout() {
    removeToken();
    localStorage.removeItem("userName");
    window.location.href = "/login.html";
}

/**
 * Retrieves the user's name from local storage
 */
function getUserName() {
    return localStorage.getItem("userName") || "User";
}
