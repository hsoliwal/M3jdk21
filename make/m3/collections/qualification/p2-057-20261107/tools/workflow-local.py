# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""workflow-local.py <workflow.yml> <checkout-root> <runner-temp> [job]: run a GitHub workflow job's `run:` steps locally.

Each step runs as GitHub's `shell: bash` does (bash --noprofile --norc -eo pipefail <script>) in <checkout-root> with
RUNNER_TEMP=<runner-temp>; `uses:` steps (checkout, setup-java) are provided by the caller: the checkout root is a
`git archive` of the workflow's sparse paths, JAVA_HOME/PATH and Maven come from the environment (MAVEN_ARGS may carry
the repository options). Stops at the first failing step; prints one line per step and exits with its status."""
import os
import subprocess
import sys
from pathlib import Path

import yaml

workflow, root, temp = Path(sys.argv[1]), Path(sys.argv[2]), Path(sys.argv[3])
data = yaml.safe_load(workflow.read_text())
jobs = data["jobs"]
job = jobs[sys.argv[4]] if len(sys.argv) > 4 else next(iter(jobs.values()))
temp.mkdir(parents=True, exist_ok=True)
env = dict(os.environ, RUNNER_TEMP=str(temp), GITHUB_WORKSPACE=str(root), CI="true")
status = 0
for number, step in enumerate(job["steps"], 1):
    name = step.get("name") or step.get("uses") or "step %d" % number
    if "run" not in step:
        print("STEP %d %s: provided by the caller (%s)" % (number, name, step.get("uses")), flush=True)
        continue
    script = temp / ("step-%02d.sh" % number)
    script.write_text(step["run"], newline="\n")
    log = temp / ("step-%02d.log" % number)
    with open(log, "w") as out:
        rc = subprocess.run(["bash", "--noprofile", "--norc", "-eo", "pipefail", str(script)], cwd=root, env=env,
                            stdout=out, stderr=subprocess.STDOUT).returncode
    tail = log.read_text(errors="replace").strip().splitlines()[-1:] or [""]
    print("STEP %d %s: exit %d | %s" % (number, name, rc, tail[0][:160]), flush=True)
    if rc:
        status = rc
        break
print("WORKFLOW %s %s" % (workflow.name, "PASS" if status == 0 else "FAIL"))
sys.exit(status)
