#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Recipe RETAINED_PORT v2 (m3/file, tree-lanes-port-20261107): hash-pinned restore of the held tree-lane leaves.

RESTORE.tsv rows name the source git blob (the JDK-named master copy of the retained PR head under
m3/qualification/master-landing-20261028/retained-variants/pr<pr>/, identical to the file at the retain merge's parent
outside package/import lines; both ids are recorded) and the target path. The blob is read from the object store
(git cat-file), its git id is re-verified, then the recorded transforms run in this order:
  1. package rename of the pre-rename donor package to jdk.m3.file and of its collections imports to jdk.m3.collections (a no-op on the
     JDK-named copies, kept so the parent blobs replay to the same bytes);
  2. the rule of the row: RESTORE_EXACT / TEST (no further change) or ADAPT:HOIST_DEFAULT_RESIDENCY (the constant
     M3FileTree.DEFAULT_RESIDENCY of the held tree owner, re-read from its retained blob and verified, is hoisted into
     the spec as DEFAULT_RESIDENCY and both references are rewritten);
  3. one //FLLDCIM anchor line after the package line (source blob, parent blob, PR, rule, recipe).
The resulting bytes are pinned by sha256_after. EDITS.tsv rows are anchored insertions into existing owners, pinned by
before/after sha256 (none in this recipe; the table is kept so the replay shape equals retained-port-20261107).
Every write refuses drift (exit 2): a blob, a pinned result or an owner that differs from the pin is never overwritten.
--check is the fixed point (exit 0 when nothing would change); --pin writes the missing pins once.
"""
import argparse, hashlib, os, re, subprocess, sys

RECIPE = "m3/file/qualification/tree-lanes-port-20261107"
HERE = os.path.dirname(os.path.abspath(__file__))
PACKAGE_RULES = (
    (re.compile(r"^package com\.m3\.file;$", re.M), "package jdk.m3.file;"),
    (re.compile(r"^import com\.m3\.collections\.", re.M), "import jdk.m3.collections."),
    (re.compile(r"^import static com\.m3\.collections\.", re.M), "import static jdk.m3.collections."),
)
# The held owner of the hoisted constant: retained PR #1619 head copy of M3FileTree (not ported; see DECISION.tsv).
HOIST_OWNER_SOURCE = ("m3/qualification/master-landing-20261028/retained-variants/pr1619/m3/file/src/main/java/jdk/m3/file/"
                      "M3FileTree.java")
HOIST_DECL = re.compile(r"^    static final int DEFAULT_RESIDENCY = (\d+);$", re.M)
HOIST_REF = "M3FileTree.DEFAULT_RESIDENCY"
HOIST_ANCHOR = "    public static final M3FileScanSpec EAGER = "


def sha256(data):
    return hashlib.sha256(data).hexdigest()


def read_tsv(path):
    rows = []
    with open(path, encoding="utf-8", newline="") as handle:
        header = None
        for line in handle:
            line = line.rstrip("\r\n")
            if not line or line.startswith("#"):
                continue
            cells = line.split("\t")
            if header is None:
                header = cells
                continue
            rows.append(dict(zip(header, cells)))
    return header, rows


def write_tsv(path, header, rows, comments):
    with open(path, "w", encoding="utf-8", newline="") as handle:
        for comment in comments:
            handle.write(comment + "\n")
        handle.write("\t".join(header) + "\n")
        for row in rows:
            handle.write("\t".join(row.get(column, "") for column in header) + "\n")


def comments_of(path):
    with open(path, encoding="utf-8") as handle:
        return [line.rstrip("\r\n") for line in handle if line.startswith("#")]


def git_blob(root, blob):
    data = subprocess.run(["git", "cat-file", "blob", blob], cwd=root, check=True, capture_output=True).stdout
    header = ("blob %d\0" % len(data)).encode("ascii")
    actual = hashlib.sha1(header + data).hexdigest()
    if actual != blob:
        raise SystemExit("blob id mismatch for %s: object store returned %s" % (blob, actual))
    return data


def hoist_value(root, owner_blob):
    """The constant value, re-read from the held owner's retained blob (refuses when the declaration is not found once)."""
    text = git_blob(root, owner_blob).decode("utf-8")
    found = HOIST_DECL.findall(text)
    if len(found) != 1:
        raise SystemExit("DEFAULT_RESIDENCY declared %d times in %s" % (len(found), HOIST_OWNER_SOURCE))
    return found[0]


def hoist(text, value, owner_blob):
    if text.count(HOIST_REF) != 2:
        raise SystemExit("%s occurs %d times, expected 2" % (HOIST_REF, text.count(HOIST_REF)))
    text = text.replace(HOIST_REF, "DEFAULT_RESIDENCY")
    if text.count(HOIST_ANCHOR) != 1:
        raise SystemExit("hoist anchor occurs %d times" % text.count(HOIST_ANCHOR))
    declaration = (
        "    //FLLDCIM: MRL - ADAPT HOIST_DEFAULT_RESIDENCY: constant DEFAULT_RESIDENCY = %s of the held tree owner "
        "(%s blob %s, retained PR #1619 head) hoisted into the spec; both references rewritten; recipe %s\n"
        "    /** Default facade residency table size (hoisted from the held tree owner). */\n"
        "    public static final int DEFAULT_RESIDENCY = %s;\n\n" % (value, HOIST_OWNER_SOURCE, owner_blob, RECIPE, value))
    return text.replace(HOIST_ANCHOR, declaration + HOIST_ANCHOR)


def transform(row, data, root, owner_blob):
    text = data.decode("utf-8")
    for pattern, replacement in PACKAGE_RULES:
        text = pattern.sub(replacement, text)
    rule = row["rule"]
    kind = "RESTORE"
    if rule == "ADAPT:HOIST_DEFAULT_RESIDENCY":
        text = hoist(text, hoist_value(root, owner_blob), owner_blob)
        kind = "MRL - ADAPT HOIST_DEFAULT_RESIDENCY"
    elif rule.startswith("ADAPT:"):
        raise SystemExit("unknown ADAPT rule %s" % rule)
    anchor = "//FLLDCIM: %s - source %s blob %s (retained PR #%s head; parent %s blob %s %s); recipe %s %s" % (
        kind, row["source"], row["blob"], row["pr"], row["parent_sha"], row["parent_blob"], row["donor_url"], RECIPE, rule)
    match = re.search(r"^package jdk\.m3\.file;$", text, re.M)
    if match is None:
        raise SystemExit("no package line in %s" % row["target"])
    text = text[:match.end()] + "\n" + anchor + text[match.end():]
    return text.encode("utf-8")


def current(path):
    if not os.path.exists(path):
        return None
    with open(path, "rb") as handle:
        return handle.read()


def unescape(payload):
    return payload.replace("\\n", "\n")


def owner_blob_of(restore_path):
    for comment in comments_of(restore_path):
        if comment.startswith("# hoist_owner_blob="):
            return comment.split("=", 1)[1].strip()
    raise SystemExit("RESTORE.tsv has no '# hoist_owner_blob=' comment")


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", default=os.path.abspath(os.path.join(HERE, "..", "..", "..", "..")))
    parser.add_argument("--check", action="store_true", help="report the fixed point, write nothing")
    parser.add_argument("--pin", action="store_true", help="write missing sha256 pins into RESTORE.tsv / EDITS.tsv")
    args = parser.parse_args()
    root = args.root
    restore_path = os.path.join(HERE, "RESTORE.tsv")
    edits_path = os.path.join(HERE, "EDITS.tsv")
    header, rows = read_tsv(restore_path)
    eheader, edits = read_tsv(edits_path)
    owner_blob = owner_blob_of(restore_path)
    changed = applied = refused = 0
    pinned = 0
    for row in rows:
        target = os.path.join(root, row["target"])
        result = transform(row, git_blob(root, row["blob"]), root, owner_blob)
        digest = sha256(result)
        if not row["sha256_after"]:
            if not args.pin:
                print("UNPINNED %s" % row["target"]); refused += 1; continue
            row["sha256_after"] = digest; pinned += 1
        if row["sha256_after"] != digest:
            print("DRIFT recipe result %s: pinned %s computed %s" % (row["target"], row["sha256_after"][:12], digest[:12]))
            refused += 1; continue
        existing = current(target)
        if existing is not None and sha256(existing) == digest:
            continue
        if existing is not None:
            print("DRIFT target %s: on disk %s pinned %s" % (row["target"], sha256(existing)[:12], digest[:12]))
            refused += 1; continue
        changed += 1
        if args.check:
            print("WOULD_WRITE %s" % row["target"]); continue
        os.makedirs(os.path.dirname(target), exist_ok=True)
        with open(target, "wb") as handle:
            handle.write(result)
        applied += 1
        print("WROTE %s %s" % (row["target"], digest[:12]))
    for edit in edits:
        target = os.path.join(root, edit["file"])
        existing = current(target)
        if existing is None:
            print("MISSING owner %s" % edit["file"]); refused += 1; continue
        before = sha256(existing)
        if edit["after_sha256"] and before == edit["after_sha256"]:
            continue
        if before != edit["before_sha256"]:
            print("DRIFT owner %s: on disk %s pinned before %s" % (edit["file"], before[:12], edit["before_sha256"][:12]))
            refused += 1; continue
        text = existing.decode("utf-8")
        anchor = edit["anchor"]
        if text.count(anchor) != 1:
            print("ANCHOR %s occurs %d times in %s" % (anchor.strip(), text.count(anchor), edit["file"])); refused += 1; continue
        payload = unescape(edit["payload"])
        if edit["op"] == "INSERT_BEFORE":
            text = text.replace(anchor, payload + anchor)
        elif edit["op"] == "INSERT_AFTER":
            text = text.replace(anchor, anchor + payload)
        else:
            print("OP %s unknown" % edit["op"]); refused += 1; continue
        result = text.encode("utf-8")
        digest = sha256(result)
        if not edit["after_sha256"]:
            if not args.pin:
                print("UNPINNED edit %s" % edit["file"]); refused += 1; continue
            edit["after_sha256"] = digest; pinned += 1
        if digest != edit["after_sha256"]:
            print("DRIFT edit result %s: pinned %s computed %s" % (edit["file"], edit["after_sha256"][:12], digest[:12]))
            refused += 1; continue
        changed += 1
        if args.check:
            print("WOULD_EDIT %s" % edit["file"]); continue
        with open(target, "wb") as handle:
            handle.write(result)
        applied += 1
        print("EDITED %s %s -> %s" % (edit["file"], before[:12], digest[:12]))
    if args.pin and pinned and not args.check:
        write_tsv(restore_path, header, rows, comments_of(restore_path))
        write_tsv(edits_path, eheader, edits, comments_of(edits_path))
    print("rules=%d edits=%d applied=%d changed=%d pinned=%d refused=%d" % (len(rows), len(edits), applied, changed, pinned, refused))
    if refused:
        sys.exit(2)
    if args.check and changed:
        sys.exit(1)


if __name__ == "__main__":
    main()
