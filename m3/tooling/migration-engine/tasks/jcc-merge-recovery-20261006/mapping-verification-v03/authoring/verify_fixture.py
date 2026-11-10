#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Capture the fixture-only binder proof, including CLI publication refusal."""
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
import subprocess
import sys

import bind_mapping as binder
from test_bind_mapping import MappingBindingTest

HERE = Path(__file__).resolve().parent
OUTPUT = HERE / "verification-v02"
OUTPUT.mkdir(exist_ok=False)
START = datetime.now(timezone.utc).isoformat()
MappingBindingTest.setUpClass()
fixture_input = OUTPUT / "fixture-source-input.json"
fixture_input.write_bytes(binder.pretty(MappingBindingTest.packet))
commands = []


def run(name, command):
    result = subprocess.run(command, cwd=HERE, capture_output=True, check=False)
    (OUTPUT / (name + ".stdout.log")).write_bytes(result.stdout)
    (OUTPUT / (name + ".stderr.log")).write_bytes(result.stderr)
    commands.append({"name": name, "command": command, "exit_code": result.returncode,
                     "stdout_sha256": binder.digest(result.stdout), "stderr_sha256": binder.digest(result.stderr)})
    return result


tests = run("01-unit-tests", [sys.executable, "-B", "-m", "unittest", "-v", "test_bind_mapping.py"])
assert tests.returncode == 0
common = [sys.executable, "-B", str(HERE / "bind_mapping.py"), "--audit-root", str(HERE.parents[1]),
          "--source-input", str(fixture_input)]
fixture = run("02-fixture-authoring", common + ["--fixture-only", "--output", str(OUTPUT / "fixture-output")])
assert fixture.returncode == 0
refused = run("03-publication-mode-refusal", common + ["--output", str(OUTPUT / "forbidden-output")])
assert refused.returncode != 0 and not (OUTPUT / "forbidden-output").exists()
receipt = {"schema": "m3-jcc-mapping-fixture-verification/1", "status": "PASS", "fixture": True,
           "started": START, "finished": datetime.now(timezone.utc).isoformat(), "unit_tests": 13,
           "commands": commands, "input_hashes": {name: binder.digest((HERE / name).read_bytes())
               for name in ["bind_mapping.py", "test_bind_mapping.py", "verify_fixture.py"]},
           "fixture_input_sha256": binder.digest(fixture_input.read_bytes()),
           "scope": "Unpublishable synthetic source input; validates authoring and refusal only. No actual source publication or target materialization claimed."}
(OUTPUT / "receipt.json").write_bytes(binder.pretty(receipt))
print(json.dumps({"status": "PASS", "unit_tests": 13, "fixture_authoring_exit": fixture.returncode,
                  "publication_refusal_exit": refused.returncode, "receipt": str(OUTPUT / "receipt.json")}))
