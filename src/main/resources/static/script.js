// Wait for the entire HTML document to be fully loaded and parsed
document.addEventListener("DOMContentLoaded", () => {
  // Select all necessary elements from the DOM
  const wrapper = document.querySelector('.wrapper');
  const btnPopup = document.querySelector('.btnLogin-popup');
  const iconClose = document.querySelector('.icon-close');
  const loginLink = document.querySelector('.login-link');
  const registerLink = document.querySelector('.register-link');
  const loginForm = document.querySelector('.form-box.login form');
  const registerForm = document.querySelector('.form-box.register form');

  // Event listener to show the popup when the login button is clicked
  if (btnPopup) {
    btnPopup.addEventListener('click', () => {
      wrapper.classList.add('active-popup');
    });
  }

  // Event listener to hide the popup when the close icon is clicked

  // Event listener to switch to the register form
  if (registerLink) {
    registerLink.addEventListener('click', (e) => {
      e.preventDefault(); // Prevent the link from navigating
      wrapper.classList.add('active');
    });
  }

  // Event listener to switch back to the login form
  if (loginLink) {
    loginLink.addEventListener('click', (e) => {
      e.preventDefault(); // Prevent the link from navigating
      wrapper.classList.remove('active');
    });
  }

  // Handle the login form submission
  if (loginForm) {
    loginForm.addEventListener('submit', (e) => {
      e.preventDefault(); // Stop the form from submitting the traditional way
      console.log("Login form submitted. Redirecting...");
      // For this demo, we just redirect to the profile page
      window.location.href = "Profile.html";
    });
  }

  // Handle the registration form submission for password validation
  if (registerForm) {
    registerForm.addEventListener('submit', (e) => {
      // Select the password fields within the registration form
      const password = registerForm.querySelector('.password').value;
      const confirmPassword = registerForm.querySelector('.confirm-password').value;

      // Check if the passwords match
      if (password !== confirmPassword) {
        alert("Passwords do not match. Please try again.");
        e.preventDefault(); // Stop the form submission if passwords don't match
      } else {
        console.log("Registration form submitted successfully.");
        // In a real application, you would send this data to a server.
        // For this demo, we can just show a success message.
        alert("Registration successful!");
      }
    });
  }
});

