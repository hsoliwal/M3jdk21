#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
from __future__ import annotations

import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]
HERE = Path(__file__).resolve().parent
MIRROR = ROOT / "m3/tooling/migration-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-object-slot-index-snapshot-20261017"

TARGETS = {
    "m3/collections/src/main/java/com/m3/collections/M3ObjectIndexSnapshot.java":
        "ed19c7b34e23f61ea5bfa808550a1189ccc07900b7022f3572d78c23c5929f0c",
    "m3/collections/src/main/java/com/m3/collections/M3ObjectSlotStore28.java":
        "02893ff5d555ef577789ca82fdbbe043172219ae59202d10a51f2fa1337d9152",
    "m3/collections/src/test/java/com/m3/collections/M3ObjectSlotIndexSelfTest.java":
        "eb0526b86720fe5d16733995ee7d10d048c64546721cc11537c22851f3271b2d",
    "m3/collections/src/test/java/com/m3/collections/M3ObjectSlotIndexTest.java":
        "eec9b203115a9896ebcc074926e4817b5ebcbdeab4e259c68d3c34b773b46ed0",
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
