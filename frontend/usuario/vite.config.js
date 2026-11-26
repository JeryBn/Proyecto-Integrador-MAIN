import {defineConfig} from 'vite';
import react from '@vitejs/plugin-react';
export default defineConfig({
  "plugins": [
    react()
  ],
  "build": {
    "rollupOptions": {
      "input": {
        "0": "index.html",
        "1": "login.html",
        "2": "bienvenida.html",
        "3": "portal.html"
      }
    }
  },
  "server": {
    "host": "0.0.0.0",
    "port": 5173,
    "strictPort": true,
    "proxy": {
      "/api": {
        "target": process.env.API_PROXY_TARGET || 'http://127.0.0.1:8088',
        "changeOrigin": false
      },
      "/login-token": {
        "target": process.env.API_PROXY_TARGET || 'http://127.0.0.1:8088',
        "changeOrigin": false
      },
      "^/login$": {
        "target": process.env.API_PROXY_TARGET || 'http://127.0.0.1:8088',
        "changeOrigin": false
      },
      "^/logout$": {
        "target": process.env.API_PROXY_TARGET || 'http://127.0.0.1:8088',
        "changeOrigin": false
      },
      "/actuator": {
        "target": process.env.API_PROXY_TARGET || 'http://127.0.0.1:8088',
        "changeOrigin": false
      }
    }
  }
});
