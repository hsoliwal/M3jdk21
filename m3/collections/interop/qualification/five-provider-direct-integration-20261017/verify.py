#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed product receiving: EXACT eight Java and one TSV recipe postimages."""
from pathlib import Path
import hashlib

HERE=Path(__file__).resolve().parent
REPO=HERE.parents[4]
JAVA=HERE/"src/main/resources/com/synexia/rewrite/hash-pinned-java/five-provider-direct-integration"
TEXT=HERE/"src/main/resources/com/synexia/rewrite/hash-pinned-text/five-provider-direct-bindings"

def contents(path):
    if path.is_symlink() or not path.is_file():
        raise ValueError("missing or symlinked file: "+str(path))
    return path.read_bytes()
def sha(path):
    return hashlib.sha256(contents(path)).hexdigest()
def blob(path):
    data=contents(path)
    return hashlib.sha1(("blob "+str(len(data))+"\0").encode()+data).hexdigest()
def check():
    rows=[]
    for record in (JAVA/"manifest.tsv").read_text(encoding="utf-8").splitlines():
        if not record or record.startswith("#"):continue
        path,before,after,template=record.split("\t")
        if ".." in Path(path).parts or Path(path).is_absolute() or not path.startswith("m3/collections/") or not path.endswith(".java"):
            raise ValueError("unsupported Java target path")
        if not path.startswith("m3/collections/src/") and not path.startswith("m3/collections/interop/src/"):
            raise ValueError("unadmitted source directory")
        expected=JAVA/template
        installed=REPO/path
        if sha(expected)!=after or sha(installed)!=after or contents(expected)!=contents(installed):
            raise ValueError("generated Java not canonical source: "+path)
        if before!="ABSENT":
            original=HERE/"preimage/M3LibraryCollections.java.txt"
            if sha(original)!=before or blob(original)!="f65660191bfbfa5e10d54f7fe1365ce9bc8c9ce4":
                raise ValueError("original five-provider factory not source pinned")
        rows.append(path)
    if len(rows)!=8 or rows!=sorted(set(rows)):
        raise ValueError("not the eight admitted Java targets")

    records=[line for line in (TEXT/"manifest.tsv").read_text().splitlines() if line and not line.startswith("#")]
    if len(records)!=1:
        raise ValueError("incorrect text owner count")
    path,before,after,template=records[0].split("\t")
    if path!="m3/collections/interop/sdk-family-bindings.tsv":
        raise ValueError("noncanonical provider binding")
    original=TEXT/"sdk-family-bindings.tsv.before"
    result=TEXT/template
    installed=REPO/path
    if sha(original)!=before or sha(result)!=after or sha(installed)!=after or contents(installed)!=contents(result):
        raise ValueError("family FQCN not exact recipe output")

    parent={
        "m3/collections/src/main/java/com/m3/collections/M3ObjectCounts.java":"a95a378efbbccf884d2dc3fcf403bcad3cd063a2",
        "m3/collections/src/main/java/com/m3/collections/M3LinkedHashMap.java":"ade59880b8c9b2bee1b32ae67ca9d29b7382d998",
        "m3/collections/interop/src/main/java/com/m3/collections/interop/M3GuavaMultiset.java":"ce8efe529e4df77339dae26854e06d46a6b9aa84",
        "m3/collections/interop/src/main/java/com/m3/collections/interop/M3EclipseBag.java":"392f3119df304353ad76a7cdd44a1129757a7de5",
        "m3/collections/interop/src/main/java/com/m3/collections/interop/M3ApacheMultiSet.java":"cae7b719a92399d19de4557bdb1717cc438922b",
        "m3/collections/interop/src/main/java/com/m3/collections/interop/M3ApacheBag.java":"5589dd57c8d7f1429ea635f2cdffe8726f595745",
    }
    for path,expected in parent.items():
        if blob(REPO/path)!=expected:
            raise ValueError("existing five-provider owner was overwritten: "+path)
    print("PASS M3_DIRECT_FIVE_PROVIDER_RECEIVER java=8 metadata=1 retained_parent_owners=6")

if __name__=="__main__":check()
