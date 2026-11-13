# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""index_free_commit_check.py <new_dir>

Behaviour contract of tools/index_free_commit.py (INDEX_FREE_COMMIT v1.1), checked in a scratch repository made
under <new_dir> (absent or empty):
  ref     the branch points at the printed commit, whose parent is the old HEAD
  tree    the commit changes exactly the listed paths, with their bytes on disk; an existing entry keeps its mode
          (100755 here), a new file gets 100644
  index   the index still describes the old HEAD, and stderr reports INDEX_STALE with that many entries
  hazard  a porcelain commit made from that index reverts the committed paths (the reason for INDEX_STALE)
  sync    git reset -q rewrites the index from HEAD and keeps the files: diff-index --cached --quiet HEAD exits 0
  quiet   a path already staged with the bytes being committed: the commit is made and nothing is reported
Prints one row per check (name, PASS or FAIL, observed), then CHECKS <passed>/<total>. Exit status 0 only if every
check passes."""
import os
import subprocess
import sys

TOOL = os.path.join(os.path.dirname(os.path.abspath(__file__)), "index_free_commit.py")
REPO = "repo"
BRANCH = "main"
BRANCH_REF = "refs/heads/" + BRANCH
CHECK_ID = "m3-check"
CHECK_EMAIL = "m3-check@example.invalid"
CHECK_DATE = "2026-10-31T00:00:00Z"
IDENTITY = {"GIT_AUTHOR_NAME": CHECK_ID, "GIT_AUTHOR_EMAIL": CHECK_EMAIL, "GIT_AUTHOR_DATE": CHECK_DATE,
            "GIT_COMMITTER_NAME": CHECK_ID, "GIT_COMMITTER_EMAIL": CHECK_EMAIL, "GIT_COMMITTER_DATE": CHECK_DATE}
LOCAL_CONFIG = (("core.autocrlf", "false"), ("core.filemode", "false"), ("commit.gpgsign", "false"),
                ("maintenance.auto", "false"), ("gc.auto", "0"))
BASE_FILES = {"a.txt": b"one\n", "d/b.txt": b"two\n", "x.sh": b"#!/bin/sh\n"}
EXECUTABLE = "x.sh"
EDITS = {"a.txt": b"one\nuno\n", "x.sh": b"#!/bin/sh\necho\n", "d/e/n.txt": b"new\n"}
SECOND_EDIT = {"a.txt": b"one\nuno\ndos\n"}
BASE_MESSAGE = "c0"
HAZARD_MESSAGE = "porcelain commit from the stale index"
MESSAGE = b"m1: index-free commit\n\nbody\n"
MESSAGE_FILE = "message.txt"
EXPECT_TREE = ["A\td/e/n.txt", "M\ta.txt", "M\tx.sh"]
EXPECT_STALE = ["D\td/e/n.txt", "M\ta.txt", "M\tx.sh"]
EXPECT_MODES = {"a.txt": "100644", "d/e/n.txt": "100644", "x.sh": "100755"}
STALE_PREFIX = "INDEX_STALE\t%d\t"
TREE_OF = "%s^{tree}"
PATH_OF = "%s:%s"
PATH_SEPARATOR = "/"
FIELD_SEPARATOR = "\t"
LIST_SEPARATOR = ","
SPACE = " "
ENCODING = "utf-8"
SHORT = 12
NOT_EMPTY = "not empty: %s"
PASS = "PASS"
FAIL = "FAIL"
ROW = "%s\t%s\t%s\n"
SUMMARY = "CHECKS\t%d/%d\n"
OBSERVED_REF = "HEAD=%s parent=%s"
OBSERVED_TREE = "%s; modes %s; bytes as on disk: %s"
OBSERVED_INDEX = "index vs HEAD %s; stderr %r"
OBSERVED_HAZARD = "%s; tree = old HEAD tree: %s"
OBSERVED_SYNC = "diff-index --cached --quiet HEAD exit %d; files kept: %s"
OBSERVED_QUIET = "parent ok: %s; stderr %r; diff-index --cached --quiet HEAD exit %d"


def run(cwd, *args, check=True):
    return subprocess.run(args, cwd=cwd, env=dict(os.environ, **IDENTITY), capture_output=True, check=check)


def git(cwd, *args, check=True):
    return run(cwd, "git", *args, check=check)


def text(cwd, *args):
    return git(cwd, *args).stdout.decode(ENCODING).strip()


def lines(cwd, *args):
    return sorted(line for line in text(cwd, *args).splitlines() if line)


def shown(entries):
    """name-status entries for one TSV cell: 'M a.txt,D d/n.txt'."""
    return LIST_SEPARATOR.join(e.replace(FIELD_SEPARATOR, SPACE) for e in entries)


def disk_path(root, rel):
    return os.path.join(root, *rel.split(PATH_SEPARATOR))


def write(root, files):
    for rel, data in files.items():
        path = disk_path(root, rel)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "wb") as f:
            f.write(data)


def read(root, rel):
    with open(disk_path(root, rel), "rb") as f:
        return f.read()


def modes(repo, commit):
    found = {}
    for rec in text(repo, "ls-tree", "-r", commit).splitlines():
        meta, name = rec.split(FIELD_SEPARATOR, 1)
        found[name] = meta.split()[0]
    return {name: found.get(name) for name in EXPECT_MODES}


def index_free_commit(repo, message, paths):
    """(commit id printed by the tool, its stderr)."""
    done = run(repo, sys.executable, TOOL, repo, message, *paths)
    return done.stdout.decode(ENCODING).strip(), done.stderr.decode(ENCODING)


def quiet_diff(repo):
    return git(repo, "diff-index", "--cached", "--quiet", "HEAD", check=False).returncode


def setup(base):
    repo = os.path.join(base, REPO)
    os.makedirs(repo)
    git(repo, "init", "-q", "-b", BRANCH)
    for key, value in LOCAL_CONFIG:
        git(repo, "config", key, value)
    write(repo, BASE_FILES)
    git(repo, "add", *BASE_FILES)
    git(repo, "update-index", "--chmod=+x", EXECUTABLE)
    git(repo, "commit", "-q", "-m", BASE_MESSAGE)
    message = os.path.join(base, MESSAGE_FILE)
    with open(message, "wb") as f:
        f.write(MESSAGE)
    return repo, message


def main(argv):
    if len(argv) != 2:
        raise SystemExit(__doc__)
    base = argv[1]
    if os.path.isdir(base) and os.listdir(base):
        raise SystemExit(NOT_EMPTY % base)
    repo, message = setup(base)
    results = []

    def check(name, ok, observed):
        results.append(ok)
        sys.stdout.write(ROW % (name, PASS if ok else FAIL, observed))

    c0 = text(repo, "rev-parse", "HEAD")
    write(repo, EDITS)
    c1, stderr = index_free_commit(repo, message, EDITS)
    head, parent = text(repo, "rev-parse", "HEAD"), text(repo, "rev-parse", "HEAD^")
    check("ref", head == c1 and parent == c0, OBSERVED_REF % (head[:SHORT], parent[:SHORT]))
    tree, got_modes = lines(repo, "diff-tree", "-r", "--no-commit-id", "--name-status", c0, c1), modes(repo, c1)
    same_bytes = all(git(repo, "cat-file", "-p", PATH_OF % (c1, rel)).stdout == data for rel, data in EDITS.items())
    check("tree", tree == EXPECT_TREE and got_modes == EXPECT_MODES and same_bytes,
          OBSERVED_TREE % (shown(tree), got_modes, same_bytes))
    stale = lines(repo, "diff-index", "--cached", "--name-status", "HEAD")
    check("index", stale == EXPECT_STALE and stderr.startswith(STALE_PREFIX % len(EXPECT_STALE)),
          OBSERVED_INDEX % (shown(stale), stderr[:len(STALE_PREFIX) + SHORT]))
    git(repo, "commit", "-q", "-m", HAZARD_MESSAGE)
    hazard = text(repo, "rev-parse", "HEAD")
    reverted = lines(repo, "diff-tree", "-r", "--no-commit-id", "--name-status", c1, hazard)
    old_tree = text(repo, "rev-parse", TREE_OF % hazard) == text(repo, "rev-parse", TREE_OF % c0)
    check("hazard", reverted == EXPECT_STALE and old_tree, OBSERVED_HAZARD % (shown(reverted), old_tree))
    git(repo, "update-ref", BRANCH_REF, c1, hazard)
    git(repo, "reset", "-q")
    synced, kept = quiet_diff(repo), all(read(repo, rel) == data for rel, data in EDITS.items())
    check("sync", synced == 0 and kept, OBSERVED_SYNC % (synced, kept))
    write(repo, SECOND_EDIT)
    git(repo, "add", *SECOND_EDIT)
    c3, stderr = index_free_commit(repo, message, SECOND_EDIT)
    on_c1 = text(repo, "rev-parse", "HEAD") == c3 and text(repo, "rev-parse", "HEAD^") == c1
    clean = quiet_diff(repo)
    check("quiet", on_c1 and not stderr and clean == 0, OBSERVED_QUIET % (on_c1, stderr, clean))
    sys.stdout.write(SUMMARY % (sum(results), len(results)))
    return 0 if all(results) else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv))
