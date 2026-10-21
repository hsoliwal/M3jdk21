#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed source-image custody for the canonical Synexia recipe receiver."""
from pathlib import Path
from hashlib import sha256
root=Path(__file__).resolve().parents[3]
expected={
    "src/java.base/share/classes/java/util/ArrayList.java":
        "d05d5efd61e1ae8514c904d28cea7335eec824becddbf62dcb0b425ec7790c7b",
    "src/java.base/share/classes/java/util/Vector.java":
        "14ebbeeb90561ab7c24ba67f4bea90d55735d5c7212fd43c9799e5ba84afdace",
    "src/java.base/share/classes/jdk/internal/mindex/M3CollectionMaskCompactor.java":
        "6d26934bab87d6bc68a0c38f0c8ceeec72dc5817e5fca12cfafbc276bafb133d",
    "test/jdk/java/util/ArrayList/M3MaskCompactionDifferential.java":
        "84f7a259016ddcaf68fdc2fabb8bf6beb567b2c9ca38144386a03ae4f31c7091",
    "test/jdk/java/util/ArrayList/M3JdkRemoveIfMaskContracts.java":
        "afb109a5f6a1543f17cd972a091c45ccfc6ad784a849761d8bec4441cfbdd18f",
}
for name,digest in expected.items():
    p=root/name
    if not p.is_file() or p.is_symlink() or sha256(p.read_bytes()).hexdigest()!=digest:
        raise SystemExit("M3JDK_NATIVE_MASK_RECEIVER_SOURCE_DRIFT "+name)
source=(root/"src/java.base/share/classes/java/util/ArrayList.java").read_text()
vector=(root/"src/java.base/share/classes/java/util/Vector.java").read_text()
helper=(root/"src/java.base/share/classes/jdk/internal/mindex/M3CollectionMaskCompactor.java").read_text()
for owner,src in [("ArrayList",source),("Vector",vector)]:
    if src.count("import jdk.internal.mindex.M3CollectionMaskCompactor;")!=1:
        raise SystemExit(owner+" did not admit one M3 internal source owner")
    if src.count("M3CollectionMaskCompactor.compact(es, beg, end, deathRow)")!=1:
        raise SystemExit(owner+" original bit-mask boundary no longer recognized")
for phrase in ("System.arraycopy(values, from + nextSurvivor, values, write, span)",
               "return write;", "long[] deleted", "removalRuns(deleted, length)"):
    if phrase not in helper: raise SystemExit("missing compaction invariant: "+phrase)
print("M3JDK_NATIVE_MASK_RECEIVER_SOURCE_PASS targets=5 java_base=true")
