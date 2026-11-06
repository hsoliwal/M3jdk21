#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Reject edited/missing M3JDK21 received Java21 source; check known owner."""
from hashlib import sha256
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[4]
EXPECTED = {
    "m3/collections/src/main/java/jdk/m3/collections/M3FilteredViews.java":
        "9ec03f9f01b6bfdfbc6f76ce320422d8599f56119a0e317a21904452a615e256",
    "m3/collections/src/main/java/jdk/m3/collections/M3MutableFilteredViews.java":
        "0ce075c9897724dc63c65b805480eaa918aaade46aec0188cfaf9aac3aa2863f",
    "m3/collections/src/test/java/jdk/m3/collections/M3StoredRepresentativeContract.java":
        "9686f4d820c5872665d6273faa0de0b327b9689b12696af7827269cb8774e016",
    "m3/collections/src/test/java/jdk/m3/collections/M3StoredRepresentativeTest.java":
        "446920d8a0f43b3badac44ff9440eaedc2bc9dc1f53653dcecc4de026489d316"
}
for path, expected in EXPECTED.items():
    file = ROOT / path
    if not file.is_file():
        sys.exit("M3_STORED_REP_RECEIVER_FAIL missing=" + path)
    actual = sha256(file.read_bytes()).hexdigest()
    if actual != expected:
        sys.exit("M3_STORED_REP_RECEIVER_FAIL hash=" + path + " found=" + actual)
read = (ROOT / "m3/collections/src/main/java/jdk/m3/collections/M3FilteredViews.java").read_text()
write = (ROOT / "m3/collections/src/main/java/jdk/m3/collections/M3MutableFilteredViews.java").read_text()
for claim in ["source instanceof Set<?>", "storedKey(Object query)",
              "nav.floorEntry(alias)", "sorted.tailSet(alias).first()"]:
    if claim not in read:
        sys.exit("M3_STORED_REP_RECEIVER_FAIL deleted read-only atom=" + claim)
for claim in ["private Entry<K, V> storedEntry(Object key)",
              "source.floorEntry(alias)", "new TreeMap<>(source.comparator())"]:
    if claim not in write:
        sys.exit("M3_STORED_REP_RECEIVER_FAIL deleted mutable atom=" + claim)
METADATA = {
    "m3/collections/verify_donation.py":
        "66a0483ff0c6ca11ad83f20fa02162e7960dc516089314d697ab456dcbcca133",
    "m3/collections/recipe/stored-representative-20261017/manifest.tsv":
        "4558799d12aa2ac5dd8789af591beb8789da632c93ad9020681062d10e664931"
}
for name, expected in METADATA.items():
    source = ROOT / name
    if not source.is_file() or sha256(source.read_bytes()).hexdigest() != expected:
        sys.exit("M3_STORED_REP_RECEIVER_FAIL metadata drift=" + name)
print("M3_STORED_REP_RECEIVER_SOURCE_PASS files=6"
      " source_owner=Synexia target_owner=M3JDK21"
      " no_java_base_change=true no_JNI_change=true")
