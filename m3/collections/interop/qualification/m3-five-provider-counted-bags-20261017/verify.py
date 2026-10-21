#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed identity verification. Does not generate/modify production Java."""
from pathlib import Path
import hashlib
import sys

HERE=Path(__file__).resolve().parent
ROOT=HERE.parents[4]
CRATE=HERE/"recipe/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-five-provider-counted-bags-20261017"

def checked(path):
    if path.is_symlink() or not path.is_file():
        raise ValueError("missing or aliased source: "+str(path))
    return path.read_bytes()

def sha(data):
    return hashlib.sha256(data).hexdigest()

def blob(data):
    return hashlib.sha1(("blob "+str(len(data))+"\0").encode("ascii")+data).hexdigest()

def verify():
    records=[]
    for line in checked(CRATE/"manifest.tsv").decode("utf-8").splitlines():
        if not line or line.startswith("#"): continue
        parts=line.split("\t")
        if len(parts)!=4: raise ValueError("invalid manifest row")
        target,before,expected,template=parts
        if before!="ABSENT" or len(expected)!=64: raise ValueError("invalid pre/post seal")
        if not target.startswith("m3/collections/interop/src/") or ".." in target.split("/"):
            raise ValueError("target escapes M3 interop owner")
        if Path(target).name+".txt"!=template:
            raise ValueError("noncanonical template mapping")
        actual=checked(ROOT/target)
        source=checked(CRATE/template)
        if source!=actual or sha(source)!=expected or sha(actual)!=expected:
            raise ValueError("recipe/generated output mismatch: "+target)
        records.append(target)
    if len(records)!=8 or records!=sorted(set(records)):
        raise ValueError("expected eight distinct ordered exact Java postimages")

    owners={
       "m3/collections/src/main/java/com/m3/collections/M3LinkedHashMap.java":
            "ade59880b8c9b2bee1b32ae67ca9d29b7382d998",
       "m3/collections/interop/src/main/java/com/m3/collections/interop/M3LibraryCollections.java":
            "f05d0949cadb14bfad90287444addf6501c21992",
    }
    for owner,expected in owners.items():
        if blob(checked(ROOT/owner))!=expected:
            raise ValueError("incumbent M3 provider owner drift: "+owner)
    if len(checked(HERE/"recipe/CATALOGUE.tsv").decode("utf-8").splitlines()) < 15:
        raise ValueError("missing categorized donor comparison")
    if b"WAITING_FOR_STRING" not in checked(HERE/"RECEIPT.tsv"):
        raise ValueError("product phase gate drift")
    print("PASS M3_FIVE_PROVIDER_COUNTED_BAGS exact_generated=8 stable_owners=2")

if __name__=="__main__":
    try: verify()
    except Exception as problem:
        print("REFUSE M3_FIVE_PROVIDER_COUNTED_BAGS: "+str(problem),file=sys.stderr)
        sys.exit(1)
