# Verificación de la reestructuración COLEGIA · 25/11/2026

## Hecho y comprobado

- Respaldo del estado previo, archivos modificados y nuevos y manifiesto de integridad móvil fuera del repositorio. No se hicieron commits ni pushes.
- Dos proyectos Vite 8.3.8 con React 19.1.1. `npm --prefix frontend/usuario run build` y equivalente admin aprobaron. `node scripts/sync-web.mjs` mantiene las rutas estáticas anteriores sin sobrescribir la portada de usuario con el panel admin.
- `npm --prefix frontend/usuario test`: 49 pruebas aprobadas; admin: 7. Incluyen las pruebas existentes de transporte, límites y sesión.
- `mvn -B -ntp -Dspring.profiles.active=test test`, desde backend/springboot: 174 pruebas, 0 fallos y 0 errores. Después de ajustar recursos públicos y añadir el adaptador Ollama: `mvn -B -ntp -Dspring.profiles.active=test -Dtest=OllamaGatewayTest,PortalSessionTest,ActividadIntegrationTest package`: 18 pruebas aprobadas, JAR generado. El adaptador Ollama se probó contra un servidor HTTP simulado controlado; no contra un modelo real.
- Django: `python manage.py test administration --settings=main_admin.test_settings --noinput` desde backend/django-admin: 101 pruebas aprobadas. Se adapta la ruta de migraciones y se conserva UTF-8 al leerlas en Windows.
- `python -m unittest discover -s scripts -p test_paquete_local.py`: 5 pruebas aprobadas después del cambio de ubicación del JAR.
- `scripts/Test-ReleaseReadinessContract.ps1`: 8 comprobaciones del contrato aprobadas. `scripts/Test-OciDeployment.ps1`: contrato estático aprobado; no es un despliegue real.
- Navegador real mediante Vite: portada COLEGIA, enlace a login React, login de alumno con sesión y CSRF hacia Spring en H2 aislado/puerto 18081, pendientes HU06 y detalle HU07 con curso, docente, fecha e instrucciones. IA desactivada durante ese recorrido. Formulario administrativo abre desde Vite 5174 y apunta a Django mediante proxy.
- Las migraciones aplicadas y el código móvil se comparan por contenido con el respaldo previo. No se cambian checksums ni datos reales.

## Comprobaciones bloqueadas

Se intentaron desde la raíz:

```text
docker compose config --quiet
docker compose build
docker compose up -d --wait
```

Los tres fallaron porque `docker` no está disponible: `The term 'docker' is not recognized as a name of a cmdlet, function, script file, or executable program.` No se instalaron componentes del sistema ni se tocaron volúmenes anteriores.

Por tanto NO están comprobados localmente: validación nativa de Compose, construcción Linux de las imágenes, primer arranque Flyway sobre MySQL 8 vacío, arranque ordenado de Django, login a través de Nginx en contenedores y persistencia tras reiniciar el conjunto. MySQL 8 se declara objetivo configurado, no migración certificada. Tampoco se comprobó inferencia real de Gemma 3 ni su disponibilidad en otra máquina.

## Reproducción aislada cuando Docker esté disponible

Copiar .env.example a .env y preparar credenciales ficticias propias. Usar un nombre nuevo como COMPOSE_PROJECT_NAME=colegia-qa-20261125 y puertos libres. No reutilizar el nombre de un entorno con datos reales.

```powershell
docker compose config --quiet
docker compose build
docker compose up -d --wait --wait-timeout 300
docker compose exec django-admin python manage.py seed_colegia
python scripts/verify-compose.py
docker compose restart mysql springboot django-admin
docker compose up -d --wait --wait-timeout 300
python scripts/verify-compose.py
docker compose down
```

El verificador lee ADMIN_USERNAME/ADMIN_PASSWORD de .env o del entorno; los portales por defecto son 5173 y 5174 y se pueden indicar con COLEGIA_USER_ORIGIN/COLEGIA_ADMIN_ORIGIN. El trabajo CI compose-qa prepara una base aislada y ejecuta esta secuencia; no se afirma que CI haya corrido antes de enviar los cambios.

`down` conserva el volumen QA. No se ejecuta `down -v`. El seed se ejecuta solo la primera vez; después de reiniciar deben seguir existiendo la cuenta y sus actividades.

## Pendientes funcionales conservados

school_publication TASK/EXAM no alimenta actividad ni una agenda unificada. La gráfica numérica es orientativa, no una nota oficial por competencia. Se mantienen modelos académicos anteriores y escolares nuevos. El parentesco se autoriza manualmente desde administración. No se atribuye a la reorganización el cierre de esas brechas.

Las imágenes anteriores y el README de FoundIA sirvieron como referencia organizativa; su dominio, JWT y reglas de negocio no se incorporaron. El histórico de MAIN se conserva en docs/historico/README-MAIN-antes-de-COLEGIA.md.

## Comprobaciones finales adicionales

Login, perfil ADMIN y logout con CSRF verificados por HTTP a través de Vite 5174 hacia el Django local anterior. Esto no verifica Nginx ni MySQL 8 en Compose. Ambos npm ci completaron desde sus lockfiles después de cerrar los servidores de QA; el primer intento con Vite activo encontró EPERM por un binario bloqueado en Windows y se corrigió cerrando esos procesos de prueba. El YAML de Compose y CI se analizó sintácticamente con PyYAML: cinco servicios, volumen persistente y MySQL sin puerto publicado; no reemplaza docker compose config. Comparación final: 70 archivos móviles y 24 migraciones idénticas al respaldo. Git diff --check sin errores de espacios. Los servidores y pestañas de QA se cerraron.

## Comprobación de la rama web/frontend-backend · 26/11/2026

- Compilaciones React Usuario y Admin aprobadas; 54 y 7 pruebas respectivamente.
- Spring Boot completo: 187 pruebas, sin fallos ni errores, con JDK 21 y bases H2 aisladas. Django: 111 pruebas aprobadas con SQLite aislado.
- En navegador local: alumno mateo.morales inicia sesión y abre el detalle de su tarea; docente monica.silva entra a su aula y evaluaciones; apoderada ana.morales consulta a sus hijos vinculados; admin.monica abre el directorio mediante Django. No se modificaron sus datos académicos.
- La clave demo de carlos.mendoza ya fue cambiada en la base local. No se restableció. Las cuentas creadas manualmente en una laptop no se incluyen al clonar Git.
- Compose config validado. El motor Docker Desktop estaba apagado: sigue pendiente probar imágenes, primer arranque MySQL 8 y persistencia tras reinicio.
- No hay cambios en mobile/. Se excluyen de la nueva instantánea los paquetes ZIP de entregables y la configuración privada; los ZIP locales se conservan.
- Nuevos commits con identidad Pamela Meneses <pamela.meneses@tecsup.edu.pe>, sin coautor añadido. Se conserva la autoría de los commits anteriores.
- Pendientes funcionales: unificación de publicaciones TASK/EXAM con actividad y agenda; correo automático sin configurar. Las comprobaciones de acceso no certifican todos los recorridos ni igualdad visual de cada píxel.
