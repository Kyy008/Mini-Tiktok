FROM node:20-alpine AS build

WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .

ARG VITE_AUTH_BASE_URL=http://localhost:8088
ARG VITE_API_BASE_URL=http://localhost:8088
ARG VITE_CLIENT_ID=tiktok-web
ARG VITE_REDIRECT_URI=http://localhost:8088/oauth/callback
ARG VITE_SCOPE="video:read video:write video:like"

ENV VITE_AUTH_BASE_URL=$VITE_AUTH_BASE_URL
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV VITE_CLIENT_ID=$VITE_CLIENT_ID
ENV VITE_REDIRECT_URI=$VITE_REDIRECT_URI
ENV VITE_SCOPE=$VITE_SCOPE

RUN npm run build

FROM nginx:1.27-alpine

COPY --from=build /app/dist /usr/share/nginx/html
COPY docker/nginx/default.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
