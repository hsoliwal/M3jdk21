# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Resolve observed mixed-workload timing variance, and check >2^31-bit capacity."""
from pathlib import Path
import argparse, hashlib, json, os, subprocess
p=argparse.ArgumentParser()
p.add_argument('--work',type=Path,required=True);p.add_argument('--jdk',type=Path,required=True)
a=p.parse_args();w=a.work.resolve();e=w/'evidence'
assert (e/'PARITY.json').is_file()
env=os.environ.copy();env['PATH']=str(a.jdk/'bin')+os.pathsep+env['PATH']
def run(name,cmd):
    cmd=list(map(str,cmd));log=e/(name+'.log');assert not log.exists(),name
    with log.open('w') as f:
        r=subprocess.Popen(cmd,env=env,stdout=f,stderr=subprocess.STDOUT)
        _,status,usage=os.wait4(r.pid,0);r.returncode=os.waitstatus_to_exitcode(status)
    (e/(name+'.json')).write_text(json.dumps({'command':cmd,'exit':r.returncode,'log_sha256':hashlib.sha256(log.read_bytes()).hexdigest(),'max_rss_kib':usage.ru_maxrss,'user_seconds':usage.ru_utime,'system_seconds':usage.ru_stime},indent=2)+'\n')
    print(name,r.returncode,'\n'.join(log.read_text().splitlines()[-2:]),flush=True)
    if r.returncode:raise SystemExit('HOLD '+name)
management=['--add-modules','java.management,jdk.management','--add-reads','java.base=java.management','--add-reads','java.base=jdk.management']
for state in ['baseline','snapshot']:
    stage=w/state
    run(state+'-30-large-capacity',['java','-Xmx512m','-Xcheck:jni','--patch-module','java.base='+str(stage/'classes')+os.pathsep+str(stage/'boot-classes'),'-Dtest.m3.masks.native.library='+str(stage/'libM3CollectionMasksQualification.so'),'java.util.M3CollectionMasksTest','large'])
cpu=sorted(os.sched_getaffinity(0))[0]
for fork in range(3):
    for state in (['baseline','snapshot'] if fork%2==0 else ['snapshot','baseline']):
        stage=w/state
        for size,density in [(65,'dense'),(65536,'all')]:
            base=['taskset','-c',str(cpu),'java','-Xms256m','-Xmx256m',*management,'--patch-module','java.base='+str(stage/'classes')+os.pathsep+str(stage/'bench-classes'),'-Dm3.bench.size='+str(size),'-Dm3.bench.density='+density]
            run(state+'-31-focused-'+str(size)+'-'+density+'-'+str(fork),[*base,'java.util.M3CollectionViewsBenchmark','integrated'])
