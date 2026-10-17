#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors
"""Read-only verification of final public receiver source bytes.

Recipe tests prove emission. This separate check proves that the checkout
contains those final postimages, including overlapping primary/recovery rows.
Maven POMs, Plugin descriptors and the central registry retain their own gates.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path, PurePosixPath

TASK = Path(__file__).resolve().parent.parent
MANIFESTS = {
    "cache/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-ast-cache-target-20261017/manifest.tsv":
        "9c4c435a4908b35451b13acccc38825a05d2d6e1635bc13735e635ad05611a8f",
    "cache/src/main/resources/com/synexia/rewrite/hash-pinned-text/m3-ast-cache-native-20261017/manifest.tsv":
        "3ce4a2fb1b0137bd88aca80c376d933eec18d25122302348a5df4a49bf6e3c5d",
    "frontend/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-ast-frontend-receiver-20261017/manifest.tsv":
        "1a146bddd3924117abfa1e335b3f2e265b59cda4e52abd6d36e3630ffaa0cf0f",
    "frontend/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-ast-frontend-compiler-20261017/manifest.tsv":
        "94420df2d3d252d6c4bd1b52469a55b52609bf075fef8f0d2e7550507ed0011f",
    "frontend/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-ast-frontend-receiver-recovery-20261017/manifest.tsv":
        "48c51c2d5c32aa52f87af8c124044cbdd5de08e61660e406873564bce6e450dd",
    "frontend/hierarchy/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-ast-hierarchy-receiver-20261017/manifest.tsv":
        "3eee1a6a89dff5adbe5daeb80a5331f726c041e7585fdf9e4da1d36acc3fe9c8",
    "frontend/module-wiring/resources/com/synexia/rewrite/hash-pinned-java/m3-ast-wiring-receiver-20261017/manifest.tsv":
        "3376b53ef986308efc1cc2ecc8ebdb0efd44f73c45a323f228af1a648e617d91",
    "frontend/module-wiring/resources/com/synexia/rewrite/hash-pinned-java/m3-ast-ordinary-local-scope-20261017/manifest.tsv":
        "0ddab1b7ecc7a580a62866a08b655409b031bc80b9948f3fe43347b881987713",
}
MODULE_INFO = "src/jdk.compiler/share/classes/module-info.java"
MODULE_INFO_CUSTODY = "frontend/upstream-contract/custody/" + MODULE_INFO + ".txt"
MODULE_INFO_SHA = "ae52d1734570fee7404aeb68b65ef86ea23ad322b72ff0b137c9039c71aafb09"
MODULE_INFO_BLOB = "13ebbaf1db2e8ba997e48c317f4b64bf8f0cfb1d"
PUBLIC_API = "src/jdk.compiler/share/classes/com/sun/source/util/M3AST.java"
PUBLIC_API_SHA = "6a8bccb501a516cca779e71e24ef814ca40825faf92fed744e48f65ad4c1d854"


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def require(condition: bool, message: str) -> None:
    if not condition:
        raise ValueError(message)


def source_path(path: str) -> bool:
    pure = PurePosixPath(path)
    if pure.is_absolute() or ".." in pure.parts or pure.as_posix() != path:
        return False
    return ((path.endswith(".java") and path.startswith(("m3/ports/compiletime/",
            "m3/ports/compiletime-openrewrite/", "m3/vendor/synexia/", "src/jdk.compiler/share/classes/")))
            or path in {"m3/vendor/synexia/synexia-mindex/runtime/native/swap/mindex_swap.c",
                        "m3/vendor/synexia/synexia-mindex/runtime/native/swap/CMakeLists.txt"})


def expected_sources(task: Path = TASK) -> dict[str, dict[str, object]]:
    result = {}
    for relative, manifest_sha in MANIFESTS.items():
        manifest = task / relative
        require(digest(manifest) == manifest_sha, "sealed manifest drift: " + relative)
        seen = set()
        for line in manifest.read_text(encoding="utf-8").splitlines():
            row = line.split("\t")
            require(len(row) == 4, "invalid source manifest row: " + relative)
            path, before, after, template = row
            require(source_path(path) and path not in seen, "invalid or duplicate receiver source path: " + path)
            seen.add(path)
            require(Path(template).name == template, "source template outside sealed resource directory")
            resource = manifest.parent / template
            require(digest(resource) == after, "sealed source template drift: " + path)
            previous = result.get(path)
            if previous is None:
                result[path] = {"sha256": after, "resource": resource, "authorities": [relative]}
            elif previous["sha256"] == after:
                previous["authorities"].append(relative)
            elif previous["sha256"] == before:
                # Only a sealed edge from the current postimage may replace it.
                result[path] = {"sha256": after, "resource": resource,
                                "authorities": previous["authorities"] + [relative]}
            else:
                raise ValueError("conflicting final source authorities: " + path)
    require(result.get(PUBLIC_API, {}).get("sha256") == PUBLIC_API_SHA,
            "preserved upstream public API authority drift")
    custody = task / MODULE_INFO_CUSTODY
    data = custody.read_bytes()
    require(hashlib.sha256(data).hexdigest() == MODULE_INFO_SHA, "upstream module descriptor custody SHA drift")
    blob = hashlib.sha1(b"blob " + str(len(data)).encode("ascii") + b"\0" + data).hexdigest()
    require(blob == MODULE_INFO_BLOB, "upstream module descriptor custody Git blob drift")
    require(MODULE_INFO not in result, "module descriptor unexpectedly entered a product recipe scope")
    result[MODULE_INFO] = {"sha256": MODULE_INFO_SHA, "resource": custody,
                           "authorities": [MODULE_INFO_CUSTODY]}
    return result


def verify_target(target: Path, expected: dict[str, dict[str, object]]) -> dict[str, object]:
    target = target.resolve()
    for relative, item in sorted(expected.items()):
        path = target / relative
        require(path.is_file() and path.resolve().is_relative_to(target), "missing or external receiver source: " + relative)
        actual = digest(path)
        require(actual == item["sha256"], "receiver source drift: " + relative +
                " (expected " + str(item["sha256"]) + ", got " + actual + ")")
    hashes = {path: item["sha256"] for path, item in sorted(expected.items())}
    closure = json.dumps(hashes, sort_keys=True, separators=(",", ":")).encode("utf-8")
    return {"schema": "M3_AST_RECEIVER_SOURCE_READBACK_V1", "status": "passed",
            "sources_verified": len(hashes), "manifest_count": len(MANIFESTS),
            "source_closure_sha256": hashlib.sha256(closure).hexdigest(),
            "module_info_sha256": hashes[MODULE_INFO], "public_api_sha256": hashes[PUBLIC_API],
            "module_metadata_and_registry_checked": False}


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--target", type=Path, required=True, help="Public M3 checkout root; only named sources are read")
    args = parser.parse_args()
    print(json.dumps(verify_target(args.target, expected_sources()), sort_keys=True))


if __name__ == "__main__":
    main()
