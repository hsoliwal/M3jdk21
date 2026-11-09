#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Adversarial archive fixtures; these tests are not genuine NLP runtime proof."""

import hashlib
import importlib.util
import json
import os
import stat
import subprocess
import tempfile
import unittest
import warnings
import zipfile
from pathlib import Path
from unittest import mock


SPEC = importlib.util.spec_from_file_location("opennlp_pack", Path(__file__).with_name("opennlp_pack.py"))
pack = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(pack)


class FixtureBase(unittest.TestCase):
    """Fixture JARs carry arbitrary bytes; only archive and custody semantics apply."""

    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix="m3-opennlp-pack-test-")
        self.addCleanup(self.temporary.cleanup)
        self.base = Path(self.temporary.name)
        self.inputs = self.base / "inputs"
        self.inputs.mkdir()

    def jar(self, artifact="opennlp-api", entries=(), version=pack.VERSION):
        path = self.inputs / f"{artifact}-{pack.VERSION}.jar"
        with zipfile.ZipFile(path, "w") as archive:
            archive.writestr(f"META-INF/maven/{pack.GROUP}/{artifact}/pom.properties",
                             f"groupId={pack.GROUP}\nartifactId={artifact}\nversion={version}\n")
            archive.writestr("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\r\n\r\n")
            for name, data in entries:
                archive.writestr(name, data)
        return path


class ArchiveFixtures(FixtureBase):

    def test_conflicting_resources_refused(self):
        first = pack._read_jar(self.jar(entries=[("configuration.txt", b"first")]))
        second = pack._read_jar(self.jar("opennlp-runtime", [("configuration.txt", b"second")]))
        with self.assertRaisesRegex(ValueError, "conflicting payload"):
            pack._merge([first, second])

    def test_equal_resources_coalesce_after_exact_comparison(self):
        first = pack._read_jar(self.jar(entries=[("configuration.txt", b"same\x00bytes")]))
        second = pack._read_jar(self.jar("opennlp-runtime", [("configuration.txt", b"same\x00bytes")]))
        payload, custody, _ = pack._merge([first, second])
        self.assertEqual(payload["configuration.txt"], b"same\x00bytes")
        self.assertEqual(len([row for row in custody if row["entry"] == "configuration.txt"]), 2)

    def test_services_preserve_first_occurrence_and_original_bytes(self):
        service = "fixture.spi.Service"
        name = pack.SERVICES + service
        first_bytes = b"# owner one\nfixture.impl.Second\nfixture.impl.First\n"
        second_bytes = b"fixture.impl.First\nfixture.impl.Third # third\n"
        first = pack._read_jar(self.jar(entries=[(name, first_bytes),
            ("fixture/spi/Service.class", b"interface fixture"),
            ("fixture/impl/First.class", b"first fixture"),
            ("fixture/impl/Second.class", b"second fixture")]))
        second = pack._read_jar(self.jar("opennlp-runtime", [(name, second_bytes),
            ("fixture/impl/Third.class", b"third fixture")]))
        payload, _, services = pack._merge([first, second])
        self.assertEqual(services[service], ["fixture.impl.Second", "fixture.impl.First", "fixture.impl.Third"])
        self.assertEqual(payload[pack.CUSTODY + "opennlp-api/" + name], first_bytes)
        self.assertEqual(payload[pack.CUSTODY + "opennlp-runtime/" + name], second_bytes)
        self.assertEqual(payload[name], b"fixture.impl.Second\nfixture.impl.First\nfixture.impl.Third\n")

    def test_missing_provider_class_refused(self):
        item = pack._read_jar(self.jar(entries=[(pack.SERVICES + "fixture.spi.Service", b"fixture.impl.Missing\n")]))
        with self.assertRaisesRegex(ValueError, "absent from CPU aggregate"):
            pack._merge([item])

    def test_different_notices_remain_exact_per_owner(self):
        first = pack._read_jar(self.jar(entries=[("META-INF/NOTICE", b"first notice\r\n")]))
        second = pack._read_jar(self.jar("opennlp-runtime", [("META-INF/NOTICE", b"second notice\n")]))
        payload, _, _ = pack._merge([first, second])
        self.assertEqual(payload[pack.CUSTODY + "opennlp-api/META-INF/NOTICE"], b"first notice\r\n")
        self.assertEqual(payload[pack.CUSTODY + "opennlp-runtime/META-INF/NOTICE"], b"second notice\n")

    def test_unsafe_paths_refused(self):
        for name in ("/absolute", "../escape", "a/../b", "a//b", "./a", "a\\b", "C:drive", "a\0tail"):
            with self.subTest(name=name), self.assertRaises(ValueError):
                pack._safe_name(zipfile.ZipInfo(name))
        for info in (zipfile.ZipInfo("link"), zipfile.ZipInfo("encrypted")):
            info.external_attr = (stat.S_IFLNK | 0o777) << 16
            with self.assertRaises(ValueError):
                pack._safe_name(info)
        encrypted = zipfile.ZipInfo("entry")
        encrypted.flag_bits = 1
        with self.assertRaises(ValueError):
            pack._safe_name(encrypted)

    def test_duplicate_entries_and_file_directory_collision_refused(self):
        with warnings.catch_warnings():
            warnings.simplefilter("ignore", UserWarning)
            path = self.jar(entries=[("same", b"one"), ("same", b"one")])
        with self.assertRaisesRegex(ValueError, "duplicate entry"):
            pack._read_jar(path)
        path = self.jar(entries=[("parent", b"file"), ("parent/child", b"child")])
        with self.assertRaisesRegex(ValueError, "file/directory"):
            pack._read_jar(path)
        path = self.jar(entries=[("parent", b"file"), ("parent/", b"")])
        with self.assertRaisesRegex(ValueError, "file/directory"):
            pack._read_jar(path)
        first = pack._read_jar(self.jar(entries=[("parent", b"file")]))
        second = pack._read_jar(self.jar("opennlp-runtime", [("parent/child", b"child")]))
        with self.assertRaisesRegex(ValueError, "file/directory aggregate"):
            pack._merge([first, second])

    def test_signed_and_sealed_archives_refused(self):
        for name in ("META-INF/KEY.SF", "META-INF/KEY.RSA", "META-INF/SIG-CUSTODY"):
            with self.subTest(name=name), self.assertRaisesRegex(ValueError, "signed JAR"):
                pack._read_jar(self.jar(entries=[(name, b"signature fixture")]))
        for attribute in (b"Sealed: tr\r\n ue\r\n", b"SHA-256-Digest: abc\r\n", b"Signature-Version: 1.0\r\n"):
            path = self.inputs / f"opennlp-api-{pack.VERSION}.jar"
            with zipfile.ZipFile(path, "w") as archive:
                archive.writestr("META-INF/MANIFEST.MF", b"Manifest-Version: 1.0\r\n" + attribute + b"\r\n")
                archive.writestr(f"META-INF/maven/{pack.GROUP}/opennlp-api/pom.properties",
                    f"groupId={pack.GROUP}\nartifactId=opennlp-api\nversion={pack.VERSION}\n")
            with self.subTest(attribute=attribute), self.assertRaisesRegex(ValueError, "signed or sealed"):
                pack._read_jar(path)

    def test_foreign_version_classifiers_and_native_inputs_refused(self):
        with self.assertRaisesRegex(ValueError, "unexpected Maven"):
            pack._read_jar(self.jar(version="2.4.0"))
        for name in ("module-info.class", "META-INF/versions/25/Type.class", "META-INF/m3-donor/foreign", "lib/native.so"):
            with self.subTest(name=name), self.assertRaises(ValueError):
                pack._read_jar(self.jar(entries=[(name, b"foreign")]))
        path = self.jar()
        wrong = path.with_name("opennlp-api-3.0.0-SNAPSHOT-tests.jar")
        wrong.write_bytes(path.read_bytes())
        with self.assertRaisesRegex(ValueError, "classifier"):
            pack._read_jar(wrong)

    def test_stored_jar_is_deterministic_and_preserves_bytes(self):
        payload = {"z/resource": b"last\x00", "a/Type.class": b"synthetic class fixture", "META-INF/MANIFEST.MF": b"Manifest-Version: 1.0\r\n\r\n"}
        first, second = self.base / "first.jar", self.base / "second.jar"
        pack._write_jar(first, payload)
        pack._write_jar(second, dict(reversed(list(payload.items()))))
        self.assertEqual(first.read_bytes(), second.read_bytes())
        with zipfile.ZipFile(first) as archive:
            self.assertIsNone(archive.testzip())
            self.assertEqual({name: archive.read(name) for name in archive.namelist()}, payload)


class SourceCheckoutFixtures(FixtureBase):
    """Real Git checkout rules; index cleanliness alone must not admit drift."""

    def setUp(self):
        super().setUp()
        self.environment = mock.patch.dict(os.environ, {
            "GIT_CONFIG_SYSTEM": os.devnull,
            "GIT_CONFIG_GLOBAL": str(self.base / "global-git-config"),
            "GIT_ATTR_NOSYSTEM": "1"})
        self.environment.start()
        self.addCleanup(self.environment.stop)
        self.source = self.base / "source"
        self.source.mkdir()
        self.git("init", "-q")
        self.git("config", "user.name", "Checkout Fixture")
        self.git("config", "user.email", "fixture@example.invalid")
        self.git("config", "core.autocrlf", "false")
        self.git("config", "core.safecrlf", "false")
        self.name = "launcher.cmd"
        self.lf = b"first line\nsecond line\n"

    def git(self, *args):
        return subprocess.run(["git", "-C", str(self.source), *args], check=True,
                              capture_output=True).stdout

    def commit(self, attributes=b"*.cmd text eol=crlf\n"):
        if attributes is not None:
            (self.source / ".gitattributes").write_bytes(attributes)
        (self.source / self.name).write_bytes(self.lf)
        self.git("add", ".")
        self.git("commit", "-qm", "Committed checkout fixture")
        self.head = self.git("rev-parse", "HEAD").decode().strip()
        self.checkout()

    def checkout(self):
        (self.source / self.name).unlink()
        self.git("checkout", "--", self.name)

    def inventory(self):
        self.assertEqual(self.git("status", "--porcelain=v1", "--untracked-files=all"), b"")
        return pack._source_inventory(self.source, self.head)

    def test_declared_crlf_retains_raw_hash_and_committed_provenance(self):
        self.commit()
        data = (self.source / self.name).read_bytes()
        self.assertEqual(data, self.lf.replace(b"\n", b"\r\n"))
        rows = {row["path"]: row for row in self.inventory()}
        row = rows[self.name]
        self.assertEqual(row["sha256"], hashlib.sha256(data).hexdigest())
        self.assertNotEqual(row["sha256"], hashlib.sha256(self.lf).hexdigest())
        facts = row["checkout"]
        self.assertEqual(facts["attribute_source_head"], self.head)
        self.assertEqual(facts["git_blob"], self.git("rev-parse", self.head + ":" + self.name).decode().strip())
        self.assertEqual(facts["git_blob_sha256"], hashlib.sha256(self.lf).hexdigest())
        self.assertEqual(facts["attributes"], {"text": "set", "eol": "crlf"})
        self.assertNotIn("checkout", rows[".gitattributes"])

    def test_mixed_endings_bare_cr_and_hidden_byte_drift_refused(self):
        self.commit()
        self.git("update-index", "--assume-unchanged", self.name)
        for data in (b"first line\r\nsecond line\n", b"first line\r\nsecond line\r",
                     b"first line\r\nsecond line\r\n\x00"):
            (self.source / self.name).write_bytes(data)
            with self.subTest(data=data), self.assertRaisesRegex(ValueError, "tracked source byte drift"):
                self.inventory()

    def test_undeclared_crlf_refused_when_index_hides_change(self):
        self.commit(attributes=None)
        self.git("update-index", "--assume-unchanged", self.name)
        (self.source / self.name).write_bytes(self.lf.replace(b"\n", b"\r\n"))
        with self.assertRaisesRegex(ValueError, "committed plain text/eol=crlf"):
            self.inventory()

    def test_core_autocrlf_only_cannot_authorize_conversion(self):
        self.commit(attributes=None)
        self.git("config", "core.autocrlf", "true")
        self.checkout()
        self.assertEqual((self.source / self.name).read_bytes(), self.lf.replace(b"\n", b"\r\n"))
        with self.assertRaisesRegex(ValueError, "committed plain text/eol=crlf"):
            self.inventory()

    def test_global_attributes_cannot_authorize_conversion(self):
        self.commit(attributes=None)
        global_attributes = self.base / "global-attributes"
        global_attributes.write_bytes(b"*.cmd text eol=crlf\n")
        self.git("config", "--global", "core.attributesFile", str(global_attributes))
        self.checkout()
        self.assertEqual((self.source / self.name).read_bytes(), self.lf.replace(b"\n", b"\r\n"))
        with self.assertRaisesRegex(ValueError, "committed plain text/eol=crlf"):
            self.inventory()

    def test_auto_text_filter_and_encoding_attributes_refused(self):
        for attributes in (b"*.cmd text=auto eol=crlf\n", b"*.cmd text eol=crlf filter=identity\n",
                           b"*.cmd text eol=crlf filter=unspecified\n",
                           b"*.cmd text eol=crlf working-tree-encoding=UTF-8\n"):
            self.commit(attributes)
            self.assertEqual((self.source / self.name).read_bytes(), self.lf.replace(b"\n", b"\r\n"))
            with self.subTest(attributes=attributes), self.assertRaisesRegex(ValueError, "committed plain text/eol=crlf"):
                self.inventory()

    def test_info_attributes_refused_for_conversion_but_raw_match_unchanged(self):
        self.commit()
        info = self.source / ".git/info/attributes"
        info.write_bytes(b"*.cmd text eol=crlf\n")
        with self.assertRaisesRegex(ValueError, "repository info attributes"):
            self.inventory()
        # Keep active info attributes while making the exact-byte case conversion-free.
        info.write_bytes(b"*.cmd -text\n")
        (self.source / self.name).write_bytes(self.lf)
        self.assertEqual((self.source / self.name).read_bytes(), self.git("show", self.head + ":" + self.name))
        # Refresh native Git stat data without changing the staged blob or HEAD.
        self.git("add", "--", self.name)
        self.assertEqual(self.git("rev-parse", ":" + self.name), self.git("rev-parse", self.head + ":" + self.name))
        self.assertEqual(self.git("rev-parse", "HEAD").decode().strip(), self.head)
        self.assertTrue(all("checkout" not in row for row in self.inventory()))

    def test_info_comments_allowed_but_info_symlink_refused(self):
        self.commit()
        info = self.source / ".git/info/attributes"
        info.write_bytes(b"# comment only\n\n")
        self.assertEqual(len([row for row in self.inventory() if "checkout" in row]), 1)
        info.unlink()
        other = self.base / "external-info-attributes"
        other.write_bytes(b"# still a symlink\n")
        info.symlink_to(other)
        with self.assertRaisesRegex(ValueError, "repository info attributes"):
            self.inventory()


class PrepareFixtures(FixtureBase):
    """A synthetic committed source tree tests pins/refusal, not Maven/NLP behavior."""

    def setUp(self):
        super().setUp()
        self.source = self.base / "source"
        self.source.mkdir()
        model = '<project xmlns="http://maven.apache.org/POM/4.0.0"><modelVersion>4.0.0</modelVersion><artifactId>{}</artifactId><version>' + pack.VERSION + '</version></project>\n'
        for directory in [".", "opennlp-core", "opennlp-core/opennlp-ml", *pack.OWNERS.values()]:
            target = self.source / directory
            target.mkdir(parents=True, exist_ok=True)
            (target / "pom.xml").write_text(model.format(target.name), encoding="utf-8")
        (self.source / "LICENSE").write_bytes(b"fixture license\n")
        (self.source / "NOTICE").write_bytes(b"fixture notice\n")
        for owner in pack.OWNERS:
            self.jar(owner, [(f"fixture/{owner.replace('-', '')}/Type.class", owner.encode())])
        subprocess.run(["git", "init", "-q", str(self.source)], check=True, capture_output=True)
        subprocess.run(["git", "-C", str(self.source), "add", "."], check=True, capture_output=True)
        subprocess.run(["git", "-C", str(self.source), "-c", "user.name=Packaging Fixture",
            "-c", "user.email=fixture@example.invalid", "commit", "-qm", "Synthetic packaging fixture"],
            check=True, capture_output=True)
        self.head = subprocess.check_output(["git", "-C", str(self.source), "rev-parse", "HEAD"], text=True).strip()

    def prepare(self, name="output", inputs=None):
        return pack.prepare(self.source, self.inputs if inputs is None else inputs,
                            self.base / name, expected_source_head=self.head)

    def test_prepare_replay_and_fresh_output(self):
        first = self.prepare("first")
        second = self.prepare("second", list(reversed(sorted(self.inputs.glob("*.jar")))))
        self.assertEqual(first["output_sha256"], second["output_sha256"])
        manifest = json.loads(first["manifest"].read_text())
        self.assertEqual(manifest["source_head"], self.head)
        self.assertEqual(manifest["declared_crlf_checkouts"], [])
        self.assertEqual(len(manifest["inputs"]), 11)
        self.assertTrue(manifest["source_and_jar_posthashes_match"])
        with self.assertRaises(FileExistsError):
            self.prepare("first")

    def test_prepare_seals_declared_checkout_without_normalizing_source_hashes(self):
        (self.source / ".gitattributes").write_bytes(b"*.cmd text eol=crlf\n")
        target = self.source / "launcher.cmd"
        target.write_bytes(b"first\nsecond\n")
        subprocess.run(["git", "-C", str(self.source), "add", "."], check=True, capture_output=True)
        subprocess.run(["git", "-C", str(self.source), "-c", "user.name=Packaging Fixture",
            "-c", "user.email=fixture@example.invalid", "commit", "-qm", "Declared checkout fixture"],
            check=True, capture_output=True)
        self.head = subprocess.check_output(["git", "-C", str(self.source), "rev-parse", "HEAD"], text=True).strip()
        target.unlink()
        subprocess.run(["git", "-C", str(self.source), "checkout", "--", "launcher.cmd"], check=True, capture_output=True)
        raw = target.read_bytes()
        self.assertEqual(raw, b"first\r\nsecond\r\n")
        prepared = self.prepare()
        manifest = json.loads(prepared["manifest"].read_text())
        self.assertEqual(len(manifest["declared_crlf_checkouts"]), 1)
        row = manifest["declared_crlf_checkouts"][0]
        self.assertEqual(row["path"], "launcher.cmd")
        self.assertEqual(row["sha256"], hashlib.sha256(raw).hexdigest())
        self.assertEqual(row["checkout"]["attribute_source_head"], self.head)
        lines = (self.base / "output/SOURCE.sha256").read_text().splitlines()
        self.assertIn(hashlib.sha256(raw).hexdigest() + "  launcher.cmd", lines)
        self.assertEqual(target.read_bytes(), raw)

    def test_wrong_head_and_staged_drift_refused(self):
        with self.assertRaisesRegex(ValueError, "commit drift"):
            pack.prepare(self.source, self.inputs, self.base / "out", expected_source_head="0" * 40)
        (self.source / "NOTICE").write_bytes(b"changed fixture\n")
        subprocess.run(["git", "-C", str(self.source), "add", "NOTICE"], check=True, capture_output=True)
        with self.assertRaisesRegex(ValueError, "clean committed"):
            self.prepare()
        self.assertFalse((self.base / "output").exists())

    def test_missing_owner_and_source_output_refused(self):
        paths = sorted(self.inputs.glob("*.jar"))
        with self.assertRaisesRegex(ValueError, "every selected CPU owner"):
            self.prepare(inputs=paths[:-1])
        with self.assertRaisesRegex(ValueError, "outside the OpenNLP"):
            pack.prepare(self.source, paths, self.source / "output", expected_source_head=self.head)

    def test_input_drift_does_not_publish_success_manifest(self):
        original = pack._write_jar

        def drift(path, payload):
            original(path, payload)
            target = next(self.inputs.glob("*.jar"))
            target.write_bytes(target.read_bytes() + b"input changed during preparation")

        with mock.patch.object(pack, "_write_jar", side_effect=drift):
            with self.assertRaisesRegex(ValueError, "JAR input drift"):
                self.prepare()
        self.assertFalse((self.base / "output/MANIFEST.json").exists())

    def test_source_drift_does_not_publish_success_manifest(self):
        original = pack._write_jar

        def drift(path, payload):
            original(path, payload)
            (self.source / "NOTICE").write_bytes(b"changed during preparation")

        with mock.patch.object(pack, "_write_jar", side_effect=drift):
            with self.assertRaisesRegex(ValueError, "clean committed"):
                self.prepare()
        self.assertFalse((self.base / "output/MANIFEST.json").exists())


if __name__ == "__main__":
    unittest.main()
