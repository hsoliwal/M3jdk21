# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
from pathlib import Path
import subprocess,sys,shutil,json
root=Path(__file__).resolve().parent
source=Path(sys.argv[1]).resolve();out=Path(sys.argv[2]).resolve();out.mkdir(parents=True,exist_ok=True)
scratch=out/'sources'
if source == out or source in out.parents or out in source.parents:
 raise ValueError('source and output roots must be disjoint')
# Copy only the seven pinned inputs, never the full SDK or unrelated donor trees.
inputs=[
 'src/java.base/share/classes/jdk/internal/mindex/M3StateRelation.java',
 'src/java.base/share/classes/jdk/internal/mindex/M3CollectionMaskCompactor.java',
 'src/java.base/share/native/libjava/M3StateRelation.c',
 'src/java.base/share/native/libjava/m3_state_relation.h',
 'm3/collections/src/main/java/com/m3/collections/M3SlotArrays.java',
 'm3/collections/src/main/java/com/m3/collections/M3SpanIndex.java',
 'm3/collections/src/main/native/collections/m3_span_index_jni.c']
for relative in inputs:
 destination=scratch/relative;destination.parent.mkdir(parents=True,exist_ok=True)
 shutil.copyfile(source/relative,destination)
internal=scratch/'src/java.base/share/classes/jdk/internal/mindex'
collection=scratch/'m3/collections/src/main/java/com/m3/collections'
native=scratch/'src/java.base/share/native/libjava'
p=internal/'M3StateRelation.java';t=p.read_text();assert t.count('result |= rows[')==2;t=t.replace('result |= rows[','M3FactWorkProbe.relationReads++; result |= rows[');p.write_text(t)
p=internal/'M3CollectionMaskCompactor.java';t=p.read_text();assert t.count('long bits = deleted[word];')==1;p.write_text(t.replace('long bits = deleted[word];','M3FactWorkProbe.maskWords++; long bits = deleted[word];'))
p=collection/'M3SpanIndex.java';t=p.read_text();needle='int middle = low + ((high - low) >>> 1);';assert 1<=t.count(needle)<=2;p.write_text(t.replace(needle,'M3SpanWorkProbe.comparisons++; '+needle))
p=native/'M3StateRelation.c';t=p.read_text();needle='while (bits != 0 && value != ceiling) {';assert t.count(needle)==1;p.write_text(t.replace('#include <string.h>','#include <string.h>\nsize_t m3_test_expansions = 0;').replace(needle,needle+'\n        ++m3_test_expansions;'))
javahome=Path(shutil.which('javac')).resolve().parents[1]
commands=[['javac','-Xlint:all','-Werror','--patch-module','java.base='+str(internal)+':'+str(root/'tests/jdk/internal/mindex'),'-d',str(out/'patch'),str(internal/'M3StateRelation.java'),str(internal/'M3CollectionMaskCompactor.java'),str(root/'tests/jdk/internal/mindex/M3FactWorkProbe.java')],
['javac','--release','21','-Xlint:all','-Werror','-d',str(out/'classes'),str(collection/'M3SlotArrays.java'),str(collection/'M3SpanIndex.java'),str(root/'tests/com/m3/collections/M3SpanWorkProbe.java')],
['gcc','-std=c17','-O2','-Wall','-Wextra','-Werror','-I'+str(javahome/'include'),'-I'+str(javahome/'include/linux'),'-I'+str(native),str(native/'M3StateRelation.c'),str(root/'tests/relation_work_probe.c'),'-o',str(out/'native_work')]]
for cmd in commands:subprocess.run(cmd,check=True)
results=[]
commands=[['java','--patch-module','java.base='+str(out/'patch'),'jdk.internal.mindex.M3FactWorkProbe',mode] for mode in ['relation','mask']]
commands+=[['java','-cp',str(out/'classes'),'com.m3.collections.M3SpanWorkProbe'],[str(out/'native_work')]]
for cmd in commands:
 r=subprocess.run(cmd,capture_output=True,text=True);results.append({'command':cmd,'exit':r.returncode,'stdout':r.stdout,'stderr':r.stderr});print(r.returncode,r.stdout.strip(),r.stderr.strip())
(out/'results.json').write_text(json.dumps(results,indent=2)+'\n')
if '--require-pass' in sys.argv and any(x['exit'] for x in results):sys.exit(1)
