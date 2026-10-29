/**
 * COMPLYMIND AI — Frontend Client Scripts & Micro-Interactions
 * SIH26262 AICTE Approval Workflow Automation
 */

window.ComplyMind = window.ComplyMind || {};

document.addEventListener('DOMContentLoaded', function () {
    // 1. Initial page loaded animation class
    document.body.classList.add('page-loaded');

    // 2. Animate numerical counters on Stat Cards
    initNumberCountUp();

    // 3. Auto-dismiss flash messages after 6 seconds
    initFlashAutoDismiss();

    // 4. Form double submission prevention
    initFormSubmitGuard();

    // 5. Initialize Toast Container if absent
    ensureToastContainer();

    // 6. Interactive Spotlight Cursor VFX on Cards
    initCardSpotlightVFX();

    // 7. Keyboard accessibility
    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') {
            const sidebar = document.getElementById('mainSidebar');
            const overlay = document.getElementById('sidebarOverlay');
            if (sidebar && sidebar.classList.contains('sidebar-mobile-open')) {
                sidebar.classList.remove('sidebar-mobile-open');
                if (overlay) overlay.classList.remove('active');
            }
        }
    });
});

/**
 * Animate numbers from 0 to their value smoothly
 */
function initNumberCountUp() {
    const counters = document.querySelectorAll('.stat-value[data-count]');
    counters.forEach(counter => {
        const rawTarget = counter.getAttribute('data-count');
        const target = parseFloat(rawTarget);
        if (isNaN(target)) return;

        const isDecimal = rawTarget.includes('.');
        const duration = 750; // ms
        const startTime = performance.now();

        function updateNumber(currentTime) {
            const elapsed = currentTime - startTime;
            const progress = Math.min(elapsed / duration, 1);
            // Ease out cubic
            const easeOut = 1 - Math.pow(1 - progress, 3);
            const current = target * easeOut;

            counter.innerText = isDecimal ? current.toFixed(1) : Math.floor(current);

            if (progress < 1) {
                requestAnimationFrame(updateNumber);
            } else {
                counter.innerText = rawTarget;
            }
        }

        requestAnimationFrame(updateNumber);
    });
}

/**
 * Auto dismiss bootstrap alerts
 */
function initFlashAutoDismiss() {
    const alerts = document.querySelectorAll('.alert-dismissible');
    alerts.forEach(alert => {
        setTimeout(() => {
            alert.classList.remove('show');
            setTimeout(() => alert.remove(), 250);
        }, 5500);
    });
}

/**
 * Prevent multiple clicks and double submissions
 */
function initFormSubmitGuard() {
    document.querySelectorAll('form').forEach(form => {
        form.addEventListener('submit', function (e) {
            const submitBtn = this.querySelector('button[type="submit"]');
            if (submitBtn && !submitBtn.disabled) {
                submitBtn.classList.add('btn-loading');
                submitBtn.setAttribute('data-original-html', submitBtn.innerHTML);
                submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-1" role="status" aria-hidden="true"></span> Processing...';
                // Reset after 10 seconds in case of client validation hold
                setTimeout(() => {
                    submitBtn.classList.remove('btn-loading');
                    const orig = submitBtn.getAttribute('data-original-html');
                    if (orig) submitBtn.innerHTML = orig;
                }, 10000);
            }
        });
    });
}

/**
 * Toast Notification System
 */
function ensureToastContainer() {
    if (!document.getElementById('cmToastContainer')) {
        const container = document.createElement('div');
        container.id = 'cmToastContainer';
        container.className = 'position-fixed bottom-0 end-0 p-3';
        container.style.zIndex = '9999';
        document.body.appendChild(container);
    }
}

window.ComplyMind.toast = function (message, type = 'info') {
    ensureToastContainer();
    const container = document.getElementById('cmToastContainer');

    const toast = document.createElement('div');
    toast.className = `toast-cm toast-${type}`;

    let icon = 'bi-info-circle-fill text-primary';
    if (type === 'success') icon = 'bi-check-circle-fill text-success';
    if (type === 'error') icon = 'bi-exclamation-triangle-fill text-danger';
    if (type === 'warning') icon = 'bi-exclamation-octagon-fill text-warning';

    toast.innerHTML = `
        <i class="bi ${icon} fs-5"></i>
        <div class="flex-grow-1">${message}</div>
        <button type="button" class="btn-close btn-close-sm" aria-label="Close" style="font-size:0.75rem;"></button>
    `;

    toast.querySelector('.btn-close').addEventListener('click', () => {
        toast.remove();
    });

    container.appendChild(toast);

    setTimeout(() => {
        toast.style.transition = 'opacity 0.3s ease, transform 0.3s ease';
        toast.style.opacity = '0';
        toast.style.transform = 'translateX(100%)';
        setTimeout(() => toast.remove(), 300);
    }, 4500);
};

/**
 * Mobile Sidebar toggles
 */
window.openSidebar = function () {
    const sidebar = document.getElementById('mainSidebar');
    const overlay = document.getElementById('sidebarOverlay');
    if (sidebar) sidebar.classList.add('sidebar-mobile-open');
    if (overlay) overlay.classList.add('active');
};

window.closeSidebar = function () {
    const sidebar = document.getElementById('mainSidebar');
    const overlay = document.getElementById('sidebarOverlay');
    if (sidebar) sidebar.classList.remove('sidebar-mobile-open');
    if (overlay) overlay.classList.remove('active');
};

/**
 * Interactive Mouse Spotlight Tracker on Cards
 */
function initCardSpotlightVFX() {
    const cards = document.querySelectorAll('.card-cm, .stat-card');
    cards.forEach(card => {
        card.addEventListener('mousemove', function (e) {
            const rect = this.getBoundingClientRect();
            const x = e.clientX - rect.left;
            const y = e.clientY - rect.top;
            this.style.setProperty('--spotlight-x', `${x}px`);
            this.style.setProperty('--spotlight-y', `${y}px`);
        });
    });
}
