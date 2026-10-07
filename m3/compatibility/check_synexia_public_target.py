#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed license/provenance boundary for Synexia public-target handoff packets."""

from __future__ import annotations

import csv
import re
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
POLICY = HERE / "synexia-public-target-policy.tsv"
APACHE_HANDOFF_PIN = HERE / "synexia-apache-handoff-pin.tsv"
PACKET_VERSION = "# SYNEXIA_M3_HANDOFF_V1"
LICENSE_POLICY = "SYNEXIA_FIRST_PARTY_APACHE2_V1"
FAST_LANE_LICENSE = "Apache-2.0"
SHA256 = re.compile(r"[0-9a-f]{64}")

REQUIRED_ROWS = {
    ("SYNEXIA_FIRST_PARTY", "Apache-2.0", "M3_TOOLING", "COPY_PRESERVE_LICENSE", "Apache-2.0"),
    ("SYNEXIA_FIRST_PARTY", "Apache-2.0", "M3_RECIPE", "COPY_PRESERVE_LICENSE", "Apache-2.0"),
    ("SYNEXIA_FIRST_PARTY", "Apache-2.0", "JDK_SEPARATE_COMPONENT", "COPY_WITH_LEGAL_NOTICE", "Apache-2.0"),
    ("THIRD_PARTY", "*", "ANY", "SEPARATE_ARTIFACT_REVIEW", "ORIGINAL"),
    ("REFERENCE_ONLY", "*", "ANY", "NO_SOURCE_COPY", "N/A"),
    (
        "OPENJDK_EXISTING",
        "GPL-2.0-only WITH Classpath-exception-2.0",
        "JDK_PRODUCT",
        "KEEP_EXISTING_LICENSE",
        "GPL-2.0-only WITH Classpath-exception-2.0",
    ),
}


def load_policy(path: Path = POLICY) -> list[tuple[str, ...]]:
    with path.open("r", encoding="utf-8", newline="") as stream:
        reader = csv.reader(stream, delimiter="	")
        try:
            header = next(reader)
        except StopIteration as failure:
            raise ValueError("empty Synexia public-target policy") from failure
        expected = [
            "source_class",
            "source_license",
            "target_class",
            "action",
            "retained_license",
        ]
        if header != expected:
            raise ValueError("invalid Synexia public-target policy header")
        rows: list[tuple[str, ...]] = []
        seen: set[tuple[str, str, str]] = set()
        for physical, cells in enumerate(reader, start=2):
            if len(cells) != 5 or any(not cell for cell in cells):
                raise ValueError(f"invalid policy row {physical}")
            row = tuple(cells)
            key = (cells[0], cells[1], cells[2])
            if key in seen:
                raise ValueError(f"duplicate policy row {physical}")
            seen.add(key)
            rows.append(row)
    if set(rows) != REQUIRED_ROWS:
        missing = REQUIRED_ROWS.difference(rows)
        extra = set(rows).difference(REQUIRED_ROWS)
        raise ValueError(f"policy drift missing={sorted(missing)!r} extra={sorted(extra)!r}")
    return rows


def load_apache_handoff_pin(path: Path = APACHE_HANDOFF_PIN) -> dict[str, str]:
    with path.open("r", encoding="utf-8", newline="") as stream:
        reader = csv.reader(stream, delimiter="\t")
        try:
            header = next(reader)
        except StopIteration as failure:
            raise ValueError("empty Synexia Apache handoff pin") from failure
        if header != ["field", "value"]:
            raise ValueError("invalid Synexia Apache handoff pin header")
        rows: dict[str, str] = {}
        for physical, cells in enumerate(reader, start=2):
            if len(cells) != 2 or not cells[0] or not cells[1]:
                raise ValueError(f"invalid Apache handoff pin row {physical}")
            if cells[0] in rows:
                raise ValueError(f"duplicate Apache handoff pin field {cells[0]}")
            rows[cells[0]] = cells[1]

    expected = {
        "source_repository": "hsoliwal/com.synexia",
        "source_pr": "9642",
        "source_commit": "25489c202af43863bda2ae23b67542c363e92450",
        "source_manifest_path": ".m3/apache-handoff.tsv",
        "source_manifest_sha256": "98274241cc6a0655e600d5fb5c58d70170aca0111770f95c14609041a1f1289a",
        "source_license": "Apache-2.0",
        "copyright_notice": "Copyright 2026 Hitesh Soliwal and contributors",
        "canonical_owner": "synexia-m3-recipe",
        "delivery_state": "PENDING_SYNEXIA_MERGE",
        "target_role": "QUALIFICATION_INPUT_ONLY",
        "automatic_application": "false",
        "target_relicense_authority": "false",
        "openjdk_retained_license": "GPL-2.0-only WITH Classpath-exception-2.0",
    }
    if rows != expected:
        raise ValueError("Synexia Apache handoff pin drift")
    if not re.fullmatch(r"[0-9a-f]{40}", rows["source_commit"]):
        raise ValueError("invalid Synexia Apache handoff commit")
    if not SHA256.fullmatch(rows["source_manifest_sha256"]):
        raise ValueError("invalid Synexia Apache handoff manifest seal")
    if rows["automatic_application"] != "false" or rows["delivery_state"] != "PENDING_SYNEXIA_MERGE":
        raise ValueError("unmerged Synexia custody PR cannot authorize automatic application")
    if rows["target_relicense_authority"] != "false":
        raise ValueError("Synexia handoff cannot relicense target code")
    return rows


def validate_packet(path: Path) -> int:
    text = path.read_text(encoding="utf-8")
    lines = text.splitlines()
    if not lines or lines[0] != PACKET_VERSION:
        raise ValueError("invalid Synexia handoff packet version")

    headers: dict[str, str] = {}
    rows: list[list[str]] = []
    for physical, line in enumerate(lines[1:], start=2):
        if not line:
            continue
        cells = line.split("	")
        if line.startswith("@"):
            if len(cells) != 2 or not cells[0].startswith("@"):
                raise ValueError(f"invalid packet header row {physical}")
            if cells[0] in headers:
                raise ValueError(f"duplicate packet header {cells[0]}")
            headers[cells[0]] = cells[1]
            continue
        if cells[0] != "ROW" or len(cells) != 13:
            raise ValueError(f"invalid packet row {physical}")
        rows.append(cells)

    if headers.get("@license-policy") != LICENSE_POLICY:
        raise ValueError("Synexia handoff packet is not on the first-party Apache-2.0 lane")
    revision = headers.get("@source-revision", "")
    if not re.fullmatch(r"[0-9a-f]{40}", revision):
        raise ValueError("invalid Synexia source revision")
    if not rows:
        raise ValueError("empty Synexia handoff packet")

    targets: set[str] = set()
    for cells in rows:
        source_sha = cells[4]
        target = cells[5]
        before = cells[6]
        repeated_sha = cells[7]
        license_id = cells[9]

        if not SHA256.fullmatch(source_sha) or source_sha != repeated_sha:
            raise ValueError(f"invalid source seal for {target}")
        if before != "ABSENT" and not SHA256.fullmatch(before):
            raise ValueError(f"invalid target preimage for {target}")
        if license_id != FAST_LANE_LICENSE:
            raise ValueError(f"non-Apache payload is not admitted by public-target fast lane: {target}")
        if target in targets:
            raise ValueError(f"duplicate target {target}")
        targets.add(target)
        if target.startswith("/") or "\\" in target or any(
            part in {"", ".", "..", ".git"} for part in target.split("/")
        ):
            raise ValueError(f"noncanonical target {target}")

    return len(rows)


def main(argv: list[str]) -> int:
    load_policy()
    load_apache_handoff_pin()
    if len(argv) > 2:
        print("usage: check_synexia_public_target.py [packet.tsv]", file=sys.stderr)
        return 2
    if len(argv) == 2:
        rows = validate_packet(Path(argv[1]))
        print(f"SYNEXIA_PUBLIC_TARGET_POLICY_PASS rows={rows} license={FAST_LANE_LICENSE}")
    else:
        print("SYNEXIA_PUBLIC_TARGET_POLICY_PASS policy=checked apache_handoff=qualification-only")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
