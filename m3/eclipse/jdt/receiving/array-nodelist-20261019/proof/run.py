#!/usr/bin/env python3
"""Compile and run the finite real-JDT ingress proof against named M3 modules.

Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
This runner uses existing source-built artifacts; it does not transform source.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import zipfile


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def tree_sha256(path: Path) -> str:
    if path.is_file():
        return sha256(path)
    digest = hashlib.sha256()
    for item in sorted(path.rglob("*")):
        if item.is_file():
            digest.update(item.relative_to(path).as_posix().encode("utf-8"))
            digest.update(b"\0")
            digest.update(bytes.fromhex(sha256(item)))
    return digest.hexdigest()


def invoke(command: list[str], log: Path) -> subprocess.CompletedProcess[str]:
    result = subprocess.run(command, capture_output=True, text=True, check=False)
    log.write_text(result.stdout + result.stderr, encoding="utf-8")
    print(result.stdout + result.stderr, end="", flush=True)
    return result


def main() -> None:
    arguments = argparse.ArgumentParser(description=__doc__)
    arguments.add_argument("--compiler-home", required=True, type=Path)
    arguments.add_argument("--runtime-home", required=True, type=Path)
    arguments.add_argument("--compiler-module-path", type=Path)
    arguments.add_argument("--runtime-module-path", type=Path)
    arguments.add_argument("--jdt-build", required=True, type=Path)
    arguments.add_argument("--dependency-dir", required=True, type=Path)
    arguments.add_argument("--output", required=True, type=Path)
    args = arguments.parse_args()
    proof = Path(__file__).resolve().parent
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=True)
    classes = output / "classes"
    classes.mkdir(exist_ok=True)
    source = proof / "src/main/java/org/eclipse/jdt/core/tests/dom/M3JdtIngressProof.java"
    core = args.jdt_build.resolve() / "jdt-exact-core-source/jdt-exact-core-source-20261019.jar"
    compiler = args.jdt_build.resolve() / "jdt-exact-compiler-source/jdt-exact-compiler-source-20261019.jar"
    dependencies = sorted(args.dependency_dir.resolve().glob("*.jar"))
    if not dependencies:
        raise SystemExit("the source-build dependency directory has no JAR files")
    for dependency in dependencies:
        with zipfile.ZipFile(dependency) as archive:
            for owner in ("org/eclipse/jdt/core/dom/ASTParser.class",
                          "org/eclipse/jdt/internal/compiler/tool/EclipseCompiler.class"):
                if owner in archive.namelist():
                    raise SystemExit("released/duplicate JDT owner in dependencies: " + str(dependency))
    classpath = os.pathsep.join(str(path) for path in [core, compiler, *dependencies])
    compile_command = [str(args.compiler_home.resolve() / "bin/javac"), "--release", "21"]
    if args.compiler_module_path:
        compile_command += ["--module-path", str(args.compiler_module_path.resolve())]
    compile_command += ["--add-modules", "jdk.m3.parser", "-cp", classpath,
                        "-d", str(classes), str(source)]
    java = str(args.runtime_home.resolve() / "bin/java")
    runtime_options = []
    if args.runtime_module_path:
        runtime_options += ["--module-path", str(args.runtime_module_path.resolve())]
    runtime_options += ["--add-modules", "jdk.m3.parser"]
    run_command = [java, *runtime_options, "-cp", str(classes) + os.pathsep + classpath,
                   "org.junit.runner.JUnitCore", "org.eclipse.jdt.core.tests.dom.M3JdtIngressProof"]
    compiled = invoke(compile_command, output / "compile.log")
    if compiled.returncode:
        raise SystemExit(compiled.returncode)
    version = invoke([java, "-version"], output / "runtime-version.log")
    descriptor = invoke([java, *runtime_options, "--describe-module", "jdk.m3.parser"],
                        output / "parser-module.log")
    if version.returncode or descriptor.returncode:
        raise SystemExit("runtime or named parser module could not be inspected")
    executed = invoke(run_command, output / "tests.log")
    passed = executed.returncode == 0 and "OK (7 tests)" in executed.stdout
    modules = args.runtime_home.resolve() / "lib/modules"
    receipt = {
        "scope": "real source-built JDT DOM to existing M3ASTIngress.eclipseJdt",
        "pass": passed,
        "tests": 7,
        "sourceSha256": sha256(source),
        "coreJar": {"path": str(core), "sha256": sha256(core)},
        "compilerJar": {"path": str(compiler), "sha256": sha256(compiler)},
        "dependencies": [{"name": path.name, "sha256": sha256(path)} for path in dependencies],
        "compilerModulePathSha256": tree_sha256(args.compiler_module_path.resolve())
            if args.compiler_module_path else None,
        "runtimeModuleMode": "external named module" if args.runtime_module_path else "linked runtime image",
        "runtimeModulePathSha256": tree_sha256(args.runtime_module_path.resolve())
            if args.runtime_module_path else None,
        "runtimeModulesSha256": sha256(modules) if modules.is_file() else None,
        "runtimeVersion": (version.stdout + version.stderr).strip(),
        "parserModuleDescriptor": (descriptor.stdout + descriptor.stderr).strip(),
        "compileCommand": compile_command,
        "runCommand": run_command,
        "testOutput": executed.stdout + executed.stderr,
    }
    (output / "receipt.json").write_text(json.dumps(receipt, indent=2) + "\n", encoding="utf-8")
    if not passed:
        raise SystemExit(executed.returncode or 1)


if __name__ == "__main__":
    main()
