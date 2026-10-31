#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Verify the exact SWT-recipe merger and source-owned M3AST forward recipes.

This is a read-only extension receipt. It never modifies source. The earlier
five-donor M3JDK21 superset verifier is independently re-executed at its own
source commit rather than weakened to accept unknown future file mutations.
"""
from __future__ import annotations

import hashlib
from pathlib import Path
import re
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[3]
BEFORE = "1559f3525dd32937d4851576f231b13f92e4cf55"
AST_SOURCE_PARENT = "d7b9e65241b717a387b25234df4cf21531f07d9f"
ECLIPSE_PARENT = "285fdace40b124357d603214df4f1398e6c2b0de"
MASTER = "2779aba3f98f31c324be43f731be7914e1a05935"
DATASET = "m3-ast-select-20261021"
CRATE = "m3-ast-build-reverse-20261017-" + DATASET
PACK = "m3/tooling/module-packs/parser"
DATA = "m3/tooling/shared-frontend/recipe-custody/datasets/" + DATASET
EVIDENCE = "m3/qualification/eclipse-ast-extension-20261021"
WORKFLOW = ".github/workflows/m3-eclipse-ast-extension-20261021.yml"

SOURCE = {
    PACK + "/src/main/java/jdk/m3/parser/M3ASTIndex.java":
        ("6d366992fdb7d4c23a63e145a939e7dc4e6a2616",
         "9424688a4f4253e8b7cdb71470bb084cc378aa36",
         "0001-M3ASTIndex.java.txt", "java", DATASET + "-p1"),
    PACK + "/src/test/java/jdk/m3/parser/test/M3ASTSelectionContract.java":
        ("ABSENT", "422a1a537fdee5799baa9a54f88bc27601683a95",
         "0002-M3ASTSelectionContract.java.txt", "java", DATASET + "-p2"),
    PACK + "/build.py":
        ("57881e7ac7bb7f2ce8f18df2f3d0b252a4ffe5cd",
         "dc57f06553988b6a200a6ac2e30e8f2f1070251b",
         "0003-build.py.txt", "text", DATASET + "-p3"),
}

def git(*arguments: str) -> bytes:
    result = subprocess.run(["git", *arguments], cwd=ROOT, check=False,
                            capture_output=True)
    if result.returncode:
        raise ValueError("Git source-custody failure "
                         + " ".join(arguments) + ": "
                         + result.stderr.decode("utf-8", "replace")[:400])
    return result.stdout

def blob(commit: str, path: str) -> tuple[str, str]:
    rows = git("ls-tree", "-z", commit, "--", path).split(b"\0")
    if len(rows) != 2 or not rows[0]:
        raise ValueError("missing or ambiguous original file: " + path)
    mode, kind, sha = rows[0].decode("utf-8").split("\t")[0].split(" ")
    original = rows[0].decode("utf-8").split("\t", 1)[1]
    if original != path or kind != "blob" or not re.fullmatch("[0-9a-f]{40}", sha):
        raise ValueError("source identity mismatch: " + path)
    return mode, sha

def file_bytes(commit: str, path: str) -> bytes:
    mode, sha = blob(commit, path)
    if mode not in ("100644", "100755"):
        raise ValueError("unsupported source mode: " + path)
    return git("cat-file", "blob", sha)

def manifest(path: Path) -> list[list[str]]:
    return [row.split("\t") for row in path.read_text(encoding="utf-8").splitlines()
            if row and not row.startswith("#")]

def main() -> None:
    head = git("rev-parse", "HEAD").decode().strip()
    for parent in (BEFORE, AST_SOURCE_PARENT, ECLIPSE_PARENT, MASTER):
        git("merge-base", "--is-ancestor", parent, head)
    eclipse_paths = git("diff", "--name-only", MASTER, ECLIPSE_PARENT).decode().splitlines()
    if len(eclipse_paths) != 12 or len(set(eclipse_paths)) != 12:
        raise ValueError("original Eclipse recipe merge has incomplete path closure")
    for path in eclipse_paths:
        original = blob(ECLIPSE_PARENT, path)
        received = blob(head, path)
        if original != received:
            raise ValueError("original Eclipse source recipe was overwritten: " + path)

    recipe = ROOT / DATA
    aggregate_rows = []
    for kind in ("java", "text"):
        group = recipe / "resources/com/synexia/rewrite" / ("hash-pinned-" + kind)
        aggregate_rows += manifest(group / CRATE / "manifest.tsv")
    if len(aggregate_rows) != len(SOURCE) or any(len(r) != 4 for r in aggregate_rows):
        raise ValueError("exact forward-recipe aggregate manifest mismatch")
    actual_by_path = {r[0]: r for r in aggregate_rows}
    if set(actual_by_path) != set(SOURCE):
        raise ValueError("unexpected AST source-owner target")

    for path, (old, new, resource, kind, stage) in SOURCE.items():
        if old != "ABSENT" and blob(BEFORE, path) != ("100644", old):
            raise ValueError("wrong original compiler source generation: " + path)
        if blob(head, path) != ("100644", new):
            raise ValueError("unapproved AST source replacement: " + path)
        lane = recipe / "resources/com/synexia/rewrite" / ("hash-pinned-" + kind)
        aggregate = lane / CRATE
        stage_path = lane / stage
        row = actual_by_path[path]
        if (row[0] != path or row[3] != resource
                or manifest(stage_path / "manifest.tsv") != [row]):
            raise ValueError("non-serial or duplicated FILE-local AST pass: " + path)
        if old == "ABSENT":
            if row[1] != "ABSENT":
                raise ValueError("new test must have no original preimage")
            if git("ls-tree", BEFORE, "--", path).strip():
                raise ValueError("new source was present in original compiler generation")
        else:
            old_bytes = git("cat-file", "blob", old)
            if (row[1] != hashlib.sha256(old_bytes).hexdigest()
                    or (aggregate / "pre" / resource).read_bytes() != old_bytes
                    or (stage_path / "pre" / resource).read_bytes() != old_bytes):
                raise ValueError("lost original source preimage: " + path)
        new_bytes = file_bytes(head, path)
        if (row[2] != hashlib.sha256(new_bytes).hexdigest()
                or (aggregate / resource).read_bytes() != new_bytes
                or (stage_path / resource).read_bytes() != new_bytes):
            raise ValueError("generated AST product no longer matches recipe: " + path)

    yaml = (recipe / "resources/META-INF/rewrite" / (CRATE + ".yml")).read_text()
    expected = [DATASET + "-p" + str(i) for i in range(1, 4)]
    if re.findall(r"^\s+crateName:\s*(\S+)\s*$", yaml, re.M) != expected:
        raise ValueError("canonical file-local recipe order drift")
    plan = manifest(recipe / "PASSES.tsv")
    if len(plan) != 3 or [x[1] for x in plan] != expected:
        raise ValueError("source/algorithm category ledger drift")

    diff = set(git("diff", "--name-only", BEFORE, head).decode().splitlines())
    allowed = set(SOURCE) | set(eclipse_paths)
    extra_roots = (DATA + "/", EVIDENCE + "/", PACK + "/qualification/ast-select-20261021/")
    for path in diff:
        if path in allowed or path == WORKFLOW or any(path.startswith(root) for root in extra_roots):
            continue
        raise ValueError("unqualified new product/source path: " + path)
    if not set(SOURCE).issubset(diff):
        raise ValueError("missing new original-parser source or fixture")
    original_gate = file_bytes(head, PACK + "/build.py").decode("utf-8")
    if ('("jdk.m3.parser.test.M3ASTSelectionContract", "M3_AST_SELECTION_PASS")'
            not in original_gate):
        raise ValueError("M3AST contract omitted from canonical parser build")
    index = file_bytes(head, PACK + "/src/main/java/jdk/m3/parser/M3ASTIndex.java").decode("utf-8")
    for required in ("selectedNodes(", "selectedSubtree(", "forEachSelectedNode(",
                     "first[current]", "parents[current]", "checkMasks("):
        if required not in index:
            raise ValueError("missing original primitive indexed AST atom: " + required)
    print(f"M3_ECLIPSE_AST_EXTENSION_GIT_PASS exact_eclipse={len(eclipse_paths)} "
          f"serial_ast={len(SOURCE)} delta={len(diff)} commit={head}")


if __name__ == "__main__":
    try:
        main()
    except (OSError, ValueError) as problem:
        print("M3_ECLIPSE_AST_EXTENSION_REFUSED:", problem, file=sys.stderr)
        sys.exit(1)
