# syntax=docker/dockerfile:1

# --- Build stage -------------------------------------------------------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Cache dependencies separately from source so a source-only change doesn't
# re-download the whole Maven repository.
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline

COPY src src
RUN ./mvnw -q -B package -DskipTests

# --- Runtime stage -------------------------------------------------------
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --system --create-home --shell /usr/sbin/nologin petapp
USER petapp

COPY --from=build /app/target/*.jar app.jar

# Real secrets (DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD,
# CORS_ALLOWED_ORIGINS, jwt.secret, api.*.key, SMTP settings) are supplied by the
# deployment environment — see application-prod.properties. Never bake them into
# the image.
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
