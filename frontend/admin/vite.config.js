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
        "1": "admin.html"
      }
    }
  },
  "server": {
    "host": "127.0.0.1",
    "port": 5174,
    "strictPort": true,
    "proxy": {
      "^/(api/admin|moderation\\.(html|js|css))": {
        "target": process.env.API_PROXY_TARGET || 'http://127.0.0.1:8090',
        "changeOrigin": false
      }
    }
  }
});
