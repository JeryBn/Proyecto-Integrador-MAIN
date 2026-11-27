# Integración de COLEGIA

Esta rama reúne la implementación de MAIN con los portales React de `origin/web/frontend-backend`. Ambos portales y Android consumen el mismo Spring y la misma base de datos. El panel utiliza el Django vigente. La web que ya sirve Spring en 8088 sigue disponible.

## Probar el diseño

Con Docker, usa la configuración de [la guía de demostración](DEMO_LOCAL.md) y ejecuta `docker compose up --build --detach --wait`. Los servicios `portal` y `panel` publican los mismos puertos que el modo de desarrollo.

Para el modo de desarrollo, arranca los backends locales siguiendo la misma guía. Desde la raíz instala las dependencias y arranca cada portal en una terminal:

```powershell
npm --prefix frontend ci
npm --prefix frontend/usuario ci
npm --prefix frontend/admin ci
npm --prefix frontend/usuario run dev
```

```powershell
npm --prefix frontend/admin run dev
```

- [Portal escolar](http://127.0.0.1:5173/) y [portada pública](http://127.0.0.1:5173/bienvenida.html).
- [Administración](http://127.0.0.1:5174/).
- Android sigue usando el backend local por el puerto 8088.

Si el backend corre en otro puerto, establece `API_PROXY_TARGET` en la terminal del portal correspondiente antes de arrancarlo. Los servidores de desarrollo escuchan solo en la máquina local.

Los portales reutilizan los módulos de MAIN para chats, estados, asistente, llamadas, contactos, notas y asistencia. El panel reutiliza cuentas, invitaciones, cursos y matrículas. Los portales de desarrollo se prueban antes de cambiar las páginas que sirve el despliegue habitual.

Las pantallas de evaluación por competencias, comunicados escolares y autoservicio de recuperación por correo de la rama original requieren sus propias APIs y migraciones. La gestión académica y el acceso de esta integración utilizan los flujos que ya ofrece MAIN.
