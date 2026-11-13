"""Source and standalone jar custody; no downloaded/cloned collection sources."""
import argparse,hashlib,json,struct,zipfile,re,subprocess
from pathlib import Path, PurePosixPath
BASE=Path(__file__).resolve().parent
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()

# Frozen packet V1 is immutable historical custody, not the current source hash.
HISTORIC_PACKET_SHA256 = "da37bfacf6b8bfa91e5ded6bb74947c54c8d54a8c70f6b15cc58c1eb94208bea"
SUCCESSOR_PACKET = BASE.parent / "guava-minmax-source-custody" / "source-bindings.json"
WORKFLOW = ".github/workflows/m3-owned-guava-minmax-priority-queue.yml"
OWN_VERIFIER = "make/m3/collections/qualification/owned-guava-minmax-priority-queue-20261018/verify.py"
ORIGINAL_RUNNER = "make/m3/collections/qualification/owned-guava-minmax-priority-queue-20261018/run.py"
RUNNER_POST_SHA256 = "6faae312cf259d842d2257ac735efddd6d342ee111c514456e429223488edf85"

# Exact first-party compile closure found in original javac 21 diagnostics.
# These four helper files were absent from the frozen 130-source set. Preserve
# that set and require exact byte identities rather than a wildcard sourcepath.
CORE_HELPERS = (
    ("m3/collections/src/main/java/jdk/m3/collections/M3IndexSort.java", 4586, "7c5ef5729b064f1528858d839b996479d16fe3687799fe765a72aad2a8f27b3a"),
    ("m3/collections/src/main/java/jdk/m3/collections/M3LaneSearch.java", 2997, "d8629cba30a905d6bae1485f082b5d794aa819a800cd391f5ba1689736c4306c"),
    ("m3/collections/src/main/java/jdk/m3/collections/M3PriorityQueueGrowth.java", 1315, "5b610826f505cd02d330621749d4c397d91f9d93a71718e5c178b6aca07d9150"),
    ("m3/collections/src/main/java/jdk/m3/collections/M3ShadowStreams.java", 9377, "53ae1d0542371d155a0ed54a7ecf02a8bb55c749b8165ffa021882e4a020b9eb"),
)

def required_core_helpers(root, selected):
    if len(selected) != 130 or len(set(selected)) != len(selected):
        raise ValueError("Guava historical core130 source selection drift")
    paths = [path for path, _, _ in CORE_HELPERS]
    if len(paths) != len(set(paths)) or paths != sorted(paths):
        raise ValueError("Guava helper selection must be unique and ordered")
    if set(paths).intersection(selected):
        raise ValueError("Guava helper is already part of historical core130")
    admitted = []
    for relative, expected_bytes, expected_sha in CORE_HELPERS:
        path = root / relative
        if not path.is_file() or path.is_symlink():
            raise ValueError("Guava helper source missing or symlink: " + relative)
        if path.stat().st_size != expected_bytes or sha(path) != expected_sha:
            raise ValueError("Guava helper source custody drift: " + relative)
        admitted.append(path)
    return tuple(admitted)

CHANGES = {
    WORKFLOW: (2220, "9543ed2e6b8430e6a80888064750beda48e486b005bf125488cc95b513d3d4b9"),
    OWN_VERIFIER: (4276, "a8fd04cc98bf3f21e5f68958eae8f82e0b5fcedbc7c6d88f92ea778828bb980b"),
    ORIGINAL_RUNNER: (18203, "815464a4b2b759a3b7d5e47ca82d6ed6d09fa5eddacb0e24fcf5ee6646796679"),
}

def qualified_source_packet():
    """Admit exactly two source successors without changing the old 165-row packet."""
    historical = BASE / "source-bindings.json"
    if (not historical.is_file() or historical.is_symlink()
            or sha(historical) != HISTORIC_PACKET_SHA256):
        raise ValueError("historic Guava source packet identity drift")
    if not SUCCESSOR_PACKET.is_file() or SUCCESSOR_PACKET.is_symlink():
        raise ValueError("missing stable Guava source successor")
    original = json.loads(historical.read_bytes())
    candidate = json.loads(SUCCESSOR_PACKET.read_bytes())
    if (set(original) != set(candidate)
            or any(original[key] != candidate[key] for key in original if key != "sources")):
        raise ValueError("Guava packet metadata or schema changed")
    before, after = original["sources"], candidate["sources"]
    if (len(before) != 165 or len(after) != 165
            or sum(row["role"] == "CURRENT_CORE_CONTEXT" for row in before) != 131
            or sum(row["role"] == "NEW_PACKET_SOURCE" for row in before) != 34):
        raise ValueError("original Guava source census changed")
    changed = set()
    for previous, following in zip(before, after):
        path = previous["path"]
        if path != following.get("path") or previous["role"] != following.get("role"):
            raise ValueError("Guava source path/order/role changed")
        if path not in CHANGES:
            if previous != following:
                raise ValueError("unreviewed Guava source drift: " + path)
            continue
        expected_size, expected_hash = CHANGES[path]
        if (previous["role"] != "NEW_PACKET_SOURCE"
                or previous["bytes"] != expected_size
                or previous["sha256"] != expected_hash
                or set(previous) != set(following)
                or any(previous[key] != following[key]
                       for key in previous if key not in ("bytes", "sha256"))):
            raise ValueError("Guava historical successor preimage mismatch: " + path)
        if path == WORKFLOW:
            if (following["bytes"] != 2385
                    or following["sha256"] != "f324172b0f8cae62b1f9ee40333b755afc191fd28198ce1e236afd21118a479c"):
                raise ValueError("Guava workflow successor image differs")
        elif path == OWN_VERIFIER:
            owner = BASE / "verify.py"
            if (following["bytes"] != owner.stat().st_size
                    or following["sha256"] != sha(owner)):
                raise ValueError("Guava original verifier successor image differs")
        elif path == ORIGINAL_RUNNER:
            owner = BASE / "run.py"
            if (not owner.is_file() or owner.is_symlink()
                    or following["bytes"] != 19347
                    or following["sha256"] != RUNNER_POST_SHA256
                    or owner.stat().st_size != 19347
                    or sha(owner) != RUNNER_POST_SHA256):
                raise ValueError("Guava original runner successor image differs")
        else:
            raise ValueError("unknown Guava source successor: " + path)
        changed.add(path)
    if changed != set(CHANGES):
        raise ValueError("Guava successor coverage incomplete")
    return candidate

def verify(root,guava=None,context_root=None):
    spec=qualified_source_packet()
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
    required_core_helpers(root,spec['coreJavaPaths'])
    owner=root/spec['ownerPath'];payload=root/spec['recipePayloadPath'];assert owner.read_bytes()==payload.read_bytes(),'receiving postimage drift'
    if guava is not None:assert sha(guava)==spec['provider']['sha256'] and guava.stat().st_size==spec['provider']['bytes']
    return spec
def verify_module(jar,pom):
    import xml.etree.ElementTree as ET
    ns={'m':'http://maven.apache.org/POM/4.0.0'};tree=ET.parse(pom)
    deps={(node.findtext('m:groupId',namespaces=ns),node.findtext('m:artifactId',namespaces=ns)):node.findtext('m:version',namespaces=ns) for node in tree.findall('m:dependencies/m:dependency',ns)}
    assert deps[('com.m3','m3-collections')]=='0.1.0-SNAPSHOT' and deps[('com.google.guava','guava')]=='33.4.8-jre'
    stem='jdk/m3/collections/guava/M3MinMaxPriorityQueue';expected={stem+suffix+'.class' for suffix in ['', '$Builder','$Heap','$QueueIterator','$ComparatorOrdering','$ReverseOrdering','$NaturalOrdering','$ReverseNaturalOrdering']}
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
