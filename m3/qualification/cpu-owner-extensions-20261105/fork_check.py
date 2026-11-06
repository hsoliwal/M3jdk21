"""fork_check.py <crate-dir-on-develop> <m3jdk-git-dir> <rename-parent-sha> <crate-target-path> <subcrate-A> <subcrate-B>
HASH_PINNED_FORK v1. For a target whose rename-parent bytes start two sibling edges (sub-crate A and B), fetches
both postimages and reports, per side, the lines it adds over the shared preimage (multiset difference) and
whether one side's added lines are all present in the other side's postimage (SUBSUMES). Prints the verdict:
A_SUBSUMES_B, B_SUBSUMES_A, EQUAL or DIVERGENT, plus the added/removed line counts."""
import json
import os
import subprocess
import sys
from collections import Counter

crate, gd, rparent, target, a, b = sys.argv[1:7]
REPO = "hsoliwal/com.synexia"


def gh(path, raw=False):
    args = ["gh", "api", path] + (["-H", "Accept: application/vnd.github.raw"] if raw else [])
    r = subprocess.run(args, capture_output=True)
    return r.stdout if raw else json.loads(r.stdout)


pre = subprocess.run(["git", "cat-file", "blob", f"{rparent}:{target}"], capture_output=True,
                     env=dict(os.environ, GIT_DIR=gd), check=True).stdout
tree = gh(f"repos/{REPO}/git/trees/develop:{crate}?recursive=1")["tree"]
posts = {}
for m in [t["path"] for t in tree if t["type"] == "blob" and t["path"].endswith("manifest.tsv")]:
    sub = m.split("/")[0]
    if sub not in (a, b):
        continue
    for row in gh(f"repos/{REPO}/contents/{crate}/{m}?ref=develop", raw=True).decode("utf-8").splitlines():
        cells = row.split("\t")
        if len(cells) >= 3 and cells[0] == target:
            name = cells[3] if len(cells) > 3 and cells[3] else target.rsplit("/", 1)[-1] + ".txt"
            posts[sub] = gh(f"repos/{REPO}/contents/{crate}/{m.rsplit('/', 1)[0]}/{name}?ref=develop", raw=True)
lines = lambda data: Counter(data.decode("utf-8", "replace").splitlines())
P, A, B = lines(pre), lines(posts[a]), lines(posts[b])
addA, addB = A - P, B - P
remA, remB = P - A, P - B
a_has_b = not (addB - A) and not (remB - remA)
b_has_a = not (addA - B) and not (remA - remB)
verdict = "EQUAL" if posts[a] == posts[b] else "A_SUBSUMES_B" if a_has_b else "B_SUBSUMES_A" if b_has_a else "DIVERGENT"
print(f"{verdict}\t{target.rsplit('/', 1)[-1]}\tA={a} +{sum(addA.values())}/-{sum(remA.values())}\tB={b} +{sum(addB.values())}/-{sum(remB.values())}"
      f"\tB_lines_missing_in_A={sum((addB - A).values())}\tA_lines_missing_in_B={sum((addA - B).values())}")
