#!/usr/bin/env python3
"""Rebuild the pinned, additive Error Prone Guava namespace receiver."""
import argparse, hashlib, json, pathlib, subprocess, zipfile
p=argparse.ArgumentParser()
p.add_argument('--cache',required=True); p.add_argument('--jdk',required=True)
p.add_argument('--classpath',required=True); p.add_argument('--output',required=True)
a=p.parse_args()
root=pathlib.Path(__file__).resolve().parent
pins=json.loads((root/'PROVENANCE.json').read_text())
sha=lambda b:hashlib.sha256(b).hexdigest()
cache=pathlib.Path(a.cache)
original=cache/'com/google/errorprone/error_prone_core/2.50.0/error_prone_core-2.50.0.jar'
pom=original.with_suffix('.pom')
for path,expected in [(original,pins['original_jar_sha256']),(pom,pins['original_pom_sha256'])]:
 if sha(path.read_bytes())!=expected: raise SystemExit('Pinned upstream artifact drift: '+str(path))
sources=sorted((root/'src/main/java').rglob('*.java'))
for source in sources:
 if sha(source.read_bytes())!=pins['corrected_sources'][str(source.relative_to(root))]:
  raise SystemExit('Corrected source drift: '+str(source))
out=pathlib.Path(a.output).resolve(); out.mkdir(parents=True,exist_ok=True)
classes=out/'classes'; classes.mkdir(exist_ok=True)
if any(classes.rglob('*.class')): raise SystemExit('Require fresh output/classes')
cp=pathlib.Path(a.classpath).read_text().strip()
cmd=[str(pathlib.Path(a.jdk)/'bin/javac'),'-J-Xmx512m','-proc:none','-source','21','-target','21','-Xlint:-options','-classpath',cp,'-d',str(classes)]
for package in ['api','code','comp','file','main','model','parser','processing','tree','util']:
 cmd += ['--add-exports','jdk.compiler/com.sun.tools.javac.'+package+'=ALL-UNNAMED']
subprocess.run(cmd+list(map(str,sources)),check=True)
version=pins['receiver_version']
jar=out/('error_prone_core-'+version+'.jar')
replacements={str(f.relative_to(classes)).replace('\\','/'):f.read_bytes() for f in classes.rglob('*.class')}
with zipfile.ZipFile(original) as src, zipfile.ZipFile(jar,'w') as dst:
 for item in src.infolist():
  if item.filename.upper().endswith(('.SF','.RSA','.DSA')): raise SystemExit('Signed upstream artifact requires explicit signing owner')
  dst.writestr(item,replacements.pop(item.filename,src.read(item.filename)))
 for name,data in sorted(replacements.items()):
  info=zipfile.ZipInfo(name,(2026,10,18,0,0,0)); info.compress_type=zipfile.ZIP_DEFLATED
  dst.writestr(info,data)
text=pom.read_text()
needle='<artifactId>error_prone_core</artifactId>'
assert text.count(needle)==1
text=text.replace(needle,needle+'\n  <version>'+version+'</version>',1)
text=text.replace('${project.version}','2.50.0')
artifact_pom=jar.with_suffix('.pom'); artifact_pom.write_text(text)
receipt={'upstream_version':'2.50.0','receiver_version':version,'jar_sha256':sha(jar.read_bytes()),'pom_sha256':sha(artifact_pom.read_bytes()),'sources':pins['corrected_sources'],'classpath':[{'path':x,'sha256':sha(pathlib.Path(x).read_bytes())} for x in cp.split(':')],'compiler':subprocess.check_output([str(pathlib.Path(a.jdk)/'bin/javac'),'-version'],text=True).strip()}
(out/'BUILD_RECEIPT.json').write_text(json.dumps(receipt,indent=2)+'\n')
print(jar); print(artifact_pom)

