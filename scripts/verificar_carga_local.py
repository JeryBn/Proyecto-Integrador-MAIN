"""Small reproducible baseline for the isolated synthetic MAIN server on loopback.

This measures authenticated reads only. It is not a production capacity or stress test.
No credentials, cookies, responses or student records are written to the report.
"""
from concurrent.futures import ThreadPoolExecutor
from http.cookiejar import CookieJar
from urllib.request import build_opener, HTTPCookieProcessor, Request
from urllib.parse import urlencode
from urllib.error import HTTPError
from pathlib import Path
from datetime import datetime, timezone
import argparse, json, math, statistics, sys, time
from urllib.parse import urlsplit

ORIGIN='http://127.0.0.1:8088'
WORKERS=8
READS=20
SECONDS=0
INTERVAL_MS=0
MIXED=False

def worker(_):
    client=build_opener(HTTPCookieProcessor(CookieJar()))
    def get(path):
        return client.open(ORIGIN+path,timeout=15)
    with get('/login-token') as r: csrf=json.load(r)
    payload=urlencode({'username':'alumno','password':'MainDemo2026!',csrf['parameterName']:csrf['token']}).encode()
    with client.open(Request(ORIGIN+'/login',data=payload),timeout=15) as r: r.read()
    with get('/api/perfil') as r:
        if json.load(r).get('usuario')!='alumno': raise RuntimeError('Synthetic login did not succeed')
    results=[];reading_started=time.perf_counter()
    paths=['/api/actividades/pendientes','/api/v1/notifications','/api/perfil'] if MIXED else ['/api/actividades/pendientes']
    try:
        for index in range(READS):
            if SECONDS and time.perf_counter()-reading_started>=SECONDS:break
            path=paths[index%len(paths)]
            start=time.perf_counter()
            try:
                with get(path) as r:
                    data=json.load(r);status=r.status
                    if path=='/api/perfil':
                        if not isinstance(data,dict) or data.get('usuario')!='alumno':status=599
                    elif not isinstance(data,list):status=599
            except HTTPError as e:status=e.code
            except Exception:status=598
            results.append((status,(time.perf_counter()-start)*1000,path))
            if INTERVAL_MS:time.sleep(INTERVAL_MS/1000)
        reading_seconds=time.perf_counter()-reading_started
    finally:
        with get('/api/csrf') as r: csrf=json.load(r)
        request=Request(ORIGIN+'/logout',data=urlencode({csrf['parameterName']:csrf['token']}).encode())
        with client.open(request,timeout=15) as r:r.read()
    return dict(results=results,readingSeconds=round(reading_seconds,2))

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('result',nargs='?',type=Path,default=Path('rendimiento-local.json'))
    parser.add_argument('--workers',type=int,choices=range(1,17),default=8)
    parser.add_argument('--reads',type=int,choices=range(1,1001),default=20)
    parser.add_argument('--seconds',type=int,default=0,help='Duration limit per authenticated worker; 0 or 30..600 seconds.')
    parser.add_argument('--interval-ms',type=int,default=0,help='Pause between reads; 0..2000 ms; sustained runs require >=100 ms.')
    parser.add_argument('--mixed',action='store_true',help='Alternate pending activities, notifications and identity.')
    parser.add_argument('--base',default=ORIGIN)
    parser.add_argument('--environment',required=True,help='Describe la base y procesos realmente usados; no incluye secretos.')
    args=parser.parse_args()
    if args.seconds and not 30<=args.seconds<=600:parser.error('--seconds must be 0 or 30..600')
    if not 0<=args.interval_ms<=2000 or (args.seconds and args.interval_ms<100):parser.error('Sustained runs need an interval of 100..2000 ms')
    origin=urlsplit(args.base)
    if origin.scheme!='http' or origin.hostname not in ('127.0.0.1','localhost') or origin.path not in ('','/') or origin.username or origin.password or origin.query or origin.fragment:
        parser.error('Esta prueba solo permite el servidor sintético HTTP en loopback, sin rutas ni credenciales.')
    ORIGIN=args.base.rstrip('/');WORKERS=args.workers;READS=args.reads;target=args.result
    SECONDS=args.seconds;INTERVAL_MS=args.interval_ms;MIXED=args.mixed
    start=time.perf_counter()
    with ThreadPoolExecutor(max_workers=WORKERS) as pool:
        batches=list(pool.map(worker,range(WORKERS)))
    elapsed=time.perf_counter()-start
    results=[r for batch in batches for r in batch['results']];latencies=sorted(r[1] for r in results)
    summary={
      'measuredAtUtc':datetime.now(timezone.utc).isoformat(),
      'environment':args.environment,
      'endpoint':'Mixed authenticated GETs' if MIXED else 'GET /api/actividades/pendientes','workers':WORKERS,'requests':len(results),
      'http200':sum(s==200 for s,_,_ in results),'errors':sum(s!=200 for s,_,_ in results),
      'durationLimitSeconds':SECONDS,'intervalMs':INTERVAL_MS,
      'workerReadSeconds':[batch['readingSeconds'] for batch in batches],
      'endpoints':{path:dict(requests=sum(p==path for _,_,p in results),errors=sum(p==path and s!=200 for s,_,p in results)) for path in sorted({p for _,_,p in results})},
      'medianMs':round(statistics.median(latencies),2),'p95Ms':round(latencies[math.ceil(.95*len(latencies))-1],2),
      'maximumMs':round(max(latencies),2),'wallSecondsIncludingLoginAndLogout':round(elapsed,2),
      'latencyScope':'Authenticated read latency excludes login and logout. Wall time includes both.',
      'limitations':'Local baseline only. No production capacity claim; no media uploads, AI provider, long disconnection, or physical devices measured.'
    }
    target.parent.mkdir(parents=True,exist_ok=True);target.write_text(json.dumps(summary,ensure_ascii=False,indent=2),encoding='utf-8')
    print(json.dumps(summary,ensure_ascii=False))
    if summary['errors']:raise SystemExit(1)
