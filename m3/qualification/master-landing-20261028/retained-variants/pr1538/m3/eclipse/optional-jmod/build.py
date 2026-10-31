#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Build the optional Eclipse core JMOD from real M3 owners without copying sources.

All outputs are staged outside source. Pre-existing M3 modules provide their own
implementation classes and descriptors; the AST overlay provides its descriptor.
A complete verify runs the JPMS ServiceLoader/NIO/AST/index/text oracle in both
the module path and linked image. --native-collections additionally packages the
existing checked JNI selector and proves resource-mask selection inside the image.
"""
from __future__ import annotations

import argparse
import hashlib
import os
from pathlib import Path
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[3]
HERE = Path(__file__).resolve().parent
DATE = "2026-10-18T00:00:00Z"
MODULES = (
    ("com.m3.text", ROOT / "m3/core/src", None),
    ("com.m3.arrays", ROOT / "m3/arrays/src/main/java", None),
    ("com.m3.ast", ROOT / "m3/ast/src/main/java", HERE / "module-overlays/com.m3.ast/src/main/java/module-info.java"),
    ("com.m3.collections", ROOT / "m3/collections/src/main/java", None),
    ("com.m3.file", ROOT / "m3/file/src/main/java", None),
    ("com.m3.eclipse.core", HERE / "src/main/java", None),
)


def run(*args: object) -> None:
    command = [str(x) for x in args]
    print("+", " ".join(command), flush=True)
    subprocess.run(command, check=True)


def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def stage_module(javac: Path, out: Path, name: str, source: Path, overlay: Path | None) -> Path:
    files = sorted(source.rglob("*.java"))
    module_files = [path for path in files if path.name == "module-info.java"]
    if overlay is not None:
        if module_files:
            raise RuntimeError(f"{name}: native descriptor appeared; review before using overlay")
        if not overlay.is_file():
            raise RuntimeError(f"{name}: missing reviewed descriptor")
        files.append(overlay)
    elif len(module_files) != 1:
        raise RuntimeError(f"{name}: requires exactly one canonical module descriptor")
    if not files:
        raise RuntimeError(f"{name}: no source files")
    folder = out / "classes" / name
    folder.mkdir(parents=True, exist_ok=False)
    cmd = [str(javac), "--release", "21", "-encoding", "UTF-8",
           "-Xlint:all,-module", "-Werror", "--module-path", str(out / "classes"),
           "-h", str(out / "headers"), "-d", str(folder)]
    run(*cmd, *files)
    if not (folder / "module-info.class").is_file():
        raise RuntimeError(f"{name}: no compiled module descriptor")
    return folder


def native_collections(jdk: Path, out: Path) -> Path:
    """Compile the existing native owner against freshly generated JDK21 headers."""
    if not sys.platform.startswith("linux"):
        raise RuntimeError("native collection JMOD qualification currently requires Linux")
    destination = out / "native"
    destination.mkdir()
    source = ROOT / "m3/collections/src/main/native/collections"
    cc = os.environ.get("CC", "cc")
    common = [cc, "-std=c17", "-Wall", "-Wextra", "-Werror", "-O2", "-fPIC",
              "-I" + str(jdk / "include"), "-I" + str(jdk / "include/linux"),
              "-I" + str(out / "headers")]
    objects = []
    for stem, owner in (("m3_index_mask_compactor", "M3IndexMaskCompactor"),
                        ("m3_index_mask_window", "M3IndexMaskWindow")):
        obj = destination / (stem + ".o")
        run(*common, "-include", out / "headers" / ("com_m3_collections_" + owner + ".h"),
            "-c", source / (stem + ".c"), "-o", obj)
        objects.append(obj)
    library = destination / "libm3_collections_mask.so"
    run(*common, "-shared", *objects, "-o", library)
    # JMOD receives only the runtime shared library, never object files or test fixtures.
    libs = out / "native-libs"
    libs.mkdir()
    shutil.copyfile(library, libs / library.name)
    return libs


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", choices=("build", "verify"))
    parser.add_argument("--out", type=Path, default=HERE / "target" / "optional-jmod")
    parser.add_argument("--jdk", type=Path, default=Path(os.environ.get("JAVA_HOME", "/usr/lib/jvm/default-java")))
    parser.add_argument("--native-collections", action="store_true",
                        help="build and qualify existing collection JNI in the linked image (Linux)")
    args = parser.parse_args()
    jdk = args.jdk.resolve()
    bin_dir = jdk / "bin"
    javac, java, jmod, jlink = (bin_dir / x for x in ("javac", "java", "jmod", "jlink"))
    for tool in (javac, java, jmod, jlink):
        if not tool.is_file():
            raise RuntimeError(f"JDK21 tool missing: {tool}")
    out = args.out.resolve()
    if out.exists():
        raise FileExistsError(f"Refusing occupied output destination: {out}")
    out.mkdir(parents=True)
    (out / "classes").mkdir()
    (out / "jmods").mkdir()
    (out / "headers").mkdir()
    receipts = ["module\tsource_sha256\tjmod_sha256\tsize_bytes\tverified"]
    for name, source, overlay in MODULES:
        folder = stage_module(javac, out, name, source, overlay)
        out_jmod = out / "jmods" / (name + ".jmod")
        legal = out / "legal" / name
        legal.mkdir(parents=True)
        license_file = ROOT / "m3/LICENSE"
        notice_file = ROOT / ("m3/collections/NOTICE" if name == "com.m3.collections" else "m3/NOTICE")
        for source_file in (license_file, notice_file):
            if not source_file.is_file():
                raise RuntimeError(f"missing canonical legal notice: {source_file}")
            shutil.copyfile(source_file, legal / source_file.name)
        options = []
        if name == "com.m3.collections" and args.native_collections:
            options = ["--libs", native_collections(jdk, out)]
        run(jmod, "create", "--date", DATE, "--class-path", folder,
            "--legal-notices", legal, *options, out_jmod)
        run(jmod, "describe", out_jmod)
        sources = sorted(source.rglob("*.java"))
        if overlay is not None:
            sources.append(overlay)
        source_digest = hashlib.sha256()
        for path in sources:
            source_digest.update(str(path.relative_to(ROOT)).encode("utf-8"))
            source_digest.update(b"\0")
            source_digest.update(bytes.fromhex(sha(path)))
        second = out / (name + "-replay.jmod")
        run(jmod, "create", "--date", DATE, "--class-path", folder,
            "--legal-notices", legal, *options, second)
        if sha(second) != sha(out_jmod):
            raise RuntimeError(f"{name}: deterministic JMOD replay mismatch")
        second.unlink()
        receipts.append("\t".join((name, source_digest.hexdigest(),
                                   sha(out_jmod), str(out_jmod.stat().st_size), "JMOD_REPLAY_IDENTICAL")))

    if args.command == "verify":
        test_sources = [HERE / "src/test/java/com/m3/eclipse/smoke" / name for name in
                        ("M3EclipseCoreSelfTest.java", "M3ModuleImageSelfTest.java")]
        tests = out / "test-classes"
        tests.mkdir()
        run(javac, "--release", "21", "-encoding", "UTF-8", "-Xlint:all,-module", "-Werror",
            "--module-path", out / "classes", "--add-modules", "com.m3.eclipse.core",
            "-d", tests, *test_sources)
        run(java, "--module-path", out / "classes", "--add-modules", "com.m3.eclipse.core",
            "--class-path", tests, "com.m3.eclipse.smoke.M3EclipseCoreSelfTest")
        image = out / "image"
        run(jlink, "--module-path", os.pathsep.join((str(jdk / "jmods"), str(out / "jmods"))),
            "--add-modules", "com.m3.eclipse.core", "--output", image,
            "--no-header-files", "--no-man-pages")
        run(image / "bin" / "java", "--list-modules")
        run(image / "bin" / "java", "--add-modules", "com.m3.eclipse.core",
            "--class-path", tests, "com.m3.eclipse.smoke.M3EclipseCoreSelfTest")
        run(image / "bin" / "java", "-Xcheck:jni", "-Djava.library.path=" + str(image / "lib"),
            "--add-modules", "com.m3.eclipse.core", "--class-path", tests,
            "com.m3.eclipse.smoke.M3ModuleImageSelfTest",
            "native" if args.native_collections else "java")
        if args.native_collections:
            linked = image / "lib/libm3_collections_mask.so"
            if sha(linked) != sha(out / "native-libs/libm3_collections_mask.so"):
                raise RuntimeError("linked native library differs from the compiled owner")
    (out / "JMOD_BUILD.tsv").write_text("\n".join(receipts) + "\n", encoding="utf-8")
    print("M3_ECLIPSE_CORE_JMOD_" + ("VERIFIED" if args.command == "verify" else "BUILT"))


if __name__ == "__main__":
    try:
        main()
    except (OSError, RuntimeError, subprocess.CalledProcessError) as error:
        print("M3_ECLIPSE_CORE_JMOD_FAILED:", error, file=sys.stderr)
        sys.exit(1)
