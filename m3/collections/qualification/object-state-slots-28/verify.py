#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed receiver receipt: verify exact generated source and immutable M3 owners."""
import hashlib
from pathlib import Path
here = Path(__file__).resolve().parent
root = here.parents[3]
for manifest, relative in (("SOURCE_RECEIPT.tsv", root), ("M3_OWNER_SHA256.tsv", root / "m3/collections/src/main/java/com/m3/collections")):
    seen = set()
    for line in (here / manifest).read_text().splitlines():
        if not line or line.startswith("#"):
            continue
        path, digest = line.split("\t")
        assert path not in seen and len(digest) == 64 and all(c in "0123456789abcdef" for c in digest)
        seen.add(path)
        file = relative / path
        assert file.is_file() and not file.is_symlink(), path
        assert hashlib.sha256(file.read_bytes()).hexdigest() == digest, path
    assert len(seen) == (2 if manifest == "SOURCE_RECEIPT.tsv" else 4)
print("PASS M3 object state source and owner seals")
