// auth.js - Protects pages that require a user to be logged in.

(function() {
  // Check for the authentication token in session storage.
  // This token is set upon successful login via auth-helper.js.
  if (!sessionStorage.getItem("authToken")) {
    // If the token does not exist, the user is not logged in.
    // Redirect them to the main index page to log in.
    window.location.replace("index.html");
  }

  // This prevents issues where a user logs out, presses the browser's back
  // button, and sees a cached version of the protected page.
  window.addEventListener("pageshow", function(event) {
    if (event.persisted) {
      // If the page was loaded from the browser's cache, force a reload
      // to re-run the authentication check.
      window.location.reload();
    }
  });
})();