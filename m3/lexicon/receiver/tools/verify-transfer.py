#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors
"""Cross-platform Java 21 transfer checks. Does not download data or require WSL/Maven."""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import subprocess


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, required=True, help="Fresh directory for classes and evidence")
    parser.add_argument("--interpreter", action="store_true", help="Also execute the composition proof with -Xint")
    args = parser.parse_args()
    module = Path(__file__).resolve().parent.parent
    output = args.output.resolve()
    if output.exists():
        parser.error("output already exists; use a fresh directory")
    for tool in ("javac", "java"):
        if shutil.which(tool) is None:
            parser.error(tool + " not found; use a JDK 21+ installation")
    output.mkdir(parents=True)
    classes = output / "classes"
    classes.mkdir()
    package = Path("com/m3/lexicon")
    sources = [module / "src/main/java" / package / (n + ".java") for n in
               ("M3RegexWordTransferRows", "M3BitLanes", "M3LexiconFormat", "M3LexiconFormatException")]
    sources += [module / "src/test/java" / package / (n + ".java") for n in
                ("M3RegexWordTransferBoundaryProof", "M3RegexWordTransferCompositionProof")]
    steps = []

    def run(name, command):
        result = subprocess.run([str(part) for part in command], capture_output=True,
                                encoding="utf-8", errors="replace", timeout=180)
        # Normalize only the platform line ending in executable observations.
        log = result.stdout.replace("\r\n", "\n") + result.stderr.replace("\r\n", "\n")
        (output / (name + ".log")).write_bytes(log.encode("utf-8"))
        steps.append({"name": name, "exit": result.returncode,
                      "log_sha256": hashlib.sha256(log.encode("utf-8")).hexdigest()})
        if result.returncode:
            print(log)
            raise RuntimeError(name + " failed")
        print(name + ": PASS")
        return log

    try:
        run("compiler-version", ["javac", "-version"])
        run("compile", ["javac", "--release", "21", "-encoding", "UTF-8", "-Xlint:all", "-Werror",
                        "-proc:none", "-d", classes, *sources])
        prefix = ["java", "-cp", classes]
        run("boundary", prefix + ["com.m3.lexicon.M3RegexWordTransferBoundaryProof"])
        log = run("legacy", prefix + ["com.m3.lexicon.M3RegexWordTransferBoundaryProof", "legacy"])
        if hashlib.sha256(log.encode("utf-8")).hexdigest() != "656f431a704d46d0df05b8dd081de0e1f9f349d8e44e612fc34359acdb53ed6a":
            raise RuntimeError("legacy observation differs from pinned baseline")
        run("composition", prefix + ["com.m3.lexicon.M3RegexWordTransferCompositionProof"])
        run("scale", prefix + ["com.m3.lexicon.M3RegexWordTransferCompositionProof", "scale"])
        if args.interpreter:
            run("composition-interpreter", ["java", "-Xint", "-cp", classes,
                                             "com.m3.lexicon.M3RegexWordTransferCompositionProof"])
        status = "PASS_FOCUSED_JAVA_ONLY"
    except (RuntimeError, subprocess.TimeoutExpired, OSError) as failure:
        status = "FAIL"
        raise SystemExit(str(failure)) from failure
    finally:
        receipt = {"scope": "stock JDK21, actual transfer owners, direct DFA oracle",
                   "status": locals().get("status", "FAIL"), "steps": steps,
                   "sources": {str(p.relative_to(module)): hashlib.sha256(p.read_bytes()).hexdigest()
                               for p in sources if p.is_file()},
                   "not_run": ["full receiver Maven", "full JDK/jtreg", "JNI", "Windows unless run there"]}
        (output / "receipt.json").write_text(json.dumps(receipt, indent=2) + "\n", encoding="utf-8")
    print(status)


if __name__ == "__main__":
    main()
