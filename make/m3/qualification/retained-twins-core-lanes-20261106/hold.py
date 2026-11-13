# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""hold.py <worktree> <leaf-dir> <path> <gate> <reason...> : a restored file that fails its compile gate against master
goes to <leaf-dir>/held/<path> (the restored bytes, sha256-pinned in HELD.tsv) and the live <path> returns to master's
content (the blob at $HOLD_BASE, default HEAD); the live tree then carries no file this leaf could not compile. Idempotent: a path
already held is re-pinned, not duplicated. HELD.tsv: path, labels (from RESTORE_LEDGER.tsv), gate, result_sha256,
reason."""
import hashlib
import os
import subprocess
import sys
from pathlib import Path

HEADER = "path\tlabels\tgate\theld_sha256\treason\n"


def main(argv):
    wt, leaf, path, gate = Path(argv[1]), Path(argv[2]), argv[3], argv[4]
    reason = " ".join(argv[5:])
    ledger = leaf / "RESTORE_LEDGER.tsv"
    labels = sorted({l.split("\t")[1] for l in list(open(ledger, encoding="utf-8"))[1:]
                     if l.split("\t")[2] == path and l.split("\t")[0] not in ("COVERED", "3WAY_CLEAN=live")},
                    key=lambda s: int(s[2:]))
    if not labels:
        raise SystemExit("REFUSED: %s is not a restored path of this leaf" % path)
    live = wt / path
    held = leaf / "held" / path
    base = os.environ.get("HOLD_BASE", "HEAD")  # the master the restore is gated against (HEAD before the commit)
    master = subprocess.run(["git", "-C", str(wt), "show", f"{base}:{path}"], capture_output=True, check=True).stdout
    if live.exists():
        data = live.read_bytes()
        if data != master:  # the live file still carries the restored bytes: move them aside, live := master's bytes
            held.parent.mkdir(parents=True, exist_ok=True)
            held.write_bytes(data)
            live.write_bytes(master)
    if not held.exists():
        raise SystemExit("REFUSED: nothing to hold for %s (live file equals master and no held copy)" % path)
    rows = {}
    tsv = leaf / "HELD.tsv"
    if tsv.exists():
        for l in list(open(tsv, encoding="utf-8"))[1:]:
            f = l.rstrip("\n").split("\t")
            rows[f[0]] = f
    rows[path] = [path, ",".join(labels), gate, hashlib.sha256(held.read_bytes()).hexdigest(), reason]
    with open(tsv, "w", encoding="utf-8", newline="\n") as o:
        o.write(HEADER)
        o.write("".join("\t".join(rows[k]) + "\n" for k in sorted(rows)))
    print("HELD", path, ",".join(labels), gate)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
