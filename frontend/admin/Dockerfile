FROM node:24-bookworm-slim AS build
WORKDIR /web
COPY frontend/admin/package*.json ./
RUN npm ci
COPY frontend/src /src
COPY frontend/admin ./
RUN npm run build
FROM nginx:stable-alpine
COPY --from=build /web/dist /usr/share/nginx/html
COPY frontend/admin/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
