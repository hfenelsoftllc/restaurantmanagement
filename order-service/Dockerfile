FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Build from monorepo root context so inter-module dependencies resolve correctly.
COPY pom.xml ./
COPY security-common ./security-common
COPY order-service ./order-service

RUN mvn -B -f security-common/pom.xml -DskipTests install
RUN mvn -B -f order-service/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build /workspace/order-service/target/*.jar app.jar

EXPOSE 9096
ENTRYPOINT ["java", "-XX:+UseG1GC", "-Xmx512m", "-jar", "/app/app.jar"]

