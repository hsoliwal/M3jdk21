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
    io_native, text_native, structured_native = native / "io", native / "text", native / "structured"
    io_native.mkdir()
    text_native.mkdir()
    structured_native.mkdir()
    headers = output / "headers"  # javac -h: m3structured.c includes jdk_m3_structured_StructuredTextTools.h
    command(output / "m3-progress-api.log", home / "bin/javap", "-public",
            "java.util.concurrent.ProgressMonitor", "java.util.concurrent.ProgressMonitors")
    # M3File uses the M3 java.base progress APIs; standard ct.sym omits them.
    command(output / "modules-javac.log", home / "bin/javac", "-source", "21", "-target", "21", "-Xlint:all,-module",
            "-Werror", "--module-source-path", sources, "-d", classes, "-h", headers, *sorted(sources.rglob("*.java")))
    command(output / "native.log", os.environ.get("CC", "cc"), "-std=c11", "-Wall", "-Wextra", "-Werror",
            "-shared", "-fPIC", "-I" + str(home / "include"), "-I" + str(home / "include/linux"),
            "-o", io_native / "libm3textio.so", native_source)
    command(output / "native-text.log", os.environ.get("CC", "cc"), "-std=c11", "-Wall", "-Wextra", "-Werror",
            "-shared", "-fPIC", "-I" + str(home / "include"), "-I" + str(home / "include/linux"),
            "-o", text_native / "libm3textops.so", native_source.with_name("m3textops.c").resolve(strict=True))
    # Received from com.synexia m3-unix4j-m3 by M3_TEXT_TOOLS_CONVERGE v1 (2026-11-06).
    for lib, target_dir in (("m3unixtext", text_native), ("m3unixaho", text_native), ("m3structured", structured_native)):
        command(output / ("native-" + lib + ".log"), os.environ.get("CC", "cc"), "-std=c11", "-Wall", "-Wextra",
                "-Werror", "-shared", "-fPIC", "-I" + str(home / "include"), "-I" + str(home / "include/linux"),
                *("-I" + str(d) for d in sorted({p.parent for p in headers.rglob("*.h")} | {headers})),
                "-o", target_dir / ("lib" + lib + ".so"), native_source.with_name(lib + ".c").resolve(strict=True))
    rows =["artifact\tversion\tjar\tmodule\trequires\tdisposition"]
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
            "--out", output / "pack", "--legal-notices", args.legal_notices.resolve(strict=True), "--native-library", "jdk.m3.io=" + str(io_native),
            "--native-library", "jdk.m3.text=" + str(text_native),
            "--native-library", "jdk.m3.structured=" + str(structured_native),
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
            "-d", smoke_classes, smoke, smoke.with_name("M3NativeTextDifferential.java").resolve(strict=True),
            smoke.with_name("M3TextKernelRegression.java").resolve(strict=True),
            smoke.with_name("M3OptionalNativeFallbackRegression.java").resolve(strict=True))
    command(output / "image-runtime.log", image / "bin/java", "-Xcheck:jni", "--add-modules",
            "jdk.m3.text,jdk.m3.io", "-cp", smoke_classes, "M3TextSmoke", output / "image-files")
    if "M3_TEXT_IMAGE_PASS tools=24" not in (output / "image-runtime.log").read_text():
        raise ValueError("missing image runtime proof")
    command(output / "image-native-parity.log", image / "bin/java", "-Xcheck:jni",
            "--add-modules", "jdk.m3.text", "-cp", smoke_classes, "M3NativeTextDifferential")
    if "M3_NATIVE_TEXT_PARITY_PASS runs=450" not in (output / "image-native-parity.log").read_text():
        raise ValueError("missing native text parity proof")
    for mode in ("native", "java"):
        log = output / ("image-text-kernels-" + mode + ".log")
        command(log, image / "bin/java", "-Xcheck:jni", "--add-modules", "jdk.m3.text",
                "--add-opens", "jdk.m3.text/jdk.m3.text=ALL-UNNAMED",
                "-Dm3.text.native.disable=" + str(mode == "java").lower(),
                "-cp", smoke_classes, "M3TextKernelRegression", mode)
        if "M3_TEXT_KERNEL_REGRESSION_PASS mode=" + mode not in log.read_text():
            raise ValueError("missing " + mode + " text kernel proof")
    for mode in ("line-load-denied", "literal-property-denied", "literal-load-denied"):
        log = output / ("image-optional-native-" + mode + ".log")
        command(log, image / "bin/java", "-Xcheck:jni", "--add-modules", "jdk.m3.text",
                "--add-opens", "jdk.m3.text/jdk.m3.text=ALL-UNNAMED",
                "-Djava.security.manager=allow", "-Dm3.text.native.disable=false",
                "-cp", smoke_classes, "M3OptionalNativeFallbackRegression", mode)
        if "M3_OPTIONAL_NATIVE_FALLBACK_PASS mode=" + mode not in log.read_text():
            raise ValueError("missing " + mode + " optional native fallback proof")
    print("M3_TEXT_PACK_PASS modules=3 tools=24")


if __name__ == "__main__":
    main()
