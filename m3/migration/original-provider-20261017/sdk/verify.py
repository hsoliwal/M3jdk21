#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Closed SDK custody; adapts existing Synexia verifier, never a mapping/schema validator."""
from __future__ import annotations
import argparse, copy, hashlib, json, os, re, tempfile
from pathlib import Path, PurePosixPath
import xml.etree.ElementTree as ET
ROOT = Path(os.path.abspath(__file__)).parent
CRATE = "m3-original-mapping-provider-20261017"
RESOURCE = "src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/" + CRATE + "/"
SUITE = "com.m3.rewrite.backport.M3OriginalCliProviderRecipeTest"
BASELINE_MANIFEST = RESOURCE + "baseline-pre.tsv"
BASELINE_MANIFEST_SHA = "11d69c05d4e4dd0ef27c8a2195827c62b5dd48a49bef34943f4c6a11fbc4899c"
BASELINE_FILES, BASELINE_BYTES = 55, 795620
METHODS = ["allMutableStatesBothOrdersAndFixedPoint","exactRecipeResultsDeliveredToDeclaredOutput","everyUnknownTargetRefusesAfterItsPositiveBaseline","everyDuplicateTargetRefusesAfterItsPositiveBaseline","everyRequiredTargetMissingRefusesAfterItsPositiveBaseline","everyNonPlainTextTargetRefusesAfterItsPositiveBaseline","everyChangedAfterScanTargetRefusesAfterItsPositiveBaseline","sealedManifestPathSafetyAndConvergedIdentity","deliveryPrechecksForeignContentsAndAllSymlinkComponents","currentMappingReconciliationPreservesNewFieldsAndReviewedMigration","frozenPreBaselineRemainsIndependentOfInstalledPost"]
def digest(data):
    return hashlib.sha256(data).hexdigest()
def git_blob(data):
    return hashlib.sha1(b"blob " + str(len(data)).encode("ascii") + b"\0" + data).hexdigest()
def unique(pairs):
    result = {}
    for key, value in pairs:
        if key in result: raise ValueError("duplicate JSON key")
        result[key] = value
    return result
def parsed(data):
    def reject(value): raise ValueError("nonfinite JSON")
    return json.loads(data, object_pairs_hook=unique, parse_constant=reject)
def relative(name):
    if not isinstance(name,str) or not name or "\\" in name or ":" in name or any(ord(c)<32 for c in name):
        raise ValueError("invalid path")
    p=PurePosixPath(name)
    if p.is_absolute() or any(x in ("", ".", "..") or x.lower()==".git" for x in name.split("/")):
        raise ValueError("unsafe path")
    return p
def safe(base, name):
    relative(name)
    base=Path(base).absolute()
    if any(p.is_symlink() for p in [base,*base.parents]): raise ValueError("redirected root")
    current=base
    for part in PurePosixPath(name).parts:
        current=current/part
        if current.is_symlink(): raise ValueError("redirected path")
    return current
def workflow_contexts(data):
    # Admission for this fixed one-job/four-step producer; not a general YAML parser.
    # GitHub forbids runner in jobs.<id>.env and permits it in steps.env/with.
    text=data.decode("utf-8",errors="strict")
    if "\n    env:\n" in text or text.count("\n        env:\n")!=4:
        raise ValueError("runner-dependent paths must be in all four step environments")
    for line in text.splitlines():
        if "runner." in line and len(line)-len(line.lstrip(" "))<10:
            raise ValueError("runner context outside the supported step scope")
def validate_packet(pins, files):
    if pins.get("schema")!="M3_ORIGINAL_MAPPING_PROVIDER_SDK_V1" or pins.get("snapshotProviderGitBlob")!="4a38e39d0755462d40c2b22548b87abde938ea0b":
        raise ValueError("SDK authority drift")
    rows=pins.get("files",[])
    names=[r["path"] for r in rows]
    if not 10<=len(rows)<=256 or len(names)!=len(set(names)) or set(files)!=set(names):
        raise ValueError("closed SDK inventory drift")
    for row in rows:
        name=row["path"];relative(name);data=files[name]
        if len(data)!=row["bytes"] or digest(data)!=row["sha256"] or git_blob(data)!=row["gitBlob"]:
            raise ValueError("SDK source drift: "+name)
    if "pins.json" in names: raise ValueError("self-pinning cycle")
    for required in ["pom.xml","verify.py","src/main/java/com/m3/rewrite/backport/M3Jdk21HashPinnedTextSnapshotRecipe.java","src/test/java/com/m3/rewrite/backport/M3OriginalCliProviderRecipeTest.java",RESOURCE+"manifest.tsv",BASELINE_MANIFEST]:
        if required not in files: raise ValueError("required SDK input absent")
    provider=next(r for r in rows if r["path"]=="src/main/java/com/m3/rewrite/backport/M3Jdk21HashPinnedTextSnapshotRecipe.java")
    if provider["gitBlob"]!=pins["snapshotProviderGitBlob"]: raise ValueError("provider replacement")
    manifest=[]
    for line in files[RESOURCE+"manifest.tsv"].decode("utf-8",errors="strict").splitlines():
        if not line or line.startswith("#"): continue
        cells=line.split("\t")
        if len(cells)!=4: raise ValueError("manifest columns")
        path,before,after,resource=cells;relative(path)
        if before!="ABSENT" and not re.fullmatch("[0-9a-f]{64}",before): raise ValueError("preimage")
        if not re.fullmatch("[0-9a-f]{64}",after) or not re.fullmatch("[A-Za-z0-9_.-]+",resource):
            raise ValueError("postimage/resource")
        raw=files[RESOURCE+resource]
        raw.decode("utf-8",errors="strict")
        if digest(raw)!=after: raise ValueError("postimage hash")
        if before not in ("ABSENT",after) and digest(files[RESOURCE+"before-"+resource])!=before:
            raise ValueError("preimage fixture hash")
        manifest.append({"path":path,"before":before,"after":after,"resource":resource})
    paths=[r["path"] for r in manifest]
    if paths!=sorted(set(paths)) or manifest!=pins["targets"]: raise ValueError("manifest pin/order drift")
    if sum(r["before"]=="ABSENT" for r in manifest)!=4 or sum(r["before"] not in ("ABSENT",r["after"]) for r in manifest)!=2:
        raise ValueError("expected four additions and two exact changes")
    workflow=next(row for row in manifest if row["path"]==".github/workflows/m3-original-mapping-provider-qualification.yml")
    if workflow["before"]!="faf6a553557508404de8b644ae7ff17ffaf45c74d5b0cb6f3df86c583a0b2bc0":
        raise ValueError("workflow must change from its exact qualified preimage")
    workflow_contexts(files[RESOURCE+workflow["resource"]])
    frozen_pre(files)
    return manifest
def traversal_error(error):
    raise ValueError("closed inventory traversal failed") from error
def regular_data(path, maximum=16*1024**2):
    path=Path(path).absolute()
    if any(p.is_symlink() for p in [path,*path.parents]) or not path.is_file() or path.stat().st_size>maximum:
        raise ValueError("unsafe or oversized file")
    with path.open("rb") as stream:data=stream.read(maximum+1)
    if len(data)>maximum: raise ValueError("file grew beyond budget")
    return data
def inputs():
    pins_data=regular_data(safe(ROOT,"pins.json"),1024**2)
    pins=parsed(pins_data)
    actual={}
    for directory,names,files in os.walk(ROOT,followlinks=False,onerror=traversal_error):
        folder=Path(directory)
        for name in names[:]:
            path=folder/name
            if path.is_symlink(): raise ValueError("SDK directory symlink")
            if folder==ROOT and name=="target": names.remove(name)
        for name in files:
            path=folder/name
            if path.is_symlink() or not path.is_file(): raise ValueError("unsafe SDK source")
            relative_name=path.relative_to(ROOT).as_posix()
            if relative_name!="pins.json": actual[relative_name]=regular_data(path)
        if len(actual)>256 or sum(len(v) for v in actual.values())>16*1024**2:
            raise ValueError("SDK source budget")
    validate_packet(pins,actual)
    return pins,pins_data,actual
def validate_report(data: bytes, classname: str, methods: list[str]) -> dict[str, int]:
    root = ET.fromstring(data)
    if root.tag != "testsuite":
        raise ValueError("expected one Surefire testsuite")
    counts = {key: int(root.get(key, "-1"))
              for key in ("tests", "failures", "errors", "skipped")}
    if counts != {"tests": len(methods), "failures": 0, "errors": 0, "skipped": 0}:
        raise ValueError("SDK suite did not execute all required gates successfully")
    cases = root.findall("testcase")
    if len(cases) != len(methods) or {case.get("name") for case in cases} != set(methods):
        raise ValueError("SDK test inventory drift")
    for case in cases:
        if case.get("classname") != classname:
            raise ValueError("SDK test class drift")
        if any(case.find(tag) is not None for tag in ("failure", "error", "skipped")):
            raise ValueError("SDK test failure, error or skip present")
    return counts
def receiver(repository,pins):
    for row in pins["targets"]:
        path=safe(repository,row["path"])
        if not path.exists():
            if row["before"]!="ABSENT": raise ValueError("missing required current source: "+row["path"])
        elif not path.is_file() or digest(regular_data(path)) not in {row["before"],row["after"]}:
            raise ValueError("receiver source drift: "+row["path"])
def emitted(base,pins):
    base=Path(base).absolute()
    if any(p.is_symlink() for p in [base,*base.parents]) or not base.is_dir(): raise ValueError("emitted root redirected or missing")
    wanted={r["path"]:r for r in pins["targets"]}
    found=set()
    for directory,names,files in os.walk(base,followlinks=False,onerror=traversal_error):
        for name in names+files:
            path=Path(directory)/name
            if path.is_symlink(): raise ValueError("emitted symlink")
        for name in files:
            path=Path(directory)/name;rel=path.relative_to(base).as_posix();found.add(rel)
            if rel not in wanted or digest(regular_data(path))!=wanted[rel]["after"]:
                raise ValueError("foreign or incorrect emitted file: "+rel)
    if found!=set(wanted): raise ValueError("missing emitted target")

def frozen_pre(files):
    # Independent original state(0), never derived from successor targets or installed POST.
    raw=files[BASELINE_MANIFEST]
    if len(raw)!=7605 or digest(raw)!=BASELINE_MANIFEST_SHA:
        raise ValueError("frozen PRE manifest identity drift")
    rows=[];total=0
    for line in raw.decode("utf-8",errors="strict").splitlines():
        cells=line.split("\t")
        if len(cells)!=3:raise ValueError("frozen PRE manifest columns")
        path,sha,resource=cells;relative(path)
        if not re.fullmatch("[0-9a-f]{64}",sha) or not re.fullmatch("[A-Za-z0-9_.-]+",resource):
            raise ValueError("frozen PRE hash/resource")
        data=files[RESOURCE+resource]
        if digest(data)!=sha:raise ValueError("frozen PRE resource drift: "+path)
        rows.append({"path":path,"after":sha,"bytes":len(data),"resource":resource})
        total+=len(data)
    names=[row["path"] for row in rows]
    if names!=sorted(set(names)) or len(rows)!=BASELINE_FILES or total!=BASELINE_BYTES:
        raise ValueError("frozen PRE closed source inventory drift")
    return rows
def baseline(base,files,excluded=()):
    base=Path(base)
    if not base.is_absolute() or base!=Path(os.path.normpath(base)):
        raise ValueError("normalized absolute frozen PRE root required")
    for other in (ROOT,*excluded):
        if other is None:continue
        other=Path(other).absolute()
        if base==other or base in other.parents or other in base.parents:
            raise ValueError("frozen PRE must be independent of SDK, checkout and POST")
    rows=frozen_pre(files)
    # Reuse the exact emission inventory/hash/traversal/symlink mechanism.
    emitted(base,{"targets":rows})
    inventory=[]
    for row in rows:
        data=regular_data(safe(base,row["path"]))
        if len(data)!=row["bytes"] or digest(data)!=row["after"]:
            raise ValueError("frozen PRE receipt-read drift: "+row["path"])
        inventory.append({"path":row["path"],"bytes":len(data),"sha256":digest(data)})
    total=sum(row["bytes"] for row in inventory)
    if total!=BASELINE_BYTES:raise ValueError("frozen PRE byte total drift")
    return {"root":str(base),"files":len(inventory),"bytes":total,
            "manifestSha256":BASELINE_MANIFEST_SHA,
            "preInventorySha256":digest((json.dumps(inventory,sort_keys=True,separators=(",",":"))+"\n").encode("utf-8")),
            "independentOfInstalledPost":True}
def self_test(pins,files):
    count=0
    def refuses(action):
        nonlocal count
        try: action()
        except (ValueError,KeyError,TypeError,UnicodeError): count+=1;return
        raise AssertionError("custody negative control admitted")
    for mutation in ["digest","size","blob","duplicate","foreign","missing","provider","manifest","selfcycle"]:
        changed=copy.deepcopy(pins);values=dict(files)
        row=changed["files"][0]
        if mutation=="digest":row["sha256"]="0"*64
        elif mutation=="size":row["bytes"]+=1
        elif mutation=="blob":row["gitBlob"]="0"*40
        elif mutation=="duplicate":changed["files"].append(dict(row))
        elif mutation=="foreign":values["foreign.java"]=b"foreign"
        elif mutation=="missing":values.pop(row["path"])
        elif mutation=="provider":changed["snapshotProviderGitBlob"]="0"*40
        elif mutation=="manifest":changed["targets"]=list(reversed(changed["targets"]))
        else:changed["files"].append({"path":"pins.json","bytes":0,"sha256":digest(b""),"gitBlob":git_blob(b"")});values["pins.json"]=b""
        refuses(lambda:validate_packet(changed,values))
    valid=ET.Element("testsuite",name=SUITE,tests=str(len(METHODS)),failures="0",errors="0",skipped="0")
    for method in METHODS:ET.SubElement(valid,"testcase",name=method,classname=SUITE)
    validate_report(ET.tostring(valid),SUITE,METHODS)
    for attr in ["tests","failures","errors","skipped"]:
        broken=copy.deepcopy(valid);broken.set(attr,"999")
        refuses(lambda:validate_report(ET.tostring(broken),SUITE,METHODS))
    for kind in ["duplicate","foreignclass","nestedfailure","missing"]:
        broken=copy.deepcopy(valid)
        if kind=="duplicate":broken[-1].set("name",METHODS[0])
        elif kind=="foreignclass":broken[0].set("classname","unrelated")
        elif kind=="nestedfailure":ET.SubElement(broken[0],"failure")
        else:broken.remove(broken[-1])
        refuses(lambda:validate_report(ET.tostring(broken),SUITE,METHODS))
    workflow_data=next(files[RESOURCE+r["resource"]] for r in pins["targets"] if r["path"]==".github/workflows/m3-original-mapping-provider-qualification.yml")
    workflow_contexts(workflow_data)
    invalid_scope=workflow_data.replace(b"    steps:\n",b"    env:\n      BAD_TMP: "+bytes([36])+b"{{ runner.temp }}\n    steps:\n",1)
    refuses(lambda:workflow_contexts(invalid_scope))
    # Bind the real provider path even when an earlier shadow has its exact old blob.
    changed=copy.deepcopy(pins);values=dict(files)
    real="src/main/java/com/m3/rewrite/backport/M3Jdk21HashPinnedTextSnapshotRecipe.java"
    shadow="shadow/M3Jdk21HashPinnedTextSnapshotRecipe.java"
    old=next(r for r in changed["files"] if r["path"]==real)
    changed["files"].insert(0,{**old,"path":shadow});values[shadow]=values[real]
    values[real]=b"foreign provider"
    old.update(bytes=len(values[real]),sha256=digest(values[real]),gitBlob=git_blob(values[real]))
    refuses(lambda:validate_packet(changed,values))
    # All filesystem probes use an explicit admitted scratch directory; never tempfile fallback.
    scratch=os.environ.get("TMPDIR")
    if not scratch or any(p.is_symlink() for p in [Path(scratch),*Path(scratch).parents]) or not Path(scratch).is_dir():
        raise ValueError("explicit safe TMPDIR required for custody self-tests")
    with tempfile.TemporaryDirectory(dir=scratch,prefix="m3-provider-custody-") as tmp:
        base=Path(tmp);realroot=base/"real";realroot.mkdir();(realroot/"ok.txt").write_bytes(b"ok")
        minimal={"targets":[{"path":"ok.txt","after":digest(b"ok")}]}
        emitted(realroot,minimal)
        link=base/"root-link";link.symlink_to(realroot,target_is_directory=True)
        refuses(lambda:emitted(link,minimal))
        parent=base/"parent-link";parent.symlink_to(base,target_is_directory=True)
        refuses(lambda:emitted(parent/"real",minimal))
        from unittest import mock
        with mock.patch("os.walk",side_effect=ValueError("forced traversal refusal")):
            refuses(lambda:emitted(realroot,minimal))

    # Six projection-specific refusals; all original22 controls above remain intact.
    with tempfile.TemporaryDirectory(dir=scratch,prefix="m3-provider-pre-") as tmp:
        base=Path(tmp);pre=base/"frozen-pre";pre.mkdir()
        rows=frozen_pre(files)
        for row in rows:
            path=safe(pre,row["path"]);path.parent.mkdir(parents=True,exist_ok=True)
            path.write_bytes(files[RESOURCE+row["resource"]])
        baseline(pre,files)
        mapping=safe(pre,"m3/docs/name-mapping.json")
        original=mapping.read_bytes()
        mapping.write_bytes(files[RESOURCE+"after-009.txt"])
        refuses(lambda:baseline(pre,files)) # POST cannot substitute for frozen PRE.
        mapping.write_bytes(original)
        first=rows[0];path=safe(pre,first["path"]);original=path.read_bytes()
        path.unlink()
        refuses(lambda:baseline(pre,files)) # Missing PRE member.
        path.write_bytes(original)
        foreign=pre/"foreign.txt";foreign.write_bytes(b"foreign")
        refuses(lambda:baseline(pre,files)) # Extra PRE member.
        foreign.unlink()
        link=base/"pre-link";link.symlink_to(pre,target_is_directory=True)
        refuses(lambda:baseline(link,files)) # Root redirection.
        outside=base/"outside.txt";outside.write_bytes(original)
        path.unlink();path.symlink_to(outside)
        refuses(lambda:baseline(pre,files)) # Child redirection even with matching bytes.
        path.unlink();path.write_bytes(original)
        baseline(pre,files) # Successful admission cannot authorize later changed bytes.
        reads=[0];read_before_mutation=regular_data;target=path
        def mutate_before_receipt_read(candidate,maximum=16*1024**2):
            if Path(candidate)==target:
                reads[0]+=1
                if reads[0]==2:target.write_bytes(bytes([original[0]^1])+original[1:])
            return read_before_mutation(candidate,maximum)
        # The first read passes emitted(); the second returns same-size changed bytes.
        # An implementation that merely records that second digest must fail this control.
        with mock.patch.dict(globals(),{"regular_data":mutate_before_receipt_read}):
            refuses(lambda:baseline(pre,files))
        if reads[0]!=2:raise AssertionError("post-admission receipt-read counterexample not reached")
        path.write_bytes(original)
        baseline(pre,files)
    if count!=28:raise AssertionError("exact original22 plus6 PRE custody controls required")
    return count
def write_new(path,record):
    path=Path(path).absolute()
    if any(p.is_symlink() for p in [path,*path.parents]): raise ValueError("receipt symlink")
    path.parent.mkdir(parents=True,exist_ok=True)
    with path.open("xb") as f:f.write((json.dumps(record,sort_keys=True,indent=2)+"\n").encode())
def main():
    ap=argparse.ArgumentParser()
    ap.add_argument("--self-test",action="store_true");ap.add_argument("--receiver",type=Path)
    ap.add_argument("--report",type=Path);ap.add_argument("--emitted",type=Path)
    ap.add_argument("--baseline-root",type=Path)
    ap.add_argument("--receipt",type=Path);ap.add_argument("--run-id")
    args=ap.parse_args();pins,raw,files=inputs()
    tests=self_test(pins,files) if args.self_test else 0
    if args.receiver:receiver(args.receiver,pins)
    report_data=None
    if args.report:
        report_data=regular_data(args.report,8*1024**2)
        if {p.name for p in args.report.parent.glob("TEST-*.xml")}!={"TEST-"+SUITE+".xml"}:
            raise ValueError("foreign or missing Surefire suite")
    counts=validate_report(report_data,SUITE,METHODS) if report_data is not None else None
    if args.emitted:emitted(args.emitted,pins)
    baseline_record=baseline(args.baseline_root,files,(args.receiver,args.emitted)) if args.baseline_root else None
    if args.receipt:
        if not args.report or not args.emitted or not args.run_id or not args.baseline_root:raise ValueError("actual named test/emission/frozen PRE/run evidence required")
        write_new(args.receipt,{"schema":"M3_ORIGINAL_PROVIDER_RECIPE_EXECUTION_V1","runId":args.run_id,"pinsSha256":digest(raw),"namedSdkMethods":METHODS,"actualSdkCounts":counts,"reportSha256":digest(report_data),"emittedTargetCount":len(pins["targets"]),"frozenPreBaseline":baseline_record,"mutableTargets":6,"addedTargets":4,"changedTargets":2,"partialStates":64,"sourceOrders":2,"fixedPoint":"executed by named SDK tests","runtimeJniQualified":False,"matchedJdkQualified":False,"performanceQualified":False,"phaseAdvanced":False})
    print("M3_ORIGINAL_PROVIDER_CUSTODY_PASS negativeControls="+str(tests)+" sdkExecution="+str(bool(counts))+" frozenPreBaseline="+str(bool(baseline_record)))
if __name__=="__main__":main()
