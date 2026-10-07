// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite.atom;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.IntBinaryOperator;

/** Deterministic generated programs and an independently expressed arithmetic oracle. */
final class SpectrumCases {
    static final int[] EDGES = {Integer.MIN_VALUE, Integer.MIN_VALUE + 1, -65536, -33,
            -32, -1, 0, 1, 2, 31, 32, 33, 255, 65536, Integer.MAX_VALUE};
    static final int EXPRESSIONS = 324;
    private static final String[] OPERATORS = {"+", "-", "*", "<<", ">>", ">>>", "&", "|", "^"};
    private static final IntBinaryOperator[] MODEL = {(a, b) -> a + b, (a, b) -> a - b,
            (a, b) -> a * b, (a, b) -> a << b, (a, b) -> a >> b, (a, b) -> a >>> b,
            (a, b) -> a & b, (a, b) -> a | b, (a, b) -> a ^ b};

    private SpectrumCases() { }

    static Map<String, String> project() {
        Map<String, String> result = new TreeMap<>();
        for (int variant = 0; variant < 4; variant++) {
            String name = "Shape" + variant;
            StringBuilder methods = new StringBuilder();
            List<String> calls = new ArrayList<>();
            for (int outer = 0; outer < OPERATORS.length; outer++) {
                for (int inner = 0; inner < OPERATORS.length; inner++) {
                    int index = outer * OPERATORS.length + inner;
                    methods.append("  private static int f").append(index)
                            .append("(int a, int b) { return ")
                            .append(expression(outer, inner, variant)).append("; }\n");
                    calls.add("f" + index + "(a, b)");
                }
            }
            result.put("spectrum/" + name + ".java", "package spectrum;\n"
                    + "public final class " + name + " {\n"
                    + "  private " + name + "() {}\n"
                    + "  public static int[] values(int a, int b) { return new int[]{"
                    + String.join(",", calls) + "}; }\n" + methods + "}\n");
        }
        result.put("spectrum/Effects.java", controls());
        return Map.copyOf(result);
    }

    static int[] expected(int variant, int a, int b) {
        int[] values = new int[81];
        for (int outer = 0; outer < MODEL.length; outer++) {
            for (int inner = 0; inner < MODEL.length; inner++) {
                int left = MODEL[inner].applyAsInt(a, b);
                int right = a ^ b;
                if (variant == 1) {
                    left = MODEL[inner].applyAsInt(-a, +b);
                    right = ~a ^ -b;
                }
                int value = MODEL[outer].applyAsInt(left, right);
                values[outer * MODEL.length + inner] = variant == 3
                        ? value + Integer.MAX_VALUE : value;
            }
        }
        return values;
    }

    private static String expression(int outer, int inner, int variant) {
        String left = "(a " + OPERATORS[inner] + " b)";
        String right = "(a ^ b)";
        if (variant == 1) {
            left = "((-a) " + OPERATORS[inner] + " (+b))";
            right = "((~a) ^ (-b))";
        }
        String expression = left + " " + OPERATORS[outer] + " " + right;
        return switch (variant) {
            case 0, 1 -> expression;
            case 2 -> "((" + expression + "))";
            case 3 -> "(" + expression + ") + 0x7fff_ffff";
            default -> throw new IllegalArgumentException("variant");
        };
    }

    static String controls() {
        return """
                package spectrum;
                public final class Effects {
                    private static int state;
                    private static final StringBuilder events = new StringBuilder();
                    private static final String LEXICAL = "return a+b; /* M3-IOP: PURE_INT_EXPRESSION */";
                    private static final String CODE =""" + "\"\"\"\n"
                + "private static int fake(int a,int b) { return a+b; }\n"
                + "class Patternizer { native int call(); } // M3-ATOM: m3$pureIntAtom\n"
                + "\"\"\";\n" + """
                    private Effects() {}
                    private static int divide(int a, int b) { return a / b; }
                    private static int modulo(int a, int b) { return a % b; }
                    private static int mutation(int a, int b) { return a++ + b; }
                    private static int field(int a, int b) { return state + a + b; }
                    private static int scalarLookalike(int a, int ignored[]) { return a + 1; }
                    private static int varargs(int a, int... ignored) { return a + 1; }
                    private static int unbox(Integer a, int b) { return a + b; }
                    private static int reserved(int m3$pureIntAtom, int b) { return m3$pureIntAtom + b; }
                    private static int cast(long a, int b) { return (int) a + b; }
                    private static int array(int[] a, int b) { return a[b]; }
                    private static int choose(int a, int b) { return a > b ? a : b; }
                    private static int callback(int a, int b) {
                        java.util.function.IntSupplier l = () -> { events.append('L'); return state++ + a; };
                        java.util.function.IntSupplier r = () -> { events.append('R'); return state + b; };
                        return l.getAsInt() + r.getAsInt();
                    }
                    private static int nested(int a, int b) {
                        int sum = 0;
                        for (int i = 0; i < 4; i++) {
                            if (a > i) for (int j = 0; j < 3; j++) sum += b;
                            else sum ^= switch (i) { case 0, 2 -> a; default -> b; };
                        }
                        return sum;
                    }
                    public static String trace(int a, int b) {
                        state = 7; events.setLength(0);
                        int v = callback(a, b) ^ mutation(a, b) ^ field(a, b) ^ nested(a, b)
                                ^ reserved(a, b) ^ cast(a, b) ^ choose(a, b)
                                ^ scalarLookalike(a, null) ^ varargs(a);
                        try { v ^= divide(a, b) ^ modulo(a, b); }
                        catch (ArithmeticException expected) { events.append('A'); }
                        try { v ^= unbox(null, b); }
                        catch (NullPointerException expected) { events.append('N'); }
                        try { v ^= array(new int[]{a}, b); }
                        catch (ArrayIndexOutOfBoundsException expected) { events.append('B'); }
                        return v + ":" + state + ":" + events + LEXICAL + CODE;
                    }
                }
                """;
    }
}
