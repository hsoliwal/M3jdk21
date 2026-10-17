# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed receiver custody for Synexia's sorted index-path recipe.

Pure read-only proof: never rewrites installed target source. OpenRewrite SDK
execution, javac/JUnit, and checked-JNI runtime evidence remain separate gates.
"""
from __future__ import annotations

import hashlib
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
MANIFEST = ROOT / "m3/collections/recipe/long-index-stage-path-20261017/manifest.tsv"
MANIFEST_SHA256 = "37686ec2db3a8570de182768bc166384322e045a0419a0bf4e38f415ce8bf1e2"
EXPECTED = {
    "m3/collections/src/main/java/com/m3/collections/M3LongIndexProjection.java": (
        "a69694a2ce22ca9599cb665e4845f5dee75d527b7c144fa3ea7fb4518ec2d3fa",
        "b923b9857f6302ed529fa4b3a4415b8f3da0f1b42701ff2b4a3d8f89854fd5f0",
        "M3LongIndexProjection.java.txt",
    ),
    "m3/collections/src/test/java/com/m3/collections/M3LongIndexProjectionPathMasksTest.java": (
        "ABSENT",
        "239c6662ba7f91c786df60a9faa19633844b20408c972f88f381a7a241a49847",
        "M3LongIndexProjectionPathMasksTest.java.txt",
    ),
}


def _sha(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def verify(manifest_bytes: bytes, load_source) -> int:
    if _sha(manifest_bytes) != MANIFEST_SHA256:
        raise ValueError("stage-mask recipe manifest drift")

    seen: set[str] = set()
    for line in manifest_bytes.decode("utf-8").splitlines():
        fields = line.split("\t")
        if len(fields) != 4:
            raise ValueError("invalid stage-mask receipt row")
        path, before, after, template = fields
        if path in seen or path not in EXPECTED:
            raise ValueError("unknown or duplicate stage-mask recipient")
        seen.add(path)
        if (before, after, template) != EXPECTED[path]:
            raise ValueError("stage-mask source lineage drift")
        if _sha(load_source(path)) != after:
            raise ValueError("stage-mask target source/postimage drift: " + path)
    if seen != EXPECTED.keys():
        raise ValueError("missing stage-mask target receipt")
    return len(seen)


def _load_source(path: str) -> bytes:
    candidate = ROOT / path
    if candidate.is_symlink():
        raise ValueError("aliased source target")
    return candidate.read_bytes()


class M3StagePathMaskReceiptTest(unittest.TestCase):
    def test_exact_installed_sources(self) -> None:
        self.assertEqual(2, verify(MANIFEST.read_bytes(), _load_source))

    def test_corrupt_manifest_refuses(self) -> None:
        corrupted = MANIFEST.read_bytes() + b"# forged\n"
        with self.assertRaises(ValueError):
            verify(corrupted, _load_source)

    def test_corrupt_source_refuses_without_writes(self) -> None:
        path = next(iter(EXPECTED))
        original = _load_source(path)
        def drifted(p: str) -> bytes:
            return original + b"// mutation\n" if p == path else _load_source(p)
        with self.assertRaises(ValueError):
            verify(MANIFEST.read_bytes(), drifted)


if __name__ == "__main__":
    unittest.main()
