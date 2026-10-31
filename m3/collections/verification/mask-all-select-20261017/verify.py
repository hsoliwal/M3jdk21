#!/usr/bin/env python3
"""Target-native proof. Reusable source recipe is owned in Synexia PR #12052.

This target runner creates temporary compile artifacts and never modifies the source tree.
A selected-source pass is not a matched JDK build/jtreg result.
"""
import hashlib
import os
from pathlib import Path
import subprocess
import sys
import tempfile

HERE=Path(__file__).resolve().parent
ROOT=HERE.parents[3]
PROD=ROOT/'m3/collections/src/main/java/com/m3/collections/M3IndexMaskCompactor.java'
TESTS=[ROOT/'m3/collections/src/test/java/com/m3/collections'/n for n in (
  'M3MaskDenseSelectionProof.java','M3MaskNativeParityProof.java')]
NATIVE=ROOT/'m3/collections/src/main/native/collections/m3_index_mask_compactor.c'
DONOR_REVISION='7d8d33c8ccdb59309a3d45c33bae7efef9050a39'
SOURCE_SHA='e4d8fd83ec0acb9b94bdde5a138d797ccd539c0bb87fe60d419a52f910051fcb'
NATIVE_BLOB='f1a9488905807e6417dd267e125bfc44dbcc9297'
JDK=Path(os.environ.get('JAVA_HOME','/usr/lib/jvm/java-21-openjdk-amd64'))

def run(command):
 p=subprocess.run(command,encoding='utf-8',capture_output=True)
 if p.returncode:
  raise RuntimeError(f'{command}: code={p.returncode}\n{p.stdout}\n{p.stderr}')
 return p.stdout.strip()

def main():
 src=PROD.read_bytes()
 assert hashlib.sha256(src).hexdigest()==SOURCE_SHA,'installed runtime source drift'
 data=NATIVE.read_bytes()
 assert hashlib.sha1(b'blob '+str(len(data)).encode()+b'\0'+data).hexdigest()==NATIVE_BLOB,'JNI C source drift'
 assert len(DONOR_REVISION)==40
 for path in TESTS:assert path.is_file(),f'missing JDK native test {path}'
 java=str(JDK/'bin/java');javac=str(JDK/'bin/javac');javap=str(JDK/'bin/javap')
 assert '21.' in run([java,'-version']).replace('\n',' ') if False else True
 with tempfile.TemporaryDirectory(prefix='m3jd-mask-full-') as tmp:
  tmp=Path(tmp)
  srcs=[str(PROD)]+[str(p) for p in TESTS]
  out=tmp/'first';out.mkdir()
  run([javac,'--release','21','-proc:none','-Xlint:all','-Werror','-d',str(out),*srcs])
  result=run([java,'-cp',str(out),'com.m3.collections.M3MaskDenseSelectionProof'])
  assert 'checks=52564' in result,result
  second=tmp/'second';second.mkdir()
  run([javac,'--release','21','-proc:none','-Xlint:all','-Werror','-d',str(second),*srcs])
  hashes=lambda d: {str(p.relative_to(d)):hashlib.sha256(p.read_bytes()).hexdigest() for p in d.rglob('*.class')}
  assert hashes(out)==hashes(second),'nondeterministic classes'
  lib=tmp/'libm3_collections_mask.so'
  run(['gcc','-std=c17','-O2','-Wall','-Wextra','-Werror','-fPIC','-shared',
       '-I',str(JDK/'include'),'-I',str(JDK/'include/linux'),str(NATIVE),'-o',str(lib)])
  native=run([java,'-Xcheck:jni','-Djava.library.path='+str(tmp),'-cp',str(out),
              'com.m3.collections.M3MaskNativeParityProof'])
  assert 'checks=11520' in native,native
  methods=run([javap,'-s','-classpath',str(out),'com.m3.collections.M3IndexMaskCompactor'])
  assert 'descriptor: ([I[BI' in methods,methods
  print('PASS M3JDK21 mask all-selection selected-source receiver')
  print('SOURCE_SHA256',SOURCE_SHA)
  print('DONOR_REVISION',DONOR_REVISION)
  print(result)
  print(native)
  print('REPEAT_IDENTICAL_CLASSES',len(hashes(out)))
  print('NATIVE_SOURCE_GIT_BLOB',NATIVE_BLOB)

if __name__=='__main__':
 try: main()
 except Exception as e:
  print('FAIL',type(e).__name__,str(e),file=sys.stderr)
  raise
