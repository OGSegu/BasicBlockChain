FROM gradle:7-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle jar

FROM openjdk:17
RUN mkdir /app
COPY --from=build /home/gradle/src/build/libs/BasicBlockChain-1.0-SNAPSHOT.jar /app/BasicBlockChain-1.0-SNAPSHOT.jar