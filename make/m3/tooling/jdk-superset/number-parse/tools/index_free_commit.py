# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""index_free_commit.py <worktree> <message_file> <path>...     (INDEX_FREE_COMMIT v1.1)

Commits the listed paths (relative to the worktree root, as they are on disk now) on top of HEAD without reading or
writing the index. For a blobless, sparse M3jdk worktree whose full index is tens of MB on a nearly full disk:
  blobs   git hash-object -w --path (the same clean filters as git add)
  trees   only the directories on the changed paths are rebuilt: git ls-tree -z <HEAD>:<dir>, entries replaced,
          git mktree -z --missing (siblings are blobs of a blobless clone and stay unverified, so nothing is
          fetched)
  commit  git commit-tree <tree> -p HEAD -F <message_file>
  ref     git update-ref refs/heads/<branch> <commit> <old HEAD>: refused if the branch moved meanwhile
Existing entries keep their mode; new files get 100644. Prints the new commit id.

The index keeps describing the old HEAD, and that is a commit hazard: a porcelain commit in that worktree
(git add --sparse <paths>; git commit) builds its tree from the index and silently reverts what was committed here.
So after each commit stderr gets 'INDEX_STALE <n>', n = the index entries that differ from the new HEAD (git
diff-index --cached compares object ids only, nothing is fetched). Until 'git reset -q' has rewritten the index from
HEAD (it touches no file, but needs the index size free on the disk of the git dir, above the disk floor) and
'git diff-index --cached --quiet HEAD' exits 0, commit in that worktree only through this tool and keep the open sync
as a HOLD row in the lane's GATES.tsv. tools/index_free_commit_check.py checks this contract in a scratch repo."""
import os
import subprocess
import sys

STALE_ENV = {"GIT_OPTIONAL_LOCKS": "0", "GIT_NO_LAZY_FETCH": "1"}
STALE_DIFF = ("diff-index", "--cached", "--name-only", "-z")
NUL = b"\0"
STALE_UNKNOWN = "?"
STALE_REPORT = ("INDEX_STALE\t%s\tindex entries differ from %s: a porcelain commit in this worktree would revert "
                "them; commit through tools/index_free_commit.py until 'git reset -q' and "
                "'git diff-index --cached --quiet HEAD' pass\n")


def git(cwd, *args, data=None, env=None):
    out = subprocess.run(("git",) + args, cwd=cwd, input=data, env=env, capture_output=True, check=True)
    return out.stdout


def stale_count(cwd, head):
    """How many index entries differ from the tree of head ('?' when git cannot tell). Object ids only: no blob is
    read, so a blobless clone fetches nothing."""
    try:
        raw = git(cwd, *STALE_DIFF, head, env=dict(os.environ, **STALE_ENV))
    except subprocess.CalledProcessError:
        return STALE_UNKNOWN
    return sum(1 for p in raw.split(NUL) if p)


def tree_entries(cwd, head, path):
    """{name: (mode, type, sha)} of the HEAD tree at path ('' = root); empty for a new directory."""
    spec = "%s:%s" % (head, path)
    try:
        raw = git(cwd, "ls-tree", "-z", spec)
    except subprocess.CalledProcessError:
        return {}
    entries = {}
    for rec in raw.decode("utf-8").split("\0"):
        if rec:
            meta, name = rec.split("\t", 1)
            mode, typ, sha = meta.split(" ")
            entries[name] = (mode, typ, sha)
    return entries


def main(argv):
    cwd, message, paths = argv[1], argv[2], argv[3:]
    head = git(cwd, "rev-parse", "HEAD").decode().strip()
    branch = git(cwd, "symbolic-ref", "HEAD").decode().strip()
    changed = {}  # dir -> {name: (mode, type, sha)}
    for p in paths:
        p = p.replace("\\", "/").strip("/")
        d, _, name = p.rpartition("/")
        old = tree_entries(cwd, head, d).get(name)
        sha = git(cwd, "hash-object", "-w", "--path", p, p).decode().strip()
        changed.setdefault(d, {})[name] = (old[0] if old else "100644", "blob", sha)
        while d:
            parent = d.rpartition("/")[0]
            changed.setdefault(parent, {})
            d = parent
    for d in sorted(changed, key=lambda x: (-x.count("/") if x else 1, x)):
        entries = tree_entries(cwd, head, d)
        entries.update(changed[d])
        body = "".join("%s %s %s\t%s\0" % (m, t, s, n) for n, (m, t, s) in sorted(entries.items()))
        sha = git(cwd, "mktree", "-z", "--missing", data=body.encode("utf-8")).decode().strip()
        if d:
            parent, _, name = d.rpartition("/")
            changed[parent][name] = ("040000", "tree", sha)
        else:
            root = sha
    commit = git(cwd, "commit-tree", root, "-p", head, "-F", os.path.abspath(message)).decode().strip()
    subject = open(message, encoding="utf-8").readline().strip()
    git(cwd, "update-ref", "-m", "commit (index-free): " + subject, branch, commit, head)
    sys.stdout.write(commit + "\n")
    stale = stale_count(cwd, commit)
    if stale:
        sys.stderr.write(STALE_REPORT % (stale, commit))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
