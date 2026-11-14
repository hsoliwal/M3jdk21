#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors
"""Compiler-parsed source-route regression tests; NOT a String runtime proof."""
from pathlib import Path
import os
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
JAVA_SOURCE = ROOT / 'm3/runtime-integration/src/main/java/M3StringReplacementRoutes.java'
STRING_SOURCE = ROOT / 'src/java.base/share/classes/java/lang/String.java'


def tool(name):
    home = os.environ.get('JAVA_HOME')
    return str(Path(home) / 'bin' / name) if home else name


class GuardImplementationTest(unittest.TestCase):
    def test_compiler_guard_is_present(self):
        self.assertTrue(JAVA_SOURCE.is_file(), "compiler-backed replacement route guard is missing")


class StringReplacementRoutesTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        if not JAVA_SOURCE.is_file():
            raise unittest.SkipTest('implementation not present; presence test remains failing')
        cls.directory = tempfile.TemporaryDirectory(prefix='m3-replacement-routes-')
        cls.addClassCleanup(cls.directory.cleanup)
        cls.classes = Path(cls.directory.name)
        subprocess.run([tool('javac'), '--release', '21', '-Xlint:all', '-Werror',
                        '-d', str(cls.classes), str(JAVA_SOURCE)],
                       check=True, capture_output=True, text=True, timeout=45)
        cls.original = STRING_SOURCE.read_text(encoding='utf-8')

    def run_guard(self, source):
        path = self.classes / 'String.java'
        path.write_text(source, encoding='utf-8')
        return subprocess.run([tool('java'), '-cp', str(self.classes),
                               'M3StringReplacementRoutes', str(path)],
                              capture_output=True, text=True, timeout=30)

    def changed(self, old, new, count=1):
        self.assertGreaterEqual(self.original.count(old), count, old)
        return self.original.replace(old, new, count)

    def assert_refused(self, source):
        result = self.run_guard(source)
        self.assertNotEqual(result.returncode, 0, result.stdout)
        self.assertIn('M3_STRING_REPLACEMENT_ROUTES_FAIL:', result.stderr)
        self.assertNotIn('M3_STRING_REPLACEMENT_ROUTES_OK', result.stdout)

    def test_current_routes_pass(self):
        result = self.run_guard(self.original)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(result.stdout.strip(), 'M3_STRING_REPLACEMENT_ROUTES_OK')

    def test_comments_and_spacing_do_not_change_ast_routes(self):
        source = self.changed('sourceM3.replaceAt(targetM3, replacement, found)',
                              'sourceM3 /* brace } { */ . replaceAt (targetM3, replacement, found)')
        result = self.run_guard(source)
        self.assertEqual(result.returncode, 0, result.stderr)

    def test_public_dispatch_loss_refuses(self):
        self.assert_refused(self.changed('String replaced = m3Replace(storage, trgtStr, targetM3, replStr, replacementM3);',
                                          'String replaced = null;'))

    def test_public_dispatch_in_comment_cannot_satisfy_gate(self):
        call = 'String replaced = m3Replace(storage, trgtStr, targetM3, replStr, replacementM3);'
        self.assert_refused(self.changed(call, '// ' + call + '\n                String replaced = null;'))

    def test_public_dispatch_in_string_cannot_satisfy_gate(self):
        call = 'String replaced = m3Replace(storage, trgtStr, targetM3, replStr, replacementM3);'
        self.assert_refused(self.changed(call, 'String replaced = "' + call + '";'))

    def test_wrong_overload_cannot_hide_missing_public_route(self):
        self.assert_refused(self.changed('replace(CharSequence target, CharSequence replacement)',
                                          'replace(Object target, Object replacement)'))

    def test_helper_loss_refuses(self):
        self.assert_refused(self.changed('private String m3Replace(', 'private String unusedReplace('))

    def test_canonical_arm_loss_refuses(self):
        self.assert_refused(self.changed('sourceM3.replaceAt(targetM3, replacement, found)', 'sourceM3'))

    def test_flat_arm_loss_refuses(self):
        self.assert_refused(self.changed('sourceM3.replaceFlatTarget(this, trgtStr, replacement, found)', 'sourceM3'))

    def test_swapped_target_selection_refuses(self):
        self.assert_refused(self.changed('return new String(targetM3 != null', 'return new String(targetM3 == null'))

    def test_absent_target_identity_loss_refuses(self):
        self.assert_refused(self.changed('if (found < 0) return this;', 'if (found < 0) return null;'))

    def test_replacement_admission_refusal_loss_refuses(self):
        self.assert_refused(self.changed('if (replacement == null) return null;', 'if (replacement == null) return this;'))

    def test_empty_target_route_loss_refuses(self):
        self.assert_refused(self.changed('return new String(sourceM3.replaceEmptyTarget(replacementM3));',
                                          'return this;'))

    def test_regex_first_route_loss_refuses(self):
        self.assert_refused(self.changed('storage.replaceLiteralRegex(this, regex, replacement, true)', 'null'))

    def test_regex_all_route_loss_refuses(self):
        self.assert_refused(self.changed('storage.replaceLiteralRegex(this, regex, replacement, false)', 'null'))

    def test_regex_first_all_flag_swap_refuses(self):
        self.assert_refused(self.changed('storage.replaceLiteralRegex(this, regex, replacement, true)',
                                          'storage.replaceLiteralRegex(this, regex, replacement, false)'))

    def test_regex_fallback_loss_refuses(self):
        self.assert_refused(self.changed('return Pattern.compile(regex).matcher(this).replaceFirst(replacement);',
                                          'return this;'))

    def test_regex_result_admission_loss_refuses(self):
        self.assert_refused(self.changed('if (replaced != null) return replaced;', 'return replaced;'))

    def changed_literal(self, old, new):
        start = self.original.index('public String replace(CharSequence target, CharSequence replacement)')
        end = self.original.index('\n    private String m3Replace(', start)
        literal = self.original[start:end]
        self.assertEqual(literal.count(old), 1, old)
        return self.original[:start] + literal.replace(old, new, 1) + self.original[end:]

    def test_flat_source_coder_loss_refuses(self):
        self.assert_refused(self.changed_literal('boolean thisIsLatin1 = this.isLatin1();',
                                                'boolean thisIsLatin1 = true;'))

    def test_flat_target_coder_loss_refuses(self):
        self.assert_refused(self.changed_literal('boolean trgtIsLatin1 = trgtStr.isLatin1();',
                                                'boolean trgtIsLatin1 = true;'))

    def test_flat_replacement_coder_loss_refuses(self):
        self.assert_refused(self.changed_literal('boolean replIsLatin1 = replStr.isLatin1();',
                                                'boolean replIsLatin1 = true;'))

    def test_flat_latin1_length_loss_refuses(self):
        self.assert_refused(self.changed_literal('StringLatin1.replace(value(), thisLen,',
                                                'StringLatin1.replace(value(), 0,'))

    def test_flat_utf16_length_loss_refuses(self):
        self.assert_refused(self.changed_literal('StringUTF16.replace(value(), thisLen, thisIsLatin1,',
                                                'StringUTF16.replace(value(), 0, thisIsLatin1,'))

    def test_flat_result_admission_loss_refuses(self):
        self.assert_refused(self.changed_literal('if (ret != null) {', 'if (ret == null) {'))

    def test_empty_target_length_arithmetic_loss_refuses(self):
        self.assert_refused(self.changed_literal('Math.addExact(thisLen, Math.multiplyExact(',
                                                'Math.addExact(0, Math.multiplyExact('))

    def test_empty_target_wrong_overflow_exception_refuses(self):
        self.assert_refused(self.changed_literal('throw new OutOfMemoryError(',
                                                'throw new IllegalArgumentException('))

    def test_empty_target_overflow_message_spaces_preserved(self):
        self.assert_refused(self.changed_literal('Required length exceeds implementation limit',
                                                'Requiredlengthexceedsimplementationlimit'))

    def test_empty_target_source_owner_loss_refuses(self):
        source = self.original
        start = source.index('} else { // trgtLen == 0')
        tail = source[start:]
        self.assertIn('M3String storage = m3();', tail)
        self.assert_refused(source[:start] + tail.replace('M3String storage = m3();',
                                                        'M3String storage = null;', 1))

    def test_empty_target_fallback_capacity_loss_refuses(self):
        self.assert_refused(self.changed_literal('StringBuilder sb = new StringBuilder(resultLen);',
                                                'StringBuilder sb = new StringBuilder();'))

    def test_empty_target_initial_insertion_loss_refuses(self):
        self.assert_refused(self.changed_literal('sb.append(replStr);', 'sb.append(this);'))

    def test_empty_target_utf16_loop_loss_refuses(self):
        self.assert_refused(self.changed_literal('sb.append(charAt(i)).append(replStr);',
                                                'sb.append(charAt(0)).append(replStr);'))

    def test_nonfinal_string_owner_refuses(self):
        self.assert_refused(self.changed('public final class String', 'public class String'))

    def test_wrong_class_refuses(self):
        self.assert_refused(self.changed('public final class String', 'public final class NotString'))

    def test_invalid_java_refuses(self):
        self.assert_refused(self.original + '\n} broken syntax')

    def test_missing_source_refuses(self):
        result = subprocess.run([tool('java'), '-cp', str(self.classes), 'M3StringReplacementRoutes',
                                 str(self.classes / 'not-found.java')],
                                capture_output=True, text=True, timeout=30)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('M3_STRING_REPLACEMENT_ROUTES_FAIL:', result.stderr)


if __name__ == '__main__':
    unittest.main(verbosity=2)
