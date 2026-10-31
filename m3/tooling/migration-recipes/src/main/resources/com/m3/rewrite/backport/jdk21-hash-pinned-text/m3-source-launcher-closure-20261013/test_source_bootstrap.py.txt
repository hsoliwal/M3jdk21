#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Behavioral proof for the real installer with isolated fake Maven/artifact fixtures."""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

SCRIPT = Path(__file__).with_name("install.sh")


class SourceBootstrapTest(unittest.TestCase):
    def exercise(self, omit_launcher=False, fail_vendor=False, missing_source=False):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            vendor = root / "m3/vendor/third_party/pom.xml"
            module = root / "m3/synexia-import/pure-int-recipe-custody/pom.xml"
            vendor.parent.mkdir(parents=True)
            module.parent.mkdir(parents=True)
            if not missing_source:
                vendor.write_text("<project/>\n")
            module.write_text("<project/>\n")
            bin_dir = root / "bin"
            bin_dir.mkdir()
            fake_mvn = bin_dir / "mvn"
            fake_mvn.write_text(
                "#!/usr/bin/env bash\n"
                "printf '%s\\n' \"$*\" >> \"$M3_CALLS\"\n"
                "case \"$*\" in\n"
                "  *third_party/pom.xml*)\n"
                "    [[ \"${M3_FAIL_VENDOR:-0}\" != 1 ]] || exit 23\n"
                "    if [[ \"${M3_OMIT_LAUNCHER:-0}\" != 1 ]]; then\n"
                "      out=\"$HOME/.m2/repository/org/junit/platform/junit-platform-launcher/1.10.2-m3-source-1/junit-platform-launcher-1.10.2-m3-source-1.jar\"\n"
                "      mkdir -p \"$(dirname \"$out\")\"\n"
                "      printf 'source-built fixture\\n' > \"$out\"\n"
                "    fi\n"
                "    ;;\n"
                "  *pure-int-recipe-custody/pom.xml*)\n"
                "    out=\"$HOME/.m2/repository/com/synexia/synexia-openrewrite-recipes/1.0.0-SNAPSHOT/synexia-openrewrite-recipes-1.0.0-SNAPSHOT.jar\"\n"
                "    mkdir -p \"$(dirname \"$out\")\"\n"
                "    printf 'source-built fixture\\n' > \"$out\"\n"
                "    ;;\n"
                "  *) exit 24 ;;\n"
                "esac\n"
            )
            fake_mvn.chmod(0o755)
            calls = root / "calls.txt"
            env = dict(os.environ, HOME=str(root / "home"),
                       PATH=str(bin_dir) + os.pathsep + os.environ["PATH"],
                       M3_CALLS=str(calls),
                       M3_OMIT_LAUNCHER="1" if omit_launcher else "0",
                       M3_FAIL_VENDOR="1" if fail_vendor else "0")
            (root / "home").mkdir()
            completed = subprocess.run(["bash", str(SCRIPT), str(root)],
                                       env=env, capture_output=True, text=True, check=False)
            return completed, calls.read_text().splitlines() if calls.exists() else []

    def test_full_source_closure_precedes_custody(self):
        completed, calls = self.exercise()
        self.assertEqual(0, completed.returncode, completed.stderr)
        self.assertEqual(2, len(calls))
        self.assertIn("junit-platform-launcher-1.10.2", calls[0])
        self.assertIn("rewrite-java-21", calls[0])
        self.assertIn("-am -DskipTests install", calls[0])
        self.assertIn("pure-int-recipe-custody/pom.xml clean verify install", calls[1])
        self.assertIn("M3JDK21_SYNEXIA_PURE_INT_CUSTODY_INSTALLED", completed.stdout)

    def test_missing_launcher_refuses_custody(self):
        completed, calls = self.exercise(omit_launcher=True)
        self.assertNotEqual(0, completed.returncode)
        self.assertIn("launcher was not installed", completed.stderr)
        self.assertEqual(1, len(calls))

    def test_source_build_failure_refuses_custody(self):
        completed, calls = self.exercise(fail_vendor=True)
        self.assertNotEqual(0, completed.returncode)
        self.assertEqual(1, len(calls))

    def test_missing_vendored_pom_refuses_before_maven(self):
        completed, calls = self.exercise(missing_source=True)
        self.assertNotEqual(0, completed.returncode)
        self.assertEqual([], calls)


if __name__ == "__main__":
    unittest.main()
