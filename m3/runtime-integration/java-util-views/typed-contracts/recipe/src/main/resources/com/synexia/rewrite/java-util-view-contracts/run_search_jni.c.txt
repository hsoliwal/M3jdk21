/* SPDX-License-Identifier: Apache-2.0
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
