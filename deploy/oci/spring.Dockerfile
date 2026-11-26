FROM node:24-bookworm-slim AS web-build
WORKDIR /workspace
COPY frontend/usuario ./frontend/usuario
COPY frontend/admin ./frontend/admin
RUN cd frontend/usuario && npm ci && npm run build && cd ../admin && npm ci && npm run build
FROM maven:3.9-eclipse-temurin-21 AS java-build
WORKDIR /workspace
COPY backend/springboot/pom.xml ./pom.xml
COPY backend/springboot/src ./src
COPY --from=web-build /workspace/frontend/usuario/dist ./src/main/resources/static
COPY --from=web-build /workspace/frontend/admin/dist/assets ./src/main/resources/static/assets
COPY --from=web-build /workspace/frontend/admin/dist/admin.html ./src/main/resources/static/admin.html
RUN mvn -B -ntp -DskipTests package
FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl ffmpeg && rm -rf /var/lib/apt/lists/* && useradd --system --uid 10001 --create-home main
WORKDIR /app
COPY --from=java-build /workspace/target/main-sprint1-0.1.0.jar /app/app.jar
COPY deploy/oci/spring-entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh
USER 10001:10001
ENTRYPOINT ["/app/entrypoint.sh"]
