#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Positive and negative fixtures for original JDK compactor source succession."""
from __future__ import annotations

from hashlib import sha256
from pathlib import Path
from tempfile import TemporaryDirectory
from unittest import TestCase, main
from unittest.mock import patch

from importlib.util import module_from_spec, spec_from_file_location

# The new proof fixture has a timeless source path. Its owner remains the
# original immutable 2026-10-18 JDK verifier, never a duplicated implementation.
_original_owner = Path(__file__).resolve().parents[1] / "native-java-base-collection-mask-20261018" / "verify.py"
_spec = spec_from_file_location("m3mask_original_source_verify", _original_owner)
if _spec is None or _spec.loader is None:
    raise RuntimeError("original JDK source verifier unavailable")
owner = module_from_spec(_spec)
_spec.loader.exec_module(owner)


class CompactorSourceSuccessionTests(TestCase):
    def setUp(self):
        self.workspace = TemporaryDirectory()
        self.addCleanup(self.workspace.cleanup)
        self.root = Path(self.workspace.name)
        self.source = {
            'src/java.base/share/classes/java/util/ArrayList.java':
                b'import jdk.internal.mindex.M3CollectionMaskCompactor;\n'
                b'M3CollectionMaskCompactor.compact(es, beg, end, deathRow)\n',
            'src/java.base/share/classes/java/util/Vector.java':
                b'import jdk.internal.mindex.M3CollectionMaskCompactor;\n'
                b'M3CollectionMaskCompactor.compact(es, beg, end, deathRow)\n',
            owner.COMPACTOR:
                b'long[] deleted\nremovalRuns(deleted, length)\n'
                b'System.arraycopy(values, from + nextSurvivor, values, write, span)\n'
                b'return write;\nprivate static int compactWords(\n'
                b'Long.numberOfTrailingZeros(\n',
            'test/jdk/java/util/ArrayList/M3MaskCompactionDifferential.java': b'differential test\n',
            'test/jdk/java/util/ArrayList/M3JdkRemoveIfMaskContracts.java': b'contract test\n',
        }
        self.expected = {path: sha256(data).hexdigest()
                         for path, data in self.source.items()}
        self.patch = patch.dict(owner.HISTORICAL_SHA256, self.expected, clear=True)
        self.patch.start()
        self.addCleanup(self.patch.stop)
        self.new_sha = self.expected[owner.COMPACTOR]
        self.succession_sha = patch.object(owner, 'APPROVED_SHA256', self.new_sha)
        self.succession_sha.start()
        self.addCleanup(self.succession_sha.stop)
        for path, data in self.source.items():
            self.write(path, data)
        self.write(owner.SUCCESSIONS, self.ledger())

    def write(self, path, data):
        destination = self.root / path
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_bytes(data if isinstance(data, bytes) else data.encode('utf-8'))

    def ledger(self):
        return '# one reviewed first-party owner only\n' + '\t'.join([
            owner.COMPACTOR, self.expected[owner.COMPACTOR], self.new_sha,
            owner.ORIGIN_PRODUCT, owner.ORIGIN_RECIPE, owner.ORIGIN_COMMIT,
            owner.LICENSE, owner.STATE]) + '\n'

    def test_accepts_exact_reviewed_owner_without_mutating_four_other_owners(self):
        self.assertEqual((4, 1), owner.verify(self.root))

    def test_rejects_wrong_current_compactor_bytes(self):
        self.write(owner.COMPACTOR, self.source[owner.COMPACTOR] + b'unreviewed\n')
        with self.assertRaisesRegex(owner.SourceCustodyError, 'SOURCE_DRIFT'):
            owner.verify(self.root)

    def test_rejects_drift_of_unchanged_original_array(self):
        self.write('src/java.base/share/classes/java/util/ArrayList.java', b'mutated\n')
        with self.assertRaisesRegex(owner.SourceCustodyError, 'SOURCE_DRIFT'):
            owner.verify(self.root)

    def test_rejects_forged_successor_digest(self):
        self.write(owner.SUCCESSIONS, self.ledger().replace(self.new_sha, 'f' * 64))
        with self.assertRaisesRegex(owner.SourceCustodyError, 'UNREVIEWED'):
            owner.verify(self.root)

    def test_rejects_duplicate_successor_records(self):
        self.write(owner.SUCCESSIONS, self.ledger() + self.ledger().splitlines()[-1] + '\n')
        with self.assertRaisesRegex(owner.SourceCustodyError, 'EXACTLY_ONE'):
            owner.verify(self.root)

    def test_rejects_wrong_provenance_or_rights(self):
        self.write(owner.SUCCESSIONS, self.ledger().replace(owner.LICENSE, 'Unknown'))
        with self.assertRaisesRegex(owner.SourceCustodyError, 'UNREVIEWED'):
            owner.verify(self.root)

    def test_rejects_missing_successor(self):
        (self.root / owner.SUCCESSIONS).unlink()
        with self.assertRaisesRegex(owner.SourceCustodyError, 'MISSING_OR_LINKED'):
            owner.verify(self.root)

    def test_rejects_symlinked_original_owner(self):
        source = self.root / owner.COMPACTOR
        source.unlink()
        source.symlink_to(self.root / 'src/java.base/share/classes/java/util/ArrayList.java')
        with self.assertRaisesRegex(owner.SourceCustodyError, 'MISSING_OR_LINKED'):
            owner.verify(self.root)

    def test_rejects_loss_of_word_compaction_contract(self):
        self.source[owner.COMPACTOR] = self.source[owner.COMPACTOR].replace(
            b'Long.numberOfTrailingZeros(', b'Long.numberOfLeadingZeros(')
        self.write(owner.COMPACTOR, self.source[owner.COMPACTOR])
        with patch.object(owner, 'APPROVED_SHA256', sha256(self.source[owner.COMPACTOR]).hexdigest()):
            self.write(owner.SUCCESSIONS, '\t'.join([
                owner.COMPACTOR, self.expected[owner.COMPACTOR], owner.APPROVED_SHA256,
                owner.ORIGIN_PRODUCT, owner.ORIGIN_RECIPE, owner.ORIGIN_COMMIT,
                owner.LICENSE, owner.STATE]) + '\n')
            with self.assertRaisesRegex(owner.SourceCustodyError, 'MISSING_COMPACTION_INVARIANT'):
                owner.verify(self.root)


if __name__ == '__main__':
    main()
