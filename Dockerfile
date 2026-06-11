# =========================================
# Stage 1: Build with Maven
# =========================================
FROM maven:3.9.16-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
# This "pre-downloads" dependencies so they are cached
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# =========================================
# Stage 2: Run with JRE
# =========================================
FROM eclipse-temurin:17.0.19_10-jre-noble
WORKDIR /app
# Copy only the compiled jar from the builder
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
