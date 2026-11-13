#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Exercise the receiving String ledger gate without importing the whole JDK audit.

The actual checker function is compiled from its AST. Runtime representation and
image checks still execute separately in check-m3string-invariants.py and CI.
"""
import ast
import contextlib
import io
import os
from pathlib import Path
import sys
import unittest

CHECKER = Path(__file__).with_name('check-m3string-invariants.py')
LEDGER = Path(os.environ.get('M3_LEDGER_FIXTURE',
              str(CHECKER.parents[1] / 'docs/string-precompute-completeness.tsv')))
RXM = 'MIndexRegexQueryPlan.Result / MIndexRegexMatch (historical RXM responsibility)'
RXA = 'MIndexRegexProgram + MIndexHybridRegex (historical RXA responsibility)'
RE2 = 'MIndexRe2MechanicalFacts.re2ProgramSize/groupCount/namedGroups'
ADAPTED = ('MIndexTextSignals / MIndexHistogram / IndexTextMetrics',
           'MIndexStringEditDistancePlan', 'MIndexCodeTextSignals / batch')
RESTRICTED = (RXM, RXA, RE2) + ADAPTED


class StringCompletenessTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        module = ast.parse(CHECKER.read_text(encoding='utf-8'), filename=str(CHECKER))
        names = {'fail', 'check_precompute_completeness'}
        functions = [node for node in module.body
                     if isinstance(node, ast.FunctionDef) and node.name in names]
        if {node.name for node in functions} != names:
            raise AssertionError('Receiving checker lost its ledger validation owner')
        namespace = {'sys': sys}
        exec(compile(ast.Module(body=functions, type_ignores=[]), str(CHECKER), 'exec'), namespace)
        cls.check = staticmethod(namespace['check_precompute_completeness'])
        cls.ledger = LEDGER.read_text(encoding='utf-8')

    def refused(self, ledger, diagnostic):
        output = io.StringIO()
        with contextlib.redirect_stderr(output), self.assertRaises(SystemExit) as raised:
            self.check(ledger)
        self.assertEqual(1, raised.exception.code)
        self.assertIn(diagnostic, output.getvalue())

    def altered(self, donor, column, value):
        rows = [line.split('\t') for line in self.ledger.splitlines()]
        matches = [row for row in rows if len(row) == 7 and row[1] == donor]
        self.assertEqual(1, len(matches))
        matches[0][column] = value
        return '\n'.join('\t'.join(row) for row in rows) + '\n'

    def test_current_ledger_is_classified_without_promoting_holds(self):
        self.check(self.ledger)
        rows = {r[1]: r for r in (line.split('\t') for line in self.ledger.splitlines())
                if len(r) == 7}
        self.assertEqual('HOLD_MATCHER_STATE_GAP', rows[RXM][3])
        self.assertEqual('HOLD_MATCHER_STATE_AND_SUBSET_SCOPE', rows[RXA][3])
        self.assertEqual('ADVISORY_NO_DUPLICATE_STRING_STATE', rows[RE2][3])
        for donor in ADAPTED:
            self.assertEqual('HIGHER_LAYER', rows[donor][0])
            self.assertEqual('PORT_ADAPTED_SECOND_PASS', rows[donor][3])

    def test_hold_or_advisory_cannot_be_promoted_or_reclassified(self):
        for donor in RESTRICTED:
            for status in ('IMPLEMENTED_FIXED', 'IMPLEMENTED_INTERNAL', 'OPTIONAL_INTERNAL',
                           'DO_NOT_PORT_TO_JAVA_LANG_STRING'):
                with self.subTest(donor=donor, status=status):
                    self.refused(self.altered(donor, 3, status), 'restricted')

    def test_hold_and_advisory_keep_scope_owner_retention_and_oracle(self):
        for donor in RESTRICTED:
            wrong_scope = 'STRING_RUNTIME' if donor in ADAPTED else 'HIGHER_LAYER'
            for column, value in ((0, wrong_scope), (2, 'java.lang.M3String'),
                                  (4, 'retained per-String regex object'),
                                  (5, 'candidate metadata replaces exact match')):
                with self.subTest(donor=donor, column=column):
                    self.refused(self.altered(donor, column, value), 'restricted')

    def test_special_status_cannot_exempt_an_unrelated_responsibility(self):
        for status in ('HOLD_MATCHER_STATE_GAP', 'HOLD_MATCHER_STATE_AND_SUBSET_SCOPE',
                       'ADVISORY_NO_DUPLICATE_STRING_STATE', 'PORT_ADAPTED_SECOND_PASS'):
            with self.subTest(status=status):
                self.refused(self.altered('MIndexTextPrecomputedFacts', 3, status),
                             'another responsibility')

    def test_missing_hold_advisory_or_existing_required_responsibility_refuses(self):
        for donor in RESTRICTED + ('MIndexUtf16RangeFacts',):
            with self.subTest(donor=donor):
                kept = [line for line in self.ledger.splitlines()
                        if len(line.split('\t')) != 7 or line.split('\t')[1] != donor]
                self.refused('\n'.join(kept), 'ledger missing')

    def test_unknown_status_duplicate_and_unfinished_rows_refuse(self):
        self.refused(self.altered('MIndexTextPrecomputedFacts', 3, 'NEW_UNREVIEWED_STATUS'),
                     'unclassified')
        row = next(line for line in self.ledger.splitlines() if RXM in line)
        self.refused(self.ledger + row + '\n', 'duplicate')
        self.refused(self.altered(RXM, 6, 'TODO classify'), 'unfinished')

    def test_existing_string_runtime_and_implemented_owner_checks_survive(self):
        self.refused(self.altered('MIndexUtf16RangeFacts', 3,
                                 'DO_NOT_PORT_TO_JAVA_LANG_STRING'), 'classified out')
        self.refused(self.altered('MIndexTextPrecomputedFacts', 2, 'no current owner'),
                     'has no target')

    def test_existing_required_disposition_cannot_be_promoted(self):
        self.refused(self.altered('MIndexPrefixZ / MIndexPrefixZCache', 3,
                                 'OPTIONAL_INTERNAL'), 'disposition drift')

    def test_empty_malformed_and_changed_header_refuse(self):
        self.refused(self.ledger.splitlines()[0] + '\n', 'ledger is empty')
        self.refused(self.ledger + 'malformed\trow\n', 'invalid')
        self.refused(self.ledger.replace('scope\t', 'wrong\t', 1), 'header changed')
        self.refused(self.altered(RXM, 6, ''), 'invalid')


if __name__ == '__main__':
    unittest.main(verbosity=2)
