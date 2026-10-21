#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Product-side fail-closed source/template receipt; does not apply transformations."""
from pathlib import Path
import hashlib
import sys

HERE=Path(__file__).resolve().parent
ROOT=HERE.parents[4]
RESOURCE=ROOT/"m3/tooling/migration-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-spring-case-indexed-20261018"
TARGETS={
    "m3/collections/interop/src/main/java/com/m3/collections/interop/M3SpringCaseInsensitiveMap.java":"24e2d7542cb86f777f90f99ae2db1cbc18b3fa5bb46441fd66e2c0461d09c7d8",
    "m3/collections/interop/src/test/java/com/m3/collections/interop/M3SpringCaseInsensitiveContract.java":"e62a783c8d389e10627a8226cbee239ae837fdafd06893f403d52b5854146767",
    "m3/collections/interop/src/test/java/com/m3/collections/interop/M3SpringCaseInsensitiveTest.java":"3cca2ebfed563f4c2eff862e580605b4b438f9632095f325833a10a6e4c47b97",
}
def exact(path):
    if path.is_symlink() or not path.is_file():
        raise ValueError(f"missing or symlinked file: {path}")
    return path.read_bytes()
def sha(value):
    return hashlib.sha256(value).hexdigest()
def blob(value):
    return hashlib.sha1(("blob "+str(len(value))+"\0").encode()+value).hexdigest()
def verify():
    seen=[]
    last=""
    for line in exact(RESOURCE/"manifest.tsv").decode("utf8").splitlines():
        if not line or line.startswith("#"):continue
        target,before,after,template=line.split("\t")
        if target<=last or before!="ABSENT" or target not in TARGETS or after!=TARGETS[target]:
            raise ValueError("recipe manifest target/preimage/order drift: "+target)
        if ".." in Path(target).parts or Path(target).is_absolute() or template!=Path(target).name+".txt":
            raise ValueError("illegal target/recipe template path")
        output=exact(ROOT/target); source=exact(RESOURCE/template)
        if output!=source or sha(source)!=after:
            raise ValueError("generated output is not exact recipe template: "+target)
        seen.append(target);last=target
    if seen!=sorted(TARGETS):
        raise ValueError("missing or duplicated generated target")
    incumbent=exact(ROOT/"m3/collections/src/main/java/com/m3/collections/M3LinkedHashMap.java")
    if blob(incumbent)!="ade59880b8c9b2bee1b32ae67ca9d29b7382d998":
        raise ValueError("authoritative M3 slot owner drift")
    receipt=exact(HERE/"RECEIVING.tsv")
    if b"WAITING_FOR_STRING" not in receipt or b"12213" not in receipt:
        raise ValueError("wrong source/phase receiving identity")
    print("PASS M3_SPRING_CASE_INDEX_RECEIVER targets=3 original_M3_owners=1")
if __name__=="__main__":
    try:verify()
    except Exception as problem:
        print("REFUSE M3_SPRING_CASE_INDEX_RECEIVER:",problem,file=sys.stderr);sys.exit(1)
