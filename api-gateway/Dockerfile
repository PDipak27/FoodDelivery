# syntax=docker/dockerfile:1
# Stage 1: build
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /workspace
COPY pom.xml .
COPY api-gateway/pom.xml          api-gateway/
COPY auth-service/pom.xml         auth-service/
COPY user-service/pom.xml         user-service/
COPY restaurant-service/pom.xml   restaurant-service/
COPY order-service/pom.xml        order-service/
COPY payment-service/pom.xml      payment-service/
COPY delivery-service/pom.xml     delivery-service/
COPY notification-service/pom.xml notification-service/
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -pl api-gateway -am -q
COPY api-gateway/src api-gateway/src
RUN --mount=type=cache,target=/root/.m2 mvn package -pl api-gateway -am -DskipTests -q

# Stage 2: run
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /workspace/api-gateway/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
