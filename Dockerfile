#Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

COPY prom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN ./mvnw clean package -DskipTests

#Stage 2: Production Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT["java", "-Xmx512m", "-Xms256m", "-jar", "app.jar"]