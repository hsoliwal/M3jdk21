// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.atom;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Exhaustive small Cartesian syntax domain, with an independently written Java arithmetic oracle. */
final class IntCases {
    static final int SIZE = 144;
    static final String OWNER = "lab.Subject";
    static final String PATH = "lab/Subject.java";
    static final String NOISE_PATH = "lab/Noise.java";
    private static final String[] BINARY = {"+", "-", "*", "<<", ">>", ">>>", "&", "|", "^"};
    private static final String[] UNARY = {"", "+", "-", "~"};
    private static final String NOISE = """
            package lab;
            import java.util.function.IntSupplier;
            public final class Noise {
                private Noise() {}
                private static int state;
                private static String trace;
                public static final String TEXT = "return a + b; M3-IOP: PURE_INT_EXPRESSION";
                // private static int fake(int a, int b) { return a + b; }
                private static int sum(IntSupplier left, IntSupplier right) {
                    return left.getAsInt() + right.getAsInt();
                }
                public static String witness(int a, int b) {
                    trace = "";
                    try {
                        int value = sum(() -> { trace += "L"; return 10 / a; },
                                        () -> { trace += "R"; return 10 / b; });
                        return trace + ":" + value;
                    } catch (ArithmeticException failure) {
                        return trace + ":" + failure.getClass().getName();
                    }
                }
                public static int divide(int a, int b) { return a / b; }
                protected static int exposed(int a, int b) { return a + b; }
                private int instance(int a, int b) { return a + b; }
                private static long wide(long a, long b) { return a + b; }
                private static int field(int a) { return state + a; }
                private static int increment(int a) { return a++ + 1; }
                private static int remainder(int a, int b) { return a % b; }
                private static int conditional(int a, int b) { return a > 0 ? a : b; }
                private static int callback(int a) { return Math.abs(a); }
                private static int indexed(int[] values) { return values[0] + 1; }
                private static int collision(int m3$pureIntAtom) { return m3$pureIntAtom + 1; }
            }
            """;

    private IntCases() {}

    static Map<String, String> project(int start, int end) {
        if (start < 0 || end > SIZE || start >= end) throw new IllegalArgumentException("range");
        StringBuilder source = new StringBuilder("package lab;\npublic final class Subject {\n"
                + "private Subject() {}\npublic static int eval(int id, int a, int b) {\n"
                + "return switch (id) {\n");
        for (int id = start; id < end; id++) {
            source.append("case ").append(id).append(" -> calc").append(id).append("(a,b);\n");
        }
        source.append("default -> throw new IllegalArgumentException(\"id\"); }; }\n");
        for (int id = start; id < end; id++) {
            int shape = id / 2;
            String left = id % 2 == 0 ? "a" : "left";
            String right = id % 2 == 0 ? "b" : "right";
            String expression = UNARY[(shape / 2) % 4] + left + " "
                    + BINARY[shape / 8] + " " + right;
            if (shape % 2 != 0) expression = "((" + expression + "))";
            source.append("private static int calc").append(id).append("(int ").append(left)
                    .append(", int ").append(right).append(") { return ")
                    .append(expression).append("; }\n");
        }
        source.append("}\n");
        Map<String, String> result = new LinkedHashMap<>();
        result.put(PATH, source.toString());
        result.put(NOISE_PATH, NOISE);
        return Map.copyOf(result);
    }

    static int expected(int id, int left, int right) {
        int value = switch (((id / 2) / 2) % 4) {
            case 0, 1 -> left;
            case 2 -> -left;
            case 3 -> ~left;
            default -> throw new AssertionError("unary");
        };
        return switch (id / 16) {
            case 0 -> value + right;
            case 1 -> value - right;
            case 2 -> value * right;
            case 3 -> value << right;
            case 4 -> value >> right;
            case 5 -> value >>> right;
            case 6 -> value & right;
            case 7 -> value | right;
            case 8 -> value ^ right;
            default -> throw new IllegalArgumentException("id");
        };
    }

    static List<int[]> inputs() {
        int[] edges = {Integer.MIN_VALUE, Integer.MIN_VALUE + 1, -65, -33, -32, -1,
                0, 1, 2, 31, 32, 33, 63, 64, Integer.MAX_VALUE - 1, Integer.MAX_VALUE};
        List<int[]> pairs = new ArrayList<>();
        for (int left : edges) for (int right : edges) pairs.add(new int[] {left, right});
        var random = new java.util.SplittableRandom(0x4d334c4142L);
        for (int i = 0; i < 32; i++) pairs.add(new int[] {random.nextInt(), random.nextInt()});
        return List.copyOf(pairs);
    }
}
