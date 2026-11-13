"""retained_audit.py <owner/repo> <ref> <retained.tsv> : RETAINED_AUDIT v1. For every retained variant
(m3/qualification/master-landing-20261028/retained-variants/<tag>/<path>) compare its bytes with the live <path> at
<ref>, by line-multiset containment:
  SAME              identical bytes
  LIVE_SUPERSET     every retained line is in live (the live side already holds the variant's content)
  RETAINED_SUPERSET every live line is in the retained copy, which has more lines -> live REGRESSED, restore candidate
  DIVERGENT         both sides have lines the other lacks -> owner review (NEC)
  LIVE_MISSING      no live path -> loss candidate
Prints one TSV row per file with the line counts."""
import base64
import json
import subprocess
import sys
from collections import Counter

PREFIX = "m3/qualification/master-landing-20261028/retained-variants/"


def content(repo, path, ref):
    r = subprocess.run(["gh", "api", f"repos/{repo}/contents/{path}?ref={ref}"], capture_output=True, text=True)
    if r.returncode:
        return None
    d = json.loads(r.stdout)
    if d.get("encoding") == "base64":
        return base64.b64decode(d["content"]).decode("utf-8", "replace")
    blob = subprocess.run(["gh", "api", f"repos/{repo}/git/blobs/{d['sha']}", "--jq", ".content"], capture_output=True, text=True)
    return base64.b64decode(blob.stdout).decode("utf-8", "replace")


repo, ref, tsv = sys.argv[1:4]
for row in open(tsv, encoding="utf-8").read().splitlines():
    status, rpath = row.split("\t")
    tag, live_path = rpath[len(PREFIX):].split("/", 1)
    ret, live = content(repo, rpath, ref), content(repo, live_path, ref)
    if ret is None:
        print(f"RETAINED_GONE\t{tag}\t{live_path}")
        continue
    if live is None:
        print(f"LIVE_MISSING\t{tag}\t{live_path}\tretained={len(ret.splitlines())}")
        continue
    if ret == live:
        print(f"SAME\t{tag}\t{live_path}")
        continue
    r, l = Counter(ret.splitlines()), Counter(live.splitlines())
    r_only, l_only = sum((r - l).values()), sum((l - r).values())
    kind = "LIVE_SUPERSET" if r_only == 0 else ("RETAINED_SUPERSET" if l_only == 0 else "DIVERGENT")
    print(f"{kind}\t{tag}\t{live_path}\tretained_only={r_only}\tlive_only={l_only}")
