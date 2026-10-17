/* SPDX-License-Identifier: Apache-2.0
 * Independent verification-only half-open RUN upper bound. Not product acceleration.
 */
#include "RunSearchJni.h"
#include <stdint.h>
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
    jint *pairs=(*env)->GetIntArrayElements(env,array,NULL);
    if(pairs==NULL)return -1; /* Preserve pending allocation exception. */
    int valid=1;
    for(jint i=0;i<runs;i++) {
        jint start=pairs[2*i],end=pairs[2*i+1];
        if(start<0||start>=end||end>65536||(i>0&&pairs[2*i-1]>=start)){valid=0;break;}
    }
    jint low=0,high=runs;
    if(valid)while(low<high) {
        jint mid=low+(high-low)/2;
        if(pairs[2*mid+1]<=value)low=mid+1;else high=mid;
    }
    (*env)->ReleaseIntArrayElements(env,array,pairs,JNI_ABORT);
    return valid?low:reject(env,"unordered or invalid half-open runs");
}
