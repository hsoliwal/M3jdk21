"""repin_apply.py <repin.tsv> <resources-root> <out-root> [<template-override> <new-template-file>] : PIN_RESEAL_APPLY v1.
Writes each manifest named in repin.tsv to <out-root> with exactly the listed rows changed: column 3 (postimage sha256)
old -> new and column 4 (template file name) old -> new. Every other byte of the manifest is kept. An optional template
override replaces one template file (crate-relative path under <resources-root>) by the given file and re-pins its row to
the new file's sha256. Refuses when a listed row is not found exactly once."""
import hashlib
import sys
from pathlib import Path

rows = [l.split("\t") for l in Path(sys.argv[1]).read_text(encoding="utf-8").splitlines() if l]
root, out = Path(sys.argv[2]), Path(sys.argv[3])
override = (sys.argv[4], Path(sys.argv[5])) if len(sys.argv) > 5 else None
by_manifest = {}
for manifest, old_name, new_name, old_sha, new_sha in rows:
    by_manifest.setdefault(manifest, []).append((old_name, new_name, old_sha, new_sha))
if override:
    rel, new_file = override
    template = root / rel
    new_sha = hashlib.sha256(new_file.read_bytes()).hexdigest()
    manifest = (template.parent / "manifest.tsv").as_posix()
    entries = by_manifest.setdefault(manifest, [])
    name = template.name
    current = [e for e in entries if e[1] == name]
    if current:
        entries[entries.index(current[0])] = (current[0][0], name, current[0][2], new_sha)
    else:
        text = Path(manifest).read_text(encoding="utf-8")
        hit = [l.split("\t") for l in text.splitlines() if l.endswith("\t" + name)]
        if len(hit) != 1:
            sys.exit(f"REFUSE override row for {name}: {len(hit)} rows")
        entries.append((name, name, hit[0][2], new_sha))
    dest = out / template.relative_to(root)
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_bytes(new_file.read_bytes())
    print(f"TEMPLATE\t{rel}\tsha256={new_sha}")
for manifest, entries in by_manifest.items():
    data = Path(manifest).read_bytes()
    for old_name, new_name, old_sha, new_sha in entries:
        old = f"\t{old_sha}\t{old_name}".encode()
        if data.count(old) != 1:
            sys.exit(f"REFUSE {manifest}: row ending {old_sha[:12]} {old_name} found {data.count(old)}x")
        data = data.replace(old, f"\t{new_sha}\t{new_name}".encode())
    dest = out / Path(manifest).relative_to(root)
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_bytes(data)
    print(f"MANIFEST\t{Path(manifest).relative_to(root).as_posix()}\trows={len(entries)}")
