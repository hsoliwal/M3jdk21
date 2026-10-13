#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Compose an opt-in M3SDK developer+vendor image from already reviewed JMOD inputs.

Reuses the existing developer module-pack builder and the exact recovered vendor JMOD
runtime probe. No source, JAVA_HOME, donor JMOD, or occupied output is overwritten.
A qualified candidate image is not a release, JEP backport or JDK spec-compatibility proof.
"""
import argparse
import hashlib
import json
import os
import sys
from pathlib import Path

import build_pack
import developer_tools

_TOOLING = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(_TOOLING / "jmod-recipes" / "tools"))
import sdk_image_probe  # noqa: E402

HOST_JAVA_FLAGS = ("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS")
TOOLING_ROOTS = ("com.m3tooling.modulepack", "com.m3pack.diagnostics",
                 "jdk.jcmd", "jdk.jdeps")
SOURCE_REPO = Path(__file__).resolve().parents[3]


def require_safe_output(out, home, vendor):
    output = Path(out).absolute()
    resolved = output.resolve()
    if (output.exists() or resolved.is_relative_to(home)
            or resolved.is_relative_to(SOURCE_REPO)
            or resolved.is_relative_to(vendor)):
        raise ValueError("M3SDK_OUTPUT_OCCUPIED_OR_UNSAFE")
    return output


def inspect_vendor_inputs(home, vendor, pack):
    """Check complete JMOD set, identity and collisions before creating an output."""
    source = Path(vendor).resolve(strict=True)
    if not source.is_dir() or source.is_relative_to(home):
        raise ValueError("M3SDK_VENDOR_SOURCE_INVALID")
    pack_file = Path(pack).resolve(strict=True)
    modules = sdk_image_probe.read_pack(pack_file.read_text(encoding="utf-8"), minimum=87)
    standard = frozenset(path.stem for path in (home / "jmods").glob("*.jmod"))
    reserved = standard | frozenset(TOOLING_ROOTS)
    if modules & reserved:
        raise ValueError("M3SDK_VENDOR_MODULE_NAME_COLLISION")
    expected = frozenset(module + ".jmod" for module in modules)
    actual = frozenset(item.name for item in source.glob("*.jmod"))
    if actual != expected:
        raise ValueError("M3SDK_VENDOR_JMOD_SET_DRIFT")

    result = []
    for module in sorted(modules):
        path = source / (module + ".jmod")
        if path.is_symlink() or not path.is_file():
            raise ValueError("M3SDK_VENDOR_UNSAFE_JMOD:" + module)
        result.append((module, path, build_pack.digest(path)))
    return tuple(result), pack_file, source


def validate_environment(home):
    if any(os.environ.get(key) for key in HOST_JAVA_FLAGS):
        raise ValueError("M3SDK_UNCONTROLLED_VM_OPTIONS")
    if build_pack.platform.system() != "Linux":
        raise ValueError("M3SDK_DEVELOPER_PROOF_LINUX_ONLY")
    return build_pack.java_home(Path(home))


def vendor_root(entries):
    """Domain-separated SHA-256 of module names and independently hashed JMOD bytes."""
    digest = hashlib.sha256(b"M3SDK_VENDOR_JMODS_V1\0")
    for name, _, content in entries:
        digest.update(name.encode("ascii"))
        digest.update(b"\0")
        digest.update(content.encode("ascii"))
        digest.update(b"\n")
    return digest.hexdigest()


def require_unchanged(entries):
    for module, source, expected in entries:
        if build_pack.digest(source) != expected:
            raise ValueError("M3SDK_VENDOR_JMOD_MUTATED:" + module)


def compose(jdk, vendor_jmods, vendor_pack, out):
    """A separate candidate output for developer tools, combined image and API proof."""
    home = validate_environment(jdk)
    sources, pack, vendor = inspect_vendor_inputs(home, vendor_jmods, vendor_pack)
    output = require_safe_output(out, home, vendor)

    # The canonical module-pack builder owns developer toolchain JMOD assembly/proofs.
    output.mkdir(parents=True, exist_ok=False)
    stage = output / "developer-stage"
    build_pack.main(["--jdk", str(home), "--profile", "developer", "--out", str(stage)])
    receipt = json.loads((stage / "RECEIPT.json").read_text(encoding="utf-8"))
    if (receipt.get("profile") != "developer"
            or receipt.get("m3jdk21_product_acceptance") is not False
            or receipt.get("developer", {}).get("status") != "LOCAL_DEVELOPER_TOOLS_PROVEN"):
        raise ValueError("M3SDK_DEVELOPER_STAGE_PROOF_MISSING")

    require_unchanged(sources)
    roots = sorted(set(("java.se", *TOOLING_ROOTS, *developer_tools.DEVELOPER_ROOTS))
                   | {name for name, _, _ in sources})
    path = os.pathsep.join((str(home / "jmods"), str(stage / "jmods"), str(vendor)))
    logs = output / "logs"
    logs.mkdir()
    image = output / "image"
    build_pack.command(
        [home / "bin/jlink", "--module-path", path, "--add-modules", ",".join(roots),
         "--strip-debug", "--no-header-files", "--no-man-pages", "--output", image],
        logs / "combined-jlink.log", 600)
    require_unchanged(sources)

    # This independently compiles and runs the real RE2J+Guava code in the new linked image.
    smoke = sdk_image_probe.prove(image, pack, output / "runtime-proof")
    if (smoke.get("status") != "LOCAL_SDK_IMAGE_RUNTIME_SMOKE_PROVEN"
            or smoke.get("product_accepted") is not False):
        raise ValueError("M3SDK_RUNTIME_API_PROOF_MISSING")

    result = {
        "schema": "M3SDK_COMPOSED_DEVELOPER_VENDOR_IMAGE_V1",
        "status": "LOCAL_COMPOSITE_SDK_CANDIDATE_PROVEN",
        "jdk_release_sha256": build_pack.digest(home / "release"),
        "vendor_pack_sha256": build_pack.digest(pack),
        "vendor_jmods_root_sha256": vendor_root(sources),
        "vendor_jmods": len(sources),
        "jlink_roots": roots,
        "developer_stage_proof": receipt.get("developer", {}).get("status"),
        "runtime_smoke": smoke["status"],
        "java21_spec_compatibility": "NOT_PROVEN_BY_THIS_CANDIDATE",
        "full_jtreg": "NOT_EXECUTED",
        "product_accepted": False,
    }
    (output / "SDK_CANDIDATE_RECEIPT.json").write_text(
        json.dumps(result, sort_keys=True, indent=2) + "\n", encoding="utf-8")
    return result


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--jdk", type=Path, required=True)
    parser.add_argument("--vendor-jmods", type=Path, required=True)
    parser.add_argument("--vendor-pack", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args(argv)
    print(json.dumps(
        compose(args.jdk, args.vendor_jmods, args.vendor_pack, args.out),
        sort_keys=True))


if __name__ == "__main__":
    main()
