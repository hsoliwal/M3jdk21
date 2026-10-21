#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Compile three opt-in text modules from an M3 JDK 21 development image."""
import argparse
import os
import platform
import re
import subprocess
import sys
from pathlib import Path


def command(log, *args):
    result = subprocess.run([str(a) for a in args], stdout=subprocess.PIPE,
                            stderr=subprocess.STDOUT, text=True, timeout=120)
    log.write_text(result.stdout, encoding="utf-8")
    if result.returncode:
        raise RuntimeError("command failed: " + str(log))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for option in ("jdk-home", "sources", "native-source", "smoke", "packager", "legal-notices", "out"):
        parser.add_argument("--" + option, type=Path, required=True)
    args = parser.parse_args()
    home = args.jdk_home.resolve(strict=True)
    if not re.search(r'^JAVA_VERSION="21(?:[.+"-])', (home / "release").read_text(), re.MULTILINE):
        raise ValueError("M3 Java 21 development toolchain required")
    if platform.system() != "Linux":
        raise ValueError("this POSIX JNI image qualification currently requires Linux")
    output = args.out.resolve()
    if output.is_relative_to(home):
        raise ValueError("output must be outside the supplied JDK")
    if output.exists():
        raise FileExistsError("fresh output directory required: " + str(output))
    sources = args.sources.resolve(strict=True)
    native_source = args.native_source.resolve(strict=True)
    smoke = args.smoke.resolve(strict=True)
    packager = args.packager.resolve(strict=True)
    output.mkdir(parents=True)
    classes, native, jars = (output / name for name in ("modules", "native", "jars"))
    for path in (classes, native, jars):
        path.mkdir()
    command(output / "m3-progress-api.log", home / "bin/javap", "-public",
            "java.util.concurrent.ProgressMonitor", "java.util.concurrent.ProgressMonitors")
    # M3File uses the M3 java.base progress APIs; standard ct.sym omits them.
    command(output / "modules-javac.log", home / "bin/javac", "-source", "21", "-target", "21", "-Xlint:all,-module",
            "-Werror", "--module-source-path", sources, "-d", classes, *sorted(sources.rglob("*.java")))
    command(output / "native.log", os.environ.get("CC", "cc"), "-std=c11", "-Wall", "-Wextra", "-Werror",
            "-shared", "-fPIC", "-I" + str(home / "include"), "-I" + str(home / "include/linux"),
            "-o", native / "libm3textio.so", native_source)
    rows = ["artifact\tversion\tjar\tmodule\trequires\tdisposition"]
    for module in ("jdk.m3.text", "jdk.m3.io", "jdk.m3.structured"):
        jar = jars / (module + ".jar")
        command(output / (module + "-jar.log"), home / "bin/jar", "--create", "--file", jar,
                "--date", "2026-01-01T00:00:00Z", "-C", classes / module, ".")
        rows.append("\t".join((module, "1", str(jar), module,
                              "java.base" if module == "jdk.m3.text" else "jdk.m3.text", "JMOD")))
    image_tsv, manifest = output / "INPUT_JMOD_IMAGE.tsv", output / "EMPTY_DESCRIPTOR_MANIFEST.tsv"
    image_tsv.write_text("\n".join(rows) + "\n")
    manifest.write_text("module\tartifacts\tunit_jars\ttarget\tsha256\tgenerated\n")
    command(output / "jmod-pack.log", sys.executable, packager, "--jdk-home", home,
            "--image-tsv", image_tsv, "--manifest", manifest, "--layer-root", sources,
            "--out", output / "pack", "--legal-notices", args.legal_notices.resolve(strict=True), "--native-library", "jdk.m3.io=" + str(native),
            "--add-modules", "java.se,jdk.compiler,jdk.jlink,jdk.jartool,jdk.jdeps,jdk.jpackage")
    image = output / "pack/image"
    for tool in ("java", "javac", "jar", "jmod", "jlink", "jdeps", "jpackage"):
        if not os.access(image / "bin" / tool, os.X_OK):
            raise ValueError("missing developer tool: " + tool)
    smoke_classes = output / "smoke"
    smoke_classes.mkdir()
    # --release 21 selects standard ct.sym APIs, excluding the new SDK modules.
    command(output / "image-javac.log", image / "bin/javac", "-source", "21", "-target", "21",
            "-Xlint:all", "-Werror", "--add-modules", "jdk.m3.text,jdk.m3.io,jdk.m3.structured",
            "-d", smoke_classes, smoke)
    command(output / "image-runtime.log", image / "bin/java", "-Xcheck:jni", "--add-modules",
            "jdk.m3.text,jdk.m3.io", "-cp", smoke_classes, "M3TextSmoke", output / "image-files")
    if "M3_TEXT_IMAGE_PASS tools=24" not in (output / "image-runtime.log").read_text():
        raise ValueError("missing image runtime proof")
    print("M3_TEXT_PACK_PASS modules=3 tools=24")


if __name__ == "__main__":
    main()
