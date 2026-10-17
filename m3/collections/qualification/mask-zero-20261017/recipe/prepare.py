#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Seal reviewed inputs; actual SDK Results alone materialize production owners."""
from pathlib import Path
from hashlib import sha1, sha256
import json

HERE = Path(__file__).resolve().parent


def digest(data):
    return sha256(data).hexdigest()


def main():
    pins = json.loads(HERE.joinpath("SOURCE_PINS.json").read_text())
    for owner in pins["providers"]:
        path = HERE / "proof/pinned/com/synexia/rewrite" / Path(owner["path"]).name
        assert digest(path.read_bytes()) == owner["sha256"], owner["path"]
    by_kind = {"java": [], "text": []}
    for source in pins["sources"]:
        path = source["path"]
        before = HERE.joinpath("preimages", path).read_bytes()
        after = HERE.joinpath("templates", path).read_bytes()
        assert digest(before) == source["before_sha256"], path
        assert sha1(b"blob " + str(len(before)).encode() + b"\0" + before).hexdigest() == source["git_blob_sha1"], path
        source["after_sha256"] = digest(after)
        if source["role"] == "UNCHANGED_SELECTOR_CUSTODY":
            assert before == after
        else:
            assert before != after, path
        kind = "java" if path.endswith(".java") else "text"
        by_kind[kind].append((source, after))
    manifests = []
    for kind, entries in by_kind.items():
        resource = HERE / "resources/com/synexia/rewrite" / ("hash-pinned-" + kind) / "m3-mask-zero-20261017"
        resource.mkdir(parents=True, exist_ok=True)
        lines = []
        for i, (source, data) in enumerate(sorted(entries, key=lambda row: row[0]["path"])):
            template = f"{i:03d}-" + Path(source["path"]).name + ".txt"
            resource.joinpath(template).write_bytes(data)
            lines.append("\t".join((source["path"], source["before_sha256"], source["after_sha256"], template)) + "\n")
        resource.joinpath("manifest.tsv").write_text("".join(lines))
        manifests.append({"kind": kind, "path": str(resource.joinpath("manifest.tsv").relative_to(HERE)), "sha256": digest(resource.joinpath("manifest.tsv").read_bytes()), "count": len(entries)})
    pins["manifests"] = manifests
    HERE.joinpath("SOURCE_PINS.json").write_text(json.dumps(pins, indent=2) + "\n")
    print(json.dumps({"source_inputs": len(pins["sources"]), "changed": sum(row["before_sha256"] != row["after_sha256"] for row in pins["sources"]), "manifests": manifests}))


if __name__ == "__main__":
    main()
