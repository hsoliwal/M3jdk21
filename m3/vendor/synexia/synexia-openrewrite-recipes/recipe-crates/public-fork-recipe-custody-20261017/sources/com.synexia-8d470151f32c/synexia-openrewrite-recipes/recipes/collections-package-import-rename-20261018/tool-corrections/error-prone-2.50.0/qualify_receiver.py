#!/usr/bin/env python3
"""Compile real old/new Guava fixtures with the admitted Error Prone provider."""
import argparse,json,pathlib,subprocess,hashlib
p=argparse.ArgumentParser()
for n in ['jdk','classpath','original-guava','renamed-classes','receiver','output']:p.add_argument('--'+n,required=True)
a=p.parse_args(); out=pathlib.Path(a.output); out.mkdir(parents=True,exist_ok=True)
original_cp=pathlib.Path(a.classpath).read_text().strip()
parts=original_cp.split(':')
old_core=[x for x in parts if x.endswith('/error_prone_core-2.50.0.jar')]
assert len(old_core)==1
cp=':'.join(a.receiver if x==old_core[0] else x for x in parts)
javac=str(pathlib.Path(a.jdk)/'bin/javac')
exports=[]
for package in ['api','code','comp','file','main','model','parser','processing','tree','util']:
 exports+=['-J--add-exports=jdk.compiler/com.sun.tools.javac.'+package+'=ALL-UNNAMED']
exports+=['-J--add-opens=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED','-J--add-opens=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED']
records=[]
for identity,prefix,owner in [('original','com.google.common',a.original_guava),('renamed','com.m3.collections.guava',a.renamed_classes)]:
 fixtures={
 'immutable-positive':('Immutable',True,'import '+prefix+'.collect.ImmutableList;\nimport com.google.errorprone.annotations.Immutable;\n@Immutable final class Probe { final ImmutableList<String> values = ImmutableList.of("ok"); }\n'),
 'immutable-negative':('Immutable',False,'import '+prefix+'.collect.ImmutableList;\nimport com.google.errorprone.annotations.Immutable;\n@Immutable final class Probe { final ImmutableList<Mutable> values = ImmutableList.of(); static final class Mutable { int state; } }\n'),
 'monitor-positive':('GuardedBy',True,'import '+prefix+'.util.concurrent.Monitor;\nimport com.google.errorprone.annotations.concurrent.GuardedBy;\nfinal class Probe { final Monitor monitor = new Monitor(); @GuardedBy("monitor") int state; final Monitor.Guard guard = new Monitor.Guard(monitor) { public boolean isSatisfied() { return state > 0; } }; void update() { monitor.enter(); try { state++; } finally { monitor.leave(); } } }\n'),
 'monitor-negative':('GuardedBy',False,'import '+prefix+'.util.concurrent.Monitor;\nimport com.google.errorprone.annotations.concurrent.GuardedBy;\nfinal class Probe { final Monitor monitor = new Monitor(); @GuardedBy("monitor") int state; void update() { state++; } }\n')}
 for name,(checker,positive,source) in fixtures.items():
  folder=out/(identity+'-'+name);folder.mkdir(exist_ok=True); f=folder/'Probe.java';f.write_text(source)
  command=[javac,'-J-Xmx512m']+exports+['-classpath',owner+':'+cp,'-processorpath',cp,'-d',str(folder),'-XDcompilePolicy=simple','--should-stop=ifError=FLOW','-Xplugin:ErrorProne -XepDisableAllChecks -Xep:Immutable:ERROR -Xep:GuardedBy:ERROR',str(f)]
  result=subprocess.run(command,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT); (folder/'diagnostic.log').write_text(result.stdout)
  if positive:
   assert result.returncode==0,result.stdout
  else:
   assert result.returncode!=0 and '['+checker+']' in result.stdout,result.stdout
   assert result.stdout.count('error:')==1,result.stdout
  records.append({'identity':identity,'fixture':name,'expected':'PASS' if positive else checker+'_REFUSAL','exit':result.returncode,'source_sha256':hashlib.sha256(source.encode()).hexdigest()})
# Compare the entire original map against the additive receiver, including every container-of parameter.
folder=out/'annotation-info';folder.mkdir(exist_ok=True)
source='''import com.google.errorprone.bugpatterns.threadsafety.WellKnownMutability;
import com.google.errorprone.ErrorProneFlags;
public final class MapProbe {
 public static void main(String[] args) {
  var known=WellKnownMutability.fromFlags(ErrorProneFlags.empty());
  known.getKnownImmutableClasses().entrySet().stream().sorted(java.util.Map.Entry.comparingByKey()).forEach(e ->
   System.out.println("I\\t"+e.getKey()+"\\t"+e.getValue().typeName()+"\\t"+new java.util.TreeSet<>(e.getValue().containerOf())));
  known.getKnownMutableClasses().stream().sorted().forEach(n -> System.out.println("M\\t"+n));
 }
}'''
f=folder/'MapProbe.java';f.write_text(source)
subprocess.run([javac,'-proc:none','-classpath',original_cp,'-d',str(folder),str(f)],check=True)
java=str(pathlib.Path(a.jdk)/'bin/java')
def runmap(classpath):return subprocess.check_output([java]+[x[2:] for x in exports]+['-cp',str(folder)+':'+classpath,'MapProbe'],text=True).splitlines()
old=runmap(original_cp);new=runmap(cp);oldset=set(old);newset=set(new)
assert oldset<=newset
aliases=set()
for line in old:
 fields=line.split('\t')
 if fields[1].startswith('com.google.common.'):
  fields[1]=fields[1].replace('com.google.common.','com.m3.collections.guava.',1)
  if fields[0]=='I':fields[2]=fields[2].replace('com.google.common.','com.m3.collections.guava.',1)
  aliases.add('\t'.join(fields))
assert newset==oldset|aliases,repr(newset^(oldset|aliases))
(folder/'ORIGINAL.tsv').write_text('\n'.join(old)+'\n');(folder/'RECEIVER.tsv').write_text('\n'.join(new)+'\n')
receipt={'fixtures':records,'original_rule_rows':len(old),'alias_rule_rows':len(aliases),'exact_additive_container_constraints':'PASS','receiver_sha256':hashlib.sha256(pathlib.Path(a.receiver).read_bytes()).hexdigest()}
(out/'QUALIFICATION.json').write_text(json.dumps(receipt,indent=2)+'\n')
print(json.dumps(receipt))

