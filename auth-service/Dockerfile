# Stage 1: build
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /workspace
COPY pom.xml .
COPY auth-service/pom.xml auth-service/
RUN mvn dependency:go-offline -pl auth-service -am -q
COPY auth-service/src auth-service/src
RUN mvn package -pl auth-service -am -DskipTests -q

# Stage 2: run
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /workspace/auth-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
