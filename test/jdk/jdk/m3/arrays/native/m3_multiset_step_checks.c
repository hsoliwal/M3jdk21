/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
#include "m3_multiset_step.h"
#include <limits.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>

static uint32_t seed = UINT32_C(0x4d334d53);
static uint32_t next_random(void) {
 seed ^= seed << 13U; seed ^= seed >> 17U; seed ^= seed << 5U; return seed;
}

/* Independent frequency oracle: count each signed value across both arrays. */
static int expected_count(int a, int b, int mode) {
 if (mode == M3_MULTI_UNION) return a > b ? a : b;
 if (mode == M3_MULTI_INTERSECT) return a < b ? a : b;
 if (mode == M3_MULTI_SUBTRACT) return a > b ? a - b : 0;
 return a > b ? a - b : b - a;
}

int main(void) {
 size_t cases = 0U;
 for (int trial = 0; trial < 32768; trial++) {
  int32_t left[40], right[40];
  size_t na = (size_t)(next_random() % 41U);
  size_t nb = (size_t)(next_random() % 41U);
  int counts_a[13]={0}, counts_b[13]={0};
  for(size_t i=0;i<na;i++) {
   int chosen=(int)(next_random()%13U);
   left[i]=(int32_t)(chosen-6); counts_a[chosen]++;
  }
  for(size_t i=0;i<nb;i++) {
   int chosen=(int)(next_random()%13U);
   right[i]=(int32_t)(chosen-6); counts_b[chosen]++;
  }
  for(size_t i=0;i<na;i++)for(size_t j=i+1;j<na;j++) if(left[j]<left[i]) {
   int32_t t=left[i];left[i]=left[j];left[j]=t;
  }
  for(size_t i=0;i<nb;i++)for(size_t j=i+1;j<nb;j++) if(right[j]<right[i]) {
   int32_t t=right[i];right[i]=right[j];right[j]=t;
  }
  for(int mode=0;mode<4;mode++) {
   int observed[13]={0};
   size_t a=0U,b=0U;
   while(a<na||b<nb) {
    m3_multiset_action event;
    if(!m3_multiset_step(mode,a<na,a<na?left[a]:0,b<nb,b<nb?right[b]:0,&event)) {
     (void)fprintf(stderr,"FAIL: unexpected step refusal\n");return EXIT_FAILURE;
    }
    if(event.emit) observed[(int)event.value + 6]++;
    a+=(size_t)event.take_left;
    b+=(size_t)event.take_right;
   }
   for(int k=0;k<13;k++)if(observed[k]!=expected_count(counts_a[k],counts_b[k],mode)) {
    (void)fprintf(stderr,"FAIL: count trial=%d mode=%d key=%d got=%d want=%d\n",
      trial,mode,k,observed[k],expected_count(counts_a[k],counts_b[k],mode));return EXIT_FAILURE;
   }
   cases++;
  }
 }
 m3_multiset_action out;
 if(m3_multiset_step(-1,1,INT32_MIN,1,INT32_MAX,&out)!=0) return EXIT_FAILURE;
 if(m3_multiset_step(4,1,INT32_MIN,1,INT32_MAX,&out)!=0) return EXIT_FAILURE;
 if(m3_multiset_step(0,0,0,0,0,&out)!=0) return EXIT_FAILURE;
 (void)printf("M3_MULTISET_NATIVE_PASS trials=%zu\n",cases);
 return EXIT_SUCCESS;
}
