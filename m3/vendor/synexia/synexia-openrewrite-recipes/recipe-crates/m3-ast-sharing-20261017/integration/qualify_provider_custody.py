#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors
"""Verify exact provider custody and clean-compile all seven authoring recipes."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import time
import xml.etree.ElementTree as ET

TASK = Path(__file__).resolve().parent.parent
PROJECTS = ("cache", "frontend", "frontend/hierarchy", "frontend/module-wiring",
            "frontend/registry", "frontend/openrewrite", "integration")


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--maven", default="mvn")
    parser.add_argument("--jdk", type=Path, required=True)
    parser.add_argument("--settings", type=Path)
    parser.add_argument("--local-repository", type=Path)
    parser.add_argument("--offline", action="store_true")
    args = parser.parse_args()
    custody = TASK / "provider-custody"
    sources = json.loads((custody / "SOURCES.json").read_text())
    for row in sources["sources"]:
        path = custody / row["path"]
        data = path.read_bytes()
        assert sha(path) == row["sha256"], f"provider drift: {path}"
        git = hashlib.sha1(b"blob " + str(len(data)).encode() + b"\0" + data).hexdigest()
        assert git == row["git_blob_sha1"], f"provider Git identity drift: {path}"
    env = dict(os.environ, JAVA_HOME=str(args.jdk.resolve()))
    env["MAVEN_OPTS"] = "-Xmx192m " + env.get("MAVEN_OPTS", "")
    evidence = TASK / "integration/evidence/provider-custody"
    evidence.mkdir(parents=True, exist_ok=True)
    rows = []
    for relative in PROJECTS:
        pom = TASK / relative / "pom.xml"
        tree = ET.parse(pom)
        source = tree.findtext("{*}build/{*}sourceDirectory")
        resolved = Path(source.replace("${project.basedir}", str(pom.parent))).resolve()
        assert resolved == custody, f"non-custody recipe source root: {pom}"
        command = [args.maven, "-B", "-f", str(pom), "clean", "test-compile", "-DskipTests"]
        if args.offline:
            command += ["-o"]
        if args.settings:
            command += ["-s", str(args.settings.resolve())]
        if args.local_repository:
            command += ["-Dmaven.repo.local=" + str(args.local_repository.resolve())]
        log = evidence / (relative.replace("/", "-") + ".log")
        start = time.monotonic()
        with log.open("w") as stream:
            result = subprocess.run(command, env=env, stdout=stream, stderr=subprocess.STDOUT)
        assert result.returncode == 0, f"recipe compilation failed: {relative}; see {log}"
        inputs = pom.parent / "target/maven-status/maven-compiler-plugin/compile/default-compile/inputFiles.lst"
        compiled = [Path(line).resolve() for line in inputs.read_text().splitlines() if line]
        assert compiled and all(path.is_relative_to(custody) for path in compiled), relative
        rows.append({"project": relative, "pom_sha256": sha(pom), "status": "PASS",
                     "provider_inputs": sorted(path.relative_to(custody).as_posix() for path in compiled),
                     "log_sha256": sha(log), "elapsed_seconds": round(time.monotonic() - start, 3)})
        print(json.dumps(rows[-1]), flush=True)
    receipt = {"schema": "M3_AST_PROVIDER_CUSTODY_QUALIFICATION_V1", "status": "PASS",
               "source_receipt_sha256": sha(custody / "SOURCES.json"), "exact_sources": len(sources["sources"]),
               "clean_compiled_projects": len(rows), "projects": rows,
               "production_outputs_changed": False,
               "scope": "Exact provider bytes and clean authoring compilation; executed recipe SDK behavioral evidence is retained separately."}
    (evidence / "QUALIFICATION.json").write_text(json.dumps(receipt, indent=2) + "\n")


if __name__ == "__main__":
    main()
