// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
import jdk.m3.tools.cpu.M3ByteSuffixIndex;
import jdk.m3.tools.cpu.M3ByteSuffixIndexNative;
import java.util.Arrays;
import java.util.Random;

/** Independent JNI/Java suffix-result pair comparison, including all empty boundaries. */
public final class M3ByteSuffixIndexNativeOracle {
    private static long checks;
    private static void check(boolean truth, String detail) {
        checks++;
        if (!truth) throw new AssertionError(detail + " @" + checks);
    }

    private static void compare(byte[] text, byte[][] patterns) {
        M3ByteSuffixIndex index = M3ByteSuffixIndex.copyOf(text);
        int total = 0;
        for (byte[] pattern : patterns) total += pattern.length;
        byte[] packed = new byte[total];
        int[] offsets = new int[patterns.length + 1];
        int next = 0;
        for (int i = 0; i < patterns.length; i++) {
            System.arraycopy(patterns[i], 0, packed, next, patterns[i].length);
            next += patterns[i].length;
            offsets[i + 1] = next;
        }
        byte[] copy = packed.clone();
        int[] originalOffsets = offsets.clone();
        int[] javaResult = index.queryBatch(packed, offsets);
        int[] nativeResult = M3ByteSuffixIndexNative.queryBatch(index, packed, offsets);
        check(Arrays.equals(nativeResult, javaResult), "JNI exact pairs");
        check(javaResult.length == patterns.length * 2, "pair cardinality");
        check(Arrays.equals(copy, packed) && Arrays.equals(originalOffsets, offsets),
                "packed input ownership");
        for (int i = 0; i < patterns.length; i++) {
            check(nativeResult[i * 2] == index.count(patterns[i]), "count vs scalar");
            check(nativeResult[i * 2 + 1] == index.first(patterns[i]), "first vs scalar");
        }
        if (nativeResult.length != 0) nativeResult[0] ^= 0x757;
        check(Arrays.equals(javaResult, M3ByteSuffixIndexNative.queryBatch(index,packed,offsets)),
                "detached JNI output");
        check(Arrays.equals(text, index.bytes()), "text immutable");
    }

    public static void main(String[] args) {
        check(M3ByteSuffixIndexNative.isAvailable(), "native must be explicitly enabled");
        compare(new byte[0], new byte[][]{});
        compare(new byte[0], new byte[][]{new byte[0],new byte[]{0},new byte[]{(byte)0xff}});
        compare(new byte[]{0, (byte)0xff, 0}, new byte[][]{new byte[0],
                new byte[]{0}, new byte[]{(byte)0xff}, new byte[]{0,(byte)0xff,0}});
        byte[] repeated = new byte[131072];
        Arrays.fill(repeated, (byte)0xff);
        compare(repeated, new byte[][]{new byte[0],new byte[]{(byte)0xff},
                new byte[]{(byte)0xff,(byte)0xff},new byte[]{0},new byte[4096]});
        Random random = new Random(0x2912ab45L);
        for (int trial = 0; trial < 1600; trial++) {
            byte[] text = new byte[random.nextInt(500)];
            random.nextBytes(text);
            if (trial % 3 == 0) Arrays.fill(text, (byte)random.nextInt(8));
            byte[][] queries = new byte[random.nextInt(35)][];
            for (int i = 0; i < queries.length; i++) {
                int length = random.nextInt(17);
                queries[i] = new byte[length];
                random.nextBytes(queries[i]);
                if (length != 0 && trial % 5 == 0) {
                    Arrays.fill(queries[i], text.length == 0 ? (byte)0 : text[0]);
                }
            }
            compare(text, queries);
        }
        System.out.println("M3_BYTE_SUFFIX_JNI_PASS checks=" + checks);
    }
}
