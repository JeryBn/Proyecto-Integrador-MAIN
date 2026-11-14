FROM node:24.22.3-bookworm-slim AS web-build
WORKDIR /workspace
COPY frontend/package.json frontend/package-lock.json ./frontend/
RUN --mount=type=cache,target=/root/.npm cd frontend && npm ci
COPY frontend ./frontend
COPY src/main/resources/static ./src/main/resources/static
RUN cd frontend && npm run build

FROM maven:3.9.16-eclipse-temurin-21-noble AS java-build
WORKDIR /workspace
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 mvn --batch-mode --no-transfer-progress dependency:go-offline
COPY src ./src
COPY --from=web-build /workspace/src/main/resources/static ./src/main/resources/static
RUN --mount=type=cache,target=/root/.m2 mvn --batch-mode --no-transfer-progress -DskipTests package

FROM eclipse-temurin:21.0.13_9-jre-noble
RUN apt-get update \
    && apt-get install --yes --no-install-recommends ca-certificates curl ffmpeg \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 --create-home --home-dir /app main
WORKDIR /app
COPY --from=java-build --chown=10001:10001 /workspace/target/main-sprint1-0.1.0.jar /app/app.jar
COPY --chown=10001:10001 deploy/oci/spring-entrypoint.sh /app/entrypoint.sh
RUN chmod 0555 /app/entrypoint.sh
USER 10001:10001
EXPOSE 8088
ENTRYPOINT ["/app/entrypoint.sh"]
