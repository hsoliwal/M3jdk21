# SPDX-License-Identifier: Apache-2.0
import hashlib
from pathlib import Path

ROOT=Path(__file__).resolve().parents[4]
RECIPE=ROOT/'m3/collections/recipe/zero-mask-window-copy-20261017'
RES=RECIPE/'src/main/resources/jdk/internal/m3/rewrite'
EXPECTED={
 'm3/collections/src/test/java/jdk/m3/collections/M3IndexMaskCompactorSelfTest.java':('native-zero-mask-window-java','hash-pinned-java','86eb2c9a436aac6f6e5b99f5c275636ca272d849dd08aa07f8796271167c63a1','c81601d08eb3d7442d4fe952a9ba580da17bd3dbe74fb581daa04ebf2f575996'),
 'm3/collections/src/main/native/collections/m3_index_mask_compactor.c':('native-zero-mask-window-c','hash-pinned-text','59b1bb47f2df09f1455e4ce2160becbeefd6da877ecddca4ad245a286d14fa48','a549b92b38eaa337c0e95f32c46607cace9ae46d8240d37bd58a3a48e296990d')}
def digest(p):
 if not p.is_file() or p.is_symlink():raise RuntimeError('missing/aliased source: '+str(p))
 return hashlib.sha256(p.read_bytes()).hexdigest()
for path,(crate,kind,before,after) in EXPECTED.items():
 owner=RES/kind/crate
 rows=[line.split('\t') for line in (owner/'manifest.tsv').read_text().splitlines() if line and not line.startswith('#')]
 if len(rows)!=1 or rows[0][:3]!=[path,before,after]:raise RuntimeError('manifest lineage drift')
 payload=owner/rows[0][3]
 if digest(payload)!=after or digest(ROOT/path)!=after or payload.read_bytes()!=(ROOT/path).read_bytes():raise RuntimeError('receiver source drift')
 preimage=owner/('M3IndexMaskCompactorSelfTest.java.before' if kind=='hash-pinned-java' else 'm3_index_mask_compactor.c.before')
 if digest(preimage)!=before:raise RuntimeError('preimage drift')
print('PASS ZERO_MASK_WINDOW_RECEIVER: exact C and Java recipe outputs')
