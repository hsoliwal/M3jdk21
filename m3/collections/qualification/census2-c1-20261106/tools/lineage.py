#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Owner line lineage over master history: for each census line (missing = postimage-only, extra = owner-only), the
commits of the owner file where it was present, the commit that removed it (missing) or introduced it (extra).
Lines compare after strip(); the owner history follows the pre- and post-rename paths (JDK naming fe35ea98841).
usage: lineage.py REPO REV LINES_FILE KIND(missing|extra) PATH [PATH...]  -> TSV on stdout"""
import subprocess
import sys

repo, rev, lines_file, kind, paths = sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4], sys.argv[5:]


def git(*args):
    return subprocess.run(['git', '-C', repo, *args], check=True, capture_output=True).stdout


log = git('log', '--format=%H %ad %s', '--date=short', '--reverse', rev, '--', *paths).decode('utf-8', 'replace').splitlines()
versions = []
for entry in log:
    sha, date, subject = entry.split(' ', 2)
    text = None
    for p in paths:
        try:
            text = git('show', '%s:%s' % (sha, p)).decode('utf-8', 'replace')
            break
        except subprocess.CalledProcessError:
            continue
    present = set(l.strip() for l in text.splitlines()) if text is not None else set()
    versions.append((sha[:11], date, subject[:90].rstrip(), present, text is not None))
wanted = [l.strip() for l in open(lines_file, encoding='utf-8').read().splitlines() if l.strip()]
print('kind\tline\tever_present\tlast_present\tchange_commit\tchange_date\tchange_subject')
for line in wanted:
    hits = [i for i, v in enumerate(versions) if line in v[3]]
    if kind == 'missing':
        if not hits:
            # never on master: report the commit that introduced the first owner version (shape authored elsewhere)
            print('\t'.join([kind, line, 'NO', '-', '-', '-', 'never on master history of the owner']))
            continue
        last = hits[-1]
        nxt = next((i for i in range(last + 1, len(versions))), None)
        ch = versions[nxt] if nxt is not None else ('-', '-', 'still present at head')
        print('\t'.join([kind, line, 'YES', versions[last][0], ch[0], ch[1], ch[2]]))
    else:
        if not hits:
            print('\t'.join([kind, line, 'NO', '-', '-', '-', 'not in owner history']))
            continue
        first = hits[0]
        # introduced = first version of a contiguous run reaching the head
        run_start = first
        for i in range(len(versions) - 1, -1, -1):
            if line not in versions[i][3]:
                run_start = i + 1
                break
        else:
            run_start = 0
        v = versions[run_start] if run_start < len(versions) else versions[first]
        print('\t'.join([kind, line, 'YES', versions[-1][0], v[0], v[1], v[2]]))
