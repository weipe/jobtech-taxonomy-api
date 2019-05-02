#!/bin/bash


## 1 = only download for relations with no existing cache. 0 = download all.
ONLY_NEW=1


if [ "$ONLY_NEW" == 0 ]; then
    echo "**** moving existing caches to /tmp" >&2
    mv cached_*.json /tmp
fi


for T in $(curl http://127.0.0.1:4444/relation/types 2>/dev/null | tr -d '[\[\]"]' | tr ',' ' '); do
    FNAME=cached_"$T".json
    if [ -e "$FNAME" ] && [ "$ONLY_NEW" == 1 ]; then
        echo "**** skipping existing file $FNAME" >&2
    else
        echo "**** downloading $T" >&2
        time curl  http://127.0.0.1:4444/relation/graph?relation-type="$T" > "$FNAME"
    fi
done
