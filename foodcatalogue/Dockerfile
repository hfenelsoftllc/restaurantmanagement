FROM maven:3.9.9-eclipse-temurin-21 AS build-security-common
WORKDIR /workspace/security-common

# Build security-common first as it's a prerequisite
COPY security-common/ ./
RUN mvn -B -DskipTests clean install

FROM maven:3.9.9-eclipse-temurin-21 AS build-foodcatalogue
WORKDIR /workspace/foodcatalogue

# Copy security-common from previous stage Maven cache
COPY --from=build-security-common /root/.m2 /root/.m2

# Copy and build foodcatalogue
COPY foodcatalogue/ ./
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build-foodcatalogue /workspace/foodcatalogue/target/*.jar app.jar

EXPOSE 9093
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

