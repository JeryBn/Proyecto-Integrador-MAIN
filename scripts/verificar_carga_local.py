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
import json, math, statistics, sys, time

ORIGIN='http://127.0.0.1:8088'
WORKERS=8
READS=20

def worker(_):
    client=build_opener(HTTPCookieProcessor(CookieJar()))
    def get(path):
        return client.open(ORIGIN+path,timeout=15)
    with get('/login-token') as r: csrf=json.load(r)
    payload=urlencode({'username':'alumno','password':'MainDemo2026!',csrf['parameterName']:csrf['token']}).encode()
    with client.open(Request(ORIGIN+'/login',data=payload),timeout=15) as r: r.read()
    with get('/api/perfil') as r:
        if json.load(r).get('usuario')!='alumno': raise RuntimeError('Synthetic login did not succeed')
    results=[]
    try:
        for _ in range(READS):
            start=time.perf_counter()
            try:
                with get('/api/actividades/pendientes') as r:
                    data=json.load(r);status=r.status
                    if not isinstance(data,list):status=599
            except HTTPError as e:status=e.code
            except Exception:status=598
            results.append((status,(time.perf_counter()-start)*1000))
    finally:
        with get('/api/csrf') as r: csrf=json.load(r)
        request=Request(ORIGIN+'/logout',data=urlencode({csrf['parameterName']:csrf['token']}).encode())
        with client.open(request,timeout=15) as r:r.read()
    return results

if __name__=='__main__':
    target=Path(sys.argv[1] if len(sys.argv)>1 else 'rendimiento-local.json')
    start=time.perf_counter()
    with ThreadPoolExecutor(max_workers=WORKERS) as pool:
        batches=list(pool.map(worker,range(WORKERS)))
    elapsed=time.perf_counter()-start
    results=[r for batch in batches for r in batch];latencies=sorted(r[1] for r in results)
    summary={
      'measuredAtUtc':datetime.now(timezone.utc).isoformat(),
      'environment':'Isolated synthetic H2 server on Windows loopback; Android Studio and emulator running',
      'endpoint':'GET /api/actividades/pendientes','workers':WORKERS,'requests':len(results),
      'http200':sum(s==200 for s,_ in results),'errors':sum(s!=200 for s,_ in results),
      'medianMs':round(statistics.median(latencies),2),'p95Ms':round(latencies[math.ceil(.95*len(latencies))-1],2),
      'maximumMs':round(max(latencies),2),'wallSecondsIncludingLoginAndLogout':round(elapsed,2),
      'latencyScope':'Authenticated read latency excludes login and logout. Wall time includes both.',
      'limitations':'Local baseline only. No production capacity claim; no media uploads, AI provider, long disconnection, or physical devices measured.'
    }
    target.parent.mkdir(parents=True,exist_ok=True);target.write_text(json.dumps(summary,ensure_ascii=False,indent=2),encoding='utf-8')
    print(json.dumps(summary,ensure_ascii=False))
    if summary['errors']:raise SystemExit(1)
