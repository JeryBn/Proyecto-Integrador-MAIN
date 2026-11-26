# Frontends COLEGIA

Dos proyectos independientes: usuario/ y admin/, cada uno con package.json, package-lock.json, Vite, pruebas y Dockerfile. Usuario consume Spring; Admin consume Django. Instalar con npm ci en cada carpeta. La documentación de arranque está en ../README.md.

Las fuentes anteriores se trasladaron conservando cambios locales. frontend/build.mjs se mantiene como puente para compilar ambos desde esta carpeta; las copias para los servidores anteriores se actualizan desde la raíz con node scripts/sync-web.mjs. Compose sirve directamente los builds separados.
