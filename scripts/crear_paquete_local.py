"""Ejecutar desde cualquier carpeta despues de npm run build y mvn package."""
from pathlib import Path
import hashlib
import zipfile

root = Path(__file__).resolve().parents[1]
jar = root / 'target/main-sprint1-0.1.0.jar'
if not jar.exists():
    raise SystemExit('Primero ejecuta mvn package en la raiz del proyecto.')
output = root / 'entregables'
output.mkdir(exist_ok=True)
package = output / 'MAIN_LOCAL_PAMELA.zip'
excluded = {'.git', '.idea', 'target', 'node_modules', 'dist', 'entregables'}
with zipfile.ZipFile(package, 'w', zipfile.ZIP_DEFLATED) as archive:
    for source in sorted(root.rglob('*')):
        relative = source.relative_to(root)
        if not source.is_file() or excluded.intersection(relative.parts):
            continue
        if source.name == '.env' or source.suffix in {'.log', '.jar', '.iml'}:
            continue
        archive.write(source, relative)
    archive.write(jar, jar.name)
(output / 'MAIN_LOCAL_PAMELA.sha256').write_text(
    hashlib.sha256(package.read_bytes()).hexdigest() + '  MAIN_LOCAL_PAMELA.zip\n',
    encoding='utf-8',
)
print(f'Paquete generado: {package} ({package.stat().st_size} bytes)')
