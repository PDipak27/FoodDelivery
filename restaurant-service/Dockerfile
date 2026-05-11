# Stage 1: build
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /workspace
COPY pom.xml .
COPY restaurant-service/pom.xml restaurant-service/
RUN mvn dependency:go-offline -pl restaurant-service -am -q
COPY restaurant-service/src restaurant-service/src
RUN mvn package -pl restaurant-service -am -DskipTests -q

# Stage 2: run
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /workspace/restaurant-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
