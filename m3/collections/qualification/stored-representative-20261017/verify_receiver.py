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
        "fd880291f6119899e530e19b1dc622bb4f1ea51c6c136ab1c510d507c303c761",
    "m3/collections/src/main/java/com/m3/collections/M3MutableFilteredViews.java":
        "bbc218ecd8c87648976501a5f09aa8baa5af94e1ea79fdef1647ac4248f59e65",
    "m3/collections/src/test/java/com/m3/collections/M3StoredRepresentativeContract.java":
        "9686f4d820c5872665d6273faa0de0b327b9689b12696af7827269cb8774e016",
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
