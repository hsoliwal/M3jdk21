#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""check_rows.py [--root DIR]: reader checks for the RESTORED rows of loss-guard-restore-20261106/HELD.tsv.

1. every row has the header's 5 fields; the original rows are a byte prefix of the file (kept, not rewritten);
2. path-keyed reading (hold.py convention, last row wins) resolves both restored paths to their RESTORED row;
3. path-set reading (restore3.py convention: skip every HELD.tsv path) still contains both paths;
4. for each RESTORED row: the live file exists with blob = source_blob and sha256 = the evidence's held sha256, and the
   held/ copy is absent, so an additive restore of these paths (restore.py refuses only different content) is a no-op;
5. no file in the repository other than this recipe directory names this HELD.tsv as an input."""
import argparse
import hashlib
import subprocess
import sys
from pathlib import Path

HELD_TSV = "m3/qualification/loss-guard-restore-20261106/HELD.tsv"
HELD_DIR = "m3/qualification/loss-guard-restore-20261106/held/"
ORIGINAL_BLOB = "b5c1f1083ec530f2581c7279bfb81fc37d6afae5"


def blob_sha(data: bytes) -> str:
    return hashlib.sha1(b"blob %d\0" % len(data) + data).hexdigest()


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", default=str(Path(__file__).resolve().parents[3]))
    root = Path(parser.parse_args().root)
    data = (root / HELD_TSV).read_bytes()
    original = subprocess.run(["git", "-C", str(root), "cat-file", "blob", ORIGINAL_BLOB], capture_output=True,
                              check=True).stdout
    failures = []
    lines = data.decode("utf-8").splitlines()
    header = lines[0].split("\t")
    bad = [i for i, line in enumerate(lines[1:], 2) if len(line.split("\t")) != len(header)]
    print("rows=%d fields=%d malformed=%s" % (len(lines) - 1, len(header), bad or 0))
    failures += ["malformed rows %s" % bad] if bad else []
    prefix = data.startswith(original)
    print("original rows kept as a byte prefix: %s" % prefix)
    failures += [] if prefix else ["original rows rewritten"]
    keyed = {line.split("\t")[0]: line.split("\t") for line in lines[1:]}
    paths = {line.split("\t")[0] for line in lines[1:]}
    restored = [line.split("\t") for line in lines[1:] if line.split("\t")[2].startswith("RESTORED")]
    for path, blob, reason, evidence, _ in restored:
        live = root / path
        sha = evidence.split("held sha256 ")[1].split(";")[0]
        live_ok = live.exists() and blob_sha(live.read_bytes()) == blob \
            and hashlib.sha256(live.read_bytes()).hexdigest() == sha
        held_absent = not (root / (HELD_DIR + path)).exists()
        resolved = keyed[path][2].startswith("RESTORED")
        print("%s: keyed=%s in_skip_set=%s live_blob=source_blob+sha256=%s held_copy_absent=%s" % (
            path.rsplit("/", 1)[1], "RESTORED" if resolved else "HELD", path in paths, live_ok, held_absent))
        failures += [path] if not (resolved and path in paths and live_ok and held_absent) else []
    print("restored rows=%d" % len(restored))
    failures += [] if len(restored) == 2 else ["expected 2 RESTORED rows"]
    refs = subprocess.run(["git", "-C", str(root), "grep", "-l", "loss-guard-restore-20261106/HELD.tsv", "--", "."],
                          capture_output=True, text=True).stdout.split()
    readers = [r for r in refs if not r.startswith("m3/qualification/held-restored-rows-20261107/")]
    print("other files naming this HELD.tsv: %d %s" % (len(readers), readers))
    print("CHECK_ROWS %s" % ("PASS" if not failures else "FAIL " + "; ".join(failures)))
    return 0 if not failures else 1


if __name__ == "__main__":
    sys.exit(main())
