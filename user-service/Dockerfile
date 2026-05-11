# Stage 1: build
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /workspace
COPY pom.xml .
COPY user-service/pom.xml user-service/
RUN mvn dependency:go-offline -pl user-service -am -q
COPY user-service/src user-service/src
RUN mvn package -pl user-service -am -DskipTests -q

# Stage 2: run
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /workspace/user-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
