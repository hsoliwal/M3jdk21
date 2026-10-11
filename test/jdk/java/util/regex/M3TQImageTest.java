/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 *
 * @test
 * @summary M3TQ bounded immutable image/restore and composed-fact contract
 * @modules java.base/jdk.internal.mindex
 * @run main/othervm M3TQImageTest
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import jdk.internal.mindex.M3TQ;

public class M3TQImageTest {
    private static long checks;

    public static void main(String[] args) {
        M3TQ leaf = M3TQ.fromExact(List.of("abcdef"));
        M3TQ shared = M3TQ.or(List.of(leaf, leaf));
        M3TQ root = M3TQ.and(List.of(shared, M3TQ.fromExact(List.of("cde"))));

        M3TQ.Image image = root.image();
        check(image.operations().length == 3, "shared image deduplicates node identity");
        check(image.children().length == 3, "image preserves ordered edges");
        check(image.primitivePayloadBytes() > 0, "image reports primitive payload");

        M3TQ restored = M3TQ.fromImage(image);
        check(restored.op() == root.op(), "restore preserves root operation");
        check(restored.toString().equals(root.toString()), "restore preserves query rendering");
        long abc = M3TQ.trigram('a', 'b', 'c');
        long cde = M3TQ.trigram('c', 'd', 'e');
        check(restored.test(value -> value == abc || value == cde)
                == root.test(value -> value == abc || value == cde),
                "restore preserves conservative evaluation");

        byte[] operations = image.operations();
        operations[0] = 99;
        long[] trigrams = image.trigrams();
        if (trigrams.length != 0) trigrams[0] = 0;
        check(image.operations()[0] != 99, "operation export is defensive");
        check(trigrams.length == 0 || image.trigrams()[0] != 0,
                "trigram export is defensive");

        M3TQ.Facts left = M3TQ.precompute("ab", 2);
        M3TQ.Facts right = M3TQ.precompute("cde", 3);
        M3TQ.Facts joined = left.concat(right, 5);
        M3TQ.Facts direct = M3TQ.precompute("abcde", 5);
        check(joined.utf16Length() == direct.utf16Length(), "concat preserves length");
        check(Arrays.equals(joined.keys(), direct.keys()), "concat preserves exact seam facts");

        M3TQ exact = M3TQ.fromExact(List.of("abc"));
        check(exact.testPrecomputed(direct), "candidate facts admit present trigram");
        M3TQ.Facts absent = M3TQ.precompute("axc", 3);
        check(!exact.testPrecomputed(absent), "candidate facts reject absent trigram");

        M3TQ repeated = M3TQ.or(List.of(leaf, leaf));
        check(repeated.termCount() == leaf.termCount() * 2,
                "term count preserves repeated shared edges");

        M3TQ deep = leaf;
        for (int index = 0; index < 4096; index++) {
            M3TQ extra = M3TQ.fromExact(List.of("abc"));
            deep = (index & 1) == 0
                    ? M3TQ.or(List.of(deep, extra))
                    : M3TQ.and(List.of(deep, extra));
        }
        M3TQ admittedDeep = deep;
        int[] callbackCalls = {0};
        expectUnsupported(() -> admittedDeep.test(value -> {
            callbackCalls[0]++;
            return false;
        }), "deep evaluation rejects before callback allocation");
        check(callbackCalls[0] == 0, "depth rejection invokes no callback");

        ArrayList<Long> rejectedVisits = new ArrayList<>();
        check(!shared.test(value -> {
            rejectedVisits.add(value);
            return false;
        }), "shared query preserves false result");
        check(rejectedVisits.size() == 2 && rejectedVisits.get(0).equals(abc)
                && rejectedVisits.get(1).equals(abc),
                "shared query preserves duplicate callback visits");

        ArrayList<Long> acceptedVisits = new ArrayList<>();
        check(shared.test(value -> {
            acceptedVisits.add(value);
            return value == abc;
        }), "shared query preserves true result");
        check(acceptedVisits.size() == 1 && acceptedVisits.get(0).equals(abc),
                "shared query preserves short-circuit order");

        expectUnsupported(() -> root.image(new M3TQ.ImageLimits(1, 0, 0, 17)),
                "tight image limits reject before admission");
        expectUnsupported(() -> M3TQ.fromImage(image, new M3TQ.ImageLimits(1, 0, 0, 17)),
                "tight restore limits reject before admission");

        System.out.println("M3_TQ_IMAGE_PASS|checks=" + checks);
    }

    private static void expectUnsupported(Runnable action, String message) {
        try {
            action.run();
            throw new AssertionError(message);
        } catch (UnsupportedOperationException expected) {
            checks++;
        }
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
