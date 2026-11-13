# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""anchor_check.py <worktree> <base> <head> <path>... [--worktree]

CNPCDA v1 (gate ANCHOR_APPEND_ONLY): //FLLDCIM anchor comments are append-only across passes. For every first-parent
commit in <base>..<head> (and, with --worktree, the uncommitted change of the worktree against <head>), each anchor
line removed from a file must come back in the same file: in the same change (an indentation change or a move inside
the file) the change is KEPT; in a later change of the range it is RESTORED_LATER (published history is never
rewritten, so a removal is repaired forward); otherwise it is REMOVED, also when its declaration moved to another
file. Anchors are compared as their stripped line text, as a multiset per (change, file).
Reads commits, trees and the blobs of the given paths only (git log -p -U0 --first-parent; a merge shows its
first-parent diff). Prints one row per (change, file) that removes or adds an anchor:
  change file removed added status (KEPT | RESTORED_LATER | REMOVED) [the anchors removed by the change]
then 'ANCHORS PASS|FAIL <changes> <removed at the end>'. Exit status 0 for PASS (no REMOVED row), 1 for FAIL."""
import collections
import os
import subprocess
import sys

ANCHOR = "//FLLDCIM:"
COMMIT_MARK = "@@@"
FILE_HEADER = "diff --git "
FILE_B_PREFIX = " b/"
REMOVED_LINE = "-"
ADDED_LINE = "+"
OLD_FILE_LINE = "--- "
NEW_FILE_LINE = "+++ "
WORKTREE_OPT = "--worktree"
WORKTREE = "WORKTREE"
RANGE = "%s..%s"
GIT_ENV = {"GIT_OPTIONAL_LOCKS": "0"}
LOG = ("log", "--first-parent", "--reverse", "--no-renames", "-p", "-U0", "--format=" + COMMIT_MARK + "%H")
DIFF = ("diff", "--no-renames", "-U0")
PATHSPEC_START = "--"
ENCODING = "utf-8"
KEPT = "KEPT"
RESTORED_LATER = "RESTORED_LATER"
REMOVED = "REMOVED"
PASS = "PASS"
FAIL = "FAIL"
ROW = "%s\t%s\t%d\t%d\t%s\t%s\n"
SUMMARY = "ANCHORS\t%s\t%d changes\t%d removed\n"
LIST_SEPARATOR = " | "
MIN_ARGS = 4


def git(cwd, *args):
    out = subprocess.run(("git",) + args, cwd=cwd, env=dict(os.environ, **GIT_ENV), capture_output=True, check=True)
    return out.stdout.decode(ENCODING)


def anchors_by_change(patch, first_change):
    """{(change, file): (removed Counter, added Counter)} from a unified diff text with COMMIT_MARK lines."""
    result = collections.OrderedDict()
    change, path = first_change, None
    for line in patch.splitlines():
        if line.startswith(COMMIT_MARK):
            change, path = line[len(COMMIT_MARK):], None
        elif line.startswith(FILE_HEADER):
            path = line[line.rindex(FILE_B_PREFIX) + len(FILE_B_PREFIX):]
        elif line.startswith(OLD_FILE_LINE) or line.startswith(NEW_FILE_LINE) or ANCHOR not in line:
            continue
        elif line.startswith(REMOVED_LINE) or line.startswith(ADDED_LINE):
            removed, added = result.setdefault((change, path), (collections.Counter(), collections.Counter()))
            (removed if line.startswith(REMOVED_LINE) else added)[line[1:].strip()] += 1
    return result


def main(argv):
    args = [a for a in argv[1:] if a != WORKTREE_OPT]
    if len(args) < MIN_ARGS:
        raise SystemExit(__doc__)
    cwd, base, head, paths = args[0], args[1], args[2], args[3:]
    changes = anchors_by_change(git(cwd, *LOG, RANGE % (base, head), PATHSPEC_START, *paths), head)
    if WORKTREE_OPT in argv:
        changes.update(anchors_by_change(git(cwd, *DIFF, head, PATHSPEC_START, *paths), WORKTREE))
    rows = [(change, path, removed, added, removed - added) for (change, path), (removed, added) in changes.items()]
    for i, (_, path, _, _, lost) in enumerate(rows):
        for _, later_path, removed, added, _ in rows[i + 1:]:
            if later_path == path:
                lost.subtract(added - removed)
        lost += collections.Counter()
    removed_total = 0
    for change, path, removed, added, still_lost in rows:
        lost_here = removed - added
        status = REMOVED if still_lost else RESTORED_LATER if lost_here else KEPT
        removed_total += sum(still_lost.values())
        sys.stdout.write(ROW % (change, path, sum(removed.values()), sum(added.values()), status,
                                LIST_SEPARATOR.join(sorted(lost_here))))
    verdict = FAIL if removed_total else PASS
    sys.stdout.write(SUMMARY % (verdict, len(changes), removed_total))
    return 0 if verdict == PASS else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv))
