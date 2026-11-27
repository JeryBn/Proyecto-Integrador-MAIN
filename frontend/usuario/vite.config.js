import {defineConfig} from 'vite';
import {fileURLToPath} from 'node:url';
import react from '@vitejs/plugin-react';
export default defineConfig({
  resolve: {alias: {'@main': fileURLToPath(new URL('../src', import.meta.url))}, dedupe: ['react', 'react-dom']},
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
    "host": "127.0.0.1",
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
