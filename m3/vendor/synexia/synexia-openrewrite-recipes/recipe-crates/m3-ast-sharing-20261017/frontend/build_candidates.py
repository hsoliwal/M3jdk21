#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Compile actual SDK candidates and produce the optional plugin and javac patch artifacts."""
import argparse
import json
import os
import re
import subprocess
import shutil
import hashlib
import tempfile
from pathlib import Path

HERE = Path(__file__).resolve().parent


def add_source(sources, path):
    """Deduplicate only byte-identical qualified sources with the same Java owner."""
    data = path.read_bytes()
    packages = re.findall(rb"^package\s+([A-Za-z_$][\w.$]*)\s*;", data, re.MULTILINE)
    if len(packages) != 1:
        raise ValueError("expected exactly one package declaration: " + str(path))
    owner = packages[0].decode("ascii") + "." + path.stem
    previous = sources.get(owner)
    if previous is not None and previous.read_bytes() != data:
        raise ValueError("conflicting qualified source snapshots for " + owner)
    sources.setdefault(owner, path)


def production_sources(root):
    return [path for path in sorted(root.rglob("*.java"))
            if "/src/main/java/" in path.as_posix()]

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--jdk", type=Path, required=True)
    parser.add_argument("--synexia", type=Path,
                        help="Compatibility argument; all compiler inputs now come from actual SDK candidates")
    parser.add_argument("--source-pins", type=Path, default=HERE / "source-pins.json")
    parser.add_argument("--hierarchy-candidates", type=Path)
    parser.add_argument("--expected-runtime-sources", type=int)
    args = parser.parse_args()
    candidate = HERE / "target/candidate/canonical"
    temporary = tempfile.TemporaryDirectory(prefix="m3-ast-qualified-artifacts-")
    workspace = Path(temporary.name)
    output = workspace / "classes"
    output.mkdir(parents=True, exist_ok=True)
    sources = {}
    receiver_root = HERE / "target/candidate/receiver"
    vendor = receiver_root / "m3/vendor/synexia"
    lineage = json.loads((HERE / "SOURCE_LINEAGE.json").read_text())
    dependency_hashes = {row["path"]: row["after_sha256"] for row in lineage["canonical_dependencies"]}
    for pin in json.loads(args.source_pins.read_text()):
        relative = pin["relative"]
        path = vendor / relative
        if hashlib.sha256(path.read_bytes()).hexdigest() != dependency_hashes[relative]:
            raise ValueError("receiver SDK dependency differs from sealed lineage: " + relative)
    for path in production_sources(vendor) + production_sources(candidate):
        add_source(sources, path)
    cache_candidates = HERE.parent / "cache/target/candidate"
    cache_target = cache_candidates / "m3-ast-cache-target-20261017"
    for path in production_sources(cache_target / "m3/vendor/synexia"):
        add_source(sources, path)
    for path in production_sources(cache_candidates / "m3-ast-cache-canonical-20261017"):
        add_source(sources, path)
    facades = {}
    for path in production_sources(receiver_root / "m3/ports/compiletime"):
        add_source(facades, path)
    for path in production_sources(cache_target / "m3/ports/compiletime"):
        add_source(facades, path)
    if args.hierarchy_candidates is not None:
        for path in production_sources(args.hierarchy_candidates):
            if b"package com.m3.compiletime.ast;" in path.read_bytes():
                add_source(facades, path)
            else:
                add_source(sources, path)
    expected_runtime = args.expected_runtime_sources
    if expected_runtime is None:
        expected_runtime = 103 if args.hierarchy_candidates is not None else 92
    expected_facades = 10 if args.hierarchy_candidates is not None else 8
    if len(sources) != expected_runtime or len(facades) != expected_facades:
        raise ValueError(f"qualified source closure differs: {len(sources)}/{expected_runtime} "
                         f"runtime, {len(facades)}/{expected_facades} facades")
    source_list = HERE / "target/runtime/sources.txt"
    source_list.parent.mkdir(parents=True, exist_ok=True)
    source_list.write_text("\n".join(str(sources[key]) for key in sorted(sources)) + "\n")
    with (HERE / "target/runtime/javac.log").open("w") as log:
        subprocess.run([str(args.jdk / "bin/javac"), "-J-Xmx384m", "--release", "21", "-Xlint:all", "-Werror",
                        "-d", str(output), "@" + str(source_list)], check=True, stdout=log, stderr=subprocess.STDOUT)
    facade_sources = [facades[key] for key in sorted(facades)]
    with (HERE / "target/runtime/javac-facade.log").open("w") as log:
        subprocess.run([str(args.jdk / "bin/javac"), "-J-Xmx256m", "--release", "21", "-Xlint:all", "-Werror",
                        "-classpath", str(output), "-d", str(output)] + [str(x) for x in facade_sources],
                       check=True, stdout=log, stderr=subprocess.STDOUT)
    service = output / "META-INF/services/com.sun.source.util.Plugin"
    service.parent.mkdir(parents=True, exist_ok=True)
    service.write_text("com.m3.compiletime.ast.M3ASTPlugin\n")
    plugin = workspace / "m3-ast-sharing.jar"
    subprocess.run([str(args.jdk / "bin/jar"), "--create", "--date=2026-10-17T00:00:00Z", "--file", str(plugin), "-C", str(output), "."], check=True)
    patch_sources = HERE / "target/candidate/compiler/src/jdk.compiler/share/classes"
    patch_output = workspace / "compiler-patch"
    patch_output.mkdir(parents=True, exist_ok=True)
    inputs = sorted(patch_sources.rglob("*.java"))
    if len(inputs) != 4:
        raise ValueError("compiler recipe must emit exactly its four reviewed sources, including the preserved upstream API")
    with (HERE / "target/javac-patch.log").open("w") as log:
        subprocess.run([str(args.jdk / "bin/javac"), "-J-Xmx256m", "-source", "21", "-target", "21",
                        "--patch-module", "jdk.compiler=" + str(patch_sources),
                        "-implicit:none", "-Xlint:all", "-Werror", "-d", str(patch_output)]
                       + [str(x) for x in inputs], check=True, stdout=log, stderr=subprocess.STDOUT)
    patch = workspace / "m3-javac-patch.jar"
    subprocess.run([str(args.jdk / "bin/jar"), "--create", "--date=2026-10-17T00:00:00Z", "--file", str(patch), "-C", str(patch_output), "."], check=True)
    # Publish complete jars only after both compiler phases have succeeded. Temporary
    # compiler output keeps independent builds and file synchronizers out of the classpath.
    for artifact in (plugin, patch):
        destination = HERE / "target" / artifact.name
        pending = destination.with_suffix(".jar.new")
        pending.write_bytes(artifact.read_bytes())
        pending.replace(destination)
    plugin = HERE / "target/m3-ast-sharing.jar"
    patch = HERE / "target/m3-javac-patch.jar"
    receipt = {"plugin": str(plugin), "patch": str(patch), "runtime_sources": len(sources),
               "facade_sources": len(facade_sources), "compiler_sources": len(inputs),
               "plugin_sha256": hashlib.sha256(plugin.read_bytes()).hexdigest(),
               "patch_sha256": hashlib.sha256(patch.read_bytes()).hexdigest(),
               "compiler_source_sha256": {path.relative_to(patch_sources).as_posix():
                                           hashlib.sha256(path.read_bytes()).hexdigest()
                                           for path in inputs},
               "source_sha256": {key: hashlib.sha256(path.read_bytes()).hexdigest()
                                 for key, path in sorted({**sources, **facades}.items())}}
    (HERE / "target/ARTIFACTS.json").write_text(json.dumps(receipt, indent=2) + "\n")
    print(json.dumps({key: value for key, value in receipt.items() if key != "source_sha256"}))
    temporary.cleanup()

if __name__ == "__main__":
    main()
