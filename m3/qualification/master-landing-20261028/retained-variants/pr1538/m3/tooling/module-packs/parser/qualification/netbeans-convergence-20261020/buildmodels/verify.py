#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors
"""Replay finite API-shape boundary fixtures against the existing Java owner.

This qualifier performs no dependency resolution or provider initialization.
Real-provider execution remains in m3-ast-real-providers-20261019.yml.
"""

import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import sys


OWNER_DIRECTORY = Path("m3/tooling/module-packs/parser/src/main/java/jdk/m3/parser")
OWNER_NAMES = (
    "M3ASTAuthoritativeModels.java",
    "M3ASTIngress.java",
    "M3ASTIndex.java",
    "M3File.java",
)
MAIN_CLASS = "jdk.m3.parser.test.M3BuildModelLimitsContract"


def sha256(data):
    return hashlib.sha256(data).hexdigest()


def require(condition, message):
    if not condition:
        raise RuntimeError(message)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo-root", type=Path, required=True)
    parser.add_argument("--jdk-home", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True,
                        help="fresh output directory; an existing path is refused")
    parser.add_argument("--expect", choices=("candidate", "baseline"), default="candidate")
    parser.add_argument("--owner-source", type=Path,
                        help="optional exact source snapshot; the selected owner hash is enforced")
    args = parser.parse_args()
    here = Path(__file__).resolve().parent
    root = args.repo_root.resolve(strict=True)
    jdk = args.jdk_home.resolve(strict=True)
    out = args.out.absolute()
    require(not out.exists() and not out.is_symlink(), "output path must be absent")
    out.mkdir(parents=True)
    receipt = {
        "kind": "FINITE_API_SHAPE_JAVA",
        "mode": args.expect,
        "status": "INCOMPLETE",
        "owner_directory": str(OWNER_DIRECTORY),
        "repo_root": str(root),
        "jdk_home": str(jdk),
        "commands": [],
        "inputs": {},
        "cases": [],
        "real_provider_execution": False,
        "openrewrite_sdk_execution": False,
        "full_jdk_qualification": False,
    }

    def run(command, log_name, timeout):
        command = [str(value) for value in command]
        entry = {"argv": command, "timeout_seconds": timeout, "log": log_name}
        receipt["commands"].append(entry)
        try:
            completed = subprocess.run(command, stdout=subprocess.PIPE,
                                       stderr=subprocess.STDOUT, timeout=timeout, check=False)
        except subprocess.TimeoutExpired as error:
            data = error.stdout or b""
            (out / log_name).write_bytes(data)
            entry["timed_out"] = True
            raise RuntimeError("timed out: " + log_name) from error
        (out / log_name).write_bytes(completed.stdout)
        entry["exit"] = completed.returncode
        entry["sha256"] = sha256(completed.stdout)
        return completed.returncode, completed.stdout

    try:
        expectations = json.loads((here / "expectations.json").read_text())
        for name in ("verify.py", "expectations.json", "cases.tsv", "FIXTURES.sha256",
                     "expected-api.txt"):
            receipt["inputs"][name] = sha256((here / name).read_bytes())
        for executable in ("java", "javac", "javap"):
            require((jdk / "bin" / executable).is_file(), "missing JDK executable: " + executable)
        code, version = run([jdk / "bin/java", "--version"], "java-version.txt", 20)
        require(code == 0, "java --version failed")
        require(version.decode().splitlines()[0].split()[1].split(".")[0] == "21",
                "this frozen qualifier requires Java 21")

        source_root = out / "sources"
        sources = []
        for name in OWNER_NAMES:
            source = root / OWNER_DIRECTORY / name
            if name == OWNER_NAMES[0] and args.owner_source is not None:
                source = args.owner_source.resolve(strict=True)
            data = source.read_bytes()
            digest = sha256(data)
            receipt["inputs"][str(OWNER_DIRECTORY / name)] = digest
            if name == OWNER_NAMES[0]:
                require(digest == expectations[args.expect + "_owner_sha256"],
                        "owner hash differs from the frozen " + args.expect + " snapshot")
                receipt["owner_source"] = str(source)
            destination = source_root / "jdk/m3/parser" / name
            destination.parent.mkdir(parents=True, exist_ok=True)
            destination.write_bytes(data)
            sources.append(destination)

        fixture_paths = []
        for line in (here / "FIXTURES.sha256").read_text().splitlines():
            digest, path = line.split("  ", 1)
            relative = Path(path)
            require(relative.parts[0] == "fixtures" and ".." not in relative.parts,
                    "unexpected fixture path")
            data = (here / relative).read_bytes()
            require(sha256(data) == digest, "frozen fixture changed: " + path)
            receipt["inputs"][path] = digest
            destination = source_root / relative.relative_to("fixtures")
            require(not destination.exists(), "fixture overlaps an owner source")
            destination.parent.mkdir(parents=True, exist_ok=True)
            destination.write_bytes(data)
            sources.append(destination)
            fixture_paths.append(relative)
        require(sorted(fixture_paths) == sorted(p.relative_to(here)
                for p in (here / "fixtures").rglob("*.java")), "fixture inventory differs")

        classes = out / "classes"
        classes.mkdir()
        code, _ = run([jdk / "bin/javac", "--release", "21", "-Xlint:all", "-Werror",
                       "-d", classes, *sources], "compile.txt", 60)
        require(code == 0, "strict compilation failed")
        code, api = run([jdk / "bin/javap", "-classpath", classes, "-protected", "-s",
                        "jdk.m3.parser.M3ASTAuthoritativeModels"], "public-protected-api.txt", 20)
        require(code == 0, "public/protected API extraction failed")
        expected_api = (here / "expected-api.txt").read_bytes()
        require(sha256(expected_api) == expectations["public_protected_api_sha256"],
                "frozen API expectation differs")
        require(api == expected_api, "public/protected API differs")
        receipt["api_sha256"] = sha256(api)

        for case in expectations["cases"]:
            name = case["case"]
            code, data = run([jdk / "bin/java", "-ea", "-cp", classes, MAIN_CLASS, name],
                             name + ".txt", 20)
            lines = data.decode("utf-8", errors="replace").splitlines()
            expected_exit = case[args.expect + "_exit"]
            expected_diagnostic = case[args.expect + "_diagnostic"]
            require(code == expected_exit, "unexpected exit for " + name)
            require(lines and lines[0] == expected_diagnostic, "unexpected diagnostic for " + name)
            if expected_exit == 0:
                require(lines == [expected_diagnostic], "unexpected extra output for " + name)
            receipt["cases"].append({"case": name, "exit": code,
                                      "diagnostic": lines[0], "expected_observation": True})

        receipt["status"] = ("PASS" if args.expect == "candidate"
                             else "BASELINE_COUNTEREXAMPLES_REPRODUCED")
        print("M3_BUILD_MODEL_LIMITS_QUALIFICATION_" + receipt["status"]
              + " cases=" + str(len(receipt["cases"])) + " kind=FINITE_API_SHAPE_JAVA")
        return 0
    except (OSError, RuntimeError, ValueError, IndexError, KeyError) as error:
        receipt["status"] = "FAIL"
        receipt["error"] = str(error)
        print("M3_BUILD_MODEL_LIMITS_QUALIFICATION_FAIL " + str(error), file=sys.stderr)
        return 1
    finally:
        (out / "receipt.json").write_text(json.dumps(receipt, indent=2) + "\n")


if __name__ == "__main__":
    sys.exit(main())
