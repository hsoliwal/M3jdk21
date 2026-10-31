#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Independent orchestration: existing receiver scripts remain separate unchanged programs."""
from __future__ import annotations
import argparse
import hashlib
import io
import json
import os
from pathlib import Path, PurePosixPath
import re
import shutil
import subprocess
import sys
import tempfile
import time
import unittest
import xml.etree.ElementTree as ET

TASK = Path(__file__).resolve().parent
SDK = TASK / "sdk"
RESOURCE = SDK / "src/main/resources/com/synexia/rewrite/charcopy-current-successor-20261019"
FIXTURES = SDK / "src/test/resources/com/synexia/rewrite/charcopy-current-successor-20261019"
OLD = "m3/runtime-integration/char-array-copy/recipe"
LEGACY = ["src/java.base/share/classes/java/lang/MIndexString.java",
          "m3/runtime-integration/tests/MIndexStringInvariant.java"]
OWNER = "src/java.base/share/classes/java/lang/M3String.java"
INVARIANT = "m3/runtime-integration/tests/M3StringInvariant.java"
SUITE = "com.synexia.rewrite.M3CharcopyCurrentSuccessorRecipeTest"
METHODS = {"namedCurrentEightKeepInputsReplayAsExactFixedPoint",
           "everyDriftMissingDuplicateLegacyAndMixedInputRefuses",
           "exactInverseAndFullHistoricalPreimagesRetainTheirIdentity"}
OLD_METHODS = {
    "test_current_canonical_tree_is_sealed_superseding_fixed_point",
    "test_historical_transform_atoms_remain_forward_reverse_reversible",
    "test_each_unknown_drift_is_refused_without_partial_write",
    "test_symlink_target_is_refused",
    "test_git_blob_uses_the_repository_nul_framing",
    "test_manifest_superseded_hashes_are_current_exact_sources",
    "test_missing_or_drifted_replacement_owner_refuses_without_recreating_history",
}
CAP, RESERVE, LOG_LIMIT = 512 * 1024 ** 2, 8 * 1024 ** 3, 1024 ** 2


def require(value, message):
    if not value:
        raise RuntimeError(message)


def sha(raw):
    return hashlib.sha256(raw).hexdigest()


def identity(raw):
    return {"bytes": len(raw), "sha256": sha(raw),
            "gitBlob": hashlib.sha1(b"blob " + str(len(raw)).encode("ascii") + b"\0" + raw).hexdigest()}


def regular(base, name):
    p = PurePosixPath(name)
    require(name and not p.is_absolute() and "\\" not in name and ":" not in name
            and all(part not in ("", ".", "..") for part in name.split("/")), "UNSAFE_PATH")
    path = base.joinpath(*p.parts)
    require(base.is_dir() and not base.is_symlink(), "UNSAFE_BASE")
    for count in range(1, len(p.parts)):
        parent = base.joinpath(*p.parts[:count])
        require(parent.is_dir() and not parent.is_symlink(), "UNSAFE_PARENT:" + name)
    require(path.is_file() and not path.is_symlink(), "INPUT_MISSING_OR_REDIRECTED:" + name)
    return path


def capacity(root):
    free = shutil.disk_usage(root).free
    require(free >= RESERVE, "8_GIB_RESERVE_REQUIRED")
    return free


def context(value):
    require(value and chr(36) + "{" not in value, "EXPLICIT_QUALIFICATION_ROOT_REQUIRED")
    root = Path(value)
    require(root.is_absolute() and root.is_dir() and root == root.resolve()
            and all(not p.is_symlink() for p in [root, *root.parents]), "UNSAFE_QUALIFICATION_ROOT")
    receiver_value = os.environ.get("M3_RECEIVER_ROOT", "")
    require(receiver_value, "EXPLICIT_RECEIVER_ROOT_REQUIRED")
    receiver = Path(receiver_value)
    require(receiver.is_absolute() and receiver.is_dir() and receiver == receiver.resolve()
            and all(not p.is_symlink() for p in [receiver, *receiver.parents]), "UNSAFE_RECEIVER_ROOT")
    require(root != receiver and not root.is_relative_to(receiver)
            and root != TASK and not root.is_relative_to(TASK), "OUTPUT_ROOT_INSIDE_SOURCE")
    require(os.environ.get("M3_QUALIFICATION_ROOT") == str(root), "QUALIFICATION_ROOT_ENV_MISMATCH")
    ident = os.environ.get("M3_QUALIFICATION_ID", "")
    require(re.fullmatch(r"[A-Za-z0-9_.-]{16,128}", ident), "FRESH_QUALIFICATION_ID_REQUIRED")
    for name in ("artifacts", "tmp", "home", "cache"):
        path = root / name
        require(path.is_dir() and not path.is_symlink(), "PROVISIONED_DIRECTORY_REQUIRED:" + name)
    for name in ("TMPDIR", "TEMP", "TMP"):
        require(os.environ.get(name) == str(root / "tmp"), "EXPLICIT_TEMP_REQUIRED:" + name)
    require(os.environ.get("HOME") == str(root / "home"), "EXPLICIT_HOME_REQUIRED")
    capacity(root)
    return root, receiver, ident


def current(receiver, rows):
    for name in LEGACY:
        p = receiver / name
        require(not p.exists() and not p.is_symlink(), "LEGACY_SOURCE_FORBIDDEN:" + name)
    result = {}
    require(len(rows) == 8 and len({r["path"] for r in rows}) == 8, "EIGHT_DISTINCT_KEEP_INPUTS")
    for row in rows:
        raw = regular(receiver, row["path"]).read_bytes()
        actual = identity(raw)
        require(all(actual[k] == row[k] for k in actual), "CURRENT_SOURCE_DRIFT:" + row["path"])
        result[row["path"]] = actual
    return result


def inputs(receiver):
    raw = regular(TASK, "INPUTS.json").read_bytes()
    data = json.loads(raw)
    require(data["schema"] == 1 and data["capBytes"] == CAP
            and data["reserveBytes"] == RESERVE, "INPUT_POLICY_DRIFT")
    rows = data["sourceFiles"]
    require(len({r["path"] for r in rows}) == len(rows), "DUPLICATE_SOURCE_INPUT")
    canonical = {}
    for row in rows:
        actual = identity(regular(TASK, row["path"]).read_bytes())
        require(all(actual[k] == row[k] for k in actual), "CANONICAL_SOURCE_DRIFT:" + row["path"])
        canonical[row["path"]] = actual
    return data, raw, canonical, current(receiver, data["receiverFiles"])


def write(root, name, value):
    path = root / "artifacts" / name
    raw = (json.dumps(value, indent=2, sort_keys=True) + "\n").encode("utf-8")
    require(len(raw) <= LOG_LIMIT, "RECEIPT_BUDGET")
    with path.open("xb") as out:
        out.write(raw)
    return {"path": "artifacts/" + name, "bytes": len(raw), "sha256": sha(raw)}


def sdk_receipt(root, ident):
    raw = regular(root, "artifacts/charcopy-guard-sdk.json").read_bytes()
    receipt = json.loads(raw)
    require(receipt["schema"] == 1 and receipt["state"] == "PASS_SOURCE_ONLY"
            and receipt["qualificationId"] == ident and receipt["tests"] == 3
            and receipt["successfulSdkRuns"] == 4 and receipt["refusedSdkRuns"] == 37
            and receipt["runtimeApplied"] is False and receipt["phaseAdvanced"] is False,
            "ACTUAL_FRESH_NAMED_SDK_RECEIPT_REQUIRED")
    require(receipt["manifestSha256"] == sha((RESOURCE / "manifest.tsv").read_bytes()),
            "SDK_MANIFEST_MISMATCH")
    descriptor = SDK / "src/main/resources/META-INF/rewrite/charcopy-current-successor-20261019.yml"
    require(receipt["descriptorSha256"] == sha(descriptor.read_bytes()), "SDK_DESCRIPTOR_MISMATCH")
    directory = SDK / "target/surefire-reports"
    expected = directory / ("TEST-" + SUITE + ".xml")
    require(sorted(directory.glob("TEST-*.xml")) == [expected], "EXACT_SDK_SUITE_REQUIRED")
    xml = expected.read_bytes()
    require(len(xml) <= LOG_LIMIT and not expected.is_symlink(), "SUREFIRE_BUDGET_OR_REDIRECTION")
    suite = ET.fromstring(xml)
    require(suite.tag == "testsuite" and suite.get("name") == SUITE, "WRONG_SDK_SUITE")
    counts = {k: int(suite.get(k, "-1")) for k in ("tests", "errors", "failures", "skipped")}
    require(counts == {"tests": 3, "errors": 0, "failures": 0, "skipped": 0}, "ALL_SDK_METHODS_MUST_PASS")
    cases = suite.findall("testcase")
    require(len(cases) == 3 and {r.get("name") for r in cases} == METHODS
            and all(r.get("classname") == SUITE
                    and all(c.tag in ("system-out", "system-err") for c in r) for r in cases),
            "SDK_METHOD_OR_SKIP_DRIFT")
    return {"path": "artifacts/charcopy-guard-sdk.json", "bytes": len(raw), "sha256": sha(raw),
            "reportRoot": "SDK", "report": expected.relative_to(SDK).as_posix(),
            "reportBytes": len(xml), "reportSha256": sha(xml), "counts": counts,
            "methods": sorted(METHODS), "successfulSdkRuns": 4, "refusedSdkRuns": 37}


def child_environment(root):
    env = dict(os.environ)
    env.update(HOME=str(root / "home"), TMPDIR=str(root / "tmp"), TEMP=str(root / "tmp"),
               TMP=str(root / "tmp"), XDG_CACHE_HOME=str(root / "cache"), PYTHONDONTWRITEBYTECODE="1")
    return env


def command(root, target, label, argv, success=True, timeout=60):
    capacity(root)
    path = root / "artifacts" / (label + ".log")
    started = time.time()
    with path.open("xb") as out:
        child = subprocess.Popen(argv, cwd=target, stdin=subprocess.DEVNULL, stdout=out,
                                 stderr=subprocess.STDOUT, env=child_environment(root),
                                 start_new_session=False)
        try:
            code = child.wait(timeout=timeout)
        except BaseException:
            if child.poll() is None:
                child.kill()
                child.wait(timeout=10)
            raise
    capacity(root)
    raw = path.read_bytes()
    require(len(raw) <= LOG_LIMIT and ((code == 0) if success else (code != 0)), "COMMAND_RESULT:" + label)
    return {"label": label, "argv": [str(x) for x in argv], "exitCode": code,
            "startedUnix": started, "finishedUnix": time.time(), "log": "artifacts/" + path.name,
            "bytes": len(raw), "sha256": sha(raw)}, raw


def snapshot(root):
    result = {}
    for folder, directories, files in os.walk(root, followlinks=False):
        for name in [*directories, *files]:
            path = Path(folder) / name
            if path.is_symlink():
                result[path.relative_to(root).as_posix()] = {"symlink": os.readlink(path)}
            elif path.is_file():
                result[path.relative_to(root).as_posix()] = identity(path.read_bytes())
        require(len(result) <= 128, "FIXTURE_FILE_BUDGET")
    return result


def put(root, name, raw):
    path = root / name
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("xb") as out:
        out.write(raw)


def seed(root, receiver, superseded=False):
    root.mkdir()
    for name in ("apply.py", "manifest.json", "test_recipe.py"):
        relative = OLD + "/" + name
        put(root, relative, regular(receiver, relative).read_bytes())
    put(root, "LICENSE", regular(receiver, "LICENSE").read_bytes())
    if superseded:
        put(root, OWNER, regular(FIXTURES, "M3String-predecessor.java.txt").read_bytes())
        put(root, INVARIANT, regular(receiver, INVARIANT).read_bytes())
    else:
        put(root, LEGACY[0], regular(FIXTURES, "MIndexString-before.java.txt").read_bytes())
        put(root, LEGACY[1], regular(FIXTURES, "MIndexStringInvariant-before.java.txt").read_bytes())


def guard_inverse(owner):
    anchor = b"        if (prepared.utf8Length < 0) {"
    offset = owner.find(anchor)
    require(offset >= 0, "CURRENT_GUARD_MISSING")
    guard = owner[offset:offset + 143]
    require(sha(guard) == "01ff1152e9e3100536675add7ff80f0d551bb7de1ea18999bcae803ff6da5b74"
            and owner.count(guard) == 2, "EXACT_TWO_143_BYTE_GUARDS_REQUIRED")
    old = regular(FIXTURES, "M3String-predecessor.java.txt").read_bytes()
    require(owner.replace(guard, b"") == old
            and sha(old) == "cd1192e27c981da03aeae2385d3d54ad31d912f043cf8c7dfba18c7b9106b728",
            "WHOLE_PREDECESSOR_RECOVERY_REQUIRED")
    return guard, old


def filesystem_tests(root, receiver, rows):
    fixture_root = root / "tmp"
    refused = []
    def copy_current():
        target = Path(tempfile.mkdtemp(prefix="charcopy-filesystem-", dir=fixture_root))
        for row in rows:
            put(target, row["path"], regular(receiver, row["path"]).read_bytes())
        return target
    def refusal(case, target, expected):
        before = snapshot(target)
        try:
            current(target, rows)
        except RuntimeError as failure:
            if str(failure) != expected:
                raise AssertionError("wrong filesystem refusal: " + str(failure)) from failure
            refused.append(case)
        else:
            raise AssertionError("filesystem guard admitted " + case)
        if snapshot(target) != before:
            raise AssertionError("partial mutation on " + case)
    class CurrentFilesystemTest(unittest.TestCase):
        def test_exact_current_tuple_is_unchanged_and_repeatable(self):
            target = copy_current()
            before = snapshot(target)
            self.assertEqual(current(target, rows), current(target, rows))
            self.assertEqual(before, snapshot(target))
        def test_every_owned_drift_missing_and_symlink_refuses(self):
            for row in rows:
                for kind in ("drift", "missing", "symlink"):
                    with self.subTest(path=row["path"], kind=kind):
                        target = copy_current()
                        path = target / row["path"]
                        if kind == "drift":
                            path.write_bytes(path.read_bytes() + b"\nunknown drift\n")
                        else:
                            path.unlink()
                            if kind == "symlink":
                                path.symlink_to(receiver / row["path"])
                        expected = ("CURRENT_SOURCE_DRIFT:" if kind == "drift"
                                    else "INPUT_MISSING_OR_REDIRECTED:") + row["path"]
                        refusal(kind + ":" + row["path"], target, expected)
        def test_broken_owned_links_and_all_legacy_occupants_refuse(self):
            for row in rows:
                target = copy_current()
                path = target / row["path"]
                path.unlink()
                path.symlink_to(target / "absent-link-target")
                refusal("broken-owned:" + row["path"], target,
                        "INPUT_MISSING_OR_REDIRECTED:" + row["path"])
            for name in LEGACY:
                for kind in ("file", "symlink", "broken"):
                    target = copy_current()
                    path = target / name
                    path.parent.mkdir(parents=True, exist_ok=True)
                    if kind == "file":
                        path.write_bytes(b"unexpected legacy occupant\n")
                    else:
                        path.symlink_to(receiver / OWNER if kind == "symlink" else target / "absent-link-target")
                    refusal("legacy-" + kind + ":" + name, target, "LEGACY_SOURCE_FORBIDDEN:" + name)
    output = io.StringIO()
    result = unittest.TextTestRunner(stream=output, verbosity=2).run(
        unittest.defaultTestLoader.loadTestsFromTestCase(CurrentFilesystemTest))
    require(result.testsRun == 3 and result.wasSuccessful() and not result.skipped
            and len(refused) == 38, "ACTUAL_FILESYSTEM_DISCRIMINATORS_REQUIRED")
    receipt = {"tests": result.testsRun, "failures": len(result.failures),
               "errors": len(result.errors), "skipped": len(result.skipped),
               "refusalCases": refused, "retainedFixtureDirectories": True,
               "scope": "REAL_FILESYSTEM_COPIES_NOT_RUNTIME_OR_RECIPE_OUTPUT"}
    return receipt, output.getvalue()


def historical(root, receiver):
    work = root / "charcopy-replay"
    require(not work.exists() and not work.is_symlink(), "FRESH_HISTORICAL_OUTPUT_REQUIRED")
    work.mkdir()
    full, superseded = work / "full", work / "superseded"
    seed(full, receiver)
    seed(superseded, receiver, superseded=True)
    metadata = json.loads(regular(receiver, OLD + "/manifest.json").read_bytes())
    require(set(metadata["files"]) == set(LEGACY), "UNCHANGED_TWO_TARGET_HISTORICAL_MANIFEST_REQUIRED")
    commands = []
    python = [sys.executable, "-I", "-S", "-B"]
    def run(target, label, switches=(), success=True):
        row, raw = command(root, target, label,
                           [*python, str(target / OLD / "apply.py"), "--target", str(target), *switches],
                           success=success)
        commands.append(row)
        return raw
    def state(target, label, expected, switches=()):
        raw = run(target, label, switches)
        require(raw.decode("utf-8").splitlines().count("M3_CHAR_ARRAY_COPY_RECIPE state=" + expected) == 1,
                "EXACT_HISTORICAL_STATE_MARKER:" + label)
    before = snapshot(full)
    for name in LEGACY:
        require(sha(regular(full, name).read_bytes()) == metadata["files"][name]["before"],
                "FULL_HISTORICAL_BEFORE_MISMATCH")
    state(full, "legacy-before-check", "before", ("--check",))
    require(snapshot(full) == before, "HISTORICAL_CHECK_MUTATED_SOURCE")
    state(full, "legacy-forward", "after")
    after = snapshot(full)
    after_sources = {name: regular(full, name).read_bytes() for name in LEGACY}
    for name in LEGACY:
        require(sha(after_sources[name]) == metadata["files"][name]["after"], "FULL_AFTER_HASH_MISMATCH")
    state(full, "legacy-after-check", "after", ("--check",))
    state(full, "legacy-forward-fixed-point", "after")
    require(snapshot(full) == after, "FORWARD_FIXED_POINT_MUTATED")
    state(full, "legacy-inverse", "before", ("--reverse",))
    require(snapshot(full) == before, "EXACT_FULL_INVERSE_MISMATCH")
    state(full, "legacy-inverse-fixed-point", "before", ("--reverse",))
    require(snapshot(full) == before, "INVERSE_FIXED_POINT_MUTATED")
    sealed = snapshot(superseded)
    for label, switches in (("superseded-check", ("--check",)),
                            ("superseded-forward", ()), ("superseded-reverse", ("--reverse",))):
        state(superseded, label, "superseded", switches)
        require(snapshot(superseded) == sealed, "SUPERSEDED_FIXTURE_MUTATED")
    old_test = superseded / OLD / "test_recipe.py"
    row, raw = command(root, superseded, "legacy-original-seven-tests", [*python, str(old_test), "-v"])
    commands.append(row)
    text = raw.decode("utf-8")
    require(re.search(r"\bRan 7 tests in ", text) and text.rstrip().endswith("OK"),
            "ACTUAL_ORIGINAL_SEVEN_TESTS_REQUIRED")
    for name in OLD_METHODS:
        require(sum(line.startswith(name + " ") for line in text.splitlines()) == 1,
                "ORIGINAL_TEST_METHOD_MISSING:" + name)
    require(snapshot(superseded) == sealed, "ORIGINAL_TEST_SUITE_MUTATED_FIXTURE")
    refused = []
    def reject(target, name, expected):
        prior = snapshot(target)
        raw = run(target, "legacy-refusal-" + str(len(refused) + 1), success=False)
        require(("ValueError: " + expected).encode("utf-8") in raw,
                "SPECIFIC_HISTORICAL_REFUSAL_REQUIRED:" + name)
        require(snapshot(target) == prior, "PARTIAL_MUTATION_ON_HISTORICAL_REFUSAL:" + name)
        refused.append(name)
    for name in LEGACY:
        for kind in ("drift", "missing", "symlink"):
            target = work / ("before-refusal-" + str(len(refused)))
            seed(target, receiver)
            path = target / name
            if kind == "drift":
                path.write_bytes(path.read_bytes() + b"\nunknown drift\n")
            else:
                path.unlink()
                if kind == "symlink":
                    path.symlink_to(full / name)
            expected = ("source drift: " + name if kind == "drift" else
                        "symlink path: " + name if kind == "symlink" else
                        "superseded owner missing: " + metadata["files"][name]["superseded_by"]["path"])
            reject(target, "before-" + kind + ":" + name, expected)
    for index, name in enumerate(LEGACY):
        target = work / ("mixed-" + str(index))
        seed(target, receiver)
        (target / name).write_bytes(after_sources[name])
        reject(target, "mixed-before-after:" + name, "mixed source state")
    for name in (OWNER, INVARIANT):
        for kind in ("drift", "missing", "broken"):
            target = work / ("superseded-refusal-" + str(len(refused)))
            seed(target, receiver, superseded=True)
            path = target / name
            if kind == "drift":
                path.write_bytes(path.read_bytes() + b"\nunknown drift\n")
            else:
                path.unlink()
                if kind == "broken":
                    path.symlink_to(target / "absent-link-target")
            expected = ("superseded owner drift: " if kind == "drift" else
                        "superseded owner missing: " if kind == "missing" else "symlink path: ") + name
            reject(target, "superseded-" + kind + ":" + name, expected)
    current_owner = regular(receiver, OWNER).read_bytes()
    guard, recovered = guard_inverse(current_owner)
    for kind in ("current", "first-only", "second-only"):
        target = work / ("successor-refusal-" + kind)
        seed(target, receiver, superseded=True)
        candidate = current_owner
        if kind != "current":
            offset = candidate.find(guard) if kind == "first-only" else candidate.rfind(guard)
            candidate = candidate[:offset] + candidate[offset + len(guard):]
        (target / OWNER).write_bytes(candidate)
        reject(target, "unadmitted-successor:" + kind, "superseded owner drift: " + OWNER)
    require(len(refused) == 17, "HISTORICAL_REFUSAL_INVENTORY_DRIFT")
    return {"commands": commands, "fullBefore": before, "fullAfter": after,
            "supersededBeforeAndAfter": sealed, "fullInverseByteExact": True,
            "originalTestMethods": sorted(OLD_METHODS), "originalTests": 7,
            "refusalCases": refused, "refusals": 17,
            "currentDelta": {"blocks": 2, "blockBytes": 143, "totalBytes": 286,
                             "guardSha256": sha(guard), "recovered": identity(recovered)}}


def execute(root, receiver, ident):
    data, raw_inputs, canonical_before, receiver_before = inputs(receiver)
    sdk = sdk_receipt(root, ident)
    fs, fs_log = filesystem_tests(root, receiver, data["receiverFiles"])
    path = root / "artifacts/charcopy-filesystem-tests.log"
    with path.open("x", encoding="utf-8") as out:
        out.write(fs_log)
    fs_ref = write(root, "charcopy-filesystem-tests.json", fs)
    history = historical(root, receiver)
    _, after_inputs, canonical_after, receiver_after = inputs(receiver)
    require(raw_inputs == after_inputs and canonical_before == canonical_after
            and receiver_before == receiver_after, "REAL_SOURCE_CUSTODY_CHANGED")
    custody = write(root, "charcopy-source-custody.json",
                    {"schema": 1, "qualificationId": ident, "inputManifestSha256": sha(raw_inputs),
                     "canonicalBefore": canonical_before, "canonicalAfter": canonical_after,
                     "receiverBefore": receiver_before, "receiverAfter": receiver_after,
                     "unchanged": True, "runtimeWrites": 0})
    result = {"schema": 1, "qualificationId": ident, "state": "PASS_SOURCE_SUCCESSOR_CUSTODY",
              "scope": "NAMED_SDK_CURRENT_ZERO_WRITE_AND_ISOLATED_HISTORICAL_PROGRAM_REPLAY",
              "sdk": sdk, "filesystem": fs_ref, "historical": history, "sourceCustody": custody,
              "sourceFoundation": data["foundation"], "receiverCommit": data["receiverCommit"],
              "currentRuntimeWrites": 0, "legacyScriptsUnchanged": True,
              "legacyProgramsAreSeparateExecutables": True,
              "historicalLicensesRelabeled": False,
              "storage": {"capBytes": CAP, "reserveBytes": RESERVE,
                          "externalAggregateAndProcessGroupSupervisorRequired": True,
                          "selfEnforcedAggregateClaim": False, "finalFreeBytes": capacity(root)},
              "originalPrivateTyped3bdeeQualified": False, "targetJavaCompiled": False,
              "matchedJdkQualified": False, "jniQualified": False,
              "runtimeBehaviorQualified": False, "performanceRetentionQualified": False,
              "runtimeApplied": False, "phaseAdvanced": False, "allCiGreen": False}
    ref = write(root, "charcopy-execution.json", result)
    print(json.dumps({"state": result["state"], "receipt": ref, "runtimeApplied": False}, sort_keys=True))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--output-root", required=True)
    parser.add_argument("--inputs-only", action="store_true")
    args = parser.parse_args()
    root, receiver, ident = context(args.output_root)
    if args.inputs_only:
        _, _, source, owned = inputs(receiver)
        require(not (root / "artifacts/charcopy-guard-sdk.json").exists(),
                "FRESH_SDK_RECEIPT_REQUIRED")
        print(json.dumps({"state": "INPUTS_ONLY", "sourceFiles": len(source),
                          "receiverFiles": len(owned), "sdkExecuted": False, "runtimeApplied": False}))
    else:
        execute(root, receiver, ident)


if __name__ == "__main__":
    main()
