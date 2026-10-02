#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Rebuild and test the exact compatibility closure. Never build/replace the installed JDK."""
from __future__ import annotations
import argparse
import hashlib
import json
import os
from pathlib import Path
import platform
import shutil
import subprocess
import sys
import tempfile
import xml.etree.ElementTree as ET
from recipe import canonical, digest, execute

ROOT = Path(__file__).resolve().parents[2]
HERE = Path(__file__).resolve().parent
PORT = ROOT / 'm3/ports/indexstring'

def input_snapshot() -> dict[str, str]:
    """Hash only declared migration inputs, not unrelated OpenJDK source trees."""
    inputs = {}
    for area in (PORT, HERE, ROOT / 'm3/tooling/migration-recipes'):
        for path in sorted(area.rglob('*')):
            if path.is_symlink(): raise RuntimeError('symlink input refused: ' + str(path))
            if (path.is_file() and path.suffix in ('.java', '.c', '.py', '.xml', '.json', '.txt')
                    and '__pycache__' not in path.parts and 'evidence' not in path.parts):
                if path.stat().st_size > 16 * 1024 * 1024:
                    raise RuntimeError('migration input exceeds 16 MiB budget: ' + str(path))
                inputs[path.relative_to(ROOT).as_posix()] = digest(path.read_bytes())
    return inputs

def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--out', required=True, type=Path)
    parser.add_argument('--bench', action='store_true')
    args = parser.parse_args()
    out = args.out.resolve()
    out.mkdir(parents=True, exist_ok=True)
    commands = []
    def run(name, command, *, cwd=ROOT):
        result = subprocess.run([str(x) for x in command], cwd=cwd, text=True,
                                stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=120)
        (out / (name + '.log')).write_text(result.stdout, encoding='utf-8')
        commands.append({'id':name,'argv':[str(x) for x in command], 'cwd':str(cwd),
                         'exit_code':result.returncode,'output_sha256':digest(result.stdout.encode())})
        if result.returncode:
            raise RuntimeError(name + ' failed; see ' + str(out / (name + '.log')))
        print(name + ': PASS', flush=True)
        return result.stdout
    receipt = {'schema':'m3.local-verification/1','os':platform.platform(),
               'architecture':platform.machine(),'commands':commands,'complete_jdk_build':False,
               'maven_openrewrite_executed':False,'full_source_reactor':False}
    try:
        java = shutil.which('java'); javac = shutil.which('javac'); gcc = shutil.which('gcc')
        if not java or not javac or not gcc: raise RuntimeError('JDK 21 and GCC are required; no silent native skip')
        version = run('java-version',[java,'-version'])
        if 'version "21.' not in version: raise RuntimeError('requires a Java 21 runtime')
        receipt['java']=version
        pinned_inputs = input_snapshot()
        for pom in ROOT.glob('m3/**/pom.xml'): ET.parse(pom)
        for script in HERE.glob('*.py'): compile(script.read_bytes(), str(script), 'exec')
        receipt['recipe_state'] = execute(PORT/'recipe.json',ROOT,'check')
        run('python-tests',[sys.executable,'-m','unittest','discover','-s',HERE,'-p','test_*.py','-v'])
        with tempfile.TemporaryDirectory(prefix='m3-closure-') as temporary:
            temp=Path(temporary); before=temp/'before'; after=temp/'after'
            before.mkdir();after.mkdir(); baseline=temp/'baseline';baseline.mkdir()
            source_files=json.loads((HERE/'source-files.json').read_text())['files']
            baseline_java=[]
            for source in source_files:
                name=Path(source['path']).name
                data=(HERE/'source-recipe/MIndexJoinedChars.before.java.txt').read_bytes() if name=='MIndexJoinedChars.java' else (PORT/'templates'/(name+'.txt')).read_bytes()
                if digest(data)!=source['sha256']: raise RuntimeError('baseline source hash differs: '+name)
                path=baseline/name;path.write_bytes(data);baseline_java.append(path)
            run('compile-baseline',[javac,'--release','21','-Xlint:all','-Werror','-d',before,*baseline_java,PORT/'src/test/java/ExistingSurfaceTest.java'])
            main_sources=sorted((PORT/'src/main/java').rglob('*.java'))
            planned = {row['path'] for row in json.loads((PORT/'recipe.json').read_text())['outputs']}
            if {p.relative_to(ROOT).as_posix() for p in main_sources} != planned:
                raise RuntimeError('unmapped/missing compatibility Java source')
            tests=sorted((PORT/'src/test/java').glob('*.java'))
            run('compile-candidate',[javac,'--release','21','-Xlint:all','-Werror','-d',after,*main_sources,*tests,HERE/'SurfaceInventory.java',PORT/'native-test/NativeBoundaryTest.java'])
            baseline_result=run('existing-baseline',[java,'-cp',before,'com.synexia.indexstring.ExistingSurfaceTest'])
            candidate_result=run('existing-candidate',[java,'-cp',after,'com.synexia.indexstring.ExistingSurfaceTest'])
            if baseline_result!=candidate_result: raise RuntimeError('existing-surface differential mismatch')
            run('retained-view',[java,'-Xmx512m','-cp',after,'com.synexia.indexstring.RetainedViewTest'])
            include=Path(javac).resolve().parents[1]/'include'
            library=temp/'libm3-boundary.so'
            run('compile-jni',[gcc,'-std=c11','-Wall','-Wextra','-Werror','-fPIC','-shared','-fsanitize=undefined','-fno-sanitize-recover=all','-I'+str(include),'-I'+str(include/'linux'),PORT/'native-test/boundary.c','-o',library])
            run('jni-boundary',[java,'-Xcheck:jni','-cp',after,'NativeBoundaryTest',library])
            receipt['native_library_sha256']=digest(library.read_bytes())
            run('string-surface',[java,'-cp',after,'SurfaceInventory','java.lang.String'])
            run('port-surface',[java,'-cp',after,'SurfaceInventory','com.synexia.indexstring.FrozenBytes','com.synexia.indexstring.FrozenChars','com.synexia.indexstring.MIndexJoinedBytes','com.synexia.indexstring.MIndexJoinedChars','com.m3.text.compat.M3Text'])
            if args.bench:
                for fork in range(1,4): run('benchmark-'+str(fork),[java,'-Xms256m','-Xmx256m','-XX:+UseSerialGC','-cp',after,'PortMicrobench'])
        scope = input_snapshot()
        if scope != pinned_inputs: raise RuntimeError('migration inputs changed during verification')
        receipt['input_files']=scope;receipt['input_scope_sha256']=digest(canonical(scope));receipt['status']='PASS'
    except (Exception, KeyboardInterrupt) as error:
        receipt['status']='FAIL';receipt['error']=str(error)
        print(str(error),file=sys.stderr)
    (out/'receipt.json').write_text(json.dumps(receipt,indent=2,sort_keys=True)+'\n')
    return 0 if receipt['status']=='PASS' else 1

if __name__=='__main__': raise SystemExit(main())
