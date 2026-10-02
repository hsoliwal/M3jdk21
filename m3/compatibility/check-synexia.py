#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
"""Compile exact Apache-2.0 test references; never import them into the shipped module."""
import hashlib,os,pathlib,subprocess,urllib.request
root=pathlib.Path(__file__).resolve().parents[1]
jdk=pathlib.Path(os.environ['M3_JDK'])
commit='c6cb340d33323348fab94e8157842455e9b72bc2'
prefix='synexia-indexstring/src/main/java/com/synexia/indexstring/'
pins={'FrozenChars.java':'0c03ca0ed8b42212fa92e4d5f9325ba864f8d11928cd6a0b030b861f668f0657',
      'FrozenBytes.java':'cb50139594817d5cb2477c4d67148d4aed2174299f05cff9a3b24416139bc6b4'}
paths=[]
for name,expected in pins.items():
    path=root/'build/reference-src'/commit/'com/synexia/indexstring'/name
    path.parent.mkdir(parents=True,exist_ok=True)
    if not path.exists():
        data=urllib.request.urlopen('https://raw.githubusercontent.com/hsoliwal/com.synexia/'+commit+'/'+prefix+name,timeout=30).read()
        if hashlib.sha256(data).hexdigest()!=expected:raise ValueError('Reference source pin mismatch: '+name)
        path.write_bytes(data)
    if hashlib.sha256(path.read_bytes()).hexdigest()!=expected:raise ValueError('Reference source pin mismatch: '+name)
    if not path.read_text().startswith('// SPDX-License-Identifier: Apache-2.0'):raise ValueError('Reference license marker changed')
    paths.append(str(path))
classes=root/'build/reference-test-classes'/commit;classes.mkdir(parents=True,exist_ok=True)
subprocess.run([str(jdk/'bin/javac'),'--release','21','--module-path',str(root/'build/com.m3.text.jar'),'--add-modules','com.m3.text','-d',str(classes),*paths,str(root/'compatibility/SynexiaContractTest.java')],check=True)
subprocess.run([str(jdk/'bin/java'),'-ea','--module-path',str(root/'build/com.m3.text.jar'),'--add-modules','com.m3.text','-cp',str(classes),'SynexiaContractTest'],check=True)
