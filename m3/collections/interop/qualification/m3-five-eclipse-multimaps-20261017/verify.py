#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Exact postimage+owner custody; no source mutation, no independent rewrite engine."""
import hashlib
from pathlib import Path
import sys

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[4]
CANONICAL = HERE / "recipe/src/main/resources/jdk/internal/m3/rewrite/hash-pinned-java/m3-five-eclipse-multimaps-20261017"

def read_checked(path: Path):
    if path.is_symlink() or not path.is_file():
        raise ValueError("missing/aliased source: " + str(path))
    return path.read_bytes()

def git_blob(data):
    return hashlib.sha1(("blob " + str(len(data)) + "\0").encode() + data).hexdigest()

def prove():
    rows=[]
    for line in read_checked(CANONICAL / "manifest.tsv").decode().splitlines():
        if not line or line.startswith("#"): continue
        path, before, after, template = line.split("\t")
        if before != "ABSENT" or len(after) != 64 or path.startswith("/") or ".." in Path(path).parts:
            raise ValueError("bad source manifest")
        if not path.startswith(("m3/collections/interop/src/main/java/jdk/m3/collections/interop/",
                                "m3/collections/interop/src/test/java/jdk/m3/collections/interop/")):
            raise ValueError("target escapes scoped M3 SDK")
        expected=read_checked(CANONICAL / template)
        installed=read_checked(ROOT / path)
        if hashlib.sha256(expected).hexdigest()!=after or installed!=expected:
            raise ValueError("generated Java does not equal canonical Maven recipe: " + path)
        rows.append(path)
    if len(rows)!=6 or rows!=sorted(set(rows)):
        raise ValueError("six exact source paths required, unique and sorted")
    pinned={
        "M3LinkedHashMap.java": "e0957cfd8a7a5abfa1c0c5e7bade32997e5ed657",
        "M3LinkedList.java": "832c838caf7a37d6f9a10b37505c3e65ed9f6292",
        "M3LinkedHashSet.java": "58fd72f6e1464a2c6e2c26a1519ee6cd226a3924",
    }
    for name, sha in pinned.items():
        p = ROOT / ("m3/collections/src/main/java/jdk/m3/collections/" + name)
        if git_blob(read_checked(p)) != sha:
            raise ValueError("existing core indexed owner changed: "+name)
    for filename in ("CATEGORY_REVIEW.tsv","FIVE_PROVIDER_OWNERS.tsv"):
        if len(read_checked(HERE/"recipe"/filename).splitlines())<6:
            raise ValueError("missing searchable five-provider algorithm/rights catalogue")
    print("PASS FIVE_LIBRARY_M3_RECEIVER generated_java=6 preserved_owner_blobs=3")

if __name__=="__main__":
    try:
        prove()
    except Exception as problem:
        print("HOLD FIVE_LIBRARY_M3_RECEIVER: "+str(problem),file=sys.stderr)
        sys.exit(1)
