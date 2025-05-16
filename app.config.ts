// app.config.ts
import { defineConfig } from "@tanstack/react-start/config";

export default defineConfig({
  tsr: {
    appDirectory: "src",
  },
  server: {
    allowedHosts: [
      "localhost",
      "127.0.0.1",
      "0.0.0.0",
      "bonds-exclusive-darkness-chick.trycloudflare.com",
    ],
  },
  vite: {
    server: {
      allowedHosts: [
        "localhost",
        "127.0.0.1",
        "0.0.0.0",
        "bonds-exclusive-darkness-chick.trycloudflare.com",
      ],
    },
  },
});
