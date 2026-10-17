# SPDX-License-Identifier: Apache-2.0
import hashlib
from pathlib import Path

SCRIPT=Path(__file__).absolute()
for p in (SCRIPT,*SCRIPT.parents):
 if p.is_symlink() or getattr(p,'is_junction',lambda:False)():raise RuntimeError('aliased qualification path')
ROOT=SCRIPT.parents[4]
OWNER=ROOT/'m3/collections/recipe/owned-spring-multivalue-map-20261017/src/main/resources/com/synexia/rewrite/hash-pinned-java/owned-spring-multivalue-map'
EXPECTED={'m3/collections/implementations/spring/src/main/java/com/m3/collections/spring/M3MultiValueMap.java': 'c1bf11b7ced59b3fdf424ce83c2941324d26ec5161e6dcbedccf8159c9c38c77', 'm3/collections/implementations/spring/src/main/java/com/m3/collections/spring/M3SingleValueMapView.java': 'e671912bc77d45792a18351219ec2659cc0f6b8fee7eda2848c6c449c2807c02', 'm3/collections/implementations/spring/src/test/java/com/m3/collections/spring/M3MultiValueMapSelfTest.java': 'ffa616717237390be73b067e385d5be13b573682e184f31d3003a7f24df84c04'}
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
