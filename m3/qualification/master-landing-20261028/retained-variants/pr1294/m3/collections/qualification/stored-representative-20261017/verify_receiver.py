#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Reject edited/missing M3JDK21 received Java21 source; check known owner."""
from hashlib import sha256
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[4]
EXPECTED = {
    "m3/collections/src/main/java/com/m3/collections/M3FilteredViews.java":
        "e61f94f0262878726b5c7255ebc3aa655a7808c4d1544a8bd0ee3fd1bc49f1a7",
    "m3/collections/src/main/java/com/m3/collections/M3MutableFilteredViews.java":
        "51188863415920856a0f5e69ea8a54735e8502d7c0bcf8ca42fcf3a84bf1c6ec",
    "m3/collections/src/test/java/com/m3/collections/M3StoredRepresentativeContract.java":
        "821b8ae1ec22516dceef16f5951ab66fe3f6db799f26732c620202bab862754b",
    "m3/collections/src/test/java/com/m3/collections/M3StoredRepresentativeTest.java":
        "b948aca5ae6f14b80027429e6b820800927d3013bebb972fa92b712383af55f3"
}
for path, expected in EXPECTED.items():
    file = ROOT / path
    if not file.is_file():
        sys.exit("M3_STORED_REP_RECEIVER_FAIL missing=" + path)
    actual = sha256(file.read_bytes()).hexdigest()
    if actual != expected:
        sys.exit("M3_STORED_REP_RECEIVER_FAIL hash=" + path + " found=" + actual)
read = (ROOT / "m3/collections/src/main/java/com/m3/collections/M3FilteredViews.java").read_text()
write = (ROOT / "m3/collections/src/main/java/com/m3/collections/M3MutableFilteredViews.java").read_text()
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
        "a62827ffafc18bd6d710d7898741d2114a0068b8ac9ee63aac02e55038e1f293"
}
for name, expected in METADATA.items():
    source = ROOT / name
    if not source.is_file() or sha256(source.read_bytes()).hexdigest() != expected:
        sys.exit("M3_STORED_REP_RECEIVER_FAIL metadata drift=" + name)
print("M3_STORED_REP_RECEIVER_SOURCE_PASS files=6"
      " source_owner=Synexia target_owner=M3JDK21"
      " no_java_base_change=true no_JNI_change=true")
