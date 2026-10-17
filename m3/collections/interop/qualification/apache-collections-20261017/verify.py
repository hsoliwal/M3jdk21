#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Source and template custody; never a substitute for live vendor binary testing."""
from pathlib import Path
import hashlib

HERE=Path(__file__).resolve().parent
ROOT=HERE.parents[4]
RES=ROOT/"m3/collections/interop/recipe/apache-collections-sdk-20261017/src/main/resources"
JAVA=RES/"com/synexia/rewrite/hash-pinned-java/apache-collections-interop"
TEXT=RES/"com/synexia/rewrite/hash-pinned-text/apache-collections-pom"

def digest(p):
    if p.is_symlink() or not p.is_file():
        raise RuntimeError(f"missing or aliased: {p}")
    return hashlib.sha256(p.read_bytes()).hexdigest()

REFINE=RES/"com/synexia/rewrite/hash-pinned-java/apache-provider-contract-refinement"
refinements={}
for line in (REFINE/"manifest.tsv").read_text().splitlines():
    if not line or line.startswith("#"): continue
    target,before,after,template=line.split("\t")
    if target!="m3/collections/interop/src/test/java/com/m3/collections/interop/M3ApacheCollectionsContract.java" or target in refinements:
        raise RuntimeError("Unexpected refinement owner")
    if digest(REFINE/template)!=after:
        raise RuntimeError("Refinement template drift")
    refinements[target]=(before,after,REFINE/template)
if len(refinements)!=1: raise RuntimeError("Expected one source-sealed oracle refinement")

java_rows=[]
for line in (JAVA/"manifest.tsv").read_text().splitlines():
    if not line or line.startswith("#"): continue
    path,before,after,template=line.split("\t")
    if before!="ABSENT" or not path.startswith("m3/collections/interop/src/") or path in [p for p,_,_ in java_rows]:
        raise RuntimeError("Java source manifest drift")
    original=ROOT/path
    source=JAVA/template
    expected=after
    payload=source
    if path in refinements:
        previous,expected,payload=refinements[path]
        if previous!=after: raise RuntimeError("Refinement lineage differs from original recipe")
    if digest(source)!=after or digest(original)!=expected or original.read_bytes()!=payload.read_bytes():
        raise RuntimeError(f"Java source not recipe output: {path}")
    java_rows.append((path,after,template))
if len(java_rows)!=3 or [p for p,_,_ in java_rows]!=sorted(p for p,_,_ in java_rows):
    raise RuntimeError("Expected exactly three distinct new Java sources")

path,before,after,template=(TEXT/"manifest.tsv").read_text().strip().split("\t")
if path!="m3/collections/interop/pom.xml":
    raise RuntimeError("Unexpected Maven metadata owner")
if digest(TEXT/"pom.xml.before")!=before or digest(TEXT/template)!=after or digest(ROOT/path)!=after:
    raise RuntimeError("POM preimage/postimage not source sealed")
if (ROOT/path).read_bytes()!=(TEXT/template).read_bytes():
    raise RuntimeError("Receiver POM not exact canonical output")
print("PASS APACHE_COLLECTIONS_RECEIVER: three Java and one POM exact outputs; oracle successor lineage verified")
