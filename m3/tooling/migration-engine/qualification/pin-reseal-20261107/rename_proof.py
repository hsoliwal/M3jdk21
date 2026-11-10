"""rename_proof.py <git-dir> <pre-rename-rev> <pre_rename_tree.txt> <resources-root> <jdk-naming-dir> : PIN_RENAME_PROOF v1.
For every manifest row of the hash-pinned crates under <resources-root> whose pinned postimage equals the sha256 of the
pre-rename blob of the same crate file (RENAME_DAMAGE), applies jdk_naming.rewrite_text (the rename's own rule table)
to that pre-rename blob and compares the bytes with today's template. EXACT means today's template is precisely the
renamed pinned text, so carrying the pin to sha256(today's template) is the reseal the rename skipped. Prints one row
per pinned template (EXACT / DIFFERS / NOT_RENAME_DAMAGE / MISSING) and writes REPIN rows (crate, name, old, new) for
the EXACT ones to repin.tsv; rows whose 4th column names a pre-rename template file get the renamed name there too."""
import hashlib
import importlib.util
import os
import re
import subprocess
import sys
from pathlib import Path

gd, rev, tree, root, jn_dir = sys.argv[1:6]
spec = importlib.util.spec_from_file_location("jdk_naming", Path(jn_dir) / "jdk_naming.py")
jn = importlib.util.module_from_spec(spec)
spec.loader.exec_module(jn)
SHA = re.compile(r"^[0-9a-f]{64}$")

paths = {}
for line in Path(tree).read_text(encoding="utf-8").splitlines():
    meta, path = line.split("\t", 1)
    paths[path] = meta.split()[2]
proc = subprocess.run(["git", "--git-dir", gd, "cat-file", "--batch"], input="\n".join(paths.values()).encode() + b"\n",
                      capture_output=True, env=dict(os.environ, GIT_NO_LAZY_FETCH="1"))
data, pos, pre = proc.stdout, 0, {}
for path in paths:
    end = data.index(b"\n", pos)
    size = int(data[pos:end].split()[2])
    body = data[end + 1:end + 1 + size]
    pos = end + 1 + size + 1
    key = (path.rsplit("/", 2)[1].replace("synexia", "m3"), path.rsplit("/", 1)[1].replace("Synexia", "M3"))
    pre.setdefault(key, []).append(body)

counts, repins = {}, []
for manifest in sorted(Path(root).rglob("manifest.tsv")):
    crate = manifest.parent.name
    for line in manifest.read_text(encoding="utf-8").splitlines():
        cells = line.split("\t")
        if len(cells) != 4 or not SHA.match(cells[2]) or not (cells[1] == "ABSENT" or SHA.match(cells[1])):
            continue
        name = cells[3].replace("Synexia", "M3")
        template = manifest.parent / name
        if not template.exists():
            kind = "MISSING"
        else:
            now = template.read_bytes()
            olds = [b for b in pre.get((crate, name), []) if hashlib.sha256(b).hexdigest() == cells[2]]
            if hashlib.sha256(now).hexdigest() == cells[2]:
                kind = "PINNED_OK" if name == cells[3] else "NAME_ONLY"
                if kind == "NAME_ONLY":
                    repins.append((manifest, cells[3], name, cells[2], cells[2]))
            elif not olds:
                kind = "NOT_RENAME_DAMAGE"
            else:
                renamed, _ = jn.rewrite_text(jn.decoded(olds[0]))
                kind = "EXACT" if jn.encoded(renamed) == now else "DIFFERS"
                if kind == "EXACT":
                    repins.append((manifest, cells[3], name, cells[2], hashlib.sha256(now).hexdigest()))
        counts[kind] = counts.get(kind, 0) + 1
        if kind not in ("PINNED_OK",):
            print(f"{kind}\t{crate}\t{cells[3]}")
with open("repin.tsv", "w", encoding="utf-8", newline="\n") as out:
    for manifest, old_name, new_name, old, new in repins:
        out.write(f"{manifest.as_posix()}\t{old_name}\t{new_name}\t{old}\t{new}\n")
print("SUMMARY " + " ".join(f"{k}={v}" for k, v in sorted(counts.items())) + f" repins={len(repins)}")
