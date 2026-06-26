FROM node:20-alpine AS build
WORKDIR /app
COPY frontend/merchant-web/package*.json ./
RUN npm ci
COPY frontend/merchant-web ./
RUN npm run build

FROM nginx:1.27-alpine
COPY deploy/nginx/spa.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
