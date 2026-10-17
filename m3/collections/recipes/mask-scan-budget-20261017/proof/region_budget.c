/* SPDX-License-Identifier: Apache-2.0
 * Instrumented JNI function table around the ACTUAL C implementation.
 * This boundary fixture complements, and never substitutes for, real Java/JNI runs.
 * Virtual array lengths exercise INT_MAX without allocating an impossible test heap.
 */
#include <jni.h>
#include <limits.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "m3_index_mask_window.c"

typedef struct { jsize length; int kind; } Array;
static Array rows, states, output;
static jint data[4100];
static jint low, high;
static int mode, pending_exception;
static long checks, cases, state_reads, row_reads, writes;
static void check(int valid) { checks++; if(!valid){fprintf(stderr,"region budget check failed %ld\n",checks);exit(2);} }
static jbyte flag(jint i) {return mode==0 ? 0 : (jbyte)((uint32_t)i*37u+7u);}
static jint value(jint i) {return i%101-50;}
static jsize JNICALL get_length(JNIEnv *env,jarray object) { (void)env;check(object!=NULL);return ((Array*)object)->length; }
static jboolean JNICALL same(JNIEnv *env,jobject a,jobject b) { (void)env;return a==b?JNI_TRUE:JNI_FALSE; }
static jboolean JNICALL exception(JNIEnv *env) { (void)env;return pending_exception?JNI_TRUE:JNI_FALSE; }
static jclass JNICALL find_class(JNIEnv *env,const char *name) { (void)env;check(strcmp(name,"java/lang/IllegalArgumentException")==0);return (jclass)&output; }
static jint JNICALL throw_new(JNIEnv *env,jclass cls,const char *message) { (void)env;(void)cls;check(message!=NULL);pending_exception=1;return 0; }
static void JNICALL delete_local(JNIEnv *env,jobject object) { (void)env;(void)object; }
static void range(jint start,jint length) {
    check(!pending_exception);check(start>=low);check(length>=0&&length<=128);
    check((jlong)start+length<=high);
}
static void JNICALL get_states(JNIEnv *env,jbyteArray object,jsize start,jsize length,jbyte *buffer) {
    (void)env;check(object==(jbyteArray)&states);range(start,length);state_reads+=length;
    for(jsize i=0;i<length;i++)buffer[i]=flag(start+i);
}
static void JNICALL get_rows(JNIEnv *env,jintArray object,jsize start,jsize length,jint *buffer) {
    (void)env;check(object==(jintArray)&rows);range(start,length);row_reads+=length;
    for(jsize i=0;i<length;i++)buffer[i]=value(start+i);
}
static void JNICALL set_rows(JNIEnv *env,jintArray object,jsize start,jsize length,const jint *buffer) {
    (void)env;check(object==(jintArray)&output);check(!pending_exception);
    check(start>=2&&length>0&&length<=128&&(jlong)start+length<=output.length-2);writes+=length;
    for(jsize i=0;i<length;i++)data[start+i]=buffer[i];
}
static const struct JNINativeInterface_ table={.GetArrayLength=get_length,.IsSameObject=same,
    .ExceptionCheck=exception,.FindClass=find_class,.ThrowNew=throw_new,.DeleteLocalRef=delete_local,
    .GetByteArrayRegion=get_states,.GetIntArrayRegion=get_rows,.SetIntArrayRegion=set_rows};
static void one(JNIEnv *env,jint n,jint first,jint budget,jint req,jint exc,jint limit) {
    cases++;rows=(Array){n,0};states=(Array){n,1};output=(Array){limit+4,2};
    for(int i=0;i<4100;i++)data[i]=0x12345678;
    low=first;high=first+(n-first<budget?n-first:budget);state_reads=0;row_reads=0;writes=0;pending_exception=0;
    jlong got=Java_com_m3_collections_M3IndexMaskWindow_scanBoundedNative(env,NULL,
        (jintArray)&rows,(jbyteArray)&states,first,budget,req,exc,(jintArray)&output,2,limit);
    check(!pending_exception);jint i=first,k=0;
    while(i<high&&k<limit){unsigned int bits=(unsigned char)flag(i);
        if((bits&(unsigned int)req)==(unsigned int)req&&(bits&(unsigned int)exc)==0u){check(data[2+k]==value(i));k++;}i++;}
    check(got==m3_window_result(i,k));check(state_reads<=budget&&row_reads<=budget&&writes==k);
    for(int j=0;j<2;j++)check(data[j]==0x12345678);
    for(int j=k+2;j<limit+4;j++)check(data[j]==0x12345678);
}
int main(void) {
    JNIEnv env=&table;
    jint lengths[]={0,1,127,128,129,4096,4097,1000000,INT_MAX};
    jint budgets[]={0,1,127,128,129,4095,4096};
    jint limits[]={0,1,129,4096};
    for(mode=0;mode<2;mode++)for(unsigned n=0;n<sizeof(lengths)/sizeof(lengths[0]);n++)
        for(unsigned b=0;b<sizeof(budgets)/sizeof(budgets[0]);b++)for(unsigned k=0;k<sizeof(limits)/sizeof(limits[0]);k++)
            for(int pos=0;pos<3;pos++)for(int mask=0;mask<4;mask++) {
                jint len=lengths[n],first=pos==0?0:pos==1?len/2:len;
                one(&env,len,first,budgets[b],mask==0?0:128,mask==3?128:mask==2?2:0,limits[k]);
            }
    mode=0;one(&env,1000000,0,256,128,0,16);
    printf("RejectMillionBudget256 cursor=256 written=0 stateReads=%ld rowReads=%ld\n",state_reads,row_reads);
    printf("RegionBudgetFixture cases=%ld checks=%ld virtualMaxLength=INT_MAX\n",cases,checks);
    return 0;
}
