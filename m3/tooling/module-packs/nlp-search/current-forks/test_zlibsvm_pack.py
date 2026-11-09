#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Explicit-input fixtures seeded from the genuine published zlibsvm 3.0.0 pair.

Run this CLI after the candidate's lint and compile gates. Archive assertions are
fixture evidence; real LibSVM/OpenNLP behavior belongs to the separate Java probe.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import shutil
import sys
import unittest
import warnings
import zipfile
from pathlib import Path
from unittest import mock

import zlibsvm_pack as pack


GENUINE_SHA256 = {
    "zlibsvm-api": "b248a317cadced28516aff243659a550034e463db7b86c753b18e912c227ac08",
    "zlibsvm-core": "1b462da36966aad5b017010216c538efa1589cd8e7cc708e57e38fc8539dd458",
    "zlibsvm-parent.pom": "652db11aa611074ef13d2c278a1978f3eb75588fefb72696d3403a530df30dc6",
    "LICENSE": "670826f9a15f2c3ca50a0381d9f05551f2b0d7e1c939241d32143b7480788d26",
    "NOTICE": "1272b1c6e5cd932e5741abf992bb1cbf66da01d9fbe8e6e10c1bed1bba24b6fb",
}
CONTEXT = None


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def entries(path: Path) -> dict[str, bytes]:
    with zipfile.ZipFile(path) as archive:
        return {info.filename: archive.read(info) for info in archive.infolist() if not info.is_dir()}


class GenuineArchiveFixtures(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        if CONTEXT is None:
            raise RuntimeError("use this file's CLI with explicit genuine input paths")

    def setUp(self):
        self.case = CONTEXT["out"] / self._testMethodName
        self.case.mkdir()
        inputs = self.case / "inputs"
        inputs.mkdir()
        self.files = {}
        for key, source in CONTEXT["inputs"].items():
            target = inputs / source.name
            shutil.copyfile(source, target)
            self.files[key] = target
        self.attempts = []

    def tearDown(self):
        (self.case / "ATTEMPTS.json").write_text(
            json.dumps(self.attempts, indent=2, sort_keys=True) + "\n", encoding="utf-8")

    def prepare(self, name="output", *, jars=None, pins=None):
        if jars is None:
            jars = [self.files["zlibsvm-core"], self.files["zlibsvm-api"]]
        if pins is None:
            pins = {key: digest(path) for key, path in self.files.items()}
        attempt = {"output": name, "jar_paths": [str(path) for path in jars],
                   "expected_sha256": pins,
                   "fixture_sha256": {key: digest(path) for key, path in self.files.items()}}
        self.attempts.append(attempt)
        try:
            result = pack.prepare(jars, self.case / name,
                parent_pom=self.files["zlibsvm-parent.pom"], license_file=self.files["LICENSE"],
                notice_file=self.files["NOTICE"], expected_sha256=pins)
        except Exception as error:
            attempt["outcome"] = type(error).__name__ + ": " + str(error)
            raise
        attempt["outcome"] = "PREPARED_FIXTURE"
        attempt["output_sha256"] = result["output_sha256"]
        return result

    def mutate(self, owner: str, additions=(), replacements=None):
        """Change an isolated genuine fixture copy using ordinary ZIP APIs."""
        path = self.files[owner]
        replacements = replacements or {}
        with zipfile.ZipFile(path) as archive:
            original = [(info, replacements.get(info.filename, archive.read(info)))
                        for info in archive.infolist()]
        with warnings.catch_warnings():
            warnings.simplefilter("ignore", UserWarning)
            with zipfile.ZipFile(path, "w") as archive:
                for info, data in original:
                    archive.writestr(info, data)
                for name, data in additions:
                    archive.writestr(name, data)

    def refusal(self, message, name="output", **kwargs):
        with self.assertRaisesRegex(ValueError, message):
            self.prepare(name, **kwargs)
        self.assertFalse((self.case / name).exists(), "preflight refusal created output")

    def test_genuine_bytes_custody_and_descriptor_facts(self):
        before = {key: digest(path) for key, path in self.files.items()}
        result = self.prepare()
        payload = entries(result["merged_jar"])
        manifest = json.loads(result["manifest"].read_text())
        facts = json.loads(result["descriptor_facts"].read_text())
        custody = {(row["artifact"], row["entry"]): row for row in manifest["entries"]}
        original_keys = set()
        all_classes = {}
        for owner in ("zlibsvm-api", "zlibsvm-core"):
            for name, data in entries(self.files[owner]).items():
                original_keys.add((owner, name))
                row = custody[(owner, name)]
                self.assertEqual(row["sha256"], hashlib.sha256(data).hexdigest())
                self.assertEqual(payload[row["output"]], data)
                expected_name = (f"META-INF/m3-donor/{owner}/{name}"
                                 if name == "META-INF/MANIFEST.MF" else name)
                self.assertEqual(row["output"], expected_name)
                if name.endswith(".class"):
                    self.assertNotIn(name, all_classes)
                    all_classes[name] = data
        self.assertEqual(set(custody), original_keys)
        self.assertEqual(len(all_classes), 28)
        self.assertEqual({name: data for name, data in payload.items() if name.endswith(".class")},
                         all_classes)
        self.assertFalse(any(name.endswith("module-info.class") for name in payload))
        self.assertEqual(payload["META-INF/m3-donor/zlibsvm-parent/pom.xml"],
                         self.files["zlibsvm-parent.pom"].read_bytes())
        for name in ("LICENSE", "NOTICE"):
            for target in ("META-INF/" + name, "META-INF/m3-donor/opennlp-support/" + name):
                self.assertEqual(payload[target], self.files[name].read_bytes())
        self.assertEqual({row["coordinate"] for row in manifest["inputs"]}, {
            "de.hs-heilbronn.mi:zlibsvm-api:3.0.0", "de.hs-heilbronn.mi:zlibsvm-core:3.0.0"})
        self.assertTrue(all(row["embedded_legal_entries"] == [] for row in manifest["inputs"]))
        self.assertEqual(facts["module"], "zlibsvm.core")
        self.assertTrue(facts["open"])
        self.assertEqual(facts["exports"], ["de.hhn.mi.configuration", "de.hhn.mi.domain",
            "de.hhn.mi.exception", "de.hhn.mi.process", "de.hhn.mi.shade.org.apache.commons.lang3",
            "de.hhn.mi.util"])
        self.assertEqual(facts["packages"], facts["exports"])
        self.assertEqual(facts["requires"], [{"module": "libsvm", "modifiers": ["transitive"]},
                                            {"module": "org.slf4j", "modifiers": []}])
        self.assertEqual(facts["uses"], [])
        self.assertEqual(facts["provides"], {})
        self.assertEqual(manifest["input_sha256"], before)
        self.assertEqual(manifest["input_post_sha256"], before)
        self.assertTrue(manifest["all_input_posthashes_match"])
        self.assertEqual(before, {key: digest(path) for key, path in self.files.items()})
        for name, value in result["output_sha256"].items():
            self.assertEqual(digest(self.case / "output" / name), value)

    def test_independent_destinations_and_caller_order_are_deterministic(self):
        first = self.prepare("first")
        second = self.prepare("second", jars=[self.files["zlibsvm-api"], self.files["zlibsvm-core"]])
        self.assertEqual(first["output_sha256"], second["output_sha256"])
        self.assertEqual(first["merged_jar"].read_bytes(), second["merged_jar"].read_bytes())
        with self.assertRaises(FileExistsError):
            self.prepare("first")

    def test_different_artifact_rights_remain_exact(self):
        for owner in ("zlibsvm-api", "zlibsvm-core"):
            self.mutate(owner, [("LICENSE", (owner + " fixture license\r\n").encode()),
                                ("META-INF/NOTICE", (owner + " fixture notice\n").encode())])
        result = self.prepare()
        payload = entries(result["merged_jar"])
        for owner in ("zlibsvm-api", "zlibsvm-core"):
            self.assertEqual(payload[f"META-INF/m3-donor/{owner}/LICENSE"],
                             (owner + " fixture license\r\n").encode())
            self.assertEqual(payload[f"META-INF/m3-donor/{owner}/META-INF/NOTICE"],
                             (owner + " fixture notice\n").encode())
        self.assertEqual(payload["META-INF/LICENSE"], self.files["LICENSE"].read_bytes())
        self.assertEqual(payload["META-INF/NOTICE"], self.files["NOTICE"].read_bytes())

    def test_duplicate_class_owners_refuse_equal_and_different_bytes(self):
        name, data = next((name, data) for name, data in entries(self.files["zlibsvm-api"]).items()
                          if name.endswith(".class"))
        original_core = self.files["zlibsvm-core"].read_bytes()
        for label, value in (("equal", data), ("different", data + b"fixture mutation")):
            with self.subTest(label=label):
                self.files["zlibsvm-core"].write_bytes(original_core)
                self.mutate("zlibsvm-core", [(name, value)])
                self.refusal("class entry has two zlibsvm owners", label)

    def test_resource_collision_refuses_and_equal_bytes_keep_both_origins(self):
        self.mutate("zlibsvm-api", [("shared.txt", b"first\x00resource")])
        self.mutate("zlibsvm-core", [("shared.txt", b"second\x00resource")])
        self.refusal("conflicting payload bytes", "conflict")
        self.mutate("zlibsvm-core", replacements={"shared.txt": b"first\x00resource"})
        result = self.prepare("equal")
        self.assertEqual(entries(result["merged_jar"])["shared.txt"], b"first\x00resource")
        manifest = json.loads(result["manifest"].read_text())
        self.assertEqual(len([row for row in manifest["entries"] if row["entry"] == "shared.txt"]), 2)

    def test_pair_identity_and_exact_hash_admission(self):
        api = self.files["zlibsvm-api"]
        self.refusal("exactly one core and one API", "missing", jars=[api])
        self.refusal("exactly one core and one API", "duplicate", jars=[api, api])
        wrong = api.with_name("zlibsvm-api-3.0.0-tests.jar")
        shutil.copyfile(api, wrong)
        self.refusal("classifier", "classifier", jars=[wrong, self.files["zlibsvm-core"]])
        pins = {key: digest(path) for key, path in self.files.items()}
        pins["zlibsvm-api"] = "0" * 64
        self.refusal("input hash drift", "hash-drift", pins=pins)
        metadata = "META-INF/maven/de.hs-heilbronn.mi/zlibsvm-api/pom.properties"
        self.mutate("zlibsvm-api", replacements={metadata:
            b"groupId=de.hs-heilbronn.mi\nartifactId=zlibsvm-api\nversion=2.0.0\n"})
        self.refusal("artifact, classifier or version", "coordinate")

    def test_shared_archive_admission_on_genuine_fixture_copies(self):
        original = self.files["zlibsvm-api"].read_bytes()
        for label, additions, message in (
            ("unsafe", [("../escape", b"unsafe")], "unsafe archive entry"),
            ("signature", [("META-INF/KEY.SF", b"signature")], "signed JAR"),
            ("duplicate", [("duplicate.txt", b"first"), ("duplicate.txt", b"second")], "duplicate entry"),
            ("custody-parent", [("META-INF/m3-donor", b"file")], "file/directory aggregate"),
        ):
            with self.subTest(label=label):
                self.files["zlibsvm-api"].write_bytes(original)
                self.mutate("zlibsvm-api", additions)
                self.refusal(message, label)

    def test_posthash_drift_never_publishes_success_manifest(self):
        original_writer = pack.archive_owner._write_jar

        def write_then_drift(path, payload):
            original_writer(path, payload)
            notice = self.files["NOTICE"]
            notice.write_bytes(notice.read_bytes() + b"\nfixture post-read drift\n")

        with mock.patch.object(pack.archive_owner, "_write_jar", side_effect=write_then_drift):
            with self.assertRaisesRegex(ValueError, "input drift during zlibsvm aggregation"):
                self.prepare()
        self.assertTrue((self.case / "output/zlibsvm.core.jar").is_file())
        self.assertFalse((self.case / "output/MANIFEST.json").exists())


def main(argv=None) -> int:
    global CONTEXT
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--core-jar", type=Path, required=True)
    parser.add_argument("--api-jar", type=Path, required=True)
    parser.add_argument("--parent-pom", type=Path, required=True)
    parser.add_argument("--license-file", type=Path, required=True)
    parser.add_argument("--notice-file", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args(argv)
    inputs = {"zlibsvm-api": args.api_jar, "zlibsvm-core": args.core_jar,
              "zlibsvm-parent.pom": args.parent_pom, "LICENSE": args.license_file,
              "NOTICE": args.notice_file}
    if any(path.is_symlink() or not path.is_file() for path in inputs.values()):
        raise ValueError("genuine regular fixture seeds required")
    inputs = {key: path.resolve(strict=True) for key, path in inputs.items()}
    output = args.out.absolute()
    if (output.exists() or output.is_symlink() or not output.parent.is_dir()
            or output.parent.resolve() != output.parent
            or any(output.is_relative_to(path.parent) for path in inputs.values())):
        raise ValueError("fresh fixture root with canonical external parent required")
    before = {key: digest(path) for key, path in inputs.items()}
    if before != GENUINE_SHA256:
        raise ValueError("genuine fixture input bytes differ from the reviewed five pins")
    output.mkdir()
    CONTEXT = {"out": output, "inputs": inputs}
    suite = unittest.defaultTestLoader.loadTestsFromTestCase(GenuineArchiveFixtures)
    result = unittest.TextTestRunner(verbosity=2).run(suite)
    after = {key: digest(path) for key, path in inputs.items()}
    success = result.wasSuccessful() and before == after
    report = {"schema": "m3-zlibsvm-genuine-fixtures-v1",
              "status": "FIXTURE_TESTS_PASSED_NOT_RUNTIME_PROOF" if success else "FIXTURE_TESTS_FAILED",
              "inputs": {key: str(path) for key, path in inputs.items()},
              "input_sha256": before, "input_post_sha256": after,
              "tests_run": result.testsRun, "errors": len(result.errors), "failures": len(result.failures),
              "original_inputs_unchanged": before == after, "java_invocations": []}
    (output / "RESULT.json").write_text(json.dumps(report, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    return 0 if success else 1


if __name__ == "__main__":
    sys.exit(main())
