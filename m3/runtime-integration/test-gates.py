#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
import json,os,pathlib,subprocess,time,selectors,re
root=pathlib.Path(__file__).resolve().parents[2]
jdk=pathlib.Path(os.environ['M3_TEST_JDK']);result=pathlib.Path(os.environ.get('M3_RESULTS',str(root/'m3/build/runtime-integration')))
java=str(jdk/'bin/java');flags=['-XX:+UnlockExperimentalVMOptions','-XX:+UseM3StringStorage','-XX:-CreateCoredumpOnCrash']
reports=[]
for label,opts in [
 ('dedup',['-XX:+UseStringDeduplication']),('share-on',['-Xshare:on']),('share-dump',['-Xshare:dump']),
 ('archive-exit',['-XX:ArchiveClassesAtExit='+str(result/'blocked-exit.jsa')]),
 ('record-dump',['-XX:+RecordDynamicDumpInfo']),('auto-archive',['-XX:+AutoCreateSharedArchive']),
 ('jvmci',['-XX:+EnableJVMCI']),('jfr',['-XX:StartFlightRecording=filename='+str(result/'blocked.jfr')]),
 ('jfr-options',['-XX:FlightRecorderOptions=stackdepth=64'])]:
 r=subprocess.run([java,*flags,*opts,'-version'],capture_output=True,text=True,timeout=30)
 (result/('gate-reject-'+label+'.log')).write_text(r.stdout+r.stderr)
 assert r.returncode!=0 and 'UseM3StringStorage does not support' in r.stderr,(label,r.returncode,r.stderr)
 reports.append({'case':label,'exit':r.returncode,'rejected':True})
for label,ordered in [('trailing-xcomp',[*flags,'-Xcomp']),('leading-xcomp',['-Xcomp',*flags]),('trailing-compiler',[*flags,'-XX:+UseCompiler'])]:
 r=subprocess.run([java,*ordered,'-XX:+PrintFlagsFinal','-cp',str(result/'classes'),'SegmentedGates'],capture_output=True,text=True,timeout=60)
 (result/('gate-'+label+'.log')).write_text(r.stdout+r.stderr)
 assert r.returncode==0 and 'SEGMENTED_GATES_PASS' in r.stdout,(label,r.returncode,r.stdout[-2000:],r.stderr)
 for name in ['UseCompiler','UseStringDeduplication','OptimizeStringConcat']:
  assert re.search(r'^\s*bool\s+'+name+r'\s+[:]?=\s+false\b',r.stdout,re.M),(label,name,r.stdout[-2000:])
 reports.append({'case':label,'exit':r.returncode,'gates_held':True})
p=subprocess.Popen([java,*flags,'-cp',str(result/'classes'),'SegmentedGates','hold'],stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
try:
 selector=selectors.DefaultSelector();selector.register(p.stdout,selectors.EVENT_READ);lines=[];deadline=time.monotonic()+60
 while time.monotonic()<deadline:
  if selector.select(timeout=1):
   line=p.stdout.readline();lines.append(line)
   if 'SEGMENTED_GATES_PASS' in line:break
   if not line and p.poll() is not None:raise AssertionError('hold JVM failed: '+''.join(lines))
 else:raise AssertionError('hold JVM timeout')
 r=subprocess.run([str(jdk/'bin/jcmd'),str(p.pid),'VM.cds','dynamic_dump',str(result/'blocked-dynamic.jsa')],capture_output=True,text=True,timeout=30)
 (result/'gate-dynamic-cds.log').write_text(r.stdout+r.stderr)
 assert 'CDS archive operations are unsupported with UseM3StringStorage' in r.stdout+r.stderr,(r.stdout,r.stderr)
 assert not (result/'blocked-dynamic.jsa').exists()
 reports.append({'case':'dynamic-cds','exit':r.returncode,'rejected':True})
 p.communicate(input='\n',timeout=30)
finally:
 if p.poll() is None:p.kill();p.wait()
(result/'gate-results.json').write_text(json.dumps(reports,indent=2)+'\n')
print('SEGMENTED_STARTUP_DYNAMIC_GATES_PASS cases='+str(len(reports)))
