"""Package clean tracked sources and a built JAR, excluding local credentials."""
import argparse,hashlib,json,re,subprocess,zipfile
from pathlib import Path

def git(root,*args):
    return subprocess.check_output(['git','-C',str(root),*args],stderr=subprocess.DEVNULL)

def private_name(path):
    name=path.name.lower()
    return ((name=='.env' or name.startswith('.env.')) and not name.endswith(('.example','.sample','.template'))
        or name=='google-services.json' or 'firebase-adminsdk' in name or 'service-account' in name
        or path.suffix.lower() in ('.jks','.keystore','.p12','.pfx','.pem'))

def build(root,output):
    root=Path(root).resolve();output=Path(output).resolve()
    if git(root,'status','--porcelain','--untracked-files=no').strip():
        raise ValueError('Guarda los cambios del código en un commit antes de empaquetar.')
    jar=root/'target/main-sprint1-0.1.0.jar'
    if not jar.is_file():raise ValueError('Primero compila el servidor con mvn package.')
    sources=[]
    for raw in git(root,'ls-files','-z').split(b'\0'):
        if not raw:continue
        relative=Path(raw.decode('utf-8'));source=root/relative
        if private_name(relative) or 'entregables' in relative.parts:continue
        if source.is_symlink() or not source.resolve().is_relative_to(root):raise ValueError('Ruta de fuente no segura.')
        if not source.is_file():raise ValueError('Falta un archivo versionado; revisa el checkout.')
        data=source.read_bytes()
        if re.search(rb'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----\r?\n[A-Za-z0-9+/=\r\n]{40,}|"private_key"\s*:\s*"-----BEGIN|\bsk-(?:proj-|svcacct-)?[A-Za-z0-9_-]{24,}',data):
            raise ValueError('Se detectó posible credencial en '+relative.as_posix()+'; no se generó el paquete.')
        sources.append((relative.as_posix(),data))
    commit=git(root,'rev-parse','HEAD').decode().strip()
    manifest=dict(commit=commit,sourceSelection='git tracked only, private names excluded',androidRelease=False,
        files=[dict(path=name,sha256=hashlib.sha256(data).hexdigest()) for name,data in sources],
        serverJarSha256=hashlib.sha256(jar.read_bytes()).hexdigest())
    output.mkdir(parents=True,exist_ok=True);package=output/'MAIN_LOCAL_PAMELA.zip';temporary=output/'MAIN_LOCAL_PAMELA.zip.tmp'
    try:
        with zipfile.ZipFile(temporary,'w',zipfile.ZIP_DEFLATED) as archive:
            for name,data in sources:archive.writestr(name,data)
            archive.write(jar,jar.name)
            archive.writestr('MAIN_PACKAGE_MANIFEST.json',json.dumps(manifest,ensure_ascii=False,indent=2))
        with zipfile.ZipFile(temporary) as archive:
            if archive.testzip() is not None:raise ValueError('El paquete no supera la comprobación de integridad.')
            if any(private_name(Path(name)) for name in archive.namelist()):raise ValueError('El paquete contiene una ruta privada.')
        temporary.replace(package)
    finally:
        if temporary.exists():temporary.unlink()
    digest=hashlib.sha256(package.read_bytes()).hexdigest()
    (output/'MAIN_LOCAL_PAMELA.sha256').write_text(digest+'  '+package.name+'\n',encoding='utf-8')
    return dict(package=str(package),commit=commit,files=len(sources),sha256=digest,bytes=package.stat().st_size,androidRelease=False)

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--output',type=Path)
    args=parser.parse_args();root=Path(__file__).resolve().parents[1]
    print(json.dumps(build(root,args.output or root/'entregables'),ensure_ascii=False))
