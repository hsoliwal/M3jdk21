#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed product receiver: exact OpenRewrite outputs and unchanged source owners."""
from pathlib import Path
import hashlib
import sys

HERE=Path(__file__).resolve().parent
ROOT=HERE.parents[3]
ENGINE=ROOT/"m3/tooling/migration-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3jdk21-long-range-map-20261017"
EXPECTED_OWNER_BLOBS={
 "m3/collections/src/main/java/com/m3/collections/M3LongRangeSet.java":
     "5dbc2ec9317c79d801bb4c312f2c0b7ffad6ffbd",
 "m3/collections/src/main/java/com/m3/collections/M3IndexPathMasks.java":
     "ef0fac5c1e71f8493baab365c8af64bb22cb6171",
 "m3/collections/src/main/native/collections/m3_index_path_masks.c":
     "086b49293257fd17cc7fbd1ebbb98460aa677e35",
 "m3/tooling/migration-recipes/src/main/java/com/synexia/rewrite/M3HashPinnedJavaSnapshotRecipe.java":
     "d58ca50dfaeb60f581b64f43545e688aca5c6530",
}
def read(path):
    if path.is_symlink() or not path.is_file():
        raise ValueError("missing/symlinked source: "+str(path))
    return path.read_bytes()
def sha256(data):return hashlib.sha256(data).hexdigest()
def blob(data):return hashlib.sha1(("blob "+str(len(data))+"\0").encode()+data).hexdigest()

def run():
    manifest=read(ENGINE/"manifest.tsv").decode("utf-8").splitlines()
    entries=[]
    for line in manifest:
        if not line or line.startswith("#"):continue
        cells=line.split("\t")
        if len(cells)!=4:raise ValueError("invalid Java manifest columns")
        path, before, after, template=cells
        if before!="ABSENT" or not path.startswith((
          "m3/collections/src/main/java/com/m3/collections/",
          "m3/collections/src/test/java/com/m3/collections/")):
            raise ValueError("unsafe target or occupied-before receipt")
        if not path.endswith(".java") or Path(path).name+".txt"!=template:
            raise ValueError("unsound generated Java resource name")
        original=read(ROOT/path)
        generated=read(ENGINE/template)
        if original!=generated or sha256(original)!=after or len(after)!=64:
            raise ValueError("generated source/template SHA-256 drift: "+path)
        entries.append(path)
    if len(entries)!=3 or entries!=sorted(set(entries)):
        raise ValueError("expected three unique sorted canonical Java targets")
    for path, expected in EXPECTED_OWNER_BLOBS.items():
        if blob(read(ROOT/path))!=expected:
            raise ValueError("unchanged Java/C JNI or preceding range owner drift: "+path)
    catalogue=read(HERE/"CATEGORY_REVIEW.tsv").decode("utf-8").splitlines()
    if not catalogue[0].startswith("category\t") or len(catalogue)<10:
        raise ValueError("missing searchable source category evidence")
    receipt=read(HERE/"RECEIPT.tsv")
    if b"WAITING_FOR_STRING" not in receipt or b"NOT_EXECUTED_test_only_selector" not in receipt:
        raise ValueError("incorrect release/qualification declaration")
    print("PASS M3_LONG_RANGE_MAP_RECEIVER outputs=3 pinned_original_owners=4 category_rows="+str(len(catalogue)-1))

if __name__=="__main__":
    try:run()
    except Exception as problem:
        print("REFUSE M3_LONG_RANGE_MAP_RECEIVER: "+str(problem),file=sys.stderr)
        sys.exit(1)
