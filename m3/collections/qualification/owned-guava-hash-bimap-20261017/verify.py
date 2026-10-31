# SPDX-License-Identifier: Apache-2.0
import hashlib
from pathlib import Path

SCRIPT=Path(__file__).absolute()
for p in (SCRIPT,*SCRIPT.parents):
 if p.is_symlink() or getattr(p,'is_junction',lambda:False)():raise RuntimeError('aliased qualification path')
ROOT=SCRIPT.parents[4]
OWNER=ROOT/'m3/collections/recipe/owned-guava-hash-bimap-20261017/src/main/resources/jdk/internal/m3/rewrite/hash-pinned-java/owned-guava-hash-bimap'
EXPECTED={'m3/collections/implementations/guava/src/main/java/jdk/m3/collections/guava/M3HashBiMap.java': 'f1cbb5dd4384c192f5f4066df1d15547a7edc3bc145a446ee3140faa129949b0', 'm3/collections/implementations/guava/src/test/java/jdk/m3/collections/guava/InverseForcePutDenseOrderDiscriminator.java': '1b39dff7fb36a899b1862a5eb2246e7e53b5ba92666362c3a1b4b1500cdd8123', 'm3/collections/implementations/guava/src/test/java/jdk/m3/collections/guava/M3HashBiMapCommonSelfTest.java': '44c19fbdb7485c0251a76eaae6070fb3292eb5f9aff6cadefae113fd11ff0285'}
EXTRA={'m3/collections/qualification/owned-guava-hash-bimap-20261017/M3HashBiMapProtocolDiscriminator.java': '02cb6be91b17e07a247c7f6559feedb03b6119534bed8ead1d21408d2db5c864', 'm3/collections/qualification/owned-guava-hash-bimap-20261017/M3HashBiMapSectionRunner.java': '373d027d53247b9771d33c3533fe3605332670dd28c64ce3f00d20778a9e71d7', 'm3/collections/qualification/owned-guava-hash-bimap-20261017/M3HashBiMapSelfTest.java': 'f0c4a3ab39e3e0ae1ae5166943c59c325567f6fa761366c08b90487318c4db45'}
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
