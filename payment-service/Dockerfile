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
COPY e2e-tests/pom.xml            e2e-tests/
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -pl payment-service -am -q
COPY payment-service/src payment-service/src
RUN --mount=type=cache,target=/root/.m2 mvn package -pl payment-service -am -DskipTests -q

# Stage 2: run
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /workspace/payment-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
