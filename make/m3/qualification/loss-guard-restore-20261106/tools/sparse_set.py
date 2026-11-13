# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""sparse_set.py <LOSS_CANDIDATES.tsv> <worktree> [extra dirs...]: sparse-checkout (no-cone) of the parent
directories of every RESTORE path plus the extra directories, then checkout of the branch."""
import subprocess
import sys

cand, wt, *extra = sys.argv[1:]
dirs = set(extra)
for line in list(open(cand, encoding="utf-8"))[1:]:
    f = line.rstrip("\n").split("\t")
    if f[5] == "RESTORE":
        dirs.add("/" + f[0].rsplit("/", 1)[0] + "/")
pats = "".join(d + "\n" for d in sorted(dirs))
print(len(dirs), "directories", file=sys.stderr)
subprocess.run(["git", "-C", wt, "sparse-checkout", "set", "--no-cone", "--stdin"], input=pats.encode(), check=True)
