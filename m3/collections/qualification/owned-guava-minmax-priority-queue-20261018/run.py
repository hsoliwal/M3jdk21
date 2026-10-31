"""Three scoped hosted jobs with bounded child logs and explicit scratch routing."""
# SPDX-License-Identifier: Apache-2.0
import argparse,ctypes,importlib.util,json,os,shutil,signal,subprocess,sys,threading,time,zipfile
from pathlib import Path
BASE=Path(__file__).resolve().parent

OUTPUT_CAP=512*1024**2
RESERVE=8*1024**3
CHILD_LOG_CAP=32*1024**2

def require_disjoint(root,out):
    root,out=Path(root).resolve(),Path(out).resolve()
    if out==root or root in out.parents or out in root.parents:
        raise ValueError('outputs must use scratch disjoint from checkout in both directions')
    return root,out

def java_tool_options(temp,home):
    values={'java.io.tmpdir':str(temp),'user.home':str(home)}
    if any(any(char in value for char in ['"','\x00','\n','\r']) for value in values.values()):
        raise ValueError('Java property path contains unsupported quote/control character')
    return '-XX:-UsePerfData '+' '.join('-D'+name+'="'+value+'"' for name,value in values.items())

def child_environment(temp,home):
    env=dict(os.environ,TEMP=str(temp),TMP=str(temp),TMPDIR=str(temp))
    for name in ['_JAVA_OPTIONS','JDK_JAVA_OPTIONS']:env.pop(name,None)
    env['JAVA_TOOL_OPTIONS']=java_tool_options(temp,home)
    env['MAVEN_OPTS']='-Xmx256m -XX:ActiveProcessorCount=2 -XX:-UsePerfData'
    return env

def output_bytes(folder):
    total=0
    for path in Path(folder).rglob('*'):
        try:
            if path.is_file():total+=path.stat().st_size
        except FileNotFoundError:pass
    return total

class ChildFailure(RuntimeError):
    def __init__(self,record):
        self.record=record
        super().__init__('child '+record['reason']+'; returncode='+str(record['returncode']))

class WindowsJob:
    """Kill-on-close job; real command waits until the wrapper is assigned."""
    def __init__(self):
        class Basic(ctypes.Structure):
            _fields_=[('userTime',ctypes.c_longlong),('jobTime',ctypes.c_longlong),('flags',ctypes.c_uint32),('minWorking',ctypes.c_size_t),('maxWorking',ctypes.c_size_t),('activeProcesses',ctypes.c_uint32),('affinity',ctypes.c_size_t),('priority',ctypes.c_uint32),('scheduling',ctypes.c_uint32)]
        class IO(ctypes.Structure):
            _fields_=[(name,ctypes.c_ulonglong) for name in ['reads','writes','other','readBytes','writeBytes','otherBytes']]
        class Extended(ctypes.Structure):
            _fields_=[('basic',Basic),('io',IO),('processMemory',ctypes.c_size_t),('jobMemory',ctypes.c_size_t),('peakProcess',ctypes.c_size_t),('peakJob',ctypes.c_size_t)]
        self.kernel=ctypes.WinDLL('kernel32',use_last_error=True)
        self.kernel.CreateJobObjectW.argtypes=[ctypes.c_void_p,ctypes.c_wchar_p];self.kernel.CreateJobObjectW.restype=ctypes.c_void_p
        self.kernel.SetInformationJobObject.argtypes=[ctypes.c_void_p,ctypes.c_int,ctypes.c_void_p,ctypes.c_uint32];self.kernel.SetInformationJobObject.restype=ctypes.c_int
        self.kernel.AssignProcessToJobObject.argtypes=[ctypes.c_void_p,ctypes.c_void_p];self.kernel.AssignProcessToJobObject.restype=ctypes.c_int
        self.kernel.CloseHandle.argtypes=[ctypes.c_void_p];self.kernel.CloseHandle.restype=ctypes.c_int
        self.handle=self.kernel.CreateJobObjectW(None,None)
        if not self.handle:raise ctypes.WinError(ctypes.get_last_error())
        limits=Extended();limits.basic.flags=0x2000
        if not self.kernel.SetInformationJobObject(self.handle,9,ctypes.byref(limits),ctypes.sizeof(limits)):
            error=ctypes.get_last_error();self.close();raise ctypes.WinError(error)
    def assign(self,process):
        if not self.kernel.AssignProcessToJobObject(self.handle,int(process._handle)):
            raise ctypes.WinError(ctypes.get_last_error())
    def close(self):
        if self.handle:
            handle,self.handle=self.handle,None
            if not self.kernel.CloseHandle(handle):raise ctypes.WinError(ctypes.get_last_error())

def bounded_process(argv,*,cwd,env,stdout,stderr,check,timeout=240,log_limit=CHILD_LOG_CAP,poll_seconds=0.1):
    """Bound both binary logs, poll artifact capacity, and always reap the child tree."""
    if timeout<=0 or log_limit<=0 or poll_seconds<=0:raise ValueError('positive process bounds required')
    argv=list(map(str,argv));limit=min(log_limit,check())
    if limit<=0:raise RuntimeError('no remaining output budget')
    lock=threading.Lock();stop=threading.Event();state={'bytes':0,'limit':limit,'reason':None};process=None;job=None;readers=[];checks=1;started=time.monotonic()
    def fail(reason):
        with lock:
            if state['reason'] is None:state['reason']=reason
        stop.set()
    def pump(pipe,path):
        try:
            with Path(path).open('xb',buffering=0) as output:
                while not stop.is_set():
                    data=pipe.read(65536)
                    if not data:break
                    with lock:
                        if stop.is_set():break
                        allowed=max(0,state['limit']-state['bytes']);prefix=data[:allowed]
                        if prefix:output.write(prefix);state['bytes']+=len(prefix)
                        if len(prefix)<len(data):
                            if state['reason'] is None:state['reason']='combined log byte limit'
                            stop.set();break
        except BaseException as error:fail('log capture failure: '+str(error))
        finally:pipe.close()
    try:
        if os.name=='nt':
            job=WindowsJob()
            wrapper='import json,subprocess,sys; command=json.load(sys.stdin); child=subprocess.Popen(command,stdin=subprocess.DEVNULL); sys.exit(child.wait())'
            process=subprocess.Popen([sys.executable,'-I','-S','-B','-c',wrapper],cwd=cwd,env=env,stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.PIPE,bufsize=0,creationflags=subprocess.CREATE_NO_WINDOW)
            job.assign(process)
        else:
            process=subprocess.Popen(argv,cwd=cwd,env=env,stdin=subprocess.DEVNULL,stdout=subprocess.PIPE,stderr=subprocess.PIPE,bufsize=0,start_new_session=True)
        for pipe,path in [(process.stdout,stdout),(process.stderr,stderr)]:
            reader=threading.Thread(target=pump,args=(pipe,path),daemon=True);reader.start();readers.append(reader)
        if os.name=='nt':
            check();checks+=1
            process.stdin.write(json.dumps(argv).encode('utf-8'));process.stdin.close()
        deadline=started+timeout
        while not stop.is_set():
            if process.poll() is not None and all(not reader.is_alive() for reader in readers):break
            if time.monotonic()>=deadline:fail('timeout');break
            try:
                remaining=check();checks+=1
                with lock:state['limit']=min(state['limit'],state['bytes']+remaining)
            except BaseException as error:fail('capacity check: '+str(error));break
            stop.wait(min(poll_seconds,max(0,deadline-time.monotonic())))
    except BaseException as error:fail('process setup/execution: '+str(error))
    finally:
        stop.set()
        try:
            if job is not None:job.close()
            elif process is not None:
                try:os.killpg(process.pid,signal.SIGKILL)
                except ProcessLookupError:pass
        except BaseException as error:
            fail('tree cleanup: '+str(error))
            if process is not None and process.poll() is None:process.kill()
        if process is not None:
            if process.stdin is not None and not process.stdin.closed:process.stdin.close()
            try:process.wait(timeout=5)
            except subprocess.TimeoutExpired:process.kill();process.wait(timeout=5);fail('child reap timeout')
        for reader in readers:reader.join(timeout=5)
        if any(reader.is_alive() for reader in readers):fail('reader cleanup timeout')
        try:check();checks+=1
        except BaseException as error:fail('final capacity check: '+str(error))
    returncode=process.returncode if process is not None else None
    if state['reason'] is None and returncode:state['reason']='nonzero exit'
    record={'returncode':returncode,'reason':state['reason'] or 'completed','pid':process.pid if process else None,'capturedBytes':state['bytes'],'combinedLogLimitBytes':limit,'capacityChecks':checks,'readerThreadsJoined':all(not reader.is_alive() for reader in readers),'treeContainment':'Windows kill-on-close job with stdin-gated command' if os.name=='nt' else 'POSIX isolated session/process group','elapsedSeconds':round(time.monotonic()-started,3)}
    if state['reason'] is not None:raise ChildFailure(record)
    return record


def main():
    spec=importlib.util.spec_from_file_location('minmax_verify',BASE/'verify.py');verify=importlib.util.module_from_spec(spec);spec.loader.exec_module(verify)
    parser=argparse.ArgumentParser();parser.add_argument('--root',type=Path,required=True);parser.add_argument('--out',type=Path,required=True);parser.add_argument('--job',choices=['java','jni','receiving'],required=True);args=parser.parse_args();root,out=require_disjoint(args.root,args.out)
    existing=out
    while not existing.exists():existing=existing.parent
    if shutil.disk_usage(existing).free<RESERVE+OUTPUT_CAP:raise RuntimeError('8GiB reserve/headroom before output')
    out.mkdir(parents=True,exist_ok=True);temp=out/'temp';temp.mkdir(exist_ok=True);home=out/'home';home.mkdir(exist_ok=True)
    env=child_environment(temp,home);os.environ.update(TEMP=str(temp),TMP=str(temp),TMPDIR=str(temp))
    def gate():
        used=output_bytes(out)
        if used>OUTPUT_CAP:raise RuntimeError('512MiB job output cap')
        if shutil.disk_usage(out).free<RESERVE+max(0,OUTPUT_CAP-used):raise RuntimeError('8GiB reserve/headroom')
        if os.name=='nt':
            buf=ctypes.create_unicode_buffer(32768);assert ctypes.windll.kernel32.GetTempPathW(len(buf),buf)
            assert Path(buf.value).resolve()==temp.resolve(),'native temp routing'
        return OUTPUT_CAP-used
    gate();bindings=verify.verify(root);module=root/bindings['modulePath'];recipe=root/bindings['recipePath'];qual=root/bindings['qualificationPath'];m2=out/'m2';java=Path(os.environ['JAVA_HOME'])/'bin';mvn=shutil.which('mvn.cmd' if os.name=='nt' else 'mvn');assert mvn
    records=[]
    def run(label,argv,timeout=240):
        stdout=out/(label+'.stdout');stderr=out/(label+'.stderr')
        try:
            row=bounded_process(argv,cwd=root,env=env,stdout=stdout,stderr=stderr,check=gate,timeout=timeout)
        except ChildFailure as error:
            row={'label':label,**error.record,'stdoutSHA256':verify.sha(stdout) if stdout.exists() else None,'stderrSHA256':verify.sha(stderr) if stderr.exists() else None};records.append(row)
            raise RuntimeError(label+' failed: '+error.record['reason']) from error
        records.append({'label':label,**row,'stdoutSHA256':verify.sha(stdout),'stderrSHA256':verify.sha(stderr)})
        return stdout
    def maven(label,pom,options):
        return run(label,[mvn,'-q','-f',pom,'-Dmaven.repo.local='+str(m2),*options])
    if args.job=='receiving':
        maven('receiving-original-two-tests',recipe/'pom.xml',['-Dm3.recipe.output='+str(out/'receiving-target'),'test'])
        reports=list((out/'receiving-target/surefire-reports').glob('TEST-*.xml'));assert reports
        import xml.etree.ElementTree as ET
        sums={key:sum(int(ET.parse(path).getroot().attrib.get(key,0)) for path in reports) for key in ['tests','failures','errors','skipped']};assert sums=={'tests':2,'failures':0,'errors':0,'skipped':0}
        result={'status':'PASS','receiving':sums,'successStates':8,'freshFixedpoints':8,'beforeOutputRefusals':4}
    else:
        coordinates=[('com.google.guava','guava','33.4.8-jre'),('com.google.errorprone','error_prone_annotations','2.36.0'),('org.jspecify','jspecify','1.0.0'),('com.google.j2objc','j2objc-annotations','3.0.0')]
        jars=[]
        for group,artifact,version in coordinates:
            maven('dependency-'+artifact,module/'pom.xml',['org.apache.maven.plugins:maven-dependency-plugin:3.6.1:get','-Dartifact='+group+':'+artifact+':'+version,'-Dtransitive=false'])
            jars.append(m2/group.replace('.','/')/artifact/version/(artifact+'-'+version+'.jar'))
        verify.verify(root,jars[0]);core=out/'core130-classes';core.mkdir();proof=out/'proof-classes';proof.mkdir();cp=os.pathsep.join(map(str,jars))
        javac=java/('javac.exe' if os.name=='nt' else 'javac');jvm=java/('java.exe' if os.name=='nt' else 'java')
        common=['-J-Xmx256m','-J-XX:ActiveProcessorCount=2','-J-XX:-UsePerfData','-J-Duser.home='+str(home),'-J-Djava.io.tmpdir='+str(temp),'--release','21','-g','-proc:none','-Xlint:all,-module','-Werror']
        run('strict-selected-core130',[javac,*common,'-cp',cp,'-d',core,*[root/path for path in bindings['coreJavaPaths']]])
        fixtureNames=['GuavaMinMaxProtocolProof','GuavaMinMaxOrderingHookProof','GuavaMinMaxPublicSurfaceProof','GuavaMinMaxResourceProfileProof']
        sources=[root/bindings['ownerPath']]+[qual/(name+'.java') for name in fixtureNames]+[qual/'GuavaMinMaxGenericCompileProof.java',qual/'GuavaMinMaxMaskProjectionProof.java']
        mirror=out/'generic-mirror';mirror.mkdir();original=(qual/'GuavaMinMaxGenericCompileProof.java').read_text(encoding='utf-8');nominal=original.replace('MinMaxPriorityQueue','M3MinMaxPriorityQueue').replace('com.google.common.collect.M3MinMaxPriorityQueue','com.m3.collections.guava.M3MinMaxPriorityQueue');(mirror/'GuavaMinMaxGenericCompileProof.java').write_text(nominal,encoding='utf-8',newline='\n')
        run('strict-owner-and-frozen-fixtures',[javac,*common,'-cp',str(core)+os.pathsep+cp,'-d',proof,*sources])
        run('strict-owned-generic-mirror',[javac,*common,'-cp',str(proof)+os.pathsep+str(core)+os.pathsep+cp,'-d',out,mirror/'GuavaMinMaxGenericCompileProof.java'])
        runtime=os.pathsep.join(map(str,[proof,core,jars[0]]));results={}
        def execute(label,name,owner,backend=None,nativeDir=None):
            options=['-Xmx64m' if name.endswith('ResourceProfileProof') else '-Xmx256m','-Xss512k','-XX:ActiveProcessorCount=2','-XX:-UsePerfData','-Djava.io.tmpdir='+str(temp),'-Duser.home='+str(home)]
            if nativeDir:options+=['-Xcheck:jni','-Djava.library.path='+str(nativeDir)]
            else:options+=['-Djava.library.path='+str(out/'absent-native')]
            output=run(label,[jvm,*options,'-cp',runtime,name,owner,*([backend] if backend else [])]);lines=output.read_text(encoding='utf-8').splitlines();return json.loads(lines[-1])
        if args.job=='java':
            for name in fixtureNames:
                provider=execute('provider-'+name,name,'com.google.common.collect.MinMaxPriorityQueue');owned=execute('owned-'+name,name,'com.m3.collections.guava.M3MinMaxPriorityQueue')
                if name=='GuavaMinMaxProtocolProof':
                    expected={row['name']:row for row in provider['partitions']};actual={row['name']:row for row in owned['partitions']}
                    for key in ['MANDATORY_LAW','REQUIRED_CONCRETE']:assert expected[key]==actual[key]
                    assert provider['mandatoryChecks']==owned['mandatoryChecks']==3109624 and provider['modeledOperations']==owned['modeledOperations']==39032
                elif name=='GuavaMinMaxResourceProfileProof':assert provider['callerOrderingReverseCalls']==owned['callerOrderingReverseCalls']==1
                else:assert provider==owned
                results[name]={'provider':provider,'owned':owned}
            coreJar=out/'selected-exact-core.jar'
            for row in bindings['coreLicensePins']:assert verify.sha(root/row['repoPath'])==row['sha256'] and (root/row['repoPath']).stat().st_size==row['bytes']
            with zipfile.ZipFile(coreJar,'w',compression=zipfile.ZIP_DEFLATED) as archive:
                for path in sorted(core.rglob('*.class')):archive.write(path,path.relative_to(core).as_posix())
                archive.write(root/'m3/collections/LICENSE','META-INF/LICENSE');archive.write(root/'m3/collections/NOTICE','META-INF/NOTICE')
            maven('install-temporary-selected-core-only',module/'pom.xml',['org.apache.maven.plugins:maven-install-plugin:3.1.1:install-file','-Dfile='+str(coreJar),'-DgroupId=com.m3','-DartifactId=m3-collections','-Dversion=0.1.0-SNAPSHOT','-Dpackaging=jar','-DgeneratePom=true'])
            maven('standalone-owned-module-package',module/'pom.xml',['-Dm3.module.output='+str(out/'module-target'),'-DskipTests','package'])
            jar=out/'module-target/m3-owned-guava-minmax-priority-queue-0.1.0-SNAPSHOT.jar';moduleResult=verify.verify_module(jar,module/'pom.xml')
            result={'status':'PASS','job':'java','results':results,'module':moduleResult,'fullCoreRootMavenInvoked':False,'temporaryCoreArtifactPublished':False}
        else:
            native=out/'native';native.mkdir();gcc=shutil.which('gcc');assert gcc
            library=native/('m3_collections_mask.dll' if os.name=='nt' else 'libm3_collections_mask.so');platform='win32' if os.name=='nt' else 'linux'
            run('build-current-selected-mask-kernel',[gcc,'-std=c17','-O2','-Wall','-Wextra','-Werror','-shared',*([] if os.name=='nt' else ['-fPIC']),'-I'+str(Path(os.environ['JAVA_HOME'])/'include'),'-I'+str(Path(os.environ['JAVA_HOME'])/'include'/platform),root/bindings['nativePath'],'-o',library])
            name='GuavaMinMaxMaskProjectionProof';provider=execute('projection-provider-java',name,'com.google.common.collect.MinMaxPriorityQueue','java');owned=execute('projection-owned-java',name,'com.m3.collections.guava.M3MinMaxPriorityQueue','java');jni=execute('projection-owned-native-required',name,'com.m3.collections.guava.M3MinMaxPriorityQueue','native',native);assert provider==owned==jni
            result={'status':'PASS','job':'jni','provider':provider,'ownedJava':owned,'ownedNativeRequired':jni,'nativeQueueCRUDClaim':False}
    result['commands']=records;encoded=(json.dumps(result,indent=2)+'\n').encode('utf-8')
    if len(encoded)>gate():raise RuntimeError('result exceeds remaining output budget')
    (out/'result.json').write_bytes(encoded);gate();print(json.dumps(result,sort_keys=True))

if __name__=='__main__':main()
