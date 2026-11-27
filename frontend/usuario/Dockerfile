FROM node:24-bookworm-slim AS build
WORKDIR /web
COPY frontend/usuario/package*.json ./
RUN npm ci
COPY frontend/src /src
COPY frontend/usuario ./
RUN npm run build
FROM nginx:stable-alpine
COPY --from=build /web/dist /usr/share/nginx/html
COPY frontend/usuario/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
