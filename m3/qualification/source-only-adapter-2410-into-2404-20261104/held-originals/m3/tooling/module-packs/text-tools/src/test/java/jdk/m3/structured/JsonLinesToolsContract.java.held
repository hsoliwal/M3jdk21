/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.structured;

import jdk.m3.text.Limits;
import jdk.m3.text.TextFrame;
import jdk.m3.text.TextLine;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/** Independent frozen-source differential and generated-lexeme contract checks. */
public final class JsonLinesToolsContract {
    private static final Limits STANDARD = Limits.standard();
    private static final AtomicLong DIFFERENTIAL = new AtomicLong();
    private static final AtomicLong EXPECTED = new AtomicLong();
    private static final long SEED = 0x4d335f4a534f4eL;

    private JsonLinesToolsContract() { }

    private record Outcome(TextFrame value, String type, String message) { }
    private record Selection(String pointer, String encoded) { }
    private record Sample(String encoded, List<Selection> selections) { }
    private record Request(TextFrame frame, String pointer, Limits limits) { }

    public static void main(String[] args) throws Exception {
        runAll();
        System.out.println("JSON_TAPE_CONTRACT PASS differential=" + DIFFERENTIAL.get()
                + " independent_expected=" + EXPECTED.get() + " seed=" + SEED);
    }

    public static void runAll() throws Exception {
        exactLexemesAndPointers();
        grammarAndFailureOrder();
        boundaryAndBudgetContracts();
        collidingObjectKeysAndArrayIndexes();
        generatedLexemesAndMutations();
        seededMalformedDocuments();
        parallelAndRepeatedCalls();
        publicApiUnchanged();
    }

    public static void exactLexemesAndPointers() {
        expect("", "", STANDARD, "");
        for (String scalar : List.of("0", "-0", "12", "-83", "1.2300e+04", "0E-0",
                "true", "false", "null", "\"\"", "\"a\\n\\t\\/b\"", "\"😀\"",
                "\"\\uD83D\\uDE00\"", "\"\\u0041\\u0061\"")) {
            expect(" \t" + scalar + " \t", "", STANDARD, scalar + "\n");
        }
        String complex = "{\"\": [false, {\"a/b\":{\"~x\": 1.2300e+04}}],"
                + "\"a~1\":true,\"n\":null,\"0\":\"zero\"}";
        expect(complex, "/", STANDARD, "[false, {\"a/b\":{\"~x\": 1.2300e+04}}]\n");
        expect(complex, "//1/a~1b/~0x", STANDARD, "1.2300e+04\n");
        expect(complex, "/a~01", STANDARD, "true\n");
        expect(complex, "/n", STANDARD, "null\n");
        expect(complex, "/0", STANDARD, "\"zero\"\n");
        expect("{\"é\":\"Ω\",\"\\uD83D\\uDE00\":\"😀\"}", "/😀", STANDARD, "\"😀\"\n");
        expect("{\"\\u0000\":0,\"\\b\\f\\n\\r\\t\":1}", "/\0", STANDARD, "0\n");
        expect("{\"\\u0000\":0,\"\\b\\f\\n\\r\\t\":1}", "/\b\f\n\r\t", STANDARD, "1\n");
        expect("{\"x\":1}\r\n{\"x\":2}\r{\"x\":3}\n", "/x", STANDARD, "1\n2\n3\n");
        TextFrame explicit = new TextFrame(List.of(new TextLine("{\n\"x\": [1,\r2]\n}", "xy")));
        expect(explicit, "/x", STANDARD, "[1,\r2]\n");
        expect("{\"x\":{\"x\":0},\"y\":{\"x\":1}}", "/x/x", STANDARD, "0\n");
        for (String pointer : List.of("/", "/0", "/00", "/-1", "/+0", "/١", "/1.0",
                "/9999999999", "/10000000000", "/2147483648")) {
            parity(TextFrame.fromString("[0,[1,2],{\"\":3}]"), pointer, STANDARD);
        }
        expect("[0,[1,2],{\"\":3}]", "/1/1", STANDARD, "2\n");
        expect("[0,[1,2],{\"\":3}]", "/2/", STANDARD, "3\n");
    }

    public static void grammarAndFailureOrder() {
        List<String> invalid = List.of(
                " ", "\t", "{", "[", "{]", "[}", "[,]", "[1,]", "{\"x\":1,}",
                "{0:0}", "{\"x\" 0}", "{\"x\":}", "{\"x\":0 \"y\":1}",
                "{\"x\":0]", "[0 1]", "[true false]", "true false", "null0", "falsex",
                "tru", "False", "NULL", "+1", "-", "01", "-01", "1.", ".5", "1.e0",
                "1e", "1E+", "1e-", "NaN", "Infinity", "--1", "\f0", "\u00a00",
                "\"", "\"x", "\"x\\", "\"\\x\"", "\"\\u\"", "\"\\u00\"", "\"\\u00zz\"",
                "\"\\uD800\"", "\"\\uDC00\"", "\"\\uD800\\u0041\"", "\"\\uD800x\"",
                "\"\\uD800\\x\"", "\"a\0b\"", "\"a\tb\"", "0\n\n", "\n0",
                "{\"a\":0,\"a\":1}", "{\"a\":0,\"\\u0061\":1}",
                "{\"/\":0,\"\\/\":1}", "{\"😀\":0,\"\\uD83D\\uDE00\":1}",
                "{\"a\":0,\"a\":}", "{\"a\":0,\"a\":1x}",
                "{\"a\":0,\"a\":{\"b\":0,\"b\":1}}", "{\"a\":0,\"a\":1 \"b\":2}");
        for (String text : invalid) {
            for (String pointer : List.of("", "/a", "/0", "/~", "bad")) {
                parity(TextFrame.fromString(text), pointer, STANDARD);
            }
        }
        for (String pointer : List.of("x", "#", "#/~1", "~", "/~", "/~2", "/~00~", "/~~")) {
            parity(TextFrame.fromString("{}"), pointer, STANDARD);
            parity(new TextFrame(List.of()), pointer, STANDARD);
        }
        failure("[0]", "/00", "invalid JSON array index");
        failure("{}", "/x", "missing JSON pointer: /x");
        failure("0", "/0", "missing JSON pointer: /0");
        failure("1.", "", "invalid JSON fraction at offset 2");
        failure("[1,]", "", "invalid JSON number at offset 3");
        failure("{\"a\":0,\"a\":1}", "", "duplicate JSON key at offset 12");
        failure("{\"a\":0,\"a\":}", "", "invalid JSON number at offset 11");
        failure("{\"a\":0,\"a\":1x}", "", "duplicate JSON key at offset 12");
        failure("\"x", "", "unterminated JSON string");
        // All records validate, including records after a previously successful extraction.
        parity(TextFrame.fromString("{\"a\":1}\n{\"a\":2,\"a\":3}"), "/a", STANDARD);
    }

    public static void boundaryAndBudgetContracts() {
        TextFrame one = TextFrame.fromString("0");
        for (TextFrame frame : Arrays.asList(null, one, new TextFrame(List.of()))) {
            for (String pointer : Arrays.asList(null, "", "/x")) {
                parity(frame, pointer, null);
                parity(frame, pointer, STANDARD);
            }
        }
        for (int length : new int[] {4095, 4096, 4097, 5000}) {
            String pointer = "/" + "a".repeat(length - 1);
            parity(one, pointer, STANDARD);
            parity(new TextFrame(List.of()), pointer, STANDARD);
        }
        for (int depth : new int[] {0, 1, 63, 64, 65, 66}) {
            String arrays = "[".repeat(depth) + "0" + "]".repeat(depth);
            parity(TextFrame.fromString(arrays), "", STANDARD);
            parity(TextFrame.fromString(arrays), "/0".repeat(depth), STANDARD);
            String objects = "{\"a\":".repeat(depth) + "0" + "}".repeat(depth);
            parity(TextFrame.fromString(objects), "", STANDARD);
            parity(TextFrame.fromString(objects), "/a".repeat(depth), STANDARD);
        }
        for (int budget : new int[] {1, 255, 256, 257, 511, 512, 513}) {
            Limits limits = new Limits(1, 0, budget, 1_000_000L);
            int effective = Math.max(256, budget);
            for (int values : new int[] {effective - 2, effective - 1, effective, effective + 1}) {
                String array = "[" + String.join(",", Collections.nCopies(values, "0")) + "]";
                parity(TextFrame.fromString(array), "", limits);
                parity(TextFrame.fromString(array), "/" + (values - 1), limits);
            }
        }
        for (long bytes : new long[] {0, 1, 2, 3, 4, 5, 8, 9, 10, 100}) {
            Limits limits = new Limits(1, 0, 16, bytes);
            for (String text : List.of("", "0", "0\n", "\"é\"", "\"😀\"", "[1,2]", "0\n0\n")) {
                parity(TextFrame.fromString(text), "", limits);
            }
        }
        for (int lines : new int[] {0, 1, 2, 3}) {
            Limits limits = new Limits(1, 0, lines, 100);
            parity(TextFrame.fromString("0\n1\n"), "", limits);
            parity(TextFrame.fromString("0\n1\n"), "bad", limits);
        }
        expect("\"é\"", "", new Limits(1, 0, 1, 5), "\"é\"\n");
    }

    public static void collidingObjectKeysAndArrayIndexes() {
        List<String> keys = new ArrayList<>();
        for (int bits = 0; bits < 512; bits++) {
            StringBuilder key = new StringBuilder();
            for (int bit = 0; bit < 9; bit++) key.append((bits & (1 << bit)) == 0 ? "Aa" : "BB");
            keys.add(key.toString());
        }
        int hash = keys.get(0).hashCode();
        for (String key : keys) check(key.hashCode() == hash, "frozen hash-collision corpus");
        Limits limits = new Limits(1, 0, 4096, 4_000_000L);
        for (int order = 0; order < 3; order++) {
            List<String> ordered = new ArrayList<>(keys);
            if (order == 1) Collections.reverse(ordered);
            if (order == 2) Collections.shuffle(ordered, new Random(SEED));
            StringBuilder document = new StringBuilder("{");
            for (int i = 0; i < ordered.size(); i++) {
                if (i != 0) document.append(',');
                document.append(quote(ordered.get(i), i % 3)).append(':').append(i);
            }
            document.append('}');
            String json = document.toString();
            for (int i = 0; i < ordered.size(); i++) {
                expect(json, "/" + ordered.get(i), limits, i + "\n");
            }
            parity(TextFrame.fromString(json), "/" + "Aa".repeat(10), limits);
            String duplicate = json.substring(0, json.length() - 1) + ","
                    + quote(ordered.get(ordered.size() / 2), 1) + ":99}";
            parity(TextFrame.fromString(duplicate), "", limits);
        }
        StringBuilder nested = new StringBuilder("[");
        for (int i = 0; i < 150; i++) {
            if (i != 0) nested.append(',');
            nested.append("[{\"a\":[").append(i).append(",null]},[],[false]]");
        }
        nested.append(']');
        for (int i = 0; i < 150; i++) {
            expect(nested.toString(), "/" + i + "/0/a/0", limits, i + "\n");
            expect(nested.toString(), "/" + i + "/2/0", limits, "false\n");
        }
    }

    public static void generatedLexemesAndMutations() {
        Random random = new Random(SEED);
        for (int trial = 0; trial < 250; trial++) {
            Sample sample = sample(random, 4);
            String document = " \t" + sample.encoded() + " \t";
            for (Selection selection : sample.selections()) {
                expect(document, selection.pointer(), STANDARD, selection.encoded() + "\n");
            }
            parity(TextFrame.fromString(document), "/__absent__", STANDARD);
            for (int mutation = 0; mutation < 12; mutation++) {
                int at = boundary(document, random.nextInt(document.length() + 1));
                char inserted = "{}[],:\\\"x0 \t".charAt(random.nextInt(12));
                String changed;
                if (mutation % 3 == 0 && at < document.length()) {
                    int after = at + Character.charCount(document.codePointAt(at));
                    changed = document.substring(0, at) + document.substring(after);
                } else if (mutation % 3 == 1 && at < document.length()) {
                    int after = at + Character.charCount(document.codePointAt(at));
                    changed = document.substring(0, at) + inserted + document.substring(after);
                } else {
                    changed = document.substring(0, at) + inserted + document.substring(at);
                }
                parity(TextFrame.fromString(changed), "", STANDARD);
                parity(TextFrame.fromString(changed), "/a", STANDARD);
            }
        }
    }

    public static void seededMalformedDocuments() {
        Random random = new Random(SEED ^ 0x5eedL);
        String alphabet = "{}[],:\\\"0123456789eE+- \ttruefalsnulx";
        String[] pointers = {"", "/a", "/0", "/1", "/~01", "/", "/00", "/~", "bad"};
        for (int trial = 0; trial < 8_000; trial++) {
            StringBuilder document = new StringBuilder();
            for (int i = random.nextInt(65); i > 0; i--) {
                document.append(alphabet.charAt(random.nextInt(alphabet.length())));
            }
            parity(TextFrame.fromString(document.toString()), pointers[random.nextInt(pointers.length)], STANDARD);
        }
    }

    public static void parallelAndRepeatedCalls() throws Exception {
        List<Request> requests = new ArrayList<>();
        Random random = new Random(SEED ^ 0xc0ffeeL);
        for (int i = 0; i < 96; i++) {
            Sample sample = sample(random, 3);
            Selection chosen = sample.selections().get(random.nextInt(sample.selections().size()));
            requests.add(new Request(TextFrame.fromString(sample.encoded()), chosen.pointer(), STANDARD));
        }
        requests.add(new Request(TextFrame.fromString("{\"x\":1,\"\\u0078\":2}"), "/x", STANDARD));
        requests.add(new Request(TextFrame.fromString("[0,1]"), "/00", STANDARD));
        List<Outcome> expected = requests.stream()
                .map(request -> outcome(true, request.frame(), request.pointer(), request.limits())).toList();
        try (var executor = Executors.newFixedThreadPool(4)) {
            List<Callable<Void>> tasks = new ArrayList<>();
            for (int worker = 0; worker < 4; worker++) {
                tasks.add(() -> {
                    for (int repeat = 0; repeat < 4; repeat++) {
                        for (int i = 0; i < requests.size(); i++) {
                            Request request = requests.get(i);
                            Outcome observed = outcome(false, request.frame(), request.pointer(), request.limits());
                            equal(expected.get(i), observed, "parallel immutable-frame replay");
                            DIFFERENTIAL.incrementAndGet();
                        }
                    }
                    return null;
                });
            }
            for (var future : executor.invokeAll(tasks, 30, TimeUnit.SECONDS)) future.get();
        }
    }

    public static void publicApiUnchanged() {
        equal(surface(FrozenJsonLinesTools.class), surface(JsonLinesTools.class), "public/protected signatures");
    }

    private static List<String> surface(Class<?> type) {
        List<String> result = new ArrayList<>();
        for (var method : type.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers()) || Modifier.isProtected(method.getModifiers())) {
                result.add(method.getName() + ":" + method.getReturnType().getName() + ":"
                        + Arrays.toString(method.getParameterTypes()) + ":" + method.getModifiers()
                        + ":" + Arrays.toString(method.getExceptionTypes()));
            }
        }
        Collections.sort(result);
        return result;
    }

    private static Sample sample(Random random, int depth) {
        if (depth == 0 || random.nextInt(3) == 0) {
            String[] literals = {"null", "true", "false", "0", "-0", "1.2300e+04", "-7E-9", "12345678901234567890"};
            String[] values = {"", "a", "Aa", "BB", "\\\"/~", "éΩ", "😀", "x\0\b\f\n\r\ty"};
            String encoded = random.nextBoolean() ? literals[random.nextInt(literals.length)]
                    : quote(values[random.nextInt(values.length)], random.nextInt(3));
            return new Sample(encoded, List.of(new Selection("", encoded)));
        }
        boolean object = random.nextBoolean();
        int width = random.nextInt(5);
        String[] keys = {"", "a", "Aa", "BB", "/", "~", "😀", "x\0", "line\nkey", "a~1", "00"};
        int firstKey = random.nextInt(keys.length);
        StringBuilder encoded = new StringBuilder(object ? "{" : "[");
        List<Selection> nested = new ArrayList<>();
        for (int i = 0; i < width; i++) {
            if (i != 0) encoded.append(random.nextBoolean() ? ", " : ",\t");
            Sample child = sample(random, depth - 1);
            String prefix;
            if (object) {
                String key = keys[(firstKey + i) % keys.length];
                encoded.append(quote(key, random.nextInt(3))).append(random.nextBoolean() ? ": " : ":\t");
                prefix = "/" + key.replace("~", "~0").replace("/", "~1");
            } else prefix = "/" + i;
            encoded.append(child.encoded());
            for (Selection selection : child.selections()) {
                nested.add(new Selection(prefix + selection.pointer(), selection.encoded()));
            }
        }
        encoded.append(object ? "}" : "]");
        String json = encoded.toString();
        List<Selection> selections = new ArrayList<>();
        selections.add(new Selection("", json));
        selections.addAll(nested);
        return new Sample(json, List.copyOf(selections));
    }

    private static String quote(String value, int mode) {
        StringBuilder result = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char unit = value.charAt(i);
            if (mode == 1 || unit < 0x20) {
                result.append("\\u");
                for (int shift = 12; shift >= 0; shift -= 4) result.append("0123456789abcdef".charAt((unit >>> shift) & 15));
            } else if (unit == '"' || unit == '\\' || (unit == '/' && mode == 2)) {
                result.append('\\').append(unit);
            } else result.append(unit);
        }
        return result.append('"').toString();
    }

    private static int boundary(String text, int at) {
        return at > 0 && at < text.length() && Character.isLowSurrogate(text.charAt(at))
                && Character.isHighSurrogate(text.charAt(at - 1)) ? at + 1 : at;
    }

    private static Outcome outcome(boolean baseline, TextFrame input, String pointer, Limits limits) {
        try {
            TextFrame result = baseline ? FrozenJsonLinesTools.select(input, pointer, limits)
                    : JsonLinesTools.select(input, pointer, limits);
            return new Outcome(result, null, null);
        } catch (RuntimeException failure) {
            return new Outcome(null, failure.getClass().getName(), failure.getMessage());
        }
    }

    private static Outcome parity(TextFrame input, String pointer, Limits limits) {
        Outcome baseline = outcome(true, input, pointer, limits);
        Outcome candidate = outcome(false, input, pointer, limits);
        equal(baseline, candidate, "source differential pointer=" + printable(pointer)
                + " input=" + printable(input == null ? null : input.text()));
        DIFFERENTIAL.incrementAndGet();
        return candidate;
    }

    private static void expect(String input, String pointer, Limits limits, String expected) {
        expect(TextFrame.fromString(input), pointer, limits, expected);
    }
    private static void expect(TextFrame input, String pointer, Limits limits, String expected) {
        Outcome result = parity(input, pointer, limits);
        equal(new Outcome(TextFrame.fromString(expected), null, null), result,
                "independent selected lexeme pointer=" + printable(pointer));
        EXPECTED.incrementAndGet();
    }
    private static void failure(String input, String pointer, String message) {
        Outcome result = parity(TextFrame.fromString(input), pointer, STANDARD);
        equal(new Outcome(null, IllegalArgumentException.class.getName(), message), result,
                "independent failure ordering");
        EXPECTED.incrementAndGet();
    }
    private static String printable(String value) {
        if (value == null) return "<null>";
        String result = value.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r")
                .replace("\t", "\\t").replace("\0", "\\0");
        return result.length() > 1000 ? result.substring(0, 1000) + "..." : result;
    }
    private static void equal(Object expected, Object actual, String context) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(context + " expected=" + expected + " actual=" + actual);
        }
    }
    private static void check(boolean condition, String context) {
        if (!condition) throw new AssertionError(context);
    }
}
