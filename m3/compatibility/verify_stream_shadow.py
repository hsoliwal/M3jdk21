# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Bounded target qualification; does not claim a full source-built JDK image."""
from pathlib import Path
import argparse,hashlib,json,shutil,subprocess
p=argparse.ArgumentParser()
p.add_argument('--root',type=Path,required=True)
p.add_argument('--jdk',type=Path,required=True)
p.add_argument('--output',type=Path,required=True)
a=p.parse_args();root=a.root.resolve();out=a.output.resolve();out.mkdir(parents=True,exist_ok=False)
paths=['java/io/InputStream.java','java/io/OutputStream.java','java/io/ShadowInputStream.java','java/util/concurrent/ProgressMonitor.java','java/util/concurrent/ProgressMonitors.java','jdk/internal/m3/io/M3IO.java']
patchsrc=out/'patch-src';files=[];seals={}
for relative in paths:
 source=root/'src/java.base/share/classes'/relative
 target=patchsrc/relative;target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(source,target);files.append(str(target));seals[str(source.relative_to(root))]=hashlib.sha256(source.read_bytes()).hexdigest()
commands=[]
def run(name,args):
 with (out/(name+'.log')).open('w') as log:
  r=subprocess.run([str(x) for x in args],stdout=log,stderr=subprocess.STDOUT)
 commands.append({'name':name,'command':[str(x) for x in args],'exit':r.returncode})
 (out/'commands.json').write_text(json.dumps(commands,indent=2)+'\n')
 if r.returncode:raise SystemExit('failed: '+name)
run('java-version',[a.jdk/'bin/java','-version'])
run('compile-java-base',[a.jdk/'bin/javac','-Xlint:all','-Werror','--patch-module','java.base='+str(patchsrc),'-d',out/'patch',*files])
streamtest=root/'test/jdk/java/io/InputStream/M3StreamProgressShadow.java'
run('compile-stream-test',[a.jdk/'bin/javac','-Xlint:all','-Werror','--patch-module','java.base='+str(out/'patch'),'-d',out/'tests',streamtest])
for mode,flags in [('mixed',[]),('interpreted',['-Xint'])]:
 run('stream-'+mode,[a.jdk/'bin/java',*flags,'-Xcheck:jni','--patch-module','java.base='+str(out/'patch'),'-cp',out/'tests','M3StreamProgressShadow'])
filetest=root/'m3/file/src/test/java/com/m3/file/M3FileTraversalTest.java'
file_sources=sorted((root/'m3/file/src/main/java').rglob('*.java'))
run('compile-file-module',[a.jdk/'bin/javac','-Xlint:all,-module','-Werror','-d',out/'file-module',*file_sources])
run('compile-traversal-test',[a.jdk/'bin/javac','-Xlint:all','-Werror','-cp',out/'file-module','-d',out/'tests',filetest])
run('traversal',[a.jdk/'bin/java','-Xcheck:jni','--module-path',out/'file-module','--add-modules','com.m3.file','--patch-module','com.m3.file='+str(out/'tests'),'-m','com.m3.file/com.m3.file.M3FileTraversalTest'])
for s in [streamtest,filetest,*file_sources]:
 seals[str(s.relative_to(root))]=hashlib.sha256(s.read_bytes()).hexdigest()
(out/'source-seals.json').write_text(json.dumps(seals,indent=2,sort_keys=True)+'\n')
print('STREAM_SHADOW_TARGET_PASS commands='+str(len(commands)))
