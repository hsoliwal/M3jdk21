# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""custody_names_check.py <worktree> <record> <src_root> <TYPE_RENAME.tsv> [--head <rev>] [--record-at <rev>]

CUSTODY_NAMES v1, the content side of CUSTODY_RECORD v1 (custody_check.py proves that the record was revisited after
each change of an absorbed file; this check proves that it names the change). Every non-merge commit from the commit
that added the record up to head (both parents of a merge included) that changes an absorbed file names its recipe in
its subject as '(<RECIPE> v<n>)' (for example '(HEX_ROUNDING v1)'); the record must contain each such name. A merge
is not a change of its own: its first-parent diff repeats the merged commits, which this check reads directly (a
record change brought in by a merge says nothing about the commits of the merging branch).
  absorbed files  <src_root>/<new_fqn as a path>.java for every row of TYPE_RENAME.tsv
  record text     the worktree file (the commit step), or the record at --record-at <rev>
Reads commits and trees only (--no-renames, GIT_NO_LAZY_FETCH=1), except the record blob with --record-at.
Prints one row per commit that changes an absorbed file: commit, absorbed files changed, recipe names, status
(NAMED | UNNAMED | UNTAGGED: no recipe name in the subject, reported, not failed), then 'CUSTODY_NAMES PASS|FAIL'.
Exit status 0 for PASS (no UNNAMED row), 1 for FAIL."""
import os
import re
import subprocess
import sys

GIT_ENV = {"GIT_NO_LAZY_FETCH": "1", "GIT_OPTIONAL_LOCKS": "0"}
COMMIT_MARK = "@@"
SUBJECT_SEPARATOR = "\t"
LOG_ADDED = ("log", "--no-renames", "--diff-filter=A", "--format=%H")
LOG_RANGE = ("log", "--no-merges", "--no-renames", "--reverse", "--name-only",
             "--format=" + COMMIT_MARK + "%H" + SUBJECT_SEPARATOR + "%s")
SHOW = "show"
BLOB_AT = "%s:%s"
RANGE_FROM_PARENT = "%s^..%s"
PATHSPEC_START = "--"
HEAD_OPT = "--head"
RECORD_AT_OPT = "--record-at"
DEFAULT_HEAD = "HEAD"
RECIPE_NAME = re.compile(r"\(([A-Z][A-Z0-9_-]* v\d+(?:\.\d+)*)\)")
FQN_COLUMN = "new_fqn"
PACKAGE_SEPARATOR = "."
PATH_SEPARATOR = "/"
WINDOWS_SEPARATOR = "\\"
FIELD_SEPARATOR = "\t"
LINE_END = "\r\n"
JAVA_SUFFIX = ".java"
NAME_SEPARATOR = ","
NO_NAME = "-"
ENCODING = "utf-8"
NAMED = "NAMED"
UNNAMED = "UNNAMED"
UNTAGGED = "UNTAGGED"
PASS = "PASS"
FAIL = "FAIL"
MIN_ARGS = 5
ROW = "%s\t%d\t%s\t%s\n"
SUMMARY = "CUSTODY_NAMES\t%s\t%d absorbed-file commits\t%d unnamed\t%d untagged\n"


def git(cwd, *args, lazy=False):
    env = dict(os.environ, **({} if lazy else GIT_ENV))
    return subprocess.run(("git",) + args, cwd=cwd, env=env, capture_output=True, check=True).stdout.decode(ENCODING)


def normalized(path):
    return path.replace(WINDOWS_SEPARATOR, PATH_SEPARATOR).strip(PATH_SEPARATOR)


def absorbed_files(ledger, src_root):
    with open(ledger, encoding=ENCODING) as f:
        rows = [line.rstrip(LINE_END).split(FIELD_SEPARATOR) for line in f if line.strip()]
    col = rows[0].index(FQN_COLUMN)
    root = normalized(src_root)
    return {root + PATH_SEPARATOR + r[col].replace(PACKAGE_SEPARATOR, PATH_SEPARATOR) + JAVA_SUFFIX for r in rows[1:]}


def commits_changing(cwd, head, record, files):
    """[(commit, subject, absorbed paths changed)], oldest first, non-merge, from the commit that added the record."""
    added = git(cwd, *LOG_ADDED, head, PATHSPEC_START, record).split()
    if not added:
        return []
    out = git(cwd, *LOG_RANGE, RANGE_FROM_PARENT % (added[-1], head), PATHSPEC_START, *sorted(files))
    commits = []
    for line in out.splitlines():
        if line.startswith(COMMIT_MARK):
            commit, _, subject = line[len(COMMIT_MARK):].partition(SUBJECT_SEPARATOR)
            commits.append((commit, subject, set()))
        elif line and commits:
            commits[-1][2].add(line)
    return [c for c in commits if c[2] & files]


def main(argv):
    if len(argv) < MIN_ARGS:
        raise SystemExit(__doc__)
    cwd, record, src_root, ledger = argv[1], normalized(argv[2]), argv[3], argv[4]
    head, record_at, rest = DEFAULT_HEAD, None, list(argv[5:])
    while rest:
        opt = rest.pop(0)
        if opt == HEAD_OPT and rest:
            head = rest.pop(0)
        elif opt == RECORD_AT_OPT and rest:
            record_at = rest.pop(0)
        else:
            raise SystemExit(__doc__)
    if record_at:
        text = git(cwd, SHOW, BLOB_AT % (record_at, record), lazy=True)
    else:
        with open(os.path.join(cwd, record), encoding=ENCODING) as f:
            text = f.read()
    files = absorbed_files(ledger, src_root)
    unnamed = untagged = 0
    commits = commits_changing(cwd, head, record, files)
    for commit, subject, paths in commits:
        names = RECIPE_NAME.findall(subject)
        missing = [n for n in names if n not in text]
        status = UNTAGGED if not names else UNNAMED if missing else NAMED
        unnamed += status == UNNAMED
        untagged += status == UNTAGGED
        sys.stdout.write(ROW % (commit, len(paths & files), NAME_SEPARATOR.join(names) or NO_NAME, status))
    verdict = PASS if commits and not unnamed else FAIL
    sys.stdout.write(SUMMARY % (verdict, len(commits), unnamed, untagged))
    return 0 if verdict == PASS else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv))
