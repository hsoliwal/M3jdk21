#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Verify the regex oracle harness receiver: policy, hash-pinned crate copy, installed sources, corpus roots."""
from __future__ import annotations

import hashlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
CRATE = ROOT / "m3/tooling/migration-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-regex-oracle-harness"
POLICY = ROOT / "m3/docs/m3jdk-synexia-porting-policy.tsv"
PROVENANCE = ROOT / "m3/ports/precompute/REGEX_ORACLE_HARNESS_PROVENANCE.tsv"
CORPUS = ROOT / "m3/ports/precompute/src/main/java/com/m3/precompute/M3RegexOracleCorpus.java"

REQUIRED_POLICY = {
    "source_workspace": "hsoliwal/com.synexia",
    "target_repository": "hsoliwal/M3jdk21",
    "llm_source_change_requires_recipe": "true",
    "recipe_must_be_content_addressed": "true",
    "fixed_point_required": "true",
    "compiler_gate_required": "true",
    "contract_gate_required": "true",
    "approximate_signals_have_semantic_authority": "false",
    "regex_authority": "JDK_PATTERN_UNTIL_OPERATION_STATE_PARITY",
}

# The Synexia Regex10kMatrixMasteryCorpus roots; the installed corpus must cite exactly these.
CORPUS_ROOTS = {
    "REGEX_ROOT": "215d0e2d2fdf7b8491cd749fdae29ef254c5bec467e47d898e3158d26617b8a6",
    "STRING_ROOT": "4785a7e1ab13050473f5b42f4a31fb241ff1adba6f60dd1fae1d3a5e140ef069",
    "MATRIX_ROOT": "e7614598149534e42ea1ff5af8c00dfea1c440ff3d6d94864123974f8c11f998",
}


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def read_policy() -> dict[str, str]:
    rows: dict[str, str] = {}
    for line in POLICY.read_text(encoding="utf-8").splitlines():
        if not line.strip():
            continue
        key, value = line.split("\t", 1)
        if key == "schema":
            continue
        if key in rows:
            raise AssertionError(f"duplicate policy key: {key}")
        rows[key] = value
    return rows


def read_manifest() -> list[tuple[str, str, str, str]]:
    rows = []
    for line in (CRATE / "manifest.tsv").read_text(encoding="utf-8").splitlines():
        if not line or line.startswith("#"):
            continue
        cells = line.split("\t")
        if len(cells) != 4:
            raise AssertionError(f"invalid manifest row: {line}")
        rows.append((cells[0], cells[1], cells[2], cells[3]))
    return rows


def main() -> None:
    policy = read_policy()
    for key, expected in REQUIRED_POLICY.items():
        actual = policy.get(key)
        if actual != expected:
            raise AssertionError(f"policy {key}: expected {expected!r}, got {actual!r}")
    entries = read_manifest()
    if len(entries) != 3:
        raise AssertionError(f"expected three regex oracle harness targets, got {len(entries)}")
    previous = ""
    for target, before, after, resource in entries:
        if target <= previous:
            raise AssertionError("manifest target ordering is not canonical")
        previous = target
        if before != "ABSENT":
            raise AssertionError(f"target is not additive: {target}")
        if not target.startswith("m3/ports/precompute/src/"):
            raise AssertionError(f"target outside the precompute port: {target}")
        target_path = ROOT / target
        resource_path = CRATE / resource
        if not target_path.is_file() or not resource_path.is_file():
            raise AssertionError(f"missing target/resource: {target}")
        if target_path.read_bytes() != resource_path.read_bytes():
            raise AssertionError(f"source/resource drift: {target}")
        if sha256(target_path) != after:
            raise AssertionError(f"manifest SHA-256 drift: {target}")
    corpus = CORPUS.read_text(encoding="utf-8")
    for name, value in CORPUS_ROOTS.items():
        if f'{name} =\n            "{value}";' not in corpus:
            raise AssertionError(f"corpus root drift: {name}")
    provenance = PROVENANCE.read_text(encoding="utf-8")
    for token in (
        "Regex10kMatrixMasteryCorpus.java",
        "MIndexRegexPrecomputeMatrix10kTest.java",
        "JDK_PATTERN_ORACLE_SIGNALS_RANK_ONLY",
    ):
        if token not in provenance:
            raise AssertionError(f"missing provenance decision: {token}")
    print("M3 regex oracle harness port invariant: PASS")
    print(f"targets={len(entries)} policy_keys={len(policy)} corpus_roots={len(CORPUS_ROOTS)}")


if __name__ == "__main__":
    main()
