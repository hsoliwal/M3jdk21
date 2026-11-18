# SPDX-License-Identifier: Apache-2.0
"""Discoverable source-identity guard in the existing M3 collections CI suite."""
import contextlib
import importlib.util
import io
from pathlib import Path
import unittest
from unittest import mock
import tempfile


class StreamCurrentContractReceiptTest(unittest.TestCase):
    def test_exact_java_receipts_and_public_erasures(self):
        path = (Path(__file__).resolve().parent / "qualification"
                / "stream-current-compatible-20261118" / "verify.py")
        spec = importlib.util.spec_from_file_location("m3_stream_compat_verify", path)
        self.assertIsNotNone(spec)
        self.assertIsNotNone(spec.loader)
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        capture = io.StringIO()
        with contextlib.redirect_stdout(capture):
            module.verify()
        self.assertIn("M3_STREAM_CURRENT_SOURCE_RECEIPT_OK", capture.getvalue())

    def load_verifier(self):
        path = (Path(__file__).resolve().parent / "qualification"
                / "stream-current-compatible-20261118" / "verify.py")
        spec = importlib.util.spec_from_file_location("m3_stream_compat_verify", path)
        self.assertIsNotNone(spec)
        self.assertIsNotNone(spec.loader)
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        return module

    @contextlib.contextmanager
    def receiving_copy(self, module):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            for relative in module.FILES:
                target = root / relative
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes((module.ROOT / relative).read_bytes())
            with mock.patch.object(module, "ROOT", root):
                yield root

    def test_prior_receipts_and_both_collate_owners_are_sealed(self):
        module = self.load_verifier()
        self.assertEqual({
            "src/jdk.m3.collections/share/classes/jdk/m3/collections/ObjectStream.java",
            "src/jdk.m3.collections/share/classes/jdk/m3/collections/ObjectStreamAdvancedKernels.java",
            "src/jdk.m3.collections/share/classes/jdk/m3/collections/ObjectStreamLoopKernels.java",
            "test/jdk/jdk/m3/collections/ObjectStreamAdvancedContractTest.java",
            "test/jdk/jdk/m3/collections/ObjectStreamLoopContractTest.java",
            "test/jdk/jdk/m3/collections/ObjectStreamCompatibilityGateTest.java",
            "src/jdk.m3.collections/share/classes/jdk/m3/collections/ObjectStreamLanguageKernels.java",
            "test/jdk/jdk/m3/collections/ObjectStreamCollateTest.java",
        }, set(module.FILES))

    def test_every_source_receipt_refuses_byte_drift(self):
        module = self.load_verifier()
        for relative in module.FILES:
            with self.subTest(path=relative), self.receiving_copy(module) as root:
                target = root / relative
                target.write_bytes(target.read_bytes() + b"\n// unqualified drift\n")
                with contextlib.redirect_stdout(io.StringIO()):
                    with self.assertRaisesRegex(RuntimeError, "postimage SHA-256 drift"):
                        module.verify()

    def test_every_missing_receiving_source_refuses(self):
        module = self.load_verifier()
        for relative in module.FILES:
            with self.subTest(path=relative), self.receiving_copy(module) as root:
                (root / relative).unlink()
                with contextlib.redirect_stdout(io.StringIO()):
                    with self.assertRaises(FileNotFoundError):
                        module.verify()

    def test_lost_api_or_delegate_refuses_independently_of_hashes(self):
        module = self.load_verifier()
        source = (module.ROOT / next(iter(module.FILES))).read_text(encoding="utf-8")
        for needle in module.MUST_RETAIN + module.INTEGRATION:
            with self.subTest(contract=needle):
                self.assertIn(needle, source)
                with self.assertRaisesRegex(RuntimeError, "lost native API or JDK delegate"):
                    module.verify_api(source.replace(needle, "removed_contract_marker"))

    def test_duplicate_and_missing_erasures_refuse(self):
        module = self.load_verifier()
        source = (module.ROOT / next(iter(module.FILES))).read_text(encoding="utf-8")
        for needle in module.UNIQUE:
            with self.subTest(contract=needle, mutation="duplicate"):
                with self.assertRaisesRegex(RuntimeError, "duplicate/missing source erasure"):
                    module.verify_api(source + "\n" + needle)
            with self.subTest(contract=needle, mutation="missing"):
                altered = source.replace(
                    needle, needle.replace("public ", "public /* missing signature */ ", 1), 1)
                with self.assertRaisesRegex(RuntimeError, "duplicate/missing source erasure"):
                    module.verify_api(altered)

    def test_null_sentinel_mapwhile_collision_remains_refused(self):
        module = self.load_verifier()
        source = (module.ROOT / next(iter(module.FILES))).read_text(encoding="utf-8")
        start = source.index("public <R> ObjectStream<R> mapWhile(")
        body = source.index("{", start)
        collision = (source[:start]
                     + "public <R> ObjectStream<R> mapWhile(Function<? super T, ? extends R> mapper) "
                     + source[body:])
        with self.assertRaisesRegex(RuntimeError, "null-sentinel mapWhile collides"):
            module.verify_api(collision)


if __name__ == "__main__":
    unittest.main()
