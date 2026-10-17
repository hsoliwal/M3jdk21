# SPDX-License-Identifier: Apache-2.0
import hashlib
from pathlib import Path

SCRIPT=Path(__file__).absolute()
for p in (SCRIPT,*SCRIPT.parents):
 if p.is_symlink() or getattr(p,'is_junction',lambda:False)():raise RuntimeError('aliased qualification path')
ROOT=SCRIPT.parents[4]
OWNER=ROOT/'m3/collections/recipe/owned-apache-bidimap-20261017/src/main/resources/com/synexia/rewrite/hash-pinned-java/owned-apache-bidimap'
EXPECTED={
 'm3/collections/implementations/apache/src/main/java/com/m3/collections/apache/M3BidiMap.java':'f811a07b8fcd6be3a4b64d61f548742ad44a047f58ecb8641c490850d51bad9e',
 'm3/collections/implementations/apache/src/test/java/com/m3/collections/apache/M3BidiMapSelfTest.java':'e6cdb48ba26466b8e8a3c6afe31036182b353d7c10cfd83896737714a209419d'}
def read(p):
 relative=p.relative_to(ROOT);q=ROOT
 for part in relative.parts:
  q=q/part
  if q.is_symlink() or getattr(q,'is_junction',lambda:False)():raise RuntimeError('aliased input')
 return p.read_bytes()
rows=[line.split('\t') for line in read(OWNER/'manifest.tsv').decode().splitlines() if line and not line.startswith('#')]
if len(rows)!=2 or [row[0] for row in rows]!=sorted(EXPECTED):raise RuntimeError('manifest ownership drift')
for path,before,after,template in rows:
 if before!='ABSENT' or after!=EXPECTED[path] or template!=Path(path).name+'.txt':raise RuntimeError('manifest lineage drift')
 payload=read(OWNER/template)
 if hashlib.sha256(payload).hexdigest()!=after or read(ROOT/path)!=payload:raise RuntimeError('source/template drift')
print('PASS OWNED_BIDI_RECEIVER: exact tested implementation and oracle recipe outputs')
