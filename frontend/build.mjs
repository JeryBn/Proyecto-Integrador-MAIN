import {execFileSync} from 'node:child_process';
for(const app of ['usuario','admin'])execFileSync(process.platform==='win32'?'npm.cmd':'npm',['--prefix',app,'run','build'],{stdio:'inherit',shell:process.platform==='win32'});
