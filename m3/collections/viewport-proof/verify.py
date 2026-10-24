#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
"""Qualify the existing viewport owners; never mutate or regenerate their sources."""
from __future__ import annotations

import argparse
import ast
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile

PHASES = ("lint", "compile", "test", "runtime")


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def no_symlinks(path: Path) -> None:
    if any(part.is_symlink() for part in (path, *path.parents)):
        raise ValueError("symlink path refused: " + str(path))


class Proof:
    """Every command is retained; stage inversion or skipping is a hard failure."""
    def __init__(self, output: Path) -> None:
        self.output = output
        self.stage = -1
        self.rows: list[dict] = []

    def advance(self, phase: str) -> None:
        index = PHASES.index(phase)
        if index != self.stage + 1:
            raise RuntimeError("verification stage order violation")
        if self.stage >= 0 and not any(row["phase"] == PHASES[self.stage] for row in self.rows):
            raise RuntimeError("empty verification stage")
        self.stage = index

    def run(self, label: str, args: list[str], *, clean_stderr: bool = False) -> str:
        if self.stage < 0 or not re.fullmatch(r"[a-z0-9-]+", label):
            raise RuntimeError("unadmitted command stage or label")
        stem = f"{len(self.rows):03d}-{label}"
        out, err = (self.output / (stem + suffix) for suffix in (".stdout", ".stderr"))
        row = {"phase": PHASES[self.stage], "command": args, "stdout": out.name, "stderr": err.name}
        try:
            with out.open("xb") as stdout, err.open("xb") as stderr:
                process = subprocess.run(args, stdout=stdout, stderr=stderr, check=False, timeout=120)
            row["exit"] = process.returncode
        except (OSError, subprocess.TimeoutExpired) as failure:
            row["exit"] = -1
            row["error"] = str(failure)
        row.update(stdout_sha256=digest(out), stderr_sha256=digest(err))
        self.rows.append(row)
        (self.output / "COMMANDS.json").write_text(json.dumps(self.rows, indent=2) + "\n")
        if row["exit"] != 0 or (clean_stderr and err.stat().st_size):
            raise RuntimeError("command failed: " + label + "; inspect retained logs")
        return out.read_text()


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, help="fresh absolute evidence directory")
    parser.add_argument("--cc", default=os.environ.get("CC", "cc"))
    args = parser.parse_args()
    root = Path(__file__).resolve().parent.parent
    q = root / "viewport-proof"
    sources = [root / "src/main/c/m3maskviewport.c",
               *[root / "src/main/java/com/m3/collections" / name for name in
                 ("M3MaskViewportKernel.java", "M3MaskViewportNative.java")],
               *[root / "src/test/java/com/m3/collections" / name for name in
                 ("M3MaskViewportKernelContract.java", "M3MaskViewportPageContract.java")],
               q / "CMakeLists.txt", q / "verify.sh", q / "verify.py",
               q / "fixtures/M3FrameRegionContract.txt",
               q / "fixtures/M3NativePageDirectContract.txt"]
    for path in sources:
        no_symlinks(path)
        if not path.is_file():
            raise ValueError("missing source: " + str(path))
    before = {str(path.relative_to(root)): digest(path) for path in sources}
    if sys.platform != "linux":
        raise ValueError("this verifier currently qualifies Linux only")
    for tool in (args.cc, "cmake", "nm", "readelf", "bash"):
        if shutil.which(tool) is None:
            raise ValueError("missing required tool: " + tool)
    javac = Path(os.environ.get("JAVA_HOME", "")) / "bin/javac"
    if not javac.is_file():
        executable = shutil.which("javac")
        if executable is None:
            raise ValueError("Java 21 compiler required")
        javac = Path(executable).resolve()
    javac = javac.resolve()
    java = javac.parent / "java"
    home = javac.parent.parent
    os.environ["JAVA_HOME"] = str(home)
    if args.output is None:
        parent = q / "build"
        no_symlinks(parent)
        parent.mkdir(exist_ok=True)
        output = Path(tempfile.mkdtemp(prefix="run-", dir=parent))
    else:
        output = args.output
        if not output.is_absolute():
            raise ValueError("absolute output required")
        no_symlinks(output)
        resolved = output.resolve()
        if output.exists() or root.is_relative_to(resolved):
            raise ValueError("fresh non-overlapping output required")
        if resolved.is_relative_to(root) and not resolved.is_relative_to(q / "build"):
            raise ValueError("output inside source refused")
        output.mkdir(parents=True)
    proof = Proof(output)
    receipt: dict = {"schema": "M3_VIEWPORT_INSTALLED_NATIVE_V1", "status": "RUNNING",
                     "source_before": before, "java_page_wired": False,
                     "java_frame_wired": False, "frame_test_only": True,
                     "full_jdk_image": False, "widget_integration": False}
    try:
        proof.advance("lint")
        for path in sources:
            data = path.read_bytes()
            if not data.endswith(b"\n") or b"\r" in data or b"\0" in data:
                raise ValueError("source encoding/hygiene refusal: " + str(path))
        ast.parse((q / "verify.py").read_text())
        proof.run("shell-lint", ["bash", "-n", str(q / "verify.sh")])
        flags = ["-std=c17", "-Wall", "-Wextra", "-Wpedantic", "-Werror", "-Wconversion",
                 "-Wshadow", "-I" + str(home / "include"), "-I" + str(home / "include/linux")]
        proof.run("native-lint", [args.cc, *flags, "-fsyntax-only", str(sources[0])])
        proof.run("toolchain", [str(javac), "-version"])
        proof.advance("compile")
        classes, headers = output / "classes", output / "headers"
        classes.mkdir(); headers.mkdir()
        proof.run("java-compile", [str(javac), "--release", "21", "-encoding", "UTF-8",
                  "-Xlint:all", "-Werror", "-h", str(headers), "-d", str(classes),
                  *map(str, sources[1:5])])
        # Exact original-owner Java admission: before receiving, compile the
        # independent test-only declaration; after receiving, compile the
        # already-qualified product Java source with no duplicate method.
        java_source_hash = digest(sources[2])
        admitted_before = "36158dcb03adc764bddfab16172cb864d926518a304704fcea390040a7cf4069"
        # The merged product master changes only the license/header text; its
        # package-private method descriptor and javac-h output match the old owner.
        admitted_master = "099802e72b48be7f489e871b77b50f70bd9e7f2b6621ea688fa5fb929f7e47bb"
        admitted_after = "8a7b47da2819923f7e64d16a9d9827f92a9ee962a9512a1fe369a75665b60957"
        admitted_page = "b488da25aa3175ffa3522782867cca231056a60ca32fe3c198dcf3e60bc5873a"
        if java_source_hash not in (admitted_before, admitted_master, admitted_after, admitted_page):
            raise ValueError("native Java owner source drift")
        product_frame_wired = java_source_hash in (admitted_after, admitted_page)
        product_page_wired = java_source_hash == admitted_page
        receipt.update(java_frame_wired=product_frame_wired,
                       java_page_wired=product_page_wired,
                       frame_test_only=not product_frame_wired)
        frame_package = output / "frame-src/com/m3/collections"
        frame_package.mkdir(parents=True)
        frame_declaration = frame_package / "M3MaskViewportNative.java"
        original_native = sources[2].read_text()
        if product_frame_wired:
            frame_declaration.write_text(original_native)
        else:
            close = "\n}\n"
            if original_native.count(close) != 1:
                raise ValueError("native proof declaration anchor drift")
            new_method = ("\n    static native int projectFramePage(int[] offsets, int[] states, "
                          "int first, int length, int required, int forbidden, "
                          "int skip, int max, int[] rows, int rowOffset, "
                          "int[] slots, int slotOffset, int[] words, int wordOffset);\n")
            frame_declaration.write_text(original_native.replace(close, new_method + close))
        # Opaque text fixture becomes a Java source only inside the isolated proof image.
        frame_contract = frame_package / "M3FrameRegionContract.java"
        frame_contract.write_bytes((q / "fixtures/M3FrameRegionContract.txt").read_bytes())
        # After the page Java-LST receipt, use the *real* package-private
        # projectPage declaration with the original Java kernel as the oracle.
        # The opaque page fixture never enters the shipped product classes.
        frame_java_inputs = [str(frame_declaration), str(frame_contract)]
        if product_page_wired:
            page_contract = frame_package / "M3NativePageDirectContract.java"
            page_contract.write_bytes((q / "fixtures/M3NativePageDirectContract.txt").read_bytes())
            frame_java_inputs.extend((str(sources[1]), str(page_contract)))
        frame_classes, frame_headers = output / "frame-classes", output / "frame-headers"
        frame_classes.mkdir(); frame_headers.mkdir()
        proof.run("frame-java-compile", [str(javac), "--release", "21", "-encoding", "UTF-8",
                  "-Xlint:all", "-Werror", "-h", str(frame_headers), "-d", str(frame_classes),
                  *frame_java_inputs], clean_stderr=True)
        frame_header = frame_headers / "com_m3_collections_M3MaskViewportNative.h"
        if ("Java_com_m3_collections_M3MaskViewportNative_projectFramePage"
                not in frame_header.read_text()):
            raise RuntimeError("frame JNI header missing declared native entrypoint")
        build, install = output / "native-build", output / "install"
        proof.run("native-configure", ["cmake", "-S", str(q), "-B", str(build),
                  "-DCMAKE_BUILD_TYPE=Release", "-DCMAKE_C_COMPILER=" + str(Path(shutil.which(args.cc)).resolve()),
                  "-DCMAKE_INSTALL_PREFIX=" + str(install)])
        proof.run("native-build", ["cmake", "--build", str(build), "--parallel", "1"])
        proof.run("native-install", ["cmake", "--install", str(build)])
        header = "com_m3_collections_M3MaskViewportNative.h"
        if (headers / header).read_bytes() != (build / "jni" / header).read_bytes():
            raise RuntimeError("independent generated JNI headers differ")
        library = install / "lib/libm3maskviewport.so"
        exports = proof.run("native-exports", ["nm", "-D", "--defined-only", str(library)])
        names = {line.split()[-1] for line in exports.splitlines() if line.strip()}
        expected = {"Java_com_m3_collections_M3MaskViewportNative_" + method
                    for method in ("project", "projectPage", "projectFramePage")}
        if names != expected:
            raise RuntimeError("unexpected native exports")
        dynamic = proof.run("native-dependencies", ["readelf", "-d", str(library)])
        if any(name in dynamic for name in ("libjvm", "libjawt", str(root), str(output))):
            raise RuntimeError("native binary contains forbidden dependency/build path")
        checked = output / "libm3maskviewport-ubsan.so"
        proof.run("native-ubsan-compile", [args.cc, *flags, "-O2", "-fPIC", "-shared",
                  "-fsanitize=undefined", "-fno-sanitize-recover=all", "-include", str(headers / header),
                  str(sources[0]), "-o", str(checked)])
        moved = output / "relocated/lib/libm3maskviewport.so"
        moved.parent.mkdir(parents=True)
        shutil.copy2(library, moved)
        if digest(library) != digest(moved):
            raise RuntimeError("installed relocation bytes differ")
        proof.advance("test")
        observations: dict[str, str] = {}
        tests = ("M3MaskViewportKernelContract", "M3MaskViewportPageContract")
        for name in tests:
            for interpreted in (False, True):
                mode = "int" if interpreted else "mixed"
                result = proof.run("java-" + name.lower() + "-" + mode,
                    [str(java), "-ea", *(["-Xint"] if interpreted else []), "-cp", str(classes),
                     "com.m3.collections." + name], clean_stderr=True)
                key = "java-" + name
                if observations.setdefault(key, result) != result:
                    raise RuntimeError("Java execution-mode mismatch")
        proof.advance("runtime")
        for kind, binary, gc, interpreted in (
                ("installed", library, "Serial", False), ("installed", library, "G1", False),
                ("installed", library, "Parallel", False), ("relocated", moved, "Serial", False),
                ("ubsan", checked, "Serial", False), ("ubsan", checked, "Serial", True)):
            for name in tests:
                label = (kind + "-" + gc + "-" + ("int" if interpreted else "mixed") + "-" + name).lower()
                result = proof.run(label, [str(java), "-ea", "-Xcheck:jni", "-Xmx64m",
                    "-XX:+Use" + gc + "GC", *(["-Xint"] if interpreted else []),
                    "-Dm3.native.path=" + str(binary), "-cp", str(classes), "com.m3.collections." + name,
                    *(["--native"] if name.endswith("PageContract") else [])], clean_stderr=True)
                key = "jni-" + name
                if observations.setdefault(key, result) != result:
                    raise RuntimeError("JNI artifact/collector/mode mismatch")
        # Independently exercise the third export against its real JVM-side
        # reference. Run from the test-only class directory, never product classes.
        frame_observation: str | None = None
        for label, binary, interpreted in (
                ("installed", library, False), ("installed", library, True),
                ("relocated", moved, False), ("ubsan", checked, False),
                ("ubsan", checked, True)):
            mode = "int" if interpreted else "mixed"
            frame_result = proof.run("frame-" + label + "-" + mode,
                [str(java), "-ea", "-Xcheck:jni", "-Xmx64m", "-XX:+UseSerialGC",
                 *(["-Xint"] if interpreted else []), "-cp", str(frame_classes),
                 "com.m3.collections.M3FrameRegionContract", str(binary)], clean_stderr=True)
            if not frame_result.startswith("M3_FRAME_REGION_PASS checks=35080 digest=ed73ef29740c1e87"):
                raise RuntimeError("frame JNI reference oracle disagrees")
            if frame_observation is None:
                frame_observation = frame_result
            elif frame_observation != frame_result:
                raise RuntimeError("frame installed/relocated/ubsan JVM mismatch")
        observations["jni-M3FrameRegionContract"] = frame_observation
        if product_page_wired:
            page_observation: str | None = None
            for label, binary, gc, interpreted in (
                    ("installed", library, "Serial", False),
                    ("installed", library, "Serial", True),
                    ("installed", library, "G1", False),
                    ("relocated", moved, "Serial", False),
                    ("ubsan", checked, "Serial", False),
                    ("ubsan", checked, "Serial", True)):
                mode = "int" if interpreted else "mixed"
                page_result = proof.run("page-direct-" + label + "-" + gc.lower() + "-" + mode,
                    [str(java), "-ea", "-Xcheck:jni", "-Xmx64m", "-XX:+Use" + gc + "GC",
                     *(["-Xint"] if interpreted else []), "-cp", str(frame_classes),
                     "com.m3.collections.M3NativePageDirectContract", str(binary)],
                    clean_stderr=True)
                if page_observation is None:
                    page_observation = page_result
                elif page_observation != page_result:
                    raise RuntimeError("page direct installed/relocated/ubsan JVM mismatch")
            if page_observation is None or not page_observation.startswith(
                    "M3_NATIVE_PAGE_DIRECT_PASS checks=123474 digest=1102f3656342aadc"):
                raise RuntimeError("direct page JNI reference oracle disagrees")
            observations["jni-M3NativePageDirectContract"] = page_observation
        after = {str(path.relative_to(root)): digest(path) for path in sources}
        if after != before:
            raise RuntimeError("source changed during qualification")
        receipt.update(status="PASS", source_after=after, observations=observations,
                       installed_sha256=digest(library), relocated_sha256=digest(moved),
                       exports=sorted(names), command_count=len(proof.rows))
    except Exception as failure:
        receipt.update(status="FAIL", error=str(failure), last_phase=PHASES[proof.stage] if proof.stage >= 0 else None)
        print(str(failure), file=sys.stderr)
    finally:
        (output / "RECEIPT.json").write_text(json.dumps(receipt, indent=2) + "\n")
    print("M3_VIEWPORT_INSTALLED_NATIVE_" + receipt["status"] + " output=" + str(output))
    return 0 if receipt["status"] == "PASS" else 1


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (OSError, ValueError) as failure:
        print("ADMISSION_REFUSED: " + str(failure), file=sys.stderr)
        sys.exit(2)
