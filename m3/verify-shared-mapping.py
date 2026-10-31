#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
"""Linux evidence that two warmed JVMs map the same read-only image inode."""
import json,os,pathlib,subprocess
root=pathlib.Path(__file__).resolve().parent
jdk=pathlib.Path(os.environ['M3_JDK'])
image=(root/(root/'build/english-image-path.txt').read_text().strip()).resolve()
command=[str(jdk/'bin/java'),'--module-path',str(root/'build/com.m3.text.jar'),'-m','com.m3.text/com.m3.text.LexiconTool','map',str(image),'5000']
processes=[];evidence=[]
try:
    for _ in range(2):
        process=subprocess.Popen(command,stdout=subprocess.PIPE,stderr=subprocess.PIPE,text=True)
        processes.append(process)
        line=process.stdout.readline().strip()
        if 'image=' not in line:raise RuntimeError('JVM did not report mapped image: '+line)
        evidence.append({'pid':process.pid,'report':line})
    inode=str(image.stat().st_ino)
    for item in evidence:
        lines=[line for line in pathlib.Path('/proc/'+str(item['pid'])+'/maps').read_text().splitlines() if str(image) in line]
        if not lines or any(line.split()[1]!='r--s' or line.split()[4]!=inode for line in lines):raise AssertionError(lines)
        item['maps']=lines
    for process in processes:
        _,err=process.communicate(timeout=15)
        if process.returncode:raise RuntimeError(err)
    output={'same_inode':inode,'image':image.name,'processes':evidence,'claim':'same read-only shared file mapping; not a measured PSS or memory-savings claim'}
    (root/'build/logs/shared-mapping.json').write_text(json.dumps(output,indent=2)+'\n')
    print('SHARED_MAPPING_PASS same_inode='+inode+' processes=2 read_only=true')
finally:
    for process in processes:
        if process.poll() is None:process.terminate();process.wait(timeout=10)
