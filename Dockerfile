FROM clojure:openjdk-8-lein as builder

COPY . /

#COPY project.clj /
#cOPY dev-config.edn /
#COPY env /
#COPY Capstanfile /
#COPY env /
# COPY log /
# COPY Procfile /
# COPY README.md /
# COPY src /
# COPY resources /
# COPY test /
# COPY test-config.edn /



RUN pwd

WORKDIR /

RUN lein uberjar

FROM openjdk:8-alpine

COPY --from=builder target/uberjar/jobtech-taxonomy-api.jar /jobtech-taxonomy-api/app.jar

EXPOSE 3000

RUN mkdir -p /.clj-nativedep/jobtech-nlp-stava/0.1.0/linux-amd64 &&\
        cd /.clj-nativedep/jobtech-nlp-stava/0.1.0/linux-amd64 &&\
        unzip /jobtech-taxonomy-api/app.jar libstava.so

CMD ["java", "-Djava.library.path=/.clj-nativedep/jobtech-nlp-stava/0.1.0/linux-amd64", "-jar", "/jobtech-taxonomy-api/app.jar"]
