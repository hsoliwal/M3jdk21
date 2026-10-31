#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Product-side fail-closed source/template receipt; does not apply transformations."""
from pathlib import Path
import hashlib
import sys

HERE=Path(__file__).resolve().parent
ROOT=HERE.parents[4]
RESOURCE=ROOT/"m3/tooling/migration-recipes/src/main/resources/jdk/internal/m3/rewrite/hash-pinned-java/m3-spring-case-indexed-20261018"
TARGETS={
    "m3/collections/interop/src/main/java/jdk/m3/collections/interop/M3SpringCaseInsensitiveMap.java":"9e529d062f40aadea2e2fc5948a804059f6727c661e270e4742fca0349e14133",
    "m3/collections/interop/src/test/java/jdk/m3/collections/interop/M3SpringCaseInsensitiveContract.java":"697c933503ae75b5c864f4503cbe0983734457abde57e3eddb99b81ae7beafe0",
    "m3/collections/interop/src/test/java/jdk/m3/collections/interop/M3SpringCaseInsensitiveTest.java":"8571cedc31dcc9cda9a4c8b53eb9c354ee244e23a02ecbc8b452f0f838ef7805",
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
    incumbent=exact(ROOT/"m3/collections/src/main/java/jdk/m3/collections/M3LinkedHashMap.java")
    if blob(incumbent)!="e0957cfd8a7a5abfa1c0c5e7bade32997e5ed657":
        raise ValueError("authoritative M3 slot owner drift")
    receipt=exact(HERE/"RECEIVING.tsv")
    if b"WAITING_FOR_STRING" not in receipt or b"12213" not in receipt:
        raise ValueError("wrong source/phase receiving identity")
    print("PASS M3_SPRING_CASE_INDEX_RECEIVER targets=3 original_M3_owners=1")
if __name__=="__main__":
    try:verify()
    except Exception as problem:
        print("REFUSE M3_SPRING_CASE_INDEX_RECEIVER:",problem,file=sys.stderr);sys.exit(1)
