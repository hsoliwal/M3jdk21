#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
"""Compile pinned Apache-2.0 Synexia test references; never ship them in M3."""
import hashlib,os,pathlib,subprocess
root=pathlib.Path(__file__).resolve().parents[1]
jdk=pathlib.Path(os.environ['M3JDK'])
commit='64a2ea61c73b548413fed6686a9daeeb0b9b0564'
pins={'FrozenChars.java':'791fffa3f9938e5266663c1cb256cc4d00f8c0f5',
      'FrozenBytes.java':'22656cfaa3ccf0c6b214f214c92324d7f3ea55be'}
def git_blob_sha(data):
    header=b'blob '+str(len(data)).encode()+bytes([0])
    return hashlib.sha1(header+data).hexdigest()

fixture=root/'compatibility'/'m3-reference'
source_dir=fixture/'src'/'com'/'synexia'/'indexstring'
paths=[]
for name,expected in pins.items():
    path=source_dir/name
    data=path.read_bytes()
    if git_blob_sha(data)!=expected:raise ValueError('Reference source blob pin mismatch: '+name)
    if not data.startswith(b'// SPDX-License-Identifier: Apache-2.0'):raise ValueError('Reference license marker changed')
    paths.append(str(path))
license_bytes=(fixture/'LICENSE-2.0.txt').read_bytes()
if git_blob_sha(license_bytes)!='29f81d812f3e768fa89638d1f72920dbfd1413a8':
    raise ValueError('Synexia Apache-2.0 license blob pin mismatch')
shim=source_dir/'MIndexAddressedView.java'
mapped_test=source_dir/'M3MappedStorageTest.java'
classes=root/'build/reference-test-classes'/commit
classes.mkdir(parents=True,exist_ok=True)
subprocess.run([str(jdk/'bin/javac'),'--release','21','--module-path',str(root/'build/jdk.m3.text.jar'),'--add-modules','jdk.m3.text','-d',str(classes),*paths,str(shim),str(mapped_test),str(root/'compatibility/M3ContractTest.java')],check=True)
subprocess.run([str(jdk/'bin/java'),'-ea','--module-path',str(root/'build/jdk.m3.text.jar'),'--add-modules','jdk.m3.text','-cp',str(classes),'M3ContractTest'],check=True)
subprocess.run([str(jdk/'bin/java'),'-ea','--module-path',str(root/'build/jdk.m3.text.jar'),'--add-modules','jdk.m3.text','-cp',str(classes),'jdk.internal.m3.indexstring.M3MappedStorageTest'],check=True)
