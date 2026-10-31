# SPDX-License-Identifier: Apache-2.0
import importlib.util
import os
from pathlib import Path
import subprocess
import shutil
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("payload_producer", ROOT / "source-overlay/produce.py")
producer = importlib.util.module_from_spec(spec)
spec.loader.exec_module(producer)

class UnnamedLoweringTest(unittest.TestCase):
    def test_unused_variables_compile_without_unnamed_preview(self):
        source = r'''import java.util.function.BiFunction;
class Fixture {
  String literal = "_ \" _";
  char character = '_';
  // _ must stay in this comment.
  /* _ must stay here as well. */
  String block = """
      _ stays in a text block
      """;
  int m3Unused0001;
  BiFunction<String,String,String> fn = (_, _) -> "_";
  Object value(Object input) {
    try { return switch (input) { case String _ -> "text"; default -> "other"; }; }
    catch (RuntimeException _) { return null; }
  }
}
'''
        transformed = producer.rename_unnamed(source)
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "Fixture.java"
            path.write_text(transformed)
            java = os.environ.get("M3_TEST_JDK") or os.environ.get("JAVA_HOME")
            javac = str(Path(java) / "bin/javac") if java else (shutil.which("javac") or "javac")
            result = subprocess.run([javac, "--release", "21", str(path)], capture_output=True, text=True)
            self.assertEqual(0, result.returncode, result.stderr)
        for line in source.splitlines():
            if any(marker in line for marker in ("String literal", "char character", "// _", "/* _", "_ stays")):
                self.assertIn(line, transformed)
        self.assertEqual(transformed, producer.rename_unnamed(transformed))

if __name__ == "__main__":
    unittest.main()
