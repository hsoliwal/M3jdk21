#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
"""Author exact, reviewed templates only; SealReplay owns target application."""
from pathlib import Path
import argparse, hashlib, json
T=Path(__file__).resolve().parent
JAVA='m3/collections/src/main/java/com/m3/collections/M3IndexMaskCompactor.java'
NATIVE='m3/collections/src/main/native/collections/m3_index_mask_compactor.c'
EXPECTED={'M3IndexMaskCompactor.java':'477135e13481942fef3b95b96a08d39c9c86dd92a649959c6d035b19b5784031','m3_index_mask_compactor.c':'8610e9fcedcc8eb698acbf3161bca8ea7d16194e61c66915f9e25fe47aae27f7'}
def sha(b):return hashlib.sha256(b).hexdigest()
def replace_once(s,a,b):
    if s.count(a)!=1:raise ValueError('reviewed atom anchor mismatch: '+a[:90])
    return s.replace(a,b,1)
def body_span(s,signature):
    # These two complete reviewed preimages are hash-admitted above. This is not
    # a generic Java/C parser or an authority to edit arbitrary source text.
    at=s.index(signature);start=s.index('{',at);level=1;end=start+1
    while level:
        if s[end]=='{':level+=1
        elif s[end]=='}':level-=1
        end+=1
    return start+1,end-1

def derive_java(s):
    long_sig='public static long[] selectLong('
    a,b=body_span(s,long_sig)
    s=s[:a]+'''
        Objects.requireNonNull(orderedRows, "orderedRows");
        validate(rowStates, orderedRows.length, requiredMask);
        return selectLongWindowJava(orderedRows, 0, rowStates, 0, orderedRows.length, requiredMask);
    '''+s[b:]
    a,b=body_span(s,'private static int countSelected(')
    s=s[:a]+'''
        return countWindow(states, 0, states.length, requiredMask);
    '''+s[b:]
    a,b=body_span(s,'static int[] selectJava(')
    s=s[:a]+'''
        return selectIntWindowJava(orderedRows, 0, rowStates, 0, orderedRows.length, requiredMask);
    '''+s[b:]
    kernels=[]
    for width,helper in [('int','selectIntWindowJava'),('long','selectLongWindowJava')]:
        signature='public static '+width+'[] '+('selectWindow(' if width=='int' else 'selectLongWindow(')
        a,b=body_span(s,signature)
        marker='        int kept = countWindow(states, stateOffset, length, requiredMask);'
        start=s.index(marker,a,b)
        kernel=s[start:b]
        kernels.append('''    /** Existing checked callers share one stable %s emission atom; no retained inputs. */
    private static %s[] %s(%s[] orderedRows, int rowOffset,
            byte[] states, int stateOffset, int length, int requiredMask) {
%s}

'''%(width,width,helper,width,kernel))
        s=s[:start]+f'        return {helper}(orderedRows, rowOffset, states, stateOffset, length, requiredMask);\n    '+s[b:]
    s=replace_once(s,'    /** Zero-mask selection is known without reading any state words. */', ''.join(kernels)+'    /** Zero-mask selection is known without reading any state words. */')
    return s

def derive_c(s):
    # Reuse/move the original native count atom; declaration order is internal.
    sig='static jint m3_count_window('
    a=s.index(sig);_,b=body_span(s,sig);counter=s[a:b+1]
    s=s[:a]+s[b+2:]
    s=replace_once(s,'/* One core for legacy whole-index and offset windows. All bounds validated by exports.',counter+'''

/* First retained ordinal in one bounded marks page. Zero-mask never reads marks. */
static jsize m3_first_selected(const jbyte *marks, jsize size, jint mask) {
    if (mask == 0) return 0;
    jsize first = 0;
    while (first < size) {
        unsigned int flags = (unsigned int)(unsigned char)marks[first];
        if ((flags & (unsigned int)mask) == (unsigned int)mask) break;
        first++;
    }
    return first;
}

/* One core for legacy whole-index and independent-offset windows. Bounds are validated by exports.''')
    s=replace_once(s,'jsize first, jsize n, jint required_mask) {','jsize first, jsize state_first, jsize n, jint required_mask) {')
    a=s.index('    jsize count = required_mask == 0 ? n : 0;');b=s.index('    jintArray result =',a)
    s=s[:a]+'''    jsize count = m3_count_window(env, states, state_first, n, required_mask);
    if (count < 0) return NULL;
'''+s[b:]
    s=replace_once(s,'if (required_mask != 0) {\n            (*env)->GetByteArrayRegion(env, states, offset, chunk, marks);','if (required_mask != 0) {\n            (*env)->GetByteArrayRegion(env, states, state_first + offset, chunk, marks);')
    s=replace_once(s,'        (*env)->GetIntArrayRegion(env, rows, first + offset, chunk, row_ids);','''        jsize begin = m3_first_selected(marks, chunk, required_mask);
        if (begin == chunk) {
            /* Keep the second state scan/drift check, but do not fetch rejected payload. */
            offset += chunk;
            continue;
        }
        (*env)->GetIntArrayRegion(env, rows, first + offset, chunk, row_ids);''')
    s=replace_once(s,'''        jsize accepted = 0;
        for (jsize i = 0; i < chunk; i++) {
            unsigned int bits = required_mask == 0 ? 0u : (unsigned int)(unsigned char)marks[i];
            if ((bits & (unsigned int)required_mask) == (unsigned int)required_mask) {
                row_ids[accepted++] = row_ids[i];
            }
        }''','''        jsize accepted = chunk;
        if (required_mask != 0) {
            accepted = 0;
            for (jsize i = begin; i < chunk; i++) {
                unsigned int bits = (unsigned int)(unsigned char)marks[i];
                if ((bits & (unsigned int)required_mask) == (unsigned int)required_mask) {
                    row_ids[accepted++] = row_ids[i];
                }
            }
        }''')
    s=replace_once(s,'m3_select_range(env, rows, states, 0, n, required_mask);','m3_select_range(env, rows, states, 0, 0, n, required_mask);')
    s=replace_once(s,'m3_select_range(env, rows, states, first, length, required_mask);','m3_select_range(env, rows, states, first, 0, length, required_mask);')
    a,b=body_span(s,'Java_com_m3_collections_M3IndexMaskCompactor_selectWindowNative(')
    s=s[:a]+'''
    (void)owner;
    if (!m3_window_valid(env, rows, states, row_offset, state_offset, length, mask)) {
        return NULL;
    }
    return m3_select_range(env, rows, states, row_offset, state_offset, length, mask);
'''+s[b:]
    s=replace_once(s,'    jlong accepted[M3_CHUNK];\n','')
    s=replace_once(s,'        (*env)->GetLongArrayRegion(env, rows, row_offset + position, chunk, values);','''        jsize begin = m3_first_selected(marks, chunk, mask);
        if (begin == chunk) {
            position += chunk;
            continue;
        }
        (*env)->GetLongArrayRegion(env, rows, row_offset + position, chunk, values);''')
    s=replace_once(s,'''            for (jsize i = 0; i < chunk; i++) {
                unsigned int flags = (unsigned int)(unsigned char)marks[i];
                if ((flags & (unsigned int)mask) == (unsigned int)mask) accepted[batch++] = values[i];
            }''','''            /* Prefix compaction cannot overwrite an unread row: batch <= i. */
            for (jsize i = begin; i < chunk; i++) {
                unsigned int flags = (unsigned int)(unsigned char)marks[i];
                if ((flags & (unsigned int)mask) == (unsigned int)mask) values[batch++] = values[i];
            }''')
    s=replace_once(s,'SetLongArrayRegion(env, result, written, batch, mask == 0 ? values : accepted);','SetLongArrayRegion(env, result, written, batch, values);')
    return s

def main():
    ap=argparse.ArgumentParser();ap.add_argument('--check',action='store_true');args=ap.parse_args()
    data={}
    for name,expected in EXPECTED.items():
        raw=(T/'before'/name).read_bytes()
        if sha(raw)!=expected:raise ValueError('original source drift: '+name)
        data[name]=raw.decode('utf-8')
    outputs={JAVA:derive_java(data['M3IndexMaskCompactor.java']).encode(),NATIVE:derive_c(data['m3_index_mask_compactor.c']).encode()}
    test='m3/collections/src/test/java/com/m3/collections/M3MaskWindowKernelSelfTest.java'
    outputs[test]=(T/'probes/M3MaskWindowKernelSelfTest.java').read_bytes()
    res=T/'src/main/resources/m3-mask-window';files={};lines=[]
    for path,raw in sorted(outputs.items()):
        name=Path(path).name+'.txt';files[res/name]=raw
        lines.append('\t'.join((path,EXPECTED.get(Path(path).name,'ABSENT'),sha(raw),name)))
    files[res/'manifest.tsv']=('\n'.join(lines)+'\n').encode()
    # Keep the manifest's earlier source states for one-file-at-a-time qualification.
    for ordinal,(path,raw) in enumerate(outputs.items(),1):
        name=Path(path).name+'.txt';root=res/f'pass-{ordinal}'
        files[root/name]=raw
        files[root/'manifest.tsv']=('\t'.join((path,EXPECTED.get(Path(path).name,'ABSENT'),sha(raw),name))+'\n').encode()
    for p,raw in files.items():
        if args.check:
            if not p.exists() or p.read_bytes()!=raw:raise ValueError('authored template drift: '+str(p))
        else:
            p.parent.mkdir(parents=True,exist_ok=True)
            if not p.exists() or p.read_bytes()!=raw:p.write_bytes(raw)
    print('AUTHOR_FIXED_POINT' if args.check else 'AUTHORED_EXACT_TEMPLATES',len(outputs))
if __name__=='__main__':main()
