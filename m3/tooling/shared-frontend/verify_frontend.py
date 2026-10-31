#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors
"""Run the real compiler/Rewrite contracts on a supplied JDK; no source edits."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess


def digest(path):
    value = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            value.update(block)
    return value.hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--jdk", required=True, type=Path)
    parser.add_argument("--repository", type=Path, default=Path(__file__).resolve().parents[3])
    parser.add_argument("--out", required=True, type=Path)
    parser.add_argument("--maven", type=Path)
    parser.add_argument("--maven-repository", type=Path)
    parser.add_argument("--online", action="store_true")
    args = parser.parse_args()
    jdk, repository, out = args.jdk.resolve(), args.repository.resolve(), args.out.absolute()
    if out.exists() or out.is_symlink():
        parser.error("--out must be a fresh directory")
    for name in ("java", "javac"):
        if not (jdk / "bin" / name).is_file():
            parser.error("JDK executable missing: " + name)
    if args.maven and not args.maven.is_file():
        parser.error("--maven must name the Maven executable")
    out.mkdir(parents=True)
    env = dict(os.environ, JAVA_HOME=str(jdk))
    commands = []

    def run(name, command):
        command = [str(value) for value in command]
        result = subprocess.run(command, cwd=repository, env=env, text=True,
                                stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        (out / (name + ".log")).write_text(result.stdout, encoding="utf-8")
        commands.append({"name": name, "argv": command, "exit": result.returncode})
        (out / "commands.json").write_text(json.dumps(commands, indent=2) + "\n")
        if result.returncode:
            raise SystemExit(name + " failed; see " + str(out / (name + ".log")))
        return result.stdout

    run("jdk-version", [jdk / "bin/java", "-version"])
    test = repository / "test/langtools/tools/javac/m3/M3ASTSharedFrontendTest.java"
    classes = out / "classes"
    classes.mkdir()
    export = "jdk.compiler/com.sun.tools.javac.m3=ALL-UNNAMED"
    run("compile", [jdk / "bin/javac", "-Xlint:all", "-Werror", "--add-exports", export,
                    "-d", classes, test])
    output = run("compiler-contracts", [jdk / "bin/java", "--add-exports", export,
                                        "-cp", classes, "M3ASTSharedFrontendTest"])
    match = re.search(r"M3_AST_SHARED_FRONTEND checks=([1-9][0-9]*)", output)
    if not match:
        raise SystemExit("compiler suite did not emit its completion marker")
    rewrite = False
    if args.maven:
        command = [args.maven.resolve(), "-B", "-ntp"]
        if not args.online:
            command.append("-o")
        if args.maven_repository:
            command.append("-Dmaven.repo.local=" + str(args.maven_repository.resolve()))
        command.extend(["-Dm3.proof.directory=" + str(out / "rewrite"), "-f",
                        repository / "m3/tooling/shared-frontend/openrewrite/pom.xml", "verify"])
        run("openrewrite", command)
        rewrite = True
    receipt = {
        "schema": "M3_SHARED_FRONTEND_RUNTIME_PROOF_1",
        "compiler_contracts": "PASS", "checks": int(match.group(1)),
        "openrewrite_contracts": "PASS" if rewrite else "NOT_RUN",
        "test_sha256": digest(test), "jdk_modules_sha256": digest(jdk / "lib/modules"),
        "jdk_release_sha256": digest(jdk / "release"),
        "scope": "Execution on the supplied image; source-build provenance is a separate receipt.",
        "performance_claim": "NONE"
    }
    (out / "RESULT.json").write_text(json.dumps(receipt, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(receipt, indent=2))


if __name__ == "__main__":
    main()
