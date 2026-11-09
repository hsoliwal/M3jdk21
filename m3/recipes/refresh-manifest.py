#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
"""Maintainer-only: regenerate pins after reviewing source changes; not an apply step."""
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
UPSTREAM = "https://github.com/openjdk/jdk21"
UPSTREAM_COMMIT = "890adb6410dab4606a4f26a942aed02fb2f55387"
SOURCE_FOLDERS = ("m3/core", "m3/docs", "m3/lexicon", "m3/compatibility")
EXPLICIT_FILES = (
    "m3/README.md",
    "m3/LICENSE",
    "m3/NOTICE",
    "m3/build.sh",
    "m3/build-openjdk.sh",
    "m3/verify-shared-mapping.py",
    "m3/.gitignore",
    "m3/recipes/apply.py",
    "m3/recipes/test_recipe.py",
    "m3/recipes/refresh-manifest.py",
    "m3/evidence/constructor-pool-retirement.md",
    "m3/evidence/build-result.json",
    "m3/runtime-integration/FOSS_REUSE_DECISION.tsv",
)
RUNTIME_FILES = tuple(
    "src/java.base/share/classes/java/lang/" + name + ".java"
    for name in ("String", "StringLatin1", "StringUTF16", "StringCoding")
) + tuple(
    "src/hotspot/share/" + name
    for name in (
        "classfile/javaClasses.hpp",
        "classfile/javaClasses.inline.hpp",
        "opto/library_call.cpp",
        "prims/jni.cpp",
    )
)


def sha256(root, paths):
    return {
        path: hashlib.sha256((root / path).read_bytes()).hexdigest()
        for path in sorted(paths)
    }


def collect_new_files(root):
    paths = set(EXPLICIT_FILES)
    for folder in SOURCE_FOLDERS:
        paths.update(
            str(path.relative_to(root))
            for path in (root / folder).rglob("*")
            if path.is_file()
        )
    return sorted(paths)


def build_manifest(root):
    return {
        "schema": 1,
        "upstream": UPSTREAM,
        "commit": UPSTREAM_COMMIT,
        "unchanged_runtime_files": sha256(root, RUNTIME_FILES),
        "new_files": sha256(root, collect_new_files(root)),
    }


def main():
    target = ROOT / "m3/recipes/manifest.json"
    target.write_text(json.dumps(build_manifest(ROOT), indent=2) + "\n")


if __name__ == "__main__":
    main()
