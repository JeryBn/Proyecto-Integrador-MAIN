import {buildSync} from 'esbuild';
import {mkdirSync,writeFileSync,cpSync} from 'node:fs';

buildSync({
  entryPoints:{main:'src/main.jsx',admin:'src/admin.jsx'},
  bundle:true,jsx:'automatic',minify:true,
  outdir:'dist/assets',
  define:{'process.env.NODE_ENV':'"production"'}
});

const page=(title,name)=>'<!doctype html><html lang="es"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>'+title+'</title><link rel="stylesheet" href="/assets/'+name+'.css"></head><body><div id="root"></div><script type="module" src="/assets/'+name+'.js"></script></body></html>';
const html=page('Mis actividades · MAIN','main');
const adminHtml=page('Administración · MAIN','admin');

writeFileSync('dist/index.html',html);
writeFileSync('dist/admin.html',adminHtml);
mkdirSync('../src/main/resources/static/assets',{recursive:true});
cpSync('dist/assets','../src/main/resources/static/assets',{recursive:true});
writeFileSync('../src/main/resources/static/index.html',html);
writeFileSync('../src/main/resources/static/admin.html',adminHtml);
console.log('React compilado y copiado al backend.');