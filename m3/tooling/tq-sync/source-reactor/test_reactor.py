# SPDX-License-Identifier: Apache-2.0
"""Execute Maven itself; no dependency resolver or build-output simulation."""
import hashlib
import json
import os
import pathlib
import re
import shutil
import subprocess
import sys
import unittest
import xml.etree.ElementTree as ET

NS = {"m": "http://maven.apache.org/POM/4.0.0"}
HERE = pathlib.Path(__file__).resolve().parent
RECEIVER = HERE.parents[3]
ROOT = pathlib.Path(os.environ["M3_QUALIFICATION_ROOT"]).resolve()
ORACLE = json.loads((HERE / "expected-projects.json").read_bytes())
COMMANDS = []


def blob(data):
    return hashlib.sha1(b"blob " + str(len(data)).encode("ascii") + b"\0" + data).hexdigest()


def ga(gav):
    return ":".join(gav.split(":")[:2])


def gav(path):
    model = ET.fromstring(path.read_bytes())
    return ":".join(model.findtext("m:" + key, namespaces=NS) or ""
                    for key in ("groupId", "artifactId", "version"))


class ReactorContract(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        if not ROOT.is_dir() or ROOT.is_symlink():
            raise RuntimeError("explicit owned qualification root required")
        binding = json.loads((HERE / "source-binding.json").read_bytes())
        if binding["runtimeApplied"] is not False or binding["phaseAdvanced"] is not False:
            raise RuntimeError("scope drift")
        for row in binding["files"]:
            path = RECEIVER / row["targetPath"]
            if path.is_symlink() or blob(path.read_bytes()) != row["gitBlob"]:
                raise RuntimeError("canonical authoring input drift: " + row["targetPath"])
        cls.expected = {row["gav"] for row in ORACLE["selectedProducers"]}
        cls.expected.add(ORACLE["consumer"]["gav"])
        if len(cls.expected) != 57:
            raise RuntimeError("nonempty exact57 source oracle required")
        # This checks versions and all collected POM bytes, not just selected models.
        rows = list(ORACLE["modelInputs"]) + [ORACLE["vendorAggregator"],
            {"path": ORACLE["consumer"]["module"] + "/pom.xml",
             "pomBlob": ORACLE["consumer"]["pomBlob"], "gav": ORACLE["consumer"]["gav"]}]
        for row in rows:
            path = RECEIVER / row.get("path", "m3/vendor/third_party/" + row.get("module", "") + "/pom.xml")
            if path.is_symlink() or blob(path.read_bytes()) != row["pomBlob"] or gav(path) != row["gav"]:
                raise RuntimeError("source model custody drift: " + str(path))
        if len(ORACLE["modelInputs"]) != 121:
            raise RuntimeError("all121 producer models required")
        version = subprocess.check_output(["mvn", "-B", "-ntp", "-Dstyle.color=never", "-version"], text=True, timeout=20)
        (ROOT / "toolchain.log").write_text(version, encoding="utf-8")
        print(version, flush=True)
        cls.observed_maven_identity = version.splitlines()[0] if version.splitlines() else None
        if version.splitlines()[:1] != [ORACLE["qualifiedMavenIdentity"]]:
            raise RuntimeError("exact inventoried preinstalled Maven identity required, no download fallback")
        if not re.search(r"Java version: 21(?:\.|,)", version):
            raise RuntimeError("actual Maven JVM must be JDK21")

    def fixture(self, name):
        receiver = ROOT / ("fixture-" + name)
        receiver.mkdir(exist_ok=False)
        # Original receiver remains immutable. Copy only already admitted sparse POMs.
        for path in sorted((RECEIVER / "m3/vendor/third_party").glob("*/pom.xml")):
            dest = receiver / path.relative_to(RECEIVER)
            dest.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(path, dest)
        for relative in ("m3/vendor/third_party/pom.xml", "m3/tooling/tq-sync/pom.xml",
                         "m3/tooling/tq-sync/source-reactor/pom.xml"):
            dest = receiver / relative
            dest.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(RECEIVER / relative, dest)
        return receiver

    def maven(self, label, receiver=RECEIVER, relative="../../../..", selected="com.m3:tq-sync"):
        command = ["mvn", "-B", "-ntp", "-o",
                   "-s", str(ROOT / "home/.m2/settings.xml"),
                   "-gs", str(ROOT / "home/.m2/global-settings.xml"),
                   "-Dmaven.repo.local=" + str(ROOT / "m2"),
                   "-Dstyle.color=never",
                   "-f", str(receiver / "m3/tooling/tq-sync/source-reactor/pom.xml")]
        if relative is not None:
            command.append("-Dm3.receiver.relative=" + relative)
        command += ["-pl", selected, "-am", "validate"]
        done = subprocess.run(command, cwd=receiver, stdin=subprocess.DEVNULL,
                              stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                              text=True, encoding="utf-8", timeout=90, check=False)
        raw = done.stdout.encode("utf-8")
        (ROOT / (label + ".log")).write_bytes(raw)
        selected_projects = []
        current_ga = None
        for line in done.stdout.splitlines():
            header = re.fullmatch(r"\[INFO\] -+< ([^\s<>]+) >-+\s*", line)
            if header:
                current_ga = header.group(1)
            building = re.fullmatch(r"\[INFO\] Building (.+) \[(\d+)/(\d+)\]\s*", line)
            if building and current_ga is not None:
                version = building.group(1).split()[-1]
                selected_projects.append(current_ga + ":" + version)
                current_ga = None
        COMMANDS.append({"label": label, "argv": command, "returncode": done.returncode,
                         "stdoutBytes": len(raw), "stdoutSha256": hashlib.sha256(raw).hexdigest(),
                         "selectedProjects": selected_projects})
        return done, selected_projects

    def test_exact_source_models(self):
        self.assertEqual(121, len(ORACLE["modelInputs"]))
        self.assertEqual(56, len(ORACLE["selectedProducers"]))
        self.assertEqual(57, len(self.expected))

    def test_actual_offline_selection(self):
        done, selected = self.maven("selected57")
        self.assertEqual(0, done.returncode, done.stdout)
        self.assertEqual(57, len(selected), done.stdout)
        self.assertEqual(self.expected, set(selected))
        self.assertEqual(ORACLE["consumer"]["gav"], selected[-1])
        positions = {ga(value): index for index, value in enumerate(selected)}
        for dependency in ("org.openrewrite:rewrite-java", "org.openrewrite:rewrite-java-21",
                           "org.slf4j:slf4j-api", "org.junit.jupiter:junit-jupiter"):
            self.assertLess(positions[dependency], positions["com.m3:tq-sync"])

    def test_missing_relative_property_refused(self):
        done, _ = self.maven("missing-property", relative=None)
        self.assertNotEqual(0, done.returncode)
        self.assertIn("Child module", done.stdout)
        self.assertIn("does not exist", done.stdout)

    def test_missing_declared_child_refused(self):
        receiver = self.fixture("missing-child")
        (receiver / "m3/vendor/third_party/rewrite-java/pom.xml").unlink()
        done, _ = self.maven("missing-child", receiver)
        self.assertNotEqual(0, done.returncode)
        self.assertIn("Child module", done.stdout)
        self.assertIn("does not exist", done.stdout)

    def test_missing_selector_refused(self):
        done, _ = self.maven("missing-selector", selected="com.m3:no-such-consumer")
        self.assertNotEqual(0, done.returncode)
        self.assertIn("Could not find the selected project in the reactor", done.stdout)

    def test_stale_producer_success_is_incomplete(self):
        receiver = self.fixture("stale-producer")
        path = receiver / "m3/vendor/third_party/rewrite-java/pom.xml"
        old = path.read_bytes()
        before = b"<version>8.17.1-m3-source-1</version>"
        self.assertIn(before, old)
        changed = old.replace(before, b"<version>8.17.1-m3-source-STale</version>", 1)
        path.write_bytes(changed)
        done, selected = self.maven("stale-producer", receiver)
        self.assertEqual(0, done.returncode, done.stdout)
        self.assertNotEqual(self.expected, set(selected))
        self.assertNotIn("org.openrewrite:rewrite-java:8.17.1-m3-source-1", selected)
        self.assertEqual(old, (RECEIVER / "m3/vendor/third_party/rewrite-java/pom.xml").read_bytes())
        # Deliberate counterexample: Maven validate success is not dependency closure proof.


if __name__ == "__main__":
    suite = unittest.defaultTestLoader.loadTestsFromTestCase(ReactorContract)
    result = unittest.TextTestRunner(verbosity=2).run(suite)
    receipt = {"schema": "M3_SOURCE_QUALIFIED_TQ_MODEL_V1",
               "checkoutCommit": os.environ["EXPECTED_SOURCE_SHA"],
               "canonicalSourceCommit": json.loads((HERE / "source-binding.json").read_bytes())["sourceCommit"],
               "sourceBindingSha256": hashlib.sha256((HERE / "source-binding.json").read_bytes()).hexdigest(),
               "mavenIdentity": getattr(ReactorContract, "observed_maven_identity", None),
               "expectedMavenIdentity": ORACLE["qualifiedMavenIdentity"],
               "historicalMaven39Qualification": "NOT_RUN",
               "tests": result.testsRun, "failures": len(result.failures),
               "errors": len(result.errors), "skips": len(result.skipped),
               "successful": result.wasSuccessful(), "commands": COMMANDS,
               "compiled": False, "tqJUnitQualified": False, "originalModelQualified": False,
               "runtimeJniQualified": False, "matchedJdkQualified": False, "phaseAdvanced": False}
    (ROOT / "model-receipt.json").write_text(json.dumps(receipt, indent=2) + "\n", encoding="utf-8")
    raise SystemExit(0 if result.wasSuccessful() and result.testsRun == 6 else 1)
