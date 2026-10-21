#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Verify installed Java outputs exactly match the Synexia source-sealed OpenRewrite templates."""
from __future__ import annotations
import hashlib
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[3]
TEMPLATES = ROOT / "m3/tooling/migration-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-native-generic-deque-priority-20261018"
SOURCE_BLOBS = {
  "M3ArrayDeque.java": "9df441b51b1912fcaece88197b0e69e6fe89986a",
  "M3PriorityQueue.java": "bd462291a8a33862d943e0afc5dd839693548e8b",
  "M3GenericReferenceOwnersProof.java": "092a8972e758237363b6abe92d7827de1322ae2b",
  "M3GenericReferenceOwnersTest.java": "d486b58e77a08cce6007ecb4657e4d790d209251",
}
OWNERS = {
  "m3/collections/src/main/java/com/m3/collections/M3SlotArrays.java":
    "e337c69710c871f8caa77a1afe53149f1fd705cc",
  "m3/collections/src/main/java/com/m3/collections/M3IndexMaskCompactor.java":
    "134fb98f8a39969c43fd75338ecb233c5fe49446",
  "m3/collections/src/main/native/collections/m3_index_mask_compactor.c":
    "aaa74686949de59b44b363741ef1c414fe3e95d0",
}
def read(path):
    if path.is_symlink() or not path.is_file():
        raise ValueError("missing/symlinked input: " + str(path))
    return path.read_bytes()
def git_blob(data):
    return hashlib.sha1(("blob %d\0" % len(data)).encode("ascii") + data).hexdigest()
def main():
    declared = []
    for row in read(TEMPLATES / "manifest.tsv").decode("utf8").splitlines():
        if not row or row.startswith("#"): continue
        fields = row.split("\t")
        if len(fields) != 4: raise ValueError("invalid source manifest")
        path,before,after,resource = fields
        if before != "ABSENT" or len(after) != 64 or ".." in Path(path).parts:
            raise ValueError("non-ABSENT or malformed target")
        if not path.startswith("m3/collections/src/") or not path.endswith(".java"):
            raise ValueError("out of collection module")
        if Path(path).name not in SOURCE_BLOBS or resource != Path(path).name + ".txt":
            raise ValueError("unexpected generated Java target")
        data = read(ROOT / path)
        template = read(TEMPLATES / resource)
        if data != template or hashlib.sha256(data).hexdigest() != after:
            raise ValueError("installed Java differs from recipe: " + path)
        if git_blob(data) != SOURCE_BLOBS[Path(path).name]:
            raise ValueError("original donor source blob changed: " + path)
        declared.append(path)
    if len(declared) != 4 or declared != sorted(set(declared)):
        raise ValueError("recipe must own four sorted, distinct sources")
    for path,expected in OWNERS.items():
        if git_blob(read(ROOT / path)) != expected:
            raise ValueError("incumbent M3 Java/C storage owner drift: " + path)
    if b"WAITING_FOR_STRING" not in read(HERE / "RECEIPT.tsv"):
        raise ValueError("phase advanced without predecessor proof")
    print("PASS M3_DIRECT_GENERIC_OWNERS targets=4 preserved_owners=3")
if __name__ == "__main__": main()
