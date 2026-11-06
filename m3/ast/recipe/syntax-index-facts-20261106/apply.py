#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe SYNTAX_INDEX_FACTS v1: hsoliwal/com.synexia M3AST recipe crates applied to the owner jdk.m3.ast.M3SyntaxIndex.

Owner: m3/ast/src/main/java/jdk/m3/ast/M3SyntaxIndex.java (M3JDK convergence census leaf L004). Every crate is one
hash-pinned snapshot recipe in the donor (manifest: target path, preimage sha256, postimage sha256); this recipe
re-applies the crate's pre -> post change to the M3JDK owner, crate by crate in the order their manifests stack (the
postimage of one crate is the preimage of the next), and relocates each crate's proof into the module's test style.

Rule kinds. EDIT: the crate's preimage and postimage, read as donor blobs and mapped to M3JDK names through the naming
recipe (m3/tooling/jdk-naming: jdk_naming.rewrite_text for text, target_path for the manifest's target path); the
owner without its //FLLDCIM: lines must equal the mapped preimage (EXACT_TARGET), otherwise the recipe refuses
(a diverged owner needs a declared EDIT row by method name); the postimage is the mapped crate postimage with every
earlier anchor kept before its declaration and one //FLLDCIM: MRL anchor per edited declaration of this crate.
FILE: the crate's proof relocated into src/test/java/jdk/m3/ast with the module header, package jdk.m3.ast, the
declared type rename and one //FLLDCIM: RELOCATE anchor before the top-level type; body_match is read back from disk
(every other line is the donor line, the donor line with the type rename, or the mapped package line). WRAPPER: the
module's JUnit entry point (Contract.main from a @Test method, as M3SyntaxIndexTest does).

Hash-pinned: every EDIT rule names the git blob of its preimage (the first at BASE_COMMIT, the next the blob the
previous rule produces); the rules of one file form a chain and the file on disk must equal one image of its chain
(applied up to that rule), anything else is a drifted preimage and the recipe refuses (exit 2) without writing.
PROVENANCE.tsv (input) names every donor blob; each row is checked against the donor tree at DONOR_COMMIT
(rev-parse of the path, sha256 of the blob) before use. Idempotent: a second run reports changed=0 and EDITS.tsv
(file, kind, crate, before blob, before sha256, after sha256, donor blobs, names, match, hunks, rule) is a fixed
point; --check writes nothing and exits 1 when a run would change a file. No donor package or path appears here
(the naming recipe rewrites .py files): donor paths live in PROVENANCE.tsv, donor bytes are read by blob sha.

usage: python -I apply.py --donor-git DIR [--root DIR] [--check]
"""
from __future__ import annotations

import argparse
import difflib
import hashlib
import importlib.util
import re
import subprocess
import sys
from pathlib import Path

sys.dont_write_bytecode = True
RECIPE = "apply.py"
VERSION = "v1"
RECIPE_DIR = Path(__file__).resolve().parent
RECIPE_DIR_REL = "m3/ast/recipe/syntax-index-facts-20261106"
BASE_COMMIT = "c4e268d495a0f87ea672f52c2f05c923562f4c04"
DONOR_REPO = "hsoliwal/com.synexia"
DONOR_COMMIT = "ca1aafc4d94f9dd1ee288226dcca070cc60991c9"
NAMING = "m3/tooling/jdk-naming/jdk_naming.py"
OWNER = "m3/ast/src/main/java/jdk/m3/ast/M3SyntaxIndex.java"
TEST_DIR = "m3/ast/src/test/java/jdk/m3/ast/"
TEST_PACKAGE = "jdk.m3.ast"
HEADER = ("// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors", "// SPDX-License-Identifier: Apache-2.0")
HEADER_LINE = re.compile(r"^(?:// |/\* )(?:SPDX-License-Identifier|SPDX-FileCopyrightText|Copyright)\b")
PACKAGE_LINE = re.compile(r"^package\s+[\w.]+\s*;\s*$")
TYPE_LINE = re.compile(r"^(?:(?:public|final|abstract|sealed|non-sealed|strictfp)\s+)*(?:class|interface|enum|record)\s+\w+")
ANCHOR = "//FLLDCIM:"
PROVENANCE_NAME = "PROVENANCE.tsv"
PLAN_NAME = "EDITS.tsv"
PLAN_COLUMNS = ("file", "kind", "crate", "before_blob", "before_sha256", "after_sha256", "donor_blobs", "names",
                "match", "hunks", "rule")
SCATTER = "m3ast-facts-stable-scatter-20261030"
POSTINGS = "m3ast-kind-postings-20261030"


class Refused(Exception):
    pass


# kind, crate, path, preimage blob ("-": new file), note, payload. EDIT payload: the declarations this crate edits,
# each (exact declaration line of the mapped crate postimage, anchor note). FILE payload: the type rename.
RULES = (
    # --- crate 1: m3ast-facts-stable-scatter-20261030 (manifest: preimage b48a1db2..., postimage 58f5bffd...) -------
    ("EDIT", SCATTER, OWNER, "04e811a24754ae86ea67664445ddb72fbc90ed70",
     "crate pre->post re-applied: java.util.Comparator import dropped; FactBuilder.freeze sorts no Fact objects, it"
     " counts per node, prefixes begins[], scatters in append (= insertion) order with begins[] as the cursor and"
     " restores the row starts; same five sidecar columns, O(F+N) instead of O(F log F + N)",
     (("        public Facts freeze() {",
       "stable count-prefix scatter replaces the (node, insertion) comparator sort; same sidecar columns"),)),
    ("FILE", SCATTER, TEST_DIR + "M3SyntaxIndexStableScatterContract.java", "-",
     "crate proof relocated: literal comparator-sort oracle vs the in-place prefix-cursor scatter over 20,004 maps"
     " (empty, sparse, duplicate keys, invalid ordinals, one 300,000-fact case)",
     ("M3AstFactStableScatterProof", "M3SyntaxIndexStableScatterContract")),
    ("WRAPPER", SCATTER, TEST_DIR + "M3SyntaxIndexStableScatterTest.java", "-",
     "JUnit entry point of the relocated proof (module style: Contract.main from one @Test)",
     ("M3SyntaxIndexStableScatterContract", "stableScatterEqualsSortedFacts", False)),
    # --- crate 2: m3ast-kind-postings-20261030 (manifest: preimage 58f5bffd... = crate 1 postimage, postimage
    # 7bfd83ec...); its preimage blob here is the owner blob crate 1 produces --------------------------------------
    ("EDIT", POSTINGS, OWNER, "7be2a8a488c1dfc1b476f6eb942417ad478a6a34",
     "crate pre->post re-applied: one private volatile KindPostings reference; nodesOfKind keeps the null check first,"
     " then builds the lazy immutable CSR lanes once (double-checked, synchronized) and answers by binary search over"
     " the sorted kind keys with a fresh copy of the source-ordered node range; private static final class"
     " KindPostings (keys, begins, counts, nodes); no public API change",
     (("    public int[] nodesOfKind(String grammarKind) {",
       "lazy CSR kind postings answer every query after the first by binary search; same source-ordered result"),
      ("    private static final class KindPostings {",
       "immutable kind lanes (sorted keys, begins, counts, source-ordered nodes) built once per index"))),
    ("FILE", POSTINGS, TEST_DIR + "M3SyntaxIndexKindPostingsContract.java", "-",
     "crate proof relocated: the original linear full scan vs the lazy postings over 9,002 maps (collision strings"
     " Aa/BB, empty and missing kinds, repeated calls, caller-mutated results, null queries, a 40,000-node corpus,"
     " eight concurrent cold readers)",
     ("M3AstKindPostingsProof", "M3SyntaxIndexKindPostingsContract")),
    ("WRAPPER", POSTINGS, TEST_DIR + "M3SyntaxIndexKindPostingsTest.java", "-",
     "JUnit entry point of the relocated proof (module style: Contract.main from one @Test)",
     ("M3SyntaxIndexKindPostingsContract", "kindPostingsEqualLinearScan", True)),
)


def sha256(data: bytes | None) -> str:
    return "-" if data is None else hashlib.sha256(data).hexdigest()


def blob_sha(data: bytes) -> str:
    return hashlib.sha1(b"blob %d\0" % len(data) + data).hexdigest()


def git(root: Path, *args: str) -> bytes:
    try:
        return subprocess.run(["git", "-C", str(root), *args], check=True, capture_output=True).stdout
    except (subprocess.CalledProcessError, OSError) as failure:
        raise Refused("git %s failed in %s (%s)" % (" ".join(args), root, failure)) from failure


def load_naming(root: Path):
    spec = importlib.util.spec_from_file_location("m3_jdk_naming", root / NAMING)
    if spec is None or spec.loader is None:
        raise Refused("naming recipe %s not found" % NAMING)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def read_provenance(donor: Path) -> dict:
    """(crate, role) -> donor blob bytes, after checking path, blob and sha256 against DONOR_COMMIT."""
    rows = [line.split("\t") for line in (RECIPE_DIR / PROVENANCE_NAME).read_text(encoding="utf-8").splitlines()
            if line and not line.startswith("#")][1:]
    blobs = {}
    for crate, role, commit, path, blob, digest in rows:
        if commit != DONOR_COMMIT:
            raise Refused("provenance row %s/%s pins commit %s, recipe pins %s" % (crate, role, commit, DONOR_COMMIT))
        actual = git(donor, "rev-parse", commit + ":" + path).decode().strip()
        data = git(donor, "cat-file", "blob", blob)
        if actual != blob or sha256(data) != digest:
            raise Refused("donor drift %s/%s: tree blob %s, pinned %s" % (crate, role, actual, blob))
        blobs[(crate, role)] = data
    return blobs


def donor_blob(blobs: dict, crate: str, role: str) -> bytes:
    if (crate, role) not in blobs:
        raise Refused("PROVENANCE.tsv has no %s row for %s" % (role, crate))
    return blobs[(crate, role)]


def eol_of(data: bytes) -> str:
    return "\r\n" if b"\r\n" in data else "\n"


def anchor_line(indent: str, crate: str, note: str, blob: str, kind: str) -> str:
    return "%s%s %s - %s: %s (%s %s, %s blob %s); recipe %s/%s %s" % (
        indent, ANCHOR, kind, crate, note, DONOR_REPO, DONOR_COMMIT[:12], "postimage" if kind == "MRL" else "donor",
        blob[:12], RECIPE_DIR_REL, RECIPE, VERSION)


def insert_before(lines: list, declaration: str, anchor: str, rel: str) -> None:
    at = [i for i, line in enumerate(lines) if line == declaration]
    if len(at) != 1:
        raise Refused("declaration occurs %d times (need 1) in %s: %r" % (len(at), rel, declaration[:60]))
    lines.insert(at[0], anchor)


def edit_post(crate: str, rel: str, pre: bytes, payload, blobs: dict, naming) -> tuple:
    manifest = donor_blob(blobs, crate, "manifest").decode("utf-8").splitlines()
    rows = [line.split("\t") for line in manifest if line and not line.startswith("#")]
    pre_d, post_d = donor_blob(blobs, crate, "pre"), donor_blob(blobs, crate, "post")
    if len(rows) != 1 or naming.target_path(rows[0][0]) != rel or rows[0][1:3] != [sha256(pre_d), sha256(post_d)]:
        raise Refused("crate %s manifest does not name %s with the pinned pre/post sha256" % (crate, rel))
    (mapped_pre, names_pre), (mapped_post, names_post) = (naming.rewrite_text(d.decode("utf-8")) for d in (pre_d, post_d))
    eol = eol_of(pre)
    lines = pre.decode("utf-8").replace(eol, "\n").split("\n")
    bound = [(line, lines[i + 1]) for i, line in enumerate(lines[:-1]) if line.lstrip().startswith(ANCHOR)]
    if "\n".join(line for line in lines if not line.lstrip().startswith(ANCHOR)) != mapped_pre:
        raise Refused("owner %s diverged from the mapped %s preimage: declare an EDIT row by method name" % (rel, crate))
    out = mapped_post.split("\n")
    for anchor, declaration in bound:
        insert_before(out, declaration, anchor, rel)
    post_blob = blob_sha(post_d)
    for declaration, note in payload:
        indent = declaration[:len(declaration) - len(declaration.lstrip())]
        insert_before(out, declaration, anchor_line(indent, crate, note, post_blob, "MRL"), rel)
    hunks = sum(line.startswith("@@") for line in difflib.unified_diff(
        mapped_pre.split("\n"), mapped_post.split("\n"), n=3, lineterm=""))
    names = ",".join("%s=%d" % kv for kv in sorted({**names_pre, **names_post}.items())) or "-"
    donors = "pre=%s post=%s manifest=%s" % (blob_sha(pre_d), post_blob, blob_sha(donor_blob(blobs, crate, "manifest")))
    return "\n".join(out).replace("\n", eol).encode("utf-8"), donors, names, "EXACT_TARGET", str(hunks)


def strip_header(lines: list) -> list:
    at = 0
    while at < len(lines) and HEADER_LINE.match(lines[at]):
        at += 1
    return lines[at:]


def rename(line: str, types: tuple) -> str:
    return re.sub(r"\b%s\b" % re.escape(types[0]), types[1], line)


def file_post(crate: str, rel: str, types: tuple, blobs: dict) -> tuple:
    data = donor_blob(blobs, crate, "proof")
    donor = strip_header(data.decode("utf-8").split("\n"))
    out, package_seen, anchored = list(HEADER), False, False
    for line in donor:
        if not package_seen and PACKAGE_LINE.match(line):
            package_seen = True
            out.append("package %s;" % TEST_PACKAGE)
            continue
        if not anchored and TYPE_LINE.match(line):
            anchored = True
            out.append(anchor_line("", crate, "crate proof relocated into the module test style; path, URL and"
                                   " digests in %s/%s" % (RECIPE_DIR_REL, PROVENANCE_NAME), blob_sha(data), "RELOCATE"))
        out.append(rename(line, types))
    if not package_seen or not anchored:
        raise Refused("donor proof of %s has no package line or no top-level type" % crate)
    return "\n".join(out).encode("utf-8"), "proof=%s" % blob_sha(data), "TYPE=1", None, "-"


def body_match(target: bytes, crate: str, types: tuple, blobs: dict) -> bool:
    """Read back from disk: every target line is explained by the donor line at its position and the rule."""
    lines = target.decode("utf-8").split("\n")
    donor = strip_header(donor_blob(blobs, crate, "proof").decode("utf-8").split("\n"))
    kept = [line for line in lines[len(HEADER):] if not line.startswith(ANCHOR)]
    if tuple(lines[:len(HEADER)]) != HEADER or sum(line.startswith(ANCHOR) for line in lines) != 1:
        return False
    if len(kept) != len(donor):
        return False
    package_seen = False
    for mine, theirs in zip(kept, donor):
        if mine in (theirs, rename(theirs, types)):
            continue
        if not package_seen and PACKAGE_LINE.match(theirs) and mine == "package %s;" % TEST_PACKAGE:
            package_seen = True
            continue
        return False
    return True


def wrapper_post(crate: str, payload: tuple) -> tuple:
    contract, method, throws = payload
    name = contract[:-len("Contract")] + "Test"
    text = "\n".join(HEADER + (
        "package %s;" % TEST_PACKAGE, "", "import org.junit.jupiter.api.Test;", "",
        "/** Maven/JUnit entry point of the proof relocated from crate %s. */" % crate,
        "final class %s {" % name,
        "    @Test void %s()%s {" % (method, " throws Exception" if throws else ""),
        "        %s.main(new String[0]);" % contract, "    }", "}", ""))
    return text.encode("utf-8"), "-", "-", "GENERATED", "-"


def run(root: Path, donor: Path, check: bool) -> int:
    naming, blobs = load_naming(root), read_provenance(donor)
    chains, disk, rows, matches = {}, {}, [], []
    for kind, crate, rel, blob, note, payload in RULES:
        if rel not in chains:
            target = root / rel
            disk[rel] = target.read_bytes() if target.exists() else None
            on_disk = disk[rel]
            start = None if blob == "-" else on_disk if on_disk is not None and blob_sha(on_disk) == blob \
                else git(root, "cat-file", "blob", blob)
            chains[rel] = [start]
        pre = chains[rel][-1]
        if (blob == "-") != (pre is None) or (pre is not None and blob_sha(pre) != blob):
            raise Refused("chain preimage of %s: rule pins %s, the previous image is %s" % (
                rel, blob, "absent" if pre is None else blob_sha(pre)))
        if kind == "EDIT":
            post, donors, names, match, hunks = edit_post(crate, rel, pre, payload, blobs, naming)
        elif kind == "FILE":
            post, donors, names, match, hunks = file_post(crate, rel, payload, blobs)
            matches.append((rel, crate, payload))
        else:
            post, donors, names, match, hunks = wrapper_post(crate, payload)
        chains[rel].append(post)
        rows.append([rel, kind, crate, blob, sha256(pre), sha256(post), donors, names, match, hunks, note])
    for rel, images in chains.items():  # every refusal before the first write
        on_disk = disk[rel]
        if on_disk is not None and on_disk not in images:
            raise Refused("drifted preimage %s: on disk %s is no image of its rule chain" % (rel, blob_sha(on_disk)))
        if on_disk is None and images[0] is not None:
            raise Refused("missing preimage %s (pinned blob %s)" % (rel, blob_sha(images[0])))
    changed = 0
    for rel, images in chains.items():
        on_disk = disk[rel]
        state = "UNCHANGED" if on_disk == images[-1] else "CHANGED"
        if state == "CHANGED" and not check:
            (root / rel).parent.mkdir(parents=True, exist_ok=True)
            (root / rel).write_bytes(images[-1])
        changed += state == "CHANGED"
        print("%s %s step=%d/%d" % (state, rel, images.index(on_disk) if on_disk in images else 0, len(images) - 1))
    matched = 0
    for rel, crate, types in matches:
        target, final = root / rel, chains[rel][-1]
        data = target.read_bytes() if target.exists() else final
        ok = body_match(final if check and data != final else data, crate, types, blobs)
        matched += ok
        for row in rows:
            if row[0] == rel:
                row[8] = "body_match=%s" % ("TRUE" if ok else "FALSE")
        if not ok:
            raise Refused("body_match=FALSE for %s" % rel)
    plan = "\n".join(["\t".join(PLAN_COLUMNS)] + ["\t".join(row) for row in rows]) + "\n"
    plan_path = RECIPE_DIR / PLAN_NAME
    plan_state = not plan_path.exists() or plan_path.read_bytes() != plan.encode("utf-8")
    if plan_state and not check:
        plan_path.write_bytes(plan.encode("utf-8"))
    kinds = [kind for kind, *_ in RULES]
    print("SYNTAX_INDEX_FACTS recipe=%s %s base=%s donor=%s crates=%d rules=%d edits=%d files=%d wrappers=%d changed=%d"
          " body_match=%d/%d plan=%s" % (RECIPE, VERSION, BASE_COMMIT[:12], DONOR_COMMIT[:12],
                                         len({crate for _k, crate, *_ in RULES}), len(RULES), kinds.count("EDIT"),
                                         kinds.count("FILE"), kinds.count("WRAPPER"), changed, matched, len(matches),
                                         "CHANGED" if plan_state else "UNCHANGED"))
    return 1 if check and (changed or plan_state) else 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--donor-git", required=True, help="a %s clone holding DONOR_COMMIT" % DONOR_REPO)
    parser.add_argument("--root", default=str(RECIPE_DIR.parents[3]), help="repository root (default: four up)")
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when a run would change a file")
    args = parser.parse_args(argv)
    try:
        return run(Path(args.root).resolve(), Path(args.donor_git).resolve(), args.check)
    except Refused as refusal:
        print("REFUSED " + str(refusal), file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
