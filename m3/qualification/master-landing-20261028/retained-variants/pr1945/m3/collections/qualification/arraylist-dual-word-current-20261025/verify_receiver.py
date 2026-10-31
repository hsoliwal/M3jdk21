#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Read-only M3JDK21 receiver: complete current postimage and native-source custody."""
from __future__ import annotations
import csv
import hashlib
import io
import json
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[4]
RECEIPT = Path(__file__).with_name("RECEIVER_RECEIPT.json")
FILES = {
    "m3/collections/src/main/java/com/m3/collections/M3ArrayList.java":
        "b1135dc23d04d4654cedab2e8b62684e1e7d4471",
    "m3/collections/src/test/java/com/m3/collections/M3ArrayListShortWordMaskContract.java":
        "e87326da9e8d59e36cd84fd1413a0f55087200aa",
    "m3/arrays/pom.xml": "668fab1d24bbebd407ed920664487db848db464e",
}
NATIVE = {
    "m3/collections/src/main/native/collections/m3_index_mask_compactor.c":
        "bedd442346de5b7dd9d5f1965c510950e0c45ea1",
    "m3/collections/src/main/native/collections/m3_index_mask_window.c":
        "bd300c90d54ede9b7f238efe8a18c918d2fcef9e",
}
CATALOGUE = "m3/collections/algorithm-catalog/arraylist-dual-word-current-20261025.tsv"
CATALOGUE_BLOB = "cb3c21e2da0508ed8af9ed3df6863b5a76d9ef59"
HEX40 = re.compile(r"[0-9a-f]{40}\Z")
HEX64 = re.compile(r"[0-9a-f]{64}\Z")


def refuse(reason: str) -> None:
    raise ValueError(reason)


def source(root: Path, relative: str) -> bytes:
    if (not isinstance(relative, str) or not relative
            or relative.startswith("/") or "\\" in relative or "\x00" in relative
            or re.match(r"^[a-zA-Z]:", relative)
            or any(p in ("", ".", "..") for p in relative.split("/"))):
        refuse("noncanonical M3JDK21 source path: " + str(relative))
    candidate = root.resolve()
    for part in relative.split("/"):
        candidate = candidate / part
        if candidate.is_symlink():
            refuse("source symlink refused: " + relative)
    if not candidate.is_file():
        refuse("required source not found: " + relative)
    return candidate.read_bytes()


def git_blob(data: bytes) -> str:
    return hashlib.sha1(
        b"blob " + str(len(data)).encode("ascii") + b"\0" + data).hexdigest()


def verify(root: Path, receipt: dict) -> dict:
    if receipt.get("schema") != "M3JDK21_REGISTER_MASK_CURRENT_RECEIPT_V1":
        refuse("receiver schema is not canonical")
    if receipt.get("state") != "DRAFT_NOT_JAVA_SE_PROMOTED":
        refuse("unreviewed runtime promotion")
    if receipt.get("source_master") != "882ebe18891c719c0e26b6ee42558e50e96fe33e":
        refuse("current source custody base drift")
    convergence = receipt.get("synexia", {})
    if convergence.get("recipe") != "com.synexia.rewrite.M3ArrayListDualWordCurrentRecipe":
        refuse("no canonical Synexia recipe linkage")
    rows = receipt.get("source_postimages")
    if not isinstance(rows, list) or len(rows) != len(FILES):
        refuse("expected exactly three reviewed source/POM postimages")
    seen = set()
    for row in rows:
        name = row.get("path")
        if name not in FILES or name in seen:
            refuse("unexpected or duplicate receiving file")
        seen.add(name)
        raw = source(root, name)
        if row.get("after_git_blob") != FILES[name] or git_blob(raw) != FILES[name]:
            refuse("receiving Git source hash drift: " + name)
        if (not HEX40.fullmatch(row.get("before_git_blob", ""))
                or not HEX64.fullmatch(row.get("before_sha256", ""))
                or row.get("after_sha256") != hashlib.sha256(raw).hexdigest()):
            refuse("receiving SHA-256/lineage drift: " + name)
    if seen != set(FILES):
        refuse("incomplete postimages")
    native = receipt.get("native_unmodified")
    if not isinstance(native, list) or len(native) != len(NATIVE):
        refuse("complete JNI source custody missing")
    native_seen = set()
    for row in native:
        name = row.get("path")
        if name not in NATIVE or name in native_seen:
            refuse("unexpected JNI file")
        native_seen.add(name)
        if row.get("git_blob") != NATIVE[name] or git_blob(source(root, name)) != NATIVE[name]:
            refuse("JNI bytecode/source drift: " + name)
    if native_seen != set(NATIVE):
        refuse("incomplete JNI source list")
    catalogue = receipt.get("catalogue")
    if (not isinstance(catalogue, dict) or catalogue.get("path") != CATALOGUE
            or catalogue.get("git_blob") != CATALOGUE_BLOB
            or catalogue.get("category_count") != 11):
        refuse("algorithm catalogue not pinned")
    raw = source(root, CATALOGUE)
    if git_blob(raw) != CATALOGUE_BLOB:
        refuse("algorithm category catalogue content drift")
    reader = csv.DictReader(io.StringIO(raw.decode("utf-8")), delimiter="\t")
    categories = set()
    for row in reader:
        category = row.get("problem_category")
        if (not category or category in categories or row.get("schema") != "M3_ALGORITHM_CATALOG_V1"
                or not row.get("custody_and_license") or not row.get("selection")):
            refuse("ambiguous or unattributed donor decision")
        categories.add(category)
    if len(categories) != 11 or not {"STABLE_BULK_REMOVE_MASK",
                                   "JNI_SPARSE_MASK_WINDOW"}.issubset(categories):
        refuse("missing algorithm comparison category")
    # The receipt must never silently upgrade historical/isolated checks to runtime acceptance.
    evidence = receipt.get("evidence", {})
    if evidence.get("source_built_jtreg_abi_gc_jmm") != "NOT_RUN":
        refuse("jtreg/ABI gate cannot be auto-promoted")
    if evidence.get("cpu_heap_native_performance") != "NOT_RUN":
        refuse("performance not qualified")
    return {"files": len(rows), "native": len(native), "categories": len(categories)}


def main() -> int:
    try:
        receipt = json.loads(RECEIPT.read_text(encoding="utf-8"))
        facts = verify(ROOT, receipt)
        print("M3JDK21_ARRAY_CURRENT_RECEIVED " + json.dumps(facts, sort_keys=True))
        return 0
    except (ValueError, OSError, UnicodeError, json.JSONDecodeError) as error:
        print("M3JDK21_ARRAY_CURRENT_REFUSED: " + str(error), file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
