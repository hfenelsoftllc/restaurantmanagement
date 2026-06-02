FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml ./
COPY security-common ./security-common
COPY payment-service-internal ./payment-service-internal

RUN mvn -B -f security-common/pom.xml -DskipTests install
RUN mvn -B -f payment-service-internal/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build /workspace/payment-service-internal/target/*.jar app.jar

EXPOSE 9097
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

