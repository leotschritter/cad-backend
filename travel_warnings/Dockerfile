####
# Multi-stage Dockerfile for Quarkus application
# Optimized for production Kubernetes deployment
####

## Stage 1: Build the application
FROM maven:3.9.5-eclipse-temurin-21 AS build
WORKDIR /build

# Copy pom.xml first for better layer caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
COPY openapi.yaml .
RUN mvn clean package -DskipTests -B

## Stage 2: Create runtime image
FROM registry.access.redhat.com/ubi9/openjdk-21-runtime:1.18

ENV LANGUAGE='en_US:en'

# Copy the jar from build stage
COPY --from=build --chown=185 /build/target/quarkus-app/lib/ /deployments/lib/
COPY --from=build --chown=185 /build/target/quarkus-app/*.jar /deployments/
COPY --from=build --chown=185 /build/target/quarkus-app/app/ /deployments/app/
COPY --from=build --chown=185 /build/target/quarkus-app/quarkus/ /deployments/quarkus/

# Set user to non-root for security
USER 185

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/q/health/live || exit 1

# Set JVM options for container
ENV JAVA_OPTS="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"
ENV JAVA_APP_JAR="/deployments/quarkus-run.jar"

ENTRYPOINT [ "java", "-jar", "/deployments/quarkus-run.jar" ]

