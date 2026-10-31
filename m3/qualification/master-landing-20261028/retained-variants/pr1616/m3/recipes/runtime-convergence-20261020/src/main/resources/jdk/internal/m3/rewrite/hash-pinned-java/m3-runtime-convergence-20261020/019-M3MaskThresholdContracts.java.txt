/*
 * Copyright (c) 2026, Hitesh Soliwal and Contributors to the Synexia Project.
 * Licensed under GPL version 2, with the Classpath exception.
 *
 * @test
 * @summary Threshold-selection maintenance preserves exact stable Object[] compaction
 * @modules java.base/jdk.internal.mindex
 * @run main/othervm --add-exports=java.base/jdk.internal.mindex=ALL-UNNAMED M3MaskThresholdContracts
 */
import java.util.Arrays;
import java.util.Random;
import jdk.internal.mindex.M3CollectionMaskCompactor;

public final class M3MaskThresholdContracts {
    private static final Object[] ITEMS = new Object[131100];
    private static long cases;
    static { for (int i = 0; i < ITEMS.length; i++) ITEMS[i] = i % 19 == 0 ? null : new Object(); }

    private static void verify(boolean[] removed, int from, boolean noisyTail) {
        int length = removed.length, to = from + length;
        Object[] source = Arrays.copyOf(ITEMS, to + 7);
        Object[] expected = source.clone(), actual = source.clone();
        int end = from;
        long[] mask = new long[((length + 63) >>> 6) + 2];
        for (int i = 0; i < length; i++) {
            if (removed[i]) mask[i >>> 6] |= 1L << (i & 63);
            else expected[end++] = source[from + i];
        }
        if (noisyTail) {
            if ((length & 63) != 0) mask[length >>> 6] |= -1L << (length & 63);
            mask[mask.length - 1] = -1L;
        }
        long[] before = mask.clone();
        int result = M3CollectionMaskCompactor.compact(actual, from, to, mask);
        if (result != end) throw new AssertionError("survivor end " + cases);
        for (int i = 0; i < actual.length; i++) {
            if (actual[i] != expected[i]) throw new AssertionError("identity/window/discarded tail " + cases + ":" + i);
        }
        if (!Arrays.equals(mask, before)) throw new AssertionError("mask mutated");
        cases++;
    }

    private static void refuses(Class<? extends Throwable> type, String message, Runnable work) {
        try { work.run(); throw new AssertionError("missing " + type); }
        catch (Throwable failure) {
            if (failure.getClass() != type) throw new AssertionError("wrong exception", failure);
            if (message != null && !message.equals(failure.getMessage())) throw new AssertionError("wrong message", failure);
        }
    }

    public static void main(String[] args) {
        for (int length = 0; length <= 16; length++) {
            for (int bits = 0; bits < (1 << length); bits++) {
                boolean[] removed = new boolean[length];
                for (int i = 0; i < length; i++) removed[i] = (bits & (1 << i)) != 0;
                verify(removed, (bits & 1) == 0 ? 0 : 5, true);
            }
        }
        // Exhaust all sixteen-bit patterns on the >=512 strategy-selection path.
        for (int bits = 0; bits < 65536; bits++) {
            boolean[] removed = new boolean[513];
            for (int i = 0; i < 16; i++) removed[60 + i] = (bits & (1 << i)) != 0;
            removed[512] = (bits & 1) != 0;
            verify(removed, 3, true);
        }
        Random random = new Random(20261020);
        int[] lengths = {511,512,513,575,576,639,640,1023,1024,1025,4095,4096,4097,65535,65536,65537,131071};
        for (int length : lengths) {
            int limit = Math.max(4, length >>> 7);
            for (int runs = Math.max(0, limit - 1); runs <= limit + 1; runs++) {
                boolean[] removed = new boolean[length];
                for (int i = 0; i < runs; i++) removed[(i * 2 + 63) % length] = true;
                verify(removed, 7, true);
            }
            for (int pattern = 0; pattern < 8; pattern++) {
                boolean[] removed = new boolean[length];
                for (int i = 0; i < length; i++) removed[i] = switch (pattern) {
                    case 0 -> false;
                    case 1 -> true;
                    case 2 -> (i & 1) == 0;
                    case 3 -> i >= 63 && i < 129;
                    case 4 -> i % 128 == 0;
                    case 5 -> random.nextBoolean();
                    case 6 -> random.nextInt(64) == 0;
                    default -> random.nextInt(64) != 0;
                };
                verify(removed, pattern, true);
            }
        }
        for (int trial = 0; trial < 2500; trial++) {
            int length = trial % 9 == 0 ? 32768 + random.nextInt(32769) : 512 + random.nextInt(4096);
            boolean[] removed = new boolean[length];
            int chance = 1 + random.nextInt(99);
            for (int i = 0; i < length; i++) removed[i] = random.nextInt(100) < chance;
            verify(removed, random.nextInt(8), true);
        }
        refuses(NullPointerException.class,"values", () -> M3CollectionMaskCompactor.compact(null,-1,2,null));
        refuses(NullPointerException.class,"deleted", () -> M3CollectionMaskCompactor.compact(new Object[1],-1,2,null));
        refuses(IndexOutOfBoundsException.class,null, () -> M3CollectionMaskCompactor.compact(new Object[1],-1,2,new long[0]));
        refuses(IllegalArgumentException.class,"mask too short", () -> M3CollectionMaskCompactor.compact(new Object[513],0,513,new long[8]));
        if (M3CollectionMaskCompactor.compact(new Object[1],1,1,new long[0]) != 1) throw new AssertionError("empty window");
        System.out.println("M3_MASK_THRESHOLD_CONTRACT_PASS cases=" + cases);
    }
}
