#!/usr/bin/env python3
"""Run and retain one bounded artifact-backed compile attempt of the task tooling."""
import datetime
import hashlib
import json
import os
from pathlib import Path
import subprocess
import time

ROOT = Path('/workspace/scratch/206049dfb270')
CRATE = ROOT / 'synexia/synexia-openrewrite-recipes/recipe-crates/m3-integrated-refinement-20261021'
OUT = ROOT / 'canonical-work/evidence/bootstrap-002'
OUT.mkdir(parents=True, exist_ok=False)
env = os.environ.copy()
env['JAVA_HOME'] = '/tmp/m3-build-tools/jdk-21.0.12.1+1'
env['PATH'] = env['JAVA_HOME'] + '/bin:' + env['PATH']
env['MAVEN_OPTS'] = '-Xmx512m -XX:ActiveProcessorCount=2 -XX:-UsePerfData'
command = ['/tmp/m3-build-tools/apache-maven-3.9.9/bin/mvn', '-B', '-ntp', '-f', str(CRATE / 'pom.xml'),
           '-Dmaven.repo.local=/tmp/m3-build-m2', '-Dhttps.proxyHost=browser-proxy', '-Dhttps.proxyPort=8889', '-Dhttp.proxyHost=browser-proxy', '-Dhttp.proxyPort=8889', 'test-compile']
start = time.monotonic()
observed = datetime.datetime.now(datetime.timezone.utc).isoformat()
with (OUT / 'maven.log').open('xb') as log:
    result = subprocess.run(command, cwd=ROOT, env=env, stdout=log, stderr=subprocess.STDOUT, timeout=600)
receipt = {'status': 'PASS' if result.returncode == 0 else 'FAIL', 'scope': 'focused-tooling-test-compile-only',
           'artifact_resolution': 'online bootstrap; not source-vendored reactor proof',
           'command': command, 'cwd': str(ROOT), 'observed_at': observed, 'exit_code': result.returncode,
           'duration_seconds': str(round(time.monotonic() - start, 3)),
           'log_sha256': hashlib.sha256((OUT / 'maven.log').read_bytes()).hexdigest(),
           'pom_sha256': hashlib.sha256((CRATE / 'pom.xml').read_bytes()).hexdigest()}
(OUT / 'RESULT.json').write_text(json.dumps(receipt, indent=2) + '\n')
print(json.dumps(receipt))
raise SystemExit(result.returncode)
