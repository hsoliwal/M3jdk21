#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
from __future__ import annotations

import argparse
import csv
import hashlib
import os
import json
from pathlib import Path

POLICY = "m3/docs/m3-intake-policy.tsv"
DIRECT = "m3/docs/m3-direct-copy-review.tsv"
MAPPING = "m3/docs/name-mapping.json"
FOSS = "m3/runtime-integration/FOSS_REUSE_DECISION.tsv"
LICENSE_INFO = "ADDITIONAL_LICENSE_INFO"

REQUIRED_MODES = {
    "EXTERNAL_RECIPE_TOOL",
    "INDEPENDENT_APACHE_MODULE",
    "DATA_OR_METADATA_EXPORT",
    "TARGET_OWNED_PORT",
    "EXPLICIT_RELICENSE_PORT",
    "EXISTING_DIRECT_COPY_REVIEW",
}


def rows(path: Path) -> list[dict[str, str]]:
    with path.open("r", encoding="utf-8", newline="") as stream:
        return list(csv.DictReader(stream, delimiter="\t"))


def require(condition: bool, message: str) -> None:
    if not condition:
        raise SystemExit(message)


def copied_m3_runtime_records(mapping: dict) -> dict[str, tuple[str, ...]]:
    result: dict[str, tuple[str, ...]] = {}
    for record in mapping.get("migration", {}).get("records", []):
        provenance = record.get("provenance", {})
        if provenance.get("copied_code") is not True:
            continue
        sources = record.get("sources", [])
        if not any(source.get("repo") == "hsoliwal/com.synexia" for source in sources):
            continue
        targets = tuple(
            target.get("path", "")
            for target in record.get("targets", [])
            if target.get("path", "").startswith("src/")
        )
        if targets:
            result[record["id"]] = targets
    return result


# Source-identity preflight for one mapped atom, not general src/ Apache admission.
_TEXT_RELATED_ID = "synexia.lexicons.related-source-range"
_TEXT_RELATED_PATH = "src/jdk.m3.text/share/classes/jdk/m3/text/SharedRelatedLexemeCatalog.java"
_TEXT_SCOPE = "m3/** independent module; src/jdk.m3.text independent module"
_TEXT_COMPONENT_PINS = (
    ("src/jdk.m3.text/share/classes/module-info.java", "bdd278210a498e2665620c02413b1e658c79370e"),
    ("src/jdk.m3.text/share/legal/m3text.md", "6e29d21cc33f50598d84a21955dea14120235bd2"),
    ("src/jdk.m3.text/share/legal/shortstring.md", "1baba1224d2db83e56ec04d639de00e2ccd5e59c"),
)
_TEXT_SOURCE_PIN = (
    "6e9b55d50693a64ef16ca29f7ad7d9953931b182",
    "38102366fa86ca63f3d00c78cd34effae7bc34ce0071544164ca74df97025b9e",
    "8ca7c18c9ce8bf43a1e72e04b67204d29b1114e9",
)


def _blob(data: bytes) -> str:
    return hashlib.sha1(b"blob " + str(len(data)).encode() + b"\0" + data).hexdigest()


def _component_bytes(root: Path, name: str) -> bytes:
    # Borrow the existing core-contract qualifier's regular-file/ancestor check.
    path = root / name
    require(path.is_file(), f"missing text component file: {name}")
    require(os.name != "nt" or hasattr(path, "is_junction"),
            "Windows junction inspection unavailable for text component")
    require(
        not any(part.is_symlink() or getattr(part, "is_junction", lambda: False)()
                for part in [path, *path.parents]),
        f"redirected text component file: {name}",
    )
    return path.read_bytes()


def _independent_related_text(root: Path, mapping: dict, policy: dict) -> bool:
    mode = policy["INDEPENDENT_APACHE_MODULE"]
    if mode["target_scope"] != _TEXT_SCOPE:
        return False
    records = [
        record for record in mapping.get("migration", {}).get("records", [])
        if record.get("id") == _TEXT_RELATED_ID
    ]
    for record in records:
        for target in record.get("targets", []):
            name = target.get("path")
            require(
                isinstance(name, str) and bool(name)
                and not name.startswith("/") and "\\" not in name and ":" not in name
                and not any(part in ("", ".", "..", ".git") for part in name.split("/")),
                "noncanonical related text component path",
            )
    selected = [
        (record, target) for record in records for target in record.get("targets", [])
        if target.get("path") == _TEXT_RELATED_PATH
    ]
    if not selected:
        return False
    require(len(records) == len(selected) == 1, "ambiguous related text component mapping")
    record, target = selected[0]
    require(
        mode["source_license"] == "Apache-2.0"
        and mode["source_copy"] == "ALLOW_WITH_NOTICES"
        and mode["runtime_dependency"] == "NO"
        and mode["disposition"] == "ADMITTED",
        "independent text component policy drift",
    )
    provenance = record.get("provenance", {})
    require(
        provenance.get("copied_code") is True
        and provenance.get("license", "").startswith("Apache-2.0"),
        "related text component provenance drift",
    )
    sources = record.get("sources", [])
    require(
        len(sources) == 1 and sources[0].get("repo") == "hsoliwal/com.synexia"
        and tuple(sources[0].get(key) for key in ("commit", "sha256", "git_blob_sha1"))
        == _TEXT_SOURCE_PIN,
        "related text component source lineage drift",
    )
    require(
        target.get("repo", "").casefold() == "hsoliwal/m3jdk21"
        and target.get("module") == "jdk.m3.text"
        and target.get("symbol") == "jdk.m3.text.SharedRelatedLexemeCatalog"
        and target.get("sha256") == "81322515e837de6473759e284464ac927578e691da16126a8a3f1b45e50c9b87"
        and target.get("git_blob_sha1") == "a8a158e31665d56b737929661b83c8e7ed9a1402",
        "related text component target identity drift",
    )
    data = _component_bytes(root, _TEXT_RELATED_PATH)
    require(
        hashlib.sha256(data).hexdigest() == target["sha256"]
        and _blob(data) == target["git_blob_sha1"],
        "related text component target bytes drift",
    )
    for name, expected in _TEXT_COMPONENT_PINS:
        require(_blob(_component_bytes(root, name)) == expected,
                f"text component boundary drift: {name}")
    return True


def verify(root: Path) -> None:
    policy_rows = rows(root / POLICY)
    require(policy_rows, "empty Synexia intake policy")
    require(
        len(policy_rows) == len(REQUIRED_MODES)
        and set(row["mode"] for row in policy_rows) == REQUIRED_MODES,
        "Synexia intake mode set drift",
    )
    policy = {row["mode"]: row for row in policy_rows}
    require(
        policy["TARGET_OWNED_PORT"]["source_copy"] == "NO_DIRECT_COPY",
        "target-owned port must refuse direct Apache source copy",
    )
    require(
        policy["EXTERNAL_RECIPE_TOOL"]["runtime_dependency"] == "NO",
        "OpenRewrite recipe tooling must not become a JDK runtime dependency",
    )
    require(
        policy["EXTERNAL_RECIPE_TOOL"]["target_scope"] == "hsoliwal/com.synexia only",
        "recipe authoring and execution must remain exclusively in Synexia",
    )
    require(
        policy["EXTERNAL_RECIPE_TOOL"]["source_copy"] == "POSTIMAGES_AND_PASSIVE_RECEIPTS_ONLY",
        "public targets may receive executed postimages and passive receipts only",
    )
    require(
        policy["EXPLICIT_RELICENSE_PORT"]["disposition"] == "REVIEW_REQUIRED",
        "direct target source copy must require explicit relicense review",
    )

    additional = " ".join((root / LICENSE_INFO).read_text(encoding="utf-8").split())
    require(
        "programs licensed under the Apache License, Version 2.0" in additional,
        "OpenJDK additional-license Apache boundary text missing",
    )
    require(
        "would not permit you to commingle code under an incompatible license" in additional,
        "OpenJDK no-commingling boundary text missing",
    )

    mapping = json.loads((root / MAPPING).read_text(encoding="utf-8"))
    copied = copied_m3_runtime_records(mapping)
    if _independent_related_text(root, mapping, policy):
        remaining = tuple(
            path for path in copied.get(_TEXT_RELATED_ID, ())
            if path != _TEXT_RELATED_PATH
        )
        if remaining:
            copied[_TEXT_RELATED_ID] = remaining
        else:
            copied.pop(_TEXT_RELATED_ID, None)
    review_rows = rows(root / DIRECT)
    review = {row["mapping_id"]: row for row in review_rows}
    require(set(review) == set(copied), "Synexia direct-copy review ledger drift")
    for mapping_id, targets in copied.items():
        row = review[mapping_id]
        require(row["review_status"] == "REVIEW_REQUIRED", f"{mapping_id}: review status drift")
        ledger_targets = tuple(part for part in row["m3jdk21_target_paths"].split(";") if part)
        require(ledger_targets == targets, f"{mapping_id}: target-path review drift")
        require(
            "relicense" in row["required_resolution"].lower()
            and "reimplementation" in row["required_resolution"].lower(),
            f"{mapping_id}: resolution alternatives incomplete",
        )

    foss_rows = rows(root / FOSS)
    synexia = [
        row for row in foss_rows
        if row.get("repository") == "https://github.com/hsoliwal/com.synexia"
    ]
    require(synexia, "Synexia missing from FOSS reuse ledger")
    for row in synexia:
        donor_license = row.get("license", "").split(";", 1)[0].strip()
        require("Apache-2.0" in donor_license, f"{row.get('capability')}: donor license drift")
        require(
            row.get("classification") != "DIRECT_IMPORT",
            f"{row.get('capability')}: unreviewed direct import is forbidden",
        )

    print(
        "M3_INTAKE_POLICY_PASS "
        f"modes={len(policy_rows)} direct_copy_reviews={len(review_rows)} "
        f"foss_synexia={len(synexia)}"
    )


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo", type=Path, default=Path(__file__).resolve().parents[2])
    args = parser.parse_args()
    verify(args.repo.resolve())


if __name__ == "__main__":
    main()
