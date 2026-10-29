#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Admit exact array-mask successors while retaining original installer evidence."""

import argparse
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import subprocess
import sys


QUALIFICATION = "m3/collections/qualification/array-mask-successor-20261019"
BITMAP = "m3/collections/qualification/m3-long-bitmap-cache-20261017/recipe"
HISTOGRAM = "m3/collections/recipe/state-mask-histogram-20261017"
MANIFEST_SHA = "ad03213f94ae8b591ab06a75b8b1bc6a515d38609392e2de1e31bd2673bfdd49"
BITMAP_OUTPUTS = {
    "m3/collections/src/main/java/com/m3/collections/M3LongBitmapFilteredSnapshot.java":
        "ef4e4f4b7a8871cba0e631b96480ad3cf1e43640e5bf84a89cc91806df2d54d9",
    "m3/collections/src/test/java/com/m3/collections/M3LongBitmapFilteredSnapshotSelfTest.java":
        "de0be6b6fa43ea9b824db0f79106213976e94f8f7cdf6a84f15378a6c090cc37",
}
HISTORICAL = {
    "bitmap": {
        "m3/compatibility/synexia-m3-collections-receiver-20261018.tsv":
            "def50d31fc5ef824d37608881b0bc004330d2ac6d32ec6a9c80f89c9807ecc05",
        BITMAP + "/src/main/java/com/synexia/m3/M3LongBitmapFilteredSnapshotRecipe.java":
            "1e8fc8f95354b1cf3431e3eed5bdfbcb44ad828be7d288fdab30c69588e0d3f5",
        **{BITMAP + "/src/main/resources/templates/" + Path(path).name: digest
           for path, digest in BITMAP_OUTPUTS.items()},
    },
    "histogram": {
        HISTOGRAM + "/SOURCE_PINS.tsv":
            "7f6142791b6ff4e06c27c3e2c1e54fc40287e1288bc959f7b3fc112a9ed02feb",
        HISTOGRAM + "/manifest.tsv":
            "3e9c665ba9dbbe959dcb025c94463114ee37cd1385161f470a3732125e443951",
        HISTOGRAM + "/native-manifest.tsv":
            "c91bd681675305d213690528c0e31fe977dc9da4abe6346e4a3ac95179cf6c45",
    },
}


def sha(data):
    return hashlib.sha256(data).hexdigest()


def contained(root, relative):
    parsed = PurePosixPath(relative)
    if (not relative or parsed.is_absolute() or parsed.as_posix() != relative
            or ".." in parsed.parts or ".git" in parsed.parts or "\\" in relative
            or any(c in relative for c in "\t\r\n\0")):
        raise ValueError("REFUSE_PATH:" + repr(relative))
    path = root / relative
    cursor = path
    while cursor != root:
        if cursor.is_symlink():
            raise ValueError("REFUSE_SYMLINK:" + relative)
        cursor = cursor.parent
    if not path.is_file():
        raise ValueError("REFUSE_MISSING_OR_NONREGULAR:" + relative)
    return path


def verified(root, relative, expected, stage):
    data = contained(root, relative).read_bytes()
    if sha(data) != expected:
        raise ValueError("REFUSE_" + stage + "_DRIFT:" + relative)
    return data


def records(data, columns):
    for line in data.decode("utf-8", errors="strict").splitlines():
        if not line or line.startswith("#"):
            continue
        row = line.split("\t")
        if len(row) != columns:
            raise ValueError("REFUSE_MANIFEST_COLUMNS")
        yield row


def check(root, family="all"):
    root = root.absolute()
    if root.is_symlink() or not root.is_dir():
        raise ValueError("REFUSE_ROOT")
    if family not in ("all", "bitmap", "histogram"):
        raise ValueError("REFUSE_FAMILY")
    selected = ("bitmap", "histogram") if family == "all" else (family,)
    manifest = verified(root, QUALIFICATION + "/SUCCESSOR.tsv", MANIFEST_SHA, "SUCCESSOR_MANIFEST")
    successors = list(records(manifest, 5))
    if len(successors) != 4 or [r[1] for r in successors] != sorted({r[1] for r in successors}):
        raise ValueError("REFUSE_SUCCESSOR_SET")
    history, original_outputs = {}, {}
    for name in selected:
        for relative, digest in HISTORICAL[name].items():
            history[relative] = verified(root, relative, digest, "HISTORICAL")
        if name == "bitmap":
            original_outputs[name] = dict(BITMAP_OUTPUTS)
        else:
            original_outputs[name] = {}
            for name_manifest in ("manifest.tsv", "native-manifest.tsv"):
                for target, before, digest, template in records(history[HISTOGRAM + "/" + name_manifest], 4):
                    if before != "ABSENT" or target in original_outputs[name]:
                        raise ValueError("REFUSE_HISTORICAL_PRECONDITION:" + target)
                    relative = HISTOGRAM + "/templates/" + template
                    history[relative] = verified(root, relative, digest, "HISTORICAL")
                    original_outputs[name][target] = digest
    current = {}
    for name, target, before, after, git_blob in successors:
        if name not in selected:
            continue
        if original_outputs[name].get(target) != before or before == after:
            raise ValueError("REFUSE_SUCCESSOR_PREIMAGE:" + target)
        data = verified(root, target, after, "CURRENT")
        if hashlib.sha1(b"blob " + str(len(data)).encode("ascii") + b"\0" + data).hexdigest() != git_blob:
            raise ValueError("REFUSE_CURRENT_BLOB:" + target)
        current[target] = data
    unchanged = {}
    for name in selected:
        for target, digest in original_outputs[name].items():
            if target not in current:
                unchanged[target] = verified(root, target, digest, "CURRENT")
    return {"family": family, "current": current, "unchanged": unchanged, "historical": history}


def state(root):
    return {p.relative_to(root).as_posix(): (sha(p.read_bytes()), p.stat().st_mode, p.stat().st_mtime_ns)
            for p in root.rglob("*") if p.is_file()}


def qualify_bitmap(root, output, java_home):
    admitted = check(root, "bitmap")
    output = output.absolute()
    if output.resolve().is_relative_to(root.resolve()):
        raise ValueError("qualification output must be outside the inspected repository")
    output.mkdir(parents=True, exist_ok=False)
    classes, temporary, logs = output / "classes", output / "tmp", output / "logs"
    for path in (classes, temporary, logs):
        path.mkdir()
    commands = []
    receipt = {"schema": "M3_ARRAY_MASK_HISTORICAL_INSTALLER_V1", "status": "FAIL",
               "historical_inputs_sha256": {p: sha(b) for p, b in admitted["historical"].items()},
               "current_inputs_sha256": {p: sha(b) for p, b in admitted["current"].items()},
               "commands": commands, "product_sources_written": False}

    def run(label, argv, expected=0, refusal=None):
        result = subprocess.run(argv, capture_output=True, text=True, timeout=90)
        (logs / (label + ".stdout.log")).write_text(result.stdout, encoding="utf-8")
        (logs / (label + ".stderr.log")).write_text(result.stderr, encoding="utf-8")
        commands.append({"case": label, "argv": argv, "exit_code": result.returncode,
                         "expected_refusal": refusal, "stdout_sha256": sha(result.stdout.encode()),
                         "stderr_sha256": sha(result.stderr.encode())})
        if result.returncode != expected or (refusal is not None and refusal not in result.stderr):
            raise AssertionError(label + " failed: " + result.stdout + result.stderr)

    try:
        source = root / (BITMAP + "/src/main/java/com/synexia/m3/M3LongBitmapFilteredSnapshotRecipe.java")
        run("original-strict-compile", [str(java_home / "bin/javac"), "--release", "21", "-proc:none",
                                        "-Xlint:all", "-Werror", "-d", str(classes), str(source)])
        resources = root / (BITMAP + "/src/main/resources")
        command = [str(java_home / "bin/java"), "-Djava.io.tmpdir=" + str(temporary), "-cp",
                   str(classes) + os.pathsep + str(resources), "com.synexia.m3.M3LongBitmapFilteredSnapshotRecipe"]
        run("original-selftest", command + ["--selftest"])
        if list(temporary.iterdir()):
            raise AssertionError("original installer selftest leaked temporary files")
        historical = output / "historical-receiver"
        (historical / "m3/collections").mkdir(parents=True)
        run("historical-missing-refusal", command + ["--check", str(historical)], 1, "REFUSE_PRODUCT_MISSING")
        run("historical-install", command + ["--apply", str(historical)])
        for path, digest in BITMAP_OUTPUTS.items():
            verified(historical, path, digest, "HISTORICAL")
        first = state(historical)
        run("historical-fixed-point", command + ["--apply", str(historical)])
        run("historical-check", command + ["--check", str(historical)])
        if state(historical) != first:
            raise AssertionError("historical fixed point changed bytes or metadata")
        current = output / "current-receiver"
        for relative, data in admitted["current"].items():
            target = current / relative
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(data)
        first = state(current)
        run("old-check-refuses-current", command + ["--check", str(current)], 1, "REFUSE_PRODUCT_DRIFT")
        run("old-apply-refuses-current", command + ["--apply", str(current)], 1, "REFUSE_PRODUCT_DRIFT")
        if state(current) != first:
            raise AssertionError("old installer changed successor sources")
        receipt.update(status="PASS", historical_preconditions_unchanged=True,
                       original_installer_refuses_successors=True,
                       fixed_point_preserves_bytes_modes_mtimes=True)
    finally:
        (output / "RECEIPT.json").write_text(json.dumps(receipt, indent=2) + "\n", encoding="utf-8")
    return len(commands)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--family", choices=("all", "bitmap", "histogram"), default="all")
    parser.add_argument("--qualify-bitmap", type=Path, metavar="OUTPUT", help="Retain old installer proof outside the inspected root")
    parser.add_argument("--java-home", type=Path, default=os.environ.get("JAVA_HOME"))
    args = parser.parse_args()
    if sys.flags.optimize:
        parser.error("Python assertions are required")
    admitted = check(args.root, args.family)
    result = {"family": args.family, "current_successors": len(admitted["current"]),
              "unchanged_current_sources": len(admitted["unchanged"]),
              "historical_inputs": len(admitted["historical"]), "product_sources_written": False}
    if args.qualify_bitmap is not None:
        if args.family == "histogram" or args.java_home is None:
            parser.error("bitmap qualification requires bitmap/all family and an explicit Java home")
        result["historical_commands"] = qualify_bitmap(args.root.absolute(), args.qualify_bitmap, Path(args.java_home))
    print("M3_ARRAY_MASK_SUCCESSOR_PASS " + json.dumps(result, sort_keys=True))


if __name__ == "__main__":
    main()
