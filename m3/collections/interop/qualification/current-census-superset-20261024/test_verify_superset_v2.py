#!/usr/bin/env python3
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
"""Finite controls for the eight-owner successor shard over the unmodified 36+40 census."""
import hashlib
from pathlib import Path
import tempfile
import unittest
import verify_superset_v2 as successor


class SuccessorContract(unittest.TestCase):
    def setUp(self):
        self.v1 = successor.load_v1()
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.receipts_v1 = self.root / 'receipts-v1'
        self.receipts = self.root / 'receipts'
        self.receipts_v1.mkdir()
        self.receipts.mkdir()
        main, test = self.v1.MAIN, self.v1.TEST
        self.older = {main + f'Old{i:02d}.java' for i in range(18)} | {test + f'OldTest{i:02d}.java' for i in range(18)}
        additive = sorted([main + f'M3AddMain{i:02d}.java' for i in range(13)]
                          + [test + f'M3AddTest{i:02d}.java' for i in range(27)])
        rows = [self.row(path, 'five-provider-index') for path in additive]
        for at, name in enumerate(self.v1.SHARDS):
            self.write(self.receipts_v1 / name, rows[at * 10:at * 10 + 10])
        libraries = (('Apache', 'apache'), ('Eclipse', 'eclipse-collections'), ('Guava', 'guava'), ('Spring', 'spring'))
        self.added = sorted([self.row(main + f'M3{lib}Streams.java', family) for lib, family in libraries]
                            + [self.row(test + f'M3{lib}StreamsTest.java', family) for lib, family in libraries])
        self.write(self.receipts / successor.SHARD, self.added)
        self.tracked = self.older | set(additive) | {row[0] for row in self.added}

    def row(self, relative, family):
        path = self.root / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        data = ('// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0\npackage com.m3.collections.interop;\n// '
                + relative + '\n').encode('utf-8')
        path.write_bytes(data)
        kind = 'PRODUCTION' if relative.startswith(self.v1.MAIN) else 'TEST'
        return [relative, hashlib.sha256(data).hexdigest(), self.v1.git_blob(data), family, kind, 'Apache-2.0']

    @staticmethod
    def write(path, rows):
        path.write_text(''.join('\t'.join(row) + '\n' for row in rows), encoding='utf-8')

    def verify(self, tracked=None):
        return successor.verify_tracked(self.v1, self.root, self.receipts_v1, self.receipts, self.older,
                                        self.tracked if tracked is None else tracked)

    def test_exact_successor_admits_eight_over_the_untouched_forty(self):
        extras, rows = self.verify()
        self.assertEqual((40, 8), (len(extras), len(rows)))

    def test_source_drift_is_refused(self):
        (self.root / self.added[0][0]).write_bytes(b'// SPDX-License-Identifier: Apache-2.0\n// drift\n')
        with self.assertRaisesRegex(ValueError, 'successor source SHA'):
            self.verify()

    def test_unadmitted_extra_source_is_refused_by_the_predecessor(self):
        with self.assertRaisesRegex(ValueError, 'installed interop tracking drift'):
            self.verify(self.tracked | {self.v1.MAIN + 'M3Unadmitted.java'})

    def test_untracked_successor_source_is_refused(self):
        with self.assertRaisesRegex(ValueError, 'successor sources not tracked'):
            self.verify(self.tracked - {self.added[0][0]})

    def test_wrong_family_or_dimension_is_refused(self):
        wrong = [list(row) for row in self.added]
        wrong[0][3] = 'jdk'
        self.write(self.receipts / successor.SHARD, wrong)
        with self.assertRaisesRegex(ValueError, 'invalid successor scope'):
            self.verify()
        seven = self.added[:7]
        self.write(self.receipts / successor.SHARD, seven)
        with self.assertRaisesRegex(ValueError, 'eight unique sorted'):
            self.verify()

    def repin_fixture(self):
        census = successor.CENSUS_DIR + 'current-source-census.tsv'
        test = self.v1.TEST + 'M3Revised.java'
        historical, predecessors, product = (self.root / name for name in ('historical', 'predecessors', 'product'))
        for path, before, after, origin in ((census, b'old census\n', b'new census\n', historical),
                                            (test, b'old test\n', b'new test\n', None)):
            (historical / path).parent.mkdir(parents=True, exist_ok=True)
            (product / path).parent.mkdir(parents=True, exist_ok=True)
            (product / path).write_bytes(after)
            if origin is None:
                predecessors.mkdir(exist_ok=True)
                (predecessors / (path[len(successor.INTEROP):].replace('/', '~') + '.txt')).write_bytes(before)
            else:
                (origin / path).write_bytes(before)
        (product / successor.CENSUS_DIR / 'verify.py').write_bytes(b'# verify\n')
        rows = [[path, source, hashlib.sha256(before).hexdigest(), self.v1.git_blob(before),
                 hashlib.sha256(after).hexdigest(), self.v1.git_blob(after), 'e8283401e0f']
                for path, source, before, after in ((census, 'historical', b'old census\n', b'new census\n'),
                                                    (test, 'predecessors', b'old test\n', b'new test\n'))]
        return historical, predecessors, product, rows, census, test

    def test_view_restores_predecessors_after_checking_both_pins(self):
        historical, predecessors, product, rows, census, test = self.repin_fixture()
        view = self.root / 'view'
        successor.build_view(self.v1, product, historical, rows, {test}, view, predecessors)
        self.assertEqual((b'old census\n', b'old test\n'), ((view / census).read_bytes(), (view / test).read_bytes()))
        (product / test).write_bytes(b'newer test\n')
        with self.assertRaisesRegex(ValueError, 'differs from its after pin'):
            successor.build_view(self.v1, product, historical, rows, {test}, self.root / 'view2', predecessors)
        (product / test).write_bytes(b'new test\n')
        (historical / census).write_bytes(b'forged census\n')
        with self.assertRaisesRegex(ValueError, 'predecessor differs from its before pin'):
            successor.build_view(self.v1, product, historical, rows, {test}, self.root / 'view3', predecessors)

    def test_repin_receipt_shape_is_exact(self):
        _historical, _predecessors, _product, rows, _census, _test = self.repin_fixture()
        self.write(self.receipts / successor.REPIN, rows)
        with self.assertRaisesRegex(ValueError, 'five seven-column rows'):
            successor.repin_rows(self.v1, self.receipts)
        rows[1][1] = 'historical'
        self.write(self.receipts / successor.REPIN, rows * 3)
        with self.assertRaisesRegex(ValueError, 'five seven-column rows|invalid re-pin row'):
            successor.repin_rows(self.v1, self.receipts)

    def test_predecessor_verifier_bytes_are_pinned(self):
        copy = self.root / 'v1copy'
        copy.mkdir()
        source = successor.V1_DIR / 'verify_superset.py'
        (copy / 'verify_superset.py').write_bytes(source.read_bytes() + b'\n')
        with self.assertRaisesRegex(ValueError, 'differs from its pinned bytes'):
            successor.load_v1(copy)


if __name__ == '__main__':
    unittest.main()
