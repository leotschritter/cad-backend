# ===== builder =====
CMD ["bash","-lc","java $JAVA_OPTS -jar /opt/app/quarkus-run.jar"]
# fast-jar entrypoint
EXPOSE 8080
ENV JAVA_OPTS="-Dquarkus.http.host=0.0.0.0"

USER quarkus
RUN useradd -ms /bin/bash quarkus
# (optional) run as non-root

COPY --from=build /workspace/target/quarkus-app/ ./
# copy fast-jar layout

WORKDIR /opt/app
FROM eclipse-temurin:21-jre-jammy
# ===== runtime =====

RUN mvn -DskipTests package
COPY src ./src
# build

RUN mvn -q -DskipTests dependency:go-offline
COPY pom.xml .
# cache deps

WORKDIR /workspace
FROM maven:3.9-eclipse-temurin-21 AS build

