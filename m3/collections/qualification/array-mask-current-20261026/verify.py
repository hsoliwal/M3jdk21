#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed read-only verification of current integrated-JDK array-mask postimages."""
from __future__ import annotations

import csv
import hashlib
import io
import json
from pathlib import Path, PurePosixPath
import sys

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[3]
RECEIPT = HERE / "RECEIVER_RECEIPT.json"
SCHEMA = "M3JDK21_SOURCE_PINNED_ARRAY_MASK_RECEIPT_V1"
EXPECTED = {
    "m3/collections/src/main/java/com/m3/collections/M3ArrayList.java",
    "m3/collections/src/test/java/com/m3/collections/M3ArrayListShortWordMaskContract.java",
    "m3/arrays/pom.xml",
}
REQUIRED_OPEN_GATES = (
    "source_built_M3JDK21_jtreg", "ABI_serialization_reflection",
    "GC_JMM", "CPU_heap_native_benchmarks",
)


def fail(message: str) -> None:
    raise ValueError(message)


def source_path(root: Path, name: str) -> Path:
    """Never dereference an alias, traversal, or symlinked source coordinate."""
    if (not isinstance(name, str) or not name or "\\" in name or "\0" in name
            or name.startswith("/") or ":" in name):
        fail("invalid source coordinate: " + str(name))
    pure = PurePosixPath(name)
    if any(part in ("", ".", "..") for part in name.split("/")):
        fail("noncanonical path: " + name)
    candidate = root.resolve()
    for component in pure.parts:
        candidate = candidate / component
        if candidate.is_symlink():
            fail("symlinked source is not a sealed file: " + name)
    if not candidate.resolve().is_relative_to(root.resolve()):
        fail("source escapes JDK checkout")
    if not candidate.is_file():
        fail("required source missing: " + name)
    return candidate


def git_blob(data: bytes) -> str:
    return hashlib.sha1(b"blob " + str(len(data)).encode("ascii") + b"\0" + data).hexdigest()


def verify_receipt(root: Path, receipt: dict) -> dict:
    if receipt.get("schema") != SCHEMA:
        fail("unknown receiver schema")
    product = receipt.get("product", {})
    if (product.get("repository") != "hsoliwal/M3jdk21"
            or product.get("java_se_class_or_module_changed") is not False
            or product.get("legacy_public_m3_sdk_identity_preserved") is not True
            or product.get("serialized_layout_changed") is not False):
        fail("canonical JDK or SDK contract assertions missing")
    if receipt.get("admission", {}).get("state") != "DRAFT_CANDIDATE_ONLY":
        fail("unverified JDK runtime promoted without qualification")
    if receipt.get("convergence", {}).get("public_product_contains_recipe_source") is not False:
        fail("recipe source illegally shipped into public JDK receiver")
    files = receipt.get("files")
    if not isinstance(files, list) or len(files) != len(EXPECTED):
        fail("unexpected number of source-sealed changes")
    seen = set()
    for file in files:
        path = file.get("path")
        if path not in EXPECTED or path in seen:
            fail("unapproved or duplicate target file")
        seen.add(path)
        content = source_path(root, path).read_bytes()
        sha = hashlib.sha256(content).hexdigest()
        if git_blob(content) != file.get("postimage_blob") or sha != file.get("postimage_sha256"):
            fail("source Git/SHA256 drift: " + path)
        if file.get("preimage_blob") == file.get("postimage_blob"):
            fail("no-op masquerading as a tested migration")
        if file.get("role", "").startswith("single owner") or path.endswith("ShortWordMaskContract.java"):
            if b"SPDX-License-Identifier: Apache-2.0 OR EPL-2.0" not in content:
                fail("current dual-license header lost: " + path)
    if seen != EXPECTED:
        fail("incomplete source migration scope")
    current = source_path(root, "m3/collections/src/main/java/com/m3/collections/M3ArrayList.java").read_text()
    if (current.count("final boolean inlineMask = oldSize <= 2 * Long.SIZE;") != 1
            or "long removalLow = 0L;" not in current or "long removalHigh = 0L;" not in current
            or "BitSet removed = inlineMask ? null : new BitSet();" not in current):
        fail("approved two-word selection atom absent")
    oracle = source_path(root, "m3/collections/src/test/java/com/m3/collections/M3ArrayListShortWordMaskContract.java").read_text()
    if "for (int n = 0; n <= 129; n++)" not in oracle or "seeded.nextInt(1025)" not in oracle:
        fail("expanded boundary oracle missing")
    java_modules = source_path(root, "m3/arrays/pom.xml").read_text()
    if ("<version>5.10.2-m3-source-1</version>" in java_modules
            or java_modules.count("<version>5.10.2</version>") != 1):
        fail("test-only Maven JUnit dependency not reconciled")
    native = receipt.get("unchanged_native")
    if not isinstance(native, list) or len(native) != 2:
        fail("JNI provenance incomplete")
    for row in native:
        native_file = source_path(root, row["path"]).read_bytes()
        if git_blob(native_file) != row["git_blob"]:
            fail("JNI source mutated without a qualified source recipe")
    catalogue = receipt.get("catalogue", {})
    data = source_path(root, catalogue.get("path", "")).read_bytes()
    if git_blob(data) != catalogue.get("git_blob"):
        fail("algorithm catalogue is not byte-pinned")
    table = list(csv.DictReader(io.StringIO(data.decode("utf-8")),
                                delimiter="\t"))
    if len(table) != catalogue.get("categories"):
        fail("catalogue category count drift")
    names = [row["problem_category"] for row in table]
    if len(names) != len(set(names)) or "STABLE_BULK_REMOVE_MASK" not in names:
        fail("missing or duplicate searchable problem categories")
    for name in REQUIRED_OPEN_GATES:
        value = receipt.get("gates", {}).get(name)
        if not isinstance(value, str) or value.startswith("PASS"):
            fail("unqualified release gate has been asserted passed: " + name)
    return {"schema": SCHEMA, "sealed_java_and_pom": len(seen),
            "unchanged_native_sources": len(native), "catalogue_categories": len(table),
            "public_api_unchanged": True, "runtime_promotion": "BLOCKED"}


def main() -> int:
    try:
        receipt = json.loads(RECEIPT.read_text(encoding="utf-8"))
        result = verify_receipt(ROOT, receipt)
        print(json.dumps(result, sort_keys=True))
        return 0
    except (OSError, ValueError, UnicodeError, KeyError, TypeError) as error:
        print("M3JDK21_ARRAYMASK_RECEIVER_REFUSED: " + str(error), file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
