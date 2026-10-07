#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
"""Extract exact existing Synexia owner sources; never rewrite the Synexia checkout."""
import argparse,hashlib,json,pathlib,subprocess
here=pathlib.Path(__file__).resolve().parent
p=argparse.ArgumentParser();p.add_argument('--git-dir',required=True);p.add_argument('--output',type=pathlib.Path,required=True);a=p.parse_args()
m=json.loads((here/'owner-pins.json').read_text());data={}
for name,expected in m['files'].items():
 raw=subprocess.check_output(['git','--git-dir='+a.git_dir,'show',m['commit']+':'+name])
 if hashlib.sha256(raw).hexdigest()!=expected:raise ValueError('owner source drift: '+name)
 target=a.output/name.split('/src/main/java/',1)[1]
 if target.is_symlink() or a.output.resolve() not in target.resolve().parents:raise ValueError('unsafe owner output')
 data[target]=raw
for target,raw in data.items():target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(raw)
print('PINNED_OWNER_SOURCES_PASS files='+str(len(data)))
