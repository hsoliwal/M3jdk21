#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Bounded qualification. Prefix extraction is NOT a matched M3JDK runtime test."""
import csv
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import textwrap

base, delivery, evidence = (Path(p).resolve() for p in sys.argv[1:4])
crate = Path(__file__).resolve().parent
evidence.mkdir(parents=True, exist_ok=True)

def execute(name, command, cwd=base, env=None, expected=0):
    run = subprocess.run(command, cwd=cwd, env=env, text=True, capture_output=True, timeout=90)
    log = f'exit={run.returncode}\n' + run.stdout + run.stderr
    (evidence/(name+'.log')).write_text(log, encoding='utf-8')
    if (expected == 0 and run.returncode != 0) or (expected != 0 and run.returncode == 0):
        raise RuntimeError(name + '\n' + log)
    print(name + ': exit=' + str(run.returncode))
    return log

with (crate/'postimages.tsv').open() as handle:
    seals = list(csv.DictReader(handle, delimiter='\t'))
for row in seals:
    raw = (delivery/row['path']).read_bytes()
    if hashlib.sha256(raw).hexdigest() != row['after_sha256']:
        raise RuntimeError('postimage drift: ' + row['path'])
    current = base/row['path']
    if row['before_sha256'] == 'ABSENT':
        if current.exists(): raise RuntimeError('absent-before admission failed')
    elif hashlib.sha256(current.read_bytes()).hexdigest() != row['before_sha256']:
        raise RuntimeError('preimage drift')

# Execute the actual isolated workflow run block, not a hand-rewritten equivalent.
before = (base/'.github/workflows/m3-foundation.yml').read_text()
after = (delivery/'.github/workflows/m3-foundation.yml').read_text()
assert before[before.index('  foundation:\n'):] == after[after.index('  foundation:\n'):]
for name, source, expected in [('before', before, 1), ('after', after, 0)]:
    start = source.index('        run: |\n') + len('        run: |\n')
    end = source.index('  foundation:\n')
    script = textwrap.dedent(source[start:end])
    with tempfile.TemporaryDirectory() as temp:
        env = dict(os.environ, TEMP=temp, TMP=temp, TMPDIR=temp, RUNNER_TEMP=temp)
        log = execute('foundation-'+name, ['bash', '-c', script], env=env, expected=expected)
    if name == 'before': assert 'invalid flag: \\' in log
    else: assert 'M3JDK_INSTANCE_INDEX_PRECOMPUTE_PASS checks=25' in log

# Independent scalar oracle and actual source-derived prefix. This intentionally stops
# before m3(): it proves ordered guards, not native storage, VM fields or positive loops.
methods = ['public boolean regionMatches(int toffset, String other, int ooffset, int len)',
           'public boolean regionMatches(boolean ignoreCase, int toffset,']
probe_tail = r'''
final class RegionPrefixProbe {
    public static void main(java.lang.String[] args) {
        long checks = 0;
        for (int ln : new int[] {0, 1, 3, 100}) for (int rn : new int[] {0, 1, 3, 100}) {
            String left = new String(ln), right = new String(rn);
            for (int len : new int[] {Integer.MIN_VALUE, Integer.MIN_VALUE+1, -1073741825, -2, -1, 0})
            for (int to : new int[] {Integer.MIN_VALUE, -1, 0, 1, ln, ln+1, Integer.MAX_VALUE})
            for (int from : new int[] {Integer.MIN_VALUE, -1, 0, 1, rn, rn+1, Integer.MAX_VALUE}) {
                boolean expected = to >= 0 && from >= 0 && (long)to+len <= ln && (long)from+len <= rn;
                if (left.regionMatches(to, right, from, len) != expected
                        || left.regionMatches(false, to, right, from, len) != expected
                        || left.regionMatches(true, to, right, from, len) != expected) throw new AssertionError("oracle");
                checks += 3;
                boolean shortCircuit = from < 0 || to < 0 || (long)to+len > ln;
                for (int mode = 0; mode < 3; mode++) {
                    try {
                        boolean result = mode == 0 ? left.regionMatches(to, null, from, len)
                            : left.regionMatches(mode == 2, to, null, from, len);
                        if (!shortCircuit || result) throw new AssertionError("null order");
                    } catch (NullPointerException expectedNull) {
                        if (shortCircuit) throw new AssertionError("null short circuit");
                    }
                    checks++;
                }
            }
        }
        System.out.println("SOURCE_PREFIX_PASS checks="+checks);
    }
}
'''
for name, root, expected in [('before', base, 1), ('after', delivery, 0)]:
    source = (root/'src/java.base/share/classes/java/lang/String.java').read_text()
    parts = []
    for method in methods:
        start = source.index(method)
        end = source.index('        M3String leftM3 = m3();', start)
        parts.append(source[start:end] + '        throw new AssertionError("M3_BOUNDARY_REACHED");\n    }\n')
    derived = 'final class String { private final int size; String(int n) {size=n;} public int length(){return size;}\n' + '\n'.join(parts) + '}\n' + probe_tail
    directory = evidence/('prefix-'+name)
    directory.mkdir(exist_ok=True)
    fixture = directory/'RegionPrefixProbe.java'
    fixture.write_text(derived)
    execute('prefix-compile-'+name, ['javac', '--release', '21', '-Xlint:all', '-Werror', '-d', str(directory), str(fixture)])
    log = execute('prefix-'+name, ['java', '-ea', '-cp', str(directory), 'RegionPrefixProbe'], expected=expected)
    if name == 'before': assert 'M3_BOUNDARY_REACHED' in log
    else: assert 'SOURCE_PREFIX_PASS' in log

classes = evidence/'regression-classes'
classes.mkdir(exist_ok=True)
test = delivery/'test/jdk/java/lang/String/M3RegionMatchesNonPositive.java'
execute('regression-compile', ['javac', '--release', '21', '-Xlint:all', '-Werror', '-d', str(classes), str(test)])
for mode, options in [('compact', []), ('utf16', ['-XX:-CompactStrings']), ('interpreter', ['-Xint']),
                      ('c2', ['-Xbatch', '-XX:-TieredCompilation'])]:
    execute('regression-'+mode, ['java', '-ea', *options, '-cp', str(classes), 'M3RegionMatchesNonPositive'])
json.loads((delivery/'m3/docs/name-mapping.json').read_text())
(evidence/'qualification-scope.json').write_text(json.dumps({
    'source_base': '855afe57a679aeb454052630de7beddcb25f5f8f',
    'stock_jdk_regression': 'PASS', 'actual_source_prefix': 'PASS',
    'existing_workflow_owner_contract': 'PASS',
    'matched_M3JDK_jtreg_HotSpot_JNI': 'NOT_RUN', 'root_Maven_reactor': 'NOT_QUALIFIED',
    'all_JDK_tasks': 'OPEN', 'all_OpenJDK_bugs': 'NOT_CLAIMED'
}, indent=2)+'\n')
print('BOUNDED_QUALIFICATION_PASS; M3_RUNTIME_PENDING')
