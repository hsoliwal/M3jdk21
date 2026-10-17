#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
"""Read-only installed-source equality check; never replace an existing target."""
import argparse
import hashlib
from pathlib import Path

def main():
    ap=argparse.ArgumentParser();ap.add_argument('--root',required=True,type=Path);ap.add_argument('--sdk',required=True,type=Path)
    a=ap.parse_args();r=Path(__file__).resolve().parent/'src/main/resources/com/synexia/rewrite/java-util-view-contracts'
    count=0
    for line in (r/'manifest.tsv').read_text().splitlines():
        path,pre,post,template=line.split('\t');data=(a.root/path).read_bytes()
        if hashlib.sha256(data).hexdigest()!=post:raise RuntimeError('installed source mismatch '+path)
        if template!='KEEP' and data!=(a.sdk/path).read_bytes():raise RuntimeError('installed/SDK mismatch '+path)
        count+=1
    print('INSTALLED_SDK_EQUALITY_PASS paths='+str(count)+' changes=0')
if __name__=='__main__':main()
