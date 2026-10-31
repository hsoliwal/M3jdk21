from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import subprocess

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
OUT = HERE / "maven-v02"
OUT.mkdir(exist_ok=False)
workspace = HERE / "input-workspace"
toolchain = ROOT / "toolchain"
jdk = toolchain / "jdk-21+35"
maven = toolchain / "apache-maven-3.9.9/bin/mvn"
pom = workspace / "m3/tooling/migration-recipes/tasks/jcc-merge-recovery-20261006/verification/pom.xml"
command = [str(maven), "-o", "-B", "-f", str(pom), "-Dmaven.repo.local=" + str(HERE / "m2-diagnostic-v02"),
           "-Dm3.recovery.materialized=" + str(HERE / "materialized-v02"), "verify"]
env = dict(os.environ, JAVA_HOME=str(jdk), PATH=str(jdk / "bin") + os.pathsep + os.environ["PATH"])
start = datetime.now(timezone.utc).isoformat()
with (OUT / "stdout.log").open("wb") as stdout, (OUT / "stderr.log").open("wb") as stderr:
    result = subprocess.run(command, cwd=workspace, env=env, stdout=stdout, stderr=stderr, check=False)
inputs = json.loads((HERE / "INPUT_SEAL.json").read_text())["files"]
unchanged = all(hashlib.sha256((workspace / row["path"]).read_bytes()).hexdigest() == row["sha256"] for row in inputs)
receipt = {"schema": "m3-jcc-recovery-maven-diagnostic/1", "started": start, "finished": datetime.now(timezone.utc).isoformat(),
           "command": command, "cwd": str(workspace), "java_home": str(jdk), "exit_code": result.returncode,
           "asm93_addon_receipt_sha256": "c49c23fd5ec3fc02ad0c3332e16fe4b0f8c9ea4bfab296ad013c726bc226823d",
           "input_seal_sha256": hashlib.sha256((HERE / "INPUT_SEAL.json").read_bytes()).hexdigest(),
           "inputs_unchanged": unchanged,
           "offline_cache_receipt": "runtime-resolution/actual-acquisition/OFFLINE_DIAGNOSTIC_CACHE_V02.json",
           "offline_cache_receipt_sha256": "034d226e546825818559e5517ce62bb00dd855f9ce59760fa607049388280da9",
           "scope": "Focused finite offline compiler/JUnit diagnostic with exact artifact identities. Complete vendored source/native/plugin closure remains separately pending; no JDK/export admission.",
           "stdout_sha256": hashlib.sha256((OUT / "stdout.log").read_bytes()).hexdigest(),
           "stderr_sha256": hashlib.sha256((OUT / "stderr.log").read_bytes()).hexdigest()}
(OUT / "receipt.json").write_text(json.dumps(receipt, indent=2) + "\n")
print(json.dumps(receipt, indent=2))
raise SystemExit(result.returncode if unchanged else 2)
