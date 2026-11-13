# SPDX-License-Identifier: Apache-2.0
import hashlib
from pathlib import Path

SCRIPT=Path(__file__).absolute()
for p in (SCRIPT,*SCRIPT.parents):
 if p.is_symlink() or getattr(p,'is_junction',lambda:False)():raise RuntimeError('aliased qualification path')
ROOT=SCRIPT.parents[4]
OWNER=ROOT/'m3/collections/recipe/owned-spring-multivalue-map-20261017/src/main/resources/jdk/internal/m3/rewrite/hash-pinned-java/owned-spring-multivalue-map'
EXPECTED={'m3/collections/implementations/spring/src/main/java/jdk/m3/collections/spring/M3MultiValueMap.java': '9417df7099193eaf38723ad21ce83afa35c525de76772ee25f2df41ef4204623', 'm3/collections/implementations/spring/src/main/java/jdk/m3/collections/spring/M3SingleValueMapView.java': '9b5fca3e124034c6e1994cc3f97339e0c98159a77db2706298024b526e434211', 'm3/collections/implementations/spring/src/test/java/jdk/m3/collections/spring/M3MultiValueMapSelfTest.java': 'b59774e0e139a3153f0473b28b7d0236f710aaf81ecdd3f3880cb83577711095'}
def read(p):
 relative=p.relative_to(ROOT);q=ROOT
 for part in relative.parts:
  q=q/part
  if q.is_symlink() or getattr(q,'is_junction',lambda:False)():raise RuntimeError('aliased input')
 return p.read_bytes()
rows=[line.split('\t') for line in read(OWNER/'manifest.tsv').decode().splitlines() if line and not line.startswith('#')]
if len(rows)!=3 or [r[0] for r in rows]!=sorted(EXPECTED):raise RuntimeError('manifest ownership drift')
for path,before,after,template in rows:
 if before!='ABSENT' or after!=EXPECTED[path] or template!=Path(path).name+'.txt':raise RuntimeError('manifest lineage drift')
 payload=read(OWNER/template)
 if hashlib.sha256(payload).hexdigest()!=after or read(ROOT/path)!=payload:raise RuntimeError('qualified source/template drift')
print('PASS OWNED_SPRING_RECEIVER: exact runtime-qualified body/view/oracle and recipe outputs')
