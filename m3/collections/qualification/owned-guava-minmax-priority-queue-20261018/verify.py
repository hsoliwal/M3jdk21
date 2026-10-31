"""Source and standalone jar custody; no downloaded/cloned collection sources."""
import argparse,hashlib,json,struct,zipfile,re,subprocess
from pathlib import Path, PurePosixPath
BASE=Path(__file__).resolve().parent
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def verify(root,guava=None,context_root=None):
    spec=json.loads((BASE/'source-bindings.json').read_text(encoding='utf-8'))
    # The original 131 core-context pins belong to the immutable source
    # tree at the recorded base commit. Live owners may evolve under further
    # separately qualified recipes; the packet artifacts remain hash-frozen.
    base=spec['baseCommit']
    assert re.fullmatch(r'[0-9a-f]{40}',base),'invalid original source commit'
    git_root=Path(context_root if context_root else root).resolve(strict=True)
    def git(*args):
        return subprocess.check_output(['git','-C',str(git_root),*args],stderr=subprocess.PIPE)
    assert git('rev-parse','--verify',base+'^{tree}').decode().strip()==spec['baseTree'],'historical source tree drift'
    for row in spec['sources']:
        relative=row['path']
        parts=PurePosixPath(relative)
        assert not parts.is_absolute() and '..' not in parts.parts and '\\' not in relative,relative
        if row.get('role')=='CURRENT_CORE_CONTEXT':
            current=git_root/relative
            assert current.is_file() and not current.is_symlink(),relative
            oid=git('rev-parse','--verify',base+':'+relative).decode().strip()
            assert re.fullmatch(r'[0-9a-f]{40}',oid) and oid==row['gitBlob'],relative
            data=git('cat-file','blob',oid)
        elif row.get('role')=='NEW_PACKET_SOURCE':
            current=root/relative
            assert current.is_file() and not current.is_symlink(),relative
            data=current.read_bytes()
        else:
            raise AssertionError('unexpected source role: '+relative)
        assert len(data)==row['bytes'] and hashlib.sha256(data).hexdigest()==row['sha256'],relative
    assert len(spec['coreJavaPaths'])==130
    owner=root/spec['ownerPath'];payload=root/spec['recipePayloadPath'];assert owner.read_bytes()==payload.read_bytes(),'receiving postimage drift'
    if guava is not None:assert sha(guava)==spec['provider']['sha256'] and guava.stat().st_size==spec['provider']['bytes']
    return spec
def verify_module(jar,pom):
    import xml.etree.ElementTree as ET
    ns={'m':'http://maven.apache.org/POM/4.0.0'};tree=ET.parse(pom)
    deps={(node.findtext('m:groupId',namespaces=ns),node.findtext('m:artifactId',namespaces=ns)):node.findtext('m:version',namespaces=ns) for node in tree.findall('m:dependencies/m:dependency',ns)}
    assert deps[('com.m3','m3-collections')]=='0.1.0-SNAPSHOT' and deps[('com.google.guava','guava')]=='33.4.8-jre'
    stem='com/m3/collections/guava/M3MinMaxPriorityQueue';expected={stem+suffix+'.class' for suffix in ['', '$Builder','$Heap','$QueueIterator','$ComparatorOrdering','$ReverseOrdering','$NaturalOrdering','$ReverseNaturalOrdering']}
    with zipfile.ZipFile(jar) as archive:
        actual={name for name in archive.namelist() if name.endswith('.class')};assert actual==expected,(actual,expected)
        for name in actual:
            data=archive.read(name);assert data[:4]==b'\xca\xfe\xba\xbe' and struct.unpack('>H',data[6:8])[0]==65
        for name in ['LICENSE.txt','NOTICE.txt','THIRD_PARTY_NOTICES.md']:assert archive.read('META-INF/'+name)
        assert not any(name.endswith(('.dll','.so','.dylib')) for name in archive.namelist())
    return {'status':'PASS','ownedClasses':len(expected),'majorVersion':65,'externalDependencies':deps.__len__(),'coreAndGuavaRemainExternal':True}
if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--root',type=Path,required=True);parser.add_argument('--context-root',type=Path);parser.add_argument('--guava',type=Path);parser.add_argument('--module-jar',type=Path);args=parser.parse_args()
    spec=verify(args.root,args.guava,args.context_root);result={'status':'PASS','sourceRows':len(spec['sources']),'selectedCoreJava':130,'copiedCoreSources':0}
    if args.module_jar:result['module']=verify_module(args.module_jar,args.root/spec['modulePath']/'pom.xml')
    print(json.dumps(result,sort_keys=True))
