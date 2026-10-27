#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Negative and positive oracles for the existing historical custody and one successor."""
from __future__ import annotations

from contextlib import ExitStack
from pathlib import Path
from tempfile import TemporaryDirectory
from unittest import TestCase, main
from unittest.mock import patch

import verify_synexia_handoff as verifier


class HandoffLineageTests(TestCase):
    def setUp(self):
        self.temporary = TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        self.sources = [
            "m3/collections/src/main/java/com/m3/collections/M3ObservableList.java",
            "m3/collections/src/test/java/com/m3/collections/M3ObservableIndexedViewsContract.java",
            "m3/file/src/main/java/com/m3/file/M3FileMoveCandidates.java",
            verifier.EVOLVED_OWNER,
            "m3/file/src/test/java/com/m3/file/M3FileMoveCandidatesContract.java",
        ]
        self.old_bytes = b"class OriginalResourceLocks {}\n"
        self.new_bytes = b"class OriginalResourceLocks { /* additive timeout */ }\n"
        historical_rows = []
        for n, relative in enumerate(self.sources):
            old = self.old_bytes if relative == verifier.EVOLVED_OWNER else f"source-{n}\n".encode()
            current = self.new_bytes if relative == verifier.EVOLVED_OWNER else old
            self.write(relative, current)
            digest = verifier.sha256(old)
            historical_rows.append("\t".join([
                relative, "com.synexia.m3.OriginalRecipe", "ABSENT", digest,
                "M3JDK21#1810", "hsoliwal/com.synexia#12951",
                "PENDING_RECIPE_AND_PRODUCT_CI", verifier.RIGHTS,
            ]))
        self.historical = ("# historical fixed input\n" + "\n".join(historical_rows) + "\n").encode()
        self.write(verifier.HISTORY, self.historical)
        self.write(verifier.SUCCESSIONS, self.successor())
        self.patches = ExitStack()
        self.addCleanup(self.patches.close)
        self.patches.enter_context(patch.object(verifier, "HISTORY_BLOB", verifier.git_blob_sha1(self.historical)))
        self.patches.enter_context(patch.object(verifier, "HISTORICAL_OWNER_SHA", verifier.sha256(self.old_bytes)))
        self.patches.enter_context(patch.object(verifier, "CURRENT_OWNER_SHA", verifier.sha256(self.new_bytes)))

    def write(self, relative, contents):
        path = self.root / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(contents if isinstance(contents, bytes) else contents.encode())

    def successor(self):
        return "# additive review successor only\n" + "\t".join([
            verifier.EVOLVED_OWNER, verifier.sha256(self.old_bytes),
            verifier.sha256(self.new_bytes), verifier.PRODUCT_PR, verifier.CANONICAL_PR,
            verifier.STATE, verifier.RIGHTS,
        ]) + "\n"

    def test_exact_historical_and_reviewed_successor(self):
        self.assertEqual((5, 1), verifier.verify(self.root))

    def test_refuses_changed_source_without_new_review(self):
        self.write(verifier.EVOLVED_OWNER, self.new_bytes + b"unexpected\n")
        with self.assertRaisesRegex(verifier.HandoffRefusal, "unexpected Java product source drift"):
            verifier.verify(self.root)

    def test_refuses_wrong_successor_current_hash(self):
        self.write(verifier.SUCCESSIONS, self.successor().replace(
            verifier.sha256(self.new_bytes), "f" * 64))
        with self.assertRaisesRegex(verifier.HandoffRefusal, "unreviewed successor"):
            verifier.verify(self.root)

    def test_refuses_unknown_second_successor(self):
        self.write(verifier.SUCCESSIONS, self.successor() + self.successor().splitlines()[-1] + "\n")
        with self.assertRaisesRegex(verifier.HandoffRefusal, "exactly one"):
            verifier.verify(self.root)

    def test_refuses_historical_source_receipt_mutation(self):
        self.write(verifier.HISTORY, self.historical + b"# edited\n")
        with self.assertRaisesRegex(verifier.HandoffRefusal, "immutable"):
            verifier.verify(self.root)

    def test_refuses_missing_successor(self):
        (self.root / verifier.SUCCESSIONS).unlink()
        with self.assertRaisesRegex(verifier.HandoffRefusal, "missing or symlinked"):
            verifier.verify(self.root)

    def test_refuses_symlinked_original_owner(self):
        path = self.root / self.sources[0]
        path.unlink()
        path.symlink_to(self.root / self.sources[1])
        with self.assertRaisesRegex(verifier.HandoffRefusal, "symlinked custody"):
            verifier.verify(self.root)


if __name__ == "__main__":
    main()
