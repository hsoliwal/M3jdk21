#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors.
"""Fail-closed current-tree JDK21 M3 String concat size-limit and jtreg handoff proof.

This is a read-only target receiver proof, not a new canonical recipe engine.
Each complete source target is pinned by SHA-256, not filename or merge ancestry.
"""
from __future__ import annotations

import argparse
import hashlib
import re
from pathlib import Path

POOL = "src/java.base/share/classes/java/lang/M3StringPool.java"
JTREG = "test/jdk/java/lang/String/M3StringConcatLengthOverflowTest.java"
WORKFLOW = ".github/workflows/mindex-string-backing.yml"
DIGESTS = {'src/java.base/share/classes/java/lang/M3StringPool.java': '2c5a1c67416bff7c17dacae68b3d9c6cd5fc0154ab016a71ad35357ebfcee9af', 'test/jdk/java/lang/String/M3StringConcatLengthOverflowTest.java': 'a07590946ce9330772bb30a21dc35f1f1da0b87b671fdcf30c8630db33a91ac3', '.github/workflows/mindex-string-backing.yml': '3015c720ae6bb76d88ea88ec181f9bb87c534fdf567faea7da16be4d64ebc5c0'}

GUARD = (
    "if ((long) left.length() + right.length() > Integer.MAX_VALUE) {\n"
    '            throw new OutOfMemoryError("Required length exceeds implementation limit");\n'
    "        }"
)


class SourceRefusal(ValueError):
    """A receiver source or proof contract is not an admitted canonical postimage."""


def read_exact(root: Path, relative: str) -> str:
    target = root
    for part in relative.split("/"):
        if not part or part in (".", ".."):
            raise SourceRefusal("M3_JDK21_REFUSE|unsafe_path:" + relative)
        target = target / part
        if target.is_symlink():
            raise SourceRefusal("M3_JDK21_REFUSE|symlink:" + relative)
    if not target.is_file():
        raise SourceRefusal("M3_JDK21_REFUSE|missing_source:" + relative)
    raw = target.read_bytes()
    if len(raw) > 2 * 1024 * 1024:
        raise SourceRefusal("M3_JDK21_REFUSE|size_budget:" + relative)
    if hashlib.sha256(raw).hexdigest() != DIGESTS[relative]:
        raise SourceRefusal("M3_JDK21_REFUSE|sha256_drift:" + relative)
    return raw.decode("utf-8", errors="strict")


def verify(root: Path) -> dict[str, int]:
    root = root.resolve(strict=True)
    pool = read_exact(root, POOL)
    test = read_exact(root, JTREG)
    workflow = read_exact(root, WORKFLOW)

    method = pool.find("static M3String concat(M3String left, M3String right)")
    if method < 0 or pool.count(GUARD) != 1:
        raise SourceRefusal("M3_JDK21_REFUSE|concat_guard")
    left = pool.find("if (left.length() == 0) return right;", method)
    right = pool.find("if (right.length() == 0) return left;", method)
    guard = pool.find(GUARD, method)
    adjacent = pool.find("if (left.owner() == right.owner()", method)
    balanced = pool.find("return concatBalanced(left, right);", method)
    if not method < left < right < guard < adjacent < balanced:
        raise SourceRefusal("M3_JDK21_REFUSE|guard_order")
    for retained in ("MAX_RETAINED_TUPLES", "LOCAL_NATIVE_BYTES", "RETAINED_TUPLES"):
        if retained not in pool:
            raise SourceRefusal("M3_JDK21_REFUSE|pool_retention:" + retained)

    if test.count("@run main/othervm") != 2:
        raise SourceRefusal("M3_JDK21_REFUSE|both_jtreg_modes")
    for condition in (
        "-XX:+UseM3StringStorage",
        "-XX:-CompactStrings",
        "-Xmx512m",
        "expectOutOfMemory(() -> joiner.add(billion)",
        '"Requested array size exceeds VM limit"',
        'billion.concat(billion)',
        'String.join("", billion, billion)',
    ):
        if condition not in test:
            raise SourceRefusal("M3_JDK21_REFUSE|jtreg_contract:" + condition)
    if "joiner::toString" in test:
        raise SourceRefusal("M3_JDK21_REFUSE|late_joiner_assertion")

    matches = re.findall(r'^\s*run: make test TEST="([^"]+)"\s*$', workflow, re.M)
    if len(matches) != 1:
        raise SourceRefusal("M3_JDK21_REFUSE|jtreg_selector_command")
    selectors = matches[0].split()
    if len(selectors) != 49 or len(set(selectors)) != 49 or JTREG not in selectors:
        raise SourceRefusal("M3_JDK21_REFUSE|jtreg_selector_set")
    gate = workflow.find("check-m3string-concat-overflow-current.py")
    build = workflow.find("run: make java.base-java-only")
    image = workflow.find("run: make images")
    jtreg = workflow.find("run: make test TEST=")
    if not 0 <= gate < build < image < jtreg:
        raise SourceRefusal("M3_JDK21_REFUSE|build_gate_order")
    if "test_m3string_concat_overflow_current.py -v" not in workflow:
        raise SourceRefusal("M3_JDK21_REFUSE|refusal_tests_missing")
    return {"java_targets": 2, "workflow_selectors": len(selectors), "sha256": 3}


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[2])
    args = parser.parse_args()
    result = verify(args.root)
    print(
        "M3_JDK21_CONCAT_OVERFLOW_SOURCE_PASS|java_targets="
        + str(result["java_targets"])
        + "|jtreg_selectors=" + str(result["workflow_selectors"])
        + "|source_sha256=" + str(result["sha256"])
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
