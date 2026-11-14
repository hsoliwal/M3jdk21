from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import subprocess

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
OUT = HERE / "retention-maven-v01"
OUT.mkdir(exist_ok=False)
fixture = HERE / "installer-v01/retention-fixture"
proof = json.loads((HERE / "installer-v01/receipt.json").read_text())
pins = next(row for row in proof["checks"] if "fixture_baseline_commit" in row)
toolchain = ROOT / "toolchain"
jdk = toolchain / "jdk-21+35"
maven = toolchain / "apache-maven-3.9.9/bin/mvn"
env = dict(os.environ, JAVA_HOME=str(jdk), PATH=str(jdk / "bin") + os.pathsep + os.environ["PATH"])
common = [str(maven), "-o", "-B", "-f", str(fixture / "m3/history/retention-pom.xml"),
          "-Dmaven.repo.local=" + str(HERE / "m2-diagnostic-v02"), "-Dm3.repo=" + str(fixture),
          "-Dm3.retentionCommit=" + pins["fixture_candidate_commit"]]
commands = []
for name, options in [
        ("01-default-verify", ["-Dm3.output=" + str(OUT / "default-report"), "verify"]),
        ("02-explicit-baseline-execution", ["-Dm3.retentionBaseline=" + pins["fixture_baseline_commit"],
            "-Dm3.output=" + str(OUT / "explicit-report"),
            "org.codehaus.mojo:exec-maven-plugin:3.6.4:exec@retained-capability-audit"])]:
    began = datetime.now(timezone.utc).isoformat()
    command = common + options
    with (OUT / (name + ".stdout.log")).open("wb") as stdout, (OUT / (name + ".stderr.log")).open("wb") as stderr:
        result = subprocess.run(command, cwd=fixture, env=env, stdout=stdout, stderr=stderr, check=False)
    commands.append({"name": name, "command": command, "started": began,
                     "finished": datetime.now(timezone.utc).isoformat(), "exit_code": result.returncode,
                     "stdout_sha256": hashlib.sha256((OUT / (name + ".stdout.log")).read_bytes()).hexdigest(),
                     "stderr_sha256": hashlib.sha256((OUT / (name + ".stderr.log")).read_bytes()).hexdigest()})
    if result.returncode:
        break
passed = len(commands) == 2 and all(command["exit_code"] == 0 for command in commands)
summaries = {}
if passed:
    for name in ["default", "explicit"]:
        report = json.loads((OUT / (name + "-report") / "SUMMARY.json").read_text())
        assert report["targets"] == 469 and report["missing"] == 0 and report["declaration_conflict_count"] == 0
        assert report["include_parents"] is True
        assert report["explicit_baseline_commits"] == ([] if name == "default" else [pins["fixture_baseline_commit"]])
        summaries[name] = {key: report[key] for key in ["commit", "root", "targets", "retained_exact", "missing", "declaration_conflict_count", "direct_parent_commits", "explicit_baseline_commits"]}
receipt = {"schema": "m3-jcc-retention-maven-wiring-diagnostic/1", "status": "PASS" if passed else "FAIL",
           "commands": commands, "fixture_pins": pins, "summaries": summaries,
           "scope": "Existing Maven retention lifecycle and explicit profile in a synthetic immutable Git fixture. Exact JAR/POM diagnostic cache; complete source/native/plugin admission and real JDK acceptance remain separate."}
(OUT / "receipt.json").write_text(json.dumps(receipt, indent=2) + "\n")
print(json.dumps({"status": receipt["status"], "commands": len(commands), "receipt": str(OUT / "receipt.json")}))
raise SystemExit(0 if passed else 1)
