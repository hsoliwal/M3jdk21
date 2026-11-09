#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Synthetic positive and negative source-custody cases. No JDK runtime claims."""
from __future__ import annotations

from contextlib import ExitStack
from pathlib import Path
from tempfile import TemporaryDirectory
from unittest import TestCase, main

import verify_cursor_successors as custodian


class CursorSourceSuccessionTests(TestCase):
    def setUp(self):
        self.temp = TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.rows = []
        self.successors = []
        self.inputs = {}
        for i in range(8):
            source = f"test/pkg/Owner{i}.java"
            prior = f"class Owner{i} {{}}\n".encode()
            evolved = f"class Owner{i} {{ int field; }}\n".encode() if i < 3 else prior
            self.write(source, evolved)
            self.inputs[source] = evolved
            self.rows.append((source, "ABSENT" if i >= 4 else custodian.sha256(prior),
                              custodian.sha256(prior), custodian.git_blob(prior), f"{i:02d}-Owner.txt"))
            if i < 3:
                self.successors.append((source, custodian.sha256(prior), custodian.sha256(evolved),
                                        custodian.git_blob(prior), custodian.git_blob(evolved),
                                        f"test/review/owner{i}", custodian.STATE, "Apache-2.0"))
        self.historical = ("# pinned original\n" + "\n".join("\t".join(x) for x in self.rows) + "\n").encode()
        self.write(custodian.ORIGINAL_MANIFEST, self.historical)
        self.write(custodian.SUCCESSOR_MANIFEST, self.successor_text())
        self.original_blob = custodian.git_blob(self.historical)

    def write(self, name, contents):
        path = self.root / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(contents if isinstance(contents, bytes) else contents.encode())

    def successor_text(self):
        return "# three exact successors\n" + "\n".join("\t".join(x) for x in self.successors) + "\n"

    def check(self):
        return custodian.verify(self.root, historical_blob=self.original_blob,
                                successors=tuple(self.successors))

    def test_exact_eight_and_three(self):
        self.assertEqual((8, 3), self.check())

    def test_reject_historical_manifest_mutation(self):
        self.write(custodian.ORIGINAL_MANIFEST, self.historical + b"# altered\n")
        with self.assertRaisesRegex(custodian.SourceRefusal, "rewritten"):
            self.check()

    def test_reject_unreviewed_successor(self):
        self.write(custodian.SUCCESSOR_MANIFEST, self.successor_text().replace("test/review/owner0", "fake"))
        with self.assertRaisesRegex(custodian.SourceRefusal, "unreviewed"):
            self.check()

    def test_reject_additional_successor(self):
        self.write(custodian.SUCCESSOR_MANIFEST, self.successor_text() + "\t".join(self.successors[0]) + "\n")
        with self.assertRaisesRegex(custodian.SourceRefusal, "unreviewed"):
            self.check()

    def test_reject_modified_evolved_owner(self):
        target = self.rows[0][0]
        self.write(target, self.inputs[target] + b"// mutated\n")
        with self.assertRaisesRegex(custodian.SourceRefusal, "unauthorized original-owner successor"):
            self.check()

    def test_reject_modified_unchanged_owner(self):
        target = self.rows[-1][0]
        self.write(target, self.inputs[target] + b"// mutated\n")
        with self.assertRaisesRegex(custodian.SourceRefusal, "unchanged original Java source drift"):
            self.check()

    def test_refuse_missing_owner(self):
        (self.root / self.rows[0][0]).unlink()
        with self.assertRaisesRegex(custodian.SourceRefusal, "missing or symlinked"):
            self.check()

    def test_refuse_symlinked_owner(self):
        target = self.root / self.rows[0][0]
        target.unlink()
        target.symlink_to(self.root / self.rows[1][0])
        with self.assertRaisesRegex(custodian.SourceRefusal, "missing or symlinked"):
            self.check()

    def test_refuse_row_order_change(self):
        broken = [self.rows[1], self.rows[0], *self.rows[2:]]
        self.historical = ("# original\n" + "\n".join("\t".join(x) for x in broken) + "\n").encode()
        self.original_blob = custodian.git_blob(self.historical)
        self.write(custodian.ORIGINAL_MANIFEST, self.historical)
        with self.assertRaisesRegex(custodian.SourceRefusal, "cardinality/order"):
            self.check()


if __name__ == "__main__":
    main()
