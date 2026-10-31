#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Bounded, offline JMOD/jlink assembler. Upstream inputs are read-only and checksum-pinned."""
import argparse
import hashlib
import json
import platform
import re
import shutil
import subprocess
from pathlib import Path

STAMP = "2026-01-01T00:00:00Z"
FX_MODULES = {"javafx.base", "javafx.graphics", "javafx.controls"}


def digest(path):
    """Streaming byte identity atom."""
    with path.open("rb") as source:
        return hashlib.file_digest(source, "sha256").hexdigest()


def java_home(path):
    """Require an actual Java 21 development toolchain, not the ambient PATH."""
    home = path.resolve(strict=True)
    release = (home / "release").read_text()
    if not re.search(r'^JAVA_VERSION="21(?:[.+"-])', release, re.MULTILINE):
        raise ValueError("Java 21 toolchain required")
    for name in ("java", "javac", "jar", "jmod", "jlink"):
        if not (home / "bin" / name).is_file():
            raise ValueError("Missing JDK tool: " + name)
    return home


def load_fx(lock_path):
    """Validate supplied provenance/target/checksum facts; do not invent or fetch artifacts."""
    lock_path = lock_path.resolve(strict=True)
    lock = json.loads(lock_path.read_text())
    if lock.get("schema_version") != 1 or not str(lock.get("version", "")).startswith("21."):
        raise ValueError("Expected versioned JavaFX 21 lock")
    host_arch = {"amd64": "x86_64", "arm64": "aarch64"}.get(platform.machine(), platform.machine())
    if lock.get("os") != platform.system().lower() or lock.get("arch") != host_arch:
        raise ValueError("JavaFX native platform mismatch")
    artifacts = lock.get("artifacts", [])
    modules = [entry.get("module") for entry in artifacts]
    if len(modules) != 3 or set(modules) != FX_MODULES:
        raise ValueError("Exactly base, graphics and controls are required")
    result = []
    for entry in artifacts:
        checksum = entry.get("sha256", "")
        if not re.fullmatch(r"[0-9a-f]{64}", checksum):
            raise ValueError("Missing reviewed SHA-256")
        if not entry.get("license") or not entry.get("source"):
            raise ValueError("Missing source/license provenance")
        path = (lock_path.parent / entry["file"]).resolve(strict=True)
        if path.name != entry["module"] + ".jmod":
            raise ValueError("JMOD identity mismatch")
        if digest(path) != checksum:
            raise ValueError("JavaFX checksum mismatch: " + path.name)
        result.append((path, checksum))
    return lock, sorted(result)


def command(arguments, log, timeout=180):
    """No shell evaluation; preserve complete tool receipts and fail on nonzero exit."""
    run = subprocess.run([str(value) for value in arguments], text=True,
                         stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=timeout)
    log.write_text(run.stdout)
    if run.returncode:
        raise RuntimeError("Tool failed; see " + str(log))
    return run.stdout


def compile_module(home, source, classes, log, module_path=None):
    """Compile one source module using Java21, with no lint exclusions."""
    classes.mkdir()
    args = [home / "bin/javac", "--release", "21", "-Xlint:all", "-Werror", "-d", classes]
    if module_path is not None:
        args += ["--module-path", module_path]
    command(args + sorted(source.rglob("*.java")), log)


def main(argv=None):
    """Assemble only into a fresh explicit directory; never install into the supplied JDK."""
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--jdk", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    parser.add_argument("--javafx-lock", type=Path)
    args = parser.parse_args(argv)
    home = java_home(args.jdk)
    output = args.out.absolute()
    if output.resolve().is_relative_to(home):
        raise ValueError("Output inside JAVA_HOME is forbidden")
    fx_lock, fx_artifacts = load_fx(args.javafx_lock) if args.javafx_lock else (None, [])
    output.mkdir(parents=True, exist_ok=False)
    log = output / "logs"
    log.mkdir()
    jmods = output / "jmods"
    jmods.mkdir()
    source = Path(__file__).resolve().parent
    classes = output / "tool-classes"
    compile_module(home, source / "kernel/src/main/java", classes, log / "tool-compile.log")
    tool_jar = output / "m3-module-pack.jar"
    command([home / "bin/jar", "--create", "--file", tool_jar, "--date", STAMP,
             "-C", classes, ".", "-C", source / "legal", "."], log / "jar.log")
    command([home / "bin/jmod", "create", "--date", STAMP, "--class-path", tool_jar,
             "--legal-notices", source / "legal", jmods / "com.m3tooling.modulepack.jmod"], log / "tool-jmod.log")
    for path, checksum in fx_artifacts:
        target = jmods / path.name
        shutil.copyfile(path, target)
        if digest(target) != checksum:
            raise ValueError("Input drift during copy: " + str(path))
    import os
    module_path = os.pathsep.join([str(home / "jmods"), str(jmods)])
    smoke_classes = output / "diagnostics-classes"
    compile_module(home, source / "fixtures/diagnostics", smoke_classes,
                   log / "diagnostics-compile.log")
    command([home / "bin/jmod", "create", "--date", STAMP, "--class-path", smoke_classes,
             "--legal-notices", source / "legal", jmods / "com.m3pack.diagnostics.jmod"], log / "diagnostics-jmod.log")
    roots = ["com.m3tooling.modulepack", "com.m3pack.diagnostics", "jdk.jcmd", "jdk.jdeps"]
    if fx_lock:
        fx_classes = output / "javafx-classes"
        compile_module(home, source / "fixtures/javafx", fx_classes,
                       log / "javafx-compile.log", module_path)
        command([home / "bin/jmod", "create", "--date", STAMP, "--class-path", fx_classes,
                 "--legal-notices", source / "legal", jmods / "com.m3pack.fx.jmod"], log / "javafx-jmod.log")
        roots.append("com.m3pack.fx")
    inventory = [home / "bin/java", "-cp", classes, "com.m3.tools.modulepack.M3ModuleInventory",
                 ",".join(roots), home / "jmods", jmods]
    first = command(inventory, output / "MODULES.tsv")
    if command(inventory, log / "inventory-replay.tsv") != first:
        raise ValueError("Inventory fixed-point mismatch")
    if fx_lock:
        observed = {line.split("\t")[0]: line.split("\t")[1] for line in first.splitlines()[1:]}
        for name in FX_MODULES:
            if observed.get(name) != fx_lock["version"]:
                raise ValueError("Locked JavaFX descriptor version mismatch")
    for archive in sorted(jmods.glob("*.jmod")):
        command([home / "bin/jmod", "describe", archive], log / (archive.stem + ".describe.log"))
    image = output / "image"
    command([home / "bin/jlink", "--module-path", module_path, "--add-modules", ",".join(roots),
             "--strip-debug", "--no-header-files", "--no-man-pages", "--output", image],
            log / "jlink.log")
    command([image / "bin/java", "--list-modules"], log / "image-modules.log")
    smoke = command([image / "bin/java", "-Xcheck:jni", "-m",
                     "com.m3pack.diagnostics/com.m3.pack.diagnostics.M3DiagnosticsSmoke",
                     output / "probe.jfr"], log / "diagnostics-run.log")
    if "M3_DIAGNOSTICS_PASS" not in smoke:
        raise ValueError("Missing diagnostic proof")
    if fx_lock:
        fx = command([image / "bin/java", "-Xcheck:jni", "-m",
                      "com.m3pack.fx/com.m3.pack.fx.M3FxSmoke"], log / "javafx-run.log", 60)
        if "M3_JAVAFX_PASS" not in fx:
            raise ValueError("Missing JavaFX native/toolkit proof")
    release = (home / "release").read_text()
    (output / "JDK_RELEASE.txt").write_text(release)
    receipt = {"status": "LOCAL_IMAGE_PROVEN", "jdk_release_sha256": digest(home / "release"),
               "javafx": fx_lock, "roots": roots, "m3jdk21_product_acceptance": False,
               "maven_junit_jacoco": "NOT_EXECUTED_BY_THIS_SCRIPT"}
    (output / "RECEIPT.json").write_text(json.dumps(receipt, indent=2, sort_keys=True) + "\n")
    files = sorted(path for path in image.rglob("*") if path.is_file())
    (output / "IMAGE_SHA256.tsv").write_text("sha256\tpath\n" + "".join(
        digest(path) + "\t" + path.relative_to(image).as_posix() + "\n" for path in files))
    print("M3_MODULE_PACK_PASS", output)


if __name__ == "__main__":
    main()
