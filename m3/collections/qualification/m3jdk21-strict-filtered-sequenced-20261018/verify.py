#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Verify exact source-first SHA256 outputs in M3JDK21 without writing files."""
from pathlib import Path
from hashlib import sha256

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[3]
manifest = HERE / "manifest.tsv"
rows = [line.split("\t") for line in manifest.read_text(encoding="utf-8").splitlines()
        if line and not line.startswith("#")]
if len(rows) != 3:
    raise SystemExit("refuse: unexpected sealed target count")
expected_prefix = "m3/collections/src/"
for path, before, after, template in rows:
    if not path.startswith(expected_prefix) or Path(path).is_absolute() or ".." in Path(path).parts:
        raise SystemExit("refuse: invalid target path " + path)
    if len(after) != 64 or (before != "ABSENT" and len(before) != 64):
        raise SystemExit("refuse: invalid SHA256")
    actual_file = REPO / path
    template_file = HERE / "templates" / template
    actual = sha256(actual_file.read_bytes()).hexdigest()
    source_template = sha256(template_file.read_bytes()).hexdigest()
    if actual != after or source_template != after:
        raise SystemExit(f"refuse: output/template drift: {path} {actual} {source_template} expected {after}")
    print(f"SEALED {path} {after}")
old_target = HERE / "pre" / "M3FilteredViews.java.txt"
if sha256(old_target.read_bytes()).hexdigest() != rows[0][1]:
    raise SystemExit("refuse: original owning source preimage changed")
print("PASS: exactly 3 generated product files match canonical Synexia templates")
