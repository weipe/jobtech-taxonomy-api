#!/bin/bash

BRANCH=test-stava

IMAGENAME=jobtech-taxonomy-api-run-tests

if [ "$1" == "build" ]; then
    docker build --build-arg branch="$BRANCH" --no-cache -f Dockerfile-run-tests -t "$IMAGENAME" .
else
    test -f ~/.aws/accessKeys.csv || { echo "setup aws first" >&2 ; exit 1 ; }
    docker run --rm -v ~/.aws:/root/.aws -i -t "$IMAGENAME" /bin/sh /tmp/runtest.sh
fi
