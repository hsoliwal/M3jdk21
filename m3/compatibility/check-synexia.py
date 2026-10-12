#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
"""Compile pinned Apache-2.0 Synexia test references; never ship them in M3."""
import hashlib,os,pathlib,subprocess
root=pathlib.Path(__file__).resolve().parents[1]
jdk=pathlib.Path(os.environ['M3_JDK'])
commit='64a2ea61c73b548413fed6686a9daeeb0b9b0564'
pins={'FrozenChars.java':'75264c85b4da2cd7d9193d8e245364efb53aa936',
      'FrozenBytes.java':'0e249745d0e96a1d9a8754a075f33cdf42e8fc0b'}
def git_blob_sha(data):
    header=b'blob '+str(len(data)).encode()+bytes([0])
    return hashlib.sha1(header+data).hexdigest()

fixture=root/'compatibility'/'synexia-reference'
source_dir=fixture/'src'/'com'/'synexia'/'indexstring'
paths=[]
for name,expected in pins.items():
    path=source_dir/name
    data=path.read_bytes()
    if git_blob_sha(data)!=expected:raise ValueError('Reference source blob pin mismatch: '+name)
    if not data.startswith(b'// SPDX-License-Identifier: Apache-2.0'):raise ValueError('Reference license marker changed')
    paths.append(str(path))
license_text=(fixture/'LICENSE-2.0.txt').read_text(encoding='utf-8')
if 'Apache License' not in license_text or 'Version 2.0' not in license_text:
    raise ValueError('Synexia Apache-2.0 license copy missing or invalid')
shim=source_dir/'MIndexAddressedView.java'
mapped_test=source_dir/'SynexiaMappedStorageTest.java'
classes=root/'build/reference-test-classes'/commit
classes.mkdir(parents=True,exist_ok=True)
subprocess.run([str(jdk/'bin/javac'),'--release','21','--module-path',str(root/'build/com.m3.text.jar'),'--add-modules','com.m3.text','-d',str(classes),*paths,str(shim),str(mapped_test),str(root/'compatibility/SynexiaContractTest.java')],check=True)
subprocess.run([str(jdk/'bin/java'),'-ea','--module-path',str(root/'build/com.m3.text.jar'),'--add-modules','com.m3.text','-cp',str(classes),'SynexiaContractTest'],check=True)
subprocess.run([str(jdk/'bin/java'),'-ea','--module-path',str(root/'build/com.m3.text.jar'),'--add-modules','com.m3.text','-cp',str(classes),'com.synexia.indexstring.SynexiaMappedStorageTest'],check=True)
