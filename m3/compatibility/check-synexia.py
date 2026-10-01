#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
"""Fetch one exact Apache-2.0 test reference; never import it into the shipped module."""
import hashlib,os,pathlib,subprocess,urllib.request
root=pathlib.Path(__file__).resolve().parents[1]
jdk=pathlib.Path(os.environ['M3_JDK'])
commit='9c36c18a7778e6096148a3bbee8f2af7575138b4'
relative='synexia-indexstring/src/main/java/com/synexia/indexstring/FrozenChars.java'
expected='42ce1c8ac4dc83a850b900b3c5a0b60de98410495c7da0145ff99cc9d19e950f'
path=root/'build/reference-src/com/synexia/indexstring/FrozenChars.java'
path.parent.mkdir(parents=True,exist_ok=True)
if not path.exists():path.write_bytes(urllib.request.urlopen('https://raw.githubusercontent.com/hsoliwal/com.synexia/'+commit+'/'+relative,timeout=30).read())
if hashlib.sha256(path.read_bytes()).hexdigest()!=expected:raise ValueError('FrozenChars source pin mismatch')
if not path.read_text().startswith('// SPDX-License-Identifier: Apache-2.0'):raise ValueError('Reference license marker changed')
classes=root/'build/reference-test-classes';classes.mkdir(exist_ok=True)
subprocess.run([str(jdk/'bin/javac'),'--release','21','--module-path',str(root/'build/com.m3.text.jar'),'--add-modules','com.m3.text','-d',str(classes),str(path),str(root/'compatibility/SynexiaContractTest.java')],check=True)
subprocess.run([str(jdk/'bin/java'),'-ea','--module-path',str(root/'build/com.m3.text.jar'),'--add-modules','com.m3.text','-cp',str(classes),'SynexiaContractTest'],check=True)
