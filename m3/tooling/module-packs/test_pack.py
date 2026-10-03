# SPDX-License-Identifier: Apache-2.0
"""Assembler refusal tests. Synthetic archives test locks only, not module readiness."""
import hashlib
import json
import platform
import tempfile
import unittest
from pathlib import Path
import build_pack


class PackLockTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.lock_path = self.root / "lock.json"
        arch = {"amd64": "x86_64", "arm64": "aarch64"}.get(platform.machine(), platform.machine())
        self.lock = {"schema_version": 1, "version": "21.0.12",
                     "os": platform.system().lower(), "arch": arch, "artifacts": []}
        for module in sorted(build_pack.FX_MODULES):
            path = self.root / (module + ".jmod")
            path.write_bytes(b"LOCK_TEST_ONLY_NOT_A_JMOD")
            self.lock["artifacts"].append({"file": path.name, "module": module,
                "sha256": hashlib.sha256(path.read_bytes()).hexdigest(),
                "license": "fixture-only", "source": "fixture"})

    def write(self):
        self.lock_path.write_text(json.dumps(self.lock))

    def rejected(self):
        self.write()
        with self.assertRaises((ValueError, FileNotFoundError)):
            build_pack.load_fx(self.lock_path)

    def test_lock_is_deterministic_but_not_module_admission(self):
        self.write()
        self.assertEqual(build_pack.load_fx(self.lock_path), build_pack.load_fx(self.lock_path))

    def test_checksum_drift(self):
        self.lock["artifacts"][0]["sha256"] = "0" * 64
        self.rejected()

    def test_placeholder_refused(self):
        self.lock["artifacts"][0]["sha256"] = "REPLACE"
        self.rejected()

    def test_provenance_required(self):
        for key in ("source", "license"):
            with self.subTest(key=key):
                value = self.lock["artifacts"][0].pop(key)
                self.rejected()
                self.lock["artifacts"][0][key] = value

    def test_platform_mismatch(self):
        for key in ("os", "arch"):
            with self.subTest(key=key):
                value = self.lock[key]
                self.lock[key] = "wrong"
                self.rejected()
                self.lock[key] = value

    def test_module_collision(self):
        self.lock["artifacts"][1] = self.lock["artifacts"][0]
        self.rejected()

    def test_missing_artifact(self):
        (self.root / self.lock["artifacts"][0]["file"]).unlink()
        self.rejected()

    def test_identity_mismatch(self):
        entry = self.lock["artifacts"][0]
        (self.root / entry["file"]).rename(self.root / "wrong.jmod")
        entry["file"] = "wrong.jmod"
        self.rejected()

    def test_version_and_schema(self):
        for key, bad in (("version", "25.0.1"), ("schema_version", 2)):
            with self.subTest(key=key):
                value = self.lock[key]
                self.lock[key] = bad
                self.rejected()
                self.lock[key] = value

    def test_occupied_output_refused(self):
        home = Path("/usr/lib/jvm/java-21-openjdk-amd64")
        if not home.exists():
            home = Path(__import__("os").environ["JAVA_HOME"])
        with self.assertRaises(FileExistsError):
            build_pack.main(["--jdk", str(home), "--out", str(self.root)])

    def test_wrong_jdk_refused(self):
        (self.root / "release").write_text('JAVA_VERSION="25.0.1"\n')
        with self.assertRaises(ValueError):
            build_pack.java_home(self.root)


if __name__ == "__main__":
    unittest.main()
