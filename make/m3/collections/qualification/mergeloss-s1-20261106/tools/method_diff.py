# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# usage: python -I method_diff.py <base-classes> <head-classes> <fqcn>
# javap -c -p per method, constant-pool indices (#N) masked; prints IDENTICAL / CHANGED / ADDED / REMOVED per method.
import re, subprocess, sys
import os
JAVAP = os.path.join(os.environ["JAVA_HOME"], "bin", "javap")
def methods(cp, name):
    out = subprocess.run([JAVAP, "-c", "-p", "-cp", cp, name],
                         capture_output=True, text=True, check=True).stdout
    result, cur = {}, None
    for line in out.splitlines():
        if line.startswith("  ") and not line.startswith("   ") and line.rstrip().endswith(";"):
            cur = line.strip(); result[cur] = []
        elif cur and line.startswith("    "):
            result[cur].append(re.sub(r"\s+", " ", re.sub(r"#\d+", "#", line.strip())))
    return result
b, h = methods(sys.argv[1], sys.argv[3]), methods(sys.argv[2], sys.argv[3])
for sig in sorted(set(b) | set(h)):
    state = "ADDED" if sig not in b else "REMOVED" if sig not in h else "IDENTICAL" if b[sig] == h[sig] else "CHANGED"
    print("%s\t%s" % (state, sig))
