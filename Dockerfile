FROM openjdk:15-jdk-alpine

ENV APPLICATION_USER ktor
RUN adduser -D -g '' $APPLICATION_USER

RUN mkdir /app
RUN mkdir /app/sql
RUN chown -R $APPLICATION_USER /app

USER $APPLICATION_USER

COPY ./build/libs/wavechain-1.0-SNAPSHOT-all.jar /app/wavechain.jar

WORKDIR /app

CMD ["java", "-server", "-XX:+UnlockExperimentalVMOptions", "-XX:InitialRAMFraction=2", "-XX:MinRAMFraction=2", "-XX:MaxRAMFraction=2", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=100", "-XX:+UseStringDeduplication","-jar", "wavechain.jar"]
