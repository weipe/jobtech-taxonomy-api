FROM openjdk:8-alpine as builder

COPY . /

WORKDIR /

RUN apk update && apk add swig openjdk8 gcc wget git bash make libc-dev &&\
        # Get latest leiningen
        wget -O /usr/local/bin/lein https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein &&\
        chmod a+rx /usr/local/bin/lein &&\
        #
        # Fetch lein deps
        lein deps &&\
        #
        # Build native library
        # TODO: add tag to checkout, to make sure a proper, correct release is used
        git clone --depth=1 https://github.com/JobtechSwe/jobtech-nlp-stava.git &&\
        cd jobtech-nlp-stava &&\
        lein deps &&\
        lein build-lib &&\
        mv resources /stava &&\
        cd .. &&\
        rm -rf jobtech-nlp-stava &&\
        #
        lein deps &&\
        lein uberjar



FROM openjdk:8-alpine

COPY --from=builder target/uberjar/jobtech-taxonomy-api.jar /jobtech-taxonomy-api/app.jar

#COPY --from=builder /stava/ /root/.clj-nativedep/jobtech-nlp-stava/0.1.0/linux-amd64/
COPY --from=builder /stava /stava

EXPOSE 3000

#RUN chmod -R a+rx /root/.clj-nativedep/jobtech-nlp-stava/0.1.0/linux-amd64

#CMD ["java", "-Djava.library.path=/root/.clj-nativedep/jobtech-nlp-stava/0.1.0/linux-amd64", "-jar", "/jobtech-taxonomy-api/app.jar"]
CMD ["java", "-Dstava.library.path=/stava/lib/", "-Djava.library.path=/stava", "-jar", "/jobtech-taxonomy-api/app.jar"]
