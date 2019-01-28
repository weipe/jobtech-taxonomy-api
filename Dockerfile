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

CMD ["java", "-jar", "/jobtech-taxonomy-api/app.jar"]
