FROM gradle:8-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle shadowJar

FROM openjdk:17
RUN mkdir /app
COPY --from=build /home/gradle/src/build/libs/blockchain.jar /app/blockchain.jar