# syntax=docker/dockerfile:1
# The gradle:8.14-jdk21 image already contains Gradle 8.14 (same version as the wrapper),
# so the build does not depend on downloading the distribution from GitHub.
FROM gradle:8.14-jdk21 AS build
ARG MODULE_NAME=api-gateway
USER root
WORKDIR /app
ENV GRADLE_USER_HOME=/gradle-cache

COPY settings.gradle build.gradle gradle.properties ./
COPY common/build.gradle ./common/
COPY api-gateway/build.gradle ./api-gateway/
COPY auth-service/build.gradle ./auth-service/
COPY user-service/build.gradle ./user-service/
COPY music-service/build.gradle ./music-service/
COPY playlist-service/build.gradle ./playlist-service/

RUN --mount=type=cache,target=/gradle-cache,sharing=locked gradle :${MODULE_NAME}:dependencies --no-daemon -q

COPY common ./common
COPY api-gateway ./api-gateway
COPY auth-service ./auth-service
COPY user-service ./user-service
COPY music-service ./music-service
COPY playlist-service ./playlist-service

RUN --mount=type=cache,target=/gradle-cache,sharing=locked gradle :${MODULE_NAME}:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
ARG MODULE_NAME=api-gateway
RUN addgroup -S app && adduser -S -G app app
WORKDIR /app
COPY --from=build /app/${MODULE_NAME}/build/libs/*.jar app.jar
RUN chown -R app:app /app
USER app
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
