#!/usr/bin/env python3
"""Source-bound checks for the M3JDK21 String precompute bridge receiver."""

from __future__ import annotations

import csv
import hashlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
BRIDGE = ROOT / "src/java.base/share/classes/java/lang/M3StringPrecomputeBridge.java"
STRING = ROOT / "src/java.base/share/classes/java/lang/String.java"
TEST = ROOT / "test/jdk/java/lang/String/M3StringPrecomputeBridgeTest.java"
RECEIPT = ROOT / "m3/compatibility/m3string-precompute-bridge-receiver.tsv"


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def require(condition: bool, message: str) -> None:
    if not condition:
        raise SystemExit("M3_STRING_PRECOMPUTE_BRIDGE_FAIL|" + message)


def main() -> None:
    for path in (BRIDGE, STRING, TEST, RECEIPT):
        require(path.is_file(), "missing=" + str(path.relative_to(ROOT)))

    bridge = BRIDGE.read_text(encoding="utf-8")
    string = STRING.read_text(encoding="utf-8")
    test = TEST.read_text(encoding="utf-8")

    require("final class M3StringPrecomputeBridge" in bridge, "bridge-class")
    require("package java.lang;" in bridge, "bridge-package")
    require("com.synexia." not in bridge, "runtime-synexia-dependency")
    require("static final " not in bridge, "bridge-retained-static-field")
    require("private M3StringPrecomputeBridge() {}" in bridge, "bridge-constructor")

    required_routes = (
        "M3StringPrecomputeBridge.contentEquals(",
        "M3StringPrecomputeBridge.compareTo(",
        "M3StringPrecomputeBridge.startsWith(",
        "M3StringPrecomputeBridge.hashCodeValue(",
        "M3StringPrecomputeBridge.indexOf(",
        "M3StringPrecomputeBridge.lastIndexOf(",
        "M3StringPrecomputeBridge.substring(",
        "M3StringPrecomputeBridge.trim(",
        "M3StringPrecomputeBridge.strip(",
        "M3StringPrecomputeBridge.stripLeading(",
        "M3StringPrecomputeBridge.stripTrailing(",
    )
    for route in required_routes:
        require(route in string, "missing-route=" + route)

    require("private boolean cachedHashDiffers(M3String storage)" not in string,
            "cached-hash-helper-not-bridge-visible")
    require("boolean cachedHashDiffers(M3String storage)" in string,
            "cached-hash-helper-missing")
    require("M3_STRING_PRECOMPUTE_BRIDGE_PASS" in test, "jtreg-marker")

    with RECEIPT.open(newline="", encoding="utf-8") as stream:
        rows = list(csv.DictReader(stream, delimiter="\t"))
    require(len(rows) == 3, "receipt-cardinality")
    expected = {row["target_path"]: row["sha256"] for row in rows}
    actual = {
        str(BRIDGE.relative_to(ROOT)): sha256(BRIDGE),
        str(STRING.relative_to(ROOT)): sha256(STRING),
        str(TEST.relative_to(ROOT)): sha256(TEST),
    }
    require(expected == actual, "receipt-hash-drift")

    print(
        "M3_STRING_PRECOMPUTE_BRIDGE_SOURCE_PASS|"
        f"targets={len(rows)}|routes={len(required_routes)}|runtimeDependency=0|bridgeFields=0"
    )


if __name__ == "__main__":
    main()
