# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src src
RUN mvn -q -DskipTests -Dmaven.test.skip=true package

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd -r -u 1001 ptds
COPY --from=build /app/target/*.jar app.jar
RUN mkdir -p /app/storage && chown -R ptds:ptds /app
USER ptds
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
