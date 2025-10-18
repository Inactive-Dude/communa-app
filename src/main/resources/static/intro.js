document.addEventListener('DOMContentLoaded', () => {
  const introVideo = document.getElementById('intro-video');
  const mainContent = document.getElementById('main-content');
  
  // --- IMPORTANT: Change this value! ---
  // Set the time (in seconds) when you want the page to start fading in.
  // For example, if your video is 5 seconds long, you might set this to 3.
  const fadeInTime = 3; 

  let hasFadedIn = false;

  // This function runs every time the video's playback time updates
  const checkTime = () => {
    if (!hasFadedIn && introVideo.currentTime >= fadeInTime) {
      mainContent.classList.add('fade-in');
      hasFadedIn = true; // Ensure this only runs once
    }
  };

  // This function runs when the video has completely finished playing
  const onVideoEnd = () => {
    // Start fading out the video
    introVideo.style.opacity = '0';
    
    // After the video has faded out, hide it completely so it can't be interacted with
    setTimeout(() => {
      introVideo.style.display = 'none';
    }, 1000); // This should match the CSS transition duration for the video
  };

  // Add the event listeners to the video
  introVideo.addEventListener('timeupdate', checkTime);
  introVideo.addEventListener('ended', onVideoEnd);
});