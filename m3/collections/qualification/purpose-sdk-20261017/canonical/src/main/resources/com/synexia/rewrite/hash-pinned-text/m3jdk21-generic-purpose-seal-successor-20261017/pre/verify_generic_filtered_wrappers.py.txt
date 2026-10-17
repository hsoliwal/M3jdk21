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

def main() -> None:
    if not MANIFEST.is_file() or MANIFEST.is_symlink():
        raise SystemExit("missing/linked generic wrapper manifest")
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
            raise SystemExit("wrapper source drift: " + path)
        print("SEALED", path, digest)
    print("PASS five original M3 generic wrapper and contract source seals")

if __name__ == "__main__":
    main()
