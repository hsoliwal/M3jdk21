# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""make_probe.py <retargeted-test.c> <probe.c>: non-fatal copy of the retired region test; each failing assertion prints
its line and the mock's counters; adds the DeleteLocalRef table slot master's raise() calls (absent from the mock)."""
import sys
from pathlib import Path

BS = chr(92)
t = Path(sys.argv[1]).read_text()
old = ('static void require(int ok) {\n    if (!ok) { fputs("viewport region assertion failed' + BS + 'n", stderr);'
       ' exit(1); }\n}\n')
assert old in t, 'require() not found'
t = t.replace(old, 'static int probe_failures;\nstatic void require_line(int ok, int line, const char *expr);\n'
                   '#define require(ok) require_line((ok), __LINE__, #ok)\n')
t = t.replace('int main(void) {',
              'static void require_line(int ok, int line, const char *expr) {\n'
              '    if (!ok) { probe_failures++; printf("PROBE line %d FAILS: %s | allocations=%d bytes=%zu reads=%d'
              ' writes=%d pending=%d' + BS + 'n",\n'
              '            line, expr, allocations, allocation_bytes, reads, writes, pending); }\n}\n'
              'static void JNICALL del(JNIEnv *env, jobject o) { (void)env; (void)o; }\n'
              'int main(void) {\n    setvbuf(stdout, NULL, _IONBF, 0);', 1)
t = t.replace('api.FindClass = find; api.ThrowNew = fail;',
              'api.FindClass = find; api.ThrowNew = fail; api.DeleteLocalRef = del;', 1)
t = t.replace('    puts("PASS viewport JNI regions:',
              '    printf("PROBE failures=%d' + BS + 'n", probe_failures);\n    puts("PASS viewport JNI regions:', 1)
Path(sys.argv[2]).write_text(t, newline='\n')
