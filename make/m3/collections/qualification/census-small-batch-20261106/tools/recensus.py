#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Re-run the census compare for selected leaf rows against a newer M3jdk21 master.

Reuses census.py's normalisation/rename/compare code verbatim (exec of its header up to the
per-crate enumeration) and only swaps the M3 sha and the output directory.
usage: recensus.py ROWS.tsv CENSUS_DIR SYN_REPO SYN_SHA M3_REPO M3_SHA OUT_DIR
"""
import csv
import os
import sys

rows_tsv, census_dir, syn_repo, syn_sha, m3_repo, m3_sha, out = sys.argv[1:8]
# optional: file of donor same-package simple type names (not imported, so census token map misses them)
samepkg = set(open(sys.argv[8], encoding='utf-8').read().split()) if len(sys.argv) > 8 else set()
src = open(os.path.join(census_dir, 'tools', 'census.py'), encoding='utf-8').read()
head = src.split('def process_crate(')[0]
sys.argv = ['census.py', census_dir, syn_repo, syn_sha, m3_repo, m3_sha]
g = {'__name__': 'census_header'}
exec(compile(head, 'census.py', 'exec'), g)
compare = g['compare']
build_token_map = g['build_token_map']
syn, m3, sha256 = g['syn'], g['m3'], g['sha256']
normalize, nontrivial = g['normalize'], g['nontrivial']

os.makedirs(out, exist_ok=True)
hdr = ['leaf', 'crate', 'owner', 'owner_blob_sha256', 'post_sha256', 'pre', 'status', 'hunks', 'changed',
       'added_present', 'added_total', 'missing_nontrivial', 'owner_extra_nontrivial']
res_rows = []
with open(rows_tsv, encoding='utf-8') as f:
    for r in csv.DictReader(f, delimiter='\t'):
        post = syn.get(syn_sha, r['post'])
        pre = syn.get(syn_sha, r['pre']) if r['pre'] != '-' else None
        own = m3.get(m3_sha, r['owner'])
        d = os.path.join(out, r['leaf'])
        if post is None or own is None:
            res_rows.append([r['leaf'], r['crate'], r['owner'], '', '', r['pre'], 'MISSING', '', '', '', '', '', ''])
            continue
        tm = build_token_map(post.decode('utf-8', 'replace'))
        for t in samepkg:
            if t not in g['m3_type_names'] and ('M3' + t) in g['m3_type_names']:
                tm.setdefault(t, 'M3' + t)
        res = compare(post, own, pre, tm, d, r['leaf'])
        post_n = normalize(post, tm)
        own_n = normalize(own, tm)
        pre_set = set(nontrivial(normalize(pre, tm))) if pre is not None else set()
        own_set = set(nontrivial(own_n))
        post_set = set(nontrivial(post_n))
        missing = [l for l in nontrivial(post_n) if l not in own_set and l not in pre_set]
        extra = [l for l in nontrivial(own_n) if l not in post_set]
        g['write'](os.path.join(d, r['leaf'] + '.missing'), '\n'.join(missing) + '\n')
        g['write'](os.path.join(d, r['leaf'] + '.owner_extra'), '\n'.join(extra) + '\n')
        res_rows.append([r['leaf'], r['crate'], r['owner'], sha256(own), sha256(post),
                         ('-' if pre is None else 'present sha=' + sha256(pre)[:16]), res['status'], res['hunks'],
                         res['changed'], res['present'], res['total'], len(missing), len(extra)])
with open(os.path.join(out, 'RECENSUS.tsv'), 'w', encoding='utf-8', newline='\n') as f:
    f.write('\t'.join(hdr) + '\n')
    for rr in res_rows:
        f.write('\t'.join(str(x) for x in rr) + '\n')
print(open(os.path.join(out, 'RECENSUS.tsv'), encoding='utf-8').read())
