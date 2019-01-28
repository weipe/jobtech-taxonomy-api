# jobtech-taxonomy-api

generated using Luminus version "3.10.29"

FIXME

## Prerequisites

You will need [Leiningen][1] 2.0 or above installed.

[1]: https://github.com/technomancy/leiningen

## Running

To start a web server for the application, run (for example, on port 4444):

    lein run -p 4444

Then open the following URL in a web browser:

    http://127.0.0.1:4444/taxonomy/swagger-ui

## Controlling from nREPL

To start the HTTP server and any other components such as databases, run the start function:

    (start)

or with a port number:

    (start-app ["-p" "4444"])

Run the following command to start the HTTP server:

    (mount/start #'jobtech-taxonomy-api.core/http-server)

## License

GPLv3

Copyright © 2019 Jobtech
