# jobtech-taxonomy-api

generated using Luminus version "3.10.29"

FIXME

## Prerequisites

You will need [Leiningen][1] 2.0 or above installed.

[1]: https://github.com/technomancy/leiningen

## Running

You can either run from your terminal or from repl

## Controlling from Terminal

Go to project the folder .../jobtech-taxonomy-api

You can either use a web server or a local Datomic.

To start a web server for the application, run this
to connect to AWS Datomic:

    lein run -p 4444

To use a local Datomic, run:

    lein with-profile local run -p 4444


## Controlling from nREPL

Go to project (somewhere in the folder, doesn't matter where, WE THINK) and start your repl (If IntelliJ, dont' forget to  load project in your repl).

To start the HTTP server and any other components such as databases, run the start function:

    (start-app ["-p" "4444"])

(you can just do _(start)_, but the above command with port number is preferred)

Run the following command to start the HTTP server:

    (mount/start #'jobtech-taxonomy-api.core/http-server)
    
## NExt step
Then open the following URL in a web browser:

    http://127.0.0.1:4444/taxonomy/swagger-ui

## Running a query

    curl -X GET -H  "api-key: 2f904e245c1f5" --header 'Accept: application/json' 'http://127.0.0.1:4444/taxonomy/public-api/term?term=Danska'

## License

GPLv3

Copyright © 2019 Jobtech
