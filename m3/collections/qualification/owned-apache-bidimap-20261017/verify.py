# SPDX-License-Identifier: Apache-2.0
import hashlib
from pathlib import Path

SCRIPT=Path(__file__).absolute()
for p in (SCRIPT,*SCRIPT.parents):
 if p.is_symlink() or getattr(p,'is_junction',lambda:False)():raise RuntimeError('aliased qualification path')
ROOT=SCRIPT.parents[4]
OWNER=ROOT/'m3/collections/recipe/owned-apache-bidimap-20261017/src/main/resources/jdk/internal/m3/rewrite/hash-pinned-java/owned-apache-bidimap-v3'
EXPECTED={
 'm3/collections/implementations/apache/src/main/java/jdk/m3/collections/apache/M3BidiMap.java':'725bfaae37358339189eaeaf9867afe1da3bff62b7708950f1a869f54da0e52b',
 'm3/collections/implementations/apache/src/test/java/jdk/m3/collections/apache/M3BidiMapSelfTest.java':'1c171bc2d22b5371290472feb78fe074a67f016e46c3bc6f6c4d083b7f357576'}
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
V1=OWNER.parent/'owned-apache-bidimap'
if hashlib.sha256(read(V1/'M3BidiMap.java.txt')).hexdigest()!='e296581ad9e8b7e9fab4a35d9667a1ab5f4143b342af7bc6aacf1a8c98d4879b':raise RuntimeError('immutable V1 body drift')
transition=OWNER.parent/'owned-apache-bidimap-values-transition-v2'
row=read(transition/'manifest.tsv').decode().strip().split('\t')
body=next(path for path in EXPECTED if path.endswith('/M3BidiMap.java'))
if row!=[body,'e296581ad9e8b7e9fab4a35d9667a1ab5f4143b342af7bc6aacf1a8c98d4879b','875c04a6696864fc54e582d11cd0261cc91a0af669aa64f4506929ce1114d7b9','M3BidiMap.java.txt']:raise RuntimeError('V2 transition lineage drift')
if hashlib.sha256(read(transition/'M3BidiMap.java.txt')).hexdigest()!='875c04a6696864fc54e582d11cd0261cc91a0af669aa64f4506929ce1114d7b9':raise RuntimeError('immutable V2 transition output drift')
V2=OWNER.parent/'owned-apache-bidimap-v2'
if hashlib.sha256(read(V2/'M3BidiMap.java.txt')).hexdigest()!='875c04a6696864fc54e582d11cd0261cc91a0af669aa64f4506929ce1114d7b9':raise RuntimeError('immutable V2 body drift')
transition3=OWNER.parent/'owned-apache-bidimap-values-transition-v3'
row3=read(transition3/'manifest.tsv').decode().strip().split('\t')
if row3!=[body,'875c04a6696864fc54e582d11cd0261cc91a0af669aa64f4506929ce1114d7b9',EXPECTED[body],'M3BidiMap.java.txt']:raise RuntimeError('V3 transition lineage drift')
if read(transition3/'M3BidiMap.java.txt')!=read(ROOT/body):raise RuntimeError('V3 transition output drift')
print('PASS OWNED_BIDI_V3_RECEIVER: exact runtime-qualified V3 and immutable V1/V2 lineages')
