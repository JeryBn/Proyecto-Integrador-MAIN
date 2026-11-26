# Guía de interfaz COLEGIA

Se reutiliza la interfaz escolar existente: azul marino y negro para superficies, azul para acciones, texto claro y contraste visible. Conservar foco de teclado, etiquetas, estados de carga, errores junto al formulario y confirmaciones centradas. El color no debe ser la única señal de estado.

Usuario: portada pública y login renderizados en React reutilizando las plantillas estáticas de confianza; roles Alumno, Docente y Apoderado dentro del portal. La portada no pide datos escolares privados. Un error de conexión no debe mostrarse como ausencia de tareas. Admin: acceso independiente mediante Django, gestión y seguimiento escolar existentes.

COLEGIA reemplaza MAIN en títulos, marcas y mensajes de interfaz. Se mantienen variables MAIN_*, cookies, encabezados, nombres de clases/paquetes, cuentas existentes y registros históricos. Los datos persistidos no se renombran masivamente.

Portada/login no reutilizan instrucciones arbitrarias ni HTML proporcionado por usuarios: son fuentes locales controladas. Las publicaciones de usuarios continúan renderizadas como texto en los componentes existentes.

Referencia de organización: https://github.com/amoremore123/ProyectoFoundIA . La referencia aporta separación de aplicaciones y estructura documental; se conserva la apariencia escolar propia y no se copian funcionalidades ajenas al alcance.
