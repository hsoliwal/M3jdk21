#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
"""Exercise the actual live-view owner without requiring Maven or WSL."""
import argparse, subprocess
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--output',type=Path,required=True);a=p.parse_args()
r=Path(__file__).resolve().parents[3];out=a.output.resolve()
if out.exists() or out==r or r in out.parents:p.error('fresh output outside checkout required')
out.mkdir(parents=True)
commands=[('compile',['javac','--release','21','-Xlint:all','-Werror','-d',out/'classes',r/'m3/collections/src/main/java/com/m3/collections/M3CollectionViews.java',Path(__file__).with_name('M3LiveViewContinuationTest.java')]),('normal',['java','-cp',out/'classes','com.m3.collections.M3LiveViewContinuationTest']),('interpreter',['java','-Xint','-cp',out/'classes','com.m3.collections.M3LiveViewContinuationTest'])]
for name,command in commands:
 result=subprocess.run([str(x) for x in command],capture_output=True,text=True,timeout=180)
 (out/(name+'.log')).write_text(result.stdout+result.stderr)
 print(name, 'PASS' if result.returncode==0 else 'FAIL',flush=True)
 if result.returncode:raise SystemExit(result.stderr)
print('FINITE_VIEW_PROOF; installed java.util/provider parity remains a separate gate.')
