# SPDX-License-Identifier: Apache-2.0
import json
import sys
from pathlib import Path

profile,filename=sys.argv[1:]
if profile not in ('android','jre'):raise RuntimeError('explicit oracle profile required')
expected=json.loads(Path(__file__).with_name('final-protocol-results.json').read_text())[f'guava-{profile}-03']
rows=[]
for line in Path(filename).read_text(encoding='utf-8').splitlines():
 try:rows.append(json.loads(line))
 except json.JSONDecodeError:pass
summary=[r for r in rows if 'summary' in r]
cases=[r for r in rows if 'case' in r]
if len(summary)!=1 or summary[0]!=expected['summary'] or len(cases)!=66 or len({r['case'] for r in cases})!=66:raise RuntimeError('protocol corpus/summary changed')
if cases!=expected['cases']:raise RuntimeError('paired outcome/state/callback classification drift')
different=[r['case'] for r in cases if r.get('equivalent') is False]
if different!=expected['different_cases']:raise RuntimeError('protocol boundary membership changed')
for r in cases:
 if r['candidate']['invariant_failures'] or r['candidate']['common_api_failures']:raise RuntimeError('owned API failure')
 if r['oracle']['invariant_failures']:raise RuntimeError('unclassified oracle invariant failure')
bugs=[r for r in cases if r['oracle']['common_api_failures']]
if profile=='android':
 if bugs:raise RuntimeError('Android API failure')
else:
 if len(bugs)!=1 or bugs[0]['case']!='inverse/view-lifecycle/2':raise RuntimeError('unclassified oracle common failure')
 nested=bugs[0]['oracle']['outcome']['return']
 if nested[3]['error']!='java.lang.NullPointerException' or nested[4]['error']!='java.lang.NullPointerException':raise RuntimeError('oracle iterator defect classification drift')
print('PASS PROTOCOL CLASSIFICATION: '+profile+'; full JRE parity remains unadmitted; expected JRE Java assertion is retained')
