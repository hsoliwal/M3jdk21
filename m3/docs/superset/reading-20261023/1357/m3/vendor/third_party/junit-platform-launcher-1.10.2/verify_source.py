#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
# Copyright 2026 Hitesh Soliwal and contributors
# Modified 2026: resolve repository paths and report the existing coordinate/source-jar schema.
=======
# Modified 2026 by Hitesh Soliwal and contributors: resolve repository-relative source seals.
>>>>>>> 23acf33fac72fc29ecbf9e252c8e233b893d04f1
"""Check the pinned, unchanged donor and its optional built legal resources."""
import hashlib
import json
from pathlib import Path
import sys
import zipfile


def main():
    owner = Path(__file__).resolve().parent
    root = owner.parents[3]  # PROVENANCE paths are repository-relative.
    manifest = json.loads((owner / "PROVENANCE.json").read_text(encoding="utf-8"))
    for item in manifest["files"]:
        source = root / item["repository"]
        actual = hashlib.sha256(source.read_bytes()).hexdigest()
        if actual != item["sha256"]:
            raise SystemExit("Donor bytes changed: " + item["repository"])
    expected = {
        root / item["repository"]
        for item in manifest["files"]
        if item["kind"] == "unmodified-production"
    }
    if set((owner / "src/main/java").rglob("*.java")) != expected:
        raise SystemExit("Production donor inventory changed")
    receipt = {"coordinate": manifest["coordinate"],
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
               "sources_jar_sha256": manifest["sources_jar_sha256"],
               "exact_files": len(manifest["files"]),
=======
               "sources_jar_sha256": manifest["sources_jar_sha256"], "exact_files": len(manifest["files"]),
>>>>>>> 23acf33fac72fc29ecbf9e252c8e233b893d04f1
               "exact_production_files": len(expected)}
    if len(sys.argv) > 1:
        artifact = Path(sys.argv[1])
        with zipfile.ZipFile(artifact) as jar:
            for name in ("LICENSE", "THIRD_PARTY_NOTICES.md", "PROVENANCE.json"):
                if jar.read("META-INF/third-party/junit-platform-launcher-1.10.2/" + name) != (owner / (manifest["license_file"] if name == "LICENSE" else name)).read_bytes():
                    raise SystemExit("Packaged legal/provenance bytes changed: " + name)
            if "M3jdk21-Source-Inline: no upstream Automatic-Module-Name" not in jar.read("META-INF/MANIFEST.MF").decode():
                raise SystemExit("Automatic module identity changed")
        receipt["artifact_sha256"] = hashlib.sha256(artifact.read_bytes()).hexdigest()
    print(json.dumps(receipt, sort_keys=True))


if __name__ == "__main__":
    main()
