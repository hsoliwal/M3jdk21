/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.collections;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

/** Actual Java21/C17 JNI differential over reference-valued generic M3 range maps. */
public final class M3ObjectRangeMapNativeProof {
    private static long checks;
    private M3ObjectRangeMapNativeProof() { }
    private static void ok(boolean condition, String why) {
        checks++;
        if (!condition) throw new AssertionError(why + " check=" + checks);
    }
    public static void main(String[] args) {
        if (args.length != 1 || !"--native".equals(args[0])) {
            throw new IllegalArgumentException("usage: --native (real JNI required)");
        }
        if (!IndexPathMasks.nativeAvailable()) {
            throw new AssertionError("real M3IndexPathMasks JNI required; no fallback");
        }
        Random random = new Random(0x4d334a4e4952414eL);
        for (int trial = 0; trial < 600; trial++) {
            var map = new ObjectRangeMap<Integer, Integer>(Comparator.naturalOrder());
            int width = random.nextInt(84);
            for (int i = 0; i < width; i++) {
                int begin = i * 4;
                int mask = random.nextInt() & ~ObjectRangeMap.LIVE;
                map.put(begin, begin+2, (i & 3) == 0 ? null : i, mask);
            }
            var root = map.spans();
            var view = trial % 3 == 0 ? root.reversed()
                    : trial % 3 == 1 ? root.slice(root.size()/3, root.size()) : root;
            int required = trial % 2 == 0 ? Integer.MIN_VALUE : ObjectRangeMap.VISIBLE;
            int forbidden = trial % 3 == 0 ? ObjectRangeMap.DIRTY : 0;
            var javaSelection = view.selected(required, forbidden, IndexPathMasks.Backend.JAVA);
            var nativeSelection = view.selected(required, forbidden,
                    IndexPathMasks.Backend.NATIVE_REQUIRED);
            ok(Arrays.equals(javaSelection.physicalOrdinals(), nativeSelection.physicalOrdinals()),
                    "native 32-bit index parity, slices and reverse");
            for (int i = 0; i < javaSelection.size(); i++) {
                ok(javaSelection.valueAt(i) == nativeSelection.valueAt(i),
                        "original Java object reference retained, JNI returns indices only");
                int bits = javaSelection.stateAt(i);
                ok((bits & required) == required && (bits & forbidden) == 0,
                        "Java/C17 state selection truth");
            }
        }
        System.out.println("PASS M3ObjectRangeMapNativeProof checks=" + checks + " JNI=true");
    }
}
