# jobtech-taxonomy-api

generated using Luminus version "3.10.29"

FIXME

## Prerequisites

You will need [Leiningen][1] 2.0 or above installed.

[1]: https://github.com/technomancy/leiningen

You will also need to install jobtech-taxonomy-database 0.1.0-SNAPSHOT
in your local repository:

    cd <.....>/jobtech-taxonomy-database
    lein install

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


## Graph viewer

Load http://127.0.0.1:4444/graphview.html in your web browser.


## Testing
The integration test setup creates a temporary database for each test,
which makes it safe to do any modifications without leaving traces
behind.

Summary:
 - test runner: Kaocha (https://github.com/lambdaisland/kaocha).
 - test command: `lein kaocha --focus-meta TAG`
   where TAG is the name of one of the test's tags (such as `integration`).
 - status: for integration tests that rely on a live database, only one test
   can be run at a time. This means that you should assign each test a unique
   tag (e.g. `(test/deftest ^:changes-test-2 changes-test-2 ...)`), and then
   run it with `lein kaocha --focus-meta changes-test-2`.

### Howto write an integration test

#### File and namespace
Your test should reside in the directory `test/clj/jobtech_taxonomy_api/test/`.

You should either pick an existing file, or create a new file, ending
with `_test.clj`.  It should use a namespace like this: `(ns
jobtech-taxonomy-api.test.FILENAME ...)`, where FILENAME is for example
`changes-test`.

You need to require `[jobtech-taxonomy-api.test.test-utils :as util]`.

#### Define fixtures
Place one occurance of this line in your test file:
`(test/use-fixtures :each util/fixture)`.

#### Define a test which calls functions directly
Here is a simple example of a test which asserts a skill concept, and
then checks for its existence.

First, require
```
[jobtech-taxonomy-api.db.concept :as c]

```

Then write a test:
```
(test/deftest ^:concept-test-0 concept-test-0
  (test/testing "Test concept assertion."
    (c/assert-concept "skill" "cykla" "cykla")
    (let [found-concept (first (core/find-concept-by-preferred-term "cykla"))]
      (test/is (= "cykla" (get found-concept :preferredLabel))))))
```

#### Define a test which calls the Luminus REST API
Here is a simple example of a test which asserts a skill concept, and
then checks for its existence via the REST API:

First, require
```
[jobtech-taxonomy-api.db.concept :as c]

```

Then write a test:
```
(test/deftest ^:changes-test-1 changes-test-1
  (test/testing "test event stream"
    (c/assert-concept "skill" "cykla" "cykla")
    (let [[status body] (util/send-request-to-json-service
                         :get "/v0/taxonomy/public/concepts"
                         :headers [util/header-auth-user]
                         :query-params [{:key "preferredLabel", :val "cykla"}])]
      (test/is (= "cykla" (get (first body) :preferredLabel))))))
```


### Local testing vs Jenkins testing
Kaocha can only use one of either the configuration to run locally, or to run from Jenkins. The default is Jenkins.

To run locally, check your project.clj that is has the right kaocha
resource commented:

```
    :project/kaocha {:dependencies [[lambdaisland/kaocha "0.0-418"]]
                    ;; You can only comment in one resource-path:
                    :resource-paths ["env/dev/resources"] ; comment in for local use
                    ; :resource-paths ["env/integration-test/resources"] ; comment in for Jenkins
                    }
```



## Logging

By default, logging functionality is provided by the
clojure.tools.logging library. The library provides macros that
delegate to a specific logging implementation. The default
implementation used in Luminus is the logback library.

Any Clojure data structures can be logged directly.


### Examples
```
(ns example
 (:require [clojure.tools.logging :as log]))

(log/info "Hello")
=>[2015-12-24 09:04:25,711][INFO][myapp.handler] Hello

(log/debug {:user {:id "Anonymous"}})
=>[2015-12-24 09:04:25,711][DEBUG][myapp.handler] {:user {:id "Anonymous"}}
```


### Description of log levels
#### trace
#### debug
#### info
#### warn
#### error
#### fatal

### Logging of exceptions


```
(ns example
 (:require [clojure.tools.logging :as log]))

(log/error (Exception. "I'm an error") "something bad happened")
=>[2015-12-24 09:43:47,193][ERROR][myapp.handler] something bad happened
  java.lang.Exception: I'm an error
    	at myapp.handler$init.invoke(handler.clj:21)
    	at myapp.core$start_http_server.invoke(core.clj:44)
    	at myapp.core$start_app.invoke(core.clj:61)
    	...
```

### Logging backends
### Configuring logging
Each profile has its own log configuration. For example, `dev`'s
configuration is located in `env/dev/resources/logback.xml`.

It works like a standard Java log configuration, with appenders and loggers.

The default configuration logs to standard out, and to log files in log/.

## License

GPLv3

Copyright © 2019 Jobtech

## CREATE dev-config for local developement
Create the file "dev-config.edn" with this content

```
;; WARNING
;; The dev-config.edn file is used for local environment variables, such as database credentials.
;; This file is listed in .gitignore and will be excluded from version control by Git.

{:dev true
 :port 3000
 ;; when :nrepl-port is set the application starts the nREPL server on load
 :nrepl-port 7000

 ; set your dev database connection URL here
 ; :database-url "datomic:free://localhost:4334/jobtech_taxonomy_api_dev"

 ; alternatively, you can use the datomic mem db for development:
 ; :database-url "datomic:mem://jobtech_taxonomy_api_datomic_dev"
}
```
