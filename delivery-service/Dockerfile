# Stage 1: build
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /workspace
COPY pom.xml .
COPY delivery-service/pom.xml delivery-service/
RUN mvn dependency:go-offline -pl delivery-service -am -q
COPY delivery-service/src delivery-service/src
RUN mvn package -pl delivery-service -am -DskipTests -q

# Stage 2: run
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /workspace/delivery-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
