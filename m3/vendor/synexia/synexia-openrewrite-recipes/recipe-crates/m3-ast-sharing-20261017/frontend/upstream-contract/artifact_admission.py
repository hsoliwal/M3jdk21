#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors
"""Admit an exact artifact or a fresh build receipt bound to sealed compiler sources.

This checks the local builder's source/digest receipt. The linked-image proof
separately checks the artifact's four-owner class closure and executes its APIs.
It is not a signature or a substitute for running the source-qualified builder.
"""
from __future__ import annotations

import hashlib
import json
from pathlib import Path
import re

HERE = Path(__file__).resolve().parent
PATCH_SHA = "3f50d59cca1cf7cf2811c0bb1da9314b71240c7f81f37854ed6fc021f03681b4"
COMPILER_RESOURCE = HERE.parent / "src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-ast-frontend-compiler-20261017"
COMPILER_MANIFEST_SHA = "94420df2d3d252d6c4bd1b52469a55b52609bf075fef8f0d2e7550507ed0011f"
COMPILER_PREFIX = "src/jdk.compiler/share/classes/"
COMPILER_OWNERS = {
    "com/sun/source/util/M3AST.java",
    "com/sun/tools/javac/m3/M3ASTPC.java",
    "com/sun/tools/javac/m3/M3PrimitiveImageCompiler.java",
    "com/sun/tools/javac/main/JavaCompiler.java",
}


def file_sha(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def require(condition: bool, message: str) -> None:
    if not condition:
        raise ValueError(message)


def unique_object(pairs: list[tuple[str, object]]) -> dict[str, object]:
    result = {}
    for key, value in pairs:
        require(key not in result, "duplicate artifact receipt key: " + key)
        result[key] = value
    return result


def compiler_sources(resource: Path = COMPILER_RESOURCE) -> dict[str, str]:
    manifest = resource / "manifest.tsv"
    require(file_sha(manifest) == COMPILER_MANIFEST_SHA, "sealed compiler manifest drift")
    result = {}
    for row in manifest.read_text(encoding="utf-8").splitlines():
        columns = row.split("\t")
        require(len(columns) == 4, "invalid sealed compiler manifest row")
        path, _, expected, template = columns
        require(path.startswith(COMPILER_PREFIX), "compiler source outside sealed prefix")
        relative = path[len(COMPILER_PREFIX):]
        require(relative in COMPILER_OWNERS and relative not in result, "compiler source owner drift")
        require(Path(template).name == template, "compiler template outside resource directory")
        require(file_sha(resource / template) == expected, "sealed compiler template drift: " + relative)
        result[relative] = expected
    require(result.keys() == COMPILER_OWNERS, "incomplete four-source compiler manifest")
    return result


def admit_patch(patch: Path, artifact_receipt: Path | None = None,
                resource: Path = COMPILER_RESOURCE) -> dict[str, object]:
    actual = file_sha(patch)
    if artifact_receipt is None:
        require(actual == PATCH_SHA, "unqualified compiler patch bytes; use the source-qualified builder receipt for a fresh build")
        return {"mode": "exact-artifact-pin", "patch_sha256": actual}
    with artifact_receipt.open("rb") as stream:
        data = stream.read(1024 * 1024 + 1)
    require(len(data) <= 1024 * 1024, "artifact receipt exceeds finite admission limit")
    receipt = json.loads(data.decode("utf-8"), object_pairs_hook=unique_object)
    require(type(receipt) is dict, "artifact receipt must be an object")
    require(type(receipt.get("compiler_sources")) is int and receipt["compiler_sources"] == 4,
            "artifact receipt must name exactly four compiler sources")
    expected = compiler_sources(resource)
    require(type(receipt.get("compiler_source_sha256")) is dict and receipt["compiler_source_sha256"] == expected,
            "artifact receipt compiler source pins differ from the sealed manifest")
    require(receipt.get("patch_sha256") == actual, "artifact receipt patch digest does not match supplied bytes")
    result = {"mode": "sealed-build-receipt", "receipt_sha256": hashlib.sha256(data).hexdigest(),
              "compiler_manifest_sha256": COMPILER_MANIFEST_SHA,
              "compiler_source_sha256": expected, "patch_sha256": actual}
    if "plugin_sha256" in receipt:
        require(type(receipt["plugin_sha256"]) is str and re.fullmatch(r"[0-9a-f]{64}", receipt["plugin_sha256"]) is not None,
                "invalid frontend artifact digest in builder receipt")
        result["plugin_sha256"] = receipt["plugin_sha256"]
    return result
