#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Pinned Git file-accounting adapter; semantic inventory stays with m3-java-inventory.

This script never calls a lexical candidate list an exhaustive capability inventory.
It inventories all tracked entries, including excluded/unreadable/submodule entries,
and expands family hits to their containing Maven modules. It does not publish data.
Run in the private source repository; the resulting whole-estate ledger stays private.
"""
from __future__ import annotations
import argparse
import hashlib
import json
from pathlib import Path, PurePosixPath
import re
import subprocess
from recipe import Refusal

FAMILY = re.compile(rb'(?:SubMIndex|MatIndex|MIndex)[A-Za-z0-9_$]*')
MAX_TEXT = 16 * 1024 * 1024

def git(repo: Path, *args: str) -> bytes:
    return subprocess.check_output(['git', '-C', str(repo), *args], stderr=subprocess.PIPE)

def scan(repo: Path, commit: str) -> dict:
    if re.fullmatch('[a-f0-9]{40}', commit) is None: raise Refusal('use an exact 40-hex commit')
    if git(repo, 'rev-parse', commit + '^{commit}').decode().strip() != commit: raise Refusal('commit differs')
    raw = git(repo, 'ls-tree', '-r', '-z', commit)
    rows = []
    for record in raw.split(b'\0'):
        if not record: continue
        head, path = record.split(b'\t', 1); mode, kind, oid = head.decode('ascii').split()
        try: name = path.decode('utf-8')
        except UnicodeDecodeError: raise Refusal('non-UTF8 path requires an explicit path-codec adapter')
        rows.append({'path':name,'mode':mode,'object_type':kind,'git_blob':oid})
    poms = sorted((str(PurePosixPath(x['path']).parent) for x in rows if x['path'].endswith('/pom.xml') or x['path']=='pom.xml'), key=len, reverse=True)
    def module(path):
        for directory in poms:
            if directory == '.' or path.startswith(directory + '/'): return directory
        return '.'
    process = subprocess.Popen(['git','-C',str(repo),'cat-file','--batch'],stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.PIPE)
    errors=[]; selected_modules=set()
    try:
        for row in rows:
            path=row['path'];row['module']=module(path);row['reasons']=[]
            row['disposition']='excluded';row['rationale']='no family hit in this file; semantic/dependency review remains open'
            if row['object_type']!='blob':
                row.update(disposition='blocked',rationale='submodule/commit entry requires a separately pinned traversal',sha256=None)
                errors.append(path);continue
            process.stdin.write((row['git_blob']+'\n').encode());process.stdin.flush()
            header=process.stdout.readline().decode().strip().split()
            if len(header)!=3 or header[1]!='blob':raise Refusal('unexpected git object response')
            size=int(header[2]);row['bytes']=size;remaining=size;hasher=hashlib.sha256();chunks=[]
            while remaining:
                chunk=process.stdout.read(min(1<<20,remaining))
                if not chunk:raise Refusal('truncated Git object')
                hasher.update(chunk);remaining-=len(chunk)
                if size<=MAX_TEXT:chunks.append(chunk)
            if process.stdout.read(1)!=b'\n':raise Refusal('invalid Git object frame')
            row['sha256']=hasher.hexdigest()
            if FAMILY.search(path.encode()):row['reasons'].append('family-path')
            if row['mode']=='120000':
                row.update(disposition='excluded',rationale='symlink inventoried as a blob; never followed')
                continue
            if size>MAX_TEXT:
                row['reasons'].append('content-over-scan-budget');errors.append(path)
            else:
                data=b''.join(chunks)
                if b'\0' not in data and FAMILY.search(data):row['reasons'].append('family-reference')
            if row['reasons']:
                row.update(disposition='partial',rationale='candidate needs existing semantic inventory and contract review')
                selected_modules.add(row['module'])
    finally:
        process.stdin.close();process.stdout.close();process.stderr.close();process.wait()
    # Top-level root candidates do not make every nested module a false family member.
    for row in rows:
        if row['module'] in selected_modules and row['mode']!='120000' and row['object_type']=='blob':
            row['reasons'].append('same-owner-module-resource-or-dependency')
            row.update(disposition='partial',rationale='module closure candidate; includes non-prefix resources/tests/recipes')
    candidates=sorted(r['path'] for r in rows if r['disposition'] in ('partial','blocked'))
    return {'schema':'m3.git-file-accounting/1','commit':commit,
            'tree':git(repo,'rev-parse',commit+'^{tree}').decode().strip(),
            'tracked_entries':len(rows),'file_accounting_complete':True,'inventory_exhaustive':False,
            'semantic_owner':'com.synexia.m3.inventory.M3InventoryMain',
            'semantic_inventory_executed':False,'dependency_attribution_complete':False,
            'bounded_scan_gaps':sorted(errors),'candidate_paths':candidates,'entries':rows}

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--repo',type=Path,required=True);parser.add_argument('--commit',required=True)
    parser.add_argument('--out',type=Path,required=True)
    args=parser.parse_args()
    try:
        result=scan(args.repo,args.commit);args.out.parent.mkdir(parents=True,exist_ok=True)
        args.out.write_text(json.dumps(result,sort_keys=True,indent=2)+'\n')
        print(json.dumps({k:result[k] for k in ('commit','tracked_entries','file_accounting_complete','inventory_exhaustive','semantic_inventory_executed')}))
    except (Refusal,OSError,ValueError,subprocess.CalledProcessError) as error:
        parser.exit(2,'REFUSED: '+str(error)+'\n')
