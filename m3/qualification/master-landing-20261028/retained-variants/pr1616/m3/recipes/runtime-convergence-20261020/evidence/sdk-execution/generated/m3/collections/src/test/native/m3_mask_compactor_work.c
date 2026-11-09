/* SPDX-License-Identifier: Apache-2.0 */
/* Test-only JNI function-table instrumentation of the actual included C owner.
 * Separate from real JVM -Xcheck:jni qualification. No production export added. */
#include <assert.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#ifndef OWNER_SOURCE
#error OWNER_SOURCE must identify the exact C owner under test
#endif
#include OWNER_SOURCE

typedef struct { jsize n; unsigned char data[16384]; } ProbeArray;
static ProbeArray ints, longs, states, output;
static int state_reads, row_reads, pending, mutation, fault, checks;
static const char *exception_name;
static jsize JNICALL length(JNIEnv *env, jarray a) { (void)env; return ((ProbeArray *)(void *)a)->n; }
static void byte_region(JNIEnv *env,jbyteArray a,jsize at,jsize n,jbyte *to) {
    (void)env;state_reads++;
    ProbeArray *p=(ProbeArray *)(void *)a;
    assert(at>=0 && n>=0 && at<=p->n && n<=p->n-at);
    if(fault && state_reads==fault){pending=1;exception_name="injected-read-failure";return;}
    if(mutation && state_reads==5) p->data[17]=(unsigned char)(mutation==1?1:0);
    memcpy(to,p->data+(size_t)at,(size_t)n);
}
static void int_region(JNIEnv *env,jintArray a,jsize at,jsize n,jint *to) {
    (void)env;row_reads++;ProbeArray *p=(ProbeArray *)(void *)a;
    assert(at>=0 && n>=0 && at<=p->n && n<=p->n-at);
    memcpy(to,p->data+(size_t)at*sizeof(jint),(size_t)n*sizeof(jint));
}
static void long_region(JNIEnv *env,jlongArray a,jsize at,jsize n,jlong *to) {
    (void)env;row_reads++;ProbeArray *p=(ProbeArray *)(void *)a;
    assert(at>=0 && n>=0 && at<=p->n && n<=p->n-at);
    memcpy(to,p->data+(size_t)at*sizeof(jlong),(size_t)n*sizeof(jlong));
}
static jintArray new_int(JNIEnv *env,jsize n){(void)env;assert(n>=0&&n<=1024);output.n=n;return (jintArray)(void *)&output;}
static jlongArray new_long(JNIEnv *env,jsize n){(void)env;assert(n>=0&&n<=1024);output.n=n;return (jlongArray)(void *)&output;}
static void set_int(JNIEnv *env,jintArray a,jsize at,jsize n,const jint *from){(void)env;assert(a==(jintArray)(void *)&output);assert(at>=0&&n<=output.n-at);memcpy(output.data+(size_t)at*sizeof(jint),from,(size_t)n*sizeof(jint));}
static void set_long(JNIEnv *env,jlongArray a,jsize at,jsize n,const jlong *from){(void)env;assert(a==(jlongArray)(void *)&output);assert(at>=0&&n<=output.n-at);memcpy(output.data+(size_t)at*sizeof(jlong),from,(size_t)n*sizeof(jlong));}
static jboolean exception_check(JNIEnv *env){(void)env;return (jboolean)pending;}
static jclass find_class(JNIEnv *env,const char *name){(void)env;exception_name=name;return (jclass)(void *)&ints;}
static jint throw_new(JNIEnv *env,jclass c,const char *message){(void)env;(void)c;(void)message;pending=1;return 0;}
static void delete_local(JNIEnv *env,jobject o){(void)env;(void)o;}
static const struct JNINativeInterface_ functions={
 .GetArrayLength=length,.GetByteArrayRegion=byte_region,.GetIntArrayRegion=int_region,
 .GetLongArrayRegion=long_region,.NewIntArray=new_int,.NewLongArray=new_long,
 .SetIntArrayRegion=set_int,.SetLongArrayRegion=set_long,.ExceptionCheck=exception_check,
 .FindClass=find_class,.ThrowNew=throw_new,.DeleteLocalRef=delete_local
};
static void reset(int density){
 memset(&ints,0,sizeof(ints));memset(&longs,0,sizeof(longs));memset(&states,0,sizeof(states));
 ints.n=1024;longs.n=1024;states.n=1024;output.n=0;
 for(int i=0;i<1024;i++){
  jint x=i%17-8;jlong y=i%17-8;
  memcpy(ints.data+(size_t)i*sizeof(x),&x,sizeof(x));memcpy(longs.data+(size_t)i*sizeof(y),&y,sizeof(y));
  states.data[i]=(unsigned char)(density==2||(density==1&&i>=512&&i<768)?1:0);
 }
 state_reads=row_reads=pending=mutation=fault=0;exception_name=NULL;
}
int main(int argc,char **argv){
 assert(argc==2);int optimized=atoi(argv[1]);JNIEnv env=&functions;
 for(int lane=0;lane<4;lane++)for(int density=0;density<3;density++){
  reset(density);jarray result;
  if(lane==0)result=Java_com_m3_collections_M3IndexMaskCompactor_selectNative(&env,NULL,(jintArray)(void *)&ints,(jbyteArray)(void *)&states,1);
  else if(lane==1)result=Java_com_m3_collections_M3IndexMaskCompactor_selectRangeNative(&env,NULL,(jintArray)(void *)&ints,(jbyteArray)(void *)&states,0,1024,1);
  else if(lane==2)result=Java_com_m3_collections_M3IndexMaskCompactor_selectWindowNative(&env,NULL,(jintArray)(void *)&ints,0,(jbyteArray)(void *)&states,0,1024,1);
  else result=Java_com_m3_collections_M3IndexMaskCompactor_selectLongWindowNative(&env,NULL,(jlongArray)(void *)&longs,0,(jbyteArray)(void *)&states,0,1024,1);
  assert(result!=NULL&&!pending);assert(output.n==(density==0?0:density==1?256:1024));
  assert(state_reads==8);assert(row_reads==(optimized?(density==0?0:density==1?1:4):4));checks+=4;
  printf("lane=%d density=%d states=%d payload=%d result=%d\n",lane,density,state_reads,row_reads,output.n);
 }
 // Count/emit disagreement: a new match and a removed match both fail closed.
 for(int which=1;which<=2;which++){
  reset(which==1?0:2);mutation=which;
  jintArray result=Java_com_m3_collections_M3IndexMaskCompactor_selectWindowNative(&env,NULL,(jintArray)(void *)&ints,0,(jbyteArray)(void *)&states,0,1024,1);
  assert(result==NULL&&pending&&strcmp(exception_name,"java/util/ConcurrentModificationException")==0);checks++;
 }
 reset(2);fault=5;
 assert(Java_com_m3_collections_M3IndexMaskCompactor_selectWindowNative(&env,NULL,(jintArray)(void *)&ints,0,(jbyteArray)(void *)&states,0,1024,1)==NULL);
 assert(pending&&strcmp(exception_name,"injected-read-failure")==0);checks+=2;
 reset(0);
 assert(Java_com_m3_collections_M3IndexMaskCompactor_selectLongWindowNative(&env,NULL,(jlongArray)(void *)&longs,0,(jbyteArray)(void *)&states,0,1024,0)!=NULL);
 assert(state_reads==0&&row_reads==4&&output.n==1024);checks+=2;
 printf("NATIVE_WORK_PROBE checks=%d PASS (function-table instrument; actual JVM proof separate)\n",checks);
 return 0;
}
