#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Seal the reviewed optional adapter template for the existing typed snapshot recipe."""
from hashlib import sha256
from pathlib import Path
import json

HERE = Path(__file__).resolve().parent
CRATE = "m3-openrewrite-shared-ast-20261017"
TARGET = "synexia-mindex/compiler-openrewrite/src/main/java/com/synexia/mindex/compiler/ast/M3OpenRewriteSharedAST.java"

def main():
    template = HERE.parent / "templates/M3OpenRewriteSharedAST.java.txt"
    resource = HERE / "resources/com/synexia/rewrite/hash-pinned-java" / CRATE
    resource.mkdir(parents=True, exist_ok=True)
    text = template.read_bytes()
    digest = sha256(text).hexdigest()
    resource.joinpath(template.name).write_bytes(text)
    resource.joinpath("manifest.tsv").write_text(f"{TARGET}\tABSENT\t{digest}\t{template.name}\n")
    providers = HERE.parents[3] / "src/main/java/com/synexia/rewrite"
    pins = {"format": "M3_OPENREWRITE_SHARED_AST_SOURCE_PINS_V1", "target": TARGET,
            "before_sha256": "ABSENT", "after_sha256": digest,
            "template": "../templates/" + template.name,
            "provider_sources": {p.name: sha256(p.read_bytes()).hexdigest() for p in
                [providers / name for name in ("M3HashPinnedJavaSnapshotRecipe.java", "M3EditScope.java", "M3ScopeInference.java")]},
            "openrewrite": {"version": "8.17.1", "source_commit": "3bd8271be963c025ce0856072d0f09360e3184cd"}}
    HERE.joinpath("SOURCE_PINS.json").write_text(json.dumps(pins, indent=2) + "\n")
    print(json.dumps({"manifest": str(resource / "manifest.tsv"), "after_sha256": digest}))

if __name__ == "__main__":
    main()
