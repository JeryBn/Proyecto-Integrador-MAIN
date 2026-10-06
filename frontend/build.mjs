import {buildSync} from 'esbuild';
import {mkdirSync,writeFileSync,cpSync} from 'node:fs';
buildSync({entryPoints:['src/main.jsx'],bundle:true,jsx:'automatic',minify:true,outfile:'dist/assets/main.js',define:{'process.env.NODE_ENV':'"production"'}});
const html='<!doctype html><html lang="es"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Mis actividades · MAIN</title><link rel="stylesheet" href="/assets/main.css"></head><body><div id="root"></div><script type="module" src="/assets/main.js"></script></body></html>';
writeFileSync('dist/index.html',html);
mkdirSync('../src/main/resources/static/assets',{recursive:true});
cpSync('dist/assets','../src/main/resources/static/assets',{recursive:true});
writeFileSync('../src/main/resources/static/index.html',html);
console.log('React compilado y copiado al backend.');
