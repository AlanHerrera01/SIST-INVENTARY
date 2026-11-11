FROM maven:3.10.0-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY src ./src
# Cache de dependencias entre builds; los tests se ejecutan en CI y en `mvnw verify`.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -DskipTests -Dmaven.wagon.http.retryHandler.count=3 package

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /workspace/target/inventory-0.0.1-SNAPSHOT.jar /app/application.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/application.jar"]
