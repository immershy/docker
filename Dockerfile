FROM alpine:3.4
RUN apk --update add openjdk7-jre
WORKDIR .

ADD ./target/docker.jar docker.jar

EXPOSE 8888

CMD java -jar docker.jar
