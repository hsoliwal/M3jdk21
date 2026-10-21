#!/usr/bin/env python3
"""Qualify reviewed inputs without modifying product source; retain actual evidence.

The current receiving baseline compiles, but its unchanged strict sequenced
contract rejects the removed endpoint overrides. Record that failure, then compare
mutation/order atoms against a control containing only the bounded sequenced fix.
Compile all main sources as their named module and standalone proofs separately on
the class path, matching the production/test boundary in Maven. Current upstream
entry semantics, stronger tests, and corrected key/mask oracles remain untouched.
"""
from pathlib import Path
import argparse
import hashlib
import json
import os
import shutil
import subprocess
import time

parser = argparse.ArgumentParser()
parser.add_argument('--repo', type=Path, required=True)
parser.add_argument('--candidate', type=Path, required=True)
parser.add_argument('--emitted', type=Path, help='Actual SDK output root, containing the m3/ tree')
parser.add_argument('--jdk', type=Path, required=True)
parser.add_argument('--out', type=Path, required=True)
args = parser.parse_args()
repo, candidate, jdk, out = [p.resolve() for p in (args.repo,args.candidate,args.jdk,args.out)]
files=args.emitted.resolve() if args.emitted else candidate/'files'
out.mkdir(parents=True,exist_ok=False)
seals=json.loads((candidate/'SOURCE_SEALS.json').read_text())
for seal in seals:
    relative=seal['target']
    assert hashlib.sha256((files/relative).read_bytes()).hexdigest()==seal['after_sha256'], relative+' output drift'
    original=repo/relative
    if 'before_sha256' in seal:
        assert hashlib.sha256(original.read_bytes()).hexdigest()==seal['before_sha256'], relative+' receiving drift'
    else:
        assert not original.exists(), relative+' occupied receiving path'
(out/'INPUTS.json').write_text(json.dumps({'receiving_head':subprocess.check_output(
    ['git','-C',str(repo),'rev-parse','HEAD'],text=True).strip(),
    'source_kind':'ACTUAL_SDK_EMITTED' if args.emitted else 'REVIEWED_CANDIDATE_ONLY',
    'source_root':str(files),'source_seals':seals,
    'runner_sha256':hashlib.sha256(Path(__file__).read_bytes()).hexdigest()},indent=2)+'\n')
native_seals=json.loads((candidate/'NATIVE_SOURCE_SEALS.json').read_text())
for seal in native_seals:
    assert hashlib.sha256((repo/seal['path']).read_bytes()).hexdigest()==seal['sha256'], seal['path']+' native source drift'
(out/'NATIVE_INPUTS.json').write_text(json.dumps(native_seals,indent=2)+'\n')
upstream=json.loads((candidate/'PROVENANCE.json').read_text())['upstream_preserved']
for seal in upstream:
    assert hashlib.sha256((repo/seal['path']).read_bytes()).hexdigest()==seal['sha256'], seal['path']+' preserved upstream drift'
(out/'PRESERVED_UPSTREAM.json').write_text(json.dumps(upstream,indent=2)+'\n')
records=[]
def run(name, command, expected=0):
    start=time.monotonic()
    proc=subprocess.run([str(x) for x in command],capture_output=True,text=True,timeout=240)
    (out/(name+'.log')).write_text(proc.stdout+proc.stderr)
    rec={'name':name,'command':[str(x) for x in command],'exit':proc.returncode,
         'expected_exit':expected,'seconds':round(time.monotonic()-start,3)}
    records.append(rec)
    (out/'COMMANDS.json').write_text(json.dumps(records,indent=2)+'\n')
    print(name, 'exit='+str(proc.returncode), proc.stdout[-240:].strip(),flush=True)
    if proc.returncode != expected:
        raise AssertionError(name+' failed; see '+str(out/(name+'.log')))
    return proc.stdout

run('environment',[jdk/'bin/java','-version'])
main=sorted((repo/'m3/collections/src/main/java').rglob('*.java'))
base='m3/collections/src/test/java/com/m3/collections/'
contracts=[
    'M3FilteredListsContract','M3FilteredListsEdges','M3FilteredListsDeepCompositionProof',
    'M3FilteredListsReadonlyDepthProof','M3FilteredListsMaskBridgeProof',
    'M3FilteredViewsContract','M3StoredRepresentativeContract','M3FilteredSetIdentityContract',
    'M3BoundedFilteredNavigationContract','M3FilteredNavigableSetRangeContract',
    'M3FilteredNavigationReconciliationContract','M3MutableFilteredViewsContract',
    'M3StrictFilteredSequencedContract','M3FilteredMergedSequencedContract',
    'M3FilteredSplitContract','M3IndexedMultiValueOccurrenceSelfTest',
    'M3LongIndexProjectionSelfTest','M3LongIndexProjectionMaskOffsetProof',
    'M3IndexMaskCompactorSelfTest','M3NativeIndexRangeSelfTest',
    'M3RequiredExcludedMaskSelectorContract','M3RequiredExcludedMaskSelectorNativeContract']
new_contracts=['M3FilteredListMutationProof','M3IndexOrderWindowContract']
resource=repo/'m3/collections/qualification/wrapper-convergence-20261018/recipe-custody/recipe-host/passes/index-resource-probe/resources/com/synexia/rewrite/hash-pinned-java/m3-wrapper-index-resource-probe/M3IndexOrderWindowResourceProbe.java.txt'
resource_java=out/'probe-src/com/m3/collections/M3IndexOrderWindowResourceProbe.java'
resource_java.parent.mkdir(parents=True,exist_ok=True)
shutil.copyfile(resource,resource_java)

raw=out/'original-baseline'; raw.mkdir()
raw_args=raw/'sources.args'
raw_args.write_text('\n'.join(str(p) for p in main)+'\n')
run('original-baseline-main-compile',[jdk/'bin/javac','-Xlint:all,-module','-Werror',
    '-d',raw/'classes','@'+str(raw_args)])
run('original-baseline-strict-compile',[jdk/'bin/javac','-Xlint:all','-Werror',
    '-cp',raw/'classes','-d',raw/'tests',
    repo/(base+'M3StrictFilteredSequencedContract.java')])
run('original-baseline-strict-negative',[jdk/'bin/java','-Xmx256m','-cp',
    os.pathsep.join([str(raw/'tests'),str(raw/'classes')]),
    'com.m3.collections.M3StrictFilteredSequencedContract'],1)
assert 'hidden sorted removeFirst must refuse even when empty wrong exception: java.util.NoSuchElementException' in (out/'original-baseline-strict-negative.log').read_text()

reconciled='sequenced-reconciled-control'
sequenced_owner=Path('m3/collections/src/main/java/com/m3/collections/M3FilteredViews.java')
for label in [reconciled,'candidate']:
    image=out/label
    classes=image/'main-classes'; tests=image/'test-classes'; headers=image/'headers'
    classes.mkdir(parents=True); tests.mkdir(); headers.mkdir()
    sources=[]
    for p in main:
        relative=p.relative_to(repo)
        alternative=files/relative
        use_candidate=(label=='candidate' or relative==sequenced_owner) and alternative.exists()
        sources.append(alternative if use_candidate else p)
    source_hashes=[{'path':str(p),'sha256':hashlib.sha256(p.read_bytes()).hexdigest()} for p in sources]
    (image/'SOURCES.json').write_text(json.dumps(source_hashes,indent=2)+'\n')
    source_args=image/'sources.args'
    source_args.write_text('\n'.join(str(p) for p in sources)+'\n')
    run(label+'-main-compile',[jdk/'bin/javac','-Xlint:all,-module','-Werror','-d',classes,'-h',headers,'@'+str(source_args)])
    test_sources=[]
    for name in contracts:
        relative=Path(base+name+'.java')
        alternative=files/relative
        test_sources.append(alternative if alternative.exists() else repo/relative)
    test_sources.extend(files/(base+name+'.java') for name in new_contracts)
    test_sources.append(resource_java)
    test_sources.append(candidate/'proof-src/com/m3/collections/M3FilteredListMutationWitness.java')
    test_args=image/'tests.args'
    test_args.write_text('\n'.join(str(p) for p in test_sources)+'\n')
    (image/'TEST_SOURCES.json').write_text(json.dumps([
        {'path':str(p),'sha256':hashlib.sha256(p.read_bytes()).hexdigest()}
        for p in test_sources],indent=2)+'\n')
    run(label+'-test-compile',[jdk/'bin/javac','-Xlint:all','-Werror','-cp',classes,
        '-d',tests,'@'+str(test_args)])
    for owner in ['M3FilteredLists','M3FilteredViews','M3LongIndexProjection','M3SlotMap',
                  'M3LinkedHashMap','M3TreeMap','M3WeakHashMap']:
        run(label+'-api-'+owner,[jdk/'bin/javap','-classpath',classes,'-protected','-s','com.m3.collections.'+owner])

native=out/'native'; native.mkdir()
run('native-compiler-version',['gcc','--version'])
include=['-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux')]
croot=repo/'m3/collections/src/main/native/collections'
headers=out/'candidate/headers'
run('native-mask-build',['gcc','-std=c17','-O2','-Wall','-Wextra','-Werror','-pedantic','-fPIC','-shared',
    '-fsanitize=undefined','-fno-sanitize-recover=all',*include,
    '-include',headers/'com_m3_collections_M3IndexMaskCompactor.h',
    '-include',headers/'com_m3_collections_M3IndexMaskWindow.h',
    croot/'m3_index_mask_compactor.c',croot/'m3_index_mask_window.c',
    '-o',native/'libm3_collections_mask.so'])
run('native-address-build',['gcc','-std=c17','-O2','-Wall','-Wextra','-Werror','-pedantic','-fPIC','-shared',
    '-fsanitize=undefined','-fno-sanitize-recover=all',*include,
    '-include',headers/'com_m3_collections_M3LongAddressKernel.h',
    croot/'m3_long_address_kernel.c','-o',native/'libm3_collections_address.so'])
run('native-exports',['nm','-D','--defined-only',native/'libm3_collections_mask.so'])

def java(label, native_mode=False, interpreted=False):
    path=native if native_mode else out/'missing-native'
    return [jdk/'bin/java','-Xmx256m','-Xcheck:jni',*(['-Xint'] if interpreted else []),
            '-Djava.library.path='+str(path),'-cp',
            os.pathsep.join(str(out/label/p) for p in ['test-classes','main-classes'])]

negative=run('baseline-mutation-negative',java(reconciled)+['com.m3.collections.M3FilteredListMutationProof'],1)
negative_log=(out/'baseline-mutation-negative.log').read_text()
assert 'missing ConcurrentModificationException before mutation' in negative_log
run('baseline-three-mutation-witness',java(reconciled)+[
    'com.m3.collections.M3FilteredListMutationWitness','--expect-broken'])
run('candidate-three-mutation-witness',java('candidate')+[
    'com.m3.collections.M3FilteredListMutationWitness','--expect-fixed'])

# The current upstream mask oracle is already repaired and remains unchanged.
# Its complete contract executes below on both owners; retain an independent
# unsigned-byte boundary table without changing the implementation or fixture.
states=[0,1,2,3,128,255]
truth=[{'index':i,'unsigned_state':state,'required_zero':True,
        'excluded_low_bit_clear':state % 2 == 0,'selected':state % 2 == 0}
       for i,state in enumerate(states)]
assert [row['index'] for row in truth if row['selected']]==[0,2,4]
(out/'MASK_BOUNDARY_TRUTH_TABLE.json').write_text(json.dumps(truth,indent=2)+'\n')

for name in contracts:
    if name=='M3RequiredExcludedMaskSelectorNativeContract':
        continue
    before=run('baseline-'+name,java(reconciled)+['com.m3.collections.'+name])
    after=run('candidate-'+name,java('candidate')+['com.m3.collections.'+name])
    assert before==after, name+' original observation changed'

for label in [reconciled,'candidate']:
    run(label+'-order-java',java(label)+['com.m3.collections.M3IndexOrderWindowContract','--java'])
    run(label+'-order-native',java(label,True)+['com.m3.collections.M3IndexOrderWindowContract','--native'])
    run(label+'-order-missing',java(label)+['com.m3.collections.M3IndexOrderWindowContract','--missing'])

for name, mode in [
    ('M3FilteredListsMaskBridgeProof',['--native']),
    ('M3LongIndexProjectionSelfTest',[]),
    ('M3LongIndexProjectionMaskOffsetProof',[]),
    ('M3NativeIndexRangeSelfTest',['native']),
    ('M3RequiredExcludedMaskSelectorNativeContract',[]),
    ('M3IndexedMultiValueOccurrenceSelfTest',[])]:
    run('candidate-native-'+name,java('candidate',True)+['com.m3.collections.'+name,*mode])
run('candidate-native-compactor',java('candidate',True)+['com.m3.collections.M3IndexMaskCompactorSelfTest','--native'])
run('candidate-mutation',java('candidate')+['com.m3.collections.M3FilteredListMutationProof'])
run('candidate-mutation-interpreted',java('candidate',False,True)+['com.m3.collections.M3FilteredListMutationProof'])

for owner in ['M3FilteredLists','M3FilteredViews','M3LongIndexProjection','M3SlotMap',
              'M3LinkedHashMap','M3TreeMap','M3WeakHashMap']:
    assert (out/(reconciled+'-api-'+owner+'.log')).read_bytes()==(out/('candidate-api-'+owner+'.log')).read_bytes()
bh=out/reconciled/'headers'; ch=out/'candidate/headers'
assert sorted(p.name for p in bh.iterdir())==sorted(p.name for p in ch.iterdir())
assert all(p.read_bytes()==(ch/p.name).read_bytes() for p in bh.iterdir())
for label in [reconciled,'candidate']:
    run(label+'-resources',java(label)+['com.m3.collections.M3IndexOrderWindowResourceProbe','sort','32768','100'])

(out/'RESULT.json').write_text(json.dumps({'status':'PASS','production_sources':len(main),
    'original_baseline_main_compilation':'PASS',
    'current_strict_sequenced_counterexample':'REPRODUCED_NSEE_INSTEAD_OF_UOE',
    'differential_control':'ONLY_M3FilteredViews_STRICT_SEQUENCED_OVERRIDES_RESTORED',
    'unchanged_existing_standalone_contracts':len(contracts)-1,'public_descriptors':'IDENTICAL',
    'generated_jni_headers':'IDENTICAL','native':'REAL_REQUIRED_CHECKED_JNI_UBSAN',
    'baseline_mutation_counterexample':'REPRODUCED','recipe_execution':'SEPARATE_GATE',
    'current_upstream_key_mask_and_entry_fixes':'PRESERVED_UNCHANGED',
    'unsigned_mask_boundary':'INDEPENDENT_TRUTH_TABLE_AND_CURRENT_UNCHANGED_CONTRACT',
    'source_kind':'ACTUAL_SDK_EMITTED' if args.emitted else 'REVIEWED_CANDIDATE_ONLY',
    'commands':len(records)},indent=2)+'\n')
print('COLLECTION_CANDIDATE_PASS',out,flush=True)
