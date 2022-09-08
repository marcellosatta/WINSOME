#!/bin/bash

javac -d . \
-cp ./:lib/jackson-annotations-2.9.7.jar:lib/jackson-core-2.9.7.jar:lib/jackson-databind-2.9.7.jar \
src/server/*.java \
src/client/*.java \
src/common/*.java \
configFile/src/*.java
