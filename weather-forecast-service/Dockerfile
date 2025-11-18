# ===== builder =====
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# cache deps
COPY pom.xml ./
RUN mvn -B -DskipTests dependency:go-offline

# build
COPY src ./src
RUN mvn -B -DskipTests package

# ===== runtime =====
FROM eclipse-temurin:21-jre-jammy
WORKDIR /opt/app

# copy fast-jar layout
COPY --from=build /workspace/target/quarkus-app/ ./

# (optional) run as non-root
RUN useradd -ms /bin/bash quarkus
USER quarkus

ENV JAVA_OPTS="-Dquarkus.http.host=0.0.0.0"
EXPOSE 8080
# fast-jar entrypoint
CMD ["bash","-lc","java $JAVA_OPTS -jar /opt/app/quarkus-run.jar"]
