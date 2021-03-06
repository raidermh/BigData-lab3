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

# Download adn Install Elasticsearch
if [ ! -f elasticsearch-7.3.2-x86_64.rpm ]; then
    wget https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-7.3.2-x86_64.rpm
    rpm -ivh elasticsearch-*
    systemctl enable elasticsearch
    systemctl start elasticsearch
else
    echo "Elasticsearch already exists, skipping..."
fi

# Download adn Install Kibana
if [ ! -f kibana-7.3.2-x86_64.rpm ]; then
    wget https://artifacts.elastic.co/downloads/kibana/kibana-7.3.2-x86_64.rpm
    rpm -ivh kibana-*
    cp ~/BigData-lab3/example/configs/kibana/kibana.yml /etc/kibana/kibana.yml
    systemctl enable kibana
    systemctl start kibana
else
    echo "Kibana already exists, skipping..."
fi

# Download adn Install Logstash
if [ ! -f logstash-7.3.2.rpm ]; then
    wget https://artifacts.elastic.co/downloads/logstash/logstash-7.3.2.rpm
    rpm -ivh logstash-*
    cp ~/BigData-lab3/example/configs/logstash/input.conf /etc/logstash/conf.d/input.conf
    cp ~/BigData-lab3/example/configs/logstash/filter.conf /etc/logstash/conf.d/filter.conf
    cp ~/BigData-lab3/example/configs/logstash/output.conf /etc/logstash/conf.d/output.conf
    systemctl enable logstash
    systemctl start logstash
else
    echo "Logstash already exists, skipping..."
fi

# Download adn Install Filebeat
if [ ! -f filebeat-7.3.2-x86_64.rpm ]; then
    wget https://artifacts.elastic.co/downloads/beats/filebeat/filebeat-7.3.2-x86_64.rpm
    rpm -ivh filebeat-*
    cp ~/BigData-lab3/example/configs/filebeat/filebeat.yml /etc/filebeat/filebeat.yml
    systemctl enable filebeat
    systemctl start filebeat
else
    echo "Filebeat already exists, skipping..."
fi

# Build
cd ~/BigData-lab3
mvn clean package

# Run App
java -jar ~/BigData-lab3/example/target/openapi-java-sdk-example-0.6-SNAPSHOT.jar $1 $2 $3 $4
rm ~/BigData-lab3/logs/appLogs.lck




