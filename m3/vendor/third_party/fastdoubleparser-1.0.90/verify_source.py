#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Check the pinned donor, its recorded overlays and its optional built legal resources."""
import hashlib
import importlib.util
import json
from pathlib import Path
import sys
import zipfile

# Kinds compiled from src/main/java: sources-jar bytes, upstream-tag bytes, recorded M3 overlays.
PRODUCTION_KINDS = ("unmodified-production", "upstream-base-source-at-tag", "m3-overlay-source")
OVERLAY_KIND = "m3-overlay-source"
RETAINED_KIND = "unmodified-production-superseded-by-m3-overlay"


def sha256(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def check_overlays(root, owner, manifest):
    """Each overlay is the pinned recipe's output of a retained, pinned upstream original."""
    files = {item["repository"]: item for item in manifest["files"]}
    overlays = [item for item in manifest["files"] if item["kind"] == OVERLAY_KIND]
    recorded = json.loads((owner / "OVERLAY_SOURCES.json").read_text(encoding="utf-8")).get("m3_overlays", [])
    if sorted((item["repository"], item["sha256"]) for item in overlays) != sorted(
            (item["repository"], item["sha256"]) for item in recorded):
        raise SystemExit("M3 overlays differ between PROVENANCE.json and OVERLAY_SOURCES.json")
    sys.dont_write_bytecode = True
    for item in overlays:
        base = files.get(item["base_repository"])
        if base is None or base["kind"] != RETAINED_KIND or base["sha256"] != item["base_sha256"]:
            raise SystemExit("M3 overlay base is not a retained pinned original: " + item["repository"])
        recipe = root / item["recipe"]
        if sha256(recipe) != item["recipe_sha256"]:
            raise SystemExit("M3 overlay recipe changed: " + item["recipe"])
        spec = importlib.util.spec_from_file_location("m3_donor_overlay_recipe", recipe)
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        if module.overlay((root / base["repository"]).read_bytes(), item["upstream"]) != (
                root / item["repository"]).read_bytes():
            raise SystemExit("M3 overlay is not its recipe's output: " + item["repository"])
    return len(overlays)


def main():
    owner = Path(__file__).resolve().parent
    root = owner.parents[3]  # repository root: the pins are m3/vendor/third_party/<donor>/...
    manifest = json.loads((owner / "PROVENANCE.json").read_text(encoding="utf-8"))
    for item in manifest["files"]:
        source = root / item["repository"]
        actual = hashlib.sha256(source.read_bytes()).hexdigest()
        if actual != item["sha256"]:
            raise SystemExit("Donor bytes changed: " + item["repository"])
    expected = {
        root / item["repository"]
        for item in manifest["files"]
        if item["kind"] in PRODUCTION_KINDS
    }
    if set((owner / "src/main/java").rglob("*.java")) != expected:
        raise SystemExit("Production donor inventory changed")
    overlays = check_overlays(root, owner, manifest)
    receipt = {"revision": manifest.get("revision", manifest["version"]), "exact_files": len(manifest["files"]),
               "exact_production_files": len(expected), "m3_overlays": overlays}
    if len(sys.argv) > 1:
        artifact = Path(sys.argv[1])
        with zipfile.ZipFile(artifact) as jar:
            for name in ("LICENSE", "THIRD_PARTY_NOTICES.md", "PROVENANCE.json"):
                if jar.read("META-INF/third-party/fastdoubleparser-1.0.90/" + name) != (owner / name).read_bytes():
                    raise SystemExit("Packaged legal/provenance bytes changed: " + name)
            if "Automatic-Module-Name: ch.randelshofer.fastdoubleparser" not in jar.read("META-INF/MANIFEST.MF").decode():
                raise SystemExit("Automatic module identity changed")
        receipt["artifact_sha256"] = hashlib.sha256(artifact.read_bytes()).hexdigest()
    print(json.dumps(receipt, sort_keys=True))


if __name__ == "__main__":
    main()
