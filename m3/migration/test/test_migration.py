# SPDX-License-Identifier: Apache-2.0
import copy
import hashlib
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

TOOL = Path(__file__).resolve().parents[1] / 'migration.py'
spec = importlib.util.spec_from_file_location('migration', TOOL)
migration = importlib.util.module_from_spec(spec)
spec.loader.exec_module(migration)


def sha(content):
    return hashlib.sha256(content).hexdigest()


class MigrationTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.output = self.root / 'm3/algorithm.java'
        self.output.parent.mkdir()
        self.output.write_bytes(b'candidate\n')
        self.receipt = self.root / 'm3/receipt.json'
        self.receipt.write_text(json.dumps({'runs': [{'id': 'differential', 'status': 'passed',
            'exit_code': 0, 'command': ['java', 'ContractTest'],
            'inputs': {'m3/algorithm.java': sha(b'candidate\n')},
            'stdout_sha256': sha(b'PASS\n')}]}))
        self.record = {
            'id': 'm3.prefix-z', 'kind': 'specialization', 'status': 'implemented-tested',
            'reason': 'Exact candidate artifact tested; no full-JDK claim.',
            'sources': [{'repo': 'hsoliwal/com.synexia', 'commit': 'a' * 40, 'module': 'source',
                'path': 'source/WordFacts.java', 'symbol': 'example.WordFacts',
                'signatures': ['int fact(int)'], 'sha256': sha(b'baseline source'),
                'git_blob_sha1': None, 'fingerprint': None, 'revision_role': 'pinned'}],
            'targets': [{'repo': 'hsoliwal/M3jdk21', 'commit': None, 'module': 'm3',
                'path': 'm3/algorithm.java', 'symbol': 'example.Facts', 'signatures': ['int fact(int)'],
                'sha256': sha(b'candidate\n'), 'git_blob_sha1': None, 'fingerprint': None,
                'revision_role': 'candidate'}],
            'owner': 'example.Facts', 'identity': {'namespace': 'retained input owner', 'generation': 'not translated'},
            'contract': {'differences': ['new package'], 'compatibility_obligations': ['UTF-16 equality'],
                'public_surface': ['int fact(int)']},
            'format': {'schema': None, 'abi': None, 'bootstrap': 'outside java.base'},
            'dependencies': [], 'materialization': {'admission': 'existing owner', 'joins': 'none',
                'outputs': 'int lane', 'cached_facts': 'immutable instance'},
            'recipe': {'id': 'port-v1', 'version': '1', 'path': None, 'sha256': None,
                'preconditions': ['source pin'], 'rollback': 'remove receipt-owned addition'},
            'tests': [{'id': 'differential', 'receipt': 'm3/receipt.json'}],
            'provenance': {'license': 'Apache-2.0', 'publication': 'scoped migration only', 'copied_code': True},
            'sync': {'source_revision': 'a' * 40, 'target_revision': None, 'direction': 'source-to-target',
                'target_adaptations': ['new package'], 'pending': [], 'conflicts': []},
            'lineage': {'previous_sources': [], 'previous_targets': [], 'supersedes': []}
        }
        self.doc = {'schema': 1, 'mappings': [{'source': 'legacy', 'status': 'retained'}],
            'migration': {'version': 1, 'source': {'repo': 'hsoliwal/com.synexia',
                'baseline_commit': 'a' * 40, 'observed_tip': 'b' * 40, 'visibility': 'private'},
                'target': {'repo': 'hsoliwal/M3jdk21', 'baseline_commit': 'c' * 40, 'visibility': 'public'},
                'coverage': {'source_tree_complete': False, 'dependency_closure_complete': False,
                    'inventory_receipt': None, 'uninspected_domains': ['compiler']},
                'gates': [{'id': 'route-c-jit', 'status': 'open', 'reason': 'not executed'}],
                'records': [self.record]}}

    def assertInvalid(self, word=None):
        errors = migration.validate(self.doc, self.root)
        self.assertTrue(errors)
        if word:
            self.assertIn(word, ' '.join(errors))

    def inventory(self, content, complete=False):
        p = self.root / 'inventory.tsv'
        p.write_text(content)
        r = self.root / 'inventory-receipt.json'
        r.write_text(json.dumps({'repo': 'hsoliwal/com.synexia', 'commit': 'd' * 40,
            'inventory_sha256': sha(p.read_bytes()), 'scope_complete': complete,
            'producer': 'com.synexia.m3.inventory.InventoryWriter'}))
        return migration.read_inventory(p, r)

    def test_valid_partial_is_not_complete(self):
        self.assertEqual([], migration.validate(self.doc, self.root))
        self.assertTrue(migration.completion_errors(self.doc))

    def test_malformed_nested_shapes_are_refused_before_completion(self):
        for field, invalid in [('dependencies', [None]), ('contract', []),
                               ('lineage', {'previous_sources': [None], 'previous_targets': [], 'supersedes': []})]:
            doc = copy.deepcopy(self.doc)
            doc['migration']['records'][0][field] = invalid
            self.assertTrue(migration.validate(doc, self.root), field)
        for role in ('source', 'coverage'):
            doc = copy.deepcopy(self.doc)
            doc['migration'][role] = []
            self.assertTrue(migration.validate(doc, self.root), role)

    def test_inventory_from_other_tracking_ref_is_unobserved(self):
        self.record['sources'][0]['tracking_ref'] = 'refs/pull/7526/head'
        inv = self.inventory('path\tsha256\n', complete=True)
        self.assertEqual('source-unobserved', migration.reconcile(self.doc, inv, self.root)['records'][0]['decision'])

    def test_duplicate_mapping_id(self):
        self.doc['migration']['records'].append(copy.deepcopy(self.record))
        self.assertInvalid('duplicate')

    def test_duplicate_json_key_and_nonfinite_number_refused(self):
        for text in ['{"schema":1,"schema":2}', '{"n":NaN}', '{"n":Infinity}']:
            p = self.root / 'bad.json'; p.write_text(text)
            with self.assertRaises(ValueError): migration.load_json(p)

    def test_source_pin_is_required(self):
        self.record['sources'][0]['commit'] = 'main'
        self.assertInvalid('commit')

    def test_target_only_drift_invalidates_test_claim(self):
        self.output.write_text('target adaptation')
        self.assertInvalid('hash')

    def test_receipt_must_match_exact_candidate(self):
        receipt = json.loads(self.receipt.read_text())
        receipt['runs'][0]['inputs']['m3/algorithm.java'] = '0' * 64
        self.receipt.write_text(json.dumps(receipt))
        self.assertInvalid('evidence')

    def test_file_existence_is_not_test_evidence(self):
        self.record['tests'] = []
        self.assertInvalid('evidence')

    def test_failed_receipt_is_not_pass(self):
        receipt = json.loads(self.receipt.read_text())
        receipt['runs'][0]['exit_code'] = 1
        self.receipt.write_text(json.dumps(receipt))
        self.assertInvalid('evidence')

    def test_unpinned_candidate_must_be_labeled(self):
        self.record['targets'][0]['revision_role'] = 'pinned'
        self.assertInvalid('commit')

    def test_unsafe_paths_and_symlinks(self):
        for bad in ['../outside', '/absolute', 'm3/../secret', 'm3\\secret']:
            self.record['targets'][0]['path'] = bad
            self.assertInvalid()
        self.record['targets'][0]['path'] = 'm3/algorithm.java'
        self.output.unlink()
        self.output.symlink_to(self.receipt)
        self.assertInvalid('symlink')

    def test_pending_record_keeps_unknowns_visible(self):
        self.record['status'] = 'blocked'
        self.record['sources'] = []
        self.record['targets'] = []
        self.record['tests'] = []
        self.assertEqual([], migration.validate(self.doc, self.root))
        self.assertTrue(migration.completion_errors(self.doc))

    def test_exclusion_requires_reason(self):
        self.record['status'] = 'excluded'; self.record['reason'] = ''
        self.assertInvalid('reason')

    def test_previous_mapping_cannot_disappear(self):
        previous = copy.deepcopy(self.doc)
        self.doc['migration']['records'] = []
        errors = migration.validate(self.doc, self.root, previous)
        self.assertIn('lineage', ' '.join(errors))

    def test_moves_require_previous_source_lineage(self):
        previous = copy.deepcopy(self.doc)
        self.record['sources'][0]['path'] = 'source/moved/WordFacts.java'
        self.assertTrue(migration.validate(self.doc, self.root, previous))
        self.record['lineage']['previous_sources'] = previous['migration']['records'][0]['sources']
        self.assertEqual([], migration.validate(self.doc, self.root, previous))

    def test_many_to_one_allowed_same_owner(self):
        second = copy.deepcopy(self.record); second['id'] = 'm3.border'
        self.doc['migration']['records'].append(second)
        self.assertEqual([], migration.validate(self.doc, self.root))
        second['owner'] = 'competing.Owner'
        self.assertInvalid('owner')

    def test_three_way_change_classification(self):
        inv = self.inventory('path\tsha256\nsource/WordFacts.java\t' + sha(b'changed source') + '\n')
        result = migration.reconcile(self.doc, inv, self.root)
        self.assertEqual('source-change-review', result['records'][0]['decision'])
        self.output.write_text('target adaptation')
        self.assertEqual('three-way-conflict', migration.reconcile(self.doc, inv, self.root)['records'][0]['decision'])
        inv = self.inventory('path\tsha256\nsource/WordFacts.java\t' + sha(b'baseline source') + '\n')
        self.assertEqual('target-only-adaptation', migration.reconcile(self.doc, inv, self.root)['records'][0]['decision'])

    def test_incomplete_inventory_is_not_deletion(self):
        inv = self.inventory('path\tsha256\n', complete=False)
        self.assertEqual('source-unobserved', migration.reconcile(self.doc, inv, self.root)['records'][0]['decision'])
        inv = self.inventory('path\tsha256\n', complete=True)
        self.assertEqual('source-deletion-review', migration.reconcile(self.doc, inv, self.root)['records'][0]['decision'])

    def test_nonprefix_additions_are_not_lost(self):
        inv = self.inventory('path\tsha256\nsource/DictionaryOffsets.java\t' + sha(b'new') + '\n')
        self.assertEqual(['source/DictionaryOffsets.java'], migration.reconcile(self.doc, inv, self.root)['unmapped_paths'])

    def test_inventory_pin_drift_refused(self):
        self.inventory('path\tsha256\n')
        (self.root / 'inventory.tsv').write_text('changed')
        with self.assertRaises(ValueError):
            migration.read_inventory(self.root / 'inventory.tsv', self.root / 'inventory-receipt.json')

    def test_inventory_duplicates_refused(self):
        row = 'source/WordFacts.java\t' + sha(b'baseline source') + '\n'
        with self.assertRaises(ValueError): self.inventory('path\tsha256\n' + row + row)

    def test_extension_preserves_legacy_and_refuses_drift(self):
        original = json.dumps({'schema': 1, 'mappings': [{'old': True}], 'other': [1, 2]}).encode()
        extension = self.doc['migration']
        output = migration.extend_document(original, extension, sha(original))
        result = json.loads(output)
        self.assertEqual([{'old': True}], result['mappings'])
        self.assertEqual([1, 2], result['other'])
        self.assertEqual(output, migration.extend_document(output, extension, sha(output)))
        with self.assertRaises(ValueError): migration.extend_document(original + b' ', extension, sha(original))
        result['migration']['version'] = 2
        changed = json.dumps(result).encode()
        with self.assertRaises(ValueError): migration.extend_document(changed, extension, sha(changed))

    def test_extension_can_replay_from_canonical_authority(self):
        import contextlib
        import io
        original = copy.deepcopy(self.doc)
        del original['migration']
        base = self.root / 'base.json'
        base.write_text(json.dumps(original))
        canonical = self.root / 'canonical.json'
        canonical.write_text(json.dumps(self.doc))
        output = self.root / 'review.json'
        with contextlib.redirect_stdout(io.StringIO()):
            result = migration.main(['extend', '--base', str(base), '--extension', str(canonical),
                '--expected-sha256', sha(base.read_bytes()), '--output', str(output)])
        self.assertEqual(0, result)
        self.assertEqual(self.doc, json.loads(output.read_text()))
        invalid = copy.deepcopy(self.doc['migration'])
        invalid['records'][0]['dependencies'] = [None]
        with self.assertRaises(ValueError): migration.extend_document(base.read_bytes(), invalid, sha(base.read_bytes()))

    def test_output_creation_preserves_existing_work(self):
        p = self.root / 'generated.json'
        self.assertTrue(migration.write_output(p, b'new'))
        self.assertFalse(migration.write_output(p, b'new'))
        with self.assertRaises(ValueError): migration.write_output(p, b'changed')
        self.assertEqual(b'new', p.read_bytes())

    def test_report_cannot_call_partial_complete(self):
        report = migration.render_report(self.doc)
        self.assertIn('INCOMPLETE', report)
        self.assertIn('m3.prefix-z', report)
        self.assertIn('route-c-jit', report)


if __name__ == '__main__':
    unittest.main(verbosity=2)
