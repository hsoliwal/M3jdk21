# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Regression for obsolete single-symbol qualification and fail-closed ABI parity."""
import unittest
from build import NATIVE_SYMBOL, verify_exports


class NativeExportsTest(unittest.TestCase):
    def test_additive_header_owned_abi(self):
        capability = NATIVE_SYMBOL.replace("selectNative", "positionsAbiNative")
        header = "\n".join("JNIEXPORT jint JNICALL " + name + " (JNIEnv *, jclass);"
                           for name in (NATIVE_SYMBOL, capability))
        symbols = "\n".join("000000001 T " + name for name in (capability, NATIVE_SYMBOL))
        self.assertEqual(sorted((NATIVE_SYMBOL, capability)), verify_exports(header, symbols))
        with self.assertRaisesRegex(ValueError, "missing="):
            verify_exports(header, "000000001 T " + NATIVE_SYMBOL)
        with self.assertRaisesRegex(ValueError, "extra="):
            verify_exports(header, symbols + "\n000000002 T Java_unowned_extra")

    def test_original_abi_cannot_disappear(self):
        with self.assertRaisesRegex(ValueError, "existing JNI header contract"):
            verify_exports("", "")


if __name__ == "__main__":
    unittest.main()
