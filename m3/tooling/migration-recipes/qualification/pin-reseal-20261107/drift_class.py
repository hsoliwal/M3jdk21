"""drift_class.py <git-dir> <pre-rename-rev> <pre_rename_tree.txt> <pin_audit.txt> <resources-root> : PIN_DRIFT_CLASSIFY v1.
Classifies each HASH_DRIFT / MISSING_TEMPLATE finding of pin_audit.py against the tree before the JDK-naming rename
fe35ea98841. A pinned postimage that equals the sha256 of a pre-rename blob of the same crate file name (Synexia ->
M3 applied to the name) is RENAME_DAMAGE: the rename rewrote the template text but the .tsv pin was not carried.
Anything else is OLDER_DRIFT (the pin was already stale before the rename) and is reported, not repaired."""
import hashlib
import subprocess
import sys
from pathlib import Path

gd, rev, tree, audit, root = sys.argv[1:6]
blobs = {}
for line in Path(tree).read_text(encoding="utf-8").splitlines():
    meta, path = line.split("\t", 1)
    blobs[path] = meta.split()[2]
proc = subprocess.run(["git", "--git-dir", gd, "cat-file", "--batch"], input="\n".join(blobs.values()).encode() + b"\n",
                      capture_output=True, env={"GIT_NO_LAZY_FETCH": "1", "PATH": __import__("os").environ["PATH"]})
data, pos, sha_of = proc.stdout, 0, {}
for path, oid in blobs.items():
    header_end = data.index(b"\n", pos)
    size = int(data[pos:header_end].split()[2])
    body = data[header_end + 1:header_end + 1 + size]
    pos = header_end + 1 + size + 1
    sha_of[path] = hashlib.sha256(body).hexdigest()
by_name = {}
for path, digest in sha_of.items():
    name = path.rsplit("/", 1)[1].replace("Synexia", "M3")
    crate = path.rsplit("/", 2)[1].replace("synexia", "m3")
    by_name.setdefault((crate, name), set()).add(digest)
all_shas = set(sha_of.values())
counts = {}
for line in Path(audit).read_text(encoding="utf-8").splitlines():
    cells = line.split("\t")
    if cells[0] not in ("HASH_DRIFT", "MISSING_TEMPLATE"):
        continue
    crate, name = cells[1], cells[2].replace("Synexia", "M3")
    if cells[0] == "HASH_DRIFT":
        pinned = cells[3].split("=")[1]
        hit = any(d.startswith(pinned) for d in by_name.get((crate, name), ()))
        anywhere = any(d.startswith(pinned) for d in all_shas)
        kind = "RENAME_DAMAGE" if hit else ("PIN_OF_OTHER_PRE_RENAME_FILE" if anywhere else "OLDER_DRIFT")
    else:
        kind = "STALE_TEMPLATE_NAME" if cells[3] != "renamed_candidate=-" else "TEMPLATE_ABSENT"
    counts[kind] = counts.get(kind, 0) + 1
    print(f"{kind}\t{crate}\t{cells[2]}")
print("SUMMARY " + " ".join(f"{k}={v}" for k, v in sorted(counts.items())))
