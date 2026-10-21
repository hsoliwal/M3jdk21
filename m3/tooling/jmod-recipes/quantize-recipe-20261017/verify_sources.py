#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Read-only exact SHA-256 custody gate. Maven/OpenRewrite/JNI are separate authorities."""
import hashlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]
JAVA = ROOT / "m3/tooling/migration-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-cpu-int8-receiver-20261017"
META = Path(__file__).resolve().with_name("text-manifest.tsv")


def digest(contents):
    return hashlib.sha256(contents).hexdigest()


def source(path):
    location = Path(path)
    if location.is_absolute() or ".." in location.parts or not path.startswith("m3/"):
        raise ValueError("unadmitted source path " + path)
    target = ROOT / location
    if not target.is_file() or target.is_symlink():
        raise ValueError("unavailable source/rights file: " + path)
    return target.read_bytes()


def verify():
    entries = [line.split("\t") for line in (JAVA / "manifest.tsv")
               .read_text(encoding="utf-8").splitlines() if line and not line.startswith("#")]
    if len(entries) != 4 or [e[0] for e in entries] != sorted(e[0] for e in entries):
        raise ValueError("Java receipt must be four sorted source targets")
    for path, before, after, name in entries:
        actual = source(path)
        payload = (JAVA / name).read_bytes()
        if actual != payload or digest(actual) != after:
            raise ValueError("Java recipe/source mismatch: " + path)
        if before == "ABSENT":
            if (JAVA / "pre" / name).exists():
                raise ValueError("unexpected Java preimage: " + path)
        elif digest((JAVA / "pre" / name).read_bytes()) != before:
            raise ValueError("unverified modified source preimage: " + path)
    records = [line.split("\t") for line in META.read_text(encoding="utf-8").splitlines()
               if line and not line.startswith("#")]
    if len(records) != 14 or [r[0] for r in records] != sorted(r[0] for r in records):
        raise ValueError("native/legal/metadata inventory changed")
    for path, expected in records:
        if len(expected) != 64 or digest(source(path)) != expected:
            raise ValueError("native/legal/metadata SHA drift: " + path)
    prefix = ROOT / "m3/tooling/jmod-recipes"
    count = []
    for file in ("CPU_TOOL_DONOR_CATALOGUE_PIN_20261015.tsv",
                 "CPU_TOOL_RECEIVER_MAP_20261014.tsv",
                 "CPU_TOOL_RECEIVER_DEFERRALS_20261015.tsv"):
        lines = (prefix / file).read_text(encoding="utf-8").splitlines()
        count.append(sum(line.startswith("com.synexia.m3.") for line in lines))
    if count != [21, 19, 2]:
        raise ValueError("incomplete CPU-only catalogue closure " + str(count))
    print("M3_CPU_INT8_SOURCE_CUSTODY_PASS java=4 native_metadata=14"
          " donor=21 mapped=19 deferred=2 OPENREWRITE_SDK=SEPARATE JNI=SEPARATE")


if __name__ == "__main__":
    verify()
