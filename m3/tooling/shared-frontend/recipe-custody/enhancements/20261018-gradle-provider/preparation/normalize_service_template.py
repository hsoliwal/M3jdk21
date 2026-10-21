#!/usr/bin/env python3
"""Normalize this sealed task's template basename; never write a receiving source."""
from pathlib import Path
import hashlib
import json

root = Path(__file__).resolve().parent
lane = root / "canonical/synexia-openrewrite-recipes/recipe-crates/m3-ast-build-reverse-20261017/datasets/gradle-provider-20261018/resources/com/synexia/rewrite/hash-pinned-text/m3-ast-build-reverse-20261017-gradle-provider-20261018"
manifest = lane / "manifest.tsv"
before = manifest.read_bytes()
rows = [line.split("\t") for line in before.decode().splitlines()]
target = "m3/tooling/module-packs/build-support/gradle-provider/src/test/resources/META-INF/services/com.sun.source.util.M3AST$Consumer"
selected = [row for row in rows if row[0] == target]
assert len(selected) == 1
row = selected[0]
old = "0008-com.sun.source.util.M3AST$Consumer.txt"
new = "0008-M3AST-Consumer.txt"
assert row[3] == old
data = (lane / old).read_bytes()
assert hashlib.sha256(data).hexdigest() == row[2]
if (lane / new).exists():
    assert (lane / new).read_bytes() == data
else:
    (lane / new).write_bytes(data)
# Keep the prepare-produced, unreferenced resource as part of the frozen input
# inventory. Only the admitted template reference is normalized; target bytes
# and the original preparation output remain available for exact replay.
row[3] = new
after = ("\n".join("\t".join(item) for item in rows) + "\n").encode()
manifest.write_bytes(after)
print(json.dumps({"target": target, "target_sha256": row[2], "old_template": old, "new_template": new,
                  "original_template_retained": True, "manifest_pre_sha256": hashlib.sha256(before).hexdigest(),
                  "manifest_post_sha256": hashlib.sha256(after).hexdigest(), "source_content_changes": 0}))
