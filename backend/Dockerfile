# syntax=docker/dockerfile:1.7

# ---- Build Stage ----
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and POM first for dependency caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw

# Download dependencies (cached unless pom.xml changes)
RUN --mount=type=cache,target=/root/.m2 ./mvnw dependency:go-offline -B

# Copy source and build
COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 ./mvnw package -DskipTests -B

# Extract layered jar
RUN java -Djarmode=layertools -jar target/*.jar extract --destination target/extracted

# ---- Runtime Stage ----
FROM eclipse-temurin:17-jre-alpine

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy extracted layers
COPY --from=builder /app/target/extracted/dependencies/ ./
COPY --from=builder /app/target/extracted/spring-boot-loader/ ./
COPY --from=builder /app/target/extracted/snapshot-dependencies/ ./
COPY --from=builder /app/target/extracted/application/ ./

# Create uploads directory
RUN mkdir -p /app/uploads && chown -R appuser:appgroup /app

USER appuser

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=40s \
  CMD wget -qO- http://127.0.0.1:8081/actuator/health || exit 1

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
