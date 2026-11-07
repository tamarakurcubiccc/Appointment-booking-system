# ---------------------------------------------------------------------------
# Build the bundle, then serve it from nginx. The node toolchain stays in the build
# stage, so the published image is a few megabytes of static files and a web server
# rather than a full JavaScript runtime.
#
# Build context is this directory, unlike the services, which build from the repository
# root because they share the Maven reactor.
# ---------------------------------------------------------------------------
FROM node:22-alpine AS build

WORKDIR /app

# Dependencies are installed from the lockfile first, so this layer is reused whenever
# only application code changes.
COPY package.json package-lock.json* ./
RUN npm install

COPY tsconfig.json vite.config.ts index.html ./
COPY src ./src

RUN npm run build

# ---------------------------------------------------------------------------
FROM nginx:1.27-alpine AS runtime

# envsubst replaces ${GATEWAY_URL} in the template at start-up; everything in
# /etc/nginx/templates is processed by the official image's entrypoint.
COPY nginx.conf /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html

ENV GATEWAY_URL="http://api-gateway:8080"

EXPOSE 8080
