FROM gradle:7.4.0-jdk17 AS builder
LABEL authors="sin-wongyun"

WORKDIR /gateway

COPY . .

RUN gradle clean build --no-daemon

FROM adoptopenjdk/openjdk17:jdk-17.0.2_8-alpine

WORKDIR /gateway

COPY --from=builder /gateway/build/libs/*.jar app.jar

CMD ["java", "-jar", "app.jar"]
