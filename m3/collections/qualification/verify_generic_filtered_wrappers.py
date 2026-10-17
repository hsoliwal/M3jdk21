#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Strict source digest admission of existing first-party filtered Set/Map wrappers."""
import hashlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
MANIFEST = Path(__file__).with_name("generic-filtered-wrappers-postimages-20261017.tsv")
EXPECTED = {
    "m3/collections/src/main/java/com/m3/collections/M3CollectionViews.java",
    "m3/collections/src/main/java/com/m3/collections/M3FilteredMaps.java",
    "m3/collections/src/test/java/com/m3/collections/M3CollectionViewsReceiverSelfTest.java",
    "m3/collections/src/test/java/com/m3/collections/M3FilteredMapsSelfTest.java",
    "m3/collections/src/test/java/com/m3/collections/M3GenericWrappersReceiverTest.java",
}
ORIGINAL_RECEIPT_SHA256 = "a261ab7250f4d09ba81b2931b11ef61f32ca02242680fe03a35ffb9826c6c76c"
PURPOSE_RECEIPT = Path(__file__).with_name(
    "generic-filtered-purpose-postimages-20261017.tsv")
ORIGINAL_OWNER = "m3/collections/src/main/java/com/m3/collections/M3FilteredMaps.java"
ORIGINAL_OWNER_SHA256 = "1dff497079e768fda574a349cc521732d69cbacbecc19deba6888d909844f1cb"
EXPECTED_SUCCESSOR = {
    "m3/collections/src/main/java/com/m3/collections/M3FilteredMaps.java": "8e9ec760ea52dcd69b7d137a9b0efc88172dfd76df178219c1f0bd887200799e",
    "m3/collections/src/test/java/com/m3/collections/M3FilteredWrapperSelfTest.java": "b9fabf7e916744765a8b67e629d19e9e6d1e85ba2ae7e2d67a7b3ecbdb878c9d",
    "m3/collections/src/test/java/com/m3/collections/M3FilteredPurposeCompositionTest.java": "baf08471c33e0e75792c3beea04c4c0bab861b04cd3e5763072c8505e6640ac9",
}
PURPOSE_STATE = "MODULE_SOURCE_NOT_INSTALLED_JDK"


def successor_authorized() -> bool:
    # Refuse all unregistered owners, omitted tests, modified/linked manifests.
    if not PURPOSE_RECEIPT.is_file() or PURPOSE_RECEIPT.is_symlink():
        return False
    rows = [line.split("\t") for line in PURPOSE_RECEIPT.read_text(encoding="utf-8").splitlines()
            if line and not line.startswith("#")]
    if len(rows) != len(EXPECTED_SUCCESSOR) or any(len(row) != 3 for row in rows):
        return False
    manifest = {path: (digest, state) for path, digest, state in rows}
    if set(manifest) != set(EXPECTED_SUCCESSOR):
        return False
    for path, digest in EXPECTED_SUCCESSOR.items():
        if manifest[path] != (digest, PURPOSE_STATE):
            return False
        target = ROOT / path
        if not target.is_file() or target.is_symlink():
            return False
        if hashlib.sha256(target.read_bytes()).hexdigest() != digest:
            return False
    return True


def main() -> None:
    if not MANIFEST.is_file() or MANIFEST.is_symlink():
        raise SystemExit("missing/linked generic wrapper manifest")
    if hashlib.sha256(MANIFEST.read_bytes()).hexdigest() != ORIGINAL_RECEIPT_SHA256:
        raise SystemExit("original generic wrapper receipt drift")
    records = [line.split("\t") for line in MANIFEST.read_text(encoding="utf-8").splitlines()
               if line and not line.startswith("#")]
    if len(records) != 5 or {r[0] for r in records if r} != EXPECTED:
        raise SystemExit("generic wrapper manifest missing/duplicate/foreign target")
    for row in records:
        if len(row) != 3:
            raise SystemExit("malformed generic wrapper manifest row")
        path, digest, state = row
        rel = Path(path)
        if (rel.is_absolute() or ".." in rel.parts or rel.parts[:2] != ("m3", "collections")
                or rel.suffix != ".java" or len(digest) != 64
                or state != "MODULE_SOURCE_NOT_INSTALLED_JDK"):
            raise SystemExit("generic wrapper bad source authority: " + path)
        file = ROOT / rel
        if not file.is_file() or file.is_symlink():
            raise SystemExit("missing/linked wrapper source: " + path)
        actual = hashlib.sha256(file.read_bytes()).hexdigest()
        if digest != actual:
            if (path == ORIGINAL_OWNER and digest == ORIGINAL_OWNER_SHA256
                    and actual == EXPECTED_SUCCESSOR[ORIGINAL_OWNER]
                    and successor_authorized()):
                print("SEALED AUTHORIZED PURPOSE SUCCESSOR", path, actual)
                continue
            raise SystemExit("wrapper source drift: " + path)
        print("SEALED", path, digest)
    print("PASS five original M3 generic wrapper and contract source seals")

if __name__ == "__main__":
    main()
