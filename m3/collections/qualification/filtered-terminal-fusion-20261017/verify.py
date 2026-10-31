#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Read-only proof that the product holds exact Maven/OpenRewrite output and owner hashes."""
from pathlib import Path
import hashlib

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[3]
R = HERE / "src/main/resources/com/synexia/rewrite/hash-pinned-java/filtered-terminal-fusion"

def digest(p):
    if not p.is_file() or p.is_symlink():
        raise ValueError("missing, aliased, or directory: " + str(p))
    return hashlib.sha256(p.read_bytes()).hexdigest()

def blob(p):
    data = p.read_bytes()
    return hashlib.sha1(("blob " + str(len(data)) + "\0").encode() + data).hexdigest()

def check():
    rows=[]
    for line in (R / "manifest.tsv").read_text().splitlines():
        if not line or line.startswith("#"):
            continue
        fields=line.split("\t")
        if len(fields)!=4: raise ValueError("bad recipe row")
        path,before,after,template=fields
        if not path.startswith("m3/collections/src/") or not path.endswith(".java"):
            raise ValueError("wrong collection target")
        if ".." in Path(path).parts or Path(path).is_absolute():
            raise ValueError("unsafe target")
        if before!="ABSENT" and before!=digest(HERE/"src/test/resources/preimage/M3LongFilteredView.java.txt"):
            raise ValueError("wrong preimage")
        if digest(R/template)!=after or digest(REPO/path)!=after:
            raise ValueError("generated/installed target mismatch: "+path)
        if (REPO/path).read_bytes()!=(R/template).read_bytes():
            raise ValueError("not actual emitted bytes")
        rows.append(path)
    if rows != [
        "m3/collections/src/main/java/com/m3/collections/M3LongFilteredView.java",
        "m3/collections/src/test/java/com/m3/collections/M3FilteredTerminalProof.java",
    ]:
        raise ValueError("wrong exact two-target set")
    owners=[]
    for line in (HERE/"M3_OWNER_SHA256.tsv").read_text().splitlines():
        if line.startswith("#") or not line: continue
        path,sha,git_sha=line.split("\t")
        owner=REPO/path
        if digest(owner)!=sha or blob(owner)!=git_sha:
            raise ValueError("upstream M3 owner changed: "+path)
        owners.append(path)
    if len(owners)!=3:raise ValueError("not all owners checked")
    print("PASS M3_FILTERED_TERMINAL_RECEIVER two emitted sources three unchanged owners")

if __name__ == "__main__":
    check()
