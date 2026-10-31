#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
import tempfile
from pathlib import Path
import unittest
import verify_source_v2 as v2

class ZeroMaskSourceV2Tests(unittest.TestCase):
    def receipt(self):
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        file = Path(tmp.name) / "SOURCE_SUCCESSOR_V2.tsv"
        file.write_text("# original source custody\n" + "".join(
            "\t".join(row) + "\n" for row in v2.EXPECTED), encoding="utf-8")
        return file

    def test_two_original_source_owners(self):
        self.assertEqual(2, len(v2.receipt_rows(self.receipt())))

    def test_modified_sha_refused(self):
        p = self.receipt()
        p.write_text(p.read_text().replace(v2.EXPECTED[0][2], "0" * 64))
        with self.assertRaisesRegex(ValueError, "wrong exact"):
            v2.receipt_rows(p)

    def test_wrong_git_blob_refused(self):
        p = self.receipt()
        p.write_text(p.read_text().replace(v2.EXPECTED[1][3], "f" * 40))
        with self.assertRaisesRegex(ValueError, "wrong exact"):
            v2.receipt_rows(p)

    def test_reordered_and_duplicate_sources_refused(self):
        p = self.receipt()
        p.write_text("\n".join("\t".join(r) for r in reversed(v2.EXPECTED)) + "\n")
        with self.assertRaisesRegex(ValueError, "wrong exact"):
            v2.receipt_rows(p)
        p = self.receipt()
        p.write_text(p.read_text() + "\t".join(v2.EXPECTED[0]) + "\n")
        with self.assertRaisesRegex(ValueError, "wrong exact"):
            v2.receipt_rows(p)

    def test_missing_and_symlinked_receipt_refused(self):
        p = self.receipt()
        p.unlink()
        with self.assertRaisesRegex(ValueError, "missing or aliased"):
            v2.receipt_rows(p)
        target = self.receipt()
        p.symlink_to(target)
        with self.assertRaisesRegex(ValueError, "missing or aliased"):
            v2.receipt_rows(p)

    def test_missing_jni_symbol_detectable(self):
        a = b"JNIEXPORT jint JNICALL Java_com_m3_a(JNIEnv *e) {}"
        b = b"JNIEXPORT jint JNICALL Java_com_m3_b(JNIEnv *e) {}"
        self.assertTrue(set(v2.JNI_SYMBOL.findall(a)) - set(v2.JNI_SYMBOL.findall(b)))

    def test_missing_java_test_entrypoint_detectable(self):
        a = b"public static void main(String[] args) {}"
        b = b"private static void renamed() {}"
        self.assertTrue(set(v2.JAVA_METHOD.findall(a)) - set(v2.JAVA_METHOD.findall(b)))

if __name__ == "__main__":
    unittest.main(verbosity=2)
