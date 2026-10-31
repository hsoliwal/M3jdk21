#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Build the pinned M3 compiler hook on a supplied Java 21 image for bounded integration proof.

This explicit proof mode does not build HotSpot or activate M3String storage. Normal
product builds supply their already built M3JDK. All input executables stay untouched.
"""
import hashlib
import json
import os
from pathlib import Path
import shutil


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def copy_symbols(source_jdk, image):
    source = source_jdk / "lib/ct.sym"
    if not source.is_file():
        raise ValueError("A complete JDK with lib/ct.sym is required for javac --release support")
    shutil.copy2(source, image / "lib/ct.sym")
    return digest(source)


def build(repository, jdk, out, run):
    owner = Path(__file__).resolve().parent
    lock = json.loads((owner / "COMPILER_SOURCES.json").read_text())
    out.mkdir()
    source = out / "src"
    source.mkdir()
    paths = []
    for name, expected in lock.items():
        original = repository / name
        if digest(original) != expected:
            raise ValueError("Compiler source proof input drift: " + name)
        target = source / name.removeprefix("src/jdk.compiler/share/classes/")
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(original, target)
        paths.append(target)
    compiler = out / "compiler"
    run("compiler-extract", [jdk / "bin/jmod", "extract", "--dir", compiler,
                             jdk / "jmods/jdk.compiler.jmod"])
    classes = out / "classes"
    run("compiler-source-compile", [jdk / "bin/javac", "-proc:none", "-Xlint:all", "-Werror",
        "--patch-module", "jdk.compiler=" + os.pathsep.join(map(str, (source, compiler / "classes"))),
        "-d", classes, *paths])
    shutil.copytree(classes, compiler / "classes", dirs_exist_ok=True)
    jmods = out / "jmods"
    jmods.mkdir()
    for original in sorted((jdk / "jmods").glob("*.jmod")):
        if original.stem not in ("java.base", "jdk.compiler"):
            (jmods / original.name).symlink_to(original)
    run("compiler-jmod", [jdk / "bin/jmod", "create", "--class-path", compiler / "classes",
        "--cmds", compiler / "bin", "--legal-notices", compiler / "legal",
        jmods / "jdk.compiler.jmod"])
    # ModuleHashes in java.base cover the original compiler. Rebuild those hashes
    # against the new compiler; do not disable resolution or module validation.
    base = out / "java-base"
    run("base-extract", [jdk / "bin/jmod", "extract", "--dir", base, jdk / "jmods/java.base.jmod"])
    helper = owner / "fixtures/src/test/java/RemoveRecordedModuleHashes.java"
    export = "java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED"
    run("hash-tool-compile", [jdk / "bin/javac", "-Xlint:all", "-Werror", "--add-exports", export,
                              "-d", out / "hash-tool", helper])
    run("discard-stale-compiler-hashes", [jdk / "bin/java", "--add-exports", export,
        "-cp", out / "hash-tool", "RemoveRecordedModuleHashes", base / "classes/module-info.class"])
    command = [jdk / "bin/jmod", "create", "--class-path", base / "classes"]
    for option, directory in (("cmds", "bin"), ("config", "conf"), ("header-files", "include"),
                              ("legal-notices", "legal"), ("libs", "lib"), ("man-pages", "man")):
        if (base / directory).is_dir():
            command.extend(("--" + option, base / directory))
    run("base-repack", [*command, jmods / "java.base.jmod"])
    run("rebuild-all-module-hashes", [jdk / "bin/jmod", "hash", "--module-path", jmods,
                                     "--hash-modules", ".*", jmods / "java.base.jmod"])
    image = out / "image"
    run("compiler-image", [jdk / "bin/jlink", "--module-path", jmods, "--add-modules",
        "java.se,jdk.compiler,jdk.jartool,jdk.jlink,jdk.jdeps,jdk.unsupported,jdk.management,jdk.charsets",
        "--output", image])
    symbols = copy_symbols(jdk, image)
    (image / "jmods").symlink_to(jmods, target_is_directory=True)
    run("compiler-image-module-validation", [image / "bin/java", "--validate-modules"])
    receipt = {
        "schema": "M3_COMPILER_SOURCE_PROOF_1", "source_sha256": lock,
        "input_jdk_release_sha256": digest(jdk / "release"),
        "input_java_base_jmod_sha256": digest(jdk / "jmods/java.base.jmod"),
        "input_jdk_compiler_jmod_sha256": digest(jdk / "jmods/jdk.compiler.jmod"),
        "compiler_jmod_sha256": digest(jmods / "jdk.compiler.jmod"),
        "rehash_java_base_jmod_sha256": digest(jmods / "java.base.jmod"),
        "symbols_sha256": symbols, "image_modules_sha256": digest(image / "lib/modules"),
        "scope": "Actual pinned M3 compiler source, parser hook, and module services on the supplied JVM",
        "whole_jdk_source_build": False, "m3_string_storage_executed": False,
        "module_hashes": "RECOMPUTED_AND_VALIDATED", "input_jdk_modified": False,
    }
    (out / "RESULT.json").write_text(json.dumps(receipt, indent=2, sort_keys=True) + "\n")
    return image
