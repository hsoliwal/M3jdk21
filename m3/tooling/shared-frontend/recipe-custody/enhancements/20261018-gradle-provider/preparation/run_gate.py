#!/usr/bin/env python3
"""Record actual commands and outcomes for this isolated qualification; no source transform."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
from datetime import datetime, timezone

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("label")
parser.add_argument("--cwd", type=Path, required=True)
parser.add_argument("--jdk", type=Path, required=True)
parser.add_argument("--previous")
parser.add_argument("command", nargs=argparse.REMAINDER)
args = parser.parse_args()
root = Path(__file__).resolve().parent
command = args.command
if command and command[0] == "--":
    command = command[1:]
if not command:
    raise SystemExit("A real command is required")
if args.previous:
    previous = json.loads((root / "logs" / (args.previous + ".json")).read_text())
    if previous["exit_code"] != 0:
        raise SystemExit("Previous gate did not pass: " + args.previous)
record_path = root / "logs" / (args.label + ".json")
log_path = root / "logs" / (args.label + ".log")
if record_path.exists() or log_path.exists():
    raise SystemExit("Refusing to overwrite gate evidence: " + args.label)
env = dict(os.environ)
env.update({"JAVA_HOME": str(args.jdk), "PATH": str(args.jdk / "bin") + os.pathsep + env["PATH"],
            "MAVEN_OPTS": "-Xmx1536m -Djava.io.tmpdir=" + str(root / "tmp"),
            "PYTHONDONTWRITEBYTECODE": "1"})
record = {"label": args.label, "command": command, "cwd": str(args.cwd.resolve()),
          "jdk": str(args.jdk), "previous": args.previous,
          "started_utc": datetime.now(timezone.utc).isoformat()}
with log_path.open("xb") as log:
    result = subprocess.run(command, cwd=args.cwd, env=env, stdout=log, stderr=subprocess.STDOUT, check=False)
record.update({"finished_utc": datetime.now(timezone.utc).isoformat(), "exit_code": result.returncode,
               "log_sha256": hashlib.sha256(log_path.read_bytes()).hexdigest()})
record_path.write_text(json.dumps(record, indent=2) + "\n")
with (root / "EVENTS.jsonl").open("a") as events:
    events.write(json.dumps({"event": "QUALIFICATION_COMMAND", **record}) + "\n")
print(json.dumps(record))
print(log_path.read_text(errors="replace")[-12000:])
sys.exit(result.returncode)
