# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""content_merge.py <base-commit> <head-commit> <head-path> <live-ref> <live-path> <out-dir>
Three-way re-application of a PR's hunks onto the live master file: base and head blobs are first put through the
JDK-naming text rules (jdk_naming.rewrite_text, the same rules the 5d454c3be9b rename applied to the live file), then
git merge-file -p <live> <base> <head>. Prints CLEAN/CONFLICT <n> and the added/removed line counts vs the live file."""
import subprocess
import sys
from pathlib import Path

sys.path.insert(0, "S:/m3-scratch/eb-conv/restore/naming")
import jdk_naming as jn  # noqa: E402

REPO = "C:/m3-superset/conv/m3jdk21"
base_c, head_c, head_path, live_ref, live_path, out = sys.argv[1:7]
out = Path(out)
out.mkdir(parents=True, exist_ok=True)


def blob(rev, path):
    return subprocess.run(["git", "-C", REPO, "show", f"{rev}:{path}"], capture_output=True).stdout


name = Path(live_path).name
files = {}
for tag, data, rename in (("base", blob(base_c, head_path), True), ("head", blob(head_c, head_path), True),
                          ("live", blob(live_ref, live_path), False)):
    text = jn.decoded(data)
    if rename:
        text, _counts = jn.rewrite_text(text)
    files[tag] = out / f"{name}.{tag}"
    files[tag].write_bytes(jn.encoded(text))
p = subprocess.run(["git", "merge-file", "-p", "-L", "live", "-L", "base", "-L", "head", str(files["live"]),
                    str(files["base"]), str(files["head"])], capture_output=True)
merged = out / f"{name}.merged"
merged.write_bytes(p.stdout)
diff = subprocess.run(["git", "diff", "--no-index", "--numstat", str(files["live"]), str(merged)],
                      capture_output=True, text=True).stdout.strip().split("\t")[:2]
print(("CLEAN" if p.returncode == 0 else f"CONFLICT {p.returncode}"), live_path, "added/removed vs live:", diff, "merged ->", merged)
