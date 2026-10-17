#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0
"""Run one owned acceptance gate against the qualified received tree, logging exact inputs."""
import datetime
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys

ROOT = Path('/workspace/scratch/f0742e08085b/M3jdk21')
CRATE = Path('/workspace/scratch/f0742e08085b/Synexia/synexia-openrewrite-recipes/recipes/m3-five-provider-array-interop-20261017')
EVIDENCE = Path(__file__).resolve().parent
JAVA = Path('/tmp/m3-toolchain/jdk-21.0.12.1+1')
MAVEN = Path('/tmp/m3-toolchain/apache-maven-3.9.11/bin/mvn')
GATES = {
    'donation_verifier': [sys.executable, '-u', str(ROOT / 'm3/collections/verify_donation.py'), '--cost-probe'],
    'source_admission': [sys.executable, '-u', str(CRATE / 'contract/test_source_admission.py'),
                         '--target', str(ROOT), '--evidence', str(EVIDENCE / 'source-admission')],
    'core_maven_install': [str(MAVEN), '--offline', '--batch-mode', '--no-transfer-progress',
                           '-Dmaven.repo.local=/tmp/m3-maven-repository',
                           '-f', str(ROOT / 'm3/collections/pom.xml'), 'install'],
}


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


name = sys.argv[1]
command = GATES[name]
environment = dict(os.environ)
environment['JAVA_HOME'] = str(JAVA)
environment['PATH'] = str(JAVA / 'bin') + os.pathsep + str(MAVEN.parent) + os.pathsep + environment['PATH']
environment['JUNIT_CONSOLE_JAR'] = '/tmp/m3-toolchain/junit-platform-console-standalone-1.12.2.jar'
environment['PYTHONDONTWRITEBYTECODE'] = '1'
start = datetime.datetime.now(datetime.UTC).isoformat()
verifier = ROOT / 'm3/collections/verify_donation.py'
source_identity = {path.relative_to(ROOT).as_posix(): digest(path)
                   for path in sorted((ROOT / 'm3/collections/src').rglob('*')) if path.is_file()}
log_path = EVIDENCE / (name + '.log')
with log_path.open('w') as log:
    header = '$ ' + repr(command) + '\n'
    log.write(header)
    log.flush()
    print(header, end='', flush=True)
    process = subprocess.Popen(command, cwd=ROOT, env=environment, stdout=subprocess.PIPE,
                               stderr=subprocess.STDOUT, text=True, bufsize=1)
    for line in process.stdout:
        log.write(line)
        log.flush()
        print(line, end='', flush=True)
    code = process.wait()
    log.write('\nEXIT=' + str(code) + '\n')
after = {path.relative_to(ROOT).as_posix(): digest(path)
         for path in sorted((ROOT / 'm3/collections/src').rglob('*')) if path.is_file()}
unchanged = source_identity == after
receipt = {
    'schema': 'M3_RECEIVED_GATE_EXECUTION_V1', 'gate': name,
    'copyright': 'Copyright 2026 Hitesh Soliwal and contributors', 'license': 'Apache-2.0',
    'canonical_source_commit': '866e240ec1ac8600999d474eb7b8ce1930e326e3',
    'target_master_commit': 'f3c615693c41bac1bf6ff49ba53e83f85425bca0',
    'target_root': str(ROOT), 'mode': 'RECEIVED_SOURCE_QUALIFICATION',
    'command': command, 'started_utc': start, 'finished_utc': datetime.datetime.now(datetime.UTC).isoformat(),
    'exit_code': code, 'source_files_unchanged': unchanged,
    'verifier_sha256': digest(verifier), 'sdk_results_sha256': digest(CRATE / 'target/candidate/SDK_RESULTS.tsv'),
    'java_home': str(JAVA), 'junit_console_sha256': digest(Path(environment['JUNIT_CONSOLE_JAR'])),
    'source_census_sha256': hashlib.sha256(''.join(path + '\t' + sha + '\n' for path, sha in sorted(source_identity.items())).encode()).hexdigest(),
    'log_sha256': digest(log_path), 'status': 'PASS' if code == 0 and unchanged else 'FAIL',
}
(EVIDENCE / (name + '-execution.json')).write_text(json.dumps(receipt, indent=2) + '\n')
print(json.dumps(receipt, indent=2), flush=True)
raise SystemExit(code if code else (0 if unchanged else 1))
