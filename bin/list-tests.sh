#!/bin/bash

for F in $(find test -name '*.clj' -exec grep -q deftest {} \; -print)
do
    NS=`grep '(ns ' $F | sed 's|(ns ||'`
    for T in $(grep "deftest " $F | perl -pe 's|^.*? ||' | perl -pe 's|^\^.*? ||')
    do
        echo $NS/$T
    done
done
