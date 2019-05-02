// This stuff populates the type drop down menu.

var works = ["headline-to-skill", "hyperonym", "main-headline-to-headline", "meronym", "related-to"];

const url = '/relation/types';

let dropdown = document.getElementById('relation-type');
dropdown.length = 0;

let defaultOption = document.createElement('option');
defaultOption.text = 'Choose relation type';


dropdown.add(defaultOption);
dropdown.selectedIndex = 0;


fetch(url)
    .then(
        function (response) {
            if (response.status !== 200) {
                console.warn('Looks like there was a problem. Status Code: ' +
                    response.status);
                return;
            }

            // Examine the text in the response
            response.json().then(function (data) {
                let option;

                for (let i = 0; i < data.length; i++) {
                    option = document.createElement('option');
                    option.text = data[i];
                    option.value = data[i];
                    if(! works.includes(data[i])) {
                        option.disabled = true;
                    }
                    if(data[i] == "hyperonym") {
                        option.text += " (slow \uD83D\uDC0C)";
                    }
                    dropdown.add(option);
                }
            });
        }
    )
    .catch(function (err) {
        console.error('Fetch Error -', err);
    });
