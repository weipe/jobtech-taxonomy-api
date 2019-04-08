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
