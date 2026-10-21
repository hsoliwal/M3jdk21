#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed exact postimage and incumbent owner verifier; does not perform code generation."""
from pathlib import Path
import hashlib
import sys

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[4]
CANON = HERE / "canonical" / "src/main/resources"
JAVA = CANON / "com/synexia/rewrite/hash-pinned-java/m3-spring-case-sdk-20261018"
TEXT = CANON / "com/synexia/rewrite/hash-pinned-text/m3-spring-case-sdk-metadata-20261018"

def bytes_of(path):
    if path.is_symlink() or not path.is_file():
        raise ValueError("missing or aliased source/recipe file: " + str(path))
    return path.read_bytes()

def sha(data): return hashlib.sha256(data).hexdigest()
def blob(data):
    return hashlib.sha1(("blob " + str(len(data)) + "\0").encode("ascii") + data).hexdigest()

def verify_manifest(folder, expected_count):
    rows = []
    for line in bytes_of(folder/"manifest.tsv").decode("utf-8").splitlines():
        if not line or line.startswith("#"): continue
        cells = line.split("\t")
        if len(cells) != 4: raise ValueError("invalid source manifest row")
        path, before, after, template = cells
        if not path.startswith("m3/collections/interop/") or ".." in Path(path).parts:
            raise ValueError("source target out of owner boundary")
        generated = bytes_of(folder/template)
        target = bytes_of(ROOT/path)
        if sha(generated) != after or sha(target) != after or target != generated:
            raise ValueError("generated target differs from template: " + path)
        if before != "ABSENT" and sha(bytes_of(folder/"before"/template)) != before:
            raise ValueError("original source preimage changed: " + path)
        rows.append(path)
    if len(rows) != expected_count or rows != sorted(set(rows)):
        raise ValueError("source manifest cardinality/ordering drift")
    return rows

def verify():
    source = verify_manifest(JAVA, 4)
    text = verify_manifest(TEXT, 2)
    unchanged = {
        "m3/collections/interop/src/main/java/com/m3/collections/interop/M3SpringCaseInsensitiveMap.java":
            "b4ed56f4e128e875bf6ab0808f05572ff9ef0af5",
        "m3/collections/interop/src/test/java/com/m3/collections/interop/M3SpringCaseInsensitiveContract.java":
            "66a0f4edc0197f33d041f58b51fede0ce1044dcc",
        "m3/collections/interop/src/test/java/com/m3/collections/interop/M3SpringCaseInsensitiveTest.java":
            "6b2e8b8f4e241530337bd79bc379b8bfa97f73b4",
        "m3/collections/src/main/java/com/m3/collections/M3LinkedHashMap.java":
            "ade59880b8c9b2bee1b32ae67ca9d29b7382d998",
        "m3/collections/interop/pom.xml":
            "3ba7abd090f122b7cbc17f7a48b879b01c79a74f",
    }
    for path, expected in unchanged.items():
        if blob(bytes_of(ROOT/path)) != expected:
            raise ValueError("unchanged incumbent or dependency drift: " + path)
    java_engine = HERE/"engine/src/main/java/com/synexia/rewrite/M3HashPinnedJavaSnapshotRecipe.java"
    text_engine = HERE/"engine/src/main/java/com/synexia/rewrite/M3HashPinnedTextSnapshotRecipe.java"
    if blob(bytes_of(java_engine)) != "d58ca50dfaeb60f581b64f43545e688aca5c6530":
        raise ValueError("product original Java engine mirror mismatch")
    if blob(bytes_of(text_engine)) != "9d0788d3717e017d58345c5c3e62022c2915a3c8":
        raise ValueError("Synexia original text engine mirror mismatch")
    ledger = bytes_of(ROOT/"m3/collections/interop/sdk-family-bindings.tsv").decode("utf-8").splitlines()
    if len(ledger) != 24 or sum("\tspringCaseInsensitiveMap\t" in row for row in ledger) != 1:
        raise ValueError("SDK factory inventory not exactly 23 or duplicate")
    assert b"WAITING_FOR_STRING" in bytes_of(HERE/"RECEIPT.tsv")
    print("PASS SOURCE_SEALED_M3_SDK Java=" + str(len(source)) +
          " metadata=" + str(len(text)) + " preserved_owners=" + str(len(unchanged)))

if __name__ == "__main__":
    try: verify()
    except Exception as error:
        print("REFUSE M3 SDK: " + str(error), file=sys.stderr)
        raise SystemExit(1)
