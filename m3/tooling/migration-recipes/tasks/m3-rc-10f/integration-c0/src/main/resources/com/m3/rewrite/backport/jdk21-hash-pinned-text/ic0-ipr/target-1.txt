#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Prepare two exact archived inputs using the retained archive and install owners.

The caller owns exclusive workspace mutation and captures this command's stdout
and stderr. Each invocation emits its own receipt; no previous receipt is edited.
This is input preparation, not Maven, test, runtime, or product qualification.
"""
from __future__ import annotations

import argparse
import base64
import hashlib
import json
import os
from pathlib import Path
import stat
import subprocess
import sys
import tempfile

PREFIX = "m3/tooling/migration-recipes/verification/ci8891-20261006/prepared-inputs"
ARCHIVE_OWNER = PREFIX + "/acquisition/package_evidence.py"
ARCHIVE_SHA = "89d43693f508c723e431296cc06f611c8de4f94df8eaddea95a2c7742feb0574"
INSTALL_OWNER = "m3/migration/recipe.py"
INSTALL_SHA = "453d976a5f29526432be138b357d3f52f868fcc40a4ee697d969cc0836359628"
BUNDLE = PREFIX + "/bundle"
# Exact independent bundle built and verified before this wrapper was frozen.
BUNDLE_FILES = {'BUNDLE.json': (5113, '85932b7369294366da62da7b751bb9eeb496e893414ba5a95694a48b3cd3b474'), 'LANES.json': (3, '37517e5f3dc66819f61f5a7bb8ace1921282415f10551d2defa5c3eb0985b570'), 'PACKAGE.json': (1216, '2fff0dd45323639dc69f6771c528d97333861f055c643e440e88fa9e3bed273d'), 'PATHS-0001.tsv': (311, '6c90c8e7fa7dbb52c4a4dec1ecdad61abc8fb689cd60b7bf841d777cba8356b9'), 'README.md': (2360, '04c060cfc42efc15316374006298ce88e8ccb87c1d74cde8180227dff5c28f35'), 'part-0001.tar.gz': (2787, '24e9ca023badad5f98788ddb2dfcc67fe6a59c14665242f3b58d29b21141e6b6')}
BUNDLE_SHA = "85932b7369294366da62da7b751bb9eeb496e893414ba5a95694a48b3cd3b474"
PLAN_SHA = "b5712cf6ce4c0113f0fa1c09c7d8c0e50487fdf0936b8f593ba550f832a469a2"
PLAN_SEAL = "b7c182828f47bcaca54effae56235a93246f0389c3c7ac8a2a1252a5016aa3ca"
RECIPE_ID = "m3.immutable-inputs.jep485-lane28/1"
OUTPUTS = (
    ("m3/backports/recipes/jep-485-gatherers/packet.tsv",
     "replay/images/jep485-packet.tsv", 192,
     "7e79ce168d8f3d0df8e3d7d633a1351d399f5013b4f7617824fbfa9d7efe613b"),
    ("m3/tooling/lane28/src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/m3-lane28/tq-verify-plan.py.txt",
     "replay/images/tq-verify-plan.py.txt", 5089,
     "ff2c31f57aac81a7049612557a3757781927563bd2d7a7b598aa62a116e1d978"),
)
LOGICAL = {"replay/plan.json": (940, PLAN_SHA),
           **{logical: (size, digest) for _, logical, size, digest in OUTPUTS}}


class Refusal(ValueError):
    pass


def require(condition, message):
    if not condition:
        raise Refusal(message)


def digest(data):
    return hashlib.sha256(data).hexdigest()


def checked(root, relative):
    parts = relative.split("/")
    require(relative and not relative.startswith("/")
            and all(p not in ("", ".", "..") and ":" not in p for p in parts)
            and "\\" not in relative and "\x00" not in relative, "unsafe fixed path")
    path = root
    for part in parts:
        path /= part
        require(not path.is_symlink(), "symlink refused: " + relative)
    return path


def identity(path, size=None, expected=None):
    s = path.lstat()
    require(stat.S_ISREG(s.st_mode) and stat.S_IMODE(s.st_mode) == 0o644,
            "regular mode 0644 required: " + str(path))
    require(s.st_nlink == 1, "hard-linked input refused: " + str(path))
    if size is not None:
        require(s.st_size == size, "size drift: " + str(path))
    data = path.read_bytes()
    actual = digest(data)
    require(len(data) == s.st_size, "input changed during read: " + str(path))
    if expected is not None:
        require(actual == expected, "SHA256 drift: " + str(path))
    return {"sha256": actual, "bytes": len(data), "mode": "100644"}


def exact_tree(root, expected):
    require(root.is_dir() and not root.is_symlink(), "regular input directory required")
    actual = set()
    expected_dirs = {str(p) for n in expected for p in Path(n).parents if str(p) != "."}
    for parent, dirs, files in os.walk(root, followlinks=False):
        for name in dirs:
            p = Path(parent) / name
            require(not p.is_symlink() and p.relative_to(root).as_posix() in expected_dirs,
                    "unexpected input directory: " + str(p))
        for name in files:
            p = Path(parent) / name
            relative = p.relative_to(root).as_posix()
            require(relative in expected, "unexpected input file: " + relative)
            actual.add(relative)
    require(actual == set(expected), "input closure mismatch")
    return {n: identity(checked(root, n), size, sha)
            for n, (size, sha) in sorted(expected.items())}


def raw(data):
    data = data or b""
    return {"bytes": len(data), "sha256": digest(data),
            "base64": base64.b64encode(data).decode("ascii")}


def run(argv, receipt):
    row = {"argv": argv, "timeoutSeconds": 60}
    try:
        result = subprocess.run(argv, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                                timeout=60, check=False)
        row.update(exitCode=result.returncode, stdout=raw(result.stdout), stderr=raw(result.stderr))
    except (subprocess.TimeoutExpired, OSError) as error:
        row.update(exitCode=None, stdout=raw(getattr(error, "stdout", None)),
                   stderr=raw(getattr(error, "stderr", None)),
                   exception={"type": type(error).__name__, "message": str(error)})
        receipt["commands"].append(row)
        raise Refusal("child command did not complete") from error
    receipt["commands"].append(row)
    require(result.returncode == 0, "child command refused: " + argv[3])
    try:
        parsed = json.loads(result.stdout)
    except (UnicodeDecodeError, ValueError) as error:
        raise Refusal("child command did not emit its JSON receipt") from error
    require(isinstance(parsed, dict), "child receipt must be an object")
    return parsed


def prepare(root, receipt):
    require(BUNDLE_FILES is not None, "unfrozen bundle declaration")
    root = root.absolute()
    require(all(not p.is_symlink() for p in (root, *root.parents)), "symlink root refused")
    root = root.resolve()
    require(root.is_dir(), "workspace root must exist")
    receipt["root"] = str(root)
    archive = checked(root, ARCHIVE_OWNER)
    installer = checked(root, INSTALL_OWNER)
    bundle = checked(root, BUNDLE)
    baseline = {"archiveOwner": identity(archive, expected=ARCHIVE_SHA),
                "installOwner": identity(installer, expected=INSTALL_SHA),
                "bundleFiles": exact_tree(bundle, BUNDLE_FILES)}
    require(baseline["bundleFiles"]["BUNDLE.json"]["sha256"] == BUNDLE_SHA,
            "bundle authority mismatch")
    receipt["inputs"] = baseline
    receipt["before"] = {}
    for name, _, _, _ in OUTPUTS:
        p = checked(root, name)
        receipt["before"][name] = identity(p) if p.exists() else None
    python = str(Path(sys.executable).resolve())
    receipt["python"] = {"path": python, "sha256": digest(Path(python).read_bytes())}

    def stable():
        require(identity(archive, expected=ARCHIVE_SHA) == baseline["archiveOwner"], "archive owner changed")
        require(identity(installer, expected=INSTALL_SHA) == baseline["installOwner"], "install owner changed")
        require(exact_tree(bundle, BUNDLE_FILES) == baseline["bundleFiles"], "bundle changed")

    def archive_call(mode, destination=None):
        stable()
        argv = [python, "-I", "-S", str(archive), mode, "--bundle", str(bundle)]
        if destination is not None:
            argv.extend(["--output", str(destination)])
        result = run(argv, receipt)
        stable()
        require(result.get("schema") == "synexia.evidence.bundle-integrity/1"
                and result.get("status") == "ARCHIVE_INTEGRITY_VERIFIED"
                and result.get("bundleIndexSHA256") == BUNDLE_SHA
                and result.get("retainedPaths") == 3
                and result.get("reconstructed") is (destination is not None)
                and result.get("proofGatesExecuted") is False, "archive receipt mismatch")

    archive_call("verify")
    with tempfile.TemporaryDirectory(prefix="m3-immutable-inputs-") as temporary:
        extracted = Path(temporary) / "extracted"
        archive_call("extract", extracted)
        receipt["reconstructed"] = exact_tree(extracted, LOGICAL)
        plan = checked(extracted, "replay/plan.json")
        for mode in ("apply", "check"):
            stable()
            require(exact_tree(extracted, LOGICAL) == receipt["reconstructed"], "extracted input changed")
            result = run([python, "-I", "-S", str(installer), mode,
                          "--plan", str(plan), "--root", str(root)], receipt)
            stable()
            require(exact_tree(extracted, LOGICAL) == receipt["reconstructed"], "extracted input changed")
            require(result.get("recipe") == RECIPE_ID and result.get("plan_sha256") == PLAN_SEAL
                    and result.get("state") == "after" and result.get("files") == 2
                    and result.get("writes") in ((0, 2) if mode == "apply" else (0,)),
                    "sealed-install receipt mismatch")
        receipt["after"] = {name: identity(checked(root, name), size, sha)
                            for name, _, size, sha in OUTPUTS}
    receipt["status"] = "IMMUTABLE_INPUTS_PREPARED"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, required=True)
    args = parser.parse_args()
    receipt = {"schema": "m3.immutable-inputs.preparation/1", "status": "REFUSED",
               "commands": [], "proofGatesExecuted": False, "productRuntimeQualified": False}
    code = 0
    try:
        prepare(args.root, receipt)
    except (Refusal, OSError, ValueError, KeyError, TypeError) as error:
        code = 2
        receipt["exception"] = {"type": type(error).__name__, "message": str(error)}
    print(json.dumps(receipt, sort_keys=True))
    return code


if __name__ == "__main__":
    raise SystemExit(main())
