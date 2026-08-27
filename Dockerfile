# --- Build stage ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
RUN mvn -q -B -DskipTests package

# --- Runtime stage ---
FROM eclipse-temurin:21-jre-jammy

RUN groupadd --system pharmasync && useradd --system --gid pharmasync pharmasync
WORKDIR /app

COPY --from=build /build/target/pharmasync.jar app.jar
COPY migrations ./migrations

RUN chown -R pharmasync:pharmasync /app
USER pharmasync

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
