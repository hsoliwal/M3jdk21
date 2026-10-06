#!/usr/bin/env python3
from __future__ import annotations

import csv
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CATALOG = ROOT / "m3" / "docs" / "synexia-live-precompute-catalog.tsv"

ALLOWED = {
    "ABSORBED_INTERNAL",
    "PARTIAL_INTERNAL",
    "SEPARATE_INTERNAL_REQUIRED",
    "OPTIONAL_ACCELERATOR",
    "DONOR_FRAMEWORK_REFERENCE",
    "EXCLUDED_FROM_JAVA_LANG_STRING",
}

STRING_FORBIDDEN_TARGET_STATUS = {
    "EXCLUDED_FROM_JAVA_LANG_STRING",
    "SEPARATE_INTERNAL_REQUIRED",
    "OPTIONAL_ACCELERATOR",
    "DONOR_FRAMEWORK_REFERENCE",
}

def fail(message: str) -> None:
    raise SystemExit(f"synexia precompute catalogue invariant failed: {message}")

text = CATALOG.read_text(encoding="utf-8")
if "# donorCommit\tc24a83c2da24c0fe8d480023406986e926af0549" not in text:
    fail("catalogue is not pinned to the reviewed donor commit")

rows = []
with CATALOG.open(encoding="utf-8", newline="") as handle:
    filtered = (line for line in handle if not line.startswith("#"))
    reader = csv.DictReader(filtered, delimiter="\t")
    for row in reader:
        rows.append(row)

if len(rows) != 340:
    fail(f"expected 340 pinned live donor owners, found {len(rows)}")

seen = set()
for row in rows:
    source = row["sourcePath"]
    if source in seen:
        fail(f"duplicate source owner: {source}")
    seen.add(source)

    status = row["status"]
    if status not in ALLOWED:
        fail(f"unclassified/unknown status {status!r} for {source}")

    if status in STRING_FORBIDDEN_TARGET_STATUS:
        target = row["targetOwnerOrDisposition"]
        if target == "java.lang.String" or target.startswith("java.lang.String "):
            fail(f"{status} row mapped directly into java.lang.String: {source}")

    if not row["sourceBlobSha"] or len(row["sourceBlobSha"]) != 40:
        fail(f"missing/invalid donor blob pin: {source}")

    if not row["targetOwnerOrDisposition"].strip():
        fail(f"missing target/disposition: {source}")
    if not row["invariant"].strip():
        fail(f"missing invariant: {source}")

if any("REVIEW_REQUIRED" in row.values() for row in rows):
    fail("REVIEW_REQUIRED rows remain")

# Core String-semantic donor owners must never disappear from the catalogue.
required_fragments = {
    "MIndexStringCanonicalFacts.java",
    "MIndexUtf16RangeFacts.java",
    "MIndexStringSearchPlan.java",
    "MatIndexStringPositionIndex.java",
    "MIndexPreparedTrigramQuery.java",
    "MIndexStringPrecomputation.java",
}
names = {Path(row["sourcePath"]).name for row in rows}
missing = sorted(required_fragments - names)
if missing:
    fail("missing required String-semantic donor owners: " + ", ".join(missing))

print(
    "SYNEXIA_PRECOMPUTE_CATALOG_PASS "
    f"rows={len(rows)} "
    + " ".join(
        f"{status}={sum(1 for row in rows if row['status'] == status)}"
        for status in sorted(ALLOWED)
    )
)
