#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Check the pinned, unchanged donor and its optional built legal resources."""
import hashlib
import json
from pathlib import Path
import sys
import zipfile


def main():
    owner = Path(__file__).resolve().parent
    root = owner.parent.parent
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
    receipt = {"revision": manifest["revision"], "exact_files": len(manifest["files"]),
               "exact_production_files": len(expected)}
    if len(sys.argv) > 1:
        artifact = Path(sys.argv[1])
        with zipfile.ZipFile(artifact) as jar:
            for name in ("LICENSE", "THIRD_PARTY_NOTICES.md", "PROVENANCE.json"):
                if jar.read("META-INF/third-party/slf4j-api-1.7.36/" + name) != (owner / name).read_bytes():
                    raise SystemExit("Packaged legal/provenance bytes changed: " + name)
            if "Automatic-Module-Name: org.slf4j" not in jar.read("META-INF/MANIFEST.MF").decode():
                raise SystemExit("Automatic module identity changed")
        receipt["artifact_sha256"] = hashlib.sha256(artifact.read_bytes()).hexdigest()
    print(json.dumps(receipt, sort_keys=True))


if __name__ == "__main__":
    main()
