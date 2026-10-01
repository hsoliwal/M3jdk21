#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
import os,pathlib,subprocess,time,json,hashlib,shutil,concurrent.futures
r=pathlib.Path(os.environ['M3_RESULTS']); java=str(pathlib.Path(os.environ['M3_TEST_JDK'])/'bin/java')
source=r/'shared-owner/arena.bin';shared=r/'shared-two-process.bin';shutil.copy2(source,shared)
before=hashlib.sha256(shared.read_bytes()).hexdigest();stat=shared.stat()
flags=['-ea','-esa','-XX:+UnlockExperimentalVMOptions','-XX:+UseM3StringStorage','-XX:-CreateCoredumpOnCrash','--add-opens','java.base/java.lang=ALL-UNNAMED', '-Djdk.mindex.lexicon='+str(shared),'-Dm3.expect.lexicon=true','-Dm3.shared.owner=true','-cp',str(r/'classes'),'MIndexIntegration','hold']
ps=[];lines=[[],[]]
try:
 for i in range(2):
  p=subprocess.Popen([java,*flags],stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,bufsize=1);ps.append(p)
 def read_ready(i):
  while True:
   line=ps[i].stdout.readline();lines[i].append(line)
   if line.startswith('READY '):return
   if not line:raise AssertionError('child failed: '+''.join(lines[i]))
 with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
  futures=[pool.submit(read_ready,i) for i in range(2)]
  try:
   for future in futures:future.result(timeout=300)
  except BaseException:
   for p in ps:
    if p.poll() is None:p.kill()
   raise
 assert hashlib.sha256(shared.read_bytes()).hexdigest()==before
 mapped=[];ids=[]
 for ls in lines:
  maps=[line.split() for line in ls if line.startswith('MAP ')]
  assert any(x[2]=='r--s' and int(x[5])==stat.st_ino for x in maps),maps
  ids.append(next(x.split('id=')[1].split()[0] for x in ls if x.startswith('READY ')))
  mapped.append(maps)
 assert ids[0]==ids[1]
 shared.unlink()
 for i,p in enumerate(ps):
  out,_=p.communicate('\n',timeout=60);lines[i].append(out)
  assert p.returncode==0 and 'MAPPING_LIFETIME_PASS' in out,lines[i]
 print('MINDEX_TWO_PROCESS_PASS same_inode='+str(stat.st_ino)+' same_atom_id='+ids[0]+' unlink_gc_retained=true')
 (r/'two-process.json').write_text(json.dumps({'inode':stat.st_ino,'sha256':before,'ids':ids,'maps':mapped,'unlink_gc_retained':True},indent=2)+'\n')
finally:
 for p in ps:
  if p.poll() is None:p.kill();p.wait()
 for i,ls in enumerate(lines):(r/f'two-process-{i}.log').write_text(''.join(ls))
