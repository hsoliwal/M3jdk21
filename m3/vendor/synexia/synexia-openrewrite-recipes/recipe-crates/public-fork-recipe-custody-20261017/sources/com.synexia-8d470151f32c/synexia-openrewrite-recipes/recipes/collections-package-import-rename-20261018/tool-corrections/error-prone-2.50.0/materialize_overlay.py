#!/usr/bin/env python3
"""Export only changed, qualified checker class entries for offline public reconstruction."""
import argparse,hashlib,json,pathlib,shutil,zipfile
p=argparse.ArgumentParser()
p.add_argument('--original-jar',required=True);p.add_argument('--receiver-jar',required=True);p.add_argument('--output',required=True)
a=p.parse_args();root=pathlib.Path(__file__).resolve().parent;out=pathlib.Path(a.output)
pins=json.loads((root/'PROVENANCE.json').read_text());sha=lambda b:hashlib.sha256(b).hexdigest()
original=pathlib.Path(a.original_jar);receiver=pathlib.Path(a.receiver_jar)
if sha(original.read_bytes())!=pins['original_jar_sha256']:raise SystemExit('Original artifact drift')
if sha(original.with_suffix('.pom').read_bytes())!=pins['original_pom_sha256']:raise SystemExit('Original POM drift')
receipt=json.loads((receiver.parent/'BUILD_RECEIPT.json').read_text())
if pins['upstream_repository']!='google/error-prone' or pins['upstream_ref']!='v2.50.0' or receipt['upstream_version']!='2.50.0':
 raise SystemExit('Upstream receipt identity drift')
if receipt['receiver_version']!=pins['receiver_version']:raise SystemExit('Receiver receipt identity drift')
if receipt['sources']!=pins['corrected_sources']:raise SystemExit('Qualified source receipt drift')
for path,expected in pins['corrected_sources'].items():
 if sha((root/path).read_bytes())!=expected:raise SystemExit('Canonical source drift: '+path)
if sha(receiver.read_bytes())!=receipt['jar_sha256'] or receipt['jar_sha256']!=pins['receiver_jar_sha256']:
 raise SystemExit('Qualified receiver drift')
if sha(receiver.with_suffix('.pom').read_bytes())!=receipt['pom_sha256'] or receipt['pom_sha256']!=pins['receiver_pom_sha256']:
 raise SystemExit('Qualified receiver POM drift')
out.mkdir(parents=True,exist_ok=True)
entries={}
with zipfile.ZipFile(original) as old,zipfile.ZipFile(receiver) as new,zipfile.ZipFile(out/'classes.zip','w') as overlay:
 for name in new.namelist():
  data=new.read(name)
  if name not in old.namelist() or old.read(name)!=data:
   if not name.startswith('com/google/errorprone/bugpatterns/threadsafety/') or not name.endswith('.class') or name.rsplit('/',1)[-1][:-6].split('$',1)[0] not in ('WellKnownMutability','HeldLockAnalyzer'):raise SystemExit('Unowned changed entry '+name)
   info=zipfile.ZipInfo(name,(2026,10,18,0,0,0));info.compress_type=zipfile.ZIP_DEFLATED
   overlay.writestr(info,data);entries[name]=sha(data)
 if set(old.namelist())-set(new.namelist()):raise SystemExit('Unexpected removed entry')
shutil.copy2(root/'reconstruct_overlay.py',out/'reconstruct.py');shutil.copy2(root/'COPYING',out/'COPYING')
manifest={'schema':'M3_PINNED_ERROR_PRONE_CLASS_OVERLAY_V1','canonical_owner':'hsoliwal/com.synexia/synexia-openrewrite-recipes/recipes/collections-package-import-rename-20261018/tool-corrections/error-prone-2.50.0','canonical_source_pins':pins['corrected_sources'],'canonical_build_script_sha256':sha((root/'build_receiver.py').read_bytes()),'upstream_source_git_blobs':pins['upstream_source_git_blobs'],'original_jar_sha256':pins['original_jar_sha256'],'original_pom_sha256':pins['original_pom_sha256'],'receiver_version':pins['receiver_version'],'receiver_jar_sha256':sha(receiver.read_bytes()),'receiver_pom_sha256':sha(receiver.with_suffix('.pom').read_bytes()),'overlay_sha256':sha((out/'classes.zip').read_bytes()),'reconstructor_sha256':sha((out/'reconstruct.py').read_bytes()),'license_sha256':sha((out/'COPYING').read_bytes()),'entries':entries}
(out/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
print('Exported '+str(len(entries))+' changed owned class entries')
