#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
"""Fail-closed V5 custody over the immutable V3 and V4 SDK source receipts.

Retain every V4 source check, including the 33-file historical composition,
provider SHA, prior verification artifacts and arg-output no-alias checks.
Only three original V4 successor postimages may advance, with both old
V4 after-pins and new live postimage pins checked at full byte/SHA/Git blob
precision. Never mutate or silently re-pin V4 itself.
"""
from __future__ import annotations

import argparse
import hashlib
import importlib.util
import json
from pathlib import Path
import re
import sys

HERE = Path(__file__).resolve().parent
HISTORICAL = HERE.parent / "owned-apache-multivalue-20261024"
PREDECESSOR = HISTORICAL / "verify_source_binding_successor_v4.py"
V4_SHA256 = "5917743ab7b2bf6348b5ea73ec3ff5d9e1777475594ef4f520b8851a41ca05e0"
RECEIPT_NAME = "SOURCE_BINDING_SUCCESSOR.json"
RECEIPT_BYTES = 3397
RECEIPT_SHA256 = "fa889f55f2cd577eb38060186df603cb306d5dc7c13ac6fffcb86423b6a99d86"
SUCCESSOR_PATHS = (
    "m3/collections/src/main/java/jdk/m3/collections/M3ArrayList.java",
    "m3/collections/src/main/java/jdk/m3/collections/M3LongRangeSet.java",
    "m3/collections/src/main/native/collections/m3_index_mask_compactor.c",
)


def previous_module():
    if not PREDECESSOR.is_file() or PREDECESSOR.is_symlink():
        raise ValueError("missing or symlinked original V4 verifier")
    content = PREDECESSOR.read_bytes()
    if hashlib.sha256(content).hexdigest() != V4_SHA256:
        raise ValueError("original V4 verifier hash drift")
    spec = importlib.util.spec_from_file_location("m3_verified_original_v4", PREDECESSOR)
    if spec is None or spec.loader is None:
        raise ValueError("cannot import original V4 verifier")
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


def run(root: Path, jar: Path | None = None, args_file: Path | None = None) -> dict:
    root = root.resolve()
    v4 = previous_module()
    raw = v4.checked(HERE / RECEIPT_NAME)
    if len(raw) != RECEIPT_BYTES or v4.sha256(raw) != RECEIPT_SHA256:
        raise ValueError("sealed V5 receipt differs")
    receipt = json.loads(raw)
    if (receipt.get("schema") != "M3_COLLECTIONS_SDK_SOURCE_BINDING_SUCCESSOR_V5"
            or receipt.get("reviewScope") != "ONLY_THREE_V4_SUCCESSOR_CHANGES_NO_SOURCE_MUTATION"):
        raise ValueError("invalid V5 custody schema/scope")

    predecessor = receipt["predecessor"]
    original_v4_raw = v4.pinned(root, predecessor)
    original_v4 = json.loads(original_v4_raw)
    v4.pinned(root, receipt["predecessorVerifier"])
    if (len(original_v4_raw) != v4.RECEIPT_BYTES
            or v4.sha256(original_v4_raw) != v4.RECEIPT_SHA256):
        raise ValueError("original V4 receipt has changed")

    v3 = json.loads(v4.pinned(root, original_v4["predecessor"]))
    v4.pinned(root, original_v4["predecessorVerifier"])
    history = [v4.pinned(root, row) for row in v3["history"]]
    old = json.loads(history[0])
    v2 = json.loads(history[2])
    sources = old["sources"]
    owned = [row for row in sources if "/implementations/" in row["path"]]
    sdk_row = {"path": v3["sdkPath"], **v3["after"]}
    if (len(sources) != v3["oldSourceRows"]
            or len([row for row in sources if row["path"] == v3["sdkPath"]]) != 1
            or len({row["path"] for row in sources}) != len(sources)
            or len(owned) != v3["unchangedOwnedRows"]
            or owned != v2["targets"]):
        raise ValueError("original V3 historical source composition differs")
    original_pins = {row["path"]: row for row in owned + v3["arrayAndNativeOwners"]}
    original_pins[sdk_row["path"]] = sdk_row
    v4_successors = {row["path"]: row for row in original_v4["successors"]}
    if (len(v4_successors) != len(original_v4["successors"])
            or not set(v4_successors) <= set(original_pins)):
        raise ValueError("V4 successor set has changed")

    v5_successors = {row["path"]: row for row in receipt["successors"]}
    if (len(v5_successors) != len(receipt["successors"])
            or tuple(v5_successors) != SUCCESSOR_PATHS
            or not set(v5_successors) <= set(v4_successors)):
        raise ValueError("V5 must be exactly three ordered historical successors")
    for path, row in v4_successors.items():
        if not v4.same_pin({"path": path, **row["before"]}, original_pins[path]):
            raise ValueError("V4 original-before pin differs: " + path)
        if path in v5_successors:
            replacement = v5_successors[path]
            if not v4.same_pin(replacement["before"], row["after"]):
                raise ValueError("V5 previous-image pin differs: " + path)
            if not isinstance(replacement.get("category"), str) or not replacement["category"]:
                raise ValueError("V5 category provenance missing: " + path)
            v4.pinned(root, {"path": path, **replacement["after"]})
        else:
            v4.pinned(root, {"path": path, **row["after"]})
    for path, row in original_pins.items():
        if path not in v4_successors:
            v4.pinned(root, row)
    v4.pinned(root, v3["before"])

    sdk = v4.checked(v4.input_path(root, v3["sdkPath"]))
    if len(re.findall(rb"\bpublic\s+static\b", sdk)) < (
            v3["preservedOldPublicStaticMethods"] + len(v3["addedFactories"])):
        raise ValueError("SDK public declaration count fell")
    for name in v3["addedFactories"]:
        declaration = rb"public\s+static\s+[^;{=]*\b" + name.encode("ascii") + rb"\s*\("
        if len(re.findall(declaration, sdk)) != 1:
            raise ValueError("added public factory declaration differs: " + name)
    for row in original_v4["added"] + [original_v4["recipe"]]:
        v4.pinned(root, row)

    if jar is not None:
        provider = old["provider"]
        data = v4.checked(jar)
        if (jar.name != provider["basename"]
                or len(data) != provider["bytes"]
                or v4.sha256(data) != provider["sha256"]):
            raise ValueError("provider custody differs")

    if args_file is not None:
        destination = args_file.resolve()
        protected = [v4.input_path(root, path) for path in original_pins]
        protected += [v4.input_path(root, row["path"]) for row in
                      v3["history"] + original_v4["added"]]
        protected += [v4.input_path(root, row["path"]) for row in
                      (v3["before"], original_v4["predecessor"],
                       original_v4["predecessorVerifier"], original_v4["recipe"],
                       receipt["predecessor"], receipt["predecessorVerifier"])]
        protected += [HERE / RECEIPT_NAME, PREDECESSOR, Path(__file__).resolve()]
        if (args_file.is_symlink() or destination.is_relative_to(root)
                or destination in [path.resolve() for path in protected]
                or (destination.exists() and any(destination.samefile(path)
                    for path in protected))):
            raise ValueError("argument output aliases protected source")
        paths = [str(v4.input_path(root, row["path"]).resolve()).replace("\\", "/")
                 for row in owned]
        args_file.write_text("\n".join('"' + path + '"' for path in paths) + "\n",
                             encoding="utf-8")
    return {"sources": len(sources), "source_custody": "PASS_V5",
            "historical_v3_v4_immutable": True, "v5Successors": len(v5_successors),
            "unchangedV4Successors": len(v4_successors) - len(v5_successors),
            "provider_checked": jar is not None,
            "added_factories": v3["addedFactories"]}


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=HERE.parents[4])
    parser.add_argument("--jar", type=Path)
    parser.add_argument("--args", type=Path)
    args = parser.parse_args()
    print(json.dumps(run(args.root, args.jar, args.args), sort_keys=True))


if __name__ == "__main__":
    main()
