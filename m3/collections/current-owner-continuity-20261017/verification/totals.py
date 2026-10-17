#!/usr/bin/env python3
# Modified 2026: current-owner continuity ADAPT; immutable donor blob a1e863654708eb4a34353cf0672bedb9015a10c2.
# SPDX-License-Identifier: Apache-2.0
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
"""Verify emitted scalar refinements and retain all predecessor gates; never transform source."""
import argparse, hashlib, json, os, shutil, subprocess, sys, xml.etree.ElementTree as ET
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from custody import Context, add_arguments, junit_inventory


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--root', required=True, type=Path)
    ap.add_argument('--deps', required=True, type=Path)
    ap.add_argument('--junit', required=True, type=Path)
    add_arguments(ap)
    args = ap.parse_args()
    root, deps, junit = args.root.resolve(), args.deps.resolve(), args.junit.resolve()
    owner = root/'m3/collections'; packet = owner/'counts/totals'
    ctx = Context(root, args.run_id, args.expected_source_sha, 'totals', deps, junit)
    target = ctx.target; evidence = target/'evidence'
    evidence.mkdir(parents=True, exist_ok=True)
    def digest(path): return hashlib.sha256(path.read_bytes()).hexdigest()
    def run(name, command):
        p = subprocess.run([str(x) for x in command], cwd=root, text=True, capture_output=True, timeout=180)
        output = p.stdout+p.stderr
        (evidence/(name+'.stderr.log')).write_text(p.stderr)
        if 'WARNING in native method' in output or 'runtime error:' in p.stderr:
            raise RuntimeError(name+': JNI/sanitizer diagnostic')
        (evidence/(name+'.log')).write_text('$ '+repr([str(x) for x in command])+'\n'+output+'\nEXIT='+str(p.returncode)+'\n')
        if p.returncode: raise RuntimeError(name+'\n'+output[-12000:])
        print(name+': PASS', flush=True)
        return output
    if not sys.platform.startswith('linux'): raise RuntimeError('Linux JNI qualification required')
    if digest(junit) != '329bd10288875a74d04c9ca6b7c9889c265ddf87b21a9ea42a7f7392f391472a':
        raise RuntimeError('JUnit artifact identity mismatch')
    java_home = Path(os.environ.get('JAVA_HOME',str(Path(shutil.which('javac')).resolve().parent.parent)))
    # The unchanged predecessor verifies all its sealed targets, 71 runtime tests,
    # strict compilation, four original/candidate OOM controls and core dependency boundary.
    run('predecessor', [sys.executable, '-I', '-S', '-B', ctx.verification/'counts.py','--root',root,'--deps',deps,'--junit',junit, *ctx.forward])
    previous = ctx.read_prior('counts')
    expected = {}
    for kind in ('java','text'):
        resources = packet/'recipe/src/main/resources/com/synexia/rewrite'/('hash-pinned-'+kind)/'m3-count-totals'
        for line in (resources/'manifest.tsv').read_text().splitlines():
            if not line or line.startswith('#'): continue
            path,before,after,name = line.split('\t')
            if before != 'ABSENT': raise RuntimeError('This refinement must remain additive')
            full = path if kind=='java' else 'm3/collections/'+path
            if full in expected or digest(resources/name)!=after or digest(root/full)!=after:
                raise RuntimeError('Manifest/output drift: '+full)
            expected[full]=after
    actual = dict(line.split('\t') for line in (packet/'recipe-receipt.tsv').read_text().splitlines() if line and not line.startswith('#'))
    if actual!=expected: raise RuntimeError('Incomplete or drifted execution receipt')
    provenance=json.loads((packet/'provenance.json').read_text())
    for atom in provenance['borrowed_atoms']:
        if digest(root/atom['path']) != atom['sha256']:
            raise RuntimeError('Existing donor/storage owner was changed: '+atom['path'])
    core=ctx.base/'counts/core'; tests=target/'tests'
    shutil.rmtree(tests,ignore_errors=True); tests.mkdir(parents=True)
    javac=[java_home/'bin/javac','--release','21','-Xlint:all','-Werror']
    run('totals-tests-compile',javac+['-cp',str(core)+os.pathsep+str(junit),'-d',tests]+sorted((packet/'src/test/java').rglob('*.java')))
    library=target/'libm3_count_totals.so'
    run('native-compile',[os.environ.get('CC','gcc'),'-std=c17','-O2','-Wall','-Wextra','-Werror','-fsanitize=undefined','-fno-sanitize-recover=all','-shared','-fPIC','-I'+str(java_home/'include'),'-I'+str(java_home/'include/linux'),owner/'src/main/native/collections/m3_count_totals.c',owner/'src/main/native/collections/m3_count_lanes.c','-o',library])
    reports=evidence/'runtime'; shutil.rmtree(reports,ignore_errors=True)
    run('totals-runtime',[java_home/'bin/java','-Xcheck:jni','-Dm3.totals.native='+str(library),'-jar',junit,'execute','--class-path',str(core)+os.pathsep+str(tests),'--select-class','com.m3.collections.M3CountTotalsTest','--fail-if-no-tests','--reports-dir',reports])
    files=list(reports.glob('TEST-*.xml'))
    totals={k:sum(int(ET.parse(p).getroot().attrib.get(k,0)) for p in files) for k in ('tests','failures','errors','skipped')}
    if totals != {'tests':10,'failures':0,'errors':0,'skipped':0}: raise RuntimeError('runtime counts: '+str(totals))
    if junit_inventory(reports, ctx.pins['suites']['scalar']) != totals: raise ValueError('scalar method drift')
    if (evidence/'totals-runtime.stderr.log').read_text(): raise ValueError('unexpected scalar JNI stderr')
    allocation=run('allocation',[java_home/'bin/java','-Xbatch','-Xcheck:jni','-cp',str(core)+os.pathsep+str(tests),'com.m3.collections.M3CountTotalsAllocationProbe',library])
    if 'PAIRED_ALLOCATION_REDUCTION_PASS' not in allocation: raise RuntimeError('allocation evidence missing')
    (evidence/'allocation.tsv').write_text(allocation)
    run('catalogue-query',[java_home/'bin/java','-cp',ctx.base/'counts/tests','com.m3.collections.tooling.M3CountedCatalogue',packet/'catalogue.tsv','frequency'])
    meta=core/'META-INF/m3-count-totals'; meta.mkdir(parents=True,exist_ok=True)
    for name in ('README.md','provenance.json','catalogue.tsv','recipe-receipt.tsv'):
        shutil.copyfile(packet/name,meta/name)
    jar=target/'m3-collections-with-totals.jar'
    run('package',[java_home/'bin/jar','--create','--file',jar,'-C',core,'.'])
    if run('dependencies',[java_home/'bin/jdeps','--print-module-deps',jar]).strip()!='java.base':
        raise RuntimeError('core dependency boundary changed')
    report={'schema':'M3_COUNT_TOTALS_CURRENT_VERIFIED_V1','generated_targets':len(expected),'runtime':totals,
            'predecessor_runtime':previous['runtime_suites'],'predecessor_allocation_negative_controls':previous['allocation_negative_controls'],
            'predecessor_allocation_candidate_passes':previous['allocation_candidate_passes'],'core_main_units':previous['core_main_units'],
            'native':'C17 UBSan; -Xcheck:jni; original and scalar kernels in same library',
            'existing_source_owners_changed':False,'recipe_outputs':expected,'java_base_only':True,
            'allocation':'See allocation.tsv; measured local JVM workload only; not a throughput benchmark',
            'full_library_coverage':False,'installed_jdk':'NOT_RUN','full_reactor':'NOT_RUN','phase_advance':False}
    report['current_regressions'] = previous['current_regressions']
    report['current_mask_jni'] = previous['current_mask_jni']
    report['predecessor_receipt_sha256'] = digest(ctx.base/'counts/evidence/qualification.json')
    ctx.write_report(report, [core, ctx.base/'counts/interop', tests, library, jar])
    print(json.dumps(report,indent=2))

if __name__=='__main__': main()
