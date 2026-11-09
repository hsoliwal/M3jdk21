// Copyright 2026 Hitesh Soliwal and contributors
// Relocated from Synexia crate m3fs-memchr-bitmap-20261102/qualification/m3_contains_oracle.c (commit 1899bf3e4c7):
// the driver (seed, 500,000 randomized + 66,564 periodic cases, boundary checks) is unchanged; its two private
// copies of the search function are replaced by the real m3_contains bodies, extracted by native-gate.sh from
// the M3JDK master m3_fs_jni.c (baseline) and from the stage-02 image (candidate).
// SPDX-License-Identifier: Apache-2.0
#include <assert.h>
#include <stdint.h>
#include <stddef.h>
#include <stdio.h>
#include <string.h>
#include <time.h>

#include "m3_contains_baseline.inc"
#include "m3_contains_candidate.inc"

static uint64_t state = UINT64_C(0x4d33464f53534341);
static uint32_t next(void) {state ^= state << 13; state ^= state >> 7; state ^= state << 17;return (uint32_t)(state >> 16);}
int main(void) {
  uint8_t source[257], needle[257];
  uint64_t checks = 0;
  for (unsigned int trial=0;trial<500000U;trial++) {
    size_t n = (size_t)(next() % 257U), m = (size_t)(next() % 257U);
    unsigned int mode = next()%8U;
    for(size_t i=0;i<n;i++)source[i] = mode<2? (uint8_t)'a':(uint8_t)next();
    for(size_t i=0;i<m;i++)needle[i] = mode<2? (uint8_t)'a':(uint8_t)next();
    if (mode==3 && m<=n) {size_t at = (size_t)(next()%(uint32_t)(n-m+1)); memcpy(source+at,needle,m);}
    int before=baseline(source,n,needle,m);
    int after=candidate(source,n,needle,m);
    if(before!=after){fprintf(stderr,"MISMATCH %u length=%zu needle=%zu\n",trial,n,m);return 1;}
    checks++;
  }
  memset(source, 'a', sizeof(source));memset(needle,'a',sizeof(needle));
  for (size_t n=0;n<=257;n++) for(size_t m=0;m<=257;m++){
    int a=baseline(source,n,needle,m),b=candidate(source,n,needle,m);
    if(a!=b){fprintf(stderr,"periodic mismatch %zu %zu\n",n,m);return 1;}checks++;
  }
  // Guard, empty, zero and high-byte boundaries.
  source[0]=255U;needle[0]=255U;
  if(candidate(source,1,needle,1)!=1 || candidate(source,1,needle,0)!=0 || candidate(source,0,needle,1)!=0) return 2;
  printf("M3FS_NATIVE_MEMCHR_DIFFERENTIAL_PASS cases=%llu\n",(unsigned long long)checks);
  return 0;
}
