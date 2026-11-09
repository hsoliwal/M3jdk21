#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Stage exact reviewed Git test prerequisites without changing the product checkout."""

import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import tarfile

REVISION = "3bbb4712fb20adba1405741db8457164b9f436f5"
ROOTS = (
    "test/jdk/TEST.ROOT", "test/jdk/TEST.groups", "test/jdk/java/lang/String",
    "test/jdk/java/util/regex", "test/jdk/java/util/StringJoiner",
    "test/jdk/lib/testlibrary/java/lang", "test/jdk/lib/testlibrary/bootlib",
    "test/lib", "test/jtreg-ext/requires", "src/java.base/share/data/unicodedata",
)


def digest(data):
    return hashlib.sha256(data).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo", type=Path, required=True)
    parser.add_argument("--archive", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    repo = args.repo.resolve(strict=True)
    archive = args.archive.resolve(strict=True)
    output = args.output.resolve()
    if output.exists():
        raise ValueError("fixture output must be absent")
    tree = subprocess.check_output(
        ["git", "-C", str(repo), "ls-tree", "-rz", "--full-tree", REVISION,
         "--", *ROOTS])
    expected = {}
    for item in tree.split(b"\0"):
        if not item:
            continue
        metadata, raw_path = item.split(b"\t", 1)
        mode, kind, blob = metadata.decode("ascii").split()
        path = raw_path.decode("utf-8")
        if kind != "blob" or mode not in {"100644", "100755"}:
            raise ValueError("unexpected prerequisite kind or mode: " + path)
        expected[path] = (mode, blob)
    admitted = {}
    with tarfile.open(archive, mode="r:") as source:
        if source.pax_headers.get("comment") != REVISION:
            raise ValueError("archive is not the exact reviewed Git revision")
        for member in source:
            relative = Path(member.name)
            if relative.is_absolute() or ".." in relative.parts:
                raise ValueError("unsafe archive member")
            if member.isdir():
                continue
            if not member.isfile() or member.name in admitted:
                raise ValueError("unexpected or duplicate archive member")
            data = source.extractfile(member).read()
            blob = hashlib.sha1(b"blob " + str(len(data)).encode("ascii")
                                + b"\0" + data).hexdigest()
            mode, wanted = expected[member.name]
            if blob != wanted or (member.mode & 0o111) != (int(mode, 8) & 0o111):
                raise ValueError("archive member does not match Git: " + member.name)
            admitted[member.name] = (data, mode, blob)
    if admitted.keys() != expected.keys():
        raise ValueError("archive does not cover the exact prerequisite closure")
    root_bytes = admitted["test/jdk/TEST.ROOT"][0]
    if b"requiredVersion=7.2+1" not in root_bytes:
        raise ValueError("unexpected jtreg requirement")
    rows = []
    for relative, (data, mode, blob) in sorted(admitted.items()):
        destination = output / "source" / relative
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_bytes(data)
        destination.chmod(int(mode, 8) & 0o777)
        rows.append((relative, mode, blob, digest(data), str(len(data))))
    inventory = "".join("\t".join(row) + "\n" for row in rows).encode("utf-8")
    (output / "SOURCE_INVENTORY.tsv").write_bytes(inventory)
    receipt = {
        "status": "EXACT_GIT_PREREQUISITES_STAGED",
        "revision": REVISION,
        "repository": str(repo),
        "archive": str(archive),
        "archive_sha256": digest(archive.read_bytes()),
        "source_inventory_sha256": digest(inventory),
        "files": len(rows),
        "bytes": sum(len(value[0]) for value in admitted.values()),
        "roots": list(ROOTS),
        "jtreg_required_version": "7.2+1",
        "candidate_overlays": [],
        "runtime_tests": "NOT_RUN",
    }
    (output / "PREREQUISITES.json").write_text(json.dumps(receipt, indent=2) + "\n")
    print(json.dumps(receipt, indent=2))


if __name__ == "__main__":
    main()
