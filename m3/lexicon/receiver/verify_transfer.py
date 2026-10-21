# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors
"""Focused actual-owner proof. No Maven, network, downloads, WSL or fabricated dependencies.

Run with Python 3.10+ and JDK 21+:
  python verify_transfer.py --out /absolute/fresh/proof
  python verify_transfer.py --out /absolute/fresh/native-proof --jni --ubsan
On Windows use an absolute Windows output path. --jni needs a GCC/Clang-compatible
compiler and the platform JNI headers; --ubsan is optional and toolchain-dependent.
"""
from __future__ import annotations

import argparse
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--out", type=Path, required=True)
    parser.add_argument("--jni", action="store_true")
    parser.add_argument("--ubsan", action="store_true")
    parser.add_argument("--cc", default=os.environ.get("CC", "gcc"))
    args = parser.parse_args()
    if args.ubsan and not args.jni:
        parser.error("--ubsan requires --jni")
    out = args.out
    if not out.is_absolute() or out.exists():
        parser.error("--out must be an absolute, not-yet-existing directory")
    root = Path(__file__).resolve().parent
    home = Path(os.environ["JAVA_HOME"]) if os.environ.get("JAVA_HOME") else None
    suffix = ".exe" if os.name == "nt" else ""
    javac = str(home / "bin" / ("javac" + suffix)) if home else shutil.which("javac")
    if not javac:
        raise SystemExit("JDK javac not found")
    if home is None:
        home = Path(javac).resolve().parent.parent
    java = str(home / "bin" / ("java" + suffix))
    out.mkdir(parents=True)
    classes, headers = out / "classes", out / "headers"
    classes.mkdir(); headers.mkdir()
    commands: list[dict[str, object]] = []

    def run(name: str, command: list[str]) -> str:
        completed = subprocess.run(command, text=True, encoding="utf-8", errors="replace",
                                   capture_output=True, timeout=180, check=False)
        (out / (name + ".log")).write_text(completed.stdout + completed.stderr, encoding="utf-8")
        commands.append({"gate": name, "command": command, "exit": completed.returncode})
        (out / "commands.json").write_text(json.dumps(commands, indent=2) + "\n", encoding="utf-8")
        print(completed.stdout, end="")
        print(completed.stderr, end="", file=sys.stderr)
        if completed.returncode:
            raise SystemExit(f"{name} failed ({completed.returncode}); see {out}")
        return completed.stdout + completed.stderr

    version = run("javac-version", [javac, "-version"])
    match = re.search(r"javac (\d+)", version)
    if not match or int(match.group(1)) < 21:
        raise SystemExit("JDK 21+ required")
    package = Path("com/m3/lexicon")
    main_src = root / "src/main/java" / package
    test_src = root / "src/test/java" / package
    source = [main_src / (name + ".java") for name in
              ("M3RegexWordTransferRows", "M3BitLanes", "M3LexiconFormat", "M3LexiconFormatException")]
    source += [test_src / (name + ".java") for name in
               ("M3LexiconTransferRegressionProof", "M3LexiconTransferCompositionProof")]
    run("javac", [javac, "--release", "21", "-encoding", "UTF-8", "-Xlint:all", "-Werror",
                  "-proc:none", "-h", str(headers), "-d", str(classes), *map(str, source)])
    prefix = "com.m3.lexicon."
    run("regression", [java, "-cp", str(classes), prefix + "M3LexiconTransferRegressionProof"])
    run("composition", [java, "-cp", str(classes), prefix + "M3LexiconTransferCompositionProof"])
    run("composition-interpreted", [java, "-Xint", "-cp", str(classes), prefix + "M3LexiconTransferCompositionProof"])
    legacy = run("legacy", [java, "-cp", str(classes), prefix + "M3LexiconTransferRegressionProof", "--legacy"])
    if "LEGACY_IMAGES a704ab6202a31c8a32def9ad80a7bf94b5880088d487dbc69b8e905b4c4e9c4d" not in legacy:
        raise SystemExit("Pinned incumbent image commitment changed")
    if args.jni:
        platform = "win32" if os.name == "nt" else "darwin" if sys.platform == "darwin" else "linux"
        library = out / ("m3_transfer_proof.dll" if os.name == "nt" else
                         "libm3_transfer_proof.dylib" if platform == "darwin" else "libm3_transfer_proof.so")
        compiler = shutil.which(args.cc)
        if not compiler:
            raise SystemExit("GCC/Clang-compatible compiler not found: " + args.cc)
        flags = ["-std=c11", "-O2", "-Wall", "-Wextra", "-Werror", "-Wconversion", "-Wsign-conversion", "-pedantic"]
        flags += ["-dynamiclib"] if platform == "darwin" else ["-shared"]
        if os.name != "nt":
            flags += ["-fPIC"]
        if args.ubsan:
            flags += ["-fsanitize=undefined", "-fno-sanitize-recover=all"]
        run("native-compile", [compiler, *flags, "-I" + str(home / "include"),
            "-I" + str(home / "include" / platform), "-I" + str(headers),
            str(root / "src/test/native/m3_lexicon_transfer_jni.c"), "-o", str(library)])
        for label, flags in [("jni", []), ("jni-interpreted", ["-Xint"])]:
            run(label, [java, *flags, "-Xcheck:jni", "-cp", str(classes),
                prefix + "M3LexiconTransferCompositionProof", "--jni", str(library)])
    print("FOCUSED_M3LEXICONS_PASS; full module/Maven/OpenRewrite SDK/installed JDK not qualified")


if __name__ == "__main__":
    main()
