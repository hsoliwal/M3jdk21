#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors
"""Actual source-gate regressions for current UTF-16 cursor and ingress contracts.

These execute the existing checker's complete cursor gate on actual source text.
They do not substitute a String runtime or establish installed-image behavior.
"""
import ast
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[2]
CHECKER = ROOT / 'm3/runtime-integration/check-m3string-invariants.py'
OWNERS = {
    'm3': 'M3String.java',
    'string': 'String.java',
    'search_precompute': 'M3StringSearchPrecompute.java',
    'precompute_bridge': 'M3StringPrecomputeBridge.java',
}


def extract_gate():
    source = ast.parse(CHECKER.read_text(encoding='utf-8'), filename=str(CHECKER))
    names = {'java_mask_non_code', 'java_block_end', 'check_cursor_refinements'}
    definitions = [node for node in source.body
                   if isinstance(node, ast.FunctionDef) and node.name in names]
    if len(definitions) != len(names) or {node.name for node in definitions} != names:
        raise AssertionError('actual cursor gate definitions missing or duplicated')
    invocations = [node.value for node in source.body
                   if isinstance(node, ast.Expr) and isinstance(node.value, ast.Call)
                   and isinstance(node.value.func, ast.Name)
                   and node.value.func.id == 'check_cursor_refinements']
    expected = ['m3', 'search_precompute', 'string', 'precompute_bridge']
    if (len(invocations) != 1 or invocations[0].keywords
            or [ast.dump(arg) for arg in invocations[0].args]
            != [ast.dump(ast.Name(id=name, ctx=ast.Load())) for name in expected]):
        raise AssertionError('actual cursor gate invocation binding drift')
    return compile(ast.Module(body=definitions, type_ignores=[]), str(CHECKER), 'exec')


GATE = extract_gate()


def check_sources(sources):
    def fail(message):
        raise AssertionError(message)
    scope = {'re': re, 'fail': fail}
    exec(GATE, scope)
    scope['check_cursor_refinements'](sources['m3'], sources['search_precompute'],
                                      sources['string'], sources['precompute_bridge'])


INGRESS_CASES = [
    ('char snapshot remains private', 'string',
     'M3String.admit(Arrays.copyOfRange(value, offset, offset + length), 0, length)',
     'M3String.admit(value, offset, length)'),
    ('char snapshot exact extent', 'string',
     'Arrays.copyOfRange(value, offset, offset + length), 0, length)',
     'Arrays.copyOfRange(value, offset, offset + length - 1), 0, length)'),
    ('code-point snapshot remains private', 'string',
     'M3String.admitCodePoints(Arrays.copyOfRange(value, offset, offset + count), 0, count)',
     'M3String.admitCodePoints(value, offset, count)'),
    ('code-point snapshot exact extent', 'string',
     'Arrays.copyOfRange(value, offset, offset + count), 0, count)',
     'Arrays.copyOfRange(value, offset, offset + count - 1), 0, count)'),
]
EQUALITY_CASES = [
    ('canonical exact comparison', 'm3', '? comparePrefix(that, length()) == 0', '? true'),
    ('flat exact comparison', 'm3', ': compareFlatPrefix(other, length()) == 0', ': true'),
    ('hash is rejection only', 'm3', 'if (owner.javaHash != that.owner.javaHash) return false;',
     'if (owner.javaHash == that.owner.javaHash) return true;'),
    ('prefix length is bounded', 'm3', 'Objects.checkFromIndexSize(0, count, other.length());', ''),
    ('different owner offsets compare', 'm3', 'if (left == right && leftStart == rightStart) return 0;',
     'if (left == right) return 0;'),
    ('scalar exact unit comparison', 'm3',
     'int difference = left.charAt(leftStart + index) - right.charAt(rightStart + index);',
     'int difference = 0;'),
    ('mixed exact unit comparison', 'm3', 'int difference = unit - other.charAt(index);',
     'int difference = 0;'),
    ('bridge mixed comparison direction', 'precompute_bridge',
     'difference = -rightStorage.compareFlatPrefix(left, limit);',
     'difference = rightStorage.compareFlatPrefix(left, limit);'),
    ('bridge length tie-break', 'precompute_bridge',
     'return difference != 0 ? difference : length1 - length2;', 'return difference;'),
]
CURSOR_CASES = [
    ('cursor callback reads exact unit', 'm3', 'return at(index);', 'return 0;'),
    ('cursor read bounds', 'm3', 'Objects.checkIndex(index, source.length());', ''),
    ('cursor tuple left extent', 'm3', 'after = Math.min(after, leftLength - position);',
     'after = source.length();'),
    ('cursor tuple right extent', 'm3', 'before = Math.min(before, position);',
     'before = index;'),
    ('cursor left coordinate', 'm3', 'position += tuple.left.start();', 'position = 0;'),
    ('cursor right coordinate', 'm3', 'position += tuple.right.start();', 'position = 0;'),
    ('cursor run lower bound', 'm3', 'logicalBegin = index - before;', 'logicalBegin = 0;'),
    ('cursor scalar offset', 'm3', 'scalarOffset = position - before;', 'scalarOffset = 0;'),
]


class CurrentStringCursorAndIngressTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.sources = {key: (ROOT / 'src/java.base/share/classes/java/lang' / name)
                       .read_text(encoding='utf-8') for key, name in OWNERS.items()}

    def test_actual_current_sources_accepted(self):
        check_sources(self.sources)

    def reject_cases(self, cases):
        refused = 0
        for label, owner, old, new in cases:
            with self.subTest(contract=label):
                self.assertIn(old, self.sources[owner], 'mutation anchor drift: ' + label)
                sources = dict(self.sources)
                sources[owner] = sources[owner].replace(old, new, 1)
                with self.assertRaises(AssertionError, msg='actual gate accepted ' + label):
                    check_sources(sources)
                refused += 1
        self.assertEqual(refused, len(cases))

    def test_mutable_ingress_snapshots_required(self):
        self.reject_cases(INGRESS_CASES)

    def test_equal_hashes_still_require_exact_utf16(self):
        self.reject_cases(EQUALITY_CASES)

    def test_cursor_bounds_and_owner_coordinates_required(self):
        self.reject_cases(CURSOR_CASES)

    def test_comments_and_whitespace_preserve_source_gate(self):
        sources = dict(self.sources)
        sources['m3'] = sources['m3'].replace('return at(index);',
                                             'return /* brace } { */ at ( index );', 1)
        check_sources(sources)

    def test_comment_cannot_replace_exact_comparison(self):
        sources = dict(self.sources)
        sources['m3'] = sources['m3'].replace('? comparePrefix(that, length()) == 0',
                                             '? true /* comparePrefix(that, length()) == 0 */', 1)
        with self.assertRaises(AssertionError):
            check_sources(sources)


if __name__ == '__main__':
    unittest.main(verbosity=2)
