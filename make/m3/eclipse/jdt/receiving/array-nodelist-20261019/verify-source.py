#!/usr/bin/env python3
"""Verify a sealed upstream source checkout and literal generated owner receipts.

Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
This command reads source only. It performs no source transformation.
"""
from __future__ import annotations

import csv
import hashlib
import json
from pathlib import Path
import sys


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main() -> None:
    receipt = Path(__file__).resolve().parent
    if len(sys.argv) != 2:
        raise SystemExit("usage: verify-source.py <exact eclipse.jdt.core checkout>")
    donor = Path(sys.argv[1]).resolve()
    source = json.loads((receipt / "SOURCE.json").read_text(encoding="utf-8"))
    lock = receipt / source["sourceLock"]
    if digest(lock) != source["sourceLockSha256"]:
        raise SystemExit("upstream source lock checksum mismatch")
    expected = dict(line.split("  ", 1)[::-1] for line in lock.read_text(encoding="utf-8").splitlines())
    with (receipt / source["ownerTransforms"]).open(encoding="utf-8", newline="") as stream:
        transforms = {row["target_path"]: row for row in csv.DictReader(stream, delimiter="\t")}
    roots = ["org.eclipse.jdt.core.compiler.batch/src"] + [
        "org.eclipse.jdt.core/" + name
        for name in ("codeassist", "eval", "formatter", "dom", "search", "model")
    ]
    actual_paths = {
        path.relative_to(donor).as_posix()
        for root in roots
        for path in (donor / root).rglob("*")
        if path.is_file()
    }
    expected_paths = {path for path in expected if any(path.startswith(root + "/") for root in roots)}
    failures = ["unexpected source input: " + path for path in sorted(actual_paths - expected_paths)]
    failures += ["missing source input: " + path for path in sorted(expected_paths - actual_paths)]
    already_generated = []
    for name, before in expected.items():
        path = donor / name
        if not path.is_file():
            failures.append("missing locked input: " + name)
            continue
        actual = digest(path)
        after = transforms.get(name, {}).get("after_sha256")
        if actual == before:
            continue
        if after and actual == after:
            already_generated.append(name)
            continue
        failures.append("source checksum mismatch: " + name)
    generated = {
        "org.eclipse.jdt.core/dom/org/eclipse/jdt/core/dom/ASTNode.java": "src/main/java/org/eclipse/jdt/core/dom/ASTNode.java",
        "org.eclipse.jdt.core/model/org/eclipse/jdt/internal/core/builder/JavaBuilder.java": "src/main/java/org/eclipse/jdt/internal/core/builder/JavaBuilder.java",
        "org.eclipse.jdt.core.tests.model/src/org/eclipse/jdt/core/tests/dom/ASTNodeListArrayTest.java": "src/test/java/org/eclipse/jdt/core/tests/dom/ASTNodeListArrayTest.java",
        "org.eclipse.jdt.core.tests.model/src/org/eclipse/jdt/core/tests/dom/ASTNodeListStorageProbe.java": "src/test/java/org/eclipse/jdt/core/tests/dom/ASTNodeListStorageProbe.java",
    }
    for target, local in generated.items():
        path = receipt / local
        if not path.is_file() or digest(path) != transforms[target]["after_sha256"]:
            failures.append("generated owner/test checksum mismatch: " + local)
    if failures:
        raise SystemExit("\n".join(failures))
    print(json.dumps({"verifiedUpstreamCommit": source["commit"], "lockedFiles": len(expected),
                      "javaSources": sum(path.endswith(".java") for path in expected_paths),
                      "alreadyGeneratedDonorFiles": already_generated,
                      "generatedReceipts": len(generated)}, sort_keys=True))


if __name__ == "__main__":
    main()
