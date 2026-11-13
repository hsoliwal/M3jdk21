# Copyright 2026 Hitesh Soliwal and contributors.
# SPDX-License-Identifier: Apache-2.0
"""Replay the unchanged core build in isolated historical/current source cohorts.

Reuses the original joined-runtime markers and historical/current shadow pattern.
No historical source-changing recipe is applied to the newer product tree.
"""
from pathlib import Path,PurePosixPath
import argparse
import hashlib
import json
import os
import re
import signal
import subprocess
import time

TARGET='make/m3/tooling/joined-range-directory/current-baseline/CURRENT_BUILD_INPUTS.json'
def ident(raw):
    return dict(bytes=len(raw),sha256=hashlib.sha256(raw).hexdigest(),gitBlob=hashlib.sha1(b'blob '+str(len(raw)).encode()+b'\0'+raw).hexdigest())
def relative(name):
    p=PurePosixPath(name)
    if not name or p.is_absolute() or '\\' in name or ':' in name or any(s in ('','.','..') for s in name.split('/')):raise ValueError('unsafe source path')
    return Path(*p.parts)
def regular(path):
    if not path.is_file() or any(p.is_symlink() or p.is_junction() for p in [path,*path.parents]):raise ValueError('missing/redirected source: '+str(path))
    return path.read_bytes()
def admit(rows,observed):
    expected={r['path']:r for r in rows}
    if len(expected)!=len(rows) or set(expected)!=set(observed):raise ValueError('exact complete source ownership required')
    for name,row in expected.items():
        relative(name);raw=observed[name]
        if not isinstance(raw,bytes) or any(ident(raw)[k]!=row[k] for k in ['bytes','sha256','gitBlob']):raise ValueError('source identity drift: '+name)
    return True
def main():
    ap=argparse.ArgumentParser()
    for key in ['crate','emitted','inputs','out','jdk']:ap.add_argument('--'+key,type=Path,required=True)
    ap.add_argument('--historical-workspace',type=Path,required=True)
    ap.add_argument('--bash',required=True);ap.add_argument('--phase',choices=['controls','runtime'],required=True);a=ap.parse_args()
    exp=json.loads(regular(a.crate/'EXPECTED.json'))['targets'][0];raw=regular(a.emitted/TARGET)
    assert ident(raw)['sha256']==exp['afterSha256'] and ident(raw)['gitBlob']==exp['afterGit']
    current=json.loads(raw);oldraw=regular(a.crate/'contract/ORIGINAL_BUILD_INPUTS.json');old=json.loads(oldraw)
    assert ident(oldraw)['gitBlob']==current['historicalBaseline']['manifestGit']=='0ea37209f97eda757f17214f73d6a3850b01c56b'
    assert ident(oldraw)['sha256']==current['historicalBaseline']['manifestSha256']
    assert current['schema']=='M3_CURRENT_CORE_BASELINE_INPUTS_V1' and current['commit']=='1b5e64844fdb755ac8a7aee563e68c15e0126ced'
    assert old['commit']==current['historicalBaseline']['commit']=='73f59218dc4ef1a3ef79aad990e77273db2a2d60'
    assert len(current['files'])==59 and len(old['files'])==58
    now={r['path']:regular(a.inputs/'current'/relative(r['path'])) for r in current['files']}
    actual={p.relative_to(a.inputs/'current').as_posix() for p in (a.inputs/'current').rglob('*') if p.is_file() and not p.relative_to(a.inputs/'current').as_posix().startswith('m3/build/')}
    assert actual==set(now)
    before={}
    for row in old['files']:
        name=row['path'];body=now.get(name)
        before[name]=body if body is not None and ident(body)['gitBlob']==row['gitBlob'] else regular(a.inputs/'historical-overlays'/relative(name))
    admit(current['files'],now);admit(old['files'],before)
    changed=sorted(name for name in before if before[name]!=now[name]);assert changed==current['historicalBaseline']['overlays'] and len(changed)==7
    assert set(now)-set(before)=={'m3/core/test/jdk/m3/text/M3NumberSpaceConstructorProof.java'}
    a.out.mkdir(parents=True,exist_ok=True)
    if a.phase=='controls':
        cases=['current-accepted','historical-accepted']
        def reject(rows,observed,label):
            try:admit(rows,observed)
            except ValueError:cases.append(label)
            else:raise AssertionError('unexpected source admission: '+label)
        for name in now:
            drift=dict(now);drift[name]+=b'\nunknown drift\n';reject(current['files'],drift,'current-drift:'+name)
            missing=dict(now);del missing[name];reject(current['files'],missing,'current-missing:'+name)
        for label,rows,values in [('current',current['files'],now),('historical',old['files'],before)]:
            extra=dict(values);extra['unowned.java']=b'unowned';reject(rows,extra,label+'-extra')
        reject(current['files'],before,'historical-is-not-current');reject(old['files'],now,'current-is-not-historical')
        for name in changed:
            mixed=dict(now);mixed[name]=before[name];reject(current['files'],mixed,'stale-current:'+name)
            mixed=dict(before);mixed[name]=now[name];reject(old['files'],mixed,'future-historical:'+name)
        for name in ['/outside','../outside','a\\b','C:outside']:
            try:relative(name)
            except ValueError:cases.append('unsafe-path-refused')
            else:raise AssertionError('unsafe path accepted')
        assert len(cases)==142
        result=dict(status='PASS_EXACT_SOURCE_COHORT_CONTROLS',cases=len(cases),accepted=2,refused=140,labels=cases)
    else:
        constants=json.loads(regular(a.crate/'contract/ORIGINAL_BUILD_CONSTANTS.json'));assert len(constants['MARKERS'])==16 and len(constants['MODES'])==4
        records=[];cohorts={};env=dict(os.environ)
        for key in ['BASH_ENV','JAVA_TOOL_OPTIONS','JDK_JAVA_OPTIONS','_JAVA_OPTIONS']:env.pop(key,None)
        def stop(child):
            if child.poll() is not None:return
            if os.name=='nt':subprocess.run(['taskkill','/PID',str(child.pid),'/T','/F'],stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=15,check=False)
            else:os.killpg(child.pid,signal.SIGKILL)
            child.wait(timeout=15)
        def command(name,argv,cwd,command_env,stdin=None):
            log=a.out/(name+'.log');assert not log.exists();start=time.monotonic();child=None
            try:
                with log.open('xb') as output:
                    kwargs=dict(creationflags=subprocess.CREATE_NEW_PROCESS_GROUP) if os.name=='nt' else dict(start_new_session=True)
                    child=subprocess.Popen([str(s) for s in argv],cwd=cwd,env=command_env,stdin=subprocess.PIPE if stdin is not None else subprocess.DEVNULL,stdout=output,stderr=subprocess.STDOUT,**kwargs)
                    if stdin is not None:
                        child.stdin.write(stdin);child.stdin.close()
                    while child.poll() is None:
                        assert time.monotonic()-start<600 and log.stat().st_size<128*1024,'bounded core replay deadline/output gate'
                        time.sleep(.1)
                assert child.returncode==0,'original command failed; retain '+str(log)
            finally:
                if child is not None:stop(child)
                records.append(dict(name=name,argv=[str(x) for x in argv],cwd=str(cwd),exit=child.returncode if child else None,parentReaped=child is not None and child.poll() is not None,log=log.name,identity=ident(regular(log)),elapsedNs=int((time.monotonic()-start)*1e9)))
                (a.out/'commands.json').write_bytes((json.dumps(records,indent=2)+'\n').encode())
            return regular(log).decode('utf-8','replace')
        suffix='.exe' if os.name=='nt' else '';java=a.jdk/'bin'/('java'+suffix)
        command('jdk-version',[java,'-XX:-UsePerfData','-version'],a.out,env)
        wrapper=b'''set -euo pipefail
cd -- "$M3_BOUNDED_COHORT/m3"
M3_GUARDED_BUILD_ROOT="$(pwd -P)/build"
m3_checked_paths() {
  local target resolved
  for target in "$@"; do
    case "$target" in -*) continue;; esac
    resolved=$(realpath -m -- "$target")
    case "$resolved" in "$M3_GUARDED_BUILD_ROOT"/?*) ;; *) return 73;; esac
    printf 'M3_VERIFIED_FS_TARGET %s\\n' "$resolved"
  done
}
rm() { m3_checked_paths "$@" && command rm "$@"; }
mv() { m3_checked_paths "$@" && command mv "$@"; }
rmdir() { m3_checked_paths "$@" && command rmdir "$@"; }
source "$M3_BOUNDED_COHORT/make/m3/build.sh"
'''
        for label,values,rows in [('historical',before,old['files']),('current',now,current['files'])]:
            directory=a.historical_workspace if label=='historical' else a.inputs/'current'
            directory.mkdir(parents=True,exist_ok=True)
            for name,body in values.items():
                dest=directory/relative(name);dest.parent.mkdir(parents=True,exist_ok=True)
                if dest.exists():assert regular(dest)==body
                else:dest.write_bytes(body)
            expected_java={name for name in values if name.endswith('.java')}
            actual_java={p.relative_to(directory).as_posix() for domain in ['m3/core/src','m3/core/test'] for p in (directory/domain).rglob('*.java')}
            assert actual_java==expected_java,'unknown Java source in reused workspace'
            previous=directory/'m3/build'
            if previous.exists():
                old_logs=[dict(path=p.relative_to(previous).as_posix(),**ident(regular(p)),content=regular(p).decode('utf-8')) for p in sorted((previous/'logs').rglob('*')) if p.is_file()]
                (a.out/(label+'-prior-build-logs.json')).write_bytes((json.dumps(old_logs,indent=2)+'\n').encode())
                jar=previous/'jdk.m3.text.jar'
                if jar.exists():(a.out/(label+'-prior-build.jar')).write_bytes(regular(jar))
            temp=a.out/(label+'-temp');home=a.out/(label+'-home');temp.mkdir();home.mkdir()
            child_env=dict(env,M3JDK=a.jdk.as_posix(),M3_BOUNDED_COHORT=directory.as_posix())
            child_env['JAVA_TOOL_OPTIONS']='-Xmx128m -XX:-UsePerfData -Djava.io.tmpdir='+str(temp)+' -Duser.home='+str(home)
            fence=wrapper.rsplit(b'\nsource ',1)[0]+b'''\nm3_checked_paths build/owned/source build/owned/destination
if m3_checked_paths ../outside; then exit 74; fi
if m3_checked_paths build/../../outside; then exit 74; fi
'''
            command(label+'-filesystem-fence',[a.bash,'--noprofile','--norc','-c',fence.decode(),'qualification'],directory,child_env)
            text=command(label+'-original-build',[a.bash,'--noprofile','--norc','-c',wrapper.decode(),str(directory/'make/m3/build.sh')],directory,child_env)
            counts={marker:[line for line in text.splitlines() if re.match(re.escape(marker)+r' checks=[1-9][0-9]*(?:\s|$)',line)] for marker in constants['MARKERS']}
            assert all(len(lines)==4 for lines in counts.values()),'all16 original markers must execute in four modes'
            compiled={p.relative_to(directory/'m3/build/classes').as_posix():ident(regular(p)) for p in sorted((directory/'m3/build/classes').rglob('*.class'))}
            assert len(compiled)>=27
            for p in (directory/'m3/build/classes').rglob('*.class'):
                data=regular(p);assert data[:4]==bytes.fromhex('cafebabe') and int.from_bytes(data[6:8],'big')==65
            logs=[p for p in (directory/'m3/build/logs').rglob('*') if p.is_file()];assert len(logs)==66
            bundle=[dict(path=p.relative_to(directory).as_posix(),**ident(regular(p)),content=regular(p).decode('utf-8')) for p in sorted(logs)]
            assert sum(row['bytes'] for row in bundle)<256*1024
            (a.out/(label+'-complete-build-logs.json')).write_bytes((json.dumps(bundle,indent=2)+'\n').encode())
            for row in rows:assert ident(regular(directory/relative(row['path'])))=={k:row[k] for k in ['bytes','sha256','gitBlob']}
            cohorts[label]=dict(status='PASS',sourceFiles=len(rows),compiledClasses=compiled,originalPrograms=16,jvmModes=4,contracts=counts,completeLogs=len(logs),jar=ident(regular(directory/'m3/build/jdk.m3.text.jar')),sourceInputsUnchanged=True)
        assert set(cohorts['historical']['compiledClasses'])==set(cohorts['current']['compiledClasses'])
        result=dict(status='PASS_ORIGINAL_MODULAR_BUILD_HISTORICAL_AND_CURRENT',cohorts=cohorts,commands=records,scope='16 original main programs x4 modes x2 explicit source cohorts; omitted standalone/patch/native tests not claimed')
    result.update(authority=False,productChanges=0,currentCommit=current['commit'],historicalCommit=old['commit'])
    (a.out/(a.phase+'-RESULT.json')).write_bytes((json.dumps(result,indent=2)+'\n').encode())
    print(json.dumps(dict(status=result['status'],phase=a.phase,authority=False)))

if __name__=='__main__':main()
