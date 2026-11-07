import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// In development the dev server proxies /api to the gateway, so the browser only ever
// talks to one origin and CORS never enters the picture. In the container the same job
// is done by nginx (see nginx.conf), which keeps the two environments identical from
// the application's point of view: every request is a relative /api/... call.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      "/api": {
        target: process.env.GATEWAY_URL ?? "http://localhost:18080",
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: "dist",
    sourcemap: true,
  },
});
