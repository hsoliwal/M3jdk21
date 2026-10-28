/* SPDX-License-Identifier: Apache-2.0 */
/* Test-only JNI function-table instrumentation of the actual included C owner.
 * Separate from real JVM -Xcheck:jni qualification. No production export added. */
#include <assert.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <stdint.h>
#ifndef OWNER_SOURCE
#error OWNER_SOURCE must identify the exact C owner under test
#endif
#include OWNER_SOURCE

typedef struct { jsize n; unsigned char data[16384]; } ProbeArray;
static ProbeArray ints, longs, states, output;
static int state_reads, row_reads, row_elements, pending, mutation, fault, checks;
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
    (void)env;row_reads++;row_elements+=n;ProbeArray *p=(ProbeArray *)(void *)a;
    assert(at>=0 && n>=0 && at<=p->n && n<=p->n-at);
    memcpy(to,p->data+(size_t)at*sizeof(jint),(size_t)n*sizeof(jint));
}
static void long_region(JNIEnv *env,jlongArray a,jsize at,jsize n,jlong *to) {
    (void)env;row_reads++;row_elements+=n;ProbeArray *p=(ProbeArray *)(void *)a;
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
 state_reads=row_reads=row_elements=pending=mutation=fault=0;exception_name=NULL;
}

static jarray call(JNIEnv *env,int lane,int ro,int so,int n,int mask){
 if(lane==0)return Java_com_m3_collections_M3IndexMaskCompactor_selectNative(env,NULL,(jintArray)(void *)&ints,(jbyteArray)(void *)&states,mask);
 if(lane==1)return Java_com_m3_collections_M3IndexMaskCompactor_selectRangeNative(env,NULL,(jintArray)(void *)&ints,(jbyteArray)(void *)&states,ro,n,mask);
 if(lane==2)return Java_com_m3_collections_M3IndexMaskCompactor_selectWindowNative(env,NULL,(jintArray)(void *)&ints,ro,(jbyteArray)(void *)&states,so,n,mask);
 return Java_com_m3_collections_M3IndexMaskCompactor_selectLongWindowNative(env,NULL,(jlongArray)(void *)&longs,ro,(jbyteArray)(void *)&states,so,n,mask);
}
static void scenario(JNIEnv *env,int refined,int lane,int n,int pattern){
 reset(0);
 int ro=lane==0?0:17,so=lane<2?0:13;
 ints.n=longs.n=ro+n; states.n=so+n;
 memset(states.data,0,sizeof(states.data));
 for(int i=0;i<ints.n;i++){
  jint x=(i%7)-3; jlong y=i%2==0?INT64_MIN+(jlong)i:INT64_MAX-(jlong)i;
  memcpy(ints.data+(size_t)i*sizeof(x),&x,sizeof(x));memcpy(longs.data+(size_t)i*sizeof(y),&y,sizeof(y));
 }
 int mask=pattern==-4?0:1;
 for(int i=0;i<n;i++) states.data[so+i]=(unsigned char)(
     pattern>=0?i==pattern:pattern==-1?0:pattern==-2?1:pattern==-3?(i%97==41):pattern==-5?(i==0||i==n-1):0);
 int expectedCount=0,expectedReads=0,expectedElements=0;
 for(int base=0;base<n;base+=256){
  int end=n-base<256?n:base+256,first=-1,last=-1;
  for(int i=base;i<end;i++)if(mask==0 || states.data[so+i]==1){
   if(first<0) { first=i; }
   last=i;expectedCount++;
  }
  if(first>=0){expectedReads++;expectedElements+=refined?last-first+1:end-base;}
 }
 jarray result=call(env,lane,ro,so,n,mask);
 assert(result!=NULL&&!pending);checks++;
 assert(output.n==expectedCount);checks++;
 assert(row_reads==expectedReads && row_elements==expectedElements);checks++;
 assert(state_reads==(mask==0?0:2*((n+255)/256)));checks++;
 int w=0;
 for(int i=0;i<n;i++)if(mask==0 || states.data[so+i]==1){
  if(lane==3){jlong a,b;memcpy(&a,longs.data+(size_t)(ro+i)*sizeof(a),sizeof(a));memcpy(&b,output.data+(size_t)w*sizeof(b),sizeof(b));assert(a==b);}
  else {jint a,b;memcpy(&a,ints.data+(size_t)(ro+i)*sizeof(a),sizeof(a));memcpy(&b,output.data+(size_t)w*sizeof(b),sizeof(b));assert(a==b);}
  w++;checks++;
 }
 if(n==1024 && (pattern==17||pattern==-3||pattern==-2))printf("SPAN lane=%d pattern=%d payload_calls=%d payload_elements=%d state_calls=%d result=%d\n",lane,pattern,row_reads,row_elements,state_reads,output.n);
}
int main(int argc,char **argv){
 assert(argc==2);int refined=atoi(argv[1]);JNIEnv env=&functions;
 int sizes[]={0,1,255,256,257,600,1024};int cases=0;
 for(int lane=0;lane<4;lane++)for(size_t k=0;k<sizeof(sizes)/sizeof(sizes[0]);k++){
  int n=sizes[k];for(int p=-5;p<n;p++){scenario(&env,refined,lane,n,p);cases++;}
 }
 for(int lane=0;lane<4;lane++)for(int which=1;which<=2;which++){
  reset(which==1?0:2);mutation=which;
  jarray result=call(&env,lane,0,0,1024,1);
  assert(result==NULL&&pending&&strcmp(exception_name,"java/util/ConcurrentModificationException")==0);checks++;
 }
 for(int lane=0;lane<4;lane++)for(int at=1;at<=8;at++){
  reset(2);fault=at;
  assert(call(&env,lane,0,0,1024,1)==NULL);
  assert(pending&&strcmp(exception_name,"injected-read-failure")==0);checks+=2;
 }
 printf("NATIVE_SPAN_UNION cases=%d checks=%d PASS (actual included C; function-table instrument)\n",cases,checks);
 return 0;
}
