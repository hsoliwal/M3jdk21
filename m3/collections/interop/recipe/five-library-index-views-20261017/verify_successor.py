#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
"""Admit the immutable 2026-10-17 index-view postimage or its one named 32-bit successor."""
import hashlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[5]
HERE = Path(__file__).resolve().parent
CRATE = "m3jdk21-five-library-index-views-20261017"
RES = HERE / "src/main/resources/com/synexia/rewrite/hash-pinned-java" / CRATE
MIRROR = HERE / "canonical/src/main/resources/com/synexia/rewrite/hash-pinned-java" / CRATE

HISTORICAL_MANIFEST_SHA256 = "e6c0c9e779be05fafbf30158ec578a120a8154516462e71b75e03c333c27b78a"
INDEX_PATH = "m3/collections/interop/src/main/java/com/m3/collections/interop/M3FiveLibraryIndexViews.java"
TEST_PATH = "m3/collections/interop/src/test/java/com/m3/collections/interop/M3FiveLibraryIndexViewsTest.java"
SUCCESSOR_TEST_PATH = "m3/collections/interop/src/test/java/com/m3/collections/interop/M3FiveLibraryState32Test.java"
SUCCESSOR_BEFORE = "605dbbf0193dbe9b298499d399f9c3607a95ce74b26884d8b3ac9c7aad50f193"
SUCCESSOR_AFTER = "fb8125ef79ffc4788b00571ab447a1ed08eca65a56668725db5b2d799a67e85b"
SUCCESSOR_TEST_AFTER = "26548a60299d8647807401c467d013da60e2f9d248cb82450dcc6d0fe70a3d15"


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    manifest = RES / "manifest.tsv"
    assert digest(manifest) == HISTORICAL_MANIFEST_SHA256, "historical manifest drift"
    assert manifest.read_bytes() == (MIRROR / "manifest.tsv").read_bytes(), "canonical manifest drift"

    rows = {}
    for line in manifest.read_text(encoding="utf-8").splitlines():
        if not line or line.startswith("#"):
            continue
        path, before, after, template = line.split("\t")
        assert before == "ABSENT", (path, before)
        assert digest(RES / template) == after, ("historical template drift", path)
        assert (RES / template).read_bytes() == (MIRROR / template).read_bytes(), ("canonical template drift", path)
        rows[path] = after

    assert set(rows) == {INDEX_PATH, TEST_PATH}
    assert rows[INDEX_PATH] == SUCCESSOR_BEFORE

    installed_index = digest(ROOT / INDEX_PATH)
    assert installed_index in {SUCCESSOR_BEFORE, SUCCESSOR_AFTER}, (
        "unapproved index-view source", installed_index)
    assert digest(ROOT / TEST_PATH) == rows[TEST_PATH], "historical test source drift"

    if installed_index == SUCCESSOR_AFTER:
        assert (ROOT / SUCCESSOR_TEST_PATH).is_file(), "successor test missing"
        assert digest(ROOT / SUCCESSOR_TEST_PATH) == SUCCESSOR_TEST_AFTER, "successor test drift"

    receipt = HERE / "SUCCESSOR_RECEIPT.tsv"
    assert receipt.is_file() and receipt.stat().st_size > 0, "successor receipt missing"
    print("M3 five-library index-view historical/successor source PASS", installed_index)


if __name__ == "__main__":
    main()
