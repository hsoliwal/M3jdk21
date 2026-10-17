#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Real serial Java/C/JNI qualification of source-sealed candidates; no SDK emulation."""
import argparse,hashlib,json,os,shutil,subprocess,tempfile
from pathlib import Path
TASK=Path(__file__).resolve().parent
ROOT=TASK.parents[3]
RES=TASK/'src/main/resources/com/synexia/rewrite/mask-scan-budget-20261017'
J='m3/collections/src/main/java/com/m3/collections/M3IndexMaskWindow.java'
CO='m3/collections/src/main/java/com/m3/collections/M3IndexMaskCompactor.java'
C='m3/collections/src/main/native/collections/m3_index_mask_window.c'
TEST='m3/collections/src/test/java/com/m3/collections/M3MaskScanBudgetSelfTest.java'
EX='m3/collections/examples/MaskBudgetExample.java'
logs=[]
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
def run(args,negative=False):
    args=list(map(str,args));p=subprocess.run(args,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,timeout=120)
    logs.append('$ '+' '.join(args)+'\nexit='+str(p.returncode)+'\n'+p.stdout)
    if negative:
        if p.returncode==0 or not any(s in p.stdout for s in ['AssertionError','region budget check failed','IllegalStateException: invalid native mask-window cursor']):
            raise RuntimeError('negative control not detected: '+' '.join(args)+'\n'+p.stdout)
        print('Detected required negative:',Path(args[0]).name,flush=True)
    elif p.returncode:raise RuntimeError('failed command: '+' '.join(args)+'\n'+p.stdout)
    else: print(p.stdout,end='',flush=True)
    return p.stdout

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--apply',action='store_true');args=parser.parse_args()
    rows=[line.split('\t') for line in (RES/'manifest.tsv').read_text().splitlines()]
    evidence=TASK/'evidence';evidence.mkdir(exist_ok=True)
    try:
        with tempfile.TemporaryDirectory(prefix='m3-budget-') as directory:
            tmp=Path(directory);engine=tmp/'engine';engine.mkdir();pkg=TASK/'src/main/java/com/synexia/rewrite'
            run(['javac','--release','21','-Xlint:all','-Werror','-d',engine,pkg/'M3GpuToolJoinManifest.java',pkg/'MaskBudgetReplay.java'])
            candidate=tmp/'candidate';run(['java','-cp',engine,'com.synexia.rewrite.MaskBudgetReplay',ROOT,candidate])
            base=tmp/'base';new=tmp/'new';base.mkdir();new.mkdir()
            compile_args=['javac','--release','21','-proc:none','-Xlint:all','-Werror']
            run([*compile_args,'-d',base,ROOT/CO,TASK/'before'/J,TASK/'proof/LegacyWindowProof.java'])
            run([*compile_args,'-h',new,'-d',new,ROOT/CO,candidate/J,candidate/TEST,candidate/EX,TASK/'proof/LegacyWindowProof.java'])
            home=Path(shutil.which('javac')).resolve().parents[1]
            cc=['gcc','-std=c17','-Wall','-Wextra','-Werror','-Wpedantic','-fsanitize=undefined','-fno-sanitize-recover=undefined',
                '-I'+str(home/'include'),'-I'+str(home/'include/linux')]
            base_lib=tmp/'base-lib';lib=tmp/'lib';base_lib.mkdir();lib.mkdir()
            run([*cc,'-fPIC','-shared',TASK/'before'/C,'-o',base_lib/'libm3_collections_mask.so'])
            run([*cc,'-fPIC','-shared','-include',new/'com_m3_collections_M3IndexMaskWindow.h',candidate/C,'-o',lib/'libm3_collections_mask.so'])
            def java(classes,native,main,*rest,heap='32m',interp=False,negative=False):
                return run(['java','-Xcheck:jni','-Xmx'+heap,*(['-Xint'] if interp else []),'-Djava.library.path='+str(native),'-cp',classes,main,*rest],negative)
            baseline=java(base,base_lib,'LegacyWindowProof','native')
            # First serial production pass: old compiled Java clients against new C implementation.
            assert java(base,lib,'LegacyWindowProof','native')==baseline
            print('Serial pass 1: shared C core preserves original native ABI/results',flush=True)
            assert java(new,lib,'LegacyWindowProof','native')==baseline
            assert java(new,base_lib,'LegacyWindowProof','native')==baseline
            assert java(new,tmp/'missing','LegacyWindowProof')==baseline
            print('Serial pass 2: additive Java entry preserves original API results',flush=True)
            main='com.m3.collections.M3MaskScanBudgetSelfTest'
            for heap in ['32m','128m']:
                java(new,lib,main,'java',heap=heap);java(new,lib,main,'native',heap=heap)
            java(new,lib,main,'native',interp=True)
            java(new,tmp/'missing',main,'missing');java(new,base_lib,main,'legacy')
            first=java(new,tmp/'missing','MaskBudgetExample')
            assert java(new,lib,'MaskBudgetExample','native')==first
            # No public removal; the only additions are MAX_EXAMINED and scanBounded.
            old_api=run(['javap','-public','-classpath',base,'com.m3.collections.M3IndexMaskWindow'])
            new_api=run(['javap','-public','-classpath',new,'com.m3.collections.M3IndexMaskWindow'])
            old_lines=set(old_api.splitlines());new_lines=set(new_api.splitlines())
            assert old_lines<=new_lines
            additions=new_lines-old_lines
            assert len(additions)==2 and any('MAX_EXAMINED' in x for x in additions) and any('scanBounded(' in x for x in additions)
            for a,b in [('M3IndexMaskCompactor.class','M3IndexMaskCompactor.class'),('M3IndexMaskCompactor$Backend.class','M3IndexMaskCompactor$Backend.class')]:
                assert (base/'com/m3/collections'/a).read_bytes()==(new/'com/m3/collections'/b).read_bytes()
            # Existing scan source body is preserved exactly, not just the public signature.
            before=(TASK/'before'/J).read_text();after=(candidate/J).read_text()
            start=before.index('    public static long scan(');end=before.index('\n    /** Pure Java reference atom;',start)
            assert before[start:end].strip() in after
            region=tmp/'region'
            run([*cc,'-fsanitize=address,undefined','-I'+str((candidate/C).parent),TASK/'proof/region_budget.c','-o',region])
            run([region])
            # Both faulty source-budget handling and high-bit matching must be detected.
            source=(candidate/C).read_text()
            changes=[('end','if (max_examined >= 0 && max_examined < n - first) end = first + max_examined;',
                            '(void) max_examined; /* faulty ignored budget */'),
                     ('high-bit','(unsigned int) (unsigned char) marks[i]','((unsigned int) (unsigned char) marks[i] & 127u)')]
            for name,old,replacement in changes:
                assert old in source
                bad=tmp/name;bad.mkdir();badsource=bad/'m3_index_mask_window.c';badsource.write_text(source.replace(old,replacement))
                run([*cc,'-fPIC','-shared',badsource,'-o',bad/'libm3_collections_mask.so'])
                java(new,bad,main,'native',negative=True)
                if name=='end':
                    bad_region=bad/'region';run([*cc,'-I'+str(bad),TASK/'proof/region_budget.c','-o',bad_region]);run([bad_region],negative=True)
            # Admission is not publication; recheck all inputs before optional per-file installation.
            for path,before,after,template in rows:
                p=ROOT/path;current=sha(p) if p.exists() else 'ABSENT'
                if current not in [before,after]:raise RuntimeError('late drift '+path)
            if args.apply:
                for path,before,after,template in rows:
                    if template=='KEEP':continue
                    p=ROOT/path
                    if any(q.is_symlink() for q in [p,*p.parents]):raise RuntimeError('symlink target '+path)
                    p.parent.mkdir(parents=True,exist_ok=True)
                    stage=p.with_name(p.name+'.m3-candidate')
                    with stage.open('xb') as f:f.write((candidate/path).read_bytes())
                    os.replace(stage,p)
            for path,before,after,template in rows:
                if template!='KEEP' and (not (ROOT/path).exists() or sha(ROOT/path)!=after):raise RuntimeError('output not installed or drifted '+path)
            replay=tmp/'replay';run(['java','-cp',engine,'com.synexia.rewrite.MaskBudgetReplay',ROOT,replay])
            for path,before,after,template in rows:
                if template!='KEEP':assert (replay/path).read_bytes()==(candidate/path).read_bytes()
            (evidence/'GENERATED_SHA256.tsv').write_text('path\tsha256\n'+''.join(p+'\t'+a+'\n' for p,b,a,x in rows if x!='KEEP'))
            (evidence/'API_DELTA.txt').write_text('\n'.join(sorted(additions))+'\n')
            print('PASS: serial original/new ABI, source budgets, native reads, cancellation, resumption and fixed point. Actual Maven SDK/full reactor are independent gates.',flush=True)
    finally:(evidence/'qualification.log').write_text('\n'.join(logs))
if __name__=='__main__':main()
