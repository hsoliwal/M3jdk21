#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Compose existing sealed Maven recipes on one exclusive receiver checkout.

This controller does not synthesize product source or re-label historical receipts.
The bootstrap provider emits CREATE-only controls. Original Maven/JUnit delivery
methods then emit all 45 product/data targets. A second actual serial replay proves
their source fixed point. Runtime/JNI/JDK/performance qualification stays separate.
"""
from __future__ import annotations
import argparse
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import re
import subprocess
import sys
import tempfile
import unittest
import xml.etree.ElementTree as ET

MODULE = "m3/collections/current-owner-continuity-20261017"
SDK = Path(__file__).resolve().parent
RESOURCE = SDK / "src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/m3-collections-current-owner-continuity-20261017"
PLAN = SDK / "composition.json"


def require(condition, message):
    if not condition:
        raise ValueError(message)


def strict_pairs(values):
    result = {}
    for key, value in values:
        require(key not in result, "duplicate JSON key: " + key)
        result[key] = value
    return result


def read_json(path):
    return json.loads(path.read_text(encoding="utf-8"), object_pairs_hook=strict_pairs)


def digest(value):
    return hashlib.sha256(value).hexdigest()


def git_blob(value):
    return hashlib.sha1(b"blob " + str(len(value)).encode("ascii") + b"\0" + value).hexdigest()


def relative(value):
    require(isinstance(value, str) and value and len(value) <= 4096,
            "invalid relative path")
    require(not value.startswith("/") and not re.match(r"^[A-Za-z]:", value)
            and "\\" not in value and not any(ord(c) < 32 or ord(c) == 127 for c in value),
            "unsafe relative path: " + repr(value))
    parts = value.split("/")
    require(all(part and part not in (".", "..") and part.lower() != ".git" for part in parts),
            "unsafe relative path: " + value)
    require(str(PurePosixPath(value)) == value, "noncanonical relative path")
    return value


def safe(root, value):
    path = root.joinpath(*relative(value).split("/"))
    require(path.is_relative_to(root), "outside exclusive receiver")
    cursor = path
    while cursor != root:
        require(not cursor.is_symlink(), "symlink: " + str(cursor))
        cursor = cursor.parent
    require(not root.is_symlink(), "symlink receiver")
    return path


def raw(root, value):
    path = safe(root, value)
    require(path.is_file(), "missing regular source: " + value)
    return path.read_bytes()


def source_identity(root, expected):
    require(re.fullmatch(r"[0-9a-f]{40}", expected) is not None, "invalid expected source SHA")
    observed = subprocess.check_output(
        ["git", "-C", str(root), "rev-parse", "HEAD"], text=True).strip()
    require(observed == expected, "receiver checkout differs from exact expected source SHA")
    return observed


def run_identity(value, expected):
    require(re.fullmatch(r"[0-9]+-[0-9]+-" + re.escape(expected[:12]), value) is not None,
            "run-id must be GITHUB_RUN_ID-attempt-head12")
    return value


def verify_module():
    pins = read_json(SDK / "pins.json")
    require(pins["schema"] == "M3_COLLECTIONS_CONTINUITY_SDK_PINS_V1", "SDK schema")
    expected = pins["files"]
    require(len(expected) == len({row["path"] for row in expected}), "duplicate SDK pin")
    actual = set()
    for path in SDK.rglob("*"):
        require(not path.is_symlink(), "SDK symlink")
        if path.is_file():
            name = path.relative_to(SDK).as_posix()
            if name != "pins.json":
                actual.add(name)
    require(actual == {row["path"] for row in expected}, "SDK closed-file inventory mismatch")
    for row in expected:
        value = raw(SDK, row["path"])
        require(len(value) == row["bytes"] and digest(value) == row["sha256"],
                "SDK file drift: " + row["path"])
    plan = read_json(PLAN)
    require(plan["schema"] == "M3_COLLECTIONS_CURRENT_CONTINUITY_COMPOSITION_V1", "composition schema")
    require(plan["sourceCustodyOnly"] is True and plan["runtimeQualified"] is False
            and plan["jniQualified"] is False and plan["jdkQualified"] is False
            and plan["performanceQualified"] is False and plan["phaseAdvanced"] is False,
            "source-only boundary changed")
    targets = plan["bootstrap"]
    require(len(targets) <= 256 and len(targets) > 75, "provider target budget")
    require(sum(row["role"] == "IMMUTABLE_HISTORICAL_SCAFFOLD" for row in targets) == 75,
            "canonical scaffold count changed")
    require([row["path"] for row in targets] == sorted({row["path"] for row in targets}),
            "bootstrap path inventory")
    manifest = "# Source-custody bootstrap; all targets CREATE-only; original scaffold bytes retained.\n"
    for row in targets:
        relative(row["path"])
        value = raw(RESOURCE, row["resource"])
        require(digest(value) == row["sha256"] and git_blob(value) == row["gitBlob"],
                "bootstrap template drift: " + row["path"])
        if row["role"] == "RUN_BOUND_POM_ADAPTER":
            insertion = "<directory>${m3.continuity.output}</directory>"
            text = value.decode("utf-8")
            require(text.count(insertion) == 1 and git_blob(text.replace(insertion, "", 1).encode("utf-8"))
                    == row["sourceBlob"], "launcher changes more than donor build output directory")
        manifest += "\t".join((row["path"], "ABSENT", row["sha256"], row["resource"])) + "\n"
    require((RESOURCE / "manifest.tsv").read_text(encoding="utf-8") == manifest,
            "bootstrap manifest differs from sealed source bindings")
    product = plan["products"]
    require(len(product) == 45 and len({row["path"] for row in product}) == 45, "45 product owners")
    require(sum(row["before"] == "ABSENT" for row in product) == 42, "42 additive owners")
    require(not ({row["path"] for row in product} & {row["path"] for row in targets}),
            "bootstrap/product ownership overlap")
    for row in product:
        relative(row["path"])
    require(len(plan["stages"]) == 4, "four original SDK stages required")
    require(sum(len(stage["methods"]) for stage in plan["stages"]) == 34,
            "original distinct SDK method inventory")
    return pins, plan


def precheck(root, plan, after=False):
    for row in plan["bootstrap"]:
        path = safe(root, row["path"])
        if path.exists():
            require(path.is_file() and digest(path.read_bytes()) == row["sha256"],
                    "bootstrap occupied/drifted: " + row["path"])
        elif after:
            raise ValueError("missing generated bootstrap target: " + row["path"])
    for row in plan["products"]:
        path = safe(root, row["path"])
        if path.exists():
            require(path.is_file(), "product not regular: " + row["path"])
            value = digest(path.read_bytes())
            admitted = {row["after"]} if after else {row["before"], row["after"]}
            require(value in admitted, "product source drift: " + row["path"])
        else:
            require(not after and row["before"] == "ABSENT",
                    "missing required product: " + row["path"])
    changed = {row["path"]: row for row in plan["products"] if row["before"] != "ABSENT"}
    for row in plan["preserve"]:
        value = raw(root, row["path"])
        if row["path"] in changed:
            admission = changed[row["path"]]
            require(digest(value) in ({admission["after"]} if after
                    else {admission["before"], admission["after"]}),
                    "sealed count transition differs: " + row["path"])
        else:
            require(git_blob(value) == row["gitBlob"], "preserved current source drift: " + row["path"])
    allowed = {row["path"] for row in plan["preserve"]}
    allowed.update(row["path"] for row in plan["products"])
    for owner in ("m3/collections/src", "m3/collections/interop/src"):
        directory = safe(root, owner)
        require(directory.is_dir(), "missing current source directory")
        for path in directory.rglob("*"):
            require(not path.is_symlink(), "source tree symlink: " + str(path))
            if path.is_file():
                relative_path = path.relative_to(root).as_posix()
                require(relative_path in allowed, "unsealed current source: " + relative_path)


def check_reports(directory, classname, methods):
    files = sorted(directory.glob("TEST-*.xml"))
    require(bool(files), "missing actual Surefire XML: " + str(directory))
    observed = []
    for path in files:
        tree = ET.parse(path)
        suite = tree.getroot()
        require(suite.tag == "testsuite", "unexpected Surefire document")
        require(int(suite.attrib.get("tests", "-1")) == len(suite.findall("testcase")),
                "Surefire suite/testcase count mismatch")
        for flag in ("errors", "failures", "skipped"):
            require(int(suite.attrib.get(flag, "-1")) == 0, "nonzero Surefire " + flag)
        for case in suite.findall("testcase"):
            require(all(child.tag in ("system-out", "system-err") for child in case),
                    "non-passing Surefire testcase")
            observed.append((case.attrib.get("classname"), case.attrib.get("name")))
    require(len(observed) == len(set(observed)), "duplicate actual Surefire method")
    expected = {(classname, name) for name in methods}
    require(set(observed) == expected and len(observed) == len(expected),
            "actual named SDK method inventory mismatch")
    return [{"path": str(path), "sha256": digest(path.read_bytes())} for path in files]


def check_receipt(root, path, expected):
    values = {}
    source = raw(root, path)
    previous = ""
    for line in source.decode("utf-8").splitlines():
        if not line or line.startswith("#"):
            continue
        cells = line.split("\t")
        require(len(cells) == 2 and re.fullmatch(r"[0-9a-f]{64}", cells[1]) is not None,
                "malformed actual delivery receipt: " + path)
        require(cells[0] not in values and previous < cells[0], "duplicate/unsorted delivery receipt")
        previous = cells[0]
        values[cells[0]] = cells[1]
    require(values == expected, "actual delivery receipt differs from exact owner outputs: " + path)
    for name, sha in values.items():
        require(digest(raw(root, name)) == sha, "receipt/output mismatch: " + name)
    return {"path": path, "sha256": digest(source), "outputs": len(values)}


def execute(command, root, environment, log):
    print("M3_COLLECTIONS_CONTINUITY_COMMAND " + json.dumps(command), flush=True)
    require(not log.exists(), "stale command log: " + str(log))
    with log.open("xb") as stream:
        process = subprocess.run(command, cwd=root, env=environment,
                                 stdout=stream, stderr=subprocess.STDOUT, check=False)
    if process.returncode:
        with log.open("rb") as stream:
            stream.seek(max(0, log.stat().st_size - 16384))
            print(stream.read().decode("utf-8", errors="replace"), file=sys.stderr)
    require(process.returncode == 0, "actual Maven stage failed: " + str(log))


def snapshot(root, plan):
    return {row["path"]: digest(raw(root, row["path"]))
            for row in plan["bootstrap"] + plan["products"]}


def deliver(arguments, pins, plan, root):
    require(arguments.maven_repository and arguments.temp_root, "explicit cache and temp roots required")
    cache = Path(arguments.maven_repository).absolute()
    temporary = Path(arguments.temp_root).absolute()
    require(cache.is_absolute() and temporary.is_absolute(), "cache/temp must be absolute")
    require(cache.is_dir() and temporary.is_dir(), "workflow must provision owned cache/temp after storage gate")
    require(not cache.is_symlink() and not temporary.is_symlink(), "cache/temp symlink")
    run = safe(root, MODULE + "/target/" + arguments.run_id)
    require(run.is_dir(), "workflow must provision run-bound directory after its storage gate")
    result = run / "delivery.json"
    require(not result.exists() and not result.is_symlink(), "existing run receipt; use fresh run-id")
    environment = os.environ.copy()
    environment.update(TEMP=str(temporary), TMP=str(temporary), TMPDIR=str(temporary),
                       MAVEN_USER_HOME=str(temporary / "maven-user-home"))
    common = ["mvn", "-B", "-ntp", "-Dmaven.repo.local=" + str(cache),
              "-Djava.io.tmpdir=" + str(temporary),
              "-Duser.home=" + str(temporary / "home"),
              "-Dm3.continuity.runId=" + arguments.run_id,
              "-Dm3.continuity.expectedSourceSha=" + arguments.expected_source_sha]
    rounds = []
    first = None
    for label in ("first", "repeat"):
        parent = run / "delivery" / label
        require(not parent.exists(), "stale delivery stage: " + str(parent))
        parent.mkdir(parents=True)
        precheck(root, plan)
        output = parent / "sdk-bootstrap"
        command = common + ["-f", str(SDK / "pom.xml"),
                  "-Dm3.continuity.output=" + str(output),
                  "-Dm3.collections.continuity.target=" + str(root), "test"]
        execute(command, root, environment, parent / "bootstrap.log")
        bootstrap_reports = check_reports(output / "surefire-reports",
                                           plan["bootstrapTestClass"], plan["bootstrapMethods"])
        bootstrap_receipt = check_receipt(root,
            (output / "bootstrap-receipt.tsv").relative_to(root).as_posix(),
            {row["path"]: row["sha256"] for row in plan["bootstrap"]})
        stages = []
        for stage in plan["stages"]:
            precheck(root, plan)
            owner_output = parent / ("sdk-" + stage["name"])
            command = common + ["-f", str(safe(root, stage["launcher"])),
                      "-Dm3.continuity.output=" + str(owner_output),
                      "-D" + stage["deliveryProperty"] + "=" + str(root), "test"]
            execute(command, root, environment, parent / (stage["name"] + ".log"))
            reports = check_reports(owner_output / "surefire-reports", stage["testClass"], stage["methods"])
            expected = {row["path"]: row["after"] for row in plan["products"]
                        if row["stage"] == stage["name"]}
            receipt = check_receipt(root, stage["receipt"], expected)
            stages.append({"name": stage["name"], "methods": stage["methods"],
                           "reports": reports, "receipt": receipt})
        precheck(root, plan, after=True)
        observed = snapshot(root, plan)
        if first is None:
            first = observed
        else:
            require(first == observed, "actual second full composition changed product/bootstrap bytes")
        rounds.append({"label": label, "bootstrapReports": bootstrap_reports,
                       "bootstrapReceipt": bootstrap_receipt, "stages": stages})
    receipt = {"schema": "M3_COLLECTIONS_CURRENT_CONTINUITY_DELIVERY_V1",
               "sourceSha": arguments.expected_source_sha, "runId": arguments.run_id,
               "sdkPinsSha256": digest((SDK / "pins.json").read_bytes()),
               "compositionSha256": digest(PLAN.read_bytes()),
               "bootstrapTargets": len(plan["bootstrap"]), "canonicalScaffoldTargets": 75,
               "productTargets": 45, "preservedCurrentFiles": len(plan["preserve"]),
               "actualDistinctOriginalSdkMethods": 34,
               "actualOriginalSdkMethodExecutions": 68,
               "actualBootstrapMethodExecutions": 2 * len(plan["bootstrapMethods"]),
               "fullCompositionReplays": 2, "sourceFixedPoint": True,
               "rounds": rounds, "outputs": first,
               "sourceCustodyOnly": True, "runtimeQualified": False, "jniQualified": False,
               "jdkQualified": False, "performanceQualified": False, "phaseAdvanced": False}
    with result.open("x", encoding="utf-8") as stream:
        stream.write(json.dumps(receipt, indent=2, sort_keys=True) + "\n")
    print("M3_COLLECTIONS_CURRENT_CONTINUITY_DELIVERY_PASS source-only; runtime/JNI/JDK/performance remain separate",
          flush=True)


class ControllerTest(unittest.TestCase):
    def test_path_fence(self):
        for value in ("", "/x", "C:/x", "a\\b", "a//b", "a/../b", "a/.GIT/config", "a/./b", "a/\x00b"):
            with self.assertRaises(ValueError):
                relative(value)
        self.assertEqual("m3/collections/recipe/pom.xml", relative("m3/collections/recipe/pom.xml"))

    def test_duplicate_json_refusal(self):
        with self.assertRaises(ValueError):
            json.loads('{"key":1,"key":2}', object_pairs_hook=strict_pairs)

    def test_exact_run_binding(self):
        expected = "0123456789abcdef0123456789abcdef01234567"
        self.assertEqual("123-2-0123456789ab", run_identity("123-2-0123456789ab", expected))
        for value in ("123-2-other", "../x", "123-2-0123456789ac", "123-0123456789ab"):
            with self.assertRaises(ValueError):
                run_identity(value, expected)

    @staticmethod
    def fixture(directory):
        root = Path(directory)
        core = root / "m3/collections/src"
        interop = root / "m3/collections/interop/src"
        core.mkdir(parents=True); interop.mkdir(parents=True)
        name = "m3/collections/src/current.java"
        (root / name).write_bytes(b"current\n")
        plan = {"bootstrap": [{"path": "m3/collections/counts/README.md", "sha256": digest(b"scaffold\n")}],
                "products": [
                    {"path": name, "before": digest(b"current\n"), "after": digest(b"after\n")},
                    {"path": "m3/collections/src/added.java", "before": "ABSENT", "after": digest(b"added\n")}],
                "preserve": [{"path": name, "gitBlob": git_blob(b"current\n")}]}
        precheck(root, plan)
        return root, plan

    def test_global_product_precheck_refuses_without_writes(self):
        for mode in ("occupied-add", "drift-required", "missing-required"):
            with tempfile.TemporaryDirectory(prefix="m3-continuity-global-", dir=os.environ["TMPDIR"]) as directory:
                root, plan = self.fixture(directory)
                target = root / plan["products"][1 if mode == "occupied-add" else 0]["path"]
                if mode == "missing-required":
                    target.unlink()
                else:
                    target.write_bytes(b"foreign\n")
                before = {path.relative_to(root).as_posix(): path.read_bytes()
                          for path in root.rglob("*") if path.is_file()}
                with self.assertRaisesRegex(ValueError, re.escape(target.relative_to(root).as_posix())):
                    precheck(root, plan)
                after = {path.relative_to(root).as_posix(): path.read_bytes()
                         for path in root.rglob("*") if path.is_file()}
                self.assertEqual(before, after)

    def test_unknown_source_and_symlink_parent_refuse_without_writes(self):
        for mode in ("unknown-source", "symlink-parent"):
            with tempfile.TemporaryDirectory(prefix="m3-continuity-source-", dir=os.environ["TMPDIR"]) as directory:
                root, plan = self.fixture(directory)
                if mode == "unknown-source":
                    path = root / "m3/collections/src/unsealed.java"
                    path.write_bytes(b"unsealed\n")
                else:
                    path = root / "m3/collections/counts"
                    path.symlink_to(root, target_is_directory=True)
                with self.assertRaisesRegex(ValueError, "unsealed current source|symlink"):
                    precheck(root, plan)
                self.assertTrue(path.exists())

    def test_complete_sealed_sdk_and_template_inventory(self):
        pins, plan = verify_module()
        self.assertTrue(pins["files"])
        self.assertEqual(45, len(plan["products"]))
        self.assertEqual(75, sum(row["role"] == "IMMUTABLE_HISTORICAL_SCAFFOLD" for row in plan["bootstrap"]))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path)
    parser.add_argument("--run-id")
    parser.add_argument("--expected-source-sha")
    parser.add_argument("--maven-repository")
    parser.add_argument("--temp-root")
    parser.add_argument("--verify-only", action="store_true")
    parser.add_argument("--self-test", action="store_true")
    arguments = parser.parse_args()
    pins, plan = verify_module()
    if arguments.self_test:
        suite = unittest.defaultTestLoader.loadTestsFromTestCase(ControllerTest)
        result = unittest.TextTestRunner(verbosity=2).run(suite)
        require(result.wasSuccessful() and result.testsRun == 6 and not result.skipped,
                "controller self-test failed")
    if arguments.root is None:
        require(arguments.self_test, "--root required")
        return
    require(arguments.run_id and arguments.expected_source_sha, "exact run/source arguments required")
    root = arguments.root.absolute()
    require(root == root.resolve() and root.is_dir() and not root.is_symlink(), "exclusive canonical checkout required")
    source_identity(root, arguments.expected_source_sha)
    run_identity(arguments.run_id, arguments.expected_source_sha)
    precheck(root, plan)
    if arguments.verify_only:
        print("M3_COLLECTIONS_CURRENT_CONTINUITY_PRECHECK_PASS no writes; no Maven/runtime qualification")
        return
    deliver(arguments, pins, plan, root)


if __name__ == "__main__":
    main()
