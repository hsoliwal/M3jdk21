#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Current-head byte proof; never claim this replaces Java and Maven tests."""
from __future__ import annotations
import hashlib
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[3]
RES = HERE / "recipe/src/main/resources/com/synexia/rewrite/m3-five-sdk-owner-repair-20261018"

def checked(path: Path) -> bytes:
    if not path.is_file() or path.is_symlink():
        raise ValueError("missing or symlinked: " + str(path))
    return path.read_bytes()

def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()

def blob(data: bytes) -> str:
    return hashlib.sha1(f"blob {len(data)}\0".encode("ascii") + data).hexdigest()

def run() -> None:
    records = []
    for line in checked(RES / "manifest.tsv").decode("utf-8").splitlines():
        if not line or line.startswith("#"): continue
        cells = line.split("\t")
        if len(cells) != 4: raise ValueError("manifest width")
        path,before,after,name = cells
        if not path.startswith("m3/collections/src/") or not path.endswith(".java"):
            raise ValueError("target outside exact module")
        if name != Path(path).name + (".after" if before != "ABSENT" else ".txt"):
            raise ValueError("unapproved source template")
        data = checked(ROOT / path)
        expected = checked(RES / name)
        if sha256(expected) != after or sha256(data) != after or expected != data:
            raise ValueError("not exact generated source: " + path)
        records.append(path)
    if len(records) != 3 or records != sorted(set(records)):
        raise ValueError("target set altered")
    sdk = checked(ROOT / records[0]).decode("utf-8")
    baseline = checked(RES / "M3CollectionSdk.java.before").decode("utf-8")
    if baseline.count("readOnlyFilteredSortedSet(") != 2 or baseline.count("readOnlyFilteredSortedMap(") != 2:
        raise ValueError("wrong sealed SDK precursor")
    if sdk.count("readOnlyFilteredSortedSet(") != 1 or sdk.count("readOnlyFilteredSortedMap(") != 1:
        raise ValueError("duplicate sorted SDK method restored")
    for name in ("m3ArrayList(", "indexedTable(", "indexedMultiValueMap(", "longRangeSet("):
        if sdk.count(name) != 1: raise ValueError("required M3 factory missing: " + name)
    owners = {
        "M3ArrayList.java": "250905f67429fbedfb4d6f652ea6e90a516cb59c",
        "M3IndexedTable.java": "5dc062e138b273382b4f161fb4c9869d90558390",
        "M3IndexedMultiValueMap.java": "9035b651c5aab1f5850009372e2432ef196efc79",
        "M3LongRangeSet.java": "5dbc2ec9317c79d801bb4c312f2c0b7ffad6ffbd",
        "M3IndexMaskCompactor.java": "5def05aad11dbe3ab7bebe27a3c11b5d241c9a7a"
    }
    for name,pin in owners.items():
        if blob(checked(ROOT / "m3/collections/src/main/java/com/m3/collections" / name)) != pin:
            raise ValueError("existing array/mask owner was changed: " + name)
    if blob(checked(ROOT / "m3/collections/src/main/native/collections/m3_index_mask_compactor.c")) != "a073e64cb12c59eb81247421cf3efe0628004142":
        raise ValueError("native ABI source changed")
    print("PASS exact SDK source: 3 generated targets, 5 original M3 owners, unchanged JNI C")

if __name__ == "__main__":
    run()
