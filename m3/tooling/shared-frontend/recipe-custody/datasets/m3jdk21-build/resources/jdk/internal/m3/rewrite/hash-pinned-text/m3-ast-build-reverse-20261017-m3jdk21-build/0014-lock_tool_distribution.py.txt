#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Inventory a complete supplied tool tree into an offline optional-pack lock.

This records inputs; it does not download, replace upstream launchers, certify a
source build, or imply AST reuse. Review the resulting lock before assembly.
"""
import argparse
import hashlib
import json
from pathlib import Path

from tool_packs import digest

REQUIRED = {
    "maven": ("bin/mvn", "bin/mvn.cmd", "bin/m2.conf", "conf/settings.xml", "LICENSE", "NOTICE"),
    "gradle": ("bin/gradle", "bin/gradle.bat", "LICENSE", "NOTICE"),
    "ant": ("bin/ant", "bin/ant.bat", "lib/ant.jar", "lib/ant-launcher.jar", "LICENSE", "NOTICE"),
}
COMMANDS = {"maven": "m3-mvn", "gradle": "m3-gradle", "ant": "m3-ant"}
ROOTS = ("java.se", "jdk.compiler", "jdk.crypto.ec", "jdk.unsupported", "jdk.zipfs")


def distribution_pack(tool, distribution, version, source, license_name, module_roots=(), jmods=()):
    """Pin every supplied file and mode while retaining the original relative layout."""
    distribution = Path(distribution).resolve(strict=True)
    if not distribution.is_dir():
        raise ValueError("Tool distribution must be a directory")
    for relative in REQUIRED[tool]:
        if not (distribution / relative).is_file():
            raise ValueError("Missing distribution entry: " + relative)
    if not any((distribution / "lib").glob("*.jar")):
        raise ValueError("A real tool library payload is required")
    if tool == "maven" and not any((distribution / "boot").glob("plexus-classworlds-*.jar")):
        raise ValueError("Maven Classworlds bootstrap JAR is required")
    if tool == "gradle" and not (any((distribution / "lib").glob("gradle-launcher-*.jar"))
                                  or any((distribution / "lib").glob("gradle-gradle-cli-main-*.jar"))):
        raise ValueError("Gradle distribution bootstrap JAR is required")
    assets, identity = [], []
    for path in sorted(distribution.rglob("*")):
        if path.is_symlink():
            raise ValueError("Distribution symlink requires explicit packaging support: " + str(path))
        if path.is_dir():
            continue
        if not path.is_file():
            raise ValueError("Unsupported non-file distribution entry: " + str(path))
        relative = path.relative_to(distribution).as_posix()
        checksum, executable = digest(path), bool(path.stat().st_mode & 0o111)
        assets.append({"file": str(path), "target": "lib/m3-tools/" + tool + "/" + relative,
                       "sha256": checksum, "executable": executable})
        identity.append(relative + "\t" + checksum + "\t" + ("755" if executable else "644") + "\n")
    owner = Path(__file__).resolve().parent
    for suffix in ("", ".cmd"):
        launcher = owner / "build-tools/launchers" / (COMMANDS[tool] + suffix)
        assets.append({"file": str(launcher), "target": "bin/" + launcher.name,
                       "sha256": digest(launcher), "executable": not suffix})
    config = owner / "build-tools/config" / (tool + "-m3ast." + {"maven": "xml", "gradle": "init.gradle", "ant": "xml"}[tool])
    assets.append({"file": str(config), "target": "conf/m3-tools/" + config.name,
                   "sha256": digest(config), "executable": False})
    return {
        "name": tool, "version": version, "source": source, "license": license_name,
        "roots": sorted(set(ROOTS).union(module_roots)),
        "modules": [{"module": Path(path).stem, "file": str(Path(path).resolve(strict=True)),
                     "sha256": digest(Path(path))} for path in sorted(jmods)],
        "assets": assets,
        "distribution": {
            "file_count": len(identity),
            "tree_sha256": hashlib.sha256("".join(identity).encode("utf-8")).hexdigest(),
            "source_build_acceptance": "NOT_ESTABLISHED_BY_INPUT_INVENTORY",
        },
    }


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--tool", choices=tuple(REQUIRED), required=True)
    parser.add_argument("--distribution", type=Path, required=True)
    parser.add_argument("--version", required=True)
    parser.add_argument("--source", required=True, help="Reviewed source/distribution provenance")
    parser.add_argument("--license", required=True, dest="license_name")
    parser.add_argument("--module-root", action="append", default=[])
    parser.add_argument("--jmod", type=Path, action="append", default=[])
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args(argv)
    if args.out.resolve().is_relative_to(args.distribution.resolve()):
        raise ValueError("Input distribution must remain read-only")
    pack = distribution_pack(args.tool, args.distribution, args.version, args.source,
                             args.license_name, args.module_root, args.jmod)
    with args.out.open("x", encoding="utf-8", newline="\n") as target:
        json.dump({"schema_version": 1, "packs": [pack]}, target, indent=2, sort_keys=True)
        target.write("\n")
    print("M3_TOOL_INPUT_LOCK_WRITTEN", args.out)


if __name__ == "__main__":
    main()
