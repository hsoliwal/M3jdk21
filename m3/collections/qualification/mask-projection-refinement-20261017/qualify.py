#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Target proof sequencing only; source changes come exclusively from OpenRewrite Results."""
import argparse
import ast
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET

TASK = Path(__file__).resolve().parent
JAVA = 'm3/collections/src/main/java/com/m3/collections/'
NATIVE = 'm3/collections/src/main/native/collections/m3_index_mask_compactor.c'
TEST = 'm3/collections/src/test/java/com/m3/collections/M3LongIndexProjectionSelfTest.java'


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def targets():
    return [line.split('\t') for line in (TASK/'templates/manifest.tsv').read_text().splitlines()]


def inputs(root):
    paths = [p for p in TASK.rglob('*') if p.is_file() and not any(x in ('evidence', 'target', '__pycache__') for x in p.relative_to(TASK).parts)]
    paths += [root/row[0] for row in targets() if (root/row[0]).exists()]
    return sorted(set(paths))


def source_seal(root):
    return ''.join(sha(p)+'  '+p.relative_to(root).as_posix()+'\n' for p in inputs(root))


def seal(root, proof):
    text = source_seal(root)
    digest = hashlib.sha256(text.encode()).hexdigest()
    if (proof/'SOURCE.sha256').read_text() != text or (proof/'LINT_OK').read_text() != digest+'\n':
        raise RuntimeError('source drift or missing lint receipt; a new pass is required')
    return digest


def run(folder, label, command, env, expected=0):
    folder.mkdir(parents=True, exist_ok=True)
    out, err = folder/(label+'.stdout'), folder/(label+'.stderr')
    if out.exists() or err.exists():
        raise RuntimeError('no hidden replay: '+label)
    argv = [str(x) for x in command]
    with out.open('wb') as stdout, err.open('wb') as stderr:
        try:
            result = subprocess.run(argv, cwd=TASK, env=env, stdout=stdout, stderr=stderr, timeout=420)
            status = result.returncode
        except (OSError, subprocess.TimeoutExpired) as failure:
            stderr.write((str(failure)+'\n').encode()); status = -1
    with (folder/'COMMANDS.jsonl').open('a') as log:
        log.write(json.dumps({'label':label,'argv':argv,'exit':status,'expected_exit':expected,'stdout_sha256':sha(out),'stderr_sha256':sha(err)},sort_keys=True)+'\n')
    print(label, 'exit='+str(status), flush=True)
    if status != expected:
        raise RuntimeError('failed '+label+'; see '+str(err))
    return out.read_bytes()


def environment():
    javac = shutil.which('javac')
    if not javac:
        raise RuntimeError('JDK required')
    home = Path(javac).resolve().parent.parent
    return dict(os.environ, JAVA_HOME=str(home)), ['-I'+str(home/'include'), '-I'+str(home/'include/linux')]


def lint(root, proof, env, includes):
    if proof.exists() or proof.is_relative_to(root):
        raise RuntimeError('fresh proof directory outside source tree required')
    proof.mkdir(parents=True)
    seen = set()
    for path, before, after, template in targets():
        if path in seen or Path(path).is_absolute() or '..' in Path(path).parts:
            raise RuntimeError('manifest geometry')
        seen.add(path)
        actual = sha(root/path) if (root/path).exists() else 'ABSENT'
        if actual not in (before, after):
            raise RuntimeError('unadmitted target '+path)
        if template != 'KEEP' and sha(TASK/'templates'/template) != after:
            raise RuntimeError('template hash mismatch')
    for p in inputs(root):
        text = p.read_text()
        if not p.is_relative_to(TASK/'assets') and (not text.endswith('\n') or '\r' in text or any(line.rstrip()!=line for line in text.splitlines())):
            raise RuntimeError('source policy lint '+str(p))
    ET.parse(TASK/'recipe/pom.xml'); ast.parse(Path(__file__).read_text())
    mapping = json.loads((TASK/'templates/mapping.json').read_text())
    names = [item['api'] for item in mapping['sdk_api_backings']]
    if len(names) != len(set(names)) or not {'org.springframework.util.MultiValueMap',
            'org.apache.commons.collections4.MultiValuedMap', 'com.google.common.collect.ListMultimap'}.issubset(names):
        raise RuntimeError('SDK qualified-name inventory drift')
    apache = next(item for item in mapping['sdk_api_backings'] if item['library'] == 'Apache Commons Collections')
    if apache['status'] != 'PENDING_ADAPTER_AND_CONTRACT_PROOF' or apache['factory'] is not None:
        raise RuntimeError('unqualified SDK adapter promotion')
    common = ['gcc','-std=c17','-Wall','-Wextra','-Wconversion','-Werror','-pedantic','-fsyntax-only',*includes]
    run(proof/'lint','template-native',[*common,TASK/'templates/m3_index_mask_compactor.c'],env)
    run(proof/'lint','mock-native',[*common,'-DM3_SOURCE="'+str(TASK/'templates/m3_index_mask_compactor.c')+'"',TASK/'probes/MaskTransferProbe.c'],env)
    text = source_seal(root); (proof/'SOURCE.sha256').write_text(text)
    (proof/'LINT_OK').write_text(hashlib.sha256(text.encode()).hexdigest()+'\n')


def compile_image(image, output, env, includes, mask_first=False):
    if output.exists():
        raise RuntimeError('compile directory must be fresh')
    output.mkdir(parents=True)
    classes, tests, headers = output/'classes', output/'tests', output/'headers'
    for p in [classes,tests,headers]: p.mkdir()
    sources = [image/(JAVA+'M3IndexMaskCompactor.java'), image/(JAVA+'M3LongIndexProjection.java')]
    native = image/NATIVE
    for p in [*sources,native]:
        text = p.read_text()
        if not text.endswith('\n') or any(line.rstrip()!=line for line in text.splitlines()):
            raise RuntimeError('receiver lint '+str(p))
    common = ['-std=c17','-Wall','-Wextra','-Wconversion','-Werror','-pedantic',*includes]
    run(output,'native-lint',['gcc',*common,'-fsyntax-only',native],env)
    run(output,'java-compile',['javac','--release','21','-encoding','UTF-8','-Xlint:all','-Werror','-h',headers,'-d',classes,*sources],env)
    run(output,'tests-compile',['javac','--release','21','-encoding','UTF-8','-Xlint:all','-Werror','-cp',classes,'-d',tests,image/TEST,*sorted((TASK/'probes').glob('*.java'))],env)
    header = headers/'com_m3_collections_M3IndexMaskCompactor.h'
    for mode, flags in [('release',['-O2']),('ubsan',['-O1','-g','-fsanitize=undefined','-fno-sanitize-recover=all'])]:
        folder=output/mode; folder.mkdir()
        run(output,'native-compile-'+mode,['gcc',*common,*flags,'-fPIC','-shared','-fstack-usage','-include',header,native,'-o',folder/'libm3_collections_mask.so'],env)
        symbols=run(output,'jni-symbols-'+mode,['nm','-D','--defined-only',folder/'libm3_collections_mask.so'],env)
        if [s.split()[-1] for s in symbols.decode().splitlines() if 'Java_' in s] != ['Java_com_m3_collections_M3IndexMaskCompactor_selectNative']:
            raise RuntimeError('JNI ABI drift')
    for mode, flags in [('release',['-O2']),('sanitized',['-O1','-g','-fsanitize=address,undefined','-fno-sanitize-recover=all'])]:
        definitions = ['-DREQUIRE_MASK_FIRST=1'] if mask_first else []
        run(output,'mock-compile-'+mode,['gcc',*common,*flags,*definitions,'-DM3_SOURCE="'+str(native)+'"',TASK/'probes/MaskTransferProbe.c','-o',output/('mock-'+mode)],env)


def java(output, name, mode, label, env, native=None, old_tests=None, expected=0):
    cmd=['java','-ea','-Xmx256m','-Xcheck:jni']
    if native: cmd+=['-Djava.library.path='+str(output/native)]
    cmd+=['-cp',str(output/'classes')+os.pathsep+str(old_tests or output/'tests'),'com.m3.collections.'+name]
    if mode: cmd.append(mode)
    return run(output,label,cmd,env,expected)


def qualify(image, output, env, includes, base=None, reuse=False, mask_first=False):
    compile_image(image,output,env,includes,mask_first)
    for name,mode,label in [('M3LongIndexProjectionSelfTest',None,'original-java'),('MaskProjectionProbe','java','projection-java'),('MaskProjectionProbe','surface','surface')]:
        data=java(output,name,mode,label,env)
        if base and data != (base/(label+'.stdout')).read_bytes(): raise RuntimeError('Java observations changed: '+label)
    java(output,'MaskProjectionProbe','missing','missing-provider',env,'missing')
    if reuse: java(output,'MaskProjectionProbe','sharing','immutable-sharing',env)
    for mode in ['release','sanitized']: run(output,'mock-run-'+mode,[output/('mock-'+mode)],env)
    for mode in ['release','ubsan']:
        for name,argument,label in [('M3LongIndexProjectionSelfTest',None,'original-native'),('MaskProjectionProbe','native','projection-native')]:
            data=java(output,name,argument,label+'-'+mode,env,mode)
            if base and data != (base/(label+'-'+mode+'.stdout')).read_bytes(): raise RuntimeError('native observations changed')


def file_tree(path):
    return {p.relative_to(path).as_posix():sha(p) for p in path.rglob('*') if p.is_file()}



def negative_controls(image, area, env, includes):
    records = []
    mutations = [
        ('offset-borrow', JAVA+'M3LongIndexProjection.java',
         'from == 0 && to == indices.length ? indices : physicalIndices()', 'indices',
         'MaskProjectionProbe', 'java', 'index and aligned state lengths differ'),
        ('order-inference', JAVA+'M3LongIndexProjection.java',
         'if (order == requested)', 'if (order != Order.ENCOUNTER)',
         'MaskProjectionProbe', 'java', 'stable opposite/repeated ordering')]
    for name, path, old, new, cls, mode, expected in mutations:
        folder=area/name;copy=folder/'image';shutil.copytree(image,copy)
        p=copy/path;text=p.read_text();before=sha(p)
        if text.count(old)!=1:raise RuntimeError('negative fixture anchor drift')
        p.write_text(text.replace(old,new));output=folder/'compiled'
        compile_image(copy,output,env,includes,True)
        java(output,cls,mode,'expected-refusal',env,expected=1)
        if expected not in (output/'expected-refusal.stderr').read_text():raise RuntimeError('wrong negative fingerprint')
        records.append({'name':name,'before':before,'after':sha(p),'expected':expected,'exit':1})
    folder=area/'offset-to-id';copy=folder/'image';shutil.copytree(image,copy)
    p=copy/NATIVE;text=p.read_text();before=sha(p)
    old='selected[i] = row_ids[selected[i]];'
    if text.count(old)!=1:raise RuntimeError('native offset negative fixture drift')
    p.write_text(text.replace(old,'selected[i] = row_ids[i];'))
    output=folder/'compiled';compile_image(copy,output,env,includes,True)
    run(output,'expected-refusal',[output/'mock-release'],env,expected=-6)
    if 'output[accepted++]==row_value(i)' not in (output/'expected-refusal.stderr').read_text():raise RuntimeError('wrong native negative fingerprint')
    records.append({'name':'offset-to-id','before':before,'after':sha(p),'expected':'output[accepted++]==row_value(i)','exit':-6})
    (area/'NEGATIVE_RESULTS.json').write_text(json.dumps(records,indent=2)+'\n')


def package_preview(output, dest, env):
    if dest.exists(): raise RuntimeError('fresh installed prefix required')
    lib = dest/'lib'; lib.mkdir(parents=True)
    run(dest,'runtime-jar',['jar','--create','--file',lib/'m3-mask-projection.jar',
        '--date=2026-10-17T00:00:00Z','-C',output/'classes','.',
        '-C',TASK/'assets','LICENSE-2.0.txt','-C',TASK/'assets','NOTICE.txt'],env)
    run(dest,'test-jar',['jar','--create','--file',lib/'m3-mask-projection-tests.jar',
        '--date=2026-10-17T00:00:00Z','-C',output/'tests','.'],env)
    shutil.copyfile(output/'release/libm3_collections_mask.so',lib/'libm3_collections_mask.so')
    cp=str(lib/'m3-mask-projection.jar')+os.pathsep+str(lib/'m3-mask-projection-tests.jar')
    for name,mode,label in [('M3LongIndexProjectionSelfTest',None,'installed-original'),
            ('MaskProjectionProbe','native','installed-native'),('MaskProjectionProbe','sharing','installed-sharing')]:
        args=['java','-ea','-Xmx256m','-Xcheck:jni','-Djava.library.path='+str(lib),'-cp',cp,'com.m3.collections.'+name]
        if mode: args.append(mode)
        run(dest,label,args,env)
    return file_tree(lib)


def runtime(root, proof, env, includes):
    witness=seal(root,proof)
    if (proof/'SDK_OK').read_text()!=witness+'\n': raise RuntimeError('recipe qualification required')
    area=proof/'runtime'
    if area.exists(): raise RuntimeError('no runtime replay')
    area.mkdir(); receiver=proof/'receiver'; completed=[]
    def checkpoint(name):
        seal(root,proof);completed.append(name);(proof/'STAGES.json').write_text(json.dumps(completed,indent=2)+'\n')
    try:
        base=area/'baseline';qualify(receiver/'before',base,env,includes);checkpoint('baseline')
        indexed={x[0]:x for x in targets()};image=receiver/'before'
        for n,path in enumerate((TASK/'ORDER.txt').read_text().splitlines()):
            next_image=area/('image-'+str(n));shutil.copytree(image,next_image)
            before,after=indexed[path][1:3]
            actual=sha(next_image/path) if (next_image/path).exists() else 'ABSENT'
            if actual!=before or sha(receiver/'results'/path)!=after: raise RuntimeError('SDK result drift')
            (next_image/path).parent.mkdir(parents=True,exist_ok=True)
            shutil.copyfile(receiver/'results'/path,next_image/path)
            (area/('LEAF-'+str(n)+'.json')).write_text(json.dumps({'path':path,'before':before,'after':after,'producer':'actual OpenRewrite Result'},indent=2)+'\n')
            output=area/('leaf-'+str(n));qualify(next_image,output,env,includes,base,True,n>=1);image=next_image;checkpoint(path)
        if file_tree(image)!=file_tree(receiver/'after'): raise RuntimeError('serial result != final SDK image')
        java(base,'MaskProjectionProbe','sharing','negative-baseline-reuse',env,expected=1)
        if 'certified order must reuse immutable index' not in (base/'negative-baseline-reuse.stderr').read_text(): raise RuntimeError('negative fingerprint changed')
        java(output,'MaskProjectionProbe','java','old-client-java',env,old_tests=base/'tests')
        if (output/'old-client-java.stdout').read_bytes()!=(base/'projection-java.stdout').read_bytes(): raise RuntimeError('old-client contract mismatch')
        negative_controls(image,area/'negatives',env,includes)
        checkpoint('negative-and-old-client')
        repeated=area/'replay';qualify(image,repeated,env,includes,base,True,True)
        for p in ['classes','headers']:
            if file_tree(output/p)!=file_tree(repeated/p): raise RuntimeError('compiled replay mismatch')
        if sha(output/'release/libm3_collections_mask.so')!=sha(repeated/'release/libm3_collections_mask.so'): raise RuntimeError('native binary replay mismatch')
        checkpoint('replay')
        for output_dir in [base,output]:
            java(output_dir,'MaskProjectionProbe','cost','measure',env,'release')
            java(output_dir,'MaskProjectionProbe','retention','retention',env)
        checkpoint('measure')
        first=package_preview(output,area/'installed-1',env)
        second=package_preview(repeated,area/'installed-2',env)
        if first!=second: raise RuntimeError('installed library/JAR replay mismatch')
        (area/'DISTRIBUTION.json').write_text(json.dumps(first,sort_keys=True,indent=2)+'\n')
        checkpoint('installed-package-replay')
        (proof/'RUNTIME_OK').write_text(witness+'\n')
    except Exception as failure:
        (proof/'FAILURE.json').write_text(json.dumps({'completed':completed,'failure':str(failure)},indent=2)+'\n');raise


def main():
    p=argparse.ArgumentParser();p.add_argument('mode',choices=['run','runtime']);p.add_argument('--root',type=Path,required=True);p.add_argument('--proof',type=Path,required=True)
    for name in ['maven','cache','settings']:p.add_argument('--'+name,type=Path)
    a=p.parse_args();root=a.root.resolve();proof=a.proof.resolve();env,includes=environment()
    if a.mode=='runtime':runtime(root,proof,env,includes);return
    if any(v is None for v in [a.maven,a.cache,a.settings]):p.error('run requires Maven/cache/settings')
    lint(root,proof,env,includes)
    run(proof,'maven-verify',[a.maven.resolve(),'-o','-B','-ntp','-s',a.settings.resolve(),'-gs',a.settings.resolve(),'-Dmaven.repo.local='+str(a.cache.resolve()),'-f',TASK/'recipe/pom.xml','-Dm3.proof='+str(proof),'-Dm3.target.root='+str(root),'-Dm3.python='+sys.executable,'verify'],env)
    if (proof/'RUNTIME_OK').read_text()!=seal(root,proof)+'\n':raise RuntimeError('required runtime evidence missing')
    print('M3JDK_MASK_PROJECTION_VERIFIED',flush=True)

if __name__=='__main__':main()
