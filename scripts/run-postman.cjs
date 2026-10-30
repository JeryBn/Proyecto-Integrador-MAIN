// npm install newman@6.2.3 in an external tool directory; expose via NODE_PATH.
// Only a sanitized summary is saved: never request bodies, cookies or tokens.
const fs=require('node:fs');const path=require('node:path');const newman=require('newman');
const root=path.resolve(__dirname,'..');
const values={userBase:process.env.MAIN_TEST_ORIGIN,adminBase:process.env.MAIN_TEST_ORIGIN,
 username:process.env.MAIN_TEST_USERNAME,password:process.env.MAIN_TEST_USER_PASSWORD,
 adminUsername:process.env.MAIN_TEST_ADMIN_USERNAME,adminPassword:process.env.MAIN_TEST_ADMIN_PASSWORD};
for(const [key,value] of Object.entries(values))if(!value)throw Error('Falta configuración privada: '+key);
const origin=new URL(values.userBase);
if(origin.protocol!=='https:'||origin.username||origin.password||origin.pathname!=='/'||origin.search||origin.hash)throw Error('Origen HTTPS exacto requerido');
newman.run({collection:require(path.join(root,'postman/MAIN-HTTPS.postman_collection.json')),
 environment:{values:Object.entries(values).map(([key,value])=>({key,value,enabled:true}))},
 reporters:[],insecure:false,sslExtraCaCerts:process.env.MAIN_TEST_CA_CERT,
 timeoutRequest:15000,timeoutScript:5000},(error,summary)=>{
 const safe={origin:origin.origin,publicDeploymentVerified:origin.hostname!=='localhost'&&origin.hostname!=='127.0.0.1',
  certificateVerificationEnabled:true,requests:summary?.run.stats.requests.total||0,
  assertions:summary?.run.stats.assertions.total||0,failedAssertions:summary?.run.stats.assertions.failed||0,
  failures:summary?.run.failures.length||0,executionError:!!error,
  cases:(summary?.run.executions||[]).map(e=>({name:e.item.name,status:e.response?.code||0,
   assertions:(e.assertions||[]).map(a=>({name:a.assertion,passed:!a.error}))}))};
 const output=path.join(root,'docs/informes/2026-10-30-https/postman-results.json');
 fs.mkdirSync(path.dirname(output),{recursive:true});fs.writeFileSync(output,JSON.stringify(safe,null,2));
 console.log(JSON.stringify({requests:safe.requests,assertions:safe.assertions,failures:safe.failures,executionError:safe.executionError}));
 process.exitCode=error||safe.failures?1:0;
});
