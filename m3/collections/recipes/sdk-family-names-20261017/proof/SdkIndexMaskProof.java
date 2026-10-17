/* SPDX-License-Identifier: Apache-2.0 */
import com.m3.collections.M3IndexMaskCompactor;
import com.m3.collections.M3LongIndexProjection;
import java.util.*;

/** Reuses actual unchanged M3 Java/JNI owners; no predicates or object payloads enter C. */
public final class SdkIndexMaskProof {
    private static long checks;
    private static void check(boolean value) { checks++; if (!value) throw new AssertionError("mask check " + checks); }
    public static void main(String[] args) {
        boolean nativeRun = args.length > 0 && args[0].equals("native");
        var backend = nativeRun ? M3IndexMaskCompactor.Backend.NATIVE_REQUIRED : M3IndexMaskCompactor.Backend.JAVA;
        check(M3IndexMaskCompactor.nativeAvailable() == nativeRun);
        Random random = new Random(20261017);
        for (int sample = 0; sample < 4096; sample++) {
            long[] values = new long[random.nextInt(1026)];
            for (int i = 0; i < values.length; i++) values[i] = switch (random.nextInt(12)) {
                case 0 -> Long.MIN_VALUE; case 1 -> Long.MAX_VALUE; default -> random.nextInt(15) - 7;
            };
            var base = M3LongIndexProjection.snapshot(values).sortedAscending();
            Integer[] ids = new Integer[values.length]; Arrays.setAll(ids, i -> i);
            Arrays.sort(ids, Comparator.comparingLong(i -> values[i]));
            int from = random.nextInt(values.length + 1), length = random.nextInt(values.length - from + 1);
            var slice = base.slice(from, length);
            byte[] flags = new byte[length]; random.nextBytes(flags); int mask = sample & 255;
            var filtered = slice.filtered(flags, mask, backend);
            int kept = 0;
            for (int i = 0; i < length; i++) if (((flags[i] & 255) & mask) == mask) {
                check(kept < filtered.size());
                check(filtered.physicalIndex(kept) == ids[from + i]);
                check(filtered.get(kept++) == values[ids[from + i]]);
            }
            check(filtered.size() == kept);
            check(filtered.order() == M3LongIndexProjection.Order.ASCENDING);
        }
        if (!nativeRun) {
            try { M3LongIndexProjection.snapshot(new long[0]).filtered(new byte[0], 0,
                    M3IndexMaskCompactor.Backend.NATIVE_REQUIRED); throw new AssertionError("missing JNI accepted"); }
            catch (UnsatisfiedLinkError expected) { checks++; }
        }
        System.out.println("SdkIndexMaskProof mode=" + (nativeRun ? "native" : "java-missing-native") + " checks=" + checks + " seededCases=4096");
    }
}
