#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed receiver source seals, not a Maven or full JVM runtime proof."""
from hashlib import sha256
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[2]
FILES = {
    "src/java.base/share/classes/java/util/HashMap.java":
        "8e5a059db03409858132297dd68811e16c19fc83d91d4449bdeca824f073605f",
    "test/jdk/java/util/HashMap/M3HashMapBulkCopyTest.java":
        "5ec2123cf141b34fb83df840639019880780f271b80e9c5525688a93754f7ef8",
}
for relative, expected in FILES.items():
    path = ROOT / relative
    if not path.is_file():
        sys.exit("M3_HASHMAP_BACKPORT_SOURCE_FAIL missing=" + relative)
    actual = sha256(path.read_bytes()).hexdigest()
    if actual != expected:
        sys.exit(f"M3_HASHMAP_BACKPORT_SOURCE_FAIL drift={relative} found={actual}")
content = (ROOT / "src/java.base/share/classes/java/util/HashMap.java").read_text()
for required in ("private void putHashMapEntries(",
                 "putVal(e.hash, e.key, e.value, false, evict);",
                 "(hm = m).getClass() == HashMap.class",
                 "hm = umap.m",
                 "putHashMapEntries((HashMap<? extends K, ? extends V>) hm, evict);",
                 "for (Map.Entry<? extends K, ? extends V> e : m.entrySet())"):
    if required not in content:
        sys.exit("M3_HASHMAP_BACKPORT_SOURCE_FAIL route=" + required)
print("M3_HASHMAP_BACKPORT_SOURCE_PASS targets=2 java21_binary_unverified=true")
