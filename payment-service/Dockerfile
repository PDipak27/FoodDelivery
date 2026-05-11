# Stage 1: build
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /workspace
COPY pom.xml .
COPY payment-service/pom.xml payment-service/
RUN mvn dependency:go-offline -pl payment-service -am -q
COPY payment-service/src payment-service/src
RUN mvn package -pl payment-service -am -DskipTests -q

# Stage 2: run
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /workspace/payment-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
