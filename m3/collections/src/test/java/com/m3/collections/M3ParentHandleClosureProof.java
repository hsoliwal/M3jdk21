// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;
import java.util.Random;

/** Independent original-owner proof: retained native children must retain the same physical ancestor. */
public final class M3ParentHandleClosureProof {
    private static long checks;
    private static long signature = 0xcbf29ce484222325L;
    private M3ParentHandleClosureProof() { }
    private static void yes(boolean value, String why) {
        checks++;
        if (!value) throw new AssertionError(why + " check=" + checks);
    }
    private static void observation(long value) {
        signature ^= value;
        signature *= 0x100000001b3L;
    }
    private static M3TreeWindowPlan seeded(int[] parents,int[] children,int[] physical,long[] nativeHandles) {
        M3TreeWindowPlan p=new M3TreeWindowPlan();
        p.plan(parents.length,parents,children,physical);
        p.commit(parents.length,parents,children,physical,nativeHandles);
        return p;
    }
    private static void detachedOldParent() {
        int[] parents={-1,7},children={0,0},physical={-1,0};
        M3TreeWindowPlan p=seeded(parents,children,physical,new long[]{101,102});
        int[] moved={1,0};
        p.plan(2,parents,moved,physical);
        yes(p.plannedHandle(0)==0,"new root must be inserted");
        yes(p.plannedHandle(1)==0,"child of retired root cannot keep destroyed native handle");
        yes(p.retiredCount()==1 && p.retired(0)==101,"retire topmost old root only");
        yes(p.handleOf(-1,0)==101 && p.handleOf(7,0)==102,"planning must not mutate committed handles");
        p.commit(2,parents,moved,physical,new long[]{201,202});
        yes(p.handleOf(-1,1)==201 && p.handleOf(7,0)==202,"new parent and child have fresh native identities");
        yes(p.entryOf(102)==-1,"destroyed child cannot remain indexed");
    }
    private static void sameParentRetained() {
        int[] parents={-1,7,7}, children={0,0,1}, physical={-1,0,0};
        M3TreeWindowPlan p=seeded(parents,children,physical,new long[]{101,102,103});
        p.plan(3,parents,children,physical);
        for(int i=0;i<3;i++) yes(p.plannedHandle(i)==101+i,"same hierarchy must retain native handle");
        yes(p.retiredCount()==0,"nothing retired on identical plan");
        p.shiftSiblings(-1,0,1);
        int[] newChildren={1,0,1};
        p.plan(3,parents,newChildren,physical);
        for(int i=0;i<3;i++)yes(p.plannedHandle(i)==101+i,"shifted logical root kept same native parent/child chain");
        yes(p.retiredCount()==0,"logical index shift does not retire native handles");
    }
    private static void changedHierarchyWithTwoRetainedRoots() {
        int[] oldParents={-1,7,-1}, oldIndices={0,0,1}, oldPhy={-1,0,-1};
        M3TreeWindowPlan p=seeded(oldParents,oldIndices,oldPhy,new long[]{101,102,201});
        // Child's stable logical key is unchanged but its *physical parent* changes.
        int[] newParents={-1,-1,7}, newIndices={0,1,0}, newPhy={-1,-1,1};
        p.plan(3,newParents,newIndices,newPhy);
        yes(p.plannedHandle(0)==101 && p.plannedHandle(1)==201,"both roots retained");
        yes(p.plannedHandle(2)==0,"native child cannot move to another native parent using old handle");
        yes(p.retiredCount()==1 && p.retired(0)==102,"orphan child retired on old kept parent");
        p.commit(3,newParents,newIndices,newPhy,new long[]{101,201,202});
        yes(p.handleOf(7,0)==202,"changed-parent child recreated");
    }
    private static void deepChains() {
        for(int depth:new int[]{2,3,5,31,128,256,1024,4096}) {
            int[] parents=new int[depth],indices=new int[depth],physical=new int[depth];
            long[] handles=new long[depth];
            for(int i=0;i<depth;i++) {
                parents[i]=i==0?-1:100+i-1;
                indices[i]=0;
                physical[i]=i-1;
                handles[i]=i+1L;
            }
            M3TreeWindowPlan p=seeded(parents,indices,physical,handles);
            indices[0]=1;
            p.plan(depth,parents,indices,physical);
            for(int i=0;i<depth;i++) {
                yes(p.plannedHandle(i)==0,"depth "+depth+" descendant "+i+" of lost root must not be retained");
                observation(p.plannedHandle(i));
            }
            yes(p.retiredCount()==1 && p.retired(0)==1,"retire one root, not every native descendant");
        }
    }
    private static void randomized() {
        Random r=new Random(0x4d3350524f4f464cL);
        for(int frame=0;frame<3_000;frame++) {
            int n=1+r.nextInt(128), breakAt=r.nextInt(n);
            int[] parents=new int[n], indices=new int[n], physical=new int[n];
            long[] handles=new long[n];
            parents[0]=-1; physical[0]=-1; indices[0]=0;
            for(int i=0;i<n;i++) {
                if(i>0){parents[i]=100+i-1;indices[i]=0;physical[i]=i-1;}
                handles[i]=i+1;
            }
            M3TreeWindowPlan p=seeded(parents,indices,physical,handles);
            if((frame&1)==0) {
                // Lose native root; this deletes every native child.
                indices[0]=1+frame%100;
                p.plan(n,parents,indices,physical);
                for(int i=0;i<n;i++) {
                    yes(p.plannedHandle(i)==0,"orphan grandchild reuse at frame "+frame);
                    observation(p.plannedHandle(i));
                }
                yes(p.retiredCount()==1 && p.retired(0)==1,"topmost retire only");
            } else {
                // Keep root + the complete native ancestry: every row survives.
                p.plan(n,parents,indices,physical);
                for(int i=0;i<n;i++) {
                    yes(p.plannedHandle(i)==handles[i],"same ancestry must reuse at frame "+frame);
                    observation(p.plannedHandle(i));
                }
                yes(p.retiredCount()==0,"no old native rows retired");
            }
            yes(p.size()==n,"plan must not mutate committed native count");
            yes(p.handle(0)==1,"plan must not publish a committed handle");
            observation(breakAt);
        }
    }
    public static void main(String[] args) {
        detachedOldParent();
        sameParentRetained();
        changedHierarchyWithTwoRetainedRoots();
        deepChains();
        randomized();
        System.out.println("M3_TREE_PARENT_CLOSURE_PASS checks="+checks+" digest="+Long.toUnsignedString(signature,16));
    }
}
