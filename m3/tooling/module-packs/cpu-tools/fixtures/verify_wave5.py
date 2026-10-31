#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Run M3 CPU wave5 Java21 oracles in a fresh external output directory."""
import argparse,subprocess,tempfile,json,hashlib
from pathlib import Path
CLASSES=('M3BipartiteMatching','M3AhoCorasick','M3DataflowLiveness','M3CriticalPath')
TESTS={'M3MatchingSearchContract':179911,'M3DataflowScheduleContract':649078}
def main():
    p=argparse.ArgumentParser();p.add_argument('--owner',type=Path,required=True);p.add_argument('--out',type=Path,required=True)
    x=p.parse_args();owner=x.owner.resolve(strict=True);out=x.out.absolute()
    if out.exists() or out.is_relative_to(owner) or not out.parent.is_dir():raise ValueError('new external evidence directory required')
    root=owner/'src/com.m3.tools.cpu/com/m3/tools/cpu'
    sources=[root/(n+'.java') for n in CLASSES]+[owner/'fixtures'/(n+'.java') for n in TESTS]
    for f in sources:
        if not f.is_file() or f.is_symlink():raise ValueError('missing source '+str(f))
    seals={str(f.relative_to(owner)):hashlib.sha256(f.read_bytes()).hexdigest() for f in sources}
    with tempfile.TemporaryDirectory(prefix='m3-wave5-product-') as tmp:
        classes=Path(tmp)/'classes';classes.mkdir()
        result=subprocess.run(['javac','--release','21','-proc:none','-Xlint:all','-Werror','-d',str(classes),*(str(f) for f in sources)],capture_output=True,text=True,timeout=90)
        if result.returncode or result.stderr:raise RuntimeError(result.stderr)
        logs={}
        for mode in ('jit','interpreted'):
            for name,count in TESTS.items():
                cmd=['java','-Xcheck:jni']
                if mode=='interpreted':cmd.append('-Xint')
                cmd+=['-cp',str(classes),name]
                result=subprocess.run(cmd,capture_output=True,text=True,timeout=180)
                expected=('M3_WAVE5_MATCHING_SEARCH' if name.startswith('M3Matching') else 'M3_WAVE5_DATAFLOW_SCHEDULER')+'_PASS checks='+str(count)
                if result.returncode or result.stdout.strip()!=expected or result.stderr:raise RuntimeError(result.stdout+result.stderr)
                logs[mode+'/'+name]=expected
    if any(hashlib.sha256((owner/f).read_bytes()).hexdigest() != h for f,h in seals.items()):
        raise RuntimeError('source changed during verification')
    out.mkdir();report={'status':'LOCAL_JAVA21_JIT_XINT_PASS','checks_per_mode':828989,'checks':logs,'sha256':seals,'maven_sdk':'NOT_RUN','jmod':'NOT_RUN'}
    (out/'REPORT.json').write_text(json.dumps(report,indent=2,sort_keys=True)+'\n');print('M3_WAVE5_PRODUCT_PASS checks=828989 modes=2')
if __name__=='__main__':main()
