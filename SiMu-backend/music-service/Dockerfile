# syntax=docker/dockerfile:1
FROM gradle:8.14-jdk21 AS build
ARG MODULE_NAME=music-service
USER root
WORKDIR /app

COPY gradlew settings.gradle build.gradle gradle.properties ./
COPY gradle ./gradle
COPY api-gateway/build.gradle ./api-gateway/
COPY auth-service/build.gradle ./auth-service/
COPY user-service/build.gradle ./user-service/
COPY music-service/build.gradle ./music-service/
COPY playlist-service/build.gradle ./playlist-service/

RUN chmod +x ./gradlew && ./gradlew :${MODULE_NAME}:dependencies --no-daemon -q

COPY api-gateway ./api-gateway
COPY auth-service ./auth-service
COPY user-service ./user-service
COPY music-service ./music-service
COPY playlist-service ./playlist-service

RUN ./gradlew :${MODULE_NAME}:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
ARG MODULE_NAME=music-service
RUN addgroup -S app && adduser -S -G app app
WORKDIR /app
COPY --from=build /app/${MODULE_NAME}/build/libs/*.jar app.jar
RUN chown -R app:app /app
USER app
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
