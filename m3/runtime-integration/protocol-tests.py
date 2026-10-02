#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
import hashlib,json,os,pathlib,struct,subprocess
r=pathlib.Path(os.environ['M3_RESULTS']);java=str(pathlib.Path(os.environ['M3_TEST_JDK'])/'bin/java')
lex=bytearray((r/'test.m3lex').read_bytes());cases={}
x=bytearray(lex);x[-1]^=1;cases['bad-sha']=x
x=bytearray(lex);struct.pack_into('>I',x,64+12*2+8,42);x[32:64]=hashlib.sha256(x[:32]+x[64:]).digest();cases['sealed-wrong-java-hash']=x
x=bytearray(lex);x[64+12*3:64+12*4]=x[64+12*2:64+12*3];x[32:64]=hashlib.sha256(x[:32]+x[64:]).digest();cases['sealed-duplicate']=x
x=bytearray((r/'shared-owner/arena.bin').read_bytes());x[233]^=1;cases['owner-bad-crc']=x
cases['owner-incomplete']=bytearray((r/'shared-owner/arena.bin').read_bytes()[:-1])
results=[]
for name,data in cases.items():
 path=r/(name+'.bin');path.write_bytes(data)
 args=[java,'-ea','-XX:+UnlockExperimentalVMOptions','-XX:+UseM3StringStorage','-XX:-CreateCoredumpOnCrash','--add-opens','java.base/java.lang=ALL-UNNAMED','-Djdk.mindex.lexicon='+str(path),'-Dm3.expect.lexicon=false','-cp',str(r/'classes'),'MIndexStringInvariant']
 p=subprocess.run(args,capture_output=True,text=True,timeout=60);(r/('protocol-'+name+'.log')).write_text(p.stdout+p.stderr)
 assert p.returncode==0 and 'INVARIANT_PASS lexicon=false' in p.stdout,(name,p.returncode,p.stdout,p.stderr)
 results.append({'case':name,'fallback_verified':True})
(r/'protocol-results.json').write_text(json.dumps(results,indent=2)+'\n');print('MINDEX_PROTOCOL_PASS cases='+str(len(results)))
