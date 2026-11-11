import { fileURLToPath, URL } from "node:url"
import { defineConfig } from "vitest/config"
import react from "@vitejs/plugin-react"
import tailwindcss from "@tailwindcss/vite"

export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: { alias: { "@": fileURLToPath(new URL("./src", import.meta.url)) } },
  server: {
    port: 5173,
    strictPort: true,
    proxy: {
      "/oauth2": { target: process.env.API_PROXY_TARGET ?? "http://localhost:8080", changeOrigin: true },
      "/login/oauth2": { target: process.env.API_PROXY_TARGET ?? "http://localhost:8080", changeOrigin: true },
      "/actuator": { target: process.env.API_PROXY_TARGET ?? "http://localhost:8080", changeOrigin: true },
      "/api": { target: process.env.API_PROXY_TARGET ?? "http://localhost:8080", changeOrigin: true },
    },
  },
  test: {
    environment: "jsdom",
    setupFiles: ["./src/test/setup.ts"],
    include: ["src/**/*.test.{ts,tsx}"],
    restoreMocks: true,
  },
})
