/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.function.IntBinaryOperator;

/** Dependency-free deterministic array/offset/mask/merge differential tests. */
public final class M3ArrayifiedIndexOpsSelfTest {
    private static long assertions;
    private M3ArrayifiedIndexOpsSelfTest() { }
    private static void eq(boolean condition, String what) {
        assertions++; if (!condition) throw new AssertionError(what);
    }
    private static void eq(int[] actual, int[] expected, String what) {
        eq(Arrays.equals(actual, expected), what+" got="+Arrays.toString(actual)
                +" expected="+Arrays.toString(expected));
    }
    private static void expect(Class<? extends Throwable> kind, Runnable action) {
        assertions++;
        try { action.run(); } catch (Throwable t) {
            if (kind.isInstance(t)) return;
            throw new AssertionError("wrong exception",t);
        }
        throw new AssertionError("missing " + kind);
    }
    private static void trial(Random random) {
        int length = random.nextInt(170), first = random.nextInt(length + 1);
        int last = first + random.nextInt(length - first + 1);
        int count = last - first;
        int[] rows = new int[length];
        byte[] states = new byte[length + 5];
        for(int i=0;i<rows.length;i++) rows[i]=random.nextInt(13)-6;
        random.nextBytes(states);
        int[] pristine = rows.clone(); byte[] pristineStates=states.clone();
        int[] ordinals = new int[count==0?0:random.nextInt(count*2+1)];
        for(int i=0;i<ordinals.length;i++) ordinals[i]=random.nextInt(count);
        int[] expectedGather = new int[ordinals.length];
        for(int i=0;i<ordinals.length;i++) expectedGather[i]=rows[first+ordinals[i]];
        eq(M3ArrayifiedIndexOps.gather(rows,first,last,ordinals),expectedGather,"gather");
        int rotate = random.nextBoolean()?Integer.MIN_VALUE:random.nextInt();
        int[] expectedRotate = new int[count];
        for(int i=0;i<count;i++) expectedRotate[(i+Math.floorMod(rotate,Math.max(1,count)))%Math.max(1,count)]=rows[first+i];
        eq(M3ArrayifiedIndexOps.rotated(rows,first,last,rotate),expectedRotate,"rotate");
        int stateFrom=random.nextInt(6),required=random.nextInt(256),excluded=random.nextInt(256);
        List<Integer> reference = new ArrayList<>();
        for(int i=0;i<count;i++){
            int f=states[stateFrom+i]&255;
            if((f&required)==required && (f&excluded)==0) reference.add(rows[first+i]);
        }
        int[] expected=reference.stream().mapToInt(Integer::intValue).toArray();
        eq(M3ArrayifiedIndexOps.selected(rows,first,last,states,stateFrom,required,excluded,
                M3IndexMaskCompactor.Backend.JAVA),expected,"mask");
        if (M3IndexMaskCompactor.nativeAvailable()) {
            eq(M3ArrayifiedIndexOps.selected(rows,first,last,states,stateFrom,
                    required,excluded,M3IndexMaskCompactor.Backend.NATIVE_REQUIRED),expected,"native parity");
        } else {
            expect(UnsatisfiedLinkError.class,()->M3ArrayifiedIndexOps.selected(rows,first,last,
                    states,stateFrom,required,excluded,M3IndexMaskCompactor.Backend.NATIVE_REQUIRED));
        }
        eq(rows,pristine,"no index writes");
        eq(Arrays.equals(states,pristineStates),"no flag writes");
        int a = random.nextInt(140), b=random.nextInt(140);
        int[] l=new int[a+4], r=new int[b+4];
        for(int i=0;i<a;i++)l[i+2]=random.nextInt(13)-6;
        for(int i=0;i<b;i++)r[i+2]=random.nextInt(13)-6;
        Arrays.sort(l,2,a+2);Arrays.sort(r,2,b+2);
        int[] savedL=l.clone(),savedR=r.clone();
        IntBinaryOperator order=Integer::compare;
        int[] merged=M3ArrayifiedIndexOps.mergeSorted(l,2,a+2,r,2,b+2,order);
        int[] oracle = new int[a+b]; System.arraycopy(l,2,oracle,0,a);System.arraycopy(r,2,oracle,a,b);Arrays.sort(oracle);
        eq(merged,oracle,"merged values");eq(l,savedL,"left isolated");eq(r,savedR,"right isolated");
        // Ties must choose the left occurrence, not only the left value.
        int[] ll={10,20,30},rr={40,50};
        eq(M3ArrayifiedIndexOps.mergeSorted(ll,0,3,rr,0,2,(x,y)->0),
                new int[]{10,20,30,40,50},"stable ties");
    }
    public static void main(String[] args) {
        Random random = new Random(0x5c74da5619L);
        for(int trial=0;trial<3100;trial++) trial(random);
        expect(IndexOutOfBoundsException.class,()->M3ArrayifiedIndexOps.gather(new int[1],0,1,new int[]{1}));
        expect(IndexOutOfBoundsException.class,()->M3ArrayifiedIndexOps.rotated(new int[2],0,3,1));
        expect(IndexOutOfBoundsException.class,()->M3ArrayifiedIndexOps.selected(new int[2],0,2,new byte[1],0,0,0,M3IndexMaskCompactor.Backend.JAVA));
        expect(IllegalArgumentException.class,()->M3ArrayifiedIndexOps.selected(new int[0],0,0,new byte[0],0,256,0,M3IndexMaskCompactor.Backend.JAVA));
        expect(IllegalArgumentException.class,()->M3ArrayifiedIndexOps.mergeSorted(new int[]{2,1},0,2,new int[0],0,0,Integer::compare));
        eq(M3ArrayifiedIndexOps.gather(new int[]{3,2},0,2,new int[]{0,0,1}),new int[]{3,3,2},"repeat physical row");
        eq(M3ArrayifiedIndexOps.rotated(new int[]{3,2},0,2,0),new int[]{3,2},"zero rotation");
        System.out.println("PASS M3ArrayifiedIndexOpsSelfTest assertions="+assertions);
    }
}
