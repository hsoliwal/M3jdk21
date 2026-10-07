#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
from __future__ import annotations

import hashlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
CRATE = ROOT / "m3/tooling/migration-recipes/src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned/m3-regex-string-precompute-v2"
POLICY = ROOT / "m3/docs/m3jdk-synexia-porting-policy.tsv"
PROVENANCE = ROOT / "m3/ports/precompute/REGEX_STRING_V2_PROVENANCE.tsv"
MAPPING = ROOT / "m3/ports/precompute/REGEX_STRING_V2_MAP.tsv"

REQUIRED_POLICY = {
    "source_workspace": "hsoliwal/com.synexia",
    "target_repository": "hsoliwal/M3jdk21",
    "llm_source_change_requires_recipe": "true",
    "recipe_must_be_content_addressed": "true",
    "fixed_point_required": "true",
    "compiler_gate_required": "true",
    "contract_gate_required": "true",
    "native_java_parity_if_native": "true",
    "approximate_signals_have_semantic_authority": "false",
    "blind_mindex_to_m3_rename": "false",
    "collections_migrate_per_concrete_owner": "true",
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
    if len(entries) != 4:
        raise AssertionError(f"expected four regex/String V2 targets, got {len(entries)}")

    previous = ""
    for target, before, after, resource in entries:
        if target <= previous:
            raise AssertionError("manifest target ordering is not canonical")
        previous = target
        if before != "ABSENT":
            raise AssertionError(f"V2 target is not additive: {target}")
        target_path = ROOT / target
        resource_path = CRATE / resource
        if not target_path.is_file() or not resource_path.is_file():
            raise AssertionError(f"missing target/resource: {target}")
        if target_path.read_bytes() != resource_path.read_bytes():
            raise AssertionError(f"source/resource drift: {target}")
        if sha256(target_path) != after:
            raise AssertionError(f"manifest SHA-256 drift: {target}")

    provenance = PROVENANCE.read_text(encoding="utf-8")
    for token in (
        "MIndexCodeTextSignalTrialImage.java",
        "M3RegexStringPrecomputeSignals.java",
        "NOT_PORTED_DUPLICATE_NATIVE_ABI_REUSE_EXISTING_M3_JNI",
    ):
        if token not in provenance:
            raise AssertionError(f"missing provenance decision: {token}")

    mapping = MAPPING.read_text(encoding="utf-8")
    for token in (
        "com.m3.precompute.M3RegexStringTrialImage",
        "com.m3.precompute.M3RegexStringSignals",
        "REUSED_TARGET_OWNER_NO_DIRECT_PORT",
    ):
        if token not in mapping:
            raise AssertionError(f"missing target mapping: {token}")

    print("M3 regex/String V2 port invariant: PASS")
    print(f"targets={len(entries)} policy_keys={len(policy)}")

if __name__ == "__main__":
    main()
