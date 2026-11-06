# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""custody_check.py <worktree> <record> <src_root> <TYPE_RENAME.tsv> [--head <rev>] [--pending <path>...]

CUSTODY_RECORD v1. The custody record of an absorbed donor (its legal file, for example
src/java.base/share/legal/fastdoubleparser.md) names every change made to the absorbed files. A commit that changes
an absorbed file is COVERED when the same commit changes the record, COVERED_LATER when a later commit up to head
does, and UNCOVERED otherwise; one UNCOVERED commit fails the check. The record says what changed; this check proves
only that the record was revisited after each change.
  absorbed files  <src_root>/<new_fqn as a path>.java for every row of TYPE_RENAME.tsv
  range           first-parent history from the commit that added the record up to head (default HEAD)
  --pending       the paths of a commit not made yet, checked as if it sat on top of head (the commit step)
Reads commits and trees only (--no-renames, GIT_NO_LAZY_FETCH=1), so a blobless clone fetches nothing.
Prints one row per commit that touches either side (commit, absorbed files changed, record changed, status), then
'CUSTODY PASS|FAIL'. Exit status 0 for PASS, 1 for FAIL."""
import os
import subprocess
import sys

GIT_ENV = {"GIT_NO_LAZY_FETCH": "1", "GIT_OPTIONAL_LOCKS": "0"}
COMMIT_MARK = "@@"
LOG_ADDED = ("log", "--no-renames", "--diff-filter=A", "--format=%H")
LOG_RANGE = ("log", "--first-parent", "--no-renames", "--reverse", "--name-only", "--format=" + COMMIT_MARK + "%H")
RANGE_FROM_PARENT = "%s^..%s"
PATHSPEC_START = "--"
HEAD_OPT = "--head"
PENDING_OPT = "--pending"
DEFAULT_HEAD = "HEAD"
PENDING = "PENDING"
FQN_COLUMN = "new_fqn"
PACKAGE_SEPARATOR = "."
PATH_SEPARATOR = "/"
WINDOWS_SEPARATOR = "\\"
FIELD_SEPARATOR = "\t"
LINE_END = "\r\n"
ENCODING = "utf-8"
JAVA_SUFFIX = ".java"
COVERED = "COVERED"
COVERED_LATER = "COVERED_LATER"
UNCOVERED = "UNCOVERED"
RECORD_ONLY = "RECORD"
PASS = "PASS"
FAIL = "FAIL"
NO_RECORD = -1
ROW = "%s\t%d\t%d\t%s\n"
SUMMARY = "CUSTODY\t%s\t%d absorbed-file commits\t%d uncovered\n"


def git(cwd, *args):
    out = subprocess.run(("git",) + args, cwd=cwd, env=dict(os.environ, **GIT_ENV), capture_output=True, check=True)
    return out.stdout.decode(ENCODING)


def normalized(path):
    return path.replace(WINDOWS_SEPARATOR, PATH_SEPARATOR).strip(PATH_SEPARATOR)


def absorbed_files(ledger, src_root):
    """The absorbed source paths named by the rename ledger's new_fqn column."""
    with open(ledger, encoding=ENCODING) as f:
        rows = [line.rstrip(LINE_END).split(FIELD_SEPARATOR) for line in f if line.strip()]
    col = rows[0].index(FQN_COLUMN)
    root = normalized(src_root)
    return {root + PATH_SEPARATOR + r[col].replace(PACKAGE_SEPARATOR, PATH_SEPARATOR) + JAVA_SUFFIX
            for r in rows[1:]}


def history(cwd, head, record, files):
    """[(commit, changed paths among record and files)], oldest first, from the commit that added the record."""
    added = git(cwd, *LOG_ADDED, head, PATHSPEC_START, record).split()
    if not added:
        return []
    out = git(cwd, *LOG_RANGE, RANGE_FROM_PARENT % (added[-1], head), PATHSPEC_START, record, *sorted(files))
    commits = []
    for line in out.splitlines():
        if line.startswith(COMMIT_MARK):
            commits.append((line[len(COMMIT_MARK):], set()))
        elif line and commits:
            commits[-1][1].add(line)
    return commits


def coverage(commits, record, files):
    """(commit, absorbed files changed, record changed, status) for each commit that touches either side."""
    touched = [(c, len(paths & files), record in paths) for c, paths in commits if paths & (files | {record})]
    last_record = max((i for i, (_, _, r) in enumerate(touched) if r), default=NO_RECORD)
    rows = []
    for i, (c, n, r) in enumerate(touched):
        if not n:
            status = RECORD_ONLY
        elif r:
            status = COVERED
        else:
            status = COVERED_LATER if i < last_record else UNCOVERED
        rows.append((c, n, r, status))
    return rows


def main(argv):
    if len(argv) < 5:
        raise SystemExit(__doc__)
    cwd, record, src_root, ledger = argv[1], normalized(argv[2]), argv[3], argv[4]
    head, pending, rest = DEFAULT_HEAD, None, list(argv[5:])
    while rest:
        opt = rest.pop(0)
        if opt == HEAD_OPT and rest:
            head = rest.pop(0)
        elif opt == PENDING_OPT:
            pending, rest = {normalized(p) for p in rest}, []
        else:
            raise SystemExit(__doc__)
    files = absorbed_files(ledger, src_root)
    commits = history(cwd, head, record, files)
    if commits and pending is not None:
        commits.append((PENDING, pending))
    rows = coverage(commits, record, files)
    for c, n, r, status in rows:
        sys.stdout.write(ROW % (c, n, r, status))
    uncovered = sum(1 for row in rows if row[3] == UNCOVERED)
    verdict = PASS if commits and not uncovered else FAIL
    sys.stdout.write(SUMMARY % (verdict, sum(1 for row in rows if row[1]), uncovered))
    return 0 if verdict == PASS else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv))
