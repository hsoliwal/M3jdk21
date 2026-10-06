#!/usr/bin/env python3
import importlib.util
import pathlib
import shutil
import tempfile
import unittest

HERE = pathlib.Path(__file__).resolve().parent
SPEC = importlib.util.spec_from_file_location("verify", HERE / "verify.py")
MOD = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(MOD)


class VerifyTest(unittest.TestCase):
    def test_current_packet(self):
        MOD.verify()

    def test_history_denominator_fails_closed(self):
        with tempfile.TemporaryDirectory() as tmp:
            copy = pathlib.Path(tmp)
            for name in [
                "DEPENDENCY_GRAPH.tsv", "HISTORY.tsv", "TREE_SUMMARY.tsv",
                "PATH_DELTA.tsv", "POLICY.tsv", "verify.py"
            ]:
                shutil.copy2(HERE / name, copy / name)
            history = (copy / "HISTORY.tsv").read_text(encoding="utf-8").splitlines()
            (copy / "HISTORY.tsv").write_text("\n".join(history[:-1]) + "\n", encoding="utf-8")
            spec = importlib.util.spec_from_file_location("verify_copy", copy / "verify.py")
            module = importlib.util.module_from_spec(spec)
            assert spec.loader is not None
            spec.loader.exec_module(module)
            old = module.HERE
            module.HERE = copy
            try:
                with self.assertRaisesRegex(ValueError, "history denominator"):
                    module.verify()
            finally:
                module.HERE = old


if __name__ == "__main__":
    unittest.main()
