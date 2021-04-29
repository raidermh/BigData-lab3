#!/bin/bash

if [[ $# -ne 4 ]] ; then
    echo 'Please! Input 4 args: 1 - OpenApi Token 2 - Ticker Name 3 - Interval Time 4 - Boolean Is Sandbox? '
    exit 1
fi

# Install JDK 11 Version
sudo yum install java-11-openjdk-devel

# Set Env JAVA_HOME
export JAVA_HOME=$(readlink -f /usr/bin/java | sed "s:bin/java::")

# Download and Install Maven
if [ ! -f apache-maven-3.6.3-bin.tar.gz ]; then
    wget https://www-eu.apache.org/dist/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz
    tar xzf apache-maven-3.6.3-bin.tar.gz
    ln -s apache-maven-3.6.3 maven
    export PATH=/opt/maven/bin:${PATH}
else
    echo "Maven already exists, skipping..."
fi



