const canvas = document.getElementById('matrix-canvas');
const ctx = canvas.getContext('2d');

canvas.width = window.innerWidth;
canvas.height = window.innerHeight;

// The characters that will be raining
const chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
const fontSize = 16;
const columns = canvas.width / fontSize;

// Create an array of drops, one for each column
const drops = [];
for (let x = 0; x < columns; x++) {
    drops[x] = 1;
}

function draw() {
    // Fill the canvas with a semi-transparent black to create the fading effect
    ctx.fillStyle = "rgba(0, 0, 0, 0.05)";
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    // Set the color and font for the raining characters
    ctx.fillStyle = "#0F0"; // Green color
    ctx.font = fontSize + "px monospace";

    // Loop through the drops
    for (let i = 0; i < drops.length; i++) {
        // Get a random character from the chars string
        const text = chars.charAt(Math.floor(Math.random() * chars.length));
        
        // Draw the character
        ctx.fillText(text, i * fontSize, drops[i] * fontSize);

        // Reset the drop to the top if it goes off screen
        // Add a random element to make the rain uneven
        if (drops[i] * fontSize > canvas.height && Math.random() > 0.975) {
            drops[i] = 0;
        }

        // Move the drop down
        drops[i]++;
    }
}

// Start the animation loop
setInterval(draw, 33);

// Adjust canvas size on window resize
window.addEventListener('resize', () => {
    canvas.width = window.innerWidth;
    canvas.height = window.innerHeight;
    // Recalculate columns, but don't reset drops to avoid a jarring refresh
    const newColumns = canvas.width / fontSize;
    if (newColumns > drops.length) {
        for (let x = drops.length; x < newColumns; x++) {
            drops[x] = 1;
        }
    } else if (newColumns < drops.length) {
        drops.length = newColumns;
    }
});