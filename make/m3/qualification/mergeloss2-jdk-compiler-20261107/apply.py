#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe MERGELOSS2_RESTORE_3WAY v1: restore merge-loss audit phase-2 leaf 103 (src/jdk.compiler, M3AST scopes).

Origin 6f151d7ad124 ("Reuse unchanged binary dependencies in scoped original M3AST compilations") bound explicit
file/package/module/project source worksets to an unstarted javac task. The retain merge 4c9dc30c47e4 (Superset:
retain PR #1559) kept master's three owners and parked the PR's versions under
m3/qualification/master-landing-20261028/retained-variants/pr1559/.

Per file: ours = the pinned master blob, base = the origin commit's parent blob, theirs = the origin commit's blob
(= the retained copy; m3/tooling/jdk-naming rewrite_text is a no-op on all three). The recipe runs
`git merge-file -p --diff3` itself and resolves the conflict hunks by declared shape only. ADD means an empty base:
ours, then theirs. OPEN is the one M3ASTPC session-open line: master's `retainSyntax || relations != null` is kept,
inside the origin's `sourceScope == null ? open(..) : open(.., sourceScope(..))`. Any other hunk count or shape
refuses. Then the //FLLDCIM anchors are inserted (anchored sub edits, each exactly once). The final bytes must equal
the pinned postimage sha256, or the recipe refuses. A target must equal the pinned preimage or the pinned postimage;
anything else is a drifted preimage (exit 2, nothing written). Idempotent; EDITS.tsv is a fixed point; --check writes
nothing and exits 1 when a run would change a file.

usage: python -I apply.py [--root DIR] [--check]
"""
from __future__ import annotations

import argparse
import hashlib
import re
import subprocess
import sys
import tempfile
from pathlib import Path

RECIPE = "apply.py"
VERSION = "v1"
RECIPE_DIR = Path(__file__).resolve().parent
BASE_COMMIT = "a83ae5a44534df34e1e695fcfeed7158034ca759"
ORIGIN = "6f151d7ad124b78b4c6e946211c30ac2f81a091a"
DROPPING_MERGE = "4c9dc30c47e4fe3fc081d9e60d839d891c6c3cc2"
EDITS_NAME = "EDITS.tsv"
EDITS_COLUMNS = ("file", "before_sha256", "after_sha256", "leaf", "origin_commit", "dropping_merge", "threeway",
                 "rule")
JC = "src/jdk.compiler/share/classes/"
ANCHOR = ("//FLLDCIM: MRL - merge-loss leaf 103 (origin 6f151d7ad124, dropped by retain merge 4c9dc30c47e4): ")
OPEN_BASE = "        Session session = (Session) open(task, retainSyntax);\n"
OPEN_OURS = "        Session session = (Session) open(task, retainSyntax || relations != null);\n"
OPEN_THEIRS = ("        Session session = (Session) (sourceScope == null ? open(task, retainSyntax)\n"
               "                : open(task, retainSyntax, sourceScope(task, sourceScope)));\n")
OPEN_NEW = ("        Session session = (Session) (sourceScope == null ? open(task, retainSyntax || relations != null)\n"
            "                : open(task, retainSyntax || relations != null, sourceScope(task, sourceScope)));\n")
HUNK = re.compile(r"<<<<<<< ours\n(.*?)\|\|\|\|\|\|\| base\n(.*?)=======\n(.*?)>>>>>>> theirs\n", re.S)


class Refused(Exception):
    pass


# path, ours (pinned master) blob, base blob, theirs blob, expected hunk shapes, anchors [(old, new, count)],
# postimage sha256
RULES = (
    (JC + "com/sun/source/util/M3AST.java", "155c9405fa8f837d02d8ed9481c1f1af15a73807",
     "125c8953c760daf12b20fd2b046fa9275b4565dc", "a5df1500dd7c377d0414418fb9ae272354132cd0", (), [
         ("    /**\n     * Bind an explicit source workset to the original, unstarted compiler task.\n",
          "    " + ANCHOR + "explicit source worksets (Scope, ScopeKind, open with a scope)\n"
          "    /**\n     * Bind an explicit source workset to the original, unstarted compiler task.\n", 1),
     ], "7b759f90799af15baa14dd19ade1855037962530fb2304f71a23caa52340aa91"),
    (JC + "com/sun/tools/javac/m3/M3ASTPC.java", "b055922ef40744c67364e53cbed683f559a52ace",
     "e4f71ae04bdcaedc672b320f949552bfdafb3a07", "93d76ff4949809e2351da525eb203db0305a67e4",
     ("ADD", "OPEN", "ADD", "ADD", "ADD"), [
         ("    /** Build tools keep their original source selection, task and compiler arguments. */\n",
          "    " + ANCHOR + "scope= plugin option, scoped open, source/declaration admission\n"
          "    /** Build tools keep their original source selection, task and compiler arguments. */\n", 1),
     ], "38e94affc346a06fd8c145930895de230585424950361c2edc5055ed2a842c6d"),
    (JC + "com/sun/tools/javac/main/JavaCompiler.java", "090aae6cdc4fb8c7a15c90e6cabe6825a6b92917",
     "cac77db60d93c1692bf4b9cfc18428ad3c36910f", "282aa1e0596c15c18443ea1d6d7bc44923daa84e", (), [
         ("    /** Original-owner admission state; includes reads without PARSE events. */\n",
          "    " + ANCHOR + "source reads are admitted before javac opens them\n"
          "    /** Original-owner admission state; includes reads without PARSE events. */\n", 1),
     ], "4424b84868b76ed61ee41749afe28cd0dbf7680b926e1f2e475bc93096b4ad2f"),
)


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def blob_sha(data: bytes) -> str:
    return hashlib.sha1(b"blob %d\0" % len(data) + data).hexdigest()


def git_blob(root: Path, blob: str) -> bytes:
    try:
        return subprocess.run(["git", "-C", str(root), "cat-file", "blob", blob], check=True,
                              capture_output=True).stdout
    except (subprocess.CalledProcessError, OSError) as failure:
        raise Refused("blob %s is not in this repository (%s)" % (blob, failure)) from failure


def merge(ours: bytes, base: bytes, theirs: bytes) -> str:
    with tempfile.TemporaryDirectory() as scratch:
        names = []
        for name, data in (("ours", ours), ("base", base), ("theirs", theirs)):
            path = Path(scratch) / name
            path.write_bytes(data)
            names.append(str(path))
        result = subprocess.run(["git", "merge-file", "-p", "--diff3", "-L", "ours", "-L", "base", "-L", "theirs",
                                 *names], capture_output=True)
    if result.returncode < 0 or result.returncode > 127:
        raise Refused("git merge-file failed: %s" % result.stderr.decode("utf-8", "replace"))
    return result.stdout.decode("utf-8")


def resolve(text: str, shapes, path: str) -> str:
    seen = []

    def fix(match):
        ours, base, theirs = match.group(1), match.group(2), match.group(3)
        if base == "":
            seen.append("ADD")
            return ours + theirs
        if base == OPEN_BASE and ours.endswith(OPEN_OURS) and theirs == OPEN_THEIRS:
            seen.append("OPEN")
            return ours[: -len(OPEN_OURS)] + OPEN_NEW
        raise Refused("unexpected conflict hunk shape in %s" % path)

    out = HUNK.sub(fix, text)
    if tuple(seen) != tuple(shapes) or re.search(r"^(<<<<<<<|>>>>>>>|\|\|\|\|\|\|\|) ", out, re.M):
        raise Refused("conflict hunks %s in %s, declared %s" % (seen, path, list(shapes)))
    return out


def postimage(root: Path, rule) -> bytes:
    rel, ours, base, theirs, shapes, anchors, _ = rule
    text = resolve(merge(git_blob(root, ours), git_blob(root, base), git_blob(root, theirs)), shapes, rel)
    for old, new, expected in anchors:
        count = text.count(old)
        if count != expected:
            raise Refused("anchor occurs %d times (need %d) in %s: %r" % (count, expected, rel, old[:60]))
        text = text.replace(old, new)
    return text.encode("utf-8")


def run(root: Path, check: bool) -> int:
    rows, changed, writes = [], 0, []
    for rule in RULES:
        rel, ours, base, theirs, shapes, _, post_sha = rule
        target = root / rel
        if not target.exists():
            raise Refused("missing preimage %s (pinned blob %s)" % (rel, ours))
        on_disk = target.read_bytes()
        if sha256(on_disk) == post_sha:
            state = "UNCHANGED"
            pre = git_blob(root, ours)
        elif blob_sha(on_disk) == ours:
            post = postimage(root, rule)
            if sha256(post) != post_sha:
                raise Refused("postimage of %s is %s, pinned %s" % (rel, sha256(post), post_sha))
            state, pre = "CHANGED", on_disk
            changed += 1
            writes.append((target, post))
        else:
            raise Refused("drifted preimage %s: on disk %s, pinned blob %s" % (rel, blob_sha(on_disk), ours))
        threeway = "base=%s theirs=%s ours=%s git-merge-file=%s" % (
            base[:12], theirs[:12], ours[:12], "CONFLICT:%d %s" % (len(shapes), "+".join(shapes)) if shapes
            else "CLEAN")
        rows.append((rel, sha256(pre), post_sha, "103", ORIGIN, DROPPING_MERGE, threeway,
                     "RESTORE: scoped M3AST source worksets (origin commit message, canonical recipe"
                     " com.synexia.m3.AstScope20261020); theirs = retained-variants/pr1559 copy"))
        print("EDIT 103 %s %s" % (state, rel))
    if not check:  # every preimage and postimage verified before the first write
        for target, post in writes:
            target.write_bytes(post)
    table = "\n".join(["\t".join(EDITS_COLUMNS)] + ["\t".join(row) for row in rows]) + "\n"
    edits_path = RECIPE_DIR / EDITS_NAME
    table_state = not edits_path.exists() or edits_path.read_bytes() != table.encode("utf-8")
    if table_state and not check:
        edits_path.write_bytes(table.encode("utf-8"))
    print("MERGELOSS2_RESTORE_3WAY recipe=%s %s base=%s rules=%d changed=%d edits=%s" % (
        RECIPE, VERSION, BASE_COMMIT[:12], len(rows), changed, "CHANGED" if table_state else "UNCHANGED"))
    return 1 if check and (changed or table_state) else 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", default=str(RECIPE_DIR.parents[2]), help="repository root (default: three up)")
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when a run would change a file")
    args = parser.parse_args(argv)
    try:
        return run(Path(args.root).resolve(), args.check)
    except Refused as refusal:
        print("REFUSED " + str(refusal), file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
