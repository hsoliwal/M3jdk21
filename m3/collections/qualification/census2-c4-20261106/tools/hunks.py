#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Pure-addition hunks of a donor pre/post pair, normalised to whole declarations (census pass-2 C4 generator).

A diff of a pure addition can place the inserted block anywhere along a run of equal lines (difflib picked "    }"
... "    /**" splits); the block is slid (the slide keeps the post text identical) to the nearest position where it
starts with a declaration, comment or blank line and ends with a closing brace, a semicolon or a blank line, then its
leading and trailing blank lines are dropped. Refuses a non-addition opcode. Reads blobs with git cat-file.
usage (library): blocks(donor_git, rev, pre_path, post_path) -> [(pre_blob, post_blob, [lines...]), ...]"""
import difflib
import re
import subprocess

START = re.compile(r'^(\s*$|    (/\*\*|//|private |public |protected |static |final |@)|/\*|JNIEXPORT|static |#)')
END = re.compile(r'(^\s*$|^    \}$|^\}$|;$)')


def blob(git, rev, path):
    sha = subprocess.run(['git', '-C', git, 'rev-parse', rev + ':' + path], check=True, capture_output=True).stdout.decode().strip()
    data = subprocess.run(['git', '-C', git, 'cat-file', 'blob', sha], check=True, capture_output=True).stdout
    return sha, data.decode('utf-8')


def blocks(git, rev, pre_path, post_path):
    pre_sha, pre = blob(git, rev, pre_path)
    post_sha, post = blob(git, rev, post_path)
    a, b = pre.splitlines(True), post.splitlines(True)
    out = []
    for tag, i1, i2, j1, j2 in difflib.SequenceMatcher(None, a, b, autojunk=False).get_opcodes():
        if tag == 'equal':
            continue
        if tag != 'insert':
            raise SystemExit('REFUSED: %s -> %s has a %s opcode (not a pure addition)' % (pre_path, post_path, tag))
        best = None
        for k in sorted(range(-12, 13), key=abs):
            s, e = j1 + k, j2 + k
            if s < 0 or e > len(b):
                continue
            if k > 0 and b[j1:j1 + k] != b[j2:j2 + k]:
                continue
            if k < 0 and b[j1 + k:j1] != b[j2 + k:j2]:
                continue
            block = b[s:e]
            core = [l for l in block]
            while core and not core[0].strip():
                core.pop(0)
            while core and not core[-1].strip():
                core.pop()
            if core and START.match(core[0].rstrip('\n')) and END.search(core[-1].rstrip('\n')):
                best = core
                break
        if best is None:
            raise SystemExit('REFUSED: no declaration-aligned slide for a hunk of %s' % post_path)
        out.append((pre_sha, post_sha, best))
    return out
