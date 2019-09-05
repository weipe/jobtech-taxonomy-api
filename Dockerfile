FROM openjdk:8-alpine as builder

COPY . /

WORKDIR /

ARG USER=docker
ARG UID=1000
ARG GID=1000
ARG PW=docker
#RUN useradd -m ${USER} --uid=${UID} && echo "${USER}:${PW}" | chpasswd
RUN addgroup -g ${GID} ${USER} && adduser -S ${USER} -G ${USER}

USER ${UID}:${GID}
WORKDIR /home/${USER}

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
        git clone https://github.com/JobtechSwe/jobtech-nlp-stava.git &&\
        cd jobtech-nlp-stava &&\
        lein deps &&\
        lein build-lib &&\
        lein install &&\
        cd .. &&\
        #rm -rf jobtech-nlp-stava &&\
        #
        echo "DEBUG------------" >&2 && find / -name libstava.so &&\
        #mkdir -p /root/.clj-nativedep/jobtech-nlp-stava/0.1.0/linux-amd64 && cp -r /jobtech-nlp-stava/resources/libstava.so /jobtech-nlp-stava/resources/lib /root/.clj-nativedep/jobtech-nlp-stava/0.1.0/linux-amd64 &&\
        lein uberjar


FROM openjdk:8-alpine

ARG USER=docker
ARG UID=1000
ARG GID=1000
ARG PW=docker
RUN addgroup -g ${GID} ${USER} && adduser -S ${USER} -G ${USER}
USER ${UID}:${GID}
WORKDIR /home/${USER}


COPY --from=builder target/uberjar/jobtech-taxonomy-api.jar /jobtech-taxonomy-api/app.jar

#COPY --from=builder /root/.clj-nativedep/jobtech-nlp-stava/0.1.0/linux-amd64 /root/.clj-nativedep/jobtech-nlp-stava/0.1.0/linux-amd64
COPY --from=builder /root/.clj-nativedep/jobtech-nlp-stava/0.1.0/linux-amd64 /stava

EXPOSE 3000

#CMD ["java", "-Dstava.library.path=/stava/lib/", "-Djava.library.path=/root/.clj-nativedep/jobtech-nlp-stava/0.1.0/linux-amd64", "-jar", "-jar", "/jobtech-taxonomy-api/app.jar"]
CMD ["java", "-Dstava.library.path=/stava/lib/", "-Djava.library.path=/stava", "-jar", "/jobtech-taxonomy-api/app.jar"]
