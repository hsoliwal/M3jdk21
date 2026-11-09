# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Run the compiled Object contract on the actual rebuilt M3JDK21 image."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess

parser = argparse.ArgumentParser()
parser.add_argument("--jdk", required=True, type=Path)
parser.add_argument("--classes", type=Path)
parser.add_argument("--mode", choices=["normal", "int", "c1", "c2"], action="append")
args = parser.parse_args()
crate = Path(__file__).resolve().parent.parent
jdk = args.jdk.resolve()
classes = (args.classes or crate / "target/runtime-classes").resolve()
assert (classes / "M3ObjectBaseVtableContract.class").is_file(), "run strict owner proof first"
java = jdk / "bin/java"
libjvm = jdk / "lib/server/libjvm.so"
assert java.is_file() and libjvm.is_file(), "expected actual Linux server image"
sha = lambda path: hashlib.sha256(path.read_bytes()).hexdigest()
out = crate / "target/runtime-image-proof"
out.mkdir(parents=True, exist_ok=True)
options = {"normal": [], "int": ["-Xint"],
           "c1": ["-Xcomp", "-XX:TieredStopAtLevel=1"],
           "c2": ["-Xcomp", "-XX:-TieredCompilation"]}
expected = "PASS M3ObjectBaseVtableContract adapters arrays virtuals waits platform virtual"
rows = []
for mode in args.mode or ["normal", "int"]:
    # Share-off avoids treating a different archive as evidence for these owner
    # bytes; the ordinary full-image/CDS build remains an independent parent gate.
    command = [str(java), "-Xshare:off", "-Xverify:all", *options[mode], "-cp", str(classes),
               "M3ObjectBaseVtableContract"]
    completed = subprocess.run(command, capture_output=True, text=True, timeout=60)
    (out / (mode + ".command.json")).write_text(json.dumps(command, indent=2) + "\n")
    (out / (mode + ".log")).write_text(completed.stdout + completed.stderr)
    assert completed.returncode == 0 and completed.stdout.strip() == expected, mode + "\n" + completed.stdout + completed.stderr
    rows.append({"mode": mode, "exit": completed.returncode, "stdout": completed.stdout.strip()})
report = {"actual_image": str(jdk), "java_sha256": sha(java), "libjvm_sha256": sha(libjvm),
          "fixture_class_sha256": sha(classes / "M3ObjectBaseVtableContract.class"),
          "results": rows, "runtime_patch": False,
          "full_image_build_and_CDS": "separate parent integration receipt"}
(out / "RESULT.json").write_text(json.dumps(report, indent=2) + "\n")
print(json.dumps(report, indent=2))
