#!/usr/bin/env python3
"""Author the bounded three-target A3 code-string recipe; no production writes."""
from pathlib import Path
import json,hashlib,difflib
HERE=Path(__file__).resolve().parent;ROOT=HERE.parents[1]
PRIOR=ROOT/'current-ci/a3-behavior-v01';OLD=PRIOR/'workspace'
CRATE='a3-code-string-repair-20261006';BASE='m3/tooling/migration-recipes';OVER=HERE/'candidate/overlay';RES=OVER/BASE/'src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text'/CRATE
JAVA='m3/tooling/a3/src/main/java/com/m3/a3/A3RegexMatrix.java'
TEMPLATE=BASE+'/src/main/resources/com/synexia/rewrite/hash-pinned-java/a3-regex-memory-lab/A3RegexMatrix.java.after'
MANIFEST=BASE+'/src/main/resources/com/synexia/rewrite/hash-pinned-java/a3-regex-memory-lab/manifest.tsv'
def sha(b):return hashlib.sha256(b).hexdigest()
def blob(b):return hashlib.sha1(b'blob '+str(len(b)).encode()+b'\0'+b).hexdigest()
def write(p,b):
 p.parent.mkdir(parents=True,exist_ok=True)
 with p.open('xb') as f:f.write(b)
def main():
 before=(OLD/TEMPLATE).read_bytes();needle=b'"Pattern.compile("a+b?")"';replacement=b'"Pattern.compile(\\"a+b?\\")"';assert before.count(needle)==1
 after=before.replace(needle,replacement);assert len(after)==len(before)+2
 original=(OLD/MANIFEST).read_bytes();lines=original.decode().splitlines(keepends=True);found=0
 for i,line in enumerate(lines):
  cells=line.rstrip('\n').split('\t')
  if cells[0]=='src/main/java/com/m3/a3/A3RegexMatrix.java':
   assert cells==[cells[0],'ABSENT',sha(before),'A3RegexMatrix.java.after'];cells[2]=sha(after);lines[i]='\t'.join(cells)+'\n';found+=1
 assert found==1
 edits={JAVA:(before,after),TEMPLATE:(before,after),MANIFEST:(original,''.join(lines).encode())};rows=[];diff=[];manifest=[]
 for i,(path,(pre,post)) in enumerate(sorted(edits.items())):
  names=('before-%02d-%s.txt'%(i,Path(path).name),'after-%02d-%s.txt'%(i,Path(path).name))
  write(RES/names[0],pre);write(RES/names[1],post)
  rows.append({'repository_path':path,'before':{'resource':names[0],'bytes':len(pre),'sha256':sha(pre),'git_blob':blob(pre)},'after':{'resource':names[1],'bytes':len(post),'sha256':sha(post),'git_blob':blob(post)}})
  manifest.append('\t'.join([path,sha(pre),sha(post),names[1]])+'\n')
  diff.extend(difflib.unified_diff(pre.decode().splitlines(True),post.decode().splitlines(True),fromfile='a/'+path,tofile='b/'+path))
 write(RES/'manifest.tsv',''.join(manifest).encode())
 guards=[]
 for path in [BASE+'/src/main/java/com/m3/rewrite/backport/M3Jdk21HashPinnedTextSnapshotRecipe.java',BASE+'/src/main/java/com/synexia/rewrite/M3HashPinnedJavaSnapshotRecipe.java']:
  guards.append({'path':path,'sha256':sha((OLD/path).read_bytes())})
 plan={'schema':'m3.sealed-install/1','recipe_id':CRATE+'/1','source_commit':'d1b9162cd568108f4c8d82f6b6a03cccfdb91bd2','target_commit':'d1b9162cd568108f4c8d82f6b6a03cccfdb91bd2','source_repository':'hsoliwal/M3jdk21','recipe':'com.m3.rewrite.backport.A3CodeStringRepair','outputs':[{'path':r['repository_path'],'before':{k:r['before'][k] for k in ('resource','sha256')},'after':{k:r['after'][k] for k in ('resource','sha256')}} for r in rows],'guards':guards,'scope':'Escape two nested quotes in the existing code-containing regex subject, synchronize exact production/template bytes and only the bound Java-template hash.','qualification':{'whole_module_coverage_accepted':False,'jdk_image_accepted':False,'native_or_jni_accepted':False}}
 plan['plan_sha256']=sha((json.dumps(plan,sort_keys=True,separators=(',',':'),ensure_ascii=True)+'\n').encode());write(RES/'plan.json',(json.dumps(plan,indent=2)+'\n').encode())
 yaml='''---\ntype: specs.openrewrite.org/v1beta/recipe\nname: com.m3.rewrite.backport.A3CodeStringRepair\ndisplayName: Repair the exact A3 code-containing regex subject\ndescription: Escapes the two nested quotes in the production subject and its template while synchronizing the hash-pinned Java manifest.\nrecipeList:\n  - com.m3.rewrite.backport.M3Jdk21HashPinnedTextSnapshotRecipe:\n      crateName: a3-code-string-repair-20261006\n'''
 write(OVER/BASE/'src/main/resources/META-INF/rewrite/m3-a3-code-string-repair.yml',yaml.encode())
 write(HERE/'candidate/CANDIDATE_DIFF.patch',''.join(diff).encode());write(HERE/'candidate/CANDIDATES.json',(json.dumps({'crate':CRATE,'rows':rows,'plan_sha256':plan['plan_sha256']},indent=2)+'\n').encode())
 print(''.join(diff));print(json.dumps({'rows':rows,'plan_sha256':plan['plan_sha256']},indent=2))
if __name__=='__main__':main()
