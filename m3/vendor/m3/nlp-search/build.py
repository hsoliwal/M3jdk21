#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Offline full donor-source compiler. Optional overlays are executed recipe outputs.
This is the task build driver, not an application facade or transformation engine.
"""
import argparse, hashlib, json, os, shutil, subprocess, zipfile
from pathlib import Path
MODULES=['slf4j-api-2.0.12','slf4j-simple','opennlp-tools','lucene-core','lucene-analysis-common','lucene-queries','lucene-sandbox','lucene-queryparser','lucene-analysis-opennlp']
JAVA_RELEASES={'opennlp-tools': '17', 'lucene-analysis-opennlp': '21'}

def run(argv, log):
    with log.open('w') as f:
        p=subprocess.run([str(x) for x in argv],stdout=f,stderr=subprocess.STDOUT,timeout=240)
    if p.returncode: raise RuntimeError(f'{argv[0]} failed ({p.returncode}); {log}\n'+log.read_text()[-5000:])

def compile_tools(repo, out, jdk, overlay=None):
    lock=json.loads((Path(__file__).resolve().parent/'SOURCE_LOCK.json').read_text())
    for path,expected in lock.items():
        if hashlib.sha256((repo/path).read_bytes()).hexdigest()!=expected:raise ValueError('donor input drift: '+path)
    for name in MODULES:
        expected={repo/path for path in lock if path.startswith('third_party/'+name+'/')}
        actual={p for folder in ['src','build-inputs'] for p in (repo/'third_party'/name/folder).rglob('*') if p.is_file()}
        if actual!=expected:raise ValueError('donor source/resource coverage: '+name)
    if overlay:
        manifest=Path(__file__).resolve().parent/'OVERLAY_LOCK.json'
        expected=json.loads(manifest.read_text())
        actual={str(p.relative_to(overlay)):hashlib.sha256(p.read_bytes()).hexdigest() for p in overlay.rglob('*.java')}
        if actual!=expected:raise ValueError('executed recipe overlay drift or incomplete coverage')
    out.mkdir(parents=True,exist_ok=False)
    jars=[];receipt=[]
    for name in MODULES:
        owner=repo/'third_party'/name; stage=out/'source'/name
        shutil.copytree(owner/'src',stage/'src')
        if overlay:
            for p in (overlay/'third_party'/name).rglob('*.java'):
                dest=stage/p.relative_to(overlay/'third_party'/name);dest.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(p,dest)
        sources=sorted((stage/'src/main/java').rglob('*.java'))
        classes=out/'classes'/name;classes.mkdir(parents=True)
        args=out/(name+'.args');args.write_text('\n'.join('"'+str(p)+'"' for p in sources)+'\n')
        java=['-J-Xmx512m','-encoding','UTF-8','-proc:none','--release',JAVA_RELEASES.get(name, '11'),'-d',classes]
        if jars:java+=['--module-path' if name.startswith('lucene-') else '-classpath',os.pathsep.join(map(str,jars))]
        run([jdk/'bin/javac',*java,'@'+str(args)],out/(name+'.compile.log'))
        resources=stage/'src/main/resources'
        if resources.exists(): shutil.copytree(resources,classes,dirs_exist_ok=True)
        # Preserve original legal resources and repository provenance in every source-built jar.
        legal=classes/'META-INF/third-party'/name;legal.mkdir(parents=True)
        for p in owner.iterdir():
            if p.is_file() and p.name in ['LICENSE','NOTICE','THIRD_PARTY_NOTICES.md','PROVENANCE.json','SOURCE_FILES.json']:shutil.copyfile(p,legal/p.name)
        if name in ['slf4j-api-2.0.12','slf4j-simple']:
            run([jdk/'bin/javac','--release','9','--module-path',os.pathsep.join(map(str,jars)) or str(classes),'--patch-module',('org.slf4j' if name.startswith('slf4j-api') else 'org.slf4j.simple')+'='+str(classes),'-d',classes,stage/'src/main/descriptor/9/module-info.java'],out/(name+'.descriptor.log'))
        if name=='lucene-core':
            for version in [19,20,21]:
                ov=sorted((stage/f'src/main/versions/{version}').rglob('*.java'))
                target=classes/f'META-INF/versions/{version}';target.mkdir(parents=True)
                run([jdk/'bin/javac','-J-Xmx512m','-proc:none','-encoding','UTF-8','-source','11','-target','11','--patch-module','java.base='+str(owner/f'build-inputs/jdk{version}.apijar'),'--add-exports','java.base/java.lang.foreign=ALL-UNNAMED','--add-exports','java.base/jdk.incubator.vector=ALL-UNNAMED','-classpath',classes,'-d',target,*ov],out/(name+f'.java{version}.log'))
        manifest=out/(name+'.mf')
        mf='Manifest-Version: 1.0\n'
        if name=='lucene-core':mf+='Multi-Release: true\n'
        if name=='opennlp-tools':mf+='Main-Class: opennlp.tools.cmdline.CLI\nAutomatic-Module-Name: org.apache.opennlp.tools\n'
        manifest.write_text(mf+'\n');jar=out/(name+'.jar')
        oldmf=classes/'META-INF/MANIFEST.MF'
        if oldmf.exists():oldmf.unlink()
        run([jdk/'bin/jar','--create','--file',jar,'--manifest',manifest,'--date=2026-10-14T00:00:00Z','-C',classes,'.'],out/(name+'.jar.log'))
        jars.append(jar);receipt.append({'module':name,'java_files':len(sources),'jar_sha256':hashlib.sha256(jar.read_bytes()).hexdigest()})
    (out/'BUILD.json').write_text(json.dumps(receipt,indent=2)+'\n');return jars

if __name__=='__main__':
    ap=argparse.ArgumentParser();ap.add_argument('--repo',type=Path,required=True);ap.add_argument('--out',type=Path,required=True);ap.add_argument('--jdk',type=Path,required=True);ap.add_argument('--overlay',type=Path);a=ap.parse_args()
    compile_tools(a.repo.resolve(),a.out.resolve(),a.jdk.resolve(),a.overlay.resolve() if a.overlay else None)
