// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * WindowRuntime.Selection with a kept represented length against the previous class
 * (SelectionReference): after every random operation the same selectedCount, toArray, ranges,
 * complement mode and exceptions, and the kept length equals the sum of the ranges.
 */
//FLLDCIM: LEETCODIFY — differential proof for the kept represented length
final class SelectionCountTest {

    interface Op {
        void run(Object selection) throws Exception;
    }

    private static String outcome(Op op, Object selection) {
        try {
            op.run(selection);
            return "ok";
        } catch (Exception e) {
            return "threw " + e.getClass().getName() + ": " + e.getMessage();
        }
    }

    private static String state(WindowRuntime.Selection s) {
        StringBuilder out = new StringBuilder(s.selectedCount() + " " + Arrays.toString(s.toArray())
                + " " + s.complementMode() + " " + s.logicalCount());
        for (int r = 0; r < s.rangeCount(); r++) {
            out.append(" [").append(s.rangeStart(r)).append(',').append(s.rangeEndExclusive(r));
        }
        return out.toString();
    }

    private static String state(SelectionReference s) {
        StringBuilder out = new StringBuilder(s.selectedCount() + " " + Arrays.toString(s.toArray())
                + " " + s.complementMode() + " " + s.logicalCount());
        for (int r = 0; r < s.rangeCount(); r++) {
            out.append(" [").append(s.rangeStart(r)).append(',').append(s.rangeEndExclusive(r));
        }
        return out.toString();
    }

    private static long kept(WindowRuntime.Selection s) throws ReflectiveOperationException {
        Field f = WindowRuntime.Selection.class.getDeclaredField("represented");
        f.setAccessible(true);
        return f.getLong(s);
    }

    private static long sum(WindowRuntime.Selection s) {
        long sum = 0;
        for (int r = 0; r < s.rangeCount(); r++) sum += s.rangeEndExclusive(r) - s.rangeStart(r);
        return sum;
    }

    @Test
    void sameCountsAndRanges() throws ReflectiveOperationException {
        for (int seed = 0; seed < 2_000; seed++) {
            Random random = new Random(seed);
            WindowRuntime.Selection lanes = new WindowRuntime.Selection();
            SelectionReference ref = new SelectionReference();
            for (int step = 0; step < 300; step++) {
                int n = Math.max(1, ref.logicalCount());
                int a = random.nextInt(n + 2) - 1;
                int b = a + random.nextInt(8);
                int what = random.nextInt(11);
                int size = random.nextInt(40);
                String at = "seed " + seed + " step " + step + " op " + what + " " + a + "," + b;
                assertEquals(outcome(s -> apply(s, what, a, b, size), ref),
                        outcome(s -> apply(s, what, a, b, size), lanes), at);
                assertEquals(state(ref), state(lanes), at + " state");
                assertEquals(sum(lanes), kept(lanes), at + " kept length");
            }
        }
    }

    /** One operation on either class, chosen by {@code what}; reflection keeps one code path. */
    private static void apply(Object s, int what, int a, int b, int size) throws Exception {
        switch (what) {
            case 0 -> call(s, "setLogicalCount", new Class<?>[] {int.class}, size);
            case 1 -> call(s, "setSelected", new Class<?>[] {int.class, boolean.class}, a,
                    b % 2 == 0);
            case 2 -> call(s, "selectRange", new Class<?>[] {int.class, int.class}, a, b);
            case 3 -> call(s, "deselectRange", new Class<?>[] {int.class, int.class}, a, b);
            case 4 -> call(s, "insert", new Class<?>[] {int.class, int.class}, a, b - a);
            case 5 -> call(s, "remove", new Class<?>[] {int.class, int.class}, a, b - a);
            case 6 -> call(s, "invert", new Class<?>[] {});
            case 7 -> call(s, "selectAll", new Class<?>[] {});
            case 8 -> call(s, "clear", new Class<?>[] {});
            default -> call(s, "setLogicalCount", new Class<?>[] {int.class}, a + 1);
        }
    }

    private static void call(Object s, String name, Class<?>[] types, Object... args)
            throws Exception {
        try {
            s.getClass().getMethod(name, types).invoke(s, args);
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() instanceof Exception x) throw x;
            throw e;
        }
    }
}
