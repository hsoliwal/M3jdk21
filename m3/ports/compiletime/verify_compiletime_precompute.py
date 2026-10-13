#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Custody check for the compile-time String precompute port (CPU only, no JVM).

Refuses on: crate/source drift, manifest SHA-256 drift, non-additive rows, non-canonical order,
missing FOSS reuse ledger columns or decisions, missing provenance tokens, missing evidence
receipts, or a differential receipt that reports any mismatch.
"""
from __future__ import annotations

import hashlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
PORT = ROOT / "m3/ports/compiletime"
CRATE = ROOT / ("m3/tooling/migration-recipes/src/main/resources/com/m3/rewrite/backport/"
                "jdk21-hash-pinned/m3-compiletime-regex-precompute")
LEDGER_COLUMNS = ["capability", "candidate_repo", "commit", "source_paths", "license",
                  "contract_fit", "decision", "rejected_reason", "adapted_hashes", "notice_path"]


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def tsv(path: Path) -> list[list[str]]:
    return [line.split("\t") for line in path.read_text(encoding="utf-8").splitlines() if line]


def check_crate() -> int:
    rows = [r for r in tsv(CRATE / "manifest.tsv") if not r[0].startswith("#")]
    sources = sorted(p.relative_to(ROOT).as_posix() for p in PORT.joinpath("src").rglob("*.java"))
    if [r[0] for r in rows] != sources:
        raise AssertionError("crate targets differ from port sources (re-run seal_crate.py)")
    previous = ""
    for path, before, after, resource in rows:
        if path <= previous or before != "ABSENT":
            raise AssertionError(f"non-canonical or non-additive row: {path}")
        previous = path
        source = (ROOT / path).read_bytes()
        if (CRATE / resource).read_bytes() != source:
            raise AssertionError(f"source/resource drift: {path}")
        if sha256(source) != after:
            raise AssertionError(f"manifest SHA-256 drift: {path}")
    return len(rows)


def check_ledger() -> None:
    rows = tsv(PORT / "FOSS_REUSE_DECISION.tsv")
    if rows[0] != LEDGER_COLUMNS or any(len(r) != len(LEDGER_COLUMNS) for r in rows):
        raise AssertionError("FOSS_REUSE_DECISION.tsv columns")
    decisions = {r[0]: r[6] for r in rows[1:]}
    for capability, decision in {
        "class-file constant/call-site scan": "DIRECT_IMPORT",
        "regex required-literal / trigram extraction": "EVIDENCE_ONLY",
    }.items():
        if decisions.get(capability) != decision:
            raise AssertionError(f"ledger decision {capability}: {decisions.get(capability)}")


def check_evidence() -> None:
    receipt = dict(r for r in tsv(PORT / "evidence/DIFFERENTIAL_RECEIPT.tsv")[1:])
    for key in ("total_mismatches", "diff_mismatch_find", "diff_mismatch_matches",
                "diff_mismatch_replace_all", "diff_mismatch_split", "oracle_unsound",
                "oracle_find_matches_mismatches"):
        if receipt.get(key) != "0":
            raise AssertionError(f"differential receipt {key}={receipt.get(key)}")
    harvest = dict(r for r in tsv(PORT / "evidence/HARVEST_RECEIPT.tsv")[1:])
    if harvest.get("image_self_check_keys") != harvest.get("keys_total"):
        raise AssertionError("harvest self-check")
    provenance = (PORT / "PROVENANCE.tsv").read_text(encoding="utf-8")
    for token in ("MIndexRegexProgram.java", "M3PatternQuery.java", "M3TQ.java", "asm-9.6"):
        if token not in provenance:
            raise AssertionError(f"provenance token missing: {token}")
    if not (ROOT / "m3/docs/m3-compiletime-precompute.md").is_file():
        raise AssertionError("design doc missing")


def main() -> None:
    targets = check_crate()
    check_ledger()
    check_evidence()
    print(f"M3 compile-time precompute custody: PASS targets={targets}")


if __name__ == "__main__":
    main()
