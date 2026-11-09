#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Seal compact recovery custody against the exact public baseline; never edit a receiving tree."""

import argparse
import hashlib
import importlib.util
import io
import json
from pathlib import Path, PurePosixPath
import subprocess
import sys
import tarfile


PIN = "07ba6e383343d32c83cfdaf641387bbe72bf325d"
TASK = "m3-collections-contract-refinement-20261017"
HOST = "m3/arrays/qualification/empty-segments-20261017/recipe-custody"
INNER = "m3jdk21-runtime-restore-07ba"
OUTER = "m3-runtime-restore-custody"
LABEL = "runtime-restore-07ba-20261018"


def sha(data):
    return hashlib.sha256(data).hexdigest()


def git_snapshot(repository, pin, patterns, destination):
    """Materialize only pinned regular files, checking paths, modes and Git blob identities."""
    listing = subprocess.check_output(["git", "-C", str(repository), "ls-tree", "-r", "-z", "--full-tree",
                                       pin, "--", *patterns])
    expected = {}
    parents = set()
    for entry in listing.split(b"\0"):
        if not entry:
            continue
        metadata, raw_path = entry.split(b"\t", 1)
        mode, kind, blob = metadata.decode("ascii").split()
        path = raw_path.decode("utf-8")
        relative = PurePosixPath(path)
        if relative.is_absolute() or ".." in relative.parts or str(relative) != path or "\\" in path:
            raise ValueError("unsafe pinned source path")
        if kind != "blob" or mode not in ("100644", "100755") or path in expected:
            raise ValueError("pinned snapshot admits regular unique files only")
        expected[path] = {"mode": int(mode[-3:], 8), "git_blob": blob}
        parents.update(str(parent) for parent in relative.parents if str(parent) != ".")
    if not expected:
        return {}
    archive = subprocess.check_output(["git", "-c", "tar.umask=0022", "-C", str(repository), "archive", pin, "--", *sorted(expected)])
    seen = set()
    with tarfile.open(fileobj=io.BytesIO(archive), mode="r:") as bundle:
        for member in bundle:
            name = member.name.rstrip("/")
            if member.isdir():
                if name not in parents:
                    raise ValueError("unexpected archive directory")
                (destination / name).mkdir(parents=True, exist_ok=True)
                continue
            if not member.isfile() or name not in expected or name in seen:
                raise ValueError("unexpected, duplicate or nonregular archive entry")
            data = bundle.extractfile(member).read()
            blob = hashlib.sha1(b"blob " + str(len(data)).encode("ascii") + b"\0" + data).hexdigest()
            if blob != expected[name]["git_blob"] or member.mode & 0o777 != expected[name]["mode"]:
                raise ValueError("archive bytes or mode differ from the exact Git tree")
            target = destination / name
            if target.exists() or target.is_symlink():
                raise ValueError("snapshot destination already contains a selected file")
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(data)
            target.chmod(expected[name]["mode"])
            expected[name].update(sha256=sha(data), size_bytes=len(data))
            seen.add(name)
    if seen != set(expected):
        raise ValueError("archive omitted selected pinned files")
    return expected


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ("source-inventory", "recipe-owner", "repository", "generic-builder", "output"):
        parser.add_argument("--" + name, type=Path, required=True)
    args = parser.parse_args()
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=False)
    owner = args.recipe_owner.resolve()
    crate = owner / "recipe-crates" / TASK
    baseline = output / "baseline"
    baseline.mkdir()
    pinned_files = git_snapshot(args.repository.resolve(), PIN, [HOST], baseline)
    command = [sys.executable, "-B", str(args.generic_builder.resolve()),
               "--source-crate", str(crate), "--recipe-owner", str(owner),
               "--public-root", str(baseline), "--public-baseline-ref", PIN,
               "--canonical-baseline-ref", "8f8200375d8d403b58e855ee5db02f2e8fac33a8",
               "--dataset", INNER, "--inventory", str(args.source_inventory.resolve()),
               "--label", LABEL, "--output", str(output / "candidate-input")]
    result = subprocess.run(command, capture_output=True, timeout=120)
    (output / "builder.stdout.log").write_bytes(result.stdout)
    (output / "builder.stderr.log").write_bytes(result.stderr)
    if result.returncode:
        raise ValueError("unchanged public handoff builder refused: " + result.stderr.decode())
    specification = importlib.util.spec_from_file_location("original_recovery_prepare", crate / "prepare.py")
    prepare = importlib.util.module_from_spec(specification)
    specification.loader.exec_module(prepare)
    metadata = prepare.seal(crate, OUTER, "hsoliwal/M3jdk21", baseline,
                            [output / "candidate-input"], PIN, owner)
    if metadata["java_targets"] != 0 or metadata["text_targets"] > 256:
        raise ValueError("custody must retain the original text-lane admission bound")
    receipt = {"schema": "M3_RUNTIME_RECOVERY_PUBLIC_CUSTODY_PREPARATION_V1",
               "baseline": PIN, "dataset": metadata, "source_inventory_sha256": sha(args.source_inventory.read_bytes()),
               "original_builder_sha256": sha(args.generic_builder.read_bytes()), "builder_command": command,
               "baseline_files": pinned_files, "product_sources_written": False,
               "qualification": "PENDING_FRESH_ORIGINAL_MAVEN_VERIFY", "phase": "STRING_UNCHANGED"}
    (output / "PREPARATION.json").write_text(json.dumps(receipt, indent=2) + "\n")
    print(json.dumps({"dataset": OUTER, "targets": metadata["targets"],
                      "baseline_public_files": len(pinned_files), "output": str(output)}, sort_keys=True))


if __name__ == "__main__":
    main()
