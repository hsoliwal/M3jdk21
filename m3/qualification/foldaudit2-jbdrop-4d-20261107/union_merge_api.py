"""union_merge_api.py <owner/repo> <base-branch> <pr-head-sha> <work-dir> [--base=<sha>] [--commit <new-branch> <message-file>]
: UNION_MERGE_API v1 (additive superset merge without a local object store).
ours = tip of <base-branch>, theirs = PR head, base = their merge base (compare API). For every path the PR side
changed since the merge base:
  removed on the PR side           -> REFUSE (additive: nothing is deleted)
  master unchanged since the base  -> TAKE_THEIRS (PR blob id reused)
  both changed                     -> git merge-file --diff3; clean -> MERGED; conflict -> CONFLICT, the three sides
                                      and the marked merge are written to <work-dir>/<path>.{ours,base,theirs,merged}
A resolved file placed at <work-dir>/<path>.resolved replaces a CONFLICT (markers refused). Files under
<work-dir>/_extra/<path> are added as new paths (a union that keeps a whole side as its own document). With --commit and no
open conflict: one merge commit (parents: base-branch tip, PR head; tree: base-branch tree + the merged paths) on
<new-branch>, author Hitesh Soliwal. Prints one row per path and the commit sha."""
import base64
import json
import subprocess
import sys
import tempfile
from pathlib import Path


def gh(args, body=None, raw=False):
    r = subprocess.run(["gh", "api", *args] + (["--input", "-"] if body is not None else []),
                       input=json.dumps(body).encode() if body is not None else None, capture_output=True, check=True)
    return r.stdout if raw else (json.loads(r.stdout) if r.stdout.strip() else {})


def blob(repo, sha):
    return base64.b64decode(gh([f"repos/{repo}/git/blobs/{sha}"])["content"])


TREES = {}


def mode_of(repo, commit, path):
    """File mode of path at commit, walking non-recursive trees (cached); 100644 when absent."""
    sha = gh([f"repos/{repo}/git/commits/{commit}"])["tree"]["sha"] if commit not in TREES else TREES[commit]
    TREES[commit] = sha
    parts = path.split("/")
    for i, part in enumerate(parts):
        if sha not in TREES:
            TREES[sha] = {e["path"]: e for e in gh([f"repos/{repo}/git/trees/{sha}"])["tree"]}
        entry = TREES[sha].get(part)
        if entry is None:
            return "100644"
        if i == len(parts) - 1:
            return entry["mode"]
        sha = entry["sha"]
    return "100644"


def tree_map(repo, commit, paths):
    out = {}
    for p in paths:
        r = subprocess.run(["gh", "api", f"repos/{repo}/contents/{p}?ref={commit}", "-q", ".sha"], capture_output=True,
                           text=True)
        out[p] = r.stdout.strip() if r.returncode == 0 and r.stdout.strip() else None
    return out


def main():
    repo, branch, head, work = sys.argv[1], sys.argv[2], sys.argv[3], Path(sys.argv[4])
    given_base = [a.split("=", 1)[1] for a in sys.argv[5:] if a.startswith("--base=")]
    given_pr = [a.split("=", 1)[1] for a in sys.argv[5:] if a.startswith("--pr=")]
    only = [a.split("=", 1)[1] for a in sys.argv[5:] if a.startswith("--only=")]
    commit_args = [a for a in sys.argv[5:] if not a.startswith(("--base=", "--pr=", "--only="))]
    tip = gh([f"repos/{repo}/git/ref/heads/{branch}"])["object"]["sha"]
    if given_base:
        # Replay a merge-dropped PR: its head is already an ancestor of the branch (ancestry-only fold), so the
        # merge base would be the head itself; the PR's own base gives the change to re-apply.
        base = given_base[0]
    else:
        base = gh([f"repos/{repo}/compare/{tip}...{head}"])["merge_base_commit"]["sha"]
    if given_pr:
        # The pull-request files listing pages up to 3,000 files (compare stops at 300). Its blob ids are the PR
        # head's; with --base it is the PR's own change set.
        r = subprocess.run(["gh", "api", "--paginate", f"repos/{repo}/pulls/{given_pr[0]}/files?per_page=100"],
                           capture_output=True, check=True)
        pr_files = [f for page in json.loads("[" + r.stdout.decode().replace("][", "],[") + "]") for f in page]
    else:
        pr_files = gh([f"repos/{repo}/compare/{base}...{head}"])["files"]
        if len(pr_files) >= 300:
            sys.exit("REFUSE: compare truncated at 300 files; pass --pr=<number>")
    if only:
        pr_files = [f for f in pr_files if f["filename"].startswith(tuple(only[0].split(",")))]
    paths = [f["filename"] for f in pr_files]
    ours, based = tree_map(repo, tip, paths), tree_map(repo, base, paths)
    entries, conflicts = [], 0
    for f in pr_files:
        p, status = f["filename"], f["status"]
        if status == "removed":
            sys.exit(f"REFUSE removed on the PR side: {p}")
        if status == "renamed":
            sys.exit(f"REFUSE rename on the PR side: {f.get('previous_filename')} -> {p}")
        mode = mode_of(repo, head, p)
        if ours[p] == f["sha"]:
            print(f"ALREADY\t{p}")
            continue
        if ours[p] == based[p]:
            entries.append({"path": p, "mode": mode, "type": "blob", "sha": f["sha"]})
            print(f"TAKE_THEIRS\t{p}")
            continue
        resolved = work / (p + ".resolved")
        if resolved.is_file():
            data = resolved.read_bytes()
            if b"<<<<<<<" in data or b">>>>>>>" in data:
                sys.exit(f"REFUSE conflict markers in {resolved}")
            entries.append({"path": p, "mode": mode, "type": "blob", "content_bytes": data})
            print(f"RESOLVED\t{p}")
            continue
        o = blob(repo, ours[p]) if ours[p] else b""
        b = blob(repo, based[p]) if based[p] else b""
        t = blob(repo, f["sha"])
        with tempfile.TemporaryDirectory() as d:
            fo, fb, ft = (Path(d) / x for x in "obt")
            fo.write_bytes(o); fb.write_bytes(b); ft.write_bytes(t)
            r = subprocess.run(["git", "merge-file", "-p", "--diff3", str(fo), str(fb), str(ft)], capture_output=True)
        if r.returncode == 0:
            entries.append({"path": p, "mode": mode, "type": "blob", "content_bytes": r.stdout})
            print(f"MERGED\t{p}")
        else:
            conflicts += 1
            target = work / p
            target.parent.mkdir(parents=True, exist_ok=True)
            for suffix, data in (("ours", o), ("base", b), ("theirs", t), ("merged", r.stdout)):
                Path(str(target) + "." + suffix).write_bytes(data)
            print(f"CONFLICT={r.returncode}\t{p}")
    extra = work / "_extra"
    for f in sorted(extra.rglob("*")) if extra.is_dir() else []:
        if f.is_file():
            rel = f.relative_to(extra).as_posix()
            if rel in paths:
                sys.exit(f"REFUSE extra file shadows a PR path: {rel}")
            entries.append({"path": rel, "mode": "100644", "type": "blob", "content_bytes": f.read_bytes()})
            print(f"EXTRA\t{rel}")
    print(f"tip={tip} base={base} head={head} paths={len(paths)} conflicts={conflicts}")
    if not commit_args or conflicts:
        return
    new_branch, msg_file = commit_args[1], commit_args[2]
    for e in entries:
        if "content_bytes" in e:
            data = e.pop("content_bytes")
            e["sha"] = gh(["-X", "POST", f"repos/{repo}/git/blobs"],
                          {"content": base64.b64encode(data).decode(), "encoding": "base64"})["sha"]
    tree = gh(["-X", "POST", f"repos/{repo}/git/trees"],
              {"base_tree": gh([f"repos/{repo}/git/commits/{tip}"])["tree"]["sha"], "tree": entries})["sha"]
    commit = gh(["-X", "POST", f"repos/{repo}/git/commits"],
                {"message": Path(msg_file).read_text(encoding="utf-8").strip(), "tree": tree, "parents": [tip, head],
                 "author": {"name": "Hitesh Soliwal", "email": "hsoliwal@gmail.com"}})["sha"]
    gh(["-X", "POST", f"repos/{repo}/git/refs"], {"ref": f"refs/heads/{new_branch}", "sha": commit})
    print(f"commit={commit}")


main()
