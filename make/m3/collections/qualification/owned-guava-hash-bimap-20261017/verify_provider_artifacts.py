# SPDX-License-Identifier: Apache-2.0
import hashlib
import json
import sys
from pathlib import Path

rows=json.loads(Path(__file__).with_name("ARTIFACT_RECEIPTS.json").read_text())["artifacts"]
expected={row["url"].rsplit("/",1)[1]:(row["sha256"],row["bytes"]) for row in rows}
expected.update({
 "jspecify-1.0.0.jar":("1fad6e6be7557781e4d33729d49ae1cdc8fdda6fe477bb0cc68ce351eafdfbab",3819),
 "error_prone_annotations-2.36.0.jar":("77440e270b0bc9a249903c5a076c36a722c4886ca4f42675f2903a1c53ed61a5",19401),
 "j2objc-annotations-3.0.0.jar":("88241573467ddca44ffd4d74aa04c2bbfd11bf7c17e0c342c94c9de7a70a7c64",12467)})
paths=[Path(arg) for arg in sys.argv[1:]]
if len(paths)!=5 or len({p.name for p in paths})!=5 or {p.name for p in paths}!=set(expected):
 raise RuntimeError("exact two providers and three annotations required")
for path in paths:
 sha,size=expected[path.name]
 if path.stat().st_size!=size or hashlib.sha256(path.read_bytes()).hexdigest()!=sha:
  raise RuntimeError("pinned provider/annotation byte drift: "+path.name)
print("PASS PROVIDER BYTE CUSTODY: both Guava33.4.8 variants and all three annotation artifacts")
