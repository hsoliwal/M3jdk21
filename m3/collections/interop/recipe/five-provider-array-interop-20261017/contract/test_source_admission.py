#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0
"""Exercise source admission on disposable copies of an exact received tree.

The optional --template-crate mode is explicitly a preflight of canonical
postimages, not a replacement for the real OpenRewrite receiver qualification.
"""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import sys
import tempfile
import types
import unittest

sys.dont_write_bytecode = True
MASTER = 'f3c615693c41bac1bf6ff49ba53e83f85425bca0'
HERE_RELATIVE = Path('m3/collections')
SUCCESSOR = 'recipe/five-provider-array-interop-20261017/'
HISTORICAL = (
    'recipe/filtered-sorted-views-20261017/manifest.tsv',
    'recipe/stored-representative-20261017/manifest.tsv',
    'recipe/index-path-masks-20261017/manifest.tsv',
)


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def load_verifier(path):
    module = types.ModuleType('source_admission_under_test')
    module.__file__ = str(path)
    helper_path = path.parent / 'm3_object_state_offset_receipt.py'
    helper_name = 'm3_object_state_offset_receipt'
    helper = types.ModuleType(helper_name)
    helper.__file__ = str(helper_path)
    exec(compile(helper_path.read_bytes(), str(helper_path), 'exec'), helper.__dict__)
    previous = sys.modules.get(helper_name)
    sys.modules[helper_name] = helper
    try:
        exec(compile(path.read_bytes(), str(path), 'exec'), module.__dict__)
    finally:
        if previous is None:
            del sys.modules[helper_name]
        else:
            sys.modules[helper_name] = previous
    return module


def copy_input_tree(target, destination):
    source, out = target / HERE_RELATIVE, destination / HERE_RELATIVE
    shutil.copytree(source / 'src', out / 'src', symlinks=True)
    packet = json.loads((source / 'synexia-donation.json').read_text())
    relatives = (
        'verify_donation.py', 'synexia-donation.json', 'LICENSE', 'NOTICE',
        'm3_object_state_offset_receipt.py', 'qualification/object-state-offset-jni-20261017.tsv',
        'verification/PreparedLaneAllocationProbe.java', 'verification/SegmentedLaneNativeProbe.java',
        packet['installed_source_manifest'], *HISTORICAL,
        SUCCESSOR + 'master-source-manifest.tsv', SUCCESSOR + 'source-manifest.tsv',
    )
    for relative in relatives:
        original = source / relative
        if not original.exists():
            continue
        output = out / relative
        output.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(original, output, follow_symlinks=False)


def apply_preflight_templates(base, crate):
    resources = crate / 'src/main/resources/com/synexia/rewrite'
    text = resources / 'hash-pinned-text/five-provider-array-interop-20261017'
    java = resources / 'hash-pinned-java/five-provider-array-interop-20261017'
    for name, relative in (
        ('verify_donation.py.txt', 'verify_donation.py'),
        ('master-source-manifest.tsv.txt', SUCCESSOR + 'master-source-manifest.tsv'),
        ('source-manifest.tsv.txt', SUCCESSOR + 'source-manifest.tsv'),
    ):
        output = base / HERE_RELATIVE / relative
        output.parent.mkdir(parents=True, exist_ok=True)
        output.write_bytes((text / name).read_bytes())
    for line in (text / 'source-manifest.tsv.txt').read_text().splitlines():
        if not line or line.startswith('#'):
            continue
        relative, before, after, template = line.split('\t')
        output = base / relative
        if before == 'ABSENT':
            if output.exists():
                raise AssertionError('Occupied preflight new source: ' + relative)
        elif digest(output) != before:
            raise AssertionError('Preflight source preimage mismatch: ' + relative)
        payload = (java / template).read_bytes()
        if hashlib.sha256(payload).hexdigest() != after:
            raise AssertionError('Preflight template mismatch: ' + template)
        output.parent.mkdir(parents=True, exist_ok=True)
        output.write_bytes(payload)


class AdmissionContract(unittest.TestCase):
    base = None

    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix='m3-source-admission-case-')
        self.addCleanup(self.temporary.cleanup)
        self.repo = Path(self.temporary.name)
        shutil.copytree(self.base / 'm3', self.repo / 'm3', symlinks=True)
        self.here = self.repo / HERE_RELATIVE
        self.verifier = load_verifier(self.here / 'verify_donation.py')

    def verify(self):
        return self.verifier.verify_source_admission()

    def prior_rows(self):
        packet = self.verifier.read_donation_packet()
        rows = [line.split('\t') for line in (self.here / packet['installed_source_manifest']).read_text().splitlines()
                if line and not line.startswith('#')]
        rows = self.verifier.include_filtered_view_successor(rows)
        rows = self.verifier.include_stored_representative_successor(rows)
        rows = self.verifier.include_path_mask_successor(rows)
        return self.verifier.include_object_state_offset_successor(rows)

    def changed_file_refused(self, relative):
        path = self.here / relative
        original = path.read_bytes()
        path.write_bytes(original + b'\n/* admission drift */\n')
        with self.assertRaisesRegex(SystemExit, 'Installed source drift'):
            self.verify()

    def test_exact_current_source_chain_passes(self):
        _, rows, java, receipt = self.verify()
        self.assertEqual(240, len(rows))
        self.assertEqual(224, len(java))
        self.assertEqual(11, receipt['native_sources'])
        self.assertEqual(1, receipt['resource_files'])
        self.assertEqual(MASTER, receipt['master_reconciliation']['target_commit'])
        self.assertEqual({'unlisted_java_sources': 115, 'unlisted_native_sources': 9,
                          'unlisted_resources': 1, 'stale_java_digests': 8},
                         receipt['master_reconciliation']['existing_admission_gap'])

    def test_unknown_java_source_refused(self):
        (self.here / 'src/main/java/Unknown.java').write_text('class Unknown {}\n')
        with self.assertRaisesRegex(SystemExit, 'Source set changed.*Unknown.java'):
            self.verify()

    def test_unknown_native_source_refused(self):
        (self.here / 'src/main/native/Unknown.c').write_text('int unknown;\n')
        with self.assertRaisesRegex(SystemExit, 'Source set changed.*Unknown.c'):
            self.verify()

    def test_unknown_resource_refused(self):
        (self.here / 'src/main/resources/Unknown.tsv').write_text('unknown\n')
        with self.assertRaisesRegex(SystemExit, 'Source set changed.*Unknown.tsv'):
            self.verify()

    def test_missing_resource_refused(self):
        (self.here / 'src/main/resources/com/m3/collections/M3_SDK_FAMILY_CATALOG.tsv').unlink()
        with self.assertRaisesRegex(SystemExit, 'Installed source is missing'):
            self.verify()

    def test_missing_object_state_offset_receipt_refused(self):
        (self.here / 'qualification/object-state-offset-jni-20261017.tsv').unlink()
        with self.assertRaisesRegex(SystemExit, 'Historical object-state successor receipt is missing'):
            self.verify()

    def test_changed_object_state_offset_receipt_refused(self):
        receipt = self.here / 'qualification/object-state-offset-jni-20261017.tsv'
        receipt.write_bytes(receipt.read_bytes() + b'\n')
        with self.assertRaisesRegex(ValueError, 'object-state receipt SHA-256 drift'):
            self.verify()

    def test_changed_historical_helper_refused(self):
        helper = self.here / 'm3_object_state_offset_receipt.py'
        helper.write_bytes(helper.read_bytes() + b'\n# unapproved helper drift\n')
        with self.assertRaisesRegex(SystemExit, 'Historical object-state admission helper SHA-256 drift'):
            self.verify()

    def test_changed_historical_source_refused(self):
        self.changed_file_refused('src/main/java/com/m3/collections/M3LinkedHashMap.java')

    def test_changed_reconciled_existing_source_refused(self):
        self.changed_file_refused('src/main/java/com/m3/collections/M3BitLane28.java')

    def test_changed_reconciled_newly_admitted_source_refused(self):
        self.changed_file_refused('src/main/java/com/m3/collections/M3IntHashSet.java')

    def test_changed_task_source_refused(self):
        self.changed_file_refused('src/main/java/com/m3/collections/M3ObjectCounts.java')

    def test_changed_reconciled_native_source_refused(self):
        self.changed_file_refused('src/main/native/collections/m3_index_mask_window.c')

    def test_missing_task_source_refused(self):
        (self.here / 'src/test/java/com/m3/collections/M3ObjectCountsTest.java').unlink()
        with self.assertRaisesRegex(SystemExit, 'Installed source is missing'):
            self.verify()

    def test_missing_historical_source_refused(self):
        (self.here / 'src/main/java/com/m3/collections/M3LinkedHashMap.java').unlink()
        with self.assertRaisesRegex(SystemExit, 'Installed source is missing'):
            self.verify()

    def test_missing_master_receipt_refused(self):
        (self.here / SUCCESSOR / 'master-source-manifest.tsv').unlink()
        with self.assertRaisesRegex(SystemExit, 'receipt is missing'):
            self.verify()

    def test_missing_task_receipt_refused(self):
        (self.here / SUCCESSOR / 'source-manifest.tsv').unlink()
        with self.assertRaisesRegex(SystemExit, 'receipt is missing'):
            self.verify()

    def test_missing_historical_receipt_refused(self):
        (self.here / HISTORICAL[1]).unlink()
        with self.assertRaisesRegex(SystemExit, 'Historical successor receipt is missing'):
            self.verify()

    def test_duplicate_receipt_row_refused(self):
        receipt = self.here / SUCCESSOR / 'source-manifest.tsv'
        data = receipt.read_text()
        row = next(line for line in data.splitlines() if line and not line.startswith('#'))
        receipt.write_text(data + row + '\n')
        with self.assertRaisesRegex(SystemExit, 'receipt SHA-256 drift'):
            self.verify()

    def test_unknown_receipt_path_refused(self):
        receipt = self.here / SUCCESSOR / 'source-manifest.tsv'
        receipt.write_text(receipt.read_text().replace('M3ObjectCounts.java\t', 'Unknown.java\t', 1))
        with self.assertRaisesRegex(SystemExit, 'receipt SHA-256 drift'):
            self.verify()

    def test_changed_master_receipt_refused(self):
        receipt = self.here / SUCCESSOR / 'master-source-manifest.tsv'
        receipt.write_bytes(receipt.read_bytes() + b'\n')
        with self.assertRaisesRegex(SystemExit, 'receipt SHA-256 drift'):
            self.verify()

    def test_duplicate_installed_census_refused(self):
        _, rows, _, _ = self.verify()
        with self.assertRaisesRegex(SystemExit, 'Duplicate installed source path'):
            self.verifier.verify_source_census(rows + [rows[0]])

    def test_duplicate_predecessor_census_refused(self):
        rows = self.prior_rows()
        with self.assertRaisesRegex(SystemExit, 'Duplicate .* prior source path'):
            self.verifier.include_master_source_reconciliation(rows + [rows[0]])

    def test_changed_historical_before_digest_refused(self):
        rows = self.prior_rows()
        changed = [(path, '0' * 64 if path.endswith('/M3BitLane28.java') else expected)
                   for path, expected in rows]
        with self.assertRaisesRegex(SystemExit, 'predecessor census drift'):
            self.verifier.include_master_source_reconciliation(changed)

    def test_changed_reconciled_before_digest_refused(self):
        rows = self.verifier.include_master_source_reconciliation(self.prior_rows())
        changed = [(path, '0' * 64 if path.endswith('/M3SpanIndex.java') else expected)
                   for path, expected in rows]
        with self.assertRaisesRegex(SystemExit, 'predecessor census drift'):
            self.verifier.include_five_provider_source_successor(changed)

    def test_occupied_new_task_source_refused(self):
        rows = self.verifier.include_master_source_reconciliation(self.prior_rows())
        rows.append(('m3/collections/src/main/java/com/m3/collections/M3ObjectCounts.java', '0' * 64))
        with self.assertRaisesRegex(SystemExit, 'predecessor census drift'):
            self.verifier.include_five_provider_source_successor(rows)

    def test_noncanonical_census_paths_refused(self):
        invalid = ('/m3/collections/src/Foo.java', 'm3/collections/../src/Foo.java',
                   'm3/collections/src//Foo.java', 'm3/collections/src/./Foo.java',
                   'm3/collections/src\\Foo.java', 'elsewhere/src/Foo.java')
        for path in invalid:
            with self.subTest(path=path), self.assertRaisesRegex(SystemExit, 'Invalid receiver path'):
                self.verifier.verify_source_census([(path, '0' * 64)])

    def test_symlinked_task_receipt_refused(self):
        receipt = self.here / SUCCESSOR / 'source-manifest.tsv'
        alias = self.repo / 'exact-receipt-bytes.tsv'
        receipt.rename(alias)
        receipt.symlink_to(alias)
        with self.assertRaisesRegex(SystemExit, 'receipt is aliased'):
            self.verify()

    def test_symlinked_receipt_parent_refused(self):
        directory = self.here / SUCCESSOR
        alias = self.repo / 'exact-receipt-directory'
        directory.rename(alias)
        directory.symlink_to(alias, target_is_directory=True)
        with self.assertRaisesRegex(SystemExit, 'receipt is aliased'):
            self.verify()

    def test_symlinked_historical_receipt_parent_refused(self):
        directory = (self.here / HISTORICAL[1]).parent
        alias = self.repo / 'exact-historical-receipt-directory'
        directory.rename(alias)
        directory.symlink_to(alias, target_is_directory=True)
        with self.assertRaisesRegex(SystemExit, 'Historical successor receipt is aliased'):
            self.verify()

    def test_symlinked_source_refused_even_with_exact_bytes(self):
        source = self.here / 'src/main/java/com/m3/collections/M3ObjectCounts.java'
        alias = self.repo / 'exact-source-bytes.java'
        source.rename(alias)
        source.symlink_to(alias)
        with self.assertRaisesRegex(SystemExit, 'Installed source is aliased'):
            self.verify()

    def test_symlinked_known_source_directory_refused(self):
        directory = self.here / 'src/test/java'
        alias = self.repo / 'exact-test-sources'
        directory.rename(alias)
        directory.symlink_to(alias, target_is_directory=True)
        with self.assertRaisesRegex(SystemExit, 'Installed source is aliased'):
            self.verify()

    def test_symlinked_unknown_census_directory_refused(self):
        external = self.repo / 'empty-external-directory'
        external.mkdir()
        (self.here / 'src/unknown').symlink_to(external, target_is_directory=True)
        with self.assertRaisesRegex(SystemExit, 'Symlinked source census directory'):
            self.verify()

    def test_duplicate_packet_key_refused(self):
        packet = self.here / 'synexia-donation.json'
        data = packet.read_text()
        packet.write_text('{"installed_source_manifest":"bad",' + data[data.index('{') + 1:])
        with self.assertRaisesRegex(ValueError, 'Duplicate donation metadata key'):
            self.verify()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--target', type=Path, required=True, help='exact repository root to read')
    parser.add_argument('--template-crate', type=Path, help='explicit postimage preflight; never qualifies SDK receiving')
    parser.add_argument('--evidence', type=Path, required=True)
    args = parser.parse_args()
    args.evidence.mkdir(parents=True, exist_ok=True)
    # Check the actual received paths before copying fixtures. Copying through a
    # symlinked ancestor must never turn an aliased receiver into qualified input.
    if not args.template_crate:
        original_path = args.target.absolute() / HERE_RELATIVE / 'verify_donation.py'
        received = load_verifier(original_path)
        received._require_unaliased_file(original_path, 'Received verifier')
        received.verify_source_admission()
    with tempfile.TemporaryDirectory(prefix='m3-source-admission-proof-') as temporary:
        base = Path(temporary)
        copy_input_tree(args.target.resolve(), base)
        if args.template_crate:
            apply_preflight_templates(base, args.template_crate.resolve())
        AdmissionContract.base = base
        verifier = load_verifier(base / HERE_RELATIVE / 'verify_donation.py')
        _, _, _, admission = verifier.verify_source_admission()
        tests = unittest.defaultTestLoader.loadTestsFromTestCase(AdmissionContract)
        names = [test.id() for test in tests]
        log_path = args.evidence / 'source-admission-tests.log'
        with log_path.open('w') as log:
            result = unittest.TextTestRunner(stream=log, verbosity=2).run(tests)
        print(log_path.read_text(), end='')
        receipt = {
            'copyright': 'Copyright 2026 Hitesh Soliwal and contributors', 'license': 'Apache-2.0',
            'schema': 'M3_SOURCE_ADMISSION_NEGATIVE_QUALIFICATION_V1',
            'status': 'PASS' if result.wasSuccessful() else 'FAIL',
            'mode': 'POSTIMAGE_PREFLIGHT_ONLY' if args.template_crate else 'RECEIVED_SOURCE_QUALIFICATION',
            'target': str(args.target.resolve()), 'target_master_commit': MASTER,
            'driver_sha256': digest(Path(__file__)),
            'verifier_sha256': digest(base / HERE_RELATIVE / 'verify_donation.py'),
            'tests': result.testsRun, 'failures': len(result.failures), 'errors': len(result.errors),
            'skips': len(result.skipped), 'cases': names, 'admission': admission,
            'template_install_is_sdk_proof': False,
        }
        (args.evidence / 'source-admission-proof.json').write_text(json.dumps(receipt, indent=2) + '\n')
        if not result.wasSuccessful() or result.skipped:
            raise SystemExit(1)


if __name__ == '__main__':
    main()
