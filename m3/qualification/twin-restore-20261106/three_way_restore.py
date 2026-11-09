"""three_way_restore.py <owner/repo> <ref> <rows.tsv> <out-dir> : RETAINED_3WAY v1 for DIVERGENT retained variants.
rows: tag<TAB>live-path. theirs = retained copy; its source commit T is the PR head (tag pr<N>) or the superset
side recorded for the landing tag (TAG_COMMITS). base = the file at the newest develop-history commit touching the
path that is an ancestor of T (GitHub compare status behind/identical). ours = live at <ref>.
git merge-file ours base theirs: CLEAN -> <out>/merged/<path> (a restore candidate); CONFLICT -> <out>/conflict/<path>
(owner review). Prints one row per path with the base commit and the outcome."""
import base64
import json
import os
import subprocess
import sys
import tempfile
from pathlib import Path

PREFIX = "m3/qualification/master-landing-20261028/retained-variants/"
TAG_COMMITS = {"landing-20261106c2r": "49b3877cf32f37340b22646afa88bf1d05ee4bac",
               "superset-landing-syn-20261106c2": "7b30708a724c06e060df023087b25b91373e8627"}


def gh(args):
    r = subprocess.run(["gh", "api", *args], capture_output=True, text=True, encoding="utf-8", errors="replace")
    return json.loads(r.stdout) if r.returncode == 0 and r.stdout and r.stdout.strip() else None


def content(repo, path, ref):
    d = gh([f"repos/{repo}/contents/{path}?ref={ref}"])
    if not d:
        return None
    if d.get("encoding") == "base64" and d.get("content"):
        return base64.b64decode(d["content"])
    b = gh([f"repos/{repo}/git/blobs/{d['sha']}"])
    return base64.b64decode(b["content"]) if b else None


def tag_commit(repo, tag):
    if tag in TAG_COMMITS:
        return TAG_COMMITS[tag]
    pr = gh([f"repos/{repo}/pulls/{tag[2:]}"]) if tag.startswith("pr") else None
    return pr["head"]["sha"] if pr else None


repo, ref, rows, out = sys.argv[1], sys.argv[2], sys.argv[3], Path(sys.argv[4])
for line in open(rows, encoding="utf-8").read().splitlines():
    tag, path = line.split("\t")
    t = tag_commit(repo, tag)
    ours, theirs = content(repo, path, ref), content(repo, PREFIX + tag + "/" + path, ref)
    hist = gh([f"repos/{repo}/commits?path={path}&sha={ref}&per_page=30"]) or []
    base_sha = None
    for c in hist:
        cmp = gh([f"repos/{repo}/compare/{c['sha']}...{t}"]) if t else None
        if cmp and cmp.get("status") in ("ahead", "identical"):
            base_sha = c["sha"]
            break
    base = content(repo, path, base_sha) if base_sha else b""
    with tempfile.TemporaryDirectory(dir=os.environ.get("TMP")) as d:
        fo, fb, ft = (Path(d) / n for n in ("o", "b", "t"))
        fo.write_bytes(ours or b""); fb.write_bytes(base or b""); ft.write_bytes(theirs or b"")
        r = subprocess.run(["git", "merge-file", "-p", "--diff3", str(fo), str(fb), str(ft)], capture_output=True)
    kind = "CLEAN" if r.returncode == 0 else f"CONFLICT={r.returncode}"
    dst = out / ("merged" if r.returncode == 0 else "conflict") / path
    dst.parent.mkdir(parents=True, exist_ok=True)
    dst.write_bytes(r.stdout)
    same = "=theirs" if r.stdout == (theirs or b"") else ("=ours" if r.stdout == (ours or b"") else "")
    print(f"{kind}{same}\t{tag}\t{path}\tbase={base_sha[:12] if base_sha else 'NONE'}\tT={t[:12] if t else '?'}")
