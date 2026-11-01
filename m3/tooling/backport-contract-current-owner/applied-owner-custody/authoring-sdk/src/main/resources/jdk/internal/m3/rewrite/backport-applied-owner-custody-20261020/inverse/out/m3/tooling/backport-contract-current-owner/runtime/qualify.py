#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Exercise exact backport owning tests using authentic receiver and Maven postimages."""
from __future__ import annotations
import argparse
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import re
import shutil
import subprocess
import sys
import time

TASK = Path(__file__).resolve().parent
SDK = TASK.parent / "sdk"
RESERVE, CAP, LOG_LIMIT = 8 * 1024 ** 3, 512 * 1024 ** 2, 1024 ** 2
SCHEMA = "M3_BACKPORT_CONTRACT_CURRENT_OWNER_EXECUTION_V1"
SCOPE = "EXACT_EIGHT_OWNER_BACKPORT_CONTRACT_DIFFERENTIAL"
MARKER = "M3_BACKPORT_UNITTEST_RESULT "
POLICY = "m3/backports/test_compatibility_policy.py"
WAVES = "m3/backports/test_backport_execution_waves.py"
CHILD_APPROVAL = '{"a.compile": "review/child-only"}'
PINNED = {
    "m3/backports/test_compatibility_policy.py": "14f15fff4c3cfcf940c0de01eb50da27792c9fc9",
    "m3/backports/compatibility_policy.py": "89673d65b2ad42fc694d809d5c6f4d0eab7adbd8",
    "m3/backports/POST21_PRIORITY_COMPATIBILITY.tsv": "db7579c96d7dfc9c0c70fb4b28773cf89bad76ca",
    "m3/backports/JEP_CATALOGUE.tsv": "a0ef9aa95da17a19c3df8ab8aded355fad4f4598",
    "m3/backports/RELEASE_JEP_AUTHORITY.tsv": "745a26772b8050c28dd93d05c724bfa9c2ad5e18",
    "m3/backports/test_backport_execution_waves.py": "e10ae062eb1b86a6335e7d64fd520d2206a4b4c4",
    "m3/backports/backport_execution_waves.py": "cc2a6ceb90b56d3b05d1307e705586c8d7cf1b40",
    "m3/backports/BACKPORT_EXECUTION_WAVES.md": "a5421ae43c55cbcce025f95cf49838a112a507bc"
}
AFTER = {
    "m3/backports/test_compatibility_policy.py": {
        "path": "m3/backports/test_compatibility_policy.py",
        "gitBlob": "cd7b5a28ce3ec1f5a4475a145b40a3074b1a5c21",
        "bytes": 7101,
        "sha256": "69694e6e3725aeee8687a8c58bce477c7256f626d8425c04a9ce9772250a2bb6"
    },
    "m3/backports/test_backport_execution_waves.py": {
        "path": "m3/backports/test_backport_execution_waves.py",
        "gitBlob": "7a3b961caa4302a2ebd1675360e29e6b1df9697b",
        "bytes": 12454,
        "sha256": "5a59d038e108010c484f66f545ab5bc745a791182ed4794556b414ffde315dd0"
    },
    "m3/backports/BACKPORT_EXECUTION_WAVES.md": {
        "path": "m3/backports/BACKPORT_EXECUTION_WAVES.md",
        "gitBlob": "428d775ca476b187dceb26a012401b11b975bbbb",
        "bytes": 6247,
        "sha256": "df2aca41ab120bdbc8c165d4abe3552c54a91c591aab116b718b94283ae23f28"
    }
}
EXPECTED_IDS = [
    "test_backport_execution_waves.BackportExecutionWavesTest.test_approval_ledger_fails_closed",
    "test_backport_execution_waves.BackportExecutionWavesTest.test_broader_scope_is_held_until_exact_atom_approval",
    "test_backport_execution_waves.BackportExecutionWavesTest.test_collision_after_scheduler_id_normalization_fails_closed",
    "test_backport_execution_waves.BackportExecutionWavesTest.test_direct_planner_rejects_order_gaps_and_cross_work_dependencies",
    "test_backport_execution_waves.BackportExecutionWavesTest.test_direct_planner_rejects_unknown_approvals_and_invalid_scope",
    "test_backport_execution_waves.BackportExecutionWavesTest.test_duplicate_approval_ids_are_rejected_even_when_earlier_row_is_false",
    "test_backport_execution_waves.BackportExecutionWavesTest.test_emitted_waves_are_real_tsv_rows_and_single_terminated_json",
    "test_backport_execution_waves.BackportExecutionWavesTest.test_header_only_approval_ledger_is_valid_but_invalid_headers_fail",
    "test_backport_execution_waves.BackportExecutionWavesTest.test_independent_file_atoms_fan_out_and_dependencies_advance_waves",
    "test_backport_execution_waves.BackportExecutionWavesTest.test_json_output_binds_items_and_root",
    "test_backport_execution_waves.BackportExecutionWavesTest.test_malformed_approval_record_is_not_accepted_as_evidence",
    "test_backport_execution_waves.BackportExecutionWavesTest.test_non_executable_evidence_never_enters_a_wave",
    "test_backport_execution_waves.BackportExecutionWavesTest.test_nonexecutable_file_cannot_inherit_scope_approval",
    "test_backport_execution_waves.BackportExecutionWavesTest.test_scheduler_projections_are_deterministic_and_authority_free",
    "test_backport_execution_waves.BackportExecutionWavesTest.test_unapproved_g1_multimodule_backport_is_held_with_dependents",
    "test_compatibility_policy.CompatibilityPolicyTest.test_checked_in_ledger_is_valid",
    "test_compatibility_policy.CompatibilityPolicyTest.test_dependencies_must_be_present_and_not_from_later_release",
    "test_compatibility_policy.CompatibilityPolicyTest.test_duplicate_bad_url_and_pre22_rows_fail_closed",
    "test_compatibility_policy.CompatibilityPolicyTest.test_empty_and_invalid_fields_fail_closed",
    "test_compatibility_policy.CompatibilityPolicyTest.test_language_or_api_extension_cannot_be_marked_candidate_default",
    "test_compatibility_policy.CompatibilityPolicyTest.test_rejected_feature_cannot_be_opt_in_default_marker"
]
SYNTHETIC_IDS = [
    "__main__.SyntheticClassifierControls.test_candidate_is_refused",
    "__main__.SyntheticClassifierControls.test_no_is_accepted",
    "__main__.SyntheticClassifierControls.test_opt_in_is_refused",
]
COMMANDS = [
    ("backport-baseline", "baseline", 1),
    ("backport-candidate", "candidate", 0),
    ("backport-child-approval-refusal", "child-reversion", 1),
    ("backport-synthetic-validator", "synthetic", 0),
]
FALSE_FLAGS = [
    "originalPrivateQualified", "privateSourceForkReactorQualified",
    "fullControlReactorQualified", "matchedJdkQualified", "fullJdkQualified",
    "javaQualified", "nativeExecuted", "jniQualified", "performanceQualified",
    "runtimeApplied", "phaseAdvanced", "authorityDataChanged",
    "productionValidatorsChanged", "recipeSdkQualified",
    "externalToolchainBytesSealed", "sourceBindingQualifiedByDriver",
]
TRUE_FLAGS = [
    "baselineFailuresReproduced", "candidateAllOwningTestsExecuted",
    "childApprovalReversionRefused", "syntheticClassifierControlsPassed",
    "sourceOwnersUnchanged", "realTemporaryFileIoExecuted",
]

def require(condition, message):
    if not condition:
        raise RuntimeError(message)


def sha(data):
    return hashlib.sha256(data).hexdigest()


def git_blob(data):
    return hashlib.sha1(b"blob " + str(len(data)).encode("ascii") + b"\0" + data).hexdigest()


def real_directory(path, label):
    require(path.is_dir() and path == path.resolve()
            and all(not part.is_symlink() for part in [path, *path.parents]),
            "REAL_CANONICAL_DIRECTORY_REQUIRED:" + label)


def safe_file(base, relative):
    value = PurePosixPath(relative)
    require(not value.is_absolute() and value.parts and "\\" not in relative
            and all(part not in ("", ".", "..") for part in value.parts),
            "UNSAFE_INPUT_PATH")
    path = base.joinpath(*value.parts)
    for size in range(len(value.parts)):
        parent = base.joinpath(*value.parts[:size])
        require(parent.is_dir() and not parent.is_symlink(), "INPUT_PARENT_UNSAFE:" + relative)
    require(path.is_file() and not path.is_symlink(), "INPUT_MISSING_OR_UNSAFE:" + relative)
    return path


def capacity(root):
    free = shutil.disk_usage(root).free
    require(free >= RESERVE, "STORAGE_RESERVE_BELOW_8_GIB")
    return free


def context(value):
    require(value and chr(36) + "{" not in value, "EXPLICIT_QUALIFICATION_ROOT_REQUIRED")
    root = Path(value)
    require(root.is_absolute(), "ABSOLUTE_OUTPUT_ROOT_REQUIRED")
    real_directory(root, "OUTPUT")
    source_value = os.environ.get("M3_SOURCE_ROOT", "")
    require(source_value and chr(36) + "{" not in source_value, "EXPLICIT_SOURCE_ROOT_REQUIRED")
    source = Path(source_value)
    require(source.is_absolute(), "ABSOLUTE_SOURCE_ROOT_REQUIRED")
    real_directory(source, "SOURCE")
    require(root != source and not root.is_relative_to(source)
            and not source.is_relative_to(root), "SOURCE_OUTPUT_ROOTS_MUST_BE_SEPARATE")
    for name in ("artifacts", "tmp", "home", "cache"):
        real_directory(root / name, name)
    require(os.environ.get("M3_QUALIFICATION_ROOT") == str(root), "ROOT_ENVIRONMENT_MISMATCH")
    ident = os.environ.get("M3_QUALIFICATION_ID", "")
    require(re.fullmatch(r"[A-Za-z0-9_.-]{16,128}", ident), "FRESH_QUALIFICATION_ID_REQUIRED")
    for name in ("TMPDIR", "TEMP", "TMP"):
        require(os.environ.get(name) == str(root / "tmp"), "EXPLICIT_TEMP_ROUTING_REQUIRED:" + name)
    require(os.environ.get("HOME") == str(root / "home"), "EXPLICIT_HOME_ROUTING_REQUIRED")
    require(os.environ.get("XDG_CACHE_HOME") == str(root / "cache"), "EXPLICIT_CACHE_ROUTING_REQUIRED")
    capacity(root)
    return root, source, ident


def identity(raw):
    return {"bytes": len(raw), "sha256": sha(raw), "gitBlob": git_blob(raw)}


def inputs(receiver):
    real_directory(receiver, "RECEIVER")
    real_directory(TASK, "TASK")
    raw = safe_file(TASK, "INPUTS.json").read_bytes()
    require(len(raw) < 32768, "INPUT_MANIFEST_TOO_LARGE")
    metadata = json.loads(raw)
    require(metadata["schema"] == 1
            and metadata["capability"] == "backport-contract-current-owner-20261019"
            and metadata["preCodeAdmission"] == "ab8633cc645487bc69bcd6919f328a0e1b5e40a9"
            and metadata["receiverCommit"] == "9a164c31b38c7013c179a3ff38b0bb60c199c7fd"
            and metadata["capBytes"] == CAP and metadata["reserveBytes"] == RESERVE,
            "INPUT_POLICY_DRIFT")
    rows = metadata["files"]
    require(len(rows) == 8 and {row["path"] for row in rows} == set(PINNED),
            "EXACT_EIGHT_INPUTS_REQUIRED")
    require({row["path"]: row for row in metadata["replacements"]} == AFTER
            and len(metadata["replacements"]) == 3,
            "EXACT_THREE_POSTIMAGE_PINS_REQUIRED")
    require(metadata["expectedTestIds"] == EXPECTED_IDS
            and metadata["syntheticTestIds"] == SYNTHETIC_IDS,
            "FROZEN_OWNING_TEST_INVENTORY_DRIFT")
    actual = {}
    for row in rows:
        path = row["path"]
        require(row["gitBlob"] == PINNED[path] and row["bytes"] < 32768
                and row["role"] == ("REPLACE" if path in AFTER else "KEEP"),
                "CANONICAL_OWNER_IDENTITY_DRIFT:" + path)
        found = identity(safe_file(receiver, path).read_bytes())
        require(all(row[key] == found[key] for key in found), "SOURCE_PIN_DRIFT:" + path)
        actual[path] = found
    require(sum(item["bytes"] for item in actual.values()) == 59781,
            "EIGHT_OWNER_BYTES_DRIFT")
    return metadata, actual, sha(raw)


def environment(root):
    child = dict(os.environ)
    child.update(HOME=str(root / "home"), TMPDIR=str(root / "tmp"),
                 TEMP=str(root / "tmp"), TMP=str(root / "tmp"),
                 XDG_CACHE_HOME=str(root / "cache"), PYTHONDONTWRITEBYTECODE="1")
    return child


def receiver_head(receiver, env):
    expected = env.get("EXPECTED_SOURCE_SHA", "")
    require(re.fullmatch(r"[a-f0-9]{40}", expected), "EXACT_EXPECTED_SOURCE_SHA_REQUIRED")
    done = subprocess.run(["git", "rev-parse", "HEAD"], cwd=receiver,
        stdin=subprocess.DEVNULL, capture_output=True, timeout=15,
        env=env, check=False, start_new_session=False)
    require(done.returncode == 0 and not done.stderr and len(done.stdout) < 128,
            "ACTUAL_RECEIVER_HEAD_REQUIRED")
    head = done.stdout.decode("ascii").strip()
    require(head == expected, "ACTUAL_CHECKOUT_HEAD_MISMATCH")
    return head


def write(root, name, data):
    require(re.fullmatch(r"[a-z0-9-]+\.json", name), "FIXED_JSON_BASENAME_REQUIRED")
    real_directory(root / "artifacts", "ARTIFACTS")
    path = root / "artifacts" / name
    require(not path.exists() and not path.is_symlink(), "FRESH_ARTIFACT_REQUIRED:" + name)
    raw = (json.dumps(data, indent=2, sort_keys=True) + "\n").encode("utf-8")
    require(len(raw) <= LOG_LIMIT, "RECEIPT_TOO_LARGE")
    with path.open("xb") as stream:
        stream.write(raw)
    return {"path": "artifacts/" + name, "bytes": len(raw), "sha256": sha(raw)}


def command(root, source, label, argv, env, expected=0, timeout=120):
    capacity(root)
    real_directory(root / "artifacts", "ARTIFACTS")
    require(re.fullmatch(r"[a-z0-9-]+", label), "FIXED_LOG_BASENAME_REQUIRED")
    path = root / "artifacts" / (label + ".log")
    require(not path.exists() and not path.is_symlink(), "FRESH_COMMAND_LOG_REQUIRED:" + label)
    started = time.time()
    with path.open("xb") as log:
        process = subprocess.Popen(argv, cwd=source, stdin=subprocess.DEVNULL,
            stdout=log, stderr=subprocess.STDOUT, env=env, start_new_session=False)
        try:
            code = process.wait(timeout=timeout)
        except BaseException:
            if process.poll() is None:
                process.kill()
                process.wait(timeout=10)
            raise
    capacity(root)
    require(path.stat().st_size <= LOG_LIMIT, "COMMAND_LOG_EXCEEDS_1_MIB:" + label)
    raw = path.read_bytes()
    require(code == expected, "COMMAND_EXIT:" + label + ":" + str(code))
    return {"label": label, "argv": [str(item) for item in argv], "exitCode": code,
            "startedUnix": started, "finishedUnix": time.time(),
            "log": str(path.relative_to(root)), "bytes": len(raw), "sha256": sha(raw)}, raw


CHILD = "import importlib\nimport json\nimport os\nfrom pathlib import Path\nimport sys\nimport tempfile\nimport unittest\n\nreceiver = Path(sys.argv[1])\ntemp_root = Path(sys.argv[2])\nmode = sys.argv[3]\nassert receiver.is_absolute() and receiver == receiver.resolve() and receiver.is_dir()\nassert temp_root.is_absolute() and temp_root == temp_root.resolve() and temp_root.is_dir()\nassert all(os.environ.get(key) == str(temp_root) for key in (\"TMPDIR\", \"TEMP\", \"TMP\"))\nassert sys.flags.isolated == 1 and sys.flags.no_site == 1 and sys.flags.dont_write_bytecode == 1\ntempfile.tempdir = str(temp_root)\nassert tempfile.gettempdir() == str(temp_root)\nsys.path.insert(0, str(receiver / \"m3/backports\"))\npolicy_tests = importlib.import_module(\"test_compatibility_policy\")\nexpected_modules = {\n    \"test_compatibility_policy\": receiver / \"m3/backports/test_compatibility_policy.py\",\n    \"compatibility_policy\": receiver / \"m3/backports/compatibility_policy.py\",\n}\nif mode != \"synthetic\":\n    wave_tests = importlib.import_module(\"test_backport_execution_waves\")\n    expected_modules[\"test_backport_execution_waves\"] = (\n        receiver / \"m3/backports/test_backport_execution_waves.py\")\n\nclass SyntheticClassifierControls(unittest.TestCase):\n    def row(self, marker):\n        value = policy_tests._row(jep=\"999\", release=\"22\",\n            classification=\"REJECT_JVMS_LANGUAGE_SPEC\", default_java21=marker)\n        value[\"title\"] = \"Synthetic classification-only runtime control\"\n        return value\n\n    def test_no_is_accepted(self):\n        actual = policy_tests._load_rows([self.row(\"NO\")])\n        self.assertEqual(1, len(actual))\n        self.assertEqual(999, actual[0].jep)\n        self.assertEqual(\"REJECT_JVMS_LANGUAGE_SPEC\", actual[0].classification)\n        self.assertEqual(\"NO\", actual[0].default_java21)\n\n    def test_opt_in_is_refused(self):\n        with self.assertRaisesRegex(ValueError, \"cannot be default Java21\"):\n            policy_tests._load_rows([self.row(\"OPT_IN\")])\n\n    def test_candidate_is_refused(self):\n        with self.assertRaisesRegex(ValueError, \"cannot be default Java21\"):\n            policy_tests._load_rows([self.row(\"CANDIDATE\")])\n\nclass RecordingResult(unittest.TextTestResult):\n    def __init__(self, *args, **kwargs):\n        super().__init__(*args, **kwargs)\n        self.executed = []\n        self.problems = []\n        self.subtest_problems = []\n\n    def startTest(self, test):\n        self.executed.append(test.id())\n        super().startTest(test)\n\n    def addError(self, test, err):\n        self.problems.append({\"id\": test.id(), \"kind\": \"error\",\n            \"exception\": err[0].__name__, \"message\": str(err[1]),\n            \"traceback\": self._exc_info_to_string(err, test)})\n        super().addError(test, err)\n\n    def addFailure(self, test, err):\n        self.problems.append({\"id\": test.id(), \"kind\": \"failure\",\n            \"exception\": err[0].__name__, \"message\": str(err[1]),\n            \"traceback\": self._exc_info_to_string(err, test)})\n        super().addFailure(test, err)\n\n    def addSubTest(self, test, subtest, err):\n        if err is not None:\n            self.subtest_problems.append({\"id\": test.id(), \"subtest\": str(subtest)})\n        super().addSubTest(test, subtest, err)\n\ndef flatten(suite):\n    result = []\n    for item in suite:\n        if isinstance(item, unittest.TestSuite):\n            result.extend(flatten(item))\n        else:\n            result.append(item.id())\n    return result\n\nloader = unittest.TestLoader()\nif mode == \"synthetic\":\n    suite = loader.loadTestsFromTestCase(SyntheticClassifierControls)\nelse:\n    suite = unittest.TestSuite([\n        loader.loadTestsFromModule(policy_tests),\n        loader.loadTestsFromModule(wave_tests),\n    ])\nloaded = flatten(suite)\nassert len(loaded) == len(set(loaded))\nresult = unittest.TextTestRunner(verbosity=2, resultclass=RecordingResult).run(suite)\nif mode != \"synthetic\":\n    expected_modules[\"m3_backport_execution_waves\"] = receiver / \"m3/backports/backport_execution_waves.py\"\nmodules = {}\nfor name, expected in expected_modules.items():\n    actual = Path(sys.modules[name].__file__).resolve()\n    assert actual == expected\n    modules[name] = str(actual)\ndocument = {\n    \"schema\": \"M3_BACKPORT_ACTUAL_UNITTEST_RESULT_V1\",\n    \"mode\": mode, \"testsRun\": result.testsRun, \"loadedIds\": sorted(loaded),\n    \"executedIds\": sorted(result.executed),\n    \"failures\": len(result.failures), \"errors\": len(result.errors),\n    \"skips\": len(result.skipped), \"expectedFailures\": len(result.expectedFailures),\n    \"unexpectedSuccesses\": len(result.unexpectedSuccesses),\n    \"subtestProblems\": result.subtest_problems, \"problems\": result.problems,\n    \"successful\": result.wasSuccessful(), \"realFileIo\": True,\n    \"tempRoot\": str(temp_root), \"receiver\": str(receiver), \"modules\": modules,\n    \"pythonIsolation\": {\"isolated\": sys.flags.isolated, \"noSite\": sys.flags.no_site,\n        \"dontWriteBytecode\": sys.flags.dont_write_bytecode},\n}\nprint(\"M3_BACKPORT_UNITTEST_RESULT \" + json.dumps(document, sort_keys=True), flush=True)\nraise SystemExit(0 if result.wasSuccessful() else 1)\n"

def expected_failure_ids():
    return (
        "test_compatibility_policy.CompatibilityPolicyTest.test_checked_in_ledger_is_valid",
        "test_backport_execution_waves.BackportExecutionWavesTest."
        "test_broader_scope_is_held_until_exact_atom_approval",
    )


def parse_result(raw, mode, fixture, root):
    lines = raw.decode("utf-8", errors="strict").splitlines()
    matches = [line[len(MARKER):] for line in lines if line.startswith(MARKER)]
    require(len(matches) == 1, "ONE_ACTUAL_UNITTEST_RESULT_REQUIRED:" + mode)
    result = json.loads(matches[0])
    expected = SYNTHETIC_IDS if mode == "synthetic" else EXPECTED_IDS
    require(result["schema"] == "M3_BACKPORT_ACTUAL_UNITTEST_RESULT_V1"
            and result["mode"] == mode and result["loadedIds"] == expected
            and result["executedIds"] == expected and result["testsRun"] == len(expected),
            "COMPLETE_ACTUAL_TEST_INVENTORY_REQUIRED:" + mode)
    require(all(result[key] == 0 for key in (
        "skips", "expectedFailures", "unexpectedSuccesses"))
        and result["subtestProblems"] == [], "NO_SKIPS_OR_HIDDEN_FAILURES_ALLOWED:" + mode)
    require(result["receiver"] == str(fixture)
            and result["tempRoot"] == str(root / "tmp")
            and result["realFileIo"] is True
            and result["pythonIsolation"] == {
                "isolated": 1, "noSite": 1, "dontWriteBytecode": 1},
            "REAL_ISOLATED_FILESYSTEM_TESTS_REQUIRED:" + mode)
    modules = {
        "test_compatibility_policy": str(fixture / POLICY),
        "compatibility_policy": str(fixture / "m3/backports/compatibility_policy.py"),
    }
    if mode != "synthetic":
        modules.update({
            "test_backport_execution_waves": str(fixture / WAVES),
            "m3_backport_execution_waves": str(fixture / "m3/backports/backport_execution_waves.py"),
        })
    require(result["modules"] == modules, "EXACT_AUTHENTIC_MODULE_PATHS_REQUIRED:" + mode)
    problems = result["problems"]
    require(len(problems) == result["failures"] + result["errors"],
            "PROBLEM_LIST_MUST_ACCOUNT_FOR_EVERY_FAILURE:" + mode)
    policy_id, wave_id = expected_failure_ids()
    if mode == "baseline":
        require((result["errors"], result["failures"], result["successful"]) == (1, 1, False)
                and {item["id"] for item in problems} == {policy_id, wave_id},
                "EXACT_TWO_KNOWN_BASELINE_FAILURES_REQUIRED")
        policy = next(item for item in problems if item["id"] == policy_id)
        require(policy["kind"] == "error" and policy["exception"] == "KeyError"
                and policy["message"] == "401" and "KeyError: 401" in policy["traceback"],
                "EXACT_ABSENT_401_KEYERROR_REQUIRED")
    elif mode == "child-reversion":
        require((result["errors"], result["failures"], result["successful"]) == (0, 1, False)
                and [item["id"] for item in problems] == [wave_id],
                "REMOVED_CHILD_APPROVAL_MUST_FAIL_ONLY_OWNING_ASSERTION")
    else:
        require(result["errors"] == result["failures"] == 0 and not problems
                and result["successful"] is True, "ALL_CANDIDATE_TESTS_MUST_PASS:" + mode)
    if mode in ("baseline", "child-reversion"):
        wave = next(item for item in problems if item["id"] == wave_id)
        require(wave["kind"] == "failure" and wave["exception"] == "AssertionError"
                and "DEPENDENCY_HOLD" in wave["message"]
                and "SCOPE_APPROVAL_HOLD" in wave["message"],
                "EXACT_SCOPE_PRECEDENCE_FAILURE_REQUIRED")
    return result


def snapshot(directory):
    real_directory(directory, "EXACT_RECEIVER_FIXTURE")
    found = {}
    for path in directory.rglob("*"):
        require(not path.is_symlink(), "FIXTURE_SYMLINK_FORBIDDEN")
        if path.is_file():
            relative = path.relative_to(directory).as_posix()
            require(relative in PINNED, "UNEXPECTED_FIXTURE_FILE:" + relative)
            found[relative] = identity(path.read_bytes())
        else:
            require(path.is_dir(), "UNEXPECTED_FIXTURE_NODE")
    require(set(found) == set(PINNED), "EXACT_EIGHT_FIXTURE_FILES_REQUIRED")
    return found


def emitted():
    real_directory(SDK, "SDK")
    directory = SDK / "target/emitted"
    real_directory(directory, "SDK_EMITTED")
    found = {}
    paths = {path.relative_to(directory).as_posix() for path in directory.rglob("*")
             if path.is_file() or path.is_symlink()}
    require(paths == set(AFTER), "EXACT_THREE_ACTUAL_MAVEN_OUTPUTS_REQUIRED")
    for path, expected in AFTER.items():
        actual = identity(safe_file(directory, path).read_bytes())
        require(all(actual[key] == expected[key] for key in actual),
                "ACTUAL_MAVEN_POSTIMAGE_DRIFT:" + path)
        found[path] = actual
    return directory, found


def expected_fixture_maps(before, after, reverted_wave):
    candidate = {**before, **after}
    return {
        "baseline": before,
        "candidate": candidate,
        "child-reversion": {**candidate, WAVES: identity(reverted_wave)},
    }


def check_commands(commands, root, fixture_root):
    require(len(commands) == len(COMMANDS), "EXACT_FOUR_FRESH_PYTHON_COMMANDS_REQUIRED")
    results = {}
    previous_end = 0
    for command_row, (label, mode, exit_code) in zip(commands, COMMANDS):
        require(command_row["label"] == label and command_row["exitCode"] == exit_code
                and command_row["log"] == "artifacts/" + label + ".log",
                "ORDERED_COMMAND_CONTRACT_DRIFT")
        require(previous_end <= command_row["startedUnix"] <= command_row["finishedUnix"],
                "ORDERED_ACTUAL_COMMAND_TIMESTAMPS_REQUIRED")
        previous_end = command_row["finishedUnix"]
        fixture = fixture_root / ("candidate" if mode == "synthetic" else mode)
        argv = command_row["argv"]
        require(argv == [sys.executable, "-I", "-S", "-B", "-c", CHILD,
                         str(fixture), str(root / "tmp"), mode],
                "EXACT_FRESH_CHILD_INVOCATION_REQUIRED:" + label)
        raw = safe_file(root, command_row["log"]).read_bytes()
        require(len(raw) == command_row["bytes"] <= LOG_LIMIT
                and sha(raw) == command_row["sha256"], "COMMAND_LOG_CHANGED:" + label)
        results[mode] = parse_result(raw, mode, fixture, root)
    return results


def validate_execution(execution, root, receiver):
    """Recompute source, real emitted outputs, all four raw logs and final fixtures. Returns None."""
    metadata, current, manifest_hash = inputs(receiver)
    require(execution["schema"] == SCHEMA and execution["scope"] == SCOPE
            and execution["state"] == "PASS"
            and execution["inputManifestSha256"] == manifest_hash
            and execution["sourceBase"] == metadata["receiverCommit"]
            and execution["preCodeAdmission"] == metadata["preCodeAdmission"],
            "EXECUTION_IDENTITY_DRIFT")
    require(execution["qualificationId"] == os.environ.get("M3_QUALIFICATION_ID")
            and execution["receiverHead"] == receiver_head(receiver, environment(root)),
            "FRESH_EXECUTION_BINDING_REQUIRED")
    require(all(execution[key] is False for key in FALSE_FLAGS)
            and all(execution[key] is True for key in TRUE_FLAGS), "SCOPE_FLAGS_DRIFT")
    require(execution["baselineTests"] == execution["candidateTests"] == 21
            and execution["childApprovalReversionTests"] == 21
            and execution["syntheticValidatorTests"] == 3,
            "ACTUAL_COUNTS_DRIFT")
    _, postimages = emitted()
    require(execution["emitted"] == postimages, "EMITTED_CUSTODY_CHANGED")
    custody_reference = execution["sourceCustody"]
    require(custody_reference["path"] == "artifacts/backport-contract-source-custody.json",
            "FIXED_CUSTODY_ARTIFACT_REQUIRED")
    raw = safe_file(root, custody_reference["path"]).read_bytes()
    require(len(raw) == custody_reference["bytes"] <= LOG_LIMIT
            and sha(raw) == custody_reference["sha256"], "SOURCE_CUSTODY_CHANGED")
    custody = json.loads(raw)
    require(custody["schema"] == 1 and custody["qualificationId"] == execution["qualificationId"]
            and custody["inputManifestSha256"] == manifest_hash
            and custody["files"] == 8 and custody["sourceBytes"] == 59781
            and custody["before"] == custody["after"] == current
            and custody["unchanged"] is True, "EXACT_SOURCE_OWNERS_MUST_REMAIN_UNCHANGED")
    wave = safe_file(SDK / "target/emitted", WAVES).read_bytes()
    require(wave.count(CHILD_APPROVAL.encode("utf-8")) == 1, "UNIQUE_CHILD_APPROVAL_ANCHOR_REQUIRED")
    reverted = wave.replace(CHILD_APPROVAL.encode("utf-8"), b"{}")
    expected_maps = expected_fixture_maps(current, postimages, reverted)
    fixture_root = root / "backport-contract-python"
    fixture_maps = {name: snapshot(fixture_root / name) for name in expected_maps}
    require(fixture_maps == expected_maps
            and custody["fixturesBefore"] == custody["fixturesAfter"] == expected_maps,
            "ALL_THREE_RECEIVER_FIXTURES_MUST_REMAIN_EXACT")
    results = check_commands(execution["commands"], root, fixture_root)
    require(execution["results"] == results, "RESULT_SUMMARY_MUST_MATCH_ACTUAL_LOGS")
    require(execution["storage"]["capBytes"] == CAP
            and execution["storage"]["reserveBytes"] == RESERVE
            and execution["storage"]["externalAggregateAndProcessSupervisorRequired"] is True
            and execution["storage"]["selfEnforcedAggregateClaim"] is False,
            "STORAGE_AND_EXTERNAL_PROCESS_SCOPE_DRIFT")


def execute(root, receiver, ident):
    metadata, before, manifest_hash = inputs(receiver)
    require(os.environ.get("M3_PROOF_ROOT") == str(root), "PROOF_ROOT_ENVIRONMENT_MISMATCH")
    env = environment(root)
    head = receiver_head(receiver, env)
    require(sys.version_info >= (3, 11) and sys.flags.isolated
            and sys.flags.no_site and sys.flags.dont_write_bytecode,
            "PROVISIONED_ISOLATED_PYTHON_311_OR_LATER_REQUIRED")
    directory, postimages = emitted()
    work = root / "backport-contract-python"
    require(not work.exists() and not work.is_symlink(), "FRESH_RECEIVER_FIXTURE_ROOT_REQUIRED")
    work.mkdir()
    candidate_data = {path: safe_file(directory if path in AFTER else receiver, path).read_bytes()
                      for path in PINNED}
    wave = candidate_data[WAVES]
    anchor = CHILD_APPROVAL.encode("utf-8")
    require(wave.count(anchor) == 1, "UNIQUE_CHILD_APPROVAL_ANCHOR_REQUIRED")
    reverted = wave.replace(anchor, b"{}")
    expected_maps = expected_fixture_maps(before, postimages, reverted)
    for name in ("baseline", "candidate", "child-reversion"):
        destination = work / name
        destination.mkdir()
        for path in PINNED:
            raw = safe_file(receiver, path).read_bytes() if name == "baseline" else candidate_data[path]
            if name == "child-reversion" and path == WAVES:
                raw = reverted
            target = destination.joinpath(*PurePosixPath(path).parts)
            target.parent.mkdir(parents=True, exist_ok=True)
            with target.open("xb") as stream:
                stream.write(raw)
    fixture_before = {name: snapshot(work / name) for name in expected_maps}
    require(fixture_before == expected_maps, "AUTHENTIC_EIGHT_FILE_RECEIVERS_REQUIRED")
    commands = []
    for label, mode, expected in COMMANDS:
        fixture = work / ("candidate" if mode == "synthetic" else mode)
        record, raw = command(root, receiver, label,
            [sys.executable, "-I", "-S", "-B", "-c", CHILD,
             str(fixture), str(root / "tmp"), mode], env, expected=expected, timeout=90)
        parse_result(raw, mode, fixture, root)
        commands.append(record)
    results = check_commands(commands, root, work)
    _, after, final_manifest_hash = inputs(receiver)
    require(before == after and manifest_hash == final_manifest_hash
            and head == receiver_head(receiver, env), "SOURCE_OR_MANIFEST_CHANGED")
    _, final_outputs = emitted()
    require(postimages == final_outputs, "MAVEN_OUTPUTS_CHANGED_DURING_EXECUTION")
    fixture_after = {name: snapshot(work / name) for name in expected_maps}
    require(fixture_before == fixture_after, "RECEIVER_FIXTURES_CHANGED_DURING_TESTS")
    custody = write(root, "backport-contract-source-custody.json", {
        "schema": 1, "qualificationId": ident, "inputManifestSha256": manifest_hash,
        "files": 8, "sourceBytes": 59781, "before": before, "after": after, "unchanged": True,
        "fixturesBefore": fixture_before, "fixturesAfter": fixture_after,
    })
    receipt = {
        "schema": SCHEMA, "scope": SCOPE, "state": "PASS", "qualificationId": ident,
        "receiverHead": head, "ciReportedSha": os.environ.get("GITHUB_SHA", ""),
        "sourceBase": metadata["receiverCommit"], "preCodeAdmission": metadata["preCodeAdmission"],
        "inputManifestSha256": manifest_hash, "sourceCustody": custody,
        "commands": commands, "results": results, "emitted": postimages,
        "baselineTests": 21, "candidateTests": 21, "childApprovalReversionTests": 21,
        "syntheticValidatorTests": 3,
        "storage": {"capBytes": CAP, "reserveBytes": RESERVE,
            "externalAggregateAndProcessSupervisorRequired": True,
            "selfEnforcedAggregateClaim": False, "finalFreeBytes": capacity(root)},
        **{name: False for name in FALSE_FLAGS}, **{name: True for name in TRUE_FLAGS},
    }
    validate_execution(receipt, root, receiver)
    output = write(root, "backport-contract-execution.json", receipt)
    print(json.dumps({"state": "PASS", "candidateTests": 21,
        "syntheticValidatorTests": 3, "receipt": output}, sort_keys=True))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--output-root", required=True)
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument("--inputs-only", action="store_true")
    mode.add_argument("--run", action="store_true")
    args = parser.parse_args()
    root, receiver, ident = context(args.output_root)
    if args.inputs_only:
        _, actual, manifest_hash = inputs(receiver)
        print(json.dumps({"state": "INPUTS_ONLY", "qualificationId": ident,
            "files": len(actual), "inputManifestSha256": manifest_hash,
            "nativeExecuted": False}, sort_keys=True))
    else:
        execute(root, receiver, ident)


if __name__ == "__main__":
    main()
