/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/36827ee8a798ef6ce07f6fc55a2ac371b1382c06/m3-pmem-sdk/src/test/c/PmemKmpPrefix32Oracle.c blob 2cea4059db81; recipe make/m3/optane/recipe/pmem-region-core-20261106/relocate.py v2
#include <limits.h>
#include <stddef.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

/* Independent first-hit oracle: KMP prefix links fit uint32_t for m <= 1 MiB. */
static ptrdiff_t kmp32(const unsigned char *hay, size_t n,
                        const unsigned char *pat, size_t m) {
    if (m == 0) return 0;
    if (m > n) return -1;
    if (m == 1) {
        const unsigned char *hit=(const unsigned char *)memchr(hay,pat[0],n);
        return hit ? (ptrdiff_t)(hit-hay) : -1;
    }
    if (m > SIZE_MAX/sizeof(uint32_t)) return -2;
    uint32_t *prefix=(uint32_t *)malloc(m*sizeof(uint32_t));
    if (!prefix) return -2;
    prefix[0]=0;
    size_t matched=0;
    for (size_t i=1;i<m;i++) {
        while (matched && pat[i]!=pat[matched]) matched=prefix[matched-1];
        if (pat[i]==pat[matched]) matched++;
        prefix[i]=(uint32_t)matched;
    }
    matched=0;
    ptrdiff_t found=-1;
    for (size_t i=0;i<n;i++) {
        while (matched && hay[i]!=pat[matched]) matched=prefix[matched-1];
        if (hay[i]==pat[matched]) matched++;
        if (matched==m) {found=(ptrdiff_t)(i+1-m);break;}
    }
    free(prefix);
    return found;
}
static ptrdiff_t naive(const unsigned char *hay, size_t n,
                        const unsigned char *pat, size_t m) {
    if (!m) return 0;
    if (m > n) return -1;
    for (size_t i=0;i<=n-m;i++) if (memcmp(hay+i,pat,m)==0) return (ptrdiff_t)i;
    return -1;
}
static uint32_t seed=0x9e3779b9u;
static uint32_t next(void) {
    uint32_t x=seed;
    x^=x<<13; x^=x>>17; x^=x<<5;
    return seed=x;
}
int main(void) {
    unsigned char hay[257],pat[129];
    unsigned comparisons=0;
    for (int run=0;run<8000;run++) {
        size_t n=next()%257,m=next()%129;
        for (size_t i=0;i<n;i++) hay[i]=(unsigned char)(next()%7);
        for (size_t i=0;i<m;i++) pat[i]=(unsigned char)(next()%7);
        if (m<=n && (run&1)==0) {
            size_t where=next()%(n-m+1);
            memcpy(pat,hay+where,m);
        }
        ptrdiff_t expected=naive(hay,n,pat,m);
        ptrdiff_t actual=kmp32(hay,n,pat,m);
        if (expected!=actual) {
            (void)fprintf(stderr,"native KMP regression run=%d expected=%td actual=%td\n",
                          run,expected,actual);
            return 1;
        }
        comparisons++;
    }
    size_t max=(size_t)1<<20;
    unsigned char *needle=(unsigned char *)malloc(max);
    unsigned char *corpus=(unsigned char *)malloc(2*max+9);
    if (!needle || !corpus) {free(needle);free(corpus);return 2;}
    memset(needle,'a',max); needle[max-1]='b';
    memset(corpus,'a',2*max+9); corpus[2*max+8]='b';
    if (kmp32(corpus,2*max+9,needle,max)!=(ptrdiff_t)(max+9)) return 3;
    comparisons++;
    if (kmp32(corpus,2*max+8,needle,max)!=-1) return 4;
    comparisons++;
    free(corpus);free(needle);
    (void)printf("PASS C17 native KMP prefix32: %u comparisons, max_pattern=%zu, prefix32=%zu bytes\n",
                 comparisons,max,max*sizeof(uint32_t));
    return 0;
}
