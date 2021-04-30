#!/bin/bash

if [[ $# -ne 4 ]] ; then
    echo 'Please! Input 4 args: 1 - OpenApi Token 2 - Ticker Name 3 - Interval Time 4 - Boolean Is Sandbox? '
    exit 1
fi

# Set Env JAVA_HOME
export JAVA_HOME=$(readlink -f /usr/bin/java | sed "s:bin/java::")

# Build
cd ~/BigData-lab3
mvn clean package

# Run App
java -jar ~/BigData-lab3/example/target/openapi-java-sdk-example-0.6-SNAPSHOT.jar $1 $2 $3 $4 | sleep 5 | rm ~/BigData-lab3/logs/appLogs.lck
rm ~/BigData-lab3/logs/appLogs