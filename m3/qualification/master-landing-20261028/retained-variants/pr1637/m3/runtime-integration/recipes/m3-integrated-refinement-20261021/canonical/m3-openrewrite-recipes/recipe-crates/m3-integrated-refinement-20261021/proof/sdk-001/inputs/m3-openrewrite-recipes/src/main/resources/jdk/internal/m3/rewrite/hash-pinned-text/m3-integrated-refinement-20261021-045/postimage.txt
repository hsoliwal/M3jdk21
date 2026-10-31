# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
"""Refusal tests for the named current ABI, including the shared window owner."""
import importlib.util
from pathlib import Path
import unittest

SPEC = importlib.util.spec_from_file_location('collection_pack_builder', Path(__file__).parents[1] / 'build.py')
BUILDER = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(BUILDER)

COMPACTOR = {
    'Java_com_m3_collections_M3IndexMaskCompactor_selectNative',
    'Java_com_m3_collections_M3IndexMaskCompactor_rangeAbiNative',
    'Java_com_m3_collections_M3IndexMaskCompactor_selectRangeNative',
    'Java_com_m3_collections_M3IndexMaskCompactor_positionsAbiNative',
    'Java_com_m3_collections_M3IndexMaskCompactor_selectPositionsNative',
    'Java_com_m3_collections_M3IndexMaskCompactor_positionsMasksAbiNative',
    'Java_com_m3_collections_M3IndexMaskCompactor_selectPositionsMasksNative',
    'Java_com_m3_collections_M3IndexMaskCompactor_selectWindowNative',
    'Java_com_m3_collections_M3IndexMaskCompactor_selectLongWindowNative',
}
WINDOW = {'Java_com_m3_collections_M3IndexMaskWindow_scanNative',
          'Java_com_m3_collections_M3IndexMaskWindow_scanBoundedNative'}
PATHMASK = {'Java_com_m3_collections_M3IndexPathMasks_selectNative'}


class NativeAbiRefusalTest(unittest.TestCase):
    def test_existing_owners_and_exact_named_library_union(self):
        self.assertEqual(COMPACTOR, BUILDER.expected_symbols('M3IndexMaskCompactor'))
        self.assertEqual(WINDOW, BUILDER.expected_symbols('M3IndexMaskWindow'))
        self.assertEqual(PATHMASK, BUILDER.expected_symbols('M3IndexPathMasks'))
        self.assertEqual({'m3_collections_mask': ('M3IndexMaskCompactor', 'M3IndexMaskWindow'),
                          'm3_collections_pathmask': ('M3IndexPathMasks',)}, BUILDER.NATIVE_LIBRARIES)
        self.assertEqual(11, len(COMPACTOR | WINDOW))
        BUILDER.require_symbols(COMPACTOR | WINDOW, COMPACTOR | WINDOW, 'mask')
        BUILDER.require_symbols(PATHMASK, PATHMASK, 'pathmask')

    def test_compactor_only_library_refuses_missing_window_symbols(self):
        with self.assertRaisesRegex(ValueError, 'missing=.*M3IndexMaskWindow'):
            BUILDER.require_symbols(COMPACTOR, COMPACTOR | WINDOW, 'mask')

    def test_legacy_one_symbol_expectation_refuses_current_compactor(self):
        with self.assertRaisesRegex(ValueError, 'unexpected='):
            BUILDER.require_symbols(COMPACTOR,
                    {'Java_com_m3_collections_M3IndexMaskCompactor_selectNative'}, 'legacy')

    def test_same_count_unknown_export_cannot_replace_required_symbol(self):
        tampered = (COMPACTOR | WINDOW) - {'Java_com_m3_collections_M3IndexMaskWindow_scanBoundedNative'}
        tampered.add('Java_com_m3_collections_M3IndexMaskWindow_unknownNative')
        self.assertEqual(11, len(tampered))
        with self.assertRaisesRegex(ValueError, 'ABI differs'):
            BUILDER.require_symbols(tampered, COMPACTOR | WINDOW, 'mask')

    def test_duplicate_export_refuses(self):
        symbol = next(iter(PATHMASK))
        with self.assertRaisesRegex(ValueError, 'duplicate'):
            BUILDER.require_symbols([symbol, symbol], PATHMASK, 'pathmask')

    def test_generated_header_names_and_duplicate_refusal(self):
        header = '\n'.join('JNIEXPORT jlong JNICALL ' + name + '\n  (JNIEnv *, jclass);'
                           for name in sorted(WINDOW))
        BUILDER.require_symbols(BUILDER.header_symbols(header), WINDOW, 'window header')
        with self.assertRaisesRegex(ValueError, 'duplicate'):
            BUILDER.require_symbols(BUILDER.header_symbols(header + '\n' + header), WINDOW, 'header')
        with self.assertRaisesRegex(ValueError, 'missing='):
            BUILDER.require_symbols(BUILDER.header_symbols('/* no native declaration */'), WINDOW, 'header')

    def test_wrong_owner_header_refuses_even_with_same_arity(self):
        header = 'JNIEXPORT jintArray JNICALL Java_other_selectNative\n (JNIEnv *, jclass);'
        with self.assertRaisesRegex(ValueError, 'ABI differs'):
            BUILDER.require_symbols(BUILDER.header_symbols(header), PATHMASK, 'pathmask header')

    def test_nm_parser_retains_every_java_symbol_for_exact_check(self):
        lines = ['000000 T ' + symbol for symbol in sorted(COMPACTOR | WINDOW)]
        lines += ['000000 T helper_non_jni', '000000 T Java_unreviewed_native']
        actual = BUILDER.export_symbols('\n'.join(lines))
        self.assertEqual(12, len(actual))
        with self.assertRaisesRegex(ValueError, 'Java_unreviewed_native'):
            BUILDER.require_symbols(actual, COMPACTOR | WINDOW, 'mask')


if __name__ == '__main__':
    unittest.main()
