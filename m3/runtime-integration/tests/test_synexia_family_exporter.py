#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
"""Proof that the production exporter admits only complete family bundles."""

from __future__ import annotations

import importlib.util
import sys
import tempfile
import unittest
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
sys.path.insert(0, str(ROOT / "m3/runtime-integration"))

import synexia_family_sidecars as FAMILY  # noqa: E402

SPEC = importlib.util.spec_from_file_location(
    "synexia_export_for_family_test",
    ROOT / "m3/runtime-integration/export-synexia-lexicon.py",
)
EXPORT = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
sys.modules[SPEC.name] = EXPORT
SPEC.loader.exec_module(EXPORT)


class SynexiaFamilyExporterTest(unittest.TestCase):
    def test_complete_bundle_loads_and_partial_bundle_fails(self) -> None:
        digest = "0123456789abcdef" * 4
        scope = {
            "source_id": "numbers",
            "record_id": "0",
            "source_manifest_revision": "manifest-r1",
            "owner_fingerprint": "owner-f1",
        }
        rows = {
            "prefix-counts": [{
                **scope, "shard_id": 0, "image_row": 0,
                "derivation_version": "prefix-v1", "value_fingerprint": "value-0",
                "token_id": 0, "prefix_counts": [0, 1],
            }],
            "spell-index": [{
                **scope, "lexicon_fingerprint": "lex-1", "language": "und",
                "max_edit_distance": 2, "prefix_length": 3,
                "source_fingerprint": "spell-1", "delete_key": "0",
                "candidate_token_ids": [0], "frequencies": {0: 1},
            }],
            "token-frequency": [{
                **scope, "shard_id": 0, "image_row": 0,
                "derivation_version": "frequency-v1", "value_fingerprint": "value-0",
                "frequencies": {0: 1},
            }],
            "token-hash-precompute": [{
                **scope, "shard_id": 0, "image_row": 0,
                "value_fingerprint": "value-0", "tokenizer_version": "tok-v1",
                "range_start": 0, "range_end": 1,
                "range_fingerprint_first": 1, "range_fingerprint_second": 2,
                "token_sha256": [digest], "range_sha256": digest,
            }],
            "translation-projection": [{
                **scope, "source_language": "en", "target_language": "hi",
                "lexicon_fingerprint": "lex-1", "source_fingerprint": "translation-1",
                "translated_token_ids": [0], "mapped_token_count": 1,
            }],
        }
        with tempfile.TemporaryDirectory() as directory:
            sidecars = Path(directory)
            for name, data in FAMILY.render_bundle(rows).items():
                (sidecars / name).write_bytes(data)
            files, stats = EXPORT.load_family_sidecars(sidecars)
            self.assertEqual(5, len(files) - 0)
            self.assertEqual({"families": 5, "rows": 5}, stats)
            (sidecars / FAMILY.INDEX_FILE).unlink()
            with self.assertRaisesRegex(ValueError, "coverage mismatch"):
                EXPORT.load_family_sidecars(sidecars)


if __name__ == "__main__":
    unittest.main()
