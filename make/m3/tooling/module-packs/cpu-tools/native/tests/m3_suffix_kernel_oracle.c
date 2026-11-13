/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project */
/* SPDX-License-Identifier: Apache-2.0 */
/* Intentionally compiled only as a fixture: independent naïve byte substring oracle. */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <limits.h>
#include "../m3_byte_suffix_index.c"

static unsigned long long checks;
static uint32_t state = 0x2914c15u;
#define ASSERT(ok, label) do { checks++; if (!(ok)) { \
    fprintf(stderr, "M3_SUFFIX_C_ORACLE_FAIL %s check=%llu\n", label, checks); exit(2); } \
} while (0)
static uint32_t random32(void) {
    state ^= state << 13; state ^= state >> 17; state ^= state << 5;
    return state;
}

static void suffix_order(const jbyte *text, jsize n, jint *sa) {
    for (jsize i = 0; i < n; i++) sa[i] = i;
    /* Independent insertion sorting of all short test suffixes by unsigned bytes. */
    for (jsize i = 1; i < n; i++) {
        jint value = sa[i];
        jsize j = i;
        while (j > 0) {
            jint earlier = sa[j-1];
            jsize a = earlier, b = value;
            int order = 0;
            while (a < n && b < n) {
                int x = (unsigned char)text[a++];
                int y = (unsigned char)text[b++];
                if (x != y) {order = x < y ? -1 : 1; break;}
            }
            if (order == 0) order = (n - a < n - b) ? -1 : ((n-a == n-b) ? 0 : 1);
            if (order <= 0) break;
            sa[j] = sa[j-1];
            j--;
        }
        sa[j] = value;
    }
}
static void min_tree(const jint *sa, jsize n, jint *tree, jsize base) {
    for (jsize i=0;i<base*2;i++) tree[i] = INT_MAX;
    for (jsize i=0;i<n;i++) tree[base+i]=sa[i];
    for (jsize i=base-1;i>0;i--) {
        tree[i] = tree[i*2]<tree[i*2+1] ? tree[i*2] : tree[i*2+1];
    }
}
static void brute(const jbyte *text, jsize n,const jbyte *needle, jsize len, jint *count, jint *first) {
    *count = 0;
    *first = -1;
    if (len == 0) { *count = n+1; *first=0; return; }
    for (jsize i=0;i+len<=n;i++) {
        int match=1;
        for (jsize k=0;k<len;k++)if (text[i+k] != needle[k]) {match=0;break;}
        if (match) {if (*first<0) *first=i;(*count)++;}
    }
}
static void test_batch(const jbyte *text,jsize n, const jbyte *packed,
                       const jint *offsets, jsize queries,const char *tag) {
    jint *sa=malloc((size_t)(n+1)*sizeof(jint));
    jsize base=1;while(base<n)base*=2;
    jint *tree=malloc((size_t)base*2*sizeof(jint));
    jint *out=malloc((size_t)(queries*2+1)*sizeof(jint));
    ASSERT(sa!=NULL && tree!=NULL && out!=NULL,"malloc");
    suffix_order(text,n,sa);
    min_tree(sa,n,tree,base);
    out[queries*2]=INT_MAX;
    m3_suffix_query_batch(text,n,sa,tree,base,packed,offsets,queries,out);
    ASSERT(out[queries*2]==INT_MAX,"output sentinel");
    for(jsize i=0;i<queries;i++){
        jint count,first;
        brute(text,n,packed+offsets[i],offsets[i+1]-offsets[i],&count,&first);
        ASSERT(out[i*2]==count,tag);
        ASSERT(out[i*2+1]==first,tag);
    }
    free(sa);free(tree);free(out);
}

int main(void) {
    const jbyte signed_values[]={0,(jbyte)0x80,(jbyte)0xff,0,(jbyte)0xff};
    const jbyte signed_queries[]={0,(jbyte)0xff,(jbyte)0xff,0,(jbyte)0xff};
    const jint signed_offsets[]={0,0,1,2,3,5};
    test_batch(signed_values,5,signed_queries,signed_offsets,5,"signed extreme");
    const jbyte empty[]={0};
    const jint zero_off[]={0};
    const jint two_empties[]={0,0,0};
    test_batch(empty,0,empty,zero_off,0,"zero batch");
    test_batch(empty,0,empty,two_empties,2,"empty matches");
    for(int iteration=0;iteration<3500;iteration++){
        jsize n=(jsize)(random32()%75u);
        jbyte text[75];
        for(jsize i=0;i<n;i++)text[i]=(jbyte)(random32()&255u);
        if (iteration%5==0) for(jsize i=0;i<n;i++)text[i]=(jbyte)(iteration%3);
        jbyte packed[640];
        jint offsets[33]={0};
        jsize q=(jsize)(random32()%33u);
        jsize cursor=0;
        for(jsize i=0;i<q;i++){
            jsize length=(jsize)(random32()%19u);
            for(jsize k=0;k<length;k++) packed[cursor+k]=(jbyte)(random32()&255u);
            if (iteration%3==0 && length>0) memset(packed+cursor,0,(size_t)length);
            cursor+=length;
            offsets[i+1]=cursor;
        }
        test_batch(text,n,packed,offsets,q,"random batch");
    }
    /* Native large-index sorted suffix fixture: identical bytes produce descending offsets. */
    const jsize n=30000;
    jbyte *text=malloc((size_t)n);
    jint *sa=malloc((size_t)n*sizeof(jint));
    jint *tree=malloc((size_t)n*4*sizeof(jint));
    ASSERT(text!=NULL && sa!=NULL && tree!=NULL,"large malloc");
    memset(text,0xff,(size_t)n);
    for(jsize i=0;i<n;i++)sa[i]=n-i-1;
    jsize base=1;while(base<n)base<<=1;
    free(tree);tree=malloc((size_t)base*2*sizeof(jint));
    ASSERT(tree!=NULL,"large tree malloc");
    min_tree(sa,n,tree,base);
    const jbyte packed[]={ (jbyte)0xff, (jbyte)0xff, (jbyte)0xff, 0, (jbyte)0xff };
    const jint off[]={0,0,1,3,4,5};
    jint out[11]={0};out[10]=INT_MAX;
    m3_suffix_query_batch(text,n,sa,tree,base,packed,off,5,out);
    ASSERT(out[0]==n+1 && out[1]==0,"large empty");
    ASSERT(out[2]==n && out[3]==0,"large one");
    ASSERT(out[4]==n-1 && out[5]==0,"large double");
    ASSERT(out[6]==0 && out[7]==-1,"large missing");
    ASSERT(out[8]==n && out[9]==0,"large repeats");
    ASSERT(out[10]==INT_MAX,"large canary");
    free(text);free(sa);free(tree);
    printf("M3_BYTE_SUFFIX_C_PASS checks=%llu\n",checks);
    return 0;
}
