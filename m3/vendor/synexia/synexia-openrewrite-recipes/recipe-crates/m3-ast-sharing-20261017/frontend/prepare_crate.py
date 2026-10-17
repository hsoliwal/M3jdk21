#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Seal reviewed frontend sources for the existing class-backed snapshot owner."""
import argparse
import hashlib
import json
from pathlib import Path

HERE = Path(__file__).resolve().parent
AST = "synexia-mindex/compiler/src/main/java/com/synexia/mindex/compiler/ast/"
RUNTIME = "synexia-mindex/runtime/src/main/java/com/synexia/mindex/ast/"
COMPILER = "src/jdk.compiler/share/classes/"
TARGET = "m3/ports/compiletime/src/main/java/com/m3/compiletime/ast/"
NAMES = ("M3File", "M3CompilationContext", "M3AST", "M3JavacASTIndex", "M3ASTCodec", "M3ASTPlugin")

def digest(data):
    return hashlib.sha256(data).hexdigest()

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--synexia", required=True, type=Path)
    parser.add_argument("--target", required=True, type=Path)
    parser.add_argument("--source-pins", default=HERE / "source-pins.json", type=Path)
    parser.add_argument("--hierarchy-manifest", type=Path,
                        help="Qualified typed hierarchy manifest carrying the existing MIndexAST successor")
    args = parser.parse_args()
    runtime_overrides = {}
    if args.hierarchy_manifest is not None:
        matches = [row.split("\t") for row in args.hierarchy_manifest.read_text().splitlines()
                   if row and row.split("\t")[0] == RUNTIME + "MIndexAST.java"]
        if len(matches) != 1 or len(matches[0]) != 4:
            raise ValueError("hierarchy manifest must seal exactly one existing MIndexAST successor")
        path, before, after, template = matches[0]
        if Path(template).name != template or before == "ABSENT":
            raise ValueError("hierarchy successor must retain its existing-owner preimage")
        data = (args.hierarchy_manifest.parent / template).read_bytes()
        if digest(data) != after or data != (args.synexia / path).read_bytes():
            raise ValueError("hierarchy successor is not its exact applied typed recipe output")
        runtime_overrides[path] = data
    canonical = {AST + name + ".java": (HERE / "templates" / (name + ".java.txt")).read_bytes()
                 for name in NAMES}
    canonical[AST + "MIndexJavaAST.java"] = (HERE / "templates/MIndexJavaAST.java.txt").read_bytes()
    canonical[RUNTIME + "MIndexASTImage.java"] = (HERE / "templates/MIndexASTImage.java.txt").read_bytes()
    compiler = {COMPILER + path: (HERE / "templates" / template).read_bytes()
                for path, template in (
                    ("com/sun/tools/javac/main/JavaCompiler.java", "JavaCompiler.java.txt"),
                    ("com/sun/tools/javac/m3/M3ASTPC.java", "M3ASTPC.java.txt"),
                    ("com/sun/tools/javac/m3/M3PrimitiveImageCompiler.java", "M3PrimitiveImageCompiler.java.txt"),
                    ("com/sun/source/util/M3AST.java", "M3AST.java.jdk.txt"))}
    receiver = {}
    lineage = []
    for record in json.loads(args.source_pins.read_text()):
        path = record["relative"]
        data = canonical.get(path, runtime_overrides.get(path, (args.synexia / path).read_bytes()))
        blob = hashlib.sha1(b"blob " + str(len(data)).encode() + b"\0" + data).hexdigest()
        if path not in canonical and path not in runtime_overrides and blob != record["git_blob_sha1"]:
            raise ValueError("canonical dependency drift: " + path)
        receiver["m3/vendor/synexia/" + path] = data
        lineage.append(dict(path=path, base_git_blob_sha1=record["git_blob_sha1"],
                            after_sha256=digest(data), changed=path in canonical or path in runtime_overrides))
    for name in ("M3FileSourceFacts", "M3ASTWorkspaceCache"):
        receiver["m3/vendor/synexia/" + AST + name + ".java"] = (args.synexia / (AST + name + ".java")).read_bytes()
    for name in NAMES:
        data = canonical[AST + name + ".java"]
        # The facade maps only its own package; original AST identities remain canonical.
        text = data.decode().replace("package com.synexia.mindex.compiler.ast;",
                                    "package com.m3.compiletime.ast;\n\n"
                                    "import com.synexia.mindex.compiler.ast.MIndexJavaAST;\n"
                                    "import com.synexia.mindex.compiler.ast.MIndexASTDocument;\n"
                                    "import com.synexia.mindex.compiler.ast.MIndexASTOccurrence;")
        receiver[TARGET + name + ".java"] = text.encode()
        receiver["m3/vendor/synexia/" + AST + name + ".java"] = data
    families = [("canonical", canonical, args.synexia),
                ("compiler", compiler, args.target),
                ("receiver", receiver, args.target)]
    composition = json.loads((HERE / "UPSTREAM_COMPILER_COMPOSITION.json").read_text())
    for label, paths in composition["recovery_paths"].items():
        source = canonical if label.startswith("canonical") else {**compiler, **receiver}
        root = args.synexia if label.startswith("canonical") else args.target
        families.append((label, {path: source[path] for path in paths}, root))
    for label, outputs, root in families:
        resource = HERE / "src/main/resources/com/synexia/rewrite/hash-pinned-java" / ("m3-ast-frontend-" + label + "-20261017")
        resource.mkdir(parents=True, exist_ok=True)
        prior_manifest = resource / "manifest.tsv"
        prior_preimages = {row.split("\t")[0]: row.split("\t")[1]
                          for row in prior_manifest.read_text().splitlines() if row} if prior_manifest.exists() else {}
        rows = []
        for index, (path, data) in enumerate(sorted(outputs.items())):
            name = str(index).zfill(3) + "-" + Path(path).name + ".txt"
            (resource / name).write_bytes(data)
            baseline = HERE / "preimages" / label / path
            if path not in prior_preimages and not baseline.exists() and (root / path).exists():
                baseline.parent.mkdir(parents=True, exist_ok=True)
                baseline.write_bytes((root / path).read_bytes())
            before = prior_preimages.get(path, digest(baseline.read_bytes()) if baseline.exists() else "ABSENT")
            if before != "ABSENT" and (not baseline.exists() or digest(baseline.read_bytes()) != before):
                raise ValueError("sealed preimage custody drift: " + path)
            if before == "ABSENT" and baseline.exists():
                raise ValueError("unexpected preimage for an originally additive target: " + path)
            rows.append("\t".join((path, before, digest(data), name)))
        retained = {row.split("\t")[3] for row in rows}
        for obsolete in resource.glob("[0-9]*-*.java.txt"):
            if obsolete.name not in retained:
                obsolete.unlink()
        (resource / "manifest.tsv").write_text("\n".join(rows) + "\n")
    (HERE / "SOURCE_LINEAGE.json").write_text(json.dumps({
        "schema": "M3_AST_FRONTEND_SOURCE_LINEAGE_V1",
        "synexia_base": "e244d68fdee88d4a406be48e4ff1d31b60a17988",
        "m3jdk_base": "e336c083c90bd882030e9a370949136e191386f2",
        "compiler_integration_base": composition["upstream_revision"],
        "composed_runtime_manifest_sha256": digest(args.hierarchy_manifest.read_bytes())
                if args.hierarchy_manifest is not None else None,
        "canonical_dependencies": lineage,
        "policy": "Exact Apache sources remain in the independent compiletime port; jdk.compiler receives a target-owned GPL seam only."
    }, indent=2) + "\n")

if __name__ == "__main__":
    main()
