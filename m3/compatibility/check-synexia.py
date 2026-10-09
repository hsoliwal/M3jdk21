#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
"""Compile exact Apache-2.0 test references; never import them into the shipped module."""
import base64,hashlib,json,os,pathlib,subprocess,urllib.request
from urllib.error import HTTPError
root=pathlib.Path(__file__).resolve().parents[1]
jdk=pathlib.Path(os.environ['M3_JDK'])
commit='64a2ea61c73b548413fed6686a9daeeb0b9b0564'
prefix='synexia-indexstring/src/main/java/com/synexia/indexstring/'
pins={'FrozenChars.java':'75264c85b4da2cd7d9193d8e245364efb53aa936',
      'FrozenBytes.java':'0e249745d0e96a1d9a8754a075f33cdf42e8fc0b'}
SOURCE_REPOSITORY='hsoliwal/com.synexia'

def git_blob_sha(data):
    header=b'blob '+str(len(data)).encode()+bytes([0])
    return hashlib.sha1(header+data).hexdigest()

def fetch_source(name):
    token=os.environ.get('SYNEXIA_READ_TOKEN','').strip()
    if token:
        endpoint=('https://api.github.com/repos/'+SOURCE_REPOSITORY+'/contents/'+prefix+name
                  +'?ref='+commit)
        request=urllib.request.Request(endpoint,headers={
            'Accept':'application/vnd.github+json',
            'Authorization':'Bearer '+token,
            'X-GitHub-Api-Version':'2022-11-28',
            'User-Agent':'M3JDK-Synexia-compatibility'})
        with urllib.request.urlopen(request,timeout=30) as response:
            payload=json.load(response)
        if payload.get('encoding')!='base64' or not payload.get('content'):
            raise ValueError('Synexia Contents API returned non-base64 content: '+name)
        return base64.b64decode(''.join(payload['content'].split()),validate=True)
    endpoint='https://raw.githubusercontent.com/'+SOURCE_REPOSITORY+'/'+commit+'/'+prefix+name
    request=urllib.request.Request(endpoint,headers={'User-Agent':'M3JDK-Synexia-compatibility'})
    try:
        with urllib.request.urlopen(request,timeout=30) as response:
            return response.read()
    except HTTPError as error:
        if error.code==404:
            raise RuntimeError(
                'Synexia donor is private; configure the encrypted SYNEXIA_READ_TOKEN '
                'secret with read-only Contents access to '+SOURCE_REPOSITORY) from error
        raise
paths=[]
for name,expected in pins.items():
    path=root/'build/reference-src'/commit/'com/synexia/indexstring'/name
    path.parent.mkdir(parents=True,exist_ok=True)
    if not path.exists():
        data=fetch_source(name)
        if git_blob_sha(data)!=expected:raise ValueError('Reference source pin mismatch: '+name)
        path.write_bytes(data)
    if git_blob_sha(path.read_bytes())!=expected:raise ValueError('Reference source pin mismatch: '+name)
    if not path.read_text().startswith('// SPDX-License-Identifier: Apache-2.0'):raise ValueError('Reference license marker changed')
    paths.append(str(path))
classes=root/'build/reference-test-classes'/commit;classes.mkdir(parents=True,exist_ok=True)
subprocess.run([str(jdk/'bin/javac'),'--release','21','--module-path',str(root/'build/com.m3.text.jar'),'--add-modules','com.m3.text','-d',str(classes),*paths,str(root/'compatibility/SynexiaContractTest.java')],check=True)
subprocess.run([str(jdk/'bin/java'),'-ea','--module-path',str(root/'build/com.m3.text.jar'),'--add-modules','com.m3.text','-cp',str(classes),'SynexiaContractTest'],check=True)
