FROM ubuntu:22.04




ARG DEBIAN_FRONTEND=noninteractive
ENV TZ=UTC
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

RUN apt-get update && \
    apt-get install -y maven openjdk-17-jdk

WORKDIR /jmap
RUN mkdir -p /jmap/data
WORKDIR /jmap/group


ENV HOME=/home
RUN mkdir -p $HOME

COPY pom.xml pom.xml
RUN mkdir -p backend
COPY backend/pom.xml backend/pom.xml
RUN mkdir -p shared
COPY shared/pom.xml shared/pom.xml
RUN mkdir -p middleware
COPY middleware/pom.xml middleware/pom.xml

RUN mvn clean dependency:go-offline
COPY . .
RUN mvn clean install



