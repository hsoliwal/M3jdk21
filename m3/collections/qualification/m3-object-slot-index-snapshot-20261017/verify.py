#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
from __future__ import annotations

import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]
HERE = Path(__file__).resolve().parent
MIRROR = ROOT / "m3/tooling/migration-recipes/src/main/resources/jdk/internal/m3/rewrite/hash-pinned-java/m3-object-slot-index-snapshot-20261017"

TARGETS = {
    "m3/collections/src/main/java/jdk/m3/collections/M3ObjectIndexSnapshot.java":
        "67116969a93d0da9beffafdb36a8310247a206a96d7cb58ffbce3c053e1ad4f1",
    "m3/collections/src/main/java/jdk/m3/collections/M3ObjectSlotStore28.java":
        "032f837fea2e97b166fc08e7f7601d66e5d31513897a03632a08ec17bcf407bb",
    "m3/collections/src/test/java/jdk/m3/collections/M3ObjectSlotIndexSelfTest.java":
        "976975d979ffbde17bdf877349d60950da984b5f7207dc9f28ffeff6fe060ff7",
    "m3/collections/src/test/java/jdk/m3/collections/M3ObjectSlotIndexTest.java":
        "6f2b492c0be59b4c6d784b3d3be3623d5da69e7217047657c36635852b14d3b7",
}

def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()

def receipt() -> dict[str, str]:
    rows = {}
    for line in (HERE / "RECEIPT.tsv").read_text(encoding="utf-8").splitlines():
        if not line or line.startswith("schema"):
            continue
        schema, field, value = line.split("\t", 2)
        if schema != "M3_OBJECT_SLOT_INDEX_RECEIVER_V1" or field in rows:
            raise ValueError("invalid receiver receipt")
        rows[field] = value
    return rows

def manifest() -> dict[str, tuple[str, str]]:
    result = {}
    previous = ""
    for line in (MIRROR / "manifest.tsv").read_text(encoding="utf-8").splitlines():
        if not line or line.startswith("#"):
            continue
        path, before, after, resource = line.split("\t")
        if path <= previous or before != "ABSENT" or path in result:
            raise ValueError("invalid mirrored recipe manifest")
        template = MIRROR / resource
        if sha256(template) != after:
            raise ValueError("mirrored template hash drift: " + path)
        result[path] = (after, resource)
        previous = path
    return result

def main() -> None:
    r = receipt()
    if r.get("source_repository") != "hsoliwal/com.synexia":
        raise ValueError("wrong convergence owner")
    if r.get("source_pull_request") != "12080":
        raise ValueError("wrong Synexia PR")
    if r.get("receiving_state") != "WAITING_FOR_STRING" or r.get("runtime_promotion") != "false":
        raise ValueError("collections receiver promoted ahead of String")
    for field in ("source_head", "canonical_recipe_commit", "target_base"):
        value = r.get(field, "")
        if len(value) != 40 or any(c not in "0123456789abcdef" for c in value):
            raise ValueError("invalid pinned SHA: " + field)

    m = manifest()
    if set(m) != set(TARGETS):
        raise ValueError("target ownership drift")
    for target, expected in TARGETS.items():
        installed = ROOT / target
        if sha256(installed) != expected:
            raise ValueError("installed postimage drift: " + target)
        if m[target][0] != expected:
            raise ValueError("manifest/postimage mismatch: " + target)
        if installed.read_bytes() != (MIRROR / m[target][1]).read_bytes():
            raise ValueError("installed bytes differ from canonical mirror: " + target)

    local_catalogue = HERE / "CATEGORY_REVIEW.tsv"
    shared_catalogue = ROOT / "m3/collections/algorithm-catalog/object-slot-index-snapshot-20261017.tsv"
    if local_catalogue.read_bytes() != shared_catalogue.read_bytes():
        raise ValueError("shared algorithm catalogue drift")

    mapping = json.loads((ROOT / "m3/docs/name-mapping.json").read_text(encoding="utf-8"))
    plan = mapping["porting_invariant"]["receiving_sequence"]
    if plan["active_phase"] != "STRING":
        raise ValueError("String is no longer the active predecessor")
    phases = {phase["id"]: phase for phase in plan["phases"]}
    if phases["COLLECTIONS"]["state"] != "WAITING_FOR_PREDECESSOR":
        raise ValueError("Collections phase advanced without predecessor receipt")

    print("PASS M3_OBJECT_SLOT_INDEX_RECEIVER targets=4 state=WAITING_FOR_STRING "
          "jni=M3IndexMaskWindow signal=M3SignalOrder")

if __name__ == "__main__":
    main()
