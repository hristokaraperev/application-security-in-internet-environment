# syntax=docker/dockerfile:1

# ---- Build stage: compile the auth-service module (and its deps) ----
# Build context is the repository root so the parent POM and common-security
# module are available to the reactor.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY . .
RUN mvn -q -pl auth-service -am -DskipTests package

# ---- Run stage: slim JRE with just the fat jar ----
FROM eclipse-temurin:17-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /workspace/auth-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
