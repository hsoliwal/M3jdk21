/*
 * Copyright (c) 2026, Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: GPL-2.0-only
 */
/*
 * @test
 * @summary Stable mask compaction across fragmented words, offsets and poisoned tail bits
 * @modules java.base/jdk.internal.mindex
 * @run main/othervm M3MaskWordSurvivorsTest
 * @run main/othervm -Xint M3MaskWordSurvivorsTest
 */
import java.util.Arrays;
import java.util.Random;
import jdk.internal.mindex.M3CollectionMaskCompactor;

public final class M3MaskWordSurvivorsTest {
    private static long checks;
    private static final Object SHARED = new Object();

    public static void main(String[] args) {
        for (int n : new int[] {0,1,63,64,65,127,128,129,511,512,513,575,576,577,1023,1024,1025,4097}) {
            for (int offset : new int[] {0,1,7,65}) {
                for (long word : new long[] {0,-1L,1,Long.MIN_VALUE,0x5555555555555555L,
                        0xaaaaaaaaaaaaaaaaL,0x8000000000000001L,0x7ffffffffffffffeL}) {
                    long[] mask = new long[(n+63)>>>6];
                    Arrays.fill(mask, word);
                    verify(n, offset, mask, false);
                    verify(n, offset, mask, true);
                }
            }
        }
        // Every 16-bit pattern at scalar and word-dispatch sizes, with poisoned tails.
        for (int bits = 0; bits < 65536; bits++) {
            long word = Integer.toUnsignedLong(bits);
            long[] mask = {word, -1L, 0, word << 48, 0xaaaaaaaaaaaaaaaaL,
                    word << 17, 0, -1L, word};
            verify(513 + (bits & 31), bits & 7, mask, false);
            long[] extended = new long[17];
            for (int i = 0; i < extended.length; i++) extended[i] = mask[i % mask.length];
            verify(1025 + (bits & 31), bits & 7, extended, false);
        }
        Random random = new Random(0x4d33574f52444cL);
        for (int trial = 0; trial < 1200; trial++) {
            int n = 512 + random.nextInt(6000);
            long[] mask = new long[(n+63)>>>6];
            for (int i=0;i<mask.length;i++) mask[i]=random.nextLong();
            verify(n,trial & 63,mask,(trial & 1)==0);
        }
        System.out.println("M3MaskWordSurvivorsTest PASS checks="+checks);
    }

    private static void verify(int length, int offset, long[] mask, boolean typed) {
        int capacity=offset+length+9;
        Object[] values=typed?new String[capacity]:new Object[capacity];
        for(int i=0;i<capacity;i++) values[i]=i%11==0?null:typed?"v"+(i%7):i%7==0?SHARED:new Object();
        Object[] before=values.clone();
        long[] originalMask=mask.clone();
        int end=M3CollectionMaskCompactor.compact(values,offset,offset+length,mask);
        int write=offset;
        for(int i=0;i<length;i++) if((mask[i>>>6]&(1L<<(i&63)))==0) {
            check(values[write++]==before[offset+i],"stable identity");
        }
        check(write==end,"returned end");
        for(int i=0;i<offset;i++) check(values[i]==before[i],"prefix");
        for(int i=end;i<capacity;i++) check(values[i]==before[i],"untouched tail");
        check(Arrays.equals(mask,originalMask),"mask immutable");
    }

    private static void check(boolean condition,String label) {
        checks++;
        if(!condition) throw new AssertionError(label+" check="+checks);
    }
}
