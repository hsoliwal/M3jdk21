#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Bounded javac proof for bootstrap parser types or actual SDK-emitted hierarchy sources."""
import argparse
import hashlib
import json
import os
import re
import shutil
import subprocess
import tempfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
BASE = HERE.parent / "target/m3-ast-sharing.jar"


def sha(data):
    return hashlib.sha256(data).hexdigest()


def qualified_inputs(stage):
    sources = {}
    tests = {}
    for label in ("canonical", "receiver"):
        resource = HERE / "src/main/resources/com/synexia/rewrite/hash-pinned-java" / ("m3-ast-hierarchy-" + label + "-20261017")
        for row in (resource / "manifest.tsv").read_text().splitlines():
            path, _, expected, template = row.split("\t")
            file = resource / template if stage == "bootstrap" else HERE / "target/candidate" / label / path
            data = file.read_bytes()
            if sha(data) != expected:
                raise ValueError("nonqualified hierarchy source: " + path)
            package = re.findall(rb"^package\s+([A-Za-z_$][\w.$]*)\s*;", data, re.M)
            if len(package) != 1:
                raise ValueError("ambiguous Java source owner")
            owner = package[0].decode() + "." + Path(path).stem
            collection = tests if "/src/test/java/" in path else sources
            if owner in collection and collection[owner][1] != data:
                raise ValueError("conflicting hierarchy owner: " + owner)
            collection[owner] = (path, data)
    if len(sources) != 14 or len(tests) != 4:
        raise ValueError(f"hierarchy closure drift: {len(sources)} sources, {len(tests)} tests")
    return sources, tests


def atomic_file(source, destination):
    destination.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.NamedTemporaryFile(prefix=destination.name + ".", suffix=".pending",
                                     dir=destination.parent, delete=False) as stream:
        pending = Path(stream.name)
    try:
        shutil.copyfile(source, pending)
        pending.replace(destination)
    finally:
        pending.unlink(missing_ok=True)


def publish_tests(source, destination):
    # Tests are never assembled in an old Maven input directory. Replace the
    # complete six-file generation after source admission has finished.
    staging = Path(tempfile.mkdtemp(prefix=".tests-generation-", dir=destination.parent))
    retired = Path(tempfile.mkdtemp(prefix=".tests-retired-", dir=destination.parent))
    retired.rmdir()
    try:
        shutil.copytree(source, staging, dirs_exist_ok=True)
        if destination.exists():
            destination.replace(retired)
        staging.replace(destination)
    except BaseException:
        if retired.exists() and not destination.exists():
            retired.replace(destination)
        raise
    finally:
        shutil.rmtree(staging, ignore_errors=True)
        shutil.rmtree(retired, ignore_errors=True)


def build_generation(args, directory, sources, tests):
    classes = directory / "classes"
    classes.mkdir()
    base_sha = sha(args.base_jar.read_bytes())
    materialized = []
    for owner, (path, data) in sorted(sources.items()):
        file = directory / "src" / (owner.replace(".", "/") + ".java")
        file.parent.mkdir(parents=True, exist_ok=True)
        file.write_bytes(data)
        materialized.append(file)
    arguments = directory / "javac-sources.txt"
    arguments.write_text("\n".join(str(path) for path in materialized) + "\n")
    with (directory / "javac.log").open("w") as log:
        subprocess.run([str(args.jdk / "bin/javac"), "-J-Xmx256m", "--release", "21", "-Xlint:all", "-Werror",
                        "-proc:none", "-classpath", str(args.base_jar), "-d", str(classes), "@" + str(arguments)],
                       check=True, stdout=log, stderr=subprocess.STDOUT)
    jar = directory / "m3-ast-hierarchy.jar"
    class_files = [path for path in sorted(classes.rglob("*.class"))
                   if not any(part.startswith(".") for part in path.relative_to(classes).parts)]
    jar_args = [str(args.jdk / "bin/jar"), "--create", "--date=2026-10-17T00:00:00Z", "--file", str(jar)]
    for path in class_files:
        name = path.relative_to(classes).as_posix()[:-6].replace("/", ".")
        if not any(name == owner or name.startswith(owner + "$") for owner in sources):
            raise ValueError("unexpected compiled hierarchy owner: " + name)
        jar_args.extend(("-C", str(classes), path.relative_to(classes).as_posix()))
    if not class_files:
        raise ValueError("javac emitted no hierarchy classes")
    subprocess.run(jar_args, check=True)
    if args.stage == "runtime":
        test_sources = directory / "tests"
        for owner, (path, data) in tests.items():
            file = test_sources / (owner.replace(".", "/") + ".java")
            file.parent.mkdir(parents=True, exist_ok=True)
            file.write_bytes(data)
        # Existing test-only failure-injection owner, reused unchanged in both package domains.
        helpers = {}
        for package, label, expected in (
                ("com.synexia.mindex.compiler.ast", "canonical", "79ccef742545a82142ea205b580e7cde384bfbf7d0974e01bae19d721d7093a4"),
                ("com.m3.compiletime.ast", "target", "fd022b757843e340785bec9145db5db23e659be31cec01d278d1506ee2b76f46")):
            resource = HERE.parent.parent / "cache/src/main/resources/com/synexia/rewrite/hash-pinned-java" / ("m3-ast-cache-" + label + "-20261017")
            matches = [row.split("\t") for row in (resource / "manifest.tsv").read_text().splitlines()
                       if row and row.split("\t")[0].endswith("/M3ASTCacheTestSupport.java")]
            if len(matches) != 1 or matches[0][2] != expected:
                raise ValueError("existing cache test helper manifest drift")
            data = (resource / matches[0][3]).read_bytes()
            if sha(data) != expected:
                raise ValueError("existing cache test helper source drift")
            helpers[package + ".M3ASTCacheTestSupport"] = expected
            file = test_sources / (package.replace(".", "/") + "/M3ASTCacheTestSupport.java")
            file.parent.mkdir(parents=True, exist_ok=True)
            file.write_bytes(data)
    if sha(args.base_jar.read_bytes()) != base_sha:
        raise ValueError("base artifact changed during hierarchy compilation")
    receipt = {"stage": args.stage, "base_jar_sha256": base_sha, "jar_sha256": sha(jar.read_bytes()),
               "driver_sha256": sha(Path(__file__).read_bytes()),
               "isolated_compiler_generation": True,
               "sources": {owner: {"path": path, "sha256": sha(data)} for owner, (path, data) in sorted(sources.items())},
               "tests": {owner: {"path": path, "sha256": sha(data)} for owner, (path, data) in sorted(tests.items())}}
    if args.stage == "runtime":
        receipt["existing_test_helpers"] = helpers
    (directory / "COMPILE.json").write_text(json.dumps(receipt, indent=2) + "\n")
    return receipt


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--stage", choices=("bootstrap", "runtime"), required=True)
    parser.add_argument("--jdk", type=Path, default=Path(os.environ["JAVA_HOME"]) if os.environ.get("JAVA_HOME") else None)
    parser.add_argument("--base-jar", type=Path, default=BASE,
                        help="Actual source-qualified frontend artifact; defaults to the sibling packet's build output")
    parser.add_argument("--output-directory", type=Path,
                        help="Publication directory for this finite compile proof; defaults to target/<stage>")
    args = parser.parse_args()
    if args.jdk is None:
        parser.error("--jdk or JAVA_HOME must identify a complete Java 21 JDK")
    args.jdk = args.jdk.resolve()
    args.base_jar = args.base_jar.resolve()
    if not (args.jdk / "bin/javac").is_file() or not args.base_jar.is_file():
        raise ValueError("Java 21 javac and the source-qualified frontend artifact are required")
    sources, tests = qualified_inputs(args.stage)
    publication = (args.output_directory or HERE / "target" / args.stage).resolve()
    publication.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="m3-ast-hierarchy-" + args.stage + "-") as temporary:
        generation = Path(temporary)
        receipt = build_generation(args, generation, sources, tests)
        if args.stage == "runtime":
            publish_tests(generation / "tests", publication / "tests")
        for name in ("m3-ast-hierarchy.jar", "javac.log", "javac-sources.txt", "COMPILE.json"):
            atomic_file(generation / name, publication / name)
    print(json.dumps({"stage": args.stage, "sources": len(sources), "tests": len(tests), "jar_sha256": receipt["jar_sha256"]}))


if __name__ == "__main__":
    main()
