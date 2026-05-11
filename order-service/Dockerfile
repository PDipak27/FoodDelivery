# Stage 1: build
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /workspace
COPY pom.xml .
COPY order-service/pom.xml order-service/
RUN mvn dependency:go-offline -pl order-service -am -q
COPY order-service/src order-service/src
RUN mvn package -pl order-service -am -DskipTests -q

# Stage 2: run
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /workspace/order-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
