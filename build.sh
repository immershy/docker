#!/bin/bash
p=$1
if [ -z "$p" ]
then
  mvn clean install -DskipTests
fi
docker build -t dock .
docker tag dock 172.23.31.94:5000/dock
docker push 172.23.31.94:5000/dock
