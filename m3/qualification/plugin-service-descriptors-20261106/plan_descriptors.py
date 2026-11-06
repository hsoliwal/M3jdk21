# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Whole-tree service-descriptor check: m3/tooling/jdk-naming/jdk_naming.py plan_tree restricted to every
META-INF/services/ directory of REV, i.e. the recipe's tree plan projected onto the java.util.ServiceLoader
descriptors (a few hundred small blobs, so it also runs in a blobless clone, where an unrestricted tree plan would
need every blob). A descriptor that names a provider under a pre-rename package shows up as a REWRITE row; a clean
tree prints rewrites=0. Exit 0 always; the printed counts are the result (GATES.tsv records them).
usage: python -I plan_descriptors.py <repo-root> <rev> <out.tsv>"""
import importlib.util
import subprocess
import sys
from pathlib import Path

root, rev, out = Path(sys.argv[1]), sys.argv[2], Path(sys.argv[3])
spec = importlib.util.spec_from_file_location("jdk_naming", root / "m3/tooling/jdk-naming/jdk_naming.py")
jn = importlib.util.module_from_spec(spec)
spec.loader.exec_module(jn)
paths = subprocess.run(["git", "-C", str(root), "ls-tree", "-r", "--name-only", rev], check=True,
                       capture_output=True, text=True).stdout.splitlines()
descriptors = [p for p in paths if jn.SERVICE_DESCRIPTOR_DIR in p]
only = tuple(sorted({p[:p.index(jn.SERVICE_DESCRIPTOR_DIR) + len(jn.SERVICE_DESCRIPTOR_DIR)] for p in descriptors}))
scoped = [p for p in descriptors if jn.in_scope(p, only)]
result = jn.plan_tree(root, rev, only)
rows = [("op", "path", "detail")]
rows += [("MOVE", a, b) for a, b in result["moves"]]
rows += [("REWRITE", r[0], str(r[1])) for r in result["rewrites"]]
rows += [("REFUSED_" + k, a, b) for k, a, b in result["refusals"]]
out.write_text("".join("\t".join(r) + "\n" for r in rows), encoding="utf-8", newline="\n")
print("DESCRIPTOR_PLAN rev=%s descriptors=%d dirs=%d in_scope=%d moves=%d rewrites=%d refusals=%d absent=%d" % (
    rev, len(descriptors), len(only), len(scoped), len(result["moves"]), len(result["rewrites"]),
    len(result["refusals"]), len(result.get("absent", []))))
for r in rows[1:]:
    print("\t".join(r))
