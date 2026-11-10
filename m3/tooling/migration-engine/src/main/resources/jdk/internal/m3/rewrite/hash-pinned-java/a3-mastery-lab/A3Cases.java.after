// SPDX-License-Identifier: Apache-2.0
package com.m3.a3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable generated A3 hostile-source corpus and cheap precompute signals. */
final class A3Cases {
    enum CallShape {
        DIRECT,
        MODERN
    }

    record Signal(
            String sha256,
            int utf16Length,
            int codeDecoys,
            int regexDecoys,
            int textBlocks,
            int lambdaShapes,
            int switchShapes,
            int recordShapes,
            String lineEnding) {
        Signal {
            if (sha256 == null || !sha256.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("sha256");
            }
            if (utf16Length < 0
                    || codeDecoys < 0
                    || regexDecoys < 0
                    || textBlocks < 0
                    || lambdaShapes < 0
                    || switchShapes < 0
                    || recordShapes < 0) {
                throw new IllegalArgumentException("negative A3 signal");
            }
            lineEnding = Objects.requireNonNull(lineEnding, "lineEnding");
        }
    }

    private A3Cases() {}

    static List<String> fixtures() {
        ArrayList<String> fixtures = new ArrayList<>();
        List<List<String>> orders =
                List.of(
                        List.of("DATA", "COMPUTE", "API"),
                        List.of("DATA", "API", "COMPUTE"),
                        List.of("COMPUTE", "DATA", "API"),
                        List.of("COMPUTE", "API", "DATA"),
                        List.of("API", "DATA", "COMPUTE"),
                        List.of("API", "COMPUTE", "DATA"));
        for (boolean hostile : List.of(false, true)) {
            for (String eol : List.of("\n", "\r\n")) {
                for (CallShape shape : CallShape.values()) {
                    for (List<String> order : orders) {
                        fixtures.add(fixture(order, hostile, eol, shape));
                    }
                }
            }
        }
        return List.copyOf(fixtures);
    }

    static Signal signal(String source) {
        return new Signal(
                A3Fs.sha(source),
                source.length(),
                count(source, "return (a+b)*31;")
                        + count(source, "if (x) { return y; }"),
                count(source, "Pattern.compile"),
                count(source, "\"\"\""),
                count(source, "IntBinaryOperator"),
                count(source, "switch ("),
                count(source, "record Pair"),
                source.contains("\r\n") ? "CRLF" : "LF");
    }

    static void requireStableData(Signal before, Signal after) {
        if (before.codeDecoys() != after.codeDecoys()
                || before.regexDecoys() != after.regexDecoys()
                || before.textBlocks() != after.textBlocks()
                || before.lambdaShapes() != after.lambdaShapes()
                || before.switchShapes() != after.switchShapes()
                || before.recordShapes() != after.recordShapes()
                || !before.lineEnding().equals(after.lineEnding())) {
            throw new IllegalStateException("A3Lab lexical signal drift");
        }
    }

    private static String fixture(
            List<String> order,
            boolean hostile,
            String eol,
            CallShape shape) {
        Map<String, String> blocks =
                Map.of(
                        "DATA",
                        dataBlock(hostile),
                        "COMPUTE",
                        """
                            private static int compute(int a, int b) {
                                return (a + b) * 31;
                            }
                        """,
                        "API",
                        apiBlock(shape));
        StringBuilder out = new StringBuilder();
        out.append("package m3.lab;\n\npublic final class Subject {\n");
        for (String key : order) {
            out.append(blocks.get(key));
        }
        out.append("    private Subject() {}\n}\n");
        return out.toString().replace("\n", eol);
    }

    private static String apiBlock(CallShape shape) {
        String probe =
                switch (shape) {
                    case DIRECT ->
                            """
                                public static int probe(int a, int b) {
                                    return compute(a, b);
                                }
                            """;
                    case MODERN ->
                            """
                                public static int probe(int a, int b) {
                                    record Pair(int left, int right) {}
                                    Pair pair = new Pair(a, b);
                                    java.util.function.IntBinaryOperator op =
                                            (left, right) ->
                                                    switch (Math.floorMod(left, 2)) {
                                                        case 0 -> compute(left, right);
                                                        default -> compute(left, right);
                                                    };
                                    return op.applyAsInt(pair.left(), pair.right());
                                }
                            """;
                };
        return probe
                + """
                    public static boolean matches(String value) {
                        return java.util.regex.Pattern.compile(REGEX).matcher(value).find();
                    }

                    public static String payload() {
                        class Local {
                            String value() {
                                return CODE + "|" + TEXT + "|" + REGEX;
                            }
                        }
                        java.util.function.Supplier<String> supplier = new Local()::value;
                        return supplier.get();
                    }

                    public static native int nativeShape(int value);
                """;
    }

    private static String dataBlock(boolean hostile) {
        if (!hostile) {
            return """
                    private static final String CODE = "return (a+b)*31;";
                    private static final String TEXT = "if (x) { return y; }";
                    private static final String REGEX = "a+b?";
                    """;
        }
        return String.join(
                        "\n",
                        "    // Fake Java: private static int compute(int a, int b) { return (a+b)*31; }",
                        "    private static final String CODE =",
                        "            \"private static int compute(int a,int b){ return (a+b)*31; }\";",
                        "    private static final String TEXT = \"\"\"",
                        "            if (x) { return y; }",
                        "            // return (a+b)*31;",
                        "            Pattern.compile(\"(return|if)\\\\\\\\s*\\\\\\\\(\");",
                        "            \"\"\";",
                        "    private static final String REGEX =",
                        "            \"(?:return\\\\s*\\\\([^)]*\\\\)|if\\\\s*\\\\([^)]*\\\\)\\\\s*\\\\{)\";",
                        "")
                + "\n";
    }

    private static int count(String source, String token) {
        int count = 0;
        for (int at = 0; (at = source.indexOf(token, at)) >= 0; at += token.length()) {
            count++;
        }
        return count;
    }
}
