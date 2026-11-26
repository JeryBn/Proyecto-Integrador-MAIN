import {cpSync,mkdirSync} from 'node:fs';
const target=new URL('../backend/springboot/src/main/resources/static/',import.meta.url);
mkdirSync(target,{recursive:true});
cpSync(new URL('../frontend/usuario/dist/',import.meta.url),target,{recursive:true});
cpSync(new URL('../frontend/admin/dist/assets/',import.meta.url),new URL('assets/',target),{recursive:true});
cpSync(new URL('../frontend/admin/dist/admin.html',import.meta.url),new URL('admin.html',target));
console.log('Copias compatibles actualizadas; las fuentes React permanecen separadas.');
