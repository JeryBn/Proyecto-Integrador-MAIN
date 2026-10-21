"""Fixture HTTP local para comprobar carga/error/reintento del React real.

No autentica usuarios ni comprueba permisos. Para eso ejecutar las pruebas Java.
Uso: python scripts/pruebas_web_historias.py --mode-file ruta/mode.txt
Escribir en mode.txt: ok, empty, slow, error o admin-error.
Sirve exclusivamente en loopback y nunca conecta a la base o API del proyecto.
"""
import argparse
import json
from functools import partial
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from time import sleep
from urllib.parse import urlsplit

STATIC = Path(__file__).resolve().parents[1] / 'src/main/resources/static'
ACTIVITY = dict(id=1, cursoId=1, titulo='Actividad sintética de prueba de interfaz',
                curso='Curso de prueba', docente='Docente sintético', descripcion='Instrucciones sintéticas.',
                fechaLimite='2026-10-06T15:30:00', vencida=True, completada=False)

class Handler(SimpleHTTPRequestHandler):
    def __init__(self, *args, mode_file, **kwargs):
        self.mode_file = mode_file
        super().__init__(*args, directory=str(STATIC), **kwargs)

    def json(self, status, data):
        payload = json.dumps(data, ensure_ascii=False).encode()
        self.send_response(status)
        self.send_header('Content-Type', 'application/json;charset=utf-8')
        self.send_header('Content-Length', str(len(payload)))
        self.send_header('Cache-Control', 'no-store')
        self.end_headers()
        self.wfile.write(payload)

    def do_GET(self):
        path = urlsplit(self.path).path
        if not path.startswith('/api/'):
            return super().do_GET()
        mode = self.mode_file.read_text(encoding='utf-8-sig').strip()
        if mode == 'slow':
            sleep(8)
        if mode == 'error' and path == '/api/perfil':
            return self.json(503, {})
        if mode == 'admin-error' and path == '/api/admin/actividades':
            return self.json(503, dict(error=dict(code='TEST_FAILURE', message='Fallo sintético de carga para verificar reintento.')))
        values = {
            '/api/perfil': dict(nombre='CUENTA SINTÉTICA · PRUEBA DE INTERFAZ', rol='Alumno'),
            '/api/actividades/pendientes': [] if mode == 'empty' else [ACTIVITY],
            '/api/actividades/1': ACTIVITY,
            '/api/admin/csrf': dict(headerName='X-CSRF-TOKEN', parameterName='_csrf', token='fixture-no-es-credencial'),
            '/api/admin/perfil': dict(usuario='ADMIN SINTÉTICO · PRUEBA DE INTERFAZ'),
            '/api/admin/actividades': dict(items=[ACTIVITY], total=1),
            '/api/admin/cursos': dict(items=[dict(id=1,nombre='Curso de prueba')], total=1),
        }
        return self.json(200, values[path]) if path in values else self.json(404, {})

    def log_message(self, *_):
        pass

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--mode-file', type=Path, required=True)
    parser.add_argument('--port', type=int, default=8091)
    args = parser.parse_args()
    if not args.mode_file.is_file():
        parser.error('Crear primero el archivo de modo con el texto ok.')
    server = ThreadingHTTPServer(('127.0.0.1', args.port), partial(Handler, mode_file=args.mode_file))
    print(f'Fixture sintético de interfaz en http://127.0.0.1:{args.port}', flush=True)
    server.serve_forever()
