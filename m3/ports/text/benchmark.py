#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal; SPDX-License-Identifier: Apache-2.0
"""Three independent diagnostic forks with fixed G1/heap; preserve raw measurements."""
import hashlib
import json
import os
from pathlib import Path
import subprocess
import platform

root = Path(__file__).resolve().parent
jdk = Path(os.environ['M3_JDK'])
build = root / 'build'; build.mkdir(exist_ok=True)
subprocess.run([str(jdk / 'bin/javac'), '--release', '21', '-Xlint:all', '-Werror', '-cp', str(build),
                '-d', str(build), str(root / 'test/TextBenchmark.java')], check=True)
records = []
for fork in range(3):
    argv = [str(jdk / 'bin/java'), '-Xms256m', '-Xmx256m', '-XX:+UseG1GC', '-cp', str(build), 'TextBenchmark', '2000']
    run = subprocess.run(argv, check=True, capture_output=True, text=True)
    print('fork=' + str(fork) + '\n' + run.stdout)
    records.append({'fork':fork, 'argv':argv, 'stdout':run.stdout, 'stderr':run.stderr, 'exit':run.returncode})
result = {'status':'diagnostic-only', 'warmup_iterations':2000, 'measurement_iterations':2000,
          'input':'4096 Latin1 a + 4096 UTF16 U+0100, retained operands', 'records':records,
          'platform':platform.platform(),
          'jdk_version':subprocess.run([str(jdk/'bin/java'), '-version'], capture_output=True, text=True).stderr,
          'source_hashes':{p.relative_to(root).as_posix():hashlib.sha256(p.read_bytes()).hexdigest()
                           for p in [*sorted((root/'src').rglob('*.java')),root/'test/TextBenchmark.java']},
          'limitations':['not JMH, no general speedup claim','no latency percentile distribution',
                         'payload accounting excludes descriptor/key/map headers and JVM retained heap',
                         'no native/mapped backend or precomputation amortization experiment']}
(build / 'benchmark-result.json').write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
