#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Prove a candidate M3SDK *developer* image executes two real vendor library APIs.

An OpenJDK/SDK product claim still requires its own make, jtreg, JNI and release gates.
"""
import argparse
import csv
import hashlib
import io
import json
import os
import re
import subprocess
from pathlib import Path

REQUIRED_TOOLS = (
    "java", "javac", "jar", "javadoc", "javap",
    "jdeps", "jlink", "jmod", "jpackage", "jshell",
)
SMOKE = "M3_SDK_VENDOR_API_SMOKE_PASS"
REQUIRED_DONORS = frozenset(("re2j", "com.google.common"))
UNCONTROLLED = ("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS")


def sha256(path):
    with path.open("rb") as source:
        return hashlib.file_digest(source, "sha256").hexdigest()


def read_pack(text, minimum=2):
    rows = list(csv.DictReader(io.StringIO(text), delimiter="\t"))
    if not rows or set(rows[0]) != {"module", "descriptor_source", "result"}:
        raise ValueError("SDK_JMOD_PACK_HEADER")
    modules = set()
    for row in rows:
        name = row["module"]
        if (not re.fullmatch(r"[A-Za-z][A-Za-z0-9_.]*", name)
                or name in modules
                or row["result"] != "JMOD"):
            raise ValueError("SDK_JMOD_PACK_DUPLICATE_OR_FAILED")
        modules.add(name)
    if len(modules) < minimum or not REQUIRED_DONORS.issubset(modules):
        raise ValueError("SDK_JMOD_PACK_INCOMPLETE")
    return frozenset(modules)


def locate(image, name):
    for file in (image / "bin" / name, image / "bin" / (name + ".exe")):
        if file.is_file():
            return file
    raise ValueError("SDK_TOOL_MISSING:" + name)


def java21_release(image):
    release = image / "release"
    if not release.is_file():
        raise ValueError("SDK_IMAGE_RELEASE_MISSING")
    body = release.read_text(encoding="utf-8")
    match = re.search(r'^JAVA_VERSION="(21(?:[.+-][^"]*)?)"$',
                      body, flags=re.MULTILINE)
    if not match:
        raise ValueError("SDK_IMAGE_NOT_JAVA21")
    return match.group(1)


def parse_modules(text):
    modules = []
    for line in text.splitlines():
        row = line.strip()
        if not row:
            continue
        name = row.split("@", 1)[0]
        if not re.fullmatch(r"[A-Za-z][A-Za-z0-9_.]*", name) or name in modules:
            raise ValueError("SDK_IMAGE_INVALID_MODULE_LIST")
        modules.append(name)
    return frozenset(modules)


def command(args, log):
    run = subprocess.run(
        [str(value) for value in args],
        stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True,
        encoding="utf-8", errors="replace", timeout=180, check=False,
    )
    log.write_text(run.stdout, encoding="utf-8")
    if run.returncode:
        raise RuntimeError("SDK_TOOL_EXECUTION_FAILED:" + log.name)
    return run.stdout


def prove(image_path, pack_path, out_path):
    if any(os.environ.get(key) for key in UNCONTROLLED):
        raise ValueError("SDK_UNCONTROLLED_VM_OPTIONS")
    image = Path(image_path).resolve(strict=True)
    pack = Path(pack_path).resolve(strict=True)
    output = Path(out_path).absolute()
    repo = Path(__file__).resolve().parents[4]
    if (output.exists() or output.resolve().is_relative_to(image)
            or output.resolve().is_relative_to(repo)):
        raise ValueError("SDK_OUTPUT_OCCUPIED_OR_UNSAFE")
    version = java21_release(image)
    tools = {name: locate(image, name) for name in REQUIRED_TOOLS}
    manifest = read_pack(pack.read_text(encoding="utf-8"), minimum=87)
    source = Path(__file__).resolve().parent / "fixtures" / "M3SdkImageSmoke.java"
    if not source.is_file():
        raise ValueError("SDK_SMOKE_SOURCE_MISSING")

    # Candidate output is created only after all static preflight checks.
    output.mkdir(parents=True, exist_ok=False)
    logs = output / "logs"
    logs.mkdir()
    listing = command((tools["java"], "--list-modules"), logs / "modules.log")
    actual = parse_modules(listing)
    if not manifest.issubset(actual):
        raise ValueError("SDK_IMAGE_MODULES_MISSING:" + ",".join(sorted(manifest - actual)))
    command((tools["java"], "--validate-modules"), logs / "validate-modules.log")

    classes = output / "classes"
    classes.mkdir()
    command((tools["javac"], "--release", "21", "-Xlint:all", "-Werror",
             "-d", classes, source), logs / "javac-smoke.log")
    execution = command(
        (tools["java"], "-Xcheck:jni", "--add-modules",
         ",".join(sorted(REQUIRED_DONORS)),
         "-cp", classes, "M3SdkImageSmoke"),
        logs / "vendor-api-smoke.log",
    )
    if SMOKE not in execution.splitlines():
        raise ValueError("SDK_VENDOR_API_SMOKE_MISSING")
    result = {
        "schema": "M3SDK_IMAGE_RUNTIME_PROOF_V1",
        "status": "LOCAL_SDK_IMAGE_RUNTIME_SMOKE_PROVEN",
        "java_version": version,
        "jdk_release_sha256": sha256(image / "release"),
        "jmod_pack_sha256": sha256(pack),
        "java_smoke_sha256": sha256(source),
        "vendor_module_count": len(manifest),
        "linked_module_count": len(actual),
        "tools": list(REQUIRED_TOOLS),
        "vendor_apis_executed": ["com.google.re2j.Pattern",
                                 "com.google.common.collect.ImmutableList"],
        "java21_language_contract": "NOT_PROVEN_BY_SMOKE",
        "openjdk_jtreg": "NOT_EXECUTED_BY_SMOKE",
        "product_accepted": False,
    }
    (output / "RECEIPT.json").write_text(
        json.dumps(result, sort_keys=True, indent=2) + "\n", encoding="utf-8",
    )
    return result


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--image", type=Path, required=True)
    parser.add_argument("--pack", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args(argv)
    print(json.dumps(prove(args.image, args.pack, args.out), sort_keys=True))


if __name__ == "__main__":
    main()
