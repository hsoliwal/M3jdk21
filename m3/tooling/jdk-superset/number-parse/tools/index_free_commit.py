# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""index_free_commit.py <worktree> <message_file> <path>...

Commits the listed paths (relative to the worktree root, as they are on disk now) on top of HEAD without reading or
writing the index. For a blobless, sparse M3jdk worktree whose full index is tens of MB on a nearly full disk:
  blobs   git hash-object -w --path (the same clean filters as git add)
  trees   only the directories on the changed paths are rebuilt: git ls-tree -z <HEAD>:<dir>, entries replaced,
          git mktree -z --missing (siblings are blobs of a blobless clone and stay unverified, so nothing is
          fetched)
  commit  git commit-tree <tree> -p HEAD -F <message_file>
  ref     git update-ref refs/heads/<branch> <commit> <old HEAD>: refused if the branch moved meanwhile
Existing entries keep their mode; new files get 100644. The index keeps describing the old HEAD: run 'git reset -q'
once the disk has room (it rewrites the index from HEAD and touches no file). Prints the new commit id."""
import os
import subprocess
import sys


def git(cwd, *args, data=None):
    out = subprocess.run(("git",) + args, cwd=cwd, input=data, capture_output=True, check=True)
    return out.stdout


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
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
