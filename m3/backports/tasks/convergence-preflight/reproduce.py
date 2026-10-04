# SPDX-License-Identifier: Apache-2.0
"""Run against a source directory; report observations, never mutate real JDK sources."""
import csv
import hashlib
import importlib.util
import json
import os
import sys
import tempfile
from pathlib import Path

sys.path.insert(0, sys.argv[1])
import source_convergence_gate as gate
import materialize_source_convergence as materializer

def sha(b):
    return hashlib.sha256(b).hexdigest()

def fixture(root):
    source, work, evidence = [root / n for n in ("source", "work", "evidence")]
    entries = []
    for name in ("A", "B"):
        path = f"src/example/{name}.java"
        before = f"class {name} {{}}\n".encode()
        after = f"class {name} {{ /* normalized */ }}\n".encode()
        for tree, payload in ((source, before), (work, before), (evidence / "candidates", after)):
            dest = tree / path
            dest.parent.mkdir(parents=True, exist_ok=True)
            dest.write_bytes(payload)
        entries.append([path, sha(before), sha(after), "true", "CONVERGED_CHANGED", "candidates/" + path])
    manifest = evidence / "SOURCE_CONVERGENCE.tsv"
    def save():
        with manifest.open("w", newline="") as out:
            writer = csv.writer(out, delimiter="\t", lineterminator="\n")
            writer.writerow(["path", "preSha256", "postSha256", "fixedPoint", "status", "candidate"])
            writer.writerows(entries)
    save()
    targets = root / "targets.txt"
    targets.write_text("\n".join(row[0] for row in entries) + "\n")
    return source, work, evidence, manifest, targets, entries, save

result = {}
with tempfile.TemporaryDirectory() as temp:
    root = Path(temp)
    s,w,e,m,t,rows,save=fixture(root)
    rows[0][4:]=["CONVERGED_UNCHANGED",""]
    save()
    try:
        gate.verify(s,m,t)
        result["inconsistent_unchanged_accepted"]=True
    except ValueError:
        result["inconsistent_unchanged_accepted"]=False
with tempfile.TemporaryDirectory() as temp:
    s,w,e,m,t,rows,save=fixture(Path(temp))
    (e/rows[1][5]).write_text("corrupt\n")
    before=(w/rows[0][0]).read_bytes()
    try:
        materializer.materialize(s,m,w)
    except ValueError:
        pass
    result["earlier_target_written_before_later_failure"]=(w/rows[0][0]).read_bytes()!=before
with tempfile.TemporaryDirectory() as temp:
    s,w,e,m,t,rows,save=fixture(Path(temp))
    target=w/rows[0][0]; canonical=s/rows[0][0]
    before=canonical.read_bytes()
    target.unlink(); os.link(canonical,target)
    try:
        materializer.materialize(s,m,w)
    except ValueError:
        pass
    result["canonical_original_mutated_through_hardlink"]=canonical.read_bytes()!=before
with tempfile.TemporaryDirectory() as temp:
    s,w,e,m,t,rows,save=fixture(Path(temp))
    first=materializer.materialize(s,m,w)
    try:
        second=materializer.materialize(s,m,w)
        result["successful_workspace_replay_rejected"]=first!=second
    except ValueError:
        result["successful_workspace_replay_rejected"]=True
print(json.dumps(result,indent=2,sort_keys=True))
