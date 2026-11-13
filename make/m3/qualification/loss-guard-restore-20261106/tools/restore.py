# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""restore.py <repo> <LOSS_CANDIDATES.tsv> <worktree> <RESTORE_LEDGER.tsv> [--class RESTORE]

Writes every RESTORE row of the ledger into the worktree, byte for byte from its source
(source_commit_or_blob = "<commit>:<path> blob <sha>" or "commit <sha> blob <blob>"), through one
git cat-file --batch over the source blobs (one lazy fetch batch on a blobless clone). Records
RESTORE_LEDGER.tsv (path, source, blob sha, bytes). Refuses to overwrite a path that already exists in the
worktree with different content (a restore is additive)."""
import hashlib
import subprocess
import sys
from pathlib import Path

repo, cand, wt, ledger = sys.argv[1:5]
wanted = sys.argv[sys.argv.index("--class") + 1] if "--class" in sys.argv else "RESTORE"
rows = []
for line in list(open(cand, encoding="utf-8"))[1:]:
    path, method, source, pr, note, cls = line.rstrip("\n").split("\t")
    if cls != wanted:
        continue
    blob = source.split(" blob ")[1].split()[0]
    rows.append((path, source, blob))
print("restoring", len(rows), "paths", file=sys.stderr)
listing = Path(ledger).with_suffix(".blobs.txt")
listing.write_text("".join(b + "\n" for _, _, b in rows), encoding="utf-8", newline="\n")  # stdin from a file: no pipe deadlock
p = subprocess.Popen(["git", "-C", repo, "cat-file", "--batch"], stdin=open(listing, "rb"), stdout=subprocess.PIPE)
out = []
for i, (path, source, blob) in enumerate(rows):
    hdr = p.stdout.readline().decode().split()
    if len(hdr) < 3 or hdr[1] == "missing":
        raise SystemExit(f"REFUSED: blob {blob} for {path} missing")
    size = int(hdr[2])
    data = p.stdout.read(size)
    p.stdout.read(1)
    got = hashlib.sha1(b"blob %d\0" % size + data).hexdigest()
    if got != blob:
        raise SystemExit(f"REFUSED: blob mismatch {path} {got} != {blob}")
    target = Path(wt) / path
    if target.exists() and target.read_bytes() != data:
        raise SystemExit(f"REFUSED: {path} exists with different content")
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_bytes(data)
    out.append((path, source, blob, str(size)))
    if i % 25 == 0:
        print(f"heartbeat restore {i + 1}/{len(rows)}", file=sys.stderr)
with open(ledger, "w", encoding="utf-8", newline="\n") as o:
    o.write("path\tsource\tblob_sha\tbytes\n")
    for r in out:
        o.write("\t".join(r) + "\n")
print("restored", len(out), "paths,", sum(int(r[3]) for r in out), "bytes", file=sys.stderr)
