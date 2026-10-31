# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Ordered task reproduction using the existing recipe SDK and target contracts."""
from pathlib import Path
import argparse, hashlib, json, os, shutil, subprocess

p=argparse.ArgumentParser()
p.add_argument('phase', choices=['recipe', 'product'])
for arg in ['work','jdk','maven','m2']: p.add_argument('--'+arg,type=Path,required=True)
a=p.parse_args();c=Path(__file__).resolve().parent.parent;w=a.work.resolve()
w.mkdir(parents=True,exist_ok=True);e=w/'evidence';e.mkdir(exist_ok=True)
env=os.environ.copy();env['JAVA_HOME']=str(a.jdk);env['PATH']=str(a.jdk/'bin')+os.pathsep+env['PATH']
def run(name,cmd):
    cmd=list(map(str,cmd));log=e/(name+'.log');assert not log.exists(),name
    with log.open('w') as f:r=subprocess.run(cmd,cwd=w,env=env,stdout=f,stderr=subprocess.STDOUT)
    (e/(name+'.json')).write_text(json.dumps({'command':cmd,'exit':r.returncode,'log_sha256':hashlib.sha256(log.read_bytes()).hexdigest()},indent=2)+'\n')
    print(name,r.returncode,'\n'.join(log.read_text().splitlines()[-7:]),flush=True)
    if r.returncode:raise SystemExit('HOLD '+name)
def lint(name,files):
    cp=os.pathsep.join(str(a.m2/part) for part in (c/'proof/lint-classpath.txt').read_text().splitlines())
    run(name,['java','-cp',cp,'com.puppycrawl.tools.checkstyle.Main','-c',c/'proof/checkstyle.xml',*files])
source='src/java.base/share/classes/java/util'
native='src/java.base/share/native/libjava/M3CollectionMasks.c'
tests='test/jdk/java/util/Collections'
inputs=c/'proof/inputs';name='m3jdk-collection-dense-20261019'
rows=[]
for kind in ['java','text']:
    r=c/('resources/com/synexia/rewrite/hash-pinned-'+kind)/name
    for line in (r/'manifest.tsv').read_text().splitlines():
        row=line.split('\t');rows.append((r,row))
        assert hashlib.sha256((r/row[3]).read_bytes()).hexdigest()==row[2]
        if row[1]!='ABSENT':assert hashlib.sha256((inputs/row[0]).read_bytes()).hexdigest()==row[1]
if a.phase=='recipe':
    lintdir=w/'lint';lintdir.mkdir()
    for r,row in rows:
        if row[0].endswith('.java'):shutil.copyfile(r/row[3],lintdir/Path(row[0]).name)
    lint('01-recipe-lint',list(lintdir.glob('*.java'))+list(c.glob('src/test/**/*.java')))
    command=[a.maven,'-o','-B','-ntp','-Dmaven.repo.local='+str(a.m2),'-f',c/'pom.xml']
    run('02-recipe-compile',command+['test-compile'])
    run('03-recipe-sdk',command+['surefire:test','-Dmaterialize.dir='+str(w/'generated')])
else:
    assert json.loads((e/'03-recipe-sdk.json').read_text())['exit']==0
    for r,row in rows:assert hashlib.sha256((w/'generated'/row[0]).read_bytes()).hexdigest()==row[2]
    # Replay each production-file pass cumulatively. Every output comes from SDK Results.
    states=[('baseline',[]),('java-mask',[source+'/M3CollectionMasks.java']),
            ('native-mask',[source+'/M3CollectionMasks.java',native]),
            ('snapshot',[source+'/M3CollectionMasks.java',native,source+'/M3CollectionViews.java'])]
    for state,changed in states:
        stage=w/state;patch=stage/'source';classes=stage/'classes';headers=stage/'headers'
        boot=stage/'boot-source';bootclasses=stage/'boot-classes';testclasses=stage/'tests'
        for directory in [patch/'java/util',classes,headers,boot/'java/util',bootclasses,testclasses]:directory.mkdir(parents=True)
        for simple in ['Collections','M3CollectionViews','M3CollectionMasks']:
            rel=source+'/'+simple+'.java';origin=w/'generated'/rel if rel in changed else inputs/rel
            shutil.copyfile(origin,patch/'java/util'/(simple+'.java'))
        for simple in ['M3CollectionMasksTest','M3CollectionDenseTest']:
            rel=tests+'/'+simple+'.java';origin=w/'generated'/rel if simple=='M3CollectionDenseTest' else inputs/rel
            shutil.copyfile(origin,boot/'java/util'/(simple+'.java'))
        lint(state+'-04-product-lint',list(patch.rglob('*.java'))+list(boot.rglob('*.java'))+[inputs/tests/'M3CollectionViewsTest.java'])
        run(state+'-05-java-compile',['javac','-J-Xmx256m','-Xlint:all','--patch-module','java.base='+str(patch),'-h',headers,'-d',classes,*patch.rglob('*.java')])
        cfile=w/'generated'/native if native in changed else inputs/native
        flags=['-std=c17','-Wall','-Wextra','-Werror','-I'+str(a.jdk/'include'),'-I'+str(a.jdk/'include/linux'),'-I'+str(headers)]
        run(state+'-06-native-lint',['cc',*flags,'-fsyntax-only',cfile])
        library=stage/'libM3CollectionMasksQualification.so'
        run(state+'-07-native-compile',['cc',*flags,'-O3','-fPIC','-shared','-fsanitize=undefined','-fno-sanitize-recover=undefined',cfile,'-o',library])
        run(state+'-08-tests-compile',['javac','-J-Xmx256m','-Xlint:all','--patch-module','java.base='+str(classes),'-d',testclasses,inputs/tests/'M3CollectionViewsTest.java'])
        run(state+'-09-bootstrap-tests-compile',['javac','-J-Xmx256m','-Xlint:all','--patch-module','java.base='+str(classes)+os.pathsep+str(boot),'-d',bootclasses,*boot.rglob('*.java')])
        run(state+'-10-public-api',['javap','-public','-s',classes/'java/util/Collections.class'])
        run(state+'-11-jni-symbols',['nm','-D','--defined-only',library])
        for mode,label in [([],'mixed'),(['-Xint'],'interpreter')]:
            run(state+'-12-views-'+label,['java','-Xmx256m',*mode,'--patch-module','java.base='+str(classes),'-cp',testclasses,'M3CollectionViewsTest'])
        base=['java','-Xmx256m','-Xcheck:jni','--patch-module','java.base='+str(classes)+os.pathsep+str(bootclasses)]
        loaded='-Dtest.m3.masks.native.library='+str(library)
        optin='-Djdk.m3.collectionMasks.native=true'
        for options,label,mode in [([loaded],'native-full','full'),([], 'java-consumer','consumer'),([loaded,optin],'native-consumer','consumer'),([optin],'fallback-consumer','consumer')]:
            run(state+'-13-masks-'+label,[*base,*options,'java.util.M3CollectionMasksTest',mode])
        run(state+'-14-dense-java',[*base,'java.util.M3CollectionDenseTest','java'])
        run(state+'-15-dense-native',[*base,loaded,optin,'java.util.M3CollectionDenseTest','native'])
        run(state+'-16-dense-interpreter',[*base,'-Xint','java.util.M3CollectionDenseTest','java'])
        if state!='baseline':
            for suffix in ['10-public-api','12-views-mixed','12-views-interpreter','13-masks-native-full','13-masks-java-consumer','13-masks-native-consumer','13-masks-fallback-consumer','14-dense-java','15-dense-native','16-dense-interpreter']:
                assert (e/('baseline-'+suffix+'.log')).read_bytes()==(e/(state+'-'+suffix+'.log')).read_bytes(),(state,suffix)
            assert (w/'baseline/headers/java_util_M3CollectionMasks.h').read_bytes()==(headers/'java_util_M3CollectionMasks.h').read_bytes()
    (e/'PARITY.json').write_text(json.dumps({'scope':'three cumulative production-file passes','api_identical':True,'jni_header_identical':True,'contract_trace_identical':True,'full_product_image':'PENDING','other_platforms':'PENDING'},indent=2)+'\n')
