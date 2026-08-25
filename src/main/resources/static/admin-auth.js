// admin-auth.js — Guards pages that require an ADMIN to be logged in.
// Include this script on every admin dashboard page INSTEAD OF auth.js.

(function () {
    // Verify the admin JWT token set by Index(admin).html on successful login.
    if (!sessionStorage.getItem('adminToken')) {
        // No admin token found — redirect to the admin login page.
        window.location.replace('Index(admin).html');
        return;
    }

    // Prevent the browser back-button from showing a cached admin page after logout.
    window.addEventListener('pageshow', function (event) {
        if (event.persisted) {
            // Page was served from bfcache — re-run the auth check.
            if (!sessionStorage.getItem('adminToken')) {
                window.location.replace('Index(admin).html');
            }
        }
    });
})();
