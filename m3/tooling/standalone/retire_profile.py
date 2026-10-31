# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""retire_profile.py <root> <profile_id> <out_dir> [--apply] [--restore-installer COMMIT] [--keep KEEP.tsv]
                    [--under PREFIX ...]

RETIRE_PROFILE v1 (M3jdk21 standalone): find every pom.xml under <root> holding <profile><id>PROFILE_ID</id>...,
patternize the blocks (whitespace-normalized SHA-256 per block -> variants), and with --apply remove each block
exactly (and a <profiles> element left empty), keeping every other byte. Fail-closed: a pom whose block is not one
balanced <profile>...</profile> element, or that no longer parses as XML after the edit, is not written.

RETIRE_PROFILE v2 (--restore-installer COMMIT, the commit that installed the profile). The installer also
re-serialised each pom it touched, one element per line, so the v1 edit leaves bytes that no custody pin recorded
before the installer can match. A pom whose bytes are still exactly the installer's blob (git blob identity: nothing
changed it since) gets the blob of the installer's parent back, byte for byte. That is admitted only when the parent
blob and the v1 edit are equal under XML C14N 2.0 (comments kept, whitespace-only text stripped), which proves the
installer changed nothing else in it; otherwise the pom is HELD and not written. A pom changed after the installer,
or created after it, keeps the v1 edit. --keep KEEP.tsv names poms left untouched (an exact path, or a directory
prefix ending in /), each with its reason: an owner pin recorded with the profile, or a path the public-names gate
refuses to change. --under PREFIX (repeatable, a repository-relative directory) fixes the scope by path, so a sparse
and a full checkout of that scope find the same poms; without it the whole <root> is walked.
Outputs: VARIANTS.tsv (variant sha, count, sample pom) and POMS.tsv (pom, variant, action, detail). Actions:
RESTORED (v2 byte restore), RETIRED (v1 edit), KEPT, HELD; a dry run writes PLANNED_RESTORE / PLANNED_RETIRE."""
import argparse
import hashlib
import os
import re
import subprocess
import xml.etree.ElementTree as ET

PLANNED = {"RESTORED": "PLANNED_RESTORE", "RETIRED": "PLANNED_RETIRE"}


def arguments():
    parser = argparse.ArgumentParser(description="RETIRE_PROFILE v1/v2: remove one Maven profile from every pom")
    parser.add_argument("root")
    parser.add_argument("profile_id")
    parser.add_argument("out_dir")
    parser.add_argument("--apply", action="store_true")
    parser.add_argument("--restore-installer", metavar="COMMIT")
    parser.add_argument("--keep", metavar="KEEP.tsv")
    parser.add_argument("--under", metavar="PREFIX", action="append", default=[])
    return parser.parse_args()


def patterns(pid):
    block = re.compile(r"[ \t]*<profile>\s*<id>" + re.escape(pid) + r"</id>.*?</profile>[ \t]*\r?\n", re.S)
    return block, re.compile(r"[ \t]*<profiles>\s*</profiles>[ \t]*\r?\n")


def holders(root, pid, under):
    """Sorted repository-relative paths of the pom.xml files under the scope prefixes that name the profile id."""
    found = []
    for top in [os.path.join(root, prefix) for prefix in under] or [root]:
        for dirpath, dirs, files in os.walk(top):
            dirs[:] = [d for d in dirs if d not in (".git", "target")]
            p = os.path.join(dirpath, "pom.xml")
            if "pom.xml" in files and f"<id>{pid}</id>" in open(p, encoding="utf-8", errors="replace").read():
                found.append(os.path.relpath(p, root).replace(os.sep, "/"))
    return sorted(set(found))


def v1_edit(text, block, empty):
    """(variant, edited text) or (None, HELD reason) for the exact v1 block removal."""
    m = list(block.finditer(text))
    if len(m) != 1 or m[0].group(0).count("<profile>") != 1:
        return None, f"blocks={len(m)}"
    variant = hashlib.sha256(re.sub(r"\s+", " ", m[0].group(0)).strip().encode()).hexdigest()[:16]
    new = empty.sub("", text[:m[0].start()] + text[m[0].end():], count=1)
    try:
        ET.fromstring(new.encode("utf-8"))
    except ET.ParseError as e:
        return None, f"parse {e}"
    return variant, new


def read_keep(path):
    """{path or directory prefix: reason} from a KEEP.tsv (header 'path', '#' comments)."""
    rows = {}
    for line in (open(path, encoding="utf-8").read().splitlines() if path else []):
        cells = line.split("\t")
        if line.strip() and not line.startswith("#") and cells[0] != "path":
            rows[cells[0]] = cells[1] if len(cells) > 1 else "KEEP"
    return rows


def kept(rel, keep):
    return next((why for key, why in keep.items() if rel == key or key.endswith("/") and rel.startswith(key)), None)


class Objects:
    """One `git cat-file --batch` process: (blob oid, bytes) of a <rev>:<path>, or None when it is absent."""

    def __init__(self, root):
        self.proc = subprocess.Popen(["git", "-C", root, "cat-file", "--batch"],
                                     stdin=subprocess.PIPE, stdout=subprocess.PIPE)

    def blob(self, spec):
        self.proc.stdin.write(spec.encode("utf-8") + b"\n")
        self.proc.stdin.flush()
        header = self.proc.stdout.readline().rstrip(b"\n").split(b" ")
        if len(header) != 3:
            return None
        data = self.proc.stdout.read(int(header[2]))
        self.proc.stdout.read(1)
        return (header[0].decode("ascii"), data) if header[1] == b"blob" else None


def clean_oids(root, rels):
    """{path: the blob oid git stores for the working file}: eol and filter attributes applied, so a checkout that
    converted line endings (core.autocrlf with a text attribute) compares the same as one that did not."""
    out = subprocess.run(["git", "-C", root, "hash-object", "--stdin-paths"], capture_output=True, check=True,
                         input="".join(rel + "\n" for rel in rels).encode("utf-8")).stdout.decode("ascii").split()
    if len(out) != len(rels):
        raise SystemExit("git hash-object --stdin-paths returned a different number of ids")
    return dict(zip(rels, out))


def canonical(data):
    return ET.canonicalize(data, with_comments=True, strip_text=True)


def restore(objects, installer, rel, oid, new):
    """v2 decision for one pom: (action, detail, payload); payload None keeps the v1 edit."""
    at_installer = objects.blob(f"{installer}:{rel}")
    if at_installer is None or at_installer[0] != oid:
        return "RETIRED", "changed or created after the installer; v1 edit", None
    parent = objects.blob(f"{installer}^:{rel}")
    if parent is None:
        return "RETIRED", "created by the installer; v1 edit", None
    try:
        equal = canonical(parent[1]) == canonical(new.encode("utf-8"))
    except ET.ParseError as e:
        return "HELD", f"C14N parse {e}", None
    if not equal:
        return "HELD", f"installer-parent blob {parent[0]} differs from the v1 edit under C14N", None
    return "RESTORED", f"installer-parent blob {parent[0]}; C14N-equal to the v1 edit", parent[1]


def decide(args, rel, keep, objects, oids, block, empty):
    """(variant, action, detail, payload to write or None) for one pom holding the profile."""
    try:
        raw = open(os.path.join(args.root, rel), "rb").read()
        variant, new = v1_edit(raw.decode("utf-8"), block, empty)
    except UnicodeDecodeError as e:
        return "-", "HELD", f"decode {e}", None
    if variant is None:
        return "-", "HELD", new, None
    why = kept(rel, keep)
    if why is not None:
        return variant, "KEPT", why, None
    if objects is None:
        return variant, "RETIRED", "v1 edit", new
    action, detail, payload = restore(objects, args.restore_installer, rel, oids[rel], new)
    if action == "HELD":
        return variant, action, detail, None
    return variant, action, detail, new if payload is None else payload


def write(root, rel, payload):
    path = os.path.join(root, rel)
    if isinstance(payload, bytes):
        open(path, "wb").write(payload)
    else:
        open(path, "w", encoding="utf-8", newline="").write(payload)


def ledgers(out, variants, rows):
    os.makedirs(out, exist_ok=True)
    with open(os.path.join(out, "VARIANTS.tsv"), "w", encoding="utf-8", newline="\n") as w:
        w.write("variant\tcount\tsample\n" + "".join(f"{h}\t{len(v)}\t{sorted(v)[0]}\n" for h, v in
                                                    sorted(variants.items(), key=lambda kv: -len(kv[1]))))
    with open(os.path.join(out, "POMS.tsv"), "w", encoding="utf-8", newline="\n") as w:
        w.write("pom\tvariant\taction\tdetail\n" + "".join("\t".join(r) + "\n" for r in sorted(rows)))


def main():
    args = arguments()
    block, empty = patterns(args.profile_id)
    keep = read_keep(args.keep)
    objects = Objects(args.root) if args.restore_installer else None
    poms = holders(args.root, args.profile_id, args.under)
    oids = clean_oids(args.root, poms) if objects else {}
    variants, rows = {}, []
    for rel in poms:
        variant, action, detail, payload = decide(args, rel, keep, objects, oids, block, empty)
        if variant != "-":
            variants.setdefault(variant, []).append(rel)
        if args.apply and payload is not None:
            write(args.root, rel, payload)
        rows.append((rel, variant, action if args.apply else PLANNED.get(action, action), detail))
    ledgers(args.out_dir, variants, rows)
    count = {a: sum(1 for r in rows if r[2] in (a, PLANNED.get(a))) for a in ("RESTORED", "RETIRED", "KEPT", "HELD")}
    print(f"poms={len(rows)} variants={len(variants)} restored={count['RESTORED']} retired={count['RETIRED']} "
          f"kept={count['KEPT']} held={count['HELD']} {'applied' if args.apply else 'dry-run'}")


if __name__ == "__main__":
    main()
