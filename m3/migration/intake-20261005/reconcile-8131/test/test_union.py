# SPDX-License-Identifier: Apache-2.0
"""Finite reconciliation checks over exact published registry and packet bytes.

The four positive inputs are retained real artifacts, not fabricated proof receipts.
Negative inputs below are deliberately authored mutations. No test executes Java,
JNI, an upstream recipe, or a downstream product, or transfers their acceptance.
"""
import copy
import hashlib
import importlib.util
import json
from pathlib import Path
import sys
import unittest


TASK = Path(__file__).resolve().parents[1]
NAMES = ('base', 'master', 'head', 'packet')
INPUT_SHA256 = {
    'base': '67d40925b97a38c3c1a85f337806524ac590a540bd39647b3e4b0988f2ae3fbb',
    'master': '536af76dd7b7b7217707662c2f1437af4adaa53af898627b0884e0b4f4603ace',
    'head': '8e229715d6783906b1ee249bb2707cdb5e412146219be6037154927abac893ea',
    'packet': '109173cb43fd91e084415284083d0d0ff92a1039a0b21216b7336c0a2c3dcf2e',
}
DESCRIPTOR = 'synexia.counterpart.MIndexJvmDescriptor'
JCC_IDS = ('synexia.jcc-recipe-laboratory', 'synexia.jcc-java-jni-regression')

_spec = importlib.util.spec_from_file_location('_m3_reconcile_8131_derive', TASK / 'derive.py')
derivation = importlib.util.module_from_spec(_spec)
sys.modules[_spec.name] = derivation
_spec.loader.exec_module(derivation)


def sha(content):
    return hashlib.sha256(content).hexdigest()


class UnionTest(unittest.TestCase):
    def setUp(self):
        self.inputs = {name: (TASK / 'inputs' / (name + '.json')).read_bytes() for name in NAMES}
        self.assertEqual(INPUT_SHA256, {name: sha(content) for name, content in self.inputs.items()})

    def derive(self, inputs=None):
        supplied = self.inputs if inputs is None else inputs
        result = derivation.derive(*(supplied[name] for name in NAMES))
        self.assertEqual({'union', 'packet', 'report'}, set(result))
        self.assertIsInstance(result['union'], bytes)
        self.assertIsInstance(result['packet'], bytes)
        self.assertIsInstance(result['report'], dict)
        return result

    def test_all_master_values_survive_with_exact_disjoint_head_intake_append(self):
        result = self.derive()
        master, head = (json.loads(self.inputs[name]) for name in ('master', 'head'))
        union = json.loads(result['union'])
        retained = master['migration']['records']
        appended = [record for record in head['migration']['records']
                    if record['id'].startswith('synexia.intake.')]
        self.assertEqual((46, 35), (len(retained), len(appended)))
        self.assertTrue({record['id'] for record in retained}.isdisjoint(record['id'] for record in appended))
        records = union['migration']['records']
        self.assertEqual(retained + appended, records)
        self.assertEqual(81, len(records))
        self.assertEqual(81, len({record['id'] for record in records}))
        without_records = copy.deepcopy(union)
        without_records['migration']['records'] = copy.deepcopy(retained)
        self.assertEqual(master, without_records, 'every non-record JSON value must remain exactly master-owned')

    def test_richer_master_descriptor_and_two_blocked_jcc_obligations_are_unchanged(self):
        union = {record['id']: record for record in json.loads(self.derive()['union'])['migration']['records']}
        master = {record['id']: record for record in json.loads(self.inputs['master'])['migration']['records']}
        head = {record['id']: record for record in json.loads(self.inputs['head'])['migration']['records']}
        self.assertNotEqual(master[DESCRIPTOR], head[DESCRIPTOR])
        self.assertEqual(master[DESCRIPTOR], union[DESCRIPTOR])
        self.assertEqual('implemented-unverified', union[DESCRIPTOR]['status'])
        for record_id in JCC_IDS:
            with self.subTest(record=record_id):
                self.assertNotIn(record_id, head)
                self.assertEqual(master[record_id], union[record_id])
                self.assertEqual('blocked', union[record_id]['status'])

    def test_derived_packet_changes_only_authority_hash_without_changing_qualification_epoch(self):
        result = self.derive()
        original = json.loads(self.inputs['packet'])
        derived = json.loads(result['packet'])
        self.assertEqual(INPUT_SHA256['head'], original['authority']['sha256'])
        expected = copy.deepcopy(original)
        expected['authority']['sha256'] = sha(result['union'])
        self.assertNotEqual(original['authority']['sha256'], expected['authority']['sha256'])
        old_hash = original['authority']['sha256'].encode('ascii')
        new_hash = expected['authority']['sha256'].encode('ascii')
        self.assertEqual(1, self.inputs['packet'].count(old_hash))
        self.assertEqual(self.inputs['packet'].replace(old_hash, new_hash), result['packet'])
        self.assertEqual(self.inputs['packet'], result['packet'].replace(new_hash, old_hash))
        self.assertEqual(expected, derived)
        self.assertEqual(original['source'], derived['source'])
        self.assertEqual(original['target'], derived['target'])
        self.assertEqual(original['lanes'], derived['lanes'])
        self.assertEqual(original['owners'], derived['owners'])
        self.assertEqual(original['sourceRoot'], derived['sourceRoot'])

    def test_repeated_derivation_is_byte_deterministic_and_does_not_modify_supplied_inputs(self):
        before = dict(self.inputs)
        first = self.derive()
        fresh_bytes = {name: bytes(bytearray(content)) for name, content in self.inputs.items()}
        second = self.derive(fresh_bytes)
        self.assertEqual(first, second, 'union bytes, packet bytes, and provenance report must all be deterministic')
        self.assertEqual(before, self.inputs)
        self.assertEqual(before, fresh_bytes)
        self.assertEqual(before, {name: (TASK / 'inputs' / (name + '.json')).read_bytes() for name in NAMES})

    def test_each_of_four_exact_input_byte_pins_refuses_even_json_equivalent_whitespace(self):
        for name in NAMES:
            with self.subTest(input=name):
                changed = dict(self.inputs)
                changed[name] += b'\n'
                self.assertEqual(json.loads(self.inputs[name]), json.loads(changed[name]))
                with self.assertRaises(ValueError):
                    derivation.derive(*(changed[key] for key in NAMES))

    def test_historical_counts_duplicate_membership_and_foreign_members_cannot_bypass_pins(self):
        master = json.loads(self.inputs['master'])
        head = json.loads(self.inputs['head'])
        old_master = copy.deepcopy(master)
        old_master['migration']['records'] = [record for record in old_master['migration']['records']
                                              if record['id'] not in JCC_IDS]
        self.assertEqual(44, len(old_master['migration']['records']))
        duplicate_master = copy.deepcopy(master)
        duplicate_master['migration']['records'][-1] = copy.deepcopy(duplicate_master['migration']['records'][-2])
        shortened_head = copy.deepcopy(head)
        intake_index = next(index for index, record in enumerate(shortened_head['migration']['records'])
                            if record['id'].startswith('synexia.intake.'))
        shortened_head['migration']['records'].pop(intake_index)
        foreign_head = copy.deepcopy(head)
        foreign_head['migration']['records'][intake_index]['id'] = 'synexia.intake.foreign.Unreviewed'
        for variant, name, mutated in (('historical-count', 'master', old_master),
                                       ('duplicate-id', 'master', duplicate_master),
                                       ('missing-intake', 'head', shortened_head),
                                       ('foreign-id', 'head', foreign_head)):
            with self.subTest(input=name, mutation=variant):
                supplied = dict(self.inputs)
                supplied[name] = (json.dumps(mutated, indent=2) + '\n').encode()
                with self.assertRaises(ValueError):
                    derivation.derive(*(supplied[key] for key in NAMES))


if __name__ == '__main__':
    unittest.main(verbosity=2)
