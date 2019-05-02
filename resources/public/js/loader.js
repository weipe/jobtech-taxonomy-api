var scrollActive = 0;
var scrollSpeed  = 200;
var scrollWidth = 12;
var scrollCounter = 0;
var scrollText = "batfish loader is running...   ";
scrollText += scrollText;


function toggleLoader(state) {
    var slideSource = document.getElementById('loader');
    slideSource.classList.toggle('fade');

    scrollActive = state;

    if(state == 1) {
        setTimeout(scrollStepper, scrollSpeed);
    }
}


function scrollStepper() {
    let loading = document.getElementById('loader-text');
    let text = scrollText.substr(scrollCounter % scrollText.length/2, scrollWidth);

    loading.textContent = scrollText.substr(scrollCounter % (scrollText.length/2), scrollWidth);

    if(scrollActive == 1) {
        scrollCounter++;
        setTimeout(scrollStepper, scrollSpeed);
    }
  }