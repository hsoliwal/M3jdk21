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
        "e61f94f0262878726b5c7255ebc3aa655a7808c4d1544a8bd0ee3fd1bc49f1a7",
    "m3/collections/src/main/java/jdk/m3/collections/M3MutableFilteredViews.java":
        "499aea6a9750a121e60ddcf5ae62643e1e97c6e145b37b5fda28d4c2be2ec7ed",
    "m3/collections/src/test/java/jdk/m3/collections/M3StoredRepresentativeContract.java":
        "821b8ae1ec22516dceef16f5951ab66fe3f6db799f26732c620202bab862754b",
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
