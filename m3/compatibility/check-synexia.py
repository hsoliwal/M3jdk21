#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
"""Compile exact Apache-2.0 test references; never import them into the shipped module."""
import hashlib,os,pathlib,subprocess,urllib.request
root=pathlib.Path(__file__).resolve().parents[1]
jdk=pathlib.Path(os.environ['M3_JDK'])
commit='64a2ea61c73b548413fed6686a9daeeb0b9b0564'
prefix='synexia-indexstring/src/main/java/com/synexia/indexstring/'
pins={'FrozenChars.java':'75264c85b4da2cd7d9193d8e245364efb53aa936',
      'FrozenBytes.java':'0e249745d0e96a1d9a8754a075f33cdf42e8fc0b'}
def git_blob_sha(data):
    header=('blob '+str(len(data))+'\\0').encode()
    return hashlib.sha1(header+data).hexdigest()

paths=[]
for name,expected in pins.items():
    path=root/'build/reference-src'/commit/'com/synexia/indexstring'/name
    path.parent.mkdir(parents=True,exist_ok=True)
    if not path.exists():
        data=urllib.request.urlopen('https://raw.githubusercontent.com/hsoliwal/com.synexia/'+commit+'/'+prefix+name,timeout=30).read()
        if git_blob_sha(data)!=expected:raise ValueError('Reference source blob pin mismatch: '+name)
        path.write_bytes(data)
    if git_blob_sha(path.read_bytes())!=expected:raise ValueError('Reference source blob pin mismatch: '+name)
    if not path.read_text().startswith('// SPDX-License-Identifier: Apache-2.0'):raise ValueError('Reference license marker changed')
    paths.append(str(path))
classes=root/'build/reference-test-classes'/commit;classes.mkdir(parents=True,exist_ok=True)
subprocess.run([str(jdk/'bin/javac'),'--release','21','--module-path',str(root/'build/com.m3.text.jar'),'--add-modules','com.m3.text','-d',str(classes),*paths,str(root/'compatibility/SynexiaContractTest.java')],check=True)
subprocess.run([str(jdk/'bin/java'),'-ea','--module-path',str(root/'build/com.m3.text.jar'),'--add-modules','com.m3.text','-cp',str(classes),'SynexiaContractTest'],check=True)
