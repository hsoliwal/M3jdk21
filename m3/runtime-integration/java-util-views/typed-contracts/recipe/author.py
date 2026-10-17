#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
"""Author sealed candidates only. Actual receiving files are emitted by the SDK recipe.
This exact-input composition reuses the sibling wrapper's typed metadata atoms;
it is not a general parser or permission to rewrite an unpinned Java file.
"""
from pathlib import Path
import hashlib
ROOT = Path(__file__).resolve().parent / 'src/main/resources/com/synexia/rewrite/java-util-view-contracts'
JAVA = 'src/java.base/share/classes/java/util/CollectionViews.java'
C = 'test/jdk/java/util/CollectionViews/run_search_jni.c'
def blob(data):
    return hashlib.sha1(b'blob '+str(len(data)).encode()+b'\0'+data).hexdigest()
def replace(source, old, new):
    if source.count(old) != 1:
        raise ValueError('Authoring anchor mismatch')
    return source.replace(old, new, 1)
def main():
    before=(ROOT/'before'/JAVA).read_bytes()
    if blob(before)!='60ee7b46b457167cecb242d8ec14a4e9dd7653cd':
        raise ValueError('Original Java input drift')
    s=before.decode()
    s=replace(s,' * Apache notice retained; core integration license review remains a promotion gate.',
        ' * Modified 2026-10-17: preserve typed traversal metadata and saturating size.\n * Apache notice retained; core integration license review remains a promotion gate.')
    s=replace(s,'                    count++;','                    if (count != Integer.MAX_VALUE) count++;')
    s=replace(s,'        private FilteredSet(Set<E> source, Predicate<? super E> predicate) {\n            super(source, predicate);\n        }',
'''        private FilteredSet(Set<E> source, Predicate<? super E> predicate) {
            super(source, predicate);
        }

        @Override
        public Spliterator<E> spliterator() {
            return new SelectionSpliterator<>(backing.spliterator(), predicate,
                    Spliterator.DISTINCT, null);
        }''')
    s=replace(s,'        public Comparator<? super E> comparator() {\n            return sorted.comparator();\n        }',
'''        public Comparator<? super E> comparator() {
            return sorted.comparator();
        }

        /** Owner order is authoritative, including bounded and descending views. */
        @Override
        public Spliterator<E> spliterator() {
            return new SelectionSpliterator<>(sorted.spliterator(), super.predicate,
                    Spliterator.DISTINCT | Spliterator.SORTED | Spliterator.ORDERED,
                    sorted.comparator());
        }''')
    s=replace(s,'        private final Spliterator<E> source;\n        private final Predicate<? super E> predicate;',
'''        private final Spliterator<E> source;
        private final Predicate<? super E> predicate;
        private final int guaranteed;
        private final Comparator<? super E> ownerComparator;''')
    s=replace(s,'''        private SelectionSpliterator(Spliterator<E> source, Predicate<? super E> predicate) {
            this.source = source;
            this.predicate = predicate;
        }''','''        private SelectionSpliterator(Spliterator<E> source, Predicate<? super E> predicate) {
            this(source, predicate, 0, null);
        }

        private SelectionSpliterator(Spliterator<E> source, Predicate<? super E> predicate,
                int guaranteed, Comparator<? super E> ownerComparator) {
            this.source = source;
            this.predicate = predicate;
            this.guaranteed = guaranteed;
            this.ownerComparator = ownerComparator;
        }''')
    s=replace(s,'return prefix == null ? null : new SelectionSpliterator<>(prefix, predicate);',
        'return prefix == null ? null : new SelectionSpliterator<>(prefix, predicate, guaranteed, ownerComparator);')
    s=replace(s,'return source.characteristics() & (ORDERED | DISTINCT | SORTED | NONNULL | IMMUTABLE);',
        'return (source.characteristics() & (ORDERED | DISTINCT | SORTED | NONNULL | IMMUTABLE)) | guaranteed;')
    s=replace(s,'return source.getComparator();',
        'return (guaranteed & SORTED) != 0 ? ownerComparator : source.getComparator();')
    (ROOT/'CollectionViews.java.txt').write_text(s)
    old=(ROOT/'before'/C).read_bytes()
    if blob(old)!='5a9cf3274e120788dd9e9b15433d130e5f23865b':
        raise ValueError('Original C input drift')
    native='''/* SPDX-License-Identifier: Apache-2.0
 * Independent verification-only half-open RUN upper bound. Not product acceleration.
 * Modified 2026-10-17 by Hitesh Soliwal and contributors: bounded region validation
 * and query accumulation. Reuses the original complete-validation contract.
 */
#include "RunSearchJni.h"
#include <stdint.h>
#define REGION_RUNS 512
static jint reject(JNIEnv *env, const char *message) {
    jclass type=(*env)->FindClass(env,"java/lang/IllegalArgumentException");
    if(type!=NULL) { (void)(*env)->ThrowNew(env,type,message); (*env)->DeleteLocalRef(env,type); }
    return -1;
}
JNIEXPORT jint JNICALL Java_RunSearchJni_first(JNIEnv *env,jclass type,jintArray array,jint runs,jint value) {
    (void)type;
    if(array==NULL)return reject(env,"null pairs");
    jsize length=(*env)->GetArrayLength(env,array);
    if(runs<0 || runs>length/2)return reject(env,"run count exceeds pairs");
    if(runs==0)return 0;
    /* Full validation remains mandatory, even after the answer is known. Arrays
     * must remain stable for the call. No native allocation or whole-array pin. */
    jint pairs[2 * REGION_RUNS];
    jint previous_end=-1, answer=runs;
    for(jint row=0;row<runs;) {
        jint count=runs-row < REGION_RUNS ? runs-row : REGION_RUNS;
        (*env)->GetIntArrayRegion(env,array,2*row,2*count,pairs);
        if((*env)->ExceptionCheck(env))return -1;
        for(jint i=0;i<count;i++) {
            jint start=pairs[2*i],end=pairs[2*i+1];
            if(start<0||start>=end||end>65536||(row+i>0&&previous_end>=start))
                return reject(env,"unordered or invalid half-open runs");
            if(answer==runs && end>value)answer=row+i;
            previous_end=end;
        }
        row+=count;
    }
    return answer;
}
'''
    (ROOT/'run_search_jni.c.txt').write_text(native)
    print('Sealed candidate authoring: Java and JNI templates prepared; no target source written')
if __name__=='__main__': main()
