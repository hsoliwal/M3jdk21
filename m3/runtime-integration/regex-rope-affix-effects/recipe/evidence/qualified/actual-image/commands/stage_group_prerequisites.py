#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Materialize exact TEST.groups prerequisites without editing any group definitions."""

import argparse
import hashlib
import json
from pathlib import Path
import tarfile


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ("inventory", "archive", "fixture"):
        parser.add_argument("--" + name, type=Path, required=True)
    args = parser.parse_args()
    inventory = json.loads(args.inventory.read_text())
    entries = inventory["entries"]
    source_root = args.fixture.resolve(strict=True) / "source"
    wanted = {path: entry for path, entry in entries.items() if entry["kind"] == "blob"}
    admitted = {}
    with tarfile.open(args.archive, mode="r:") as archive:
        if archive.pax_headers.get("comment") != inventory["revision"]:
            raise ValueError("group archive revision drift")
        for member in archive:
            if member.isdir():
                continue
            path = Path(member.name)
            if not member.isfile() or path.is_absolute() or ".." in path.parts:
                raise ValueError("unsafe group archive member")
            if member.name not in wanted or member.name in admitted:
                raise ValueError("unrequested or duplicated group source")
            data = archive.extractfile(member).read()
            blob = hashlib.sha1(b"blob " + str(len(data)).encode("ascii") + b"\0" + data).hexdigest()
            if blob != wanted[member.name]["git_object"]:
                raise ValueError("group source Git blob mismatch")
            destination = source_root / path
            if destination.exists():
                raise ValueError("group prerequisite must be absent before materialization")
            admitted[member.name] = data
    if admitted.keys() != wanted.keys():
        raise ValueError("incomplete exact group file closure")
    for path, entry in entries.items():
        relative = Path(path)
        if relative.is_absolute() or ".." in relative.parts:
            raise ValueError("unsafe group directory path")
        if entry["kind"] == "tree":
            (source_root / relative).mkdir(parents=True, exist_ok=True)
        elif entry["kind"] != "blob":
            raise ValueError("unsupported group prerequisite kind")
    for path, data in admitted.items():
        destination = source_root / path
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_bytes(data)
        entries[path]["sha256"] = hashlib.sha256(data).hexdigest()
    inventory["archive_sha256"] = hashlib.sha256(args.archive.read_bytes()).hexdigest()
    inventory["status"] = "EXACT_REFERENCED_FILES_AND_GIT_DIRECTORY_METADATA_STAGED"
    inventory["test_root_and_groups_modified"] = False
    inventory["unselected_group_contents_claimed_complete"] = False
    (args.fixture / "GROUP_PREREQUISITES.json").write_text(json.dumps(inventory, indent=2) + "\n")
    print("GROUP_PREREQUISITES_STAGED files=" + str(len(admitted))
          + " directories=" + str(sum(entry["kind"] == "tree" for entry in entries.values())))


if __name__ == "__main__":
    main()
