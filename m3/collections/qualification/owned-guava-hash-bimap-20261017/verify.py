# SPDX-License-Identifier: Apache-2.0
import hashlib
from pathlib import Path

SCRIPT=Path(__file__).absolute()
for p in (SCRIPT,*SCRIPT.parents):
 if p.is_symlink() or getattr(p,'is_junction',lambda:False)():raise RuntimeError('aliased qualification path')
ROOT=SCRIPT.parents[4]
OWNER=ROOT/'m3/collections/recipe/owned-guava-hash-bimap-20261017/src/main/resources/com/synexia/rewrite/hash-pinned-java/owned-guava-hash-bimap'
EXPECTED={'m3/collections/implementations/guava/src/main/java/com/m3/collections/guava/M3HashBiMap.java': 'e5aca4438e0687af0aa6737a96acc60c50955722e50971a272bee9a98e443987', 'm3/collections/implementations/guava/src/test/java/com/m3/collections/guava/InverseForcePutDenseOrderDiscriminator.java': '66b8bdc1cb45b9994c615abc2c0a32dfd6a774f4b5eb78d20df4b82abbb35461', 'm3/collections/implementations/guava/src/test/java/com/m3/collections/guava/M3HashBiMapCommonSelfTest.java': 'bf5c83eb152ce2c2f3f8dc6bcda071d7b93a011189efe152c349786dbdaa443e'}
EXTRA={'m3/collections/qualification/owned-guava-hash-bimap-20261017/M3HashBiMapProtocolDiscriminator.java': 'd59f9d14c27a50f08cc868e3aa4c76ebab76ff12234a2028964dc03ad5cd1b6d', 'm3/collections/qualification/owned-guava-hash-bimap-20261017/M3HashBiMapSectionRunner.java': '38614ba7e1f86b413ba75a2b24bf98e0769907ab40abdc02c673e30f366db050', 'm3/collections/qualification/owned-guava-hash-bimap-20261017/M3HashBiMapSelfTest.java': 'd6b1229d112728a0d2f7f7936efb4b30091c6028558576c239226866453687c2'}
def read(p):
 relative=p.relative_to(ROOT);q=ROOT
 for part in relative.parts:
  q=q/part
  if q.is_symlink() or getattr(q,'is_junction',lambda:False)():raise RuntimeError('aliased input')
 return p.read_bytes()
rows=[r.split('\t') for r in read(OWNER/'manifest.tsv').decode().splitlines() if r and not r.startswith('#')]
if len(rows)!=3 or [r[0] for r in rows]!=sorted(EXPECTED):raise RuntimeError('manifest ownership drift')
for target,before,after,template in rows:
 if before!='ABSENT' or after!=EXPECTED[target] or template!=Path(target).name+'.txt':raise RuntimeError('manifest lineage drift')
 payload=read(OWNER/template)
 if hashlib.sha256(payload).hexdigest()!=after or read(ROOT/target)!=payload:raise RuntimeError('qualified source/template drift')
for target,sha in EXTRA.items():
 if hashlib.sha256(read(ROOT/target)).hexdigest()!=sha:raise RuntimeError('qualification fixture drift')
print('PASS OWNED_GUAVA_RECEIVER: exact scoped-runtime body/tests and qualification fixture custody')
