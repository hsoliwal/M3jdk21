#!/usr/bin/env python3
"""Source-level proof for the target-owned family-sidecar bridge."""
from __future__ import annotations

import importlib.util
import tempfile
from pathlib import Path


MODULE_PATH = Path(__file__).parents[1] / "synexia-precompute-sidecar.py"
SPEC = importlib.util.spec_from_file_location("m3jdk_sidecar", MODULE_PATH)
assert SPEC and SPEC.loader
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)


def families():
    scope = {"source_id": "dictlang.dictionary", "record_id": "r-1",
             "source_manifest_revision": "manifest-1", "owner_fingerprint": "owner-1"}
    digest = "0123456789abcdef" * 4
    return {
        "prefix-counts": [{**scope, "shard_id": 0, "image_row": 0, "derivation_version": "v1", "value_fingerprint": "value-1", "token_id": 7, "prefix_counts": [0, 1, 1, 2]}],
        "spell-index": [{**scope, "lexicon_fingerprint": "lex-1", "language": "en", "max_edit_distance": 2, "prefix_length": 4, "source_fingerprint": "spell-1", "delete_key": "helo", "candidate_token_ids": [2, 7], "frequencies": {2: 4, 7: 1}}],
        "token-frequency": [{**scope, "shard_id": 0, "image_row": 0, "derivation_version": "v1", "value_fingerprint": "value-1", "frequencies": {1: 2, 2: 1}}],
        "token-hash-precompute": [{**scope, "shard_id": 0, "image_row": 0, "value_fingerprint": "value-1", "tokenizer_version": "tok-1", "range_start": 0, "range_end": 2, "range_fingerprint_first": 7, "range_fingerprint_second": 13, "token_sha256": [digest, digest], "range_sha256": digest}],
        "translation-projection": [{**scope, "source_language": "en", "target_language": "hi", "lexicon_fingerprint": "lex-1", "source_fingerprint": "src-1", "translated_token_ids": [1, 2, 3], "mapped_token_count": 2}],
    }


def main() -> None:
    first = MODULE.render_bundle(families())
    assert first == MODULE.render_bundle(families())
    assert MODULE.verify_bundle(first) == {"families": 5, "rows": 5}
    with tempfile.TemporaryDirectory(prefix="m3jdk-sidecar-test-") as temporary:
        output = Path(temporary) / "bundle"
        MODULE.write_bundle(output, first)
        files = {path.name: path.read_bytes() for path in output.iterdir()}
        assert MODULE.verify_bundle(files) == {"families": 5, "rows": 5}
    tampered = dict(first)
    tampered["synexia.spell.tsv"] += b"tampered"
    try:
        MODULE.verify_bundle(tampered)
    except ValueError:
        print("M3JDK_SIDECAR_TEST_PASS checks=4 families=5 rows=5")
        return
    raise AssertionError("tampered checksum was accepted")


if __name__ == "__main__":
    main()
