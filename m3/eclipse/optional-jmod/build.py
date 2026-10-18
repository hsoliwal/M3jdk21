#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Build the optional Eclipse core JMOD from real M3 owners without copying sources.

All outputs are staged outside source. Pre-existing M3 modules provide their own
implementation classes; AST/collections overlays provide only module descriptors.
A complete verify also runs the JPMS ServiceLoader/NIO/AST/index/text oracle.
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
    ("com.m3.collections", ROOT / "m3/collections/src/main/java", HERE / "module-overlays/com.m3.collections/src/main/java/module-info.java"),
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
           "-d", str(folder)]
    run(*cmd, *files)
    if not (folder / "module-info.class").is_file():
        raise RuntimeError(f"{name}: no compiled module descriptor")
    return folder


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", choices=("build", "verify"))
    parser.add_argument("--out", type=Path, default=HERE / "target" / "optional-jmod")
    parser.add_argument("--jdk", type=Path, default=Path(os.environ.get("JAVA_HOME", "/usr/lib/jvm/default-java")))
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
        run(jmod, "create", "--date", DATE, "--class-path", folder,
            "--legal-notices", legal, out_jmod)
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
            "--legal-notices", legal, second)
        if sha(second) != sha(out_jmod):
            raise RuntimeError(f"{name}: deterministic JMOD replay mismatch")
        second.unlink()
        receipts.append("\t".join((name, source_digest.hexdigest(),
                                   sha(out_jmod), str(out_jmod.stat().st_size), "JMOD_REPLAY_IDENTICAL")))

    if args.command == "verify":
        test_source = HERE / "src/test/java/com/m3/eclipse/smoke/M3EclipseCoreSelfTest.java"
        tests = out / "test-classes"
        tests.mkdir()
        run(javac, "--release", "21", "-encoding", "UTF-8", "-Xlint:all,-module", "-Werror",
            "--module-path", out / "classes", "--add-modules", "com.m3.eclipse.core",
            "-d", tests, test_source)
        run(java, "--module-path", out / "classes", "--add-modules", "com.m3.eclipse.core",
            "--class-path", tests, "com.m3.eclipse.smoke.M3EclipseCoreSelfTest")
        image = out / "image"
        run(jlink, "--module-path", os.pathsep.join((str(jdk / "jmods"), str(out / "jmods"))),
            "--add-modules", "com.m3.eclipse.core", "--output", image,
            "--no-header-files", "--no-man-pages")
        run(image / "bin" / "java", "--list-modules")
    (out / "JMOD_BUILD.tsv").write_text("\n".join(receipts) + "\n", encoding="utf-8")
    print("M3_ECLIPSE_CORE_JMOD_" + ("VERIFIED" if args.command == "verify" else "BUILT"))


if __name__ == "__main__":
    try:
        main()
    except (OSError, RuntimeError, subprocess.CalledProcessError) as error:
        print("M3_ECLIPSE_CORE_JMOD_FAILED:", error, file=sys.stderr)
        sys.exit(1)
