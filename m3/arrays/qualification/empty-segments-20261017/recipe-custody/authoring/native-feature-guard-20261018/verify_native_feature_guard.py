#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Compare a sealed JNI leaf across supported feature-macro definitions."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--before", type=Path, required=True)
    parser.add_argument("--after", type=Path, required=True)
    parser.add_argument("--jdk", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=True)
    flags = ["-std=c11", "-O2", "-g0", "-fPIC", "-fno-ident",
             "-Wall", "-Wextra", "-Werror", "-D_REENTRANT",
             "-D_LARGEFILE64_SOURCE", "-DLINUX",
             "-I" + str(args.jdk.resolve() / "include"),
             "-I" + str(args.jdk.resolve() / "include/linux")]
    variants = [("before-standalone", args.before, []),
                ("after-standalone", args.after, []),
                ("after-build-defined", args.after, ["-D_GNU_SOURCE"]),
                ("after-defined-zero", args.after, ["-D_GNU_SOURCE=0"])]
    records = []
    for name, source, definitions in variants:
        steps = []
        for stage, options, suffix in [("preprocess", ["-E", "-P"], ".i"),
                                       ("compile", ["-c"], ".o")]:
            result = output / (name + suffix)
            command = ["gcc"] + flags + definitions + options + [str(source.resolve()), "-o", str(result)]
            completed = subprocess.run(command, capture_output=True, text=True)
            (output / (name + "-" + stage + ".log")).write_text(completed.stdout + completed.stderr)
            steps.append({"stage": stage, "command": command, "exit": completed.returncode})
            if completed.returncode:
                raise RuntimeError(name + ": " + completed.stderr)
        records.append({"variant": name, "source_sha256": digest(source),
                        "preprocessed_sha256": digest(output / (name + ".i")),
                        "object_sha256": digest(output / (name + ".o")), "steps": steps})
    preprocessed = {item["preprocessed_sha256"] for item in records}
    objects = {item["object_sha256"] for item in records}
    if len(preprocessed) != 1 or len(objects) != 1:
        raise AssertionError("native source or object code changed")
    report = {"schema": "M3_NATIVE_FEATURE_GUARD_EQUIVALENCE_V1", "status": "PASS",
              "variants": records, "preprocessed_identity": True, "object_identity": True,
              "limit": "Finite Linux/GCC13.3/JDK21 source closure; full owning JDK build and runtime remain separate gates."}
    (output / "receipt.json").write_text(json.dumps(report, indent=2) + "\n")
    print(json.dumps({"status": "PASS", "variants": len(records),
                      "preprocessed_sha256": next(iter(preprocessed)),
                      "object_sha256": next(iter(objects))}))


if __name__ == "__main__":
    main()
