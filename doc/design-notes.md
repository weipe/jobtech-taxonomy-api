Anteckningar:


Krav på klient:

Ska inte gå sönder om vi lägger på nya fält.



* Stäm av med LA om de kommer att använda transaktions API:et




Vi testar att skapa specifika endpoints för varje typ i API:et, tex

GET /occupation-name/A23_123   ->  ger ett concept av typen occupation name


Dessa endpoints är till för extern konsumption.



Vi skapar en typad eventström som har ett "envelop-object" som wrappar ett payload object som innehåller senaste versionen av conceptet som har förändrats. Vi testar att ta bort event-typen


Exemepel:
[
{:concept-type "occupation-group"
 :transaction-id 1
 :transaction-date 2019-02-01 14:00:00
 :concept { :id "A23_v42"
            :type "occupation-group"
            :deprecated false
            :ssyk-2012 "2451"
            :prefered-term "Mjukvaruutvecklare mfl"
    }
}
{:concept-type "occupation-group"
 :transaction-id 2
 :transaction-date 2019-02-01 15:00:00
 :concept { :id "A23_v42"
            :type "occupation-group"
            :deprecated true
            :ssyk-2012 "2451"
            :prefered-term "Mjukvaruutvecklare mfl"
    }
}

]



### Anteckningar efter möte med ledigt arbete

#### Vad de använder Taxonomi-tjänsterna till

* Validering av data
* Drop downlistor (Ifyllnadshjälp i skapandet av annonser)
* Behöver kunna hämta yrkesbenämningar givet yrkesgrupp
* Behöver kunna hämta yrkesgrupp givet yrkesområde
* Type ahead (Ifyllnadshjälp i skapandet av annonser)
* Översätta mellan standarder SSYK -> ISCO  för leverans till Eures
* Validera annonser som kommit in i äldre taxonomiversion via DXA. Behöver se om värdena ändrats nyligen.
* Signal om förändring av Taxonomin för att refresha cachear.


Vi måste komma med rekomendationer vilka id:n man ska använda.
Landskoder, 2-ställig, 3-ställig. Kommunkod


När man väljer Utbildninginriktning så vill man bara få förslag på giltiga utbildningsnivåer.

# Best practice

## Använd enbart Jobtech Taxonomy id:n
Använd enbart Jobtech taxonomy id:n för allt. Använd aldrig SSYK kod för att identifiera en yrkesgrupp eller en SUN-kod för att identifiera en utbildningsinriktning. Dessa är inte beständiga över tid. Översätt istället Jobtech Taxonomy id:n till SSYK vid den tidpunkt då SSYK:t behövs.

## Spara alltid id tillsammans med term / label
När man persisterar Platsannonser, CVn med mera som använder taxonomivärden bör man alltid spara id och term tillsammans. 
Det gör det lättare att hantera datat istället för att tvingas slå upp referenser. 


