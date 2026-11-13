#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Generator of ADD_ROWS.tsv (RESTORE_ADD_ROWS v2) for qualification/rank-stack-restore-20261107.

For each MEMBERS target of THREE_WAY.tsv (step 1, use=OURS: the 3-way conflicts) the generator parses the top-level
members of ours (master) and theirs (the retained variant), keys them name#k (k-th occurrence, overloads), and emits
every maximal run of theirs-only members as three rows: SEPARATOR (the blank theirs line after the preceding shared
member), ANCHOR (one CNPCDA line naming the slice, its origin commits and the dropping merge) and MEMBERS (the theirs
slice, byte-for-byte, sha256 prefix). The run goes after the end line of the same preceding shared member in ours; a
run that starts with a method skips back over shared fields to the preceding shared method (so methods never land in a
field block); a run before every shared member goes after the last #include. Origins come from git blame of the
retained variant's pre-rename head (THREE_WAY.tsv head column), mapped to the audit leaves of LEAF_ORIGINS.tsv.
HUNK rows (single statements, not members) are declared in tools/HUNKS.tsv and copied through.
CPU only. usage: python -I gen_rows.py [--check] [--repo <git dir>]
"""
import argparse
import hashlib
import re
import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent.parent
RECIPE = 'make/m3/collections/qualification/rank-stack-restore-20261107/add_rows.py v2'
JAVA_MEMBER = re.compile(r'^    (?:@\w+\s+)*(?:(?:public|private|protected|static|final|native|synchronized|abstract'
                         r'|volatile|transient)\s+)*(?:<[^>]+>\s+)?([\w.<>\[\], ?]+?)\s+(\w+)\s*(\(|=|;)')
JAVA_TYPE = re.compile(r'^    (?:(?:public|private|protected|static|final|abstract|sealed)\s+)*'
                       r'(class|interface|enum|record)\s+(\w+)')
C_FUNC = re.compile(r'^(\w+)\s*\(')
C_DECL = re.compile(r'^(?:static\s+|JNIEXPORT\s+|inline\s+)*[\w\s*]+?\b(\w+)\s*\(')
C_ENUM = re.compile(r'^(?:static\s+const\s+\w+\s+|enum\s*\{\s*)(\w+)')
STRIP = re.compile(r'"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|//.*')


def git(repo, *args):
    return subprocess.run(['git', '-C', str(repo), *args], check=True, capture_output=True).stdout


def table(path):
    rows = [line.split('\t') for line in path.read_text(encoding='utf-8').splitlines()
            if line and not line.startswith('#')]
    return [dict(zip(rows[0], row)) for row in rows[1:]]


def span_end(lines, i, java):
    depth, started, j = 0, False, i
    while j < len(lines):
        s = STRIP.sub('', lines[j])
        depth += s.count('{') - s.count('}')
        started = started or '{' in s
        if (started and depth <= 0) or (not started and s.rstrip().endswith(';') and (depth == 0 or not java)):
            return j
        j += 1
    return j


def members(lines, java):
    """(key, kind, first, last) 1-based, keys name#k."""
    out, seen, i = [], {}, 0
    while i < len(lines):
        line, name, kind = lines[i], None, None
        if java:
            t = JAVA_TYPE.match(line)
            m = None if t else JAVA_MEMBER.match(line)
            if t:
                name, kind = t.group(2), t.group(1)
            elif m and not line.strip().startswith(('return', 'throw', '//', '*', '/*')):
                name, kind = m.group(2), 'method' if m.group(3) == '(' else 'field'
        elif line and not line[0].isspace() and not line.startswith(('#', '/', '*', '}', '{')):
            e = C_ENUM.match(line)
            f = None if e else (C_FUNC.match(line) or C_DECL.match(line))
            if e:
                name, kind = e.group(1), 'const'
            elif f:
                name, kind = f.group(1), 'function'
            elif i + 1 < len(lines) and C_FUNC.match(lines[i + 1]):
                i += 1
                name, kind = C_FUNC.match(lines[i]).group(1), 'function'
        if name:
            j = span_end(lines, i, java)
            k = seen.get(name, 0)
            seen[name] = k + 1
            out.append((f'{name}#{k}', kind, i + 1, j + 1))
            i = j + 1
            continue
        i += 1
    return out


def blame(repo, head, path):
    raw = git(repo, 'blame', '-s', '-l', '--line-porcelain', head, '--', path).decode('utf-8', 'replace')
    origins, commit = [], None
    for line in raw.splitlines():
        if re.match(r'^[0-9a-f]{40} ', line):
            commit = line[:11]
        elif line.startswith('\t'):
            origins.append(commit)
    return origins


def sha16(lines):
    return hashlib.sha256(''.join(lines).encode()).hexdigest()[:16]


def runs_for(target, ours_text, theirs_text, origins, leaves, indent):
    java = target['path'].endswith('.java')
    ours_lines = ours_text.splitlines(keepends=True)
    theirs_lines = theirs_text.splitlines(keepends=True)
    om = {k: (kind, a, b) for k, kind, a, b in members([l.rstrip('\n') for l in ours_lines], java)}
    tm = members([l.rstrip('\n') for l in theirs_lines], java)
    last_include_t = max(i for i, l in enumerate(theirs_lines, 1) if l.startswith('#include')) if not java else 0
    last_include_o = max(i for i, l in enumerate(ours_lines, 1) if l.startswith('#include')) if not java else 0
    rows, run = [], []

    def flush(prev_shared, prev_method):
        if not run:
            return
        first_kind = run[0][1]
        anchor = prev_method if (java and first_kind not in ('field',) and prev_shared and prev_shared[1] == 'field') \
            else prev_shared
        t_from = (anchor[3] + 1) if anchor and anchor is prev_shared else None
        if anchor is None:
            t_from, o_after = last_include_t + 1, last_include_o
        else:
            o_after = om[anchor[0]][2]
            if t_from is None:   # skipped back over shared fields: slice starts after the last shared field
                t_from = prev_shared[3] + 1
        t_to = run[-1][3]
        sep = theirs_lines[t_from - 1].strip() == ''
        body_from = t_from + 1 if sep else t_from
        commits = []
        for c in origins[body_from - 1:t_to]:
            if c not in commits:
                commits.append(c)
        leaf_ids = sorted({leaves[c] for c in commits if c in leaves}, key=lambda x: [int(n) for n in x.split('+')])
        symbols = ' '.join(k.split('#')[0] for k, *_ in run)
        rows.append({'kind': 'SEPARATOR', 'after': o_after, 'a': t_from, 'b': t_from} if sep else None)
        rows.append({'kind': 'ANCHOR', 'after': o_after, 'text': (
            f"{indent}//FLLDCIM: MRL - RESTORE-ADD {target['target']} theirs {target['theirs'][:12]} lines "
            f"{body_from}-{t_to} (leaves {' '.join(leaf_ids) or '-'}; origins {' '.join(commits)}); dropped by "
            f"retain merge {target['merge'][:12]}; recipe {RECIPE}")})
        rows.append({'kind': 'MEMBERS', 'after': o_after, 'a': body_from, 'b': t_to, 'symbols': symbols,
                     'origins': ' '.join(commits)})
        run.clear()

    prev_shared = prev_method = None
    for key, kind, a, b in tm:
        if key in om:
            flush(prev_shared, prev_method)
            prev_shared = (key, kind, a, b)
            if kind != 'field':
                prev_method = prev_shared
        else:
            run.append((key, kind, a, b))
    flush(prev_shared, prev_method)
    return [r for r in rows if r], theirs_lines


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--check', action='store_true')
    parser.add_argument('--repo', default=str(HERE.parents[4]))
    args = parser.parse_args()
    repo = Path(args.repo)
    leaves = {row['origin']: row['leaf'] for row in table(HERE / 'tools' / 'LEAF_ORIGINS.tsv')}
    out = ['# SPDX-License-Identifier: Apache-2.0',
           '# ADD_ROWS.tsv - declared ADD rows of recipe RESTORE_ADD_ROWS v2 (add_rows.py), generated by tools/gen_rows.py: '
           'per target, the result is the pre text (ours, or the clean 3-way output for a MERGE step) plus exactly these '
           'rows in table order after the named pre line; source T<step>:<a>-<b> = lines of that step\'s theirs blob '
           '(THREE_WAY.tsv) copied byte-for-byte (sha256_16 = first 16 hex of sha256 of the slice); ANCHOR = one new '
           'CNPCDA line (text column). No other line of ours or theirs is used.',
           'row\ttarget\tkind\tafter_line\tsource\tsha256_16\tsymbols\torigins\ttext']
    n = 0
    for target in table(HERE / 'THREE_WAY.tsv'):
        if target['strategy'] != 'MEMBERS':
            continue
        ours = git(repo, 'cat-file', '-p', target['ours']).decode()
        theirs = git(repo, 'cat-file', '-p', target['theirs']).decode()
        origins = blame(repo, target['head'], target['head_path'])
        indent = '    ' if target['path'].endswith('.java') else ''
        rows, tl = runs_for(target, ours, theirs, origins, leaves, indent)
        for r in rows:
            n += 1
            src = f"T{target['step']}:{r['a']}-{r['b']}" if 'a' in r else '-'
            digest = sha16(tl[r['a'] - 1:r['b']]) if 'a' in r else '-'
            out.append('\t'.join([f'R{n:03d}', target['target'], r['kind'], str(r['after']), src, digest,
                                  r.get('symbols', '-') or '-', r.get('origins', '-'), r.get('text', '-')]))
    for hunk in table(HERE / 'tools' / 'HUNKS.tsv'):
        n += 1
        out.append('\t'.join([f'R{n:03d}', hunk['target'], hunk['kind'], hunk['after_line'], hunk['source'],
                              hunk['sha256_16'], hunk['symbols'], hunk['origins'], hunk['text']]))
    text = '\n'.join(out) + '\n'
    path = HERE / 'ADD_ROWS.tsv'
    current = path.read_text(encoding='utf-8') if path.exists() else ''
    changed = int(current != text)
    if not args.check and changed:
        with open(path, 'w', encoding='utf-8', newline='') as handle:
            handle.write(text)
    print(f"GEN_ROWS rows={n} {'check' if args.check else 'write'} changed={changed}")
    return 1 if args.check and changed else 0


if __name__ == '__main__':
    sys.exit(main())
