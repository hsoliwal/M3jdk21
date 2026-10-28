#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
"""Compile actual Java owners; on Linux compile/run actual existing JNI exports too."""
import argparse, os, shutil, subprocess, sys
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--output',type=Path,required=True)
p.add_argument('--java-only',action='store_true');a=p.parse_args()
root=Path(__file__).resolve().parents[3];out=a.output.resolve()
if out.exists() or out==root or root in out.parents: p.error('fresh output outside checkout required')
out.mkdir(parents=True);classes=out/'classes';headers=out/'headers'
j=root/'src/java.base/share/classes/jdk/internal/mindex'
test=Path(__file__).with_name('M3IntegratedEffectsProof.java')
def run(name,cmd):
 r=subprocess.run([str(x) for x in cmd],capture_output=True,text=True,timeout=180)
 (out/(name+'.log')).write_text(r.stdout+r.stderr)
 print(name, 'PASS' if r.returncode==0 else 'FAIL',flush=True)
 if r.returncode: print(r.stdout+r.stderr);raise SystemExit(r.returncode)
run('compile',['javac','-Xlint:all','-Werror','--patch-module','java.base='+str(j)+os.pathsep+str(test.parent),'-d',classes,'-h',headers,j/'M3RegexDpPlan.java',j/'M3StateRelation.java',test])
run('java',['java','--patch-module','java.base='+str(classes),'jdk.internal.mindex.M3IntegratedEffectsProof'])
if not a.java_only:
 if not sys.platform.startswith('linux'): p.error('native script qualified only on Linux; use --java-only elsewhere')
 javac=shutil.which('javac');home=Path(javac).resolve().parents[1]
 source=root/'src/java.base/share/native/libjava/M3RegexDpPlan.c'
 for mode in ['builtin','portable']:
  library=out/('libeffects-'+mode+'.so')
  flags=['-DM3_REGEX_PORTABLE'] if mode=='portable' else []
  run('native-compile-'+mode,['gcc','-std=c17','-O2','-Wall','-Wextra','-Wpedantic','-Werror','-fPIC','-shared','-fsanitize=undefined','-fno-sanitize-recover=all',*flags,'-I'+str(home/'include'),'-I'+str(home/'include/linux'),'-I'+str(headers),source,'-o',library])
  run('native-'+mode,['java','-Xcheck:jni','--patch-module','java.base='+str(classes),'jdk.internal.mindex.M3IntegratedEffectsProof',library])
 run('interpreter',['java','-Xint','-Xcheck:jni','--patch-module','java.base='+str(classes),'jdk.internal.mindex.M3IntegratedEffectsProof',out/'libeffects-builtin.so'])
print('FINITE_CORE_PROOF; full JDK image/jtreg/platform/performance gates remain open.')
