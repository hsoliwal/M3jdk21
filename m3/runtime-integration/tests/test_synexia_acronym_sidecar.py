"""Proof for the dedicated m3lex-acronym-v1 string sidecar."""
from __future__ import annotations

import hashlib
import importlib.util
import unittest
from pathlib import Path

MODULE = Path(__file__).parents[1] / "synexia-acronym-sidecar.py"
SPEC = importlib.util.spec_from_file_location("m3_acronym_sidecar", MODULE)
assert SPEC and SPEC.loader
SIDECAR = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(SIDECAR)

OWNER = "0123456789abcdef" * 4


class AcronymSidecarTest(unittest.TestCase):
    def row(self, acronym="API"):
        return {
            "source_id": SIDECAR.SOURCE_ID,
            "record_id": acronym,
            "source_manifest_revision": "synexia-acronym-1",
            "owner_fingerprint": OWNER,
            "acronym": acronym,
            "expansion": "application programming interface",
            "domain": "computing",
        }

    def test_deterministic_round_trip_and_scope(self):
        files = SIDECAR.render([self.row("API"), self.row("HTTP")])
        self.assertEqual(SIDECAR.verify(files), {"rows": 2, "files": 2})
        self.assertEqual(files, SIDECAR.render([self.row("API"), self.row("HTTP")]))

    def test_string_validation_and_identity(self):
        for field, value in (
            ("source_id", "foreign.family"),
            ("record_id", "wrong"),
            ("acronym", "not valid"),
            ("expansion", "  "),
            ("domain", "Computing"),
        ):
            row = self.row()
            row[field] = value
            with self.subTest(field=field):
                with self.assertRaises(ValueError):
                    SIDECAR.render([row])

    def test_empty_sidecar_fails_closed(self):
        with self.assertRaises(ValueError):
            SIDECAR.render([])
        content = ("\t".join(SIDECAR.COLUMNS) + "\n").encode("utf-8")
        index = SIDECAR._tsv(
            SIDECAR.INDEX_COLUMNS,
            [(SIDECAR.SCHEMA_VERSION, SIDECAR.DATA_FILE, "0", hashlib.sha256(content).hexdigest())],
        )
        with self.assertRaises(ValueError):
            SIDECAR.verify({SIDECAR.DATA_FILE: content, SIDECAR.INDEX_FILE: index})

    def test_python_verifier_matches_java_order_scope_contract(self):
        mixed = self.row("HTTP")
        mixed["source_manifest_revision"] = "other-revision"
        with self.assertRaises(ValueError):
            SIDECAR.render([self.row("API"), mixed])

        files = SIDECAR.render([self.row("API"), self.row("HTTP")])
        lines = files[SIDECAR.DATA_FILE].decode("utf-8").splitlines()
        reordered = ("\n".join([lines[0], lines[2], lines[1], ""])).encode("utf-8")
        files[SIDECAR.DATA_FILE] = reordered
        files[SIDECAR.INDEX_FILE] = SIDECAR._tsv(
            SIDECAR.INDEX_COLUMNS,
            [(SIDECAR.SCHEMA_VERSION, SIDECAR.DATA_FILE, "2", hashlib.sha256(reordered).hexdigest())],
        )
        with self.assertRaises(ValueError):
            SIDECAR.verify(files)

        duplicated = ("\n".join([lines[0], lines[1], lines[1], ""])).encode("utf-8")
        files[SIDECAR.DATA_FILE] = duplicated
        files[SIDECAR.INDEX_FILE] = SIDECAR._tsv(
            SIDECAR.INDEX_COLUMNS,
            [(SIDECAR.SCHEMA_VERSION, SIDECAR.DATA_FILE, "2", hashlib.sha256(duplicated).hexdigest())],
        )
        with self.assertRaises(ValueError):
            SIDECAR.verify(files)

    def test_order_duplicate_and_checksum_fail_closed(self):
        with self.assertRaises(ValueError):
            SIDECAR.render([self.row("HTTP"), self.row("API")])
        with self.assertRaises(ValueError):
            SIDECAR.render([self.row("API"), self.row("API")])
        files = SIDECAR.render([self.row()])
        files[SIDECAR.DATA_FILE] += b"tamper"
        with self.assertRaises(ValueError):
            SIDECAR.verify(files)


if __name__ == "__main__":
    unittest.main()
