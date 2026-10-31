# SPDX-License-Identifier: Apache-2.0
"""Regression expectations for the target's shared mapping custody function."""
import ast
import copy
import json
from pathlib import Path
import sys
import unittest

CRATE = Path(__file__).resolve().parent
ROOT = CRATE.parents[2]
source = Path(sys.argv.pop(1)) if len(sys.argv) > 1 else ROOT / 'm3/tooling/tq-sync/verify-plan.py'
tree = ast.parse(source.read_text())
function = next(node for node in tree.body if isinstance(node, ast.FunctionDef) and node.name == 'retained_mapping')
namespace = {'copy': copy}
exec(compile(ast.Module(body=[function], type_ignores=[]), str(source), 'exec'), namespace)
retain = namespace['retained_mapping']
resource = CRATE / 'src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/m3-mr'
before = json.loads((resource / 'name-mapping.json.txt.before').read_text())
after = json.loads((resource / 'name-mapping.json.txt').read_text())


class CustodyTest(unittest.TestCase):
    def test_exact_and_additive_authority(self):
        frozen = copy.deepcopy(after)
        retain(before, before)
        retain(after, before)
        self.assertEqual(frozen, after)
        self.assertEqual(52, len(before['migration']['records']))
        self.assertEqual(55, len(after['migration']['records']))

    def test_prior_record_change(self):
        changed = copy.deepcopy(after)
        changed['migration']['records'][0]['status'] = 'pending'
        with self.assertRaises(AssertionError): retain(changed, before)

    def test_prior_record_removal(self):
        changed = copy.deepcopy(after)
        changed['migration']['records'].pop(1)
        with self.assertRaises(AssertionError): retain(changed, before)

    def test_prior_record_reordering(self):
        changed = copy.deepcopy(after)
        changed['migration']['records'][0:2] = reversed(changed['migration']['records'][0:2])
        with self.assertRaises(AssertionError): retain(changed, before)

    def test_duplicate_and_empty_identity(self):
        for identity in (after['migration']['records'][0]['id'], ''):
            changed = copy.deepcopy(after)
            changed['migration']['records'][-1]['id'] = identity
            with self.assertRaises(AssertionError): retain(changed, before)

    def test_gates_unchanged(self):
        changed = copy.deepcopy(after)
        changed['migration']['gates'] = []
        with self.assertRaises(AssertionError): retain(changed, before)

    def test_authority_unchanged(self):
        changed = copy.deepcopy(after)
        changed['migration']['coverage']['source_tree_complete'] = True
        with self.assertRaises(AssertionError): retain(changed, before)


if __name__ == '__main__':
    unittest.main()
