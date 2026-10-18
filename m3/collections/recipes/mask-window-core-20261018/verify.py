#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
"""Ordered finite qualification of actual source bytes via the existing Synexia SealReplay."""
from pathlib import Path
import argparse, ast, hashlib, json, os, re, shutil, subprocess, sys, time
T=Path(__file__).resolve().parent
J='m3/collections/src/main/java/com/m3/collections/M3IndexMaskCompactor.java'
C='m3/collections/src/main/native/collections/m3_index_mask_compactor.c'
TEST='m3/collections/src/test/java/com/m3/collections/M3MaskWindowKernelSelfTest.java'
R=T/'src/main/resources/m3-mask-window'
BASE='f29b0afe7ad253cc0562bc8dfdd48fc1acec710e'
def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest()
def rows(res=R): return [line.split('\t') for line in (res/'manifest.tsv').read_text().splitlines()]
def put(root,path,data):
    f=root/path;f.parent.mkdir(parents=True,exist_ok=True);f.write_bytes(data)
def inventory(root):
    return {str(p.relative_to(root)):sha(p) for p in sorted(root.rglob('*')) if p.is_file() and not p.is_symlink()}
def main():
    ap=argparse.ArgumentParser();ap.add_argument('--out',type=Path,required=True)
    ap.add_argument('--root',type=Path,help='read-only exact target postimage and unchanged-owner admission')
    args=ap.parse_args();out=args.out.absolute();out.mkdir(parents=True,exist_ok=False)
    commands=[];events=[];result={'status':'RUNNING','target_base':BASE,'complete_chat':False}
    def event(stage,details):
        events.append({'ordinal':len(events),'stage':stage,**details})
        (out/'EVENTS.json').write_text(json.dumps(events,indent=2)+'\n')
    def run(stage,argv,fail=False,marker=None):
        argv=list(map(str,argv));started=time.monotonic()
        process=subprocess.run(argv,cwd=T,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=120)
        log=out/f'{len(commands):03d}-{stage}.log';log.write_text(process.stdout)
        record={'stage':stage,'argv':argv,'exit':process.returncode,'expected_failure':fail,'log':log.name,
                'sha256':sha(log),'seconds':time.monotonic()-started};commands.append(record)
        (out/'COMMANDS.json').write_text(json.dumps(commands,indent=2)+'\n')
        if (process.returncode==0)==fail or (marker and marker not in process.stdout):
            raise RuntimeError('gate failed: '+stage+' '+str(argv)+'\n'+process.stdout)
        print(stage,process.returncode,process.stdout.strip()[-180:],flush=True)
        return process.stdout
    try:
        event('INVENTORY',{'recipe':inventory(T),'out_was_absent':True})
        for pin in json.loads((T/'TOOLING_PINS.json').read_text()):
            if sha(T/pin['path'])!=pin['sha256']:raise ValueError('tooling drift')
        for pin in json.loads((T/'OWNER_PINS.json').read_text()):
            if sha(T/'owners'/pin['name'])!=pin['sha256']:raise ValueError('actual owner drift')
        if args.root:
            installed=args.root.absolute()
            for path,_,digest,_ in rows():
                candidate=installed/path
                if candidate.is_symlink() or not candidate.is_file() or sha(candidate)!=digest:
                    raise ValueError('installed source mismatch: '+path)
            for pin in json.loads((T/'OWNER_PINS.json').read_text()):
                candidate=installed/Path(J).parent/pin['name']
                if candidate.is_symlink() or not candidate.is_file() or sha(candidate)!=pin['sha256']:
                    raise ValueError('installed consumer mismatch: '+pin['name'])
            event('INSTALLED_ADMISSION',{'root':str(installed),'result':'EXACT_FIVE_SOURCE_BYTES'})
        for f in [T/'author.py',T/'verify.py']:ast.parse(f.read_text(),filename=str(f))
        import xml.etree.ElementTree as ET
        ET.parse(T/'pom.xml');event('LINT_RECIPE',{'python_and_pom':'PASS'})
        run('author-check',['python3',T/'author.py','--check'],marker='AUTHOR_FIXED_POINT')
        tools=sorted((T/'src/main/java').rglob('*.java'))
        lint=out/'recipe-lint';lint.mkdir();cp=out/'recipe-classes';cp.mkdir()
        run('recipe-lint',['javac','--release','21','-proc:none','-Xlint:all','-Werror','-d',lint,*tools])
        run('recipe-compile',['javac','--release','21','-proc:none','-d',cp,*tools])
        def seal(root,res=R,mode='apply',fail=False):
            answer=run('recipe-'+mode,['java','-cp',cp,'com.synexia.rewrite.SealReplay',mode,root,res],fail)
            if not fail and mode=='check' and 'changes=0' not in answer:raise ValueError('not a fixed point')
            return answer
        def seed(root):
            root.mkdir(parents=True,exist_ok=True)
            for path in [J,C]:put(root,path,(T/'before'/Path(path).name).read_bytes())
        for bits in range(8):
            root=out/f'recipe-state-{bits}';seed(root)
            for index,(path,_,_,template) in enumerate(rows()):
                if bits&(1<<index):put(root,path,(R/template).read_bytes())
            decoy=root/'quoted-source.txt';decoy.write_text('M3IndexMaskCompactor in non-code')
            seal(root);seal(root,mode='check')
            times={p:(root/p).stat().st_mtime_ns for p,_,_,_ in rows()}
            if 'changes=0' not in seal(root):raise ValueError('repeat apply wrote')
            for path,_,digest,_ in rows():
                if sha(root/path)!=digest or (root/path).stat().st_mtime_ns!=times[path]:raise ValueError('fixed point drift')
            if decoy.read_text()!='M3IndexMaskCompactor in non-code':raise ValueError('decoy changed')
        faults=['missing','drift','occupied','template','duplicate','escape','symlink']
        for kind in faults:
            root=out/f'refuse-{kind}';seed(root);res=out/f'resources-{kind}';shutil.copytree(R,res)
            if kind=='missing':(root/J).unlink()
            elif kind=='drift':(root/J).write_text('unreviewed')
            elif kind=='occupied':put(root,TEST,b'occupied')
            elif kind=='template':(res/rows()[0][3]).write_text('corrupt')
            elif kind=='duplicate':
                with (res/'manifest.tsv').open('a') as f:f.write('\t'.join(rows()[0])+'\n')
            elif kind=='escape':(res/'manifest.tsv').write_text((res/'manifest.tsv').read_text().replace(J,'../escape.java'))
            else:(root/J).unlink();(root/J).symlink_to(out/'outside')
            before=inventory(root);answer=seal(root,res,fail=True)
            signatures={'missing':'required source missing','drift':'source drift','occupied':'source drift',
                        'template':'template hash mismatch','duplicate':'duplicate template target',
                        'escape':'noncanonical template path','symlink':'symlink target ancestor'}
            if signatures[kind] not in answer:raise ValueError('wrong refusal signature: '+kind)
            if inventory(root)!=before:raise ValueError('refusal changed source')
        event('RECIPE_TEST',{'partial_states':8,'fixed_points':8,'prewrite_refusals':len(faults)})
        javac=Path(shutil.which('javac')).resolve();home=javac.parents[1]
        cflags=['-std=c17','-Wall','-Wextra','-Wconversion','-Wshadow','-Wpedantic','-Werror',
                '-I'+str(home/'include'),'-I'+str(home/'include/linux')]
        ubsan=['-fsanitize=undefined','-fno-sanitize-recover=all']
        sources={};outputs={};java_out={};api=[];exports=[]
        root=out/'candidate';seed(root)
        for file in (T/'owners').glob('*.java'):put(root,str(Path(J).parent/file.name),file.read_bytes())
        # The frozen probe is a fixture for the original and intermediate images; target creation is pass 3.
        probe=T/'probes/M3MaskWindowKernelSelfTest.java'
        for stage,res in [('before',None),('java',R/'pass-1'),('native',R/'pass-2'),('test',R/'pass-3')]:
            pre=inventory(root)
            if res is not None:seal(root,res);seal(root,res,mode='check')
            event('PATCH',{'name':stage,'pre':pre,'post':inventory(root)})
            folder=out/stage;folder.mkdir();classes=folder/'classes';classes.mkdir();jl=folder/'lint';jl.mkdir()
            headers=folder/'headers';headers.mkdir();lib=folder/'native';lib.mkdir()
            actual=[root/J,*sorted((root/Path(J).parent).glob('M3Long*.java')),root/TEST if (root/TEST).exists() else probe]
            run('java-lint',['javac','--release','21','-proc:none','-Xlint:all','-Werror','-d',jl,*actual])
            run('native-lint',['gcc',*cflags,'-fsyntax-only',root/C])
            definition='-DPRODUCT_SOURCE="'+str(root/C)+'"'
            run('probe-lint',['gcc',*cflags,definition,'-fsyntax-only',T/'probes/native_probe.c'])
            event('LINT',{'name':stage,'result':'PASS'})
            run('java-compile',['javac','--release','21','-proc:none','-h',headers,'-d',classes,*actual])
            library=lib/'libm3_collections_mask.so'
            run('native-compile',['gcc',*cflags,*ubsan,'-O2','-fPIC','-shared','-fstack-usage',root/C,'-o',library])
            native_probe=folder/'native-probe'
            run('probe-compile',['gcc',*cflags,*ubsan,'-O2',definition,T/'probes/native_probe.c','-o',native_probe])
            event('COMPILE',{'name':stage,'result':'PASS'})
            descriptor=''
            for clazz in ['com.m3.collections.M3IndexMaskCompactor','com.m3.collections.M3IndexMaskCompactor$Backend']:
                descriptor+=run('api',['javap','-public','-s','-cp',classes,clazz])
            api.append(descriptor)
            symbols=run('symbols',['nm','-D','--defined-only',library])
            exports.append(sorted(s.split()[-1] for s in symbols.splitlines() if 'Java_' in s))
            outputs[stage]={'native_work':run('probe-test',[native_probe],marker='NATIVE_PROBE_PASS')}
            launcher=['java','-Xmx32m','-Xcheck:jni','-Djava.library.path='+str(lib),'-cp',classes,'com.m3.collections.M3MaskWindowKernelSelfTest']
            outputs[stage]['java']=run('java-test',launcher,marker='M3_MASK_WINDOW_PASS')
            event('TEST',{'name':stage,'result':'PASS'})
            outputs[stage]['jni']=run('jni-runtime',launcher+['--native'],marker='M3_MASK_WINDOW_PASS')
            empty=folder/'empty';empty.mkdir()
            outputs[stage]['missing']=run('missing-runtime',['java','-Djava.library.path='+str(empty),'-cp',classes,
                'com.m3.collections.M3MaskWindowKernelSelfTest','--missing'],marker='M3_MASK_WINDOW_PASS')
            event('RUNTIME',{'name':stage,'result':'PASS'})
            sources[stage]={'java':sha(root/J),'native':sha(root/C)};java_out[stage]=(classes,lib)
        for lane in ['java','jni','missing']:
            if len({v[lane] for v in outputs.values()})!=1:raise ValueError('observed trace drift '+lane)
        if len(set(api))!=1 or any(x!=exports[0] for x in exports):raise ValueError('public API or JNI exports changed')
        # Cross-version binary matrix, after both original and candidate have passed all prior stages.
        matrix={}
        for name,(classes,library) in {'old-java-new-native':(java_out['before'][0],java_out['test'][1]),
                                      'new-java-old-native':(java_out['test'][0],java_out['before'][1])}.items():
            matrix[name]=run('binary-matrix',['java','-Xmx32m','-Xcheck:jni','-Djava.library.path='+str(library),'-cp',classes,
                'com.m3.collections.M3MaskWindowKernelSelfTest','--native'],marker='M3_MASK_WINDOW_PASS')
            if matrix[name]!=outputs['before']['jni']:raise ValueError('binary matrix drift')
        # Older pre-positions JNI binary must still serve the current Java fallback.
        legacy=out/'legacy-native';legacy.mkdir();legacy_source=T/'legacy/m3_index_mask_compactor.c'
        event('PATCH_TEST_LEGACY',{'source':sha(legacy_source)})
        run('legacy-lint',['gcc',*cflags,'-fsyntax-only',legacy_source])
        run('legacy-compile',['gcc',*cflags,*ubsan,'-O2','-fPIC','-shared',legacy_source,'-o',legacy/'libm3_collections_mask.so'])
        matrix['new-java-pre-positions-native']=run('legacy-runtime',['java','-Xmx32m','-Xcheck:jni',
            '-Djava.library.path='+str(legacy),'-cp',java_out['test'][0],
            'com.m3.collections.M3MaskWindowKernelSelfTest','--native'],marker='M3_MASK_WINDOW_PASS')
        if matrix['new-java-pre-positions-native']!=outputs['before']['jni']:raise ValueError('legacy positions fallback drift')
        # Deliberately broken test images must be detected; product recipe output is never edited.
        mutants={
            'state-offset':('states, state_first + offset, chunk, marks','states, offset, chunk, marks'),
            'long-payload':('values[batch++] = values[i];','values[batch++] = values[i] ^ 1;')}
        for name,(old,new) in mutants.items():
            folder=out/('mutant-'+name);folder.mkdir();source=folder/'mutant.c'
            text=(root/C).read_text()
            if text.count(old)!=1:raise ValueError('mutant anchor drift')
            source.write_text(text.replace(old,new))
            event('PATCH_TEST_MUTANT',{'name':name,'before':sha(root/C),'after':sha(source)})
            run('mutant-lint',['gcc',*cflags,'-fsyntax-only',source])
            library=folder/'libm3_collections_mask.so'
            run('mutant-compile',['gcc',*cflags,*ubsan,'-O2','-fPIC','-shared',source,'-o',library])
            run('mutant-runtime',['java','-Xmx32m','-Xcheck:jni','-Djava.library.path='+str(folder),'-cp',java_out['test'][0],
                'com.m3.collections.M3MaskWindowKernelSelfTest','--native'],fail=True,marker=(
                    'java.util.ConcurrentModificationException: aligned selection states changed'
                    if name=='state-offset' else 'java.lang.AssertionError: long independent offsets'))
        seal(root,mode='check');times={p:(root/p).stat().st_mtime_ns for p,_,_,_ in rows()};seal(root)
        if any((root/p).stat().st_mtime_ns!=mtime for p,mtime in times.items()):raise ValueError('final replay wrote')
        maven=shutil.which('mvn')
        result={'status':'PASS_SELECTED_DIRECT_GATES','target_base':BASE,'source_stages':sources,'outputs':outputs,
                'matrix':matrix,'public_api':'BYTE_IDENTICAL_JAVAP','jni_exports':exports[0],
                'partial_states':8,'prewrite_refusals':7,'detected_mutants':2,'fixed_point':'NO_WRITES',
                'maven':'AVAILABLE_NOT_EXECUTED' if maven else 'NOT_INSTALLED_NOT_EXECUTED',
                'openrewrite_sdk':'NOT_USED_EXACT_SOURCE_REPLAY','full_module_jdk':'NOT_RUN',
                'cross_platform':'NOT_RUN','complete_chat':False,'phase_advanced':False,
                'candidate_root':str(root),'commands':len(commands)}
        event('VERIFIED',{'scope':'selected direct Java/JNI and recipe tests','complete_chat':False})
        print('QUALIFICATION_PASS',len(commands),'commands',flush=True)
    except BaseException as ex:
        result.update(status='FAIL',error=repr(ex));event('FAIL',{'error':repr(ex)})
        raise
    finally:
        (out/'RESULT.json').write_text(json.dumps(result,indent=2)+'\n')
if __name__=='__main__':main()
