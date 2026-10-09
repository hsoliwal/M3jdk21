#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
"""Executable proof for the typed M3JDK family-sidecar bundle."""

from __future__ import annotations

import unittest

import synexia_family_sidecars as family


class SynexiaFamilySidecarTest(unittest.TestCase):
    def test_deterministic_complete_bundle_and_legacy_mode(self) -> None:
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
        first = family.render_bundle(rows)
        second = family.render_bundle(rows)
        self.assertEqual(first, second)
        self.assertEqual(
            {"families": 5, "rows": 5},
            family.verify_optional_bundle(first),
        )
        self.assertEqual(
            {"legacy": True, "families": 0, "rows": 0},
            family.verify_optional_bundle({"synexia.shards.tsv": b"legacy\n"}),
        )

    def test_partial_and_noncanonical_bundles_fail_closed(self) -> None:
        digest = "0123456789abcdef" * 4
        scope = {
            "source_id": "numbers", "record_id": "0",
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
        bundle = family.render_bundle(rows)
        with self.assertRaisesRegex(ValueError, "complete bundle"):
            family.verify_optional_bundle(
                {family.INDEX_FILE: bundle[family.INDEX_FILE]}
            )
        broken = dict(bundle)
        broken[family.INDEX_FILE] = b"\xef\xbb\xbf" + broken[family.INDEX_FILE]
        with self.assertRaisesRegex(ValueError, "BOM"):
            family.verify_optional_bundle(broken)


if __name__ == "__main__":
    result = unittest.main(exit=False)
    if not result.result.wasSuccessful():
        raise SystemExit(1)
    print("M3JDK_FAMILY_SIDECAR_TEST_PASS checks=4")
