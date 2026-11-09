/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project */
/* SPDX-License-Identifier: Apache-2.0 */
#define M3_RADIX_COUNT_PROBES
#include "../m3_byte_radix_trie.c"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define MAX_TEST_NODES 256
static jint temp_child[MAX_TEST_NODES][256];
static jint temp_prefix[MAX_TEST_NODES];
static jint node_start[MAX_TEST_NODES+1];
static jint packed_edge[MAX_TEST_NODES];
static unsigned long long checks;
static unsigned int random_state = 0x4d335241u;

static unsigned int rnd(void) {
    random_state ^= random_state << 13;
    random_state ^= random_state >> 17;
    random_state ^= random_state << 5;
    return random_state;
}
static void require(int condition, const char *reason) {
    checks++;
    if (!condition) {
        fprintf(stderr, "M3_RADIX_KERNEL_FAILURE: %s @ %llu\n", reason, checks);
        exit(3);
    }
}
static int starts_with(const jbyte *key, int key_length,
                       const jbyte *query, int query_length) {
    return query_length <= key_length &&
        memcmp(key, query, (size_t)query_length) == 0;
}
static jint build(int count, const jbyte keys[][12], const int *len, int *out_nodes) {
    for (int n=0;n<MAX_TEST_NODES;n++) {
        temp_prefix[n] = 0;
        for(int i=0;i<256;i++)temp_child[n][i]=-1;
    }
    jint nodes=1;
    for (int k=0;k<count;k++) {
        jint node=0;
        temp_prefix[0]++;
        for (int pos=0;pos<len[k];pos++) {
            const int byte_value=(unsigned char)keys[k][pos];
            if (temp_child[node][byte_value]==-1) {
                require(nodes<MAX_TEST_NODES,"builder capacity");
                temp_child[node][byte_value]=nodes++;
            }
            node=temp_child[node][byte_value];
            temp_prefix[node]++;
        }
    }
    node_start[0]=0;
    for (int node=0;node<nodes;node++) {
        int degree=0;
        for(int symbol=0;symbol<256;symbol++)
            if(temp_child[node][symbol]>=0)degree++;
        node_start[node+1]=node_start[node]+degree;
    }
    require(node_start[nodes]==nodes-1,"tree edges");
    for (int node=0;node<nodes;node++) {
        int out=node_start[node];
        for (int symbol=0;symbol<256;symbol++) {
            if(temp_child[node][symbol]>=0) {
                packed_edge[out++]=(jint)(((uint32_t)symbol<<24)|(uint32_t)temp_child[node][symbol]);
            }
        }
        require(out==node_start[node+1],"sorted adjacency completeness");
    }
    *out_nodes=nodes;
    return node_start[nodes];
}
int main(void) {
    for (int iteration=0;iteration<2200;iteration++) {
        jbyte keys[24][12]={{0}};
        int lengths[24]={0};
        int count=(int)(rnd()%25u);
        for(int k=0;k<count;k++) {
            lengths[k]=(int)(rnd()%11u);
            for(int j=0;j<lengths[k];j++) {
                unsigned int value=rnd();
                keys[k][j]=(jbyte)(value & 255u);
                if ((value&3u)==0) keys[k][j]=(jbyte)(value%5u);
            }
        }
        int nodes;
        jint edge_count=build(count,(const jbyte (*)[12])keys,lengths,&nodes);
        for(int q=0;q<65;q++) {
            jbyte query[12]={0};
            int qlen=(int)(rnd()%12u);
            for(int j=0;j<qlen;j++)query[j]=(jbyte)rnd();
            if ((rnd()&1u) && count>0) {
                int chosen=(int)(rnd()%(unsigned int)count);
                qlen=(int)(rnd()%(unsigned int)(lengths[chosen]+1));
                memcpy(query,keys[chosen],(size_t)qlen);
            }
            int expected=0;
            for(int k=0;k<count;k++)expected+=starts_with(keys[k],lengths[k],query,qlen);
            jint actual=-1;
            require(m3_radix_prefix_count(node_start,nodes,packed_edge,edge_count,
                    temp_prefix,query,qlen,0,qlen,&actual)==0,"kernel success");
            require(actual==expected,"kernel independent prefix frequency");
            require(m3_radix_prefix_count(node_start,nodes,packed_edge,edge_count,
                    temp_prefix,query,qlen,-1,1,&actual)!=0,"invalid start rejected");
            require(m3_radix_prefix_count(node_start,nodes,packed_edge,edge_count,
                    temp_prefix,query,qlen,0,qlen+1,&actual)!=0,"invalid length rejected");
        }
    }
    printf("M3_RADIX_KERNEL_PASS checks=%llu binary_compares=%llu\n",checks,m3_radix_compares);
    return 0;
}
