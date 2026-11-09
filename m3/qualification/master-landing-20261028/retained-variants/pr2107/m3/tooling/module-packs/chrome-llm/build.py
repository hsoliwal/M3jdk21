#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Build and exercise the received jdk.m3.llm module using explicit Java 21 tools.

This target-only build never transforms product source or downloads dependencies.
The optional progress patch is a test fixture, never installed into the linked image.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess

MODULE = "jdk.m3.llm"
STAMP = "2026-01-01T00:00:00Z"


def sha256(path: Path) -> str:
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def command(arguments: list, log: Path, records: list, timeout: int = 180) -> str:
    args = [str(arg) for arg in arguments]
    result = subprocess.run(args, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                            text=True, timeout=timeout, check=False)
    log.write_text(result.stdout, encoding="utf-8")
    records.append({"command": args, "exit": result.returncode, "log": log.name})
    if result.returncode:
        raise RuntimeError("command failed; see " + str(log))
    return result.stdout


def directory_inventory(root: Path) -> list[dict]:
    return [{"path": path.relative_to(root).as_posix(), "sha256": sha256(path),
             "bytes": path.stat().st_size}
            for path in sorted(root.rglob("*")) if path.is_file()]


def copy_resources(source: Path, destination: Path) -> None:
    if not source.is_dir():
        return
    for resource in sorted(source.rglob("*")):
        if resource.is_file():
            target = destination / resource.relative_to(source)
            if target.exists():
                raise ValueError("resource would overwrite compiled output: " + str(target))
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(resource, target)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--jdk-home", required=True, type=Path)
    parser.add_argument("--out", required=True, type=Path)
    parser.add_argument("--project", type=Path, default=Path(__file__).resolve().parent)
    parser.add_argument("--progress-patch", type=Path,
                        help="Explicit test-only directory containing the two real M3 progress API sources")
    parser.add_argument("--native-library", action="append", default=[], type=Path,
                        help="Already compiled target JNI library to include in the module's JMOD")
    parser.add_argument("--node", type=Path,
                        help="Explicit Node executable for browser resource mock tests; never live Chrome")
    parser.add_argument("--test-main", action="append", default=[],
                        help="Additional dependency-free test main class, repeated as needed")
    parser.add_argument("--test-arg", action="append", default=[], metavar="MAIN=ARG",
                        help="Positional argument for a selected test main; repeat to preserve argument order")
    args = parser.parse_args()
    home = args.jdk_home.resolve(strict=True)
    release = home / "release"
    if not re.search(r'^JAVA_VERSION="21(?:[.+"-])', release.read_text(), re.MULTILINE):
        raise ValueError("explicit Java 21 development toolchain required")
    for tool in ("java", "javac", "jar", "jmod", "jlink", "jdeps", "javap"):
        if not os.access(home / "bin" / tool, os.X_OK):
            raise ValueError("missing development tool: " + tool)
    project = args.project.resolve(strict=True)
    source = project / "src/main/java"
    resources = project / "src/main/resources"
    tests = project / "src/test/java"
    test_resources = project / "src/test/resources"
    legal = project / "legal"
    java_files = sorted(source.rglob("*.java"))
    if not (source / "module-info.java").is_file() or not java_files:
        raise ValueError("received named-module sources are missing")
    if not (tests / "ModuleSmoke.java").is_file():
        raise ValueError("target module smoke test is missing")
    if not legal.is_dir() or not any(legal.iterdir()):
        raise ValueError("legal notices are required")
    output = args.out.resolve()
    if output == project or any(output.is_relative_to(root) for root in
                                (source, resources, tests, test_resources, legal, home)):
        raise ValueError("output must not overwrite the toolchain or received source")
    if output.exists():
        raise FileExistsError("fresh output directory required: " + str(output))
    native_inputs = [p.resolve(strict=True) for p in args.native_library]
    if any(not p.is_file() for p in native_inputs):
        raise ValueError("native inputs must be files")
    if len({p.name for p in native_inputs}) != len(native_inputs):
        raise ValueError("duplicate native library filename")
    test_mains = ["ModuleSmoke", *args.test_main]
    if len(set(test_mains)) != len(test_mains) or any(
            not re.fullmatch(r"[A-Za-z_$][\w$]*(?:\.[A-Za-z_$][\w$]*)*", n)
            for n in test_mains):
        raise ValueError("invalid or duplicate test main")
    test_arguments = {name: [] for name in test_mains}
    for argument in args.test_arg:
        main_class, separator, value = argument.partition("=")
        if not separator or main_class not in test_arguments:
            raise ValueError("--test-arg requires an explicitly selected MAIN=ARG")
        test_arguments[main_class].append(value)
    output.mkdir(parents=True)
    logs = output / "logs"
    logs.mkdir()
    records: list[dict] = []
    receipt = {
        "schema": "M3_LLM_MODULE_PROOF_V1", "status": "STARTED",
        "module": MODULE, "jdk_home": str(home),
        "jdk_release_sha256": sha256(release),
        "progress_mode": "PATCHED_STOCK_FIXTURE" if args.progress_patch else "SUPPLIED_IMAGE",
        "full_m3jdk21_product_build_jtreg": "NOT_RUN",
        "live_chrome_inference": "NOT_RUN", "commands": records,
        "source_files": directory_inventory(source),
        "resource_files": directory_inventory(resources) if resources.is_dir() else [],
        "test_resource_files": directory_inventory(test_resources) if test_resources.is_dir() else [],
        "native_inputs": [{"file": p.name, "sha256": sha256(p)} for p in native_inputs],
    }
    try:
        command([home / "bin/java", "-version"], logs / "java-version.log", records)
        command([home / "bin/javac", "-version"], logs / "javac-version.log", records)
        patch_args: list[str] = []
        if args.progress_patch:
            patch_source = args.progress_patch.resolve(strict=True)
            patch_java = sorted(patch_source.rglob("*.java"))
            if len(patch_java) != 2 or {p.name for p in patch_java} != {
                    "ProgressMonitor.java", "ProgressMonitors.java"}:
                raise ValueError("progress fixture must contain exactly the two real progress API sources")
            patch_classes = output / "progress-fixture"
            patch_classes.mkdir()
            command([home / "bin/javac", "-source", "21", "-target", "21",
                     "-Xlint:all", "-Werror", "--patch-module", "java.base=" + str(patch_source),
                     "-d", patch_classes, *patch_java], logs / "progress-fixture-compile.log", records)
            patch_args = ["--patch-module", "java.base=" + str(patch_classes)]
            receipt["progress_fixture"] = directory_inventory(patch_source)
        classes = output / "classes"
        classes.mkdir()
        command([home / "bin/javac", "-source", "21", "-target", "21",
                 "-encoding", "UTF-8", "-Xlint:all", "-Werror", *patch_args,
                 "-d", classes, *java_files], logs / "module-compile.log", records)
        copy_resources(resources, classes)
        jar = output / (MODULE + ".jar")
        command([home / "bin/jar", "--create", "--file", jar, "--date", STAMP,
                 "-C", classes, "."], logs / "jar-create.log", records)
        command([home / "bin/jar", "--describe-module", "--file", jar],
                logs / "jar-module.log", records)
        native = output / "native"
        if native_inputs:
            native.mkdir()
            for library in native_inputs:
                copied = native / library.name
                shutil.copyfile(library, copied)
                if sha256(copied) != sha256(library):
                    raise ValueError("native input changed during copy")
        jmods = output / "jmods"
        jmods.mkdir()
        jmod_args = [home / "bin/jmod", "create", "--date", STAMP,
                     "--class-path", classes, "--legal-notices", legal]
        if native_inputs:
            jmod_args += ["--libs", native]
        archive = jmods / (MODULE + ".jmod")
        command([*jmod_args, archive], logs / "jmod-create.log", records)
        command([home / "bin/jmod", "describe", archive], logs / "jmod-describe.log", records)
        image = output / "image"
        module_path = os.pathsep.join((str(home / "jmods"), str(jmods)))
        roots = MODULE + ",jdk.compiler,jdk.jartool,jdk.jlink,jdk.jdeps"
        command([home / "bin/jlink", "--module-path", module_path, "--add-modules", roots,
                 "--strip-debug", "--no-man-pages", "--output", image], logs / "jlink.log", records)
        modules = command([image / "bin/java", "--list-modules"], logs / "image-modules.log", records)
        if not any(line.split("@", 1)[0] == MODULE for line in modules.splitlines()):
            raise ValueError("linked image is missing the received module")
        test_classes = output / "test-classes"
        test_classes.mkdir()
        # Existing facet oracles exercise package-private invariants. Keep those
        # tests inside a separate module patch instead of exporting internals.
        package_pattern = re.compile(r"^package\s+([\w.]+)\s*;", re.MULTILINE)
        def package_of(path):
            match = package_pattern.search(path.read_text(encoding="utf-8"))
            return match.group(1) if match else ""
        runtime_packages = {package_of(path) for path in java_files} - {""}
        module_tests = [p for p in sorted(tests.rglob("*.java"))
                        if package_of(p) in runtime_packages]
        external_tests = [p for p in sorted(tests.rglob("*.java"))
                          if package_of(p) not in runtime_packages]
        module_test_names = {package_of(p) + "." + p.stem for p in module_tests}
        test_patch = output / "module-test-classes"
        if module_tests:
            test_patch.mkdir()
            command([image / "bin/javac", "-source", "21", "-target", "21",
                     "-encoding", "UTF-8", "-Xlint:all", "-Werror", *patch_args,
                     "--patch-module", MODULE + "=" + str(tests),
                     "-d", test_patch, *module_tests],
                    logs / "image-module-tests-compile.log", records)
        command([image / "bin/javac", "-source", "21", "-target", "21",
                 "-encoding", "UTF-8", "-Xlint:all", "-Werror", *patch_args,
                 "--add-modules", MODULE, "-d", test_classes, *external_tests],
                logs / "image-tests-compile.log", records)
        copy_resources(test_resources, test_classes)
        for index, main_class in enumerate(test_mains):
            launcher = (["--patch-module", MODULE + "=" + str(test_patch),
                         "--module", MODULE + "/" + main_class]
                        if main_class in module_test_names else
                        ["--add-modules", MODULE, "-cp", test_classes, main_class])
            result = command([image / "bin/java", "-Xcheck:jni", *patch_args,
                              *launcher, *test_arguments[main_class]],
                             logs / ("image-test-" + str(index) + ".log"), records)
            if main_class == "ModuleSmoke" and "M3_LLM_MODULE_PASS" not in result:
                raise ValueError("missing named-module proof marker")
        receipt["status"] = "LOCAL_MODULE_IMAGE_PROVEN"
        receipt["jar_sha256"] = sha256(jar)
        receipt["jmod_sha256"] = sha256(archive)
        receipt["test_mains"] = test_mains
        receipt["test_arguments"] = test_arguments
        if args.node:
            node = args.node.resolve(strict=True)
            if not os.access(node, os.X_OK):
                raise ValueError("--node must be an executable")
            browser_tests = output / "browser-tests"
            browser_tests.mkdir()
            script_path = Path("jdk/m3/llm/browser")
            copy_resources(resources / script_path, browser_tests)
            copy_resources(test_resources / script_path, browser_tests)
            scripts = sorted(browser_tests.glob("*.test.mjs"))
            if len(scripts) != 4:
                raise ValueError("expected the four received browser regression suites")
            command([node, "--version"], logs / "node-version.log", records)
            for script in scripts:
                command([node, script], logs / (script.stem + ".log"), records, timeout=60)
            receipt["browser_mock_tests"] = "PASS"
        else:
            receipt["browser_mock_tests"] = "NOT_RUN"
        receipt["image_inventory"] = directory_inventory(image)
        print("M3_LLM_PACK_PASS module=" + MODULE + " progress=" + receipt["progress_mode"])
    except Exception as failure:
        receipt["status"] = "FAILED"
        receipt["failure"] = str(failure)
        raise
    finally:
        (output / "RECEIPT.json").write_text(json.dumps(receipt, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
