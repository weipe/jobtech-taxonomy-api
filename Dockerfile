FROM openjdk:8-alpine as builder

COPY . /

WORKDIR /

ARG USER=docker
ARG UID=1000
ARG GID=1000
ARG PW=docker
RUN addgroup -g ${GID} ${USER} && adduser -S ${USER} -G ${USER}

RUN apk update && apk add swig openjdk8 gcc wget git bash make libc-dev sudo &&\
        wget -O /usr/local/bin/lein https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein &&\
        chmod a+rx /usr/local/bin/lein &&\
        mkdir -p /home/${USER} &&\
        chown -R ${USER}:${USER} /home/${USER}

RUN     git clone https://github.com/JobtechSwe/jobtech-nlp-stava.git &&\
        cd jobtech-nlp-stava &&\
        lein deps &&\
        lein build-lib &&\
        chown -R ${USER}:${USER} ../jobtech-nlp-stava &&\
        mkdir -p /home/${USER} && chown -R ${USER}:${USER} /home/${USER} &&\
        sudo -u ${USER} env HOME=/home/${USER} lein install &&\
        cd .. &&\
        lein uberjar



FROM openjdk:8-alpine

COPY --from=builder /target/uberjar/jobtech-taxonomy-api.jar /jobtech-taxonomy-api/app.jar

COPY --from=builder /jobtech-nlp-stava/resources /stava

EXPOSE 3000

## stupid
RUN chgrp -R 0 /jobtech-taxonomy-api /stava && \
    chmod -R g=u /jobtech-taxonomy-api /stava &&\
    mkdir -p /root/.clj-nativedep/jobtech-nlp-stava/0.1.0 &&\
    ln -s /stava /.clj-nativedep/jobtech-nlp-stava/0.1.0/linux-amd64 &&\
    ln -s /stava /root/.clj-nativedep/jobtech-nlp-stava/0.1.0/linux-amd64

CMD ["java", "-Dstava.library.path=/stava/lib/", "-Djava.library.path=/stava", "-jar", "/jobtech-taxonomy-api/app.jar"]
