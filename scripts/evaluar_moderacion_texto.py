"""Bounded real-provider evaluation of seven synthetic examples contributed by Pamela.

No queues, uploads, real users, billing changes or automatic publication. Reference
routes interpret the initial policy; they are not independent human acceptance.
"""
import argparse
import csv
import hashlib
import json
import os
from pathlib import Path
import sys
import time

root=Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--run',action='store_true',help='Execute up to seven real moderation requests.')
args=parser.parse_args()
expected={f'M20-{n:02}':('HUMAN_REQUIRED' if n in (4,5) else 'CLEAR') for n in (1,2,3,4,5,6,9)}
with (root/'docs/revision/pamela/CASOS_M20.csv').open(encoding='utf-8-sig',newline='') as stream:
    cases=[row for row in csv.DictReader(stream) if row['id'] in expected]
assert len(cases)==7
if not args.run:
    print('Vista previa: siete ejemplos textuales sintéticos; excluidos marcadores y casos de interfaz. Usar --run para evaluar.');raise SystemExit
key=os.environ.get('OPENAI_API_KEY','')
if not key:
    for line in (root/'.env.local').read_text(encoding='utf-8-sig').splitlines():
        if line.startswith('OPENAI_API_KEY='):
            key=line.split('=',1)[1].strip().strip('"\'');break
if not key:raise SystemExit('Falta la clave existente; no se creó ni solicitó otra.')
sys.path.insert(0,str(root/'admin-django'));os.environ.setdefault('DJANGO_SETTINGS_MODULE','main_admin.test_settings')
import django
django.setup()
from administration.automatic_moderation import OpenAiModerator,ProviderUnavailable
provider=OpenAiModerator(key);results=[];blocked=None
for row in cases:
    try:
        answer=provider.assess(row['ejemplo_sintetico'])
    except ProviderUnavailable as error:
        blocked=str(error);break
    reference=expected[row['id']]
    results.append(dict(id=row['id'],textSha256=hashlib.sha256(row['ejemplo_sintetico'].encode()).hexdigest(),
                        referenceRoute=reference,providerRoute=answer.outcome,categories=list(answer.categories),
                        model=answer.model,agrees=reference==answer.outcome))
    time.sleep(.35)
result=dict(date='2026-10-30',synthetic=True,realProvider=True,source='docs/revision/pamela/CASOS_M20.csv',
            sourceAuthor='Pamela Regina Meneses Mayhua',referenceLabels='Initial policy interpretation; pending independent human confirmation',
            referenceIds=list(expected),excludedIds=['M20-07','M20-08','M20-10','M20-11','M20-12'],
            requestsCompleted=len(results),blocked=blocked,queueProcessed=False,publicationEnabled=False,
            humanAcceptance=False,representativeAccuracyClaimed=False,cases=results,
            agreements=sum(r['agrees'] for r in results),
            referenceRiskSentClear=sum(r['referenceRoute']=='HUMAN_REQUIRED' and r['providerRoute']=='CLEAR' for r in results),
            referenceClearSentToHuman=sum(r['referenceRoute']=='CLEAR' and r['providerRoute']=='HUMAN_REQUIRED' for r in results))
out=root/'docs/informes/2026-10-30-evaluacion-texto/results.json';out.parent.mkdir(parents=True,exist_ok=True)
out.write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps({k:v for k,v in result.items() if k not in ('cases','referenceIds','excludedIds')},ensure_ascii=False))
if blocked:raise SystemExit(2)
