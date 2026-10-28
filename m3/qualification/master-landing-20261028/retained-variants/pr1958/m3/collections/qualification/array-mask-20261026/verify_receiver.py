#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Read-only target-side source/JNI receipt validation. NOT a rewrite recipe."""
from __future__ import annotations

import hashlib
import json
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[4]
RECEIPT = Path(__file__).with_name("RECEIVER_RECEIPT.json")
SCHEMA = "M3JDK21_ARRAY_MASK_RECEIVER_20261026_V1"


def blob(raw: bytes) -> str:
    return hashlib.sha1(b"blob " + str(len(raw)).encode("ascii") + b"\0" + raw).hexdigest()


def verify_blob(raw: bytes, expected_blob: str,
                expected_sha256: str | None = None) -> None:
    if blob(raw) != expected_blob:
        raise ValueError("exact Git source blob drift")
    if expected_sha256 is not None and hashlib.sha256(raw).hexdigest() != expected_sha256:
        raise ValueError("exact SHA-256 source drift")


def safe_file(path: str) -> Path:
    if (not isinstance(path, str) or not path
            or "\\" in path or "\x00" in path
            or path.startswith("/") or any(p in ("", ".", "..") for p in path.split("/"))):
        raise ValueError("noncanonical product source path")
    candidate = ROOT
    for part in path.split("/"):
        candidate = candidate / part
        if candidate.is_symlink():
            raise ValueError("symlink source path refused")
    if not candidate.resolve().is_relative_to(ROOT.resolve()):
        raise ValueError("source path escapes product checkout")
    if not candidate.is_file():
        raise ValueError("product receiving source missing: " + path)
    return candidate


def verify() -> dict:
    data = json.loads(RECEIPT.read_text(encoding="utf-8"))
    if data.get("schema") != SCHEMA:
        raise ValueError("unexpected receiver receipt schema")
    product = data["product"]
    if (product.get("public_java_se_api_changed") is not False
            or product.get("legacy_public_m3_api_removed") is not False
            or product.get("integrated_jdk_not_overlay") is not True):
        raise ValueError("target incorrectly claims public JDK API promotion")
    if data.get("admission") != "DRAFT_REVIEW_ONLY_UNTIL_ALL_RECIPE_AND_TARGET_GATES_PASS":
        raise ValueError("premature M3JDK runtime promotion")
    source = data["files"]
    if len(source) != 3 or len({row["path"] for row in source}) != 3:
        raise ValueError("receiving scope is not exactly three existing files")
    expected_paths = {
        "m3/collections/src/main/java/com/m3/collections/M3ArrayList.java",
        "m3/collections/src/test/java/com/m3/collections/M3ArrayListShortWordMaskContract.java",
        "m3/arrays/pom.xml",
    }
    if {row["path"] for row in source} != expected_paths:
        raise ValueError("unreviewed target source in receiving scope")
    for row in source:
        verify_blob(safe_file(row["path"]).read_bytes(), row["after_blob"],
                    row["after_sha256"])
        if not row.get("before_blob") or not row.get("before_sha256"):
            raise ValueError("missing immutable source baseline seal")
    list_source = safe_file("m3/collections/src/main/java/com/m3/collections/M3ArrayList.java").read_text(encoding="utf-8")
    for required in ("public final class M3ArrayList<E>",
                     "final boolean inlineMask = oldSize <= 2 * Long.SIZE;",
                     "long removalLow = 0L;", "long removalHigh = 0L;",
                     "BitSet removed = inlineMask ? null : new BitSet();",
                     "Arrays.fill(elements, write, oldSize, null);"):
        if required not in list_source:
            raise ValueError("array owner contract atom missing: " + required)
    oracle = safe_file("m3/collections/src/test/java/com/m3/collections/M3ArrayListShortWordMaskContract.java").read_text(encoding="utf-8")
    for token in ("SPDX-License-Identifier: Apache-2.0 OR EPL-2.0",
                  "for (int n = 0; n <= 129; n++)", "seeded.nextInt(1025)"):
        if token not in oracle:
            raise ValueError("current license/expanded oracle missing: " + token)
    pom = safe_file("m3/arrays/pom.xml").read_text(encoding="utf-8")
    if ("<version>5.10.2-m3-source-1</version>" in pom
            or "<version>5.10.2</version>" not in pom):
        raise ValueError("unresolved Maven test dependency reappeared")
    native = data["native_sources_unchanged"]
    if len(native) != 2:
        raise ValueError("wrong unchanged JNI source family")
    for item in native:
        verify_blob(safe_file(item["path"]).read_bytes(), item["git_blob"])
    return {"schema": SCHEMA, "exact_postimages": len(source),
            "unchanged_native_sources": len(native), "result": "PASS",
            "not_qualified": ["source-built JDK/jtreg", "JNI/GC/JMM stress",
                              "CPU/heap/native performance"]}


def main() -> int:
    try:
        print(json.dumps(verify(), sort_keys=True))
        return 0
    except (ValueError, OSError, KeyError, TypeError, json.JSONDecodeError) as error:
        print("M3JDK21_ARRAY_MASK_RECEIVER_REFUSED: " + str(error), file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
