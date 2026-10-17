#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed source/image check: exact recipe postimages only; no transformation."""
from pathlib import Path
import hashlib

HERE=Path(__file__).resolve().parent
REPO=HERE.parents[3]
RES=HERE/"src/main/resources/com/synexia/rewrite/hash-pinned-java/sequenced-sdk-jdk21"
PRE=HERE/"src/test/resources/preimage/M3CollectionSdk.java.txt"

def file_digest(path):
    if path.is_symlink() or not path.is_file():
        raise RuntimeError("missing or symlinked file: "+str(path))
    return hashlib.sha256(path.read_bytes()).hexdigest()

def git_blob(path):
    data=path.read_bytes()
    return hashlib.sha1(("blob "+str(len(data))+"\0").encode()+data).hexdigest()

def verify():
    owned=[]
    for line in (RES/"manifest.tsv").read_text(encoding="utf-8").splitlines():
        if not line or line.startswith("#"):continue
        path,before,after,template=line.split("\t")
        if path in owned or not path.startswith("m3/collections/src/") or ".." in Path(path).parts:
            raise RuntimeError("unexpected target ownership")
        if not path.endswith(".java"):raise RuntimeError("not Java source")
        actual=REPO/path
        resource=RES/template
        if file_digest(actual)!=after or file_digest(resource)!=after or actual.read_bytes()!=resource.read_bytes():
            raise RuntimeError("source not generated canonical bytes: "+path)
        if before!="ABSENT" and (file_digest(PRE)!=before or git_blob(PRE)!="042069cd167191e1faa1710fb9a9e65bca14983c"):
            raise RuntimeError("original facade identity drift")
        owned.append(path)
    if owned!=[
        "m3/collections/src/main/java/com/m3/collections/M3CollectionSdk.java",
        "m3/collections/src/test/java/com/m3/collections/M3SequencedSdkContract.java",
    ]:raise RuntimeError("unexpected source file list")
    owners={
        "m3/collections/src/main/java/com/m3/collections/M3LinkedHashMap.java":"ade59880b8c9b2bee1b32ae67ca9d29b7382d998",
        "m3/collections/src/main/java/com/m3/collections/M3LinkedHashSet.java":"e1ac806d1ebe8aac1b9efe7bcfbaa3f675e7d0d6",
        "m3/collections/src/main/java/com/m3/collections/M3LinkedList.java":"da39d792baf7311691d2d6e8398adf62f3aa88a8",
    }
    for path,expected in owners.items():
        if git_blob(REPO/path)!=expected:raise RuntimeError("M3 canonical owner drift: "+path)
    print("PASS SEQUENCED_SDK exact_generated_sources=2 unchanged_owners=3")

if __name__=="__main__":verify()
