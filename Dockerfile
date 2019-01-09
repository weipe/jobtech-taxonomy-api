FROM openjdk:8-alpine

COPY target/uberjar/jobtech-taxonomy-api.jar /jobtech-taxonomy-api/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/jobtech-taxonomy-api/app.jar"]
