// Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
package com.m3.collections;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

final class M3TreeRebasedAdmissionTest {
    private static M3TreeWindowPlan committed() {
        var p = new M3TreeWindowPlan();
        int[] parent={-1,-1,-1}, index={10,11,12}, physical={-1,-1,-1};
        p.plan(3,parent,index,physical);
        p.commit(3,parent,index,physical,new long[]{101,102,103});
        return p;
    }
    private static void unchanged(M3TreeWindowPlan p) {
        assertEquals(3,p.size());
        assertEquals(101,p.handleOf(-1,10));
        assertEquals(102,p.handleOf(-1,11));
        assertEquals(103,p.handleOf(-1,12));
        assertEquals(0,p.entryOf(101));
        assertEquals(1,p.entryOf(102));
        assertEquals(2,p.entryOf(103));
    }
    @Test void commitWithLateZeroHandleRefusesWithoutPublishing() {
        var p=committed();int[] parent={-1,-1,-1},index={20,21,22},physical={-1,-1,-1};
        p.plan(3,parent,index,physical);
        assertThrows(IllegalArgumentException.class,()->p.commit(3,parent,index,physical,new long[]{201,202,0}));
        unchanged(p);
    }
    @Test void shortCommitStructuralLanesRefuseBeforeIndexClearing() {
        var p=committed();int[] parent={-1,-1,-1},index={20,21,22},physical={-1,-1,-1};
        p.plan(3,parent,index,physical);
        assertThrows(IllegalArgumentException.class,()->p.commit(3,new int[]{-1},index,physical,new long[]{201,202,203}));
        unchanged(p);
    }
    @Test void duplicateHandleCommitRefusesWithOriginalBijection() {
        var p=committed();int[] parent={-1,-1,-1},index={20,21,22},physical={-1,-1,-1};
        p.plan(3,parent,index,physical);
        assertThrows(IllegalArgumentException.class,()->p.commit(3,parent,index,physical,new long[]{201,202,201}));
        unchanged(p);
    }
    @Test void duplicateCoordinateDirectCommitRefusesWithoutPublication() {
        var p=committed();int[] parents={-1,-1,-1},indices={20,20,22},physical={-1,-1,-1};
        // A direct commit overload has independent arrays; plan input is still unique.
        p.plan(3,new int[]{-1,-1,-1},new int[]{20,21,22},physical);
        assertThrows(IllegalArgumentException.class,()->p.commit(3,parents,indices,physical,new long[]{201,202,203}));
        unchanged(p);
    }
    @Test void duplicateCoordinatePlanRefusesBeforeMutation() {
        var p=committed();int[] parents={-1,7,-1},indices={10,0,12},physical={-1,0,-1};
        p.plan(3,parents,indices,physical);
        assertEquals(0,p.previousSibling(2));
        assertThrows(IllegalArgumentException.class,()->p.plan(3,new int[]{-1,-1,-1},new int[]{10,10,12},new int[]{-1,-1,-1}));
        assertEquals(0,p.previousSibling(2));
        unchanged(p);
    }
    @Test void invalidLateParentRefusesBeforeSiblingMutation() {
        var p=committed();int[] parents={-1,7,-1},indices={10,0,12},physical={-1,0,-1};
        p.plan(3,parents,indices,physical);
        assertEquals(-1,p.previousSibling(1));
        assertEquals(0,p.previousSibling(2));
        assertThrows(IllegalArgumentException.class,()->p.plan(3,parents,indices,new int[]{-1,-1,2}));
        assertEquals(-1,p.previousSibling(1));
        assertEquals(0,p.previousSibling(2));
        unchanged(p);
    }
    @Test void committedHandleMappingSupportsAllNonzeroLongValues() {
        var p=committed();int[] a={-1,7,8},b={0,0,0},c={-1,0,0};
        p.plan(3,a,b,c);
        p.commit(3,a,b,c,new long[]{Long.MIN_VALUE,Long.MAX_VALUE,-1L});
        assertEquals(0,p.entryOf(Long.MIN_VALUE));
        assertEquals(1,p.entryOf(Long.MAX_VALUE));
        assertEquals(2,p.entryOf(-1L));
        assertEquals(Long.MIN_VALUE,p.handleOf(-1,0));
    }
    @Test void unchangedCurrentShiftSiblingsOverflowAndCollisionGuards() {
        var p=committed();
        assertThrows(IllegalArgumentException.class,()->p.shiftSiblings(-1,11,Integer.MAX_VALUE));
        unchanged(p);
        assertThrows(IllegalArgumentException.class,()->p.shiftSiblings(-1,11,-1));
        unchanged(p);
        p.shiftSiblings(-1,11,1);
        assertEquals(102,p.handleOf(-1,12));
        assertEquals(103,p.handleOf(-1,13));
        assertEquals(101,p.handleOf(-1,10));
    }
    @Test void repeatedPlannerReusesLastChildPrimitiveScratch() throws ReflectiveOperationException {
        var p=committed();
        Field f=M3TreeWindowPlan.class.getDeclaredField("lastChild");f.setAccessible(true);
        int[] first={-1,-1,-1},index={20,21,22},physical={-1,-1,-1};
        p.plan(3,first,index,physical);
        Object identity=f.get(p);
        for(int i=0;i<500;i++) {
            p.plan(i%3,first,index,physical);
            assertSame(identity,f.get(p));
        }
    }
    @Test void randomRollingWindowsKeepResidentHandles() {
        var p=new M3TreeWindowPlan();int[] parents=new int[128],indices=new int[128],phys=new int[128];
        Arrays.fill(parents,-1);Arrays.fill(phys,-1);
        long next=1;
        for(int frame=0;frame<2500;frame++) {
            int count=frame%37;
            for(int i=0;i<count;i++)indices[i]=frame%67+i;
            p.plan(count,parents,indices,phys);
            long[] handles=new long[count];
            for(int i=0;i<count;i++) {
                long old=p.plannedHandle(i);
                handles[i]=old!=0?old:next++;
            }
            p.commit(count,parents,indices,phys,handles);
            for(int i=0;i<count;i++) {
                assertEquals(handles[i],p.handleOf(-1,indices[i]));
                assertEquals(i,p.entryOf(handles[i]));
            }
        }
    }
}
