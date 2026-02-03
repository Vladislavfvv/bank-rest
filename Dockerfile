# === Stage 1: Build application ===
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom.xml for dependency caching
COPY pom.xml .
RUN mvn -B dependency:go-offline || true

# Copy source code
COPY src ./src

# Build project (skip tests to speed up image build)
RUN mvn -B clean package -DskipTests

# === Stage 2: Run application ===
FROM eclipse-temurin:17-jdk
WORKDIR /app

# Copy JAR file from build stage
COPY --from=build /app/target/*.jar app.jar

# Port for Bank Cards API
EXPOSE 8080

# Environment variable for profile
ENV SPRING_PROFILES_ACTIVE=docker

# Start application
ENTRYPOINT ["java", "-jar", "app.jar"]