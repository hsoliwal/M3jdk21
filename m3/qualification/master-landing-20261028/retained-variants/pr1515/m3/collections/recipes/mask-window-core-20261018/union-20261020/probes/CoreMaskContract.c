/* SPDX-License-Identifier: Apache-2.0 */
/* Independent per-bit oracle for the pinned common JNI header, not a new runtime. */
#include <stdint.h>
#include <limits.h>
#include <stdio.h>
#include <stdlib.h>
#include "m3_jni_core.h"
#define CHECK(c) do {checks++;if(!(c)){fprintf(stderr,"failure line %d\n",__LINE__);return 1;}} while(0)
int main(void) {
 uint64_t checks=0;
 for(int state=0;state<256;state++)for(int req=0;req<256;req++)for(int ex=0;ex<256;ex++) {
  int expected=1;
  for(unsigned int bit=0;bit<8;bit++) {
   unsigned int flag=1u<<bit;
   if((((unsigned int)req&flag)!=0 && ((unsigned int)state&flag)==0)
      || (((unsigned int)ex&flag)!=0 && ((unsigned int)state&flag)!=0))expected=0;
  }
  CHECK(m3_jni_matches_u8((jbyte)state,req,ex)==expected);
 }
 int32_t values[]={INT32_MIN,-1,0,1,255,256,INT32_MAX-1,INT32_MAX};
 for(size_t a=0;a<8;a++)for(size_t b=0;b<8;b++)for(size_t c=0;c<8;c++){
  int64_t cap=values[a],first=values[b],count=values[c];
  int expected=cap>=0&&first>=0&&count>=0&&first+count<=cap;
  CHECK(m3_jni_valid_window((jsize)cap,(jint)first,(jint)count)==expected);
 }
 CHECK(!m3_jni_matches_u8((jbyte)255,-1,0));CHECK(!m3_jni_matches_u8(0,0,256));
 printf("COMMON_CORE checks=%llu PASS\n",(unsigned long long)checks);return 0;
}
