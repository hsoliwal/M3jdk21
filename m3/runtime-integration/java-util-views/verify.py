#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
# Thin target verification; canonical source-changing recipe lives in Synexia #12015.
from pathlib import Path
import hashlib, json, shutil, subprocess, sys
if not __debug__: raise SystemExit('assertions must be enabled')
root=Path(__file__).resolve().parents[3]
if len(sys.argv)!=2: raise SystemExit('usage: verify.py <new-evidence-directory>')
out=Path(sys.argv[1]).absolute()
if out.exists(): raise SystemExit('output must be absent')
rows=(Path(__file__).with_name('targets.tsv')).read_text().splitlines()[1:]
for row in rows:
    path,before,expected=row.split('\t')
    if hashlib.sha256((root/path).read_bytes()).hexdigest()!=expected:
        raise SystemExit('generated source drift: '+path)
out.mkdir(parents=True)
commands=[]
def run(name,args,witness=None):
    p=subprocess.run(list(map(str,args)),stdout=subprocess.PIPE,stderr=subprocess.PIPE,text=True,timeout=90)
    (out/(name+'.log')).write_text(p.stdout+p.stderr)
    commands.append(dict(name=name,argv=list(map(str,args)),exit=p.returncode,stdout=p.stdout,stderr=p.stderr))
    (out/'commands.json').write_text(json.dumps(commands,indent=2)+'\n')
    if p.returncode or (witness and witness not in p.stdout+p.stderr): raise SystemExit(name+' failed')
    return p
src=out/'source/java/util';src.mkdir(parents=True)
shutil.copy2(root/'src/java.base/share/classes/java/util/CollectionViews.java',src/'CollectionViews.java')
for name in ['base','classes','headers']: (out/name).mkdir()
flags=['-proc:none','-Xlint:all','-Werror']
run('core-compile',['javac','--patch-module','java.base='+str(out/'source'),*flags,'-d',out/'base',src/'CollectionViews.java'])
patch=['--patch-module','java.base='+str(out/'base')]
tests=root/'test/jdk/java/util/CollectionViews'
run('test-compile',['javac',*patch,*flags,'-h',out/'headers','-d',out/'classes',root/'m3/collections/src/main/java/com/m3/collections/M3CollectionViews.java',*sorted(tests.glob('*.java'))])
run('bootstrap',['java',*patch,'-Xss256k','-cp',out/'classes','BootstrapViewsTest'],'module=java.base loader=bootstrap PASS')
for name,vm in [('default',[]),('interpreter',['-Xint']),('c1',['-XX:TieredStopAtLevel=1']),('c2',['-XX:-TieredCompilation','-Xbatch'])]:
    run(name,['java',*patch,'--add-opens','java.base/java.util=ALL-UNNAMED','-Xss256k',*vm,'-cp',out/'classes','CollectionViewsContract'],'PASS')
jhome=Path(shutil.which('javac')).resolve().parent.parent
lib=out/'libwrapper-runs.so'
run('native-build',['gcc','-std=c17','-O2','-Wall','-Wextra','-Werror','-fsanitize=undefined','-fno-sanitize-recover=all','-fPIC','-shared','-I'+str(out/'headers'),'-I'+str(jhome/'include'),'-I'+str(jhome/'include/linux'),tests/'run_search_jni.c','-o',lib])
p=run('jni',['java',*patch,'--add-opens','java.base/java.util=ALL-UNNAMED','-Xcheck:jni','-Xss256k','-cp',out/'classes','CollectionViewsContract','--native',lib],'native_projection=PASS queries=1027')
if p.stderr: raise SystemExit('JNI diagnostics')
print('PASS: six source seals; bootstrap owner; four VM modes; checked JNI/UBSan.')
print('Not a source-built JDK, JCK, full provider census or canonical Maven recipe gate.')
