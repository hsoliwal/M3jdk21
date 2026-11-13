// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.text;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.regex.Pattern;

/** Executable differential contract against the separately frozen preimage. */
public final class CsvRangeContract {
    private static long checks;
    private CsvRangeContract() { }

    public static void main(String[] args) throws Exception {
        String mode = args.length == 1 ? args[0] : "java";
        boolean expectedNative = "native".equals(mode);
        require(NativeLineOps.available() == !"java".equals(mode), "native library mode mismatch");
        require(NativeLineOps.structuralAvailable() == expectedNative, "structural capability mismatch");
        fixedContracts();
        generatedValid();
        generatedMalformedAndBudgets();
        compositeAndBoundaryCases();
        System.out.println("CSV_RANGE_CONTRACT_PASS checks=" + checks
                + " native=" + NativeLineOps.available() + " structural=" + NativeLineOps.structuralAvailable());
    }

    static void fixedContracts() {
        String[] inputs = {"", "\n", "\r", "\r\n", ",", ",,", "a,", "\"\"",
                "\"\"\"\"", "\"a,b\",c\r\n", "a\r\nb\nc\rd", "a,b\"c",
                "\"a\"x", "\"a", "a,\"b", "\"a\"\"", "\"a\"\"\"", "a\u0000b,c",
                "\uD83D\uDE00,\"x\"\"y\"\n", "\uD800,\uDC00", "\"a\r\nb\",c"};
        for (String input : inputs) {
            compare(input, ',', 4096, 50, 50, 4096);
            compare(input, '\t', 4096, 50, 50, 4096);
            compare(input, '\u0000', 4096, 50, 50, 4096);
            compare(input, '\u00e9', 4096, 50, 50, 4096);
        }
        for (char delimiter : new char[] {'"', '\r', '\n', '\uD800', '\uDFFF'})
            compare("\"a", delimiter, 1, 1, 1, 1);
        require(failure(() -> CsvTools.parse(null, ',', CsvTools.Limits.defaults()))
                .equals("java.lang.NullPointerException:input"), "null input");
        require(failure(() -> CsvTools.parse("", ',', null))
                .equals("java.lang.NullPointerException:limits"), "null limits");
        CsvTools.Table table = CsvTools.parse("a,\"x\"\"y\",c\n2,z,3\n", ',', CsvTools.Limits.defaults());
        require(CsvTools.parse("\r\n", ',', CsvTools.Limits.defaults()).rows().size() == 1,
                "CRLF is one terminator, not two empty rows");
        require(CsvTools.parse("\r\"\"\n", ',', CsvTools.Limits.defaults()).rows().size() == 2,
                "quoted empty row disambiguates consecutive CR then LF records");
        CsvTools.Table projection = CsvTools.select(table, 1, 0, 1);
        require(projection.rows().get(0).cells().get(0) == table.rows().get(0).cells().get(1),
                "projection must preserve Cell identity");
        require(projection.rows().get(0).cells().get(0) == projection.rows().get(0).cells().get(2),
                "repeated projection shares the original Cell");
        require(CsvTools.whereMatches(table, 1, Pattern.compile("^z$")).rows().get(0)
                == table.rows().get(1), "filter must preserve Row identity");
        CsvTools.Cell cell = table.rows().get(0).cells().get(1);
        require(cell.contentEquals("x\"y"), "decoded field");
        require(!cell.contentEquals(null), "null comparison");
        require(cell.subSequence(1, 3).subSequence(0, 1).toString().equals("\""),
                "nested subsequence decoded coordinates");
        require(failure(() -> table.rows().clear()).startsWith("java.lang.UnsupportedOperationException"),
                "immutable rows");
        require(failure(() -> table.rows().get(0).cells().clear())
                .startsWith("java.lang.UnsupportedOperationException"), "immutable cells");
        List<CsvTools.Cell> mutable = new ArrayList<>(table.rows().get(0).cells());
        CsvTools.Row row = new CsvTools.Row(mutable);
        mutable.clear();
        require(row.cells().size() == 3, "Row defensively copies caller list");
        compareOperations("a,\"x\"\"y\",c\n2,z,3\n", ',');
    }

    static void generatedValid() {
        Random random = new Random(2026110401L);
        char[] delimiters = {',', '\t', ';', '|', '\u0000', '\u00e9'};
        String[] atoms = {"a", "Z", "0", " ", "\r", "\n", "\"", ",", "\t",
                "\u0000", "\uD83D\uDE00", "\uD800", "\uDC00", "\u00e9"};
        for (int trial = 0; trial < 4000; trial++) {
            char delimiter = delimiters[random.nextInt(delimiters.length)];
            int rows = 1 + random.nextInt(6);
            List<List<String>> expected = new ArrayList<>();
            StringBuilder input = new StringBuilder();
            for (int r = 0; r < rows; r++) {
                int columns = 1 + random.nextInt(7);
                List<String> fields = new ArrayList<>();
                for (int c = 0; c < columns; c++) {
                    StringBuilder field = new StringBuilder();
                    for (int i = random.nextInt(14); i > 0; i--)
                        field.append(atoms[random.nextInt(atoms.length)]);
                    fields.add(field.toString());
                    if (c != 0) input.append(delimiter);
                    boolean forceQuote = random.nextBoolean();
                    encode(input, field.toString(), delimiter, forceQuote || field.isEmpty());
                }
                expected.add(fields);
                input.append(switch (random.nextInt(3)) { case 0 -> "\n"; case 1 -> "\r"; default -> "\r\n"; });
            }
            String source = input.toString();
            compare(source, delimiter, 10000, 100, 100, 10000);
            CsvTools.Table actual = CsvTools.parse(source, delimiter, CsvTools.Limits.defaults());
            checks++;
            if (!cells(actual).equals(expected)) throw new AssertionError(
                    "independent encoded-field model trial " + trial + " input=" + quoted(source)
                    + " expected=" + quoted(expected.toString()) + " actual=" + quoted(cells(actual).toString()));
            require(cells(CsvTools.parse(CsvTools.serialize(actual, delimiter, "\r\n", 20000),
                    delimiter, CsvTools.Limits.defaults())).equals(expected), "roundtrip model " + trial);
        }
    }

    static void generatedMalformedAndBudgets() {
        Random random = new Random(2026110402L);
        char[] alphabet = {'a', 'b', ',', '\t', ';', '"', '\r', '\n', '\u0000', '\uD83D', '\uDE00'};
        char[] delimiters = {',', '\t', ';', '\u0000'};
        for (int trial = 0; trial < 12000; trial++) {
            StringBuilder input = new StringBuilder();
            for (int i = random.nextInt(100); i > 0; i--)
                input.append(alphabet[random.nextInt(alphabet.length)]);
            int length = 1 + random.nextInt(110);
            int rows = 1 + random.nextInt(8);
            int columns = 1 + random.nextInt(8);
            int field = 1 + random.nextInt(30);
            compare(input.toString(), delimiters[random.nextInt(delimiters.length)],
                    length, rows, columns, field);
        }
        String[] sources = {"a,b\n1,2\n", "\"a\"\"b\",c", "a,\"unterminated", "\"a\"junk", "a,b\"c"};
        for (String input : sources) for (int n = 1; n <= input.length() + 1; n++) {
            compare(input, ',', n, 1, 1, 1);
            compare(input, ',', 100, n, n, n);
        }
    }

    static void compositeAndBoundaryCases() {
        for (int seam = 0; seam < 4 * 256; seam++) {
            String prefix = "x".repeat(seam);
            String input = prefix + ",\"a\"\"b\r\nc\",\uD83D\uDE00\r\n";
            compare(input, ',', 10000, 100, 100, 10000);
            compare("\"" + prefix + "\"\"b\",z\n", ',', 10000, 100, 100, 10000);
            compare("\"" + prefix + "\"junk", ',', 10000, 100, 100, 10000);
            compare(prefix + "\"", ',', 10000, 100, 100, 10000);
        }
        String[] composites = {"\uD83D\uDE00,\"a\"\"b\",\"c\r\nd\"\n",
                "a,\"unterminated", "a,b\"c", "\"a\"junk", "", ",,", "a,\n"};
        for (String source : composites) {
            CountingSequence before = new CountingSequence(source);
            CountingSequence after = new CountingSequence(source);
            String expected = observationBefore(before, ',', 1000, 20, 20, 1000);
            String actual = observationAfter(after, ',', 1000, 20, 20, 1000);
            require(expected.equals(actual), "generic CharSequence value parity");
            require(before.reads == after.reads, "generic CharSequence callback sequence count");
            require(before.trace.toString().equals(after.trace.toString()),
                    "generic CharSequence callback index sequence");
        }
    }

    private static void compare(String source, char delimiter, int input, int rows, int columns, int field) {
        String before = observationBefore(source, delimiter, input, rows, columns, field);
        String after = observationAfter(source, delimiter, input, rows, columns, field);
        checks++;
        if (!before.equals(after)) throw new AssertionError("baseline mismatch input=" + quoted(source)
                + " delimiter=" + (int) delimiter + " limits=" + input + "," + rows + ","
                + columns + "," + field + " before=" + quoted(before) + " after=" + quoted(after));
    }

    private static String observationBefore(CharSequence source, char delimiter,
                                            int input, int rows, int columns, int field) {
        try {
            CsvToolsBefore.Table table = CsvToolsBefore.parse(source, delimiter,
                    new CsvToolsBefore.Limits(input, rows, columns, field));
            StringBuilder out = new StringBuilder("OK:");
            for (CsvToolsBefore.Row row : table.rows()) {
                out.append('[');
                for (CsvToolsBefore.Cell cell : row.cells()) {
                    out.append(cell.length()).append(':').append(cell).append(';');
                    for (int i = 0; i < cell.length(); i++) out.append((int) cell.charAt(i)).append(',');
                    out.append(':').append(cell.subSequence(0, cell.length()));
                }
                out.append(']');
            }
            out.append('|').append(CsvToolsBefore.serialize(table, delimiter, "\n", 100000));
            out.append('|').append(CsvToolsBefore.serialize(table, delimiter, "\r\n", 100000));
            return out.toString();
        } catch (RuntimeException failure) { return failure.getClass().getName() + ':' + failure.getMessage(); }
    }

    private static String observationAfter(CharSequence source, char delimiter,
                                           int input, int rows, int columns, int field) {
        try {
            CsvTools.Table table = CsvTools.parse(source, delimiter,
                    new CsvTools.Limits(input, rows, columns, field));
            StringBuilder out = new StringBuilder("OK:");
            for (CsvTools.Row row : table.rows()) {
                out.append('[');
                for (CsvTools.Cell cell : row.cells()) {
                    out.append(cell.length()).append(':').append(cell).append(';');
                    for (int i = 0; i < cell.length(); i++) out.append((int) cell.charAt(i)).append(',');
                    out.append(':').append(cell.subSequence(0, cell.length()));
                }
                out.append(']');
            }
            out.append('|').append(CsvTools.serialize(table, delimiter, "\n", 100000));
            out.append('|').append(CsvTools.serialize(table, delimiter, "\r\n", 100000));
            return out.toString();
        } catch (RuntimeException failure) { return failure.getClass().getName() + ':' + failure.getMessage(); }
    }

    private static void compareOperations(String source, char delimiter) {
        CsvToolsBefore.Table before = CsvToolsBefore.parse(source, delimiter, CsvToolsBefore.Limits.defaults());
        CsvTools.Table after = CsvTools.parse(source, delimiter, CsvTools.Limits.defaults());
        for (int[] selection : new int[][] {{}, {0}, {2, 1, 1, 0}, {-1}, {3}, {Integer.MAX_VALUE}}) {
            String expected = valueOrFailure(() -> CsvToolsBefore.serialize(
                    CsvToolsBefore.select(before, selection), delimiter, "\n", 1000));
            String actual = valueOrFailure(() -> CsvTools.serialize(
                    CsvTools.select(after, selection), delimiter, "\n", 1000));
            require(expected.equals(actual), "projection parity");
        }
        for (int column = -1; column < 4; column++) {
            int c = column;
            for (String regex : new String[] {"", "z", "x.*y", "[^a]", "^2$"}) {
                Pattern pattern = Pattern.compile(regex);
                String expected = valueOrFailure(() -> CsvToolsBefore.serialize(
                        CsvToolsBefore.whereMatches(before, c, pattern), delimiter, "\n", 1000));
                String actual = valueOrFailure(() -> CsvTools.serialize(
                        CsvTools.whereMatches(after, c, pattern), delimiter, "\n", 1000));
                require(expected.equals(actual), "regex parity");
            }
        }
        for (int budget = -1; budget < 45; budget++) {
            int b = budget;
            require(valueOrFailure(() -> CsvToolsBefore.serialize(before, delimiter, "\n", b))
                    .equals(valueOrFailure(() -> CsvTools.serialize(after, delimiter, "\n", b))),
                    "serialization budget parity");
        }
        CsvToolsBefore.Cell a = before.rows().get(0).cells().get(1);
        CsvTools.Cell b = after.rows().get(0).cells().get(1);
        for (int i = -1; i <= a.length(); i++) {
            int at = i;
            require(valueOrFailure(() -> Integer.toString(a.charAt(at)))
                    .equals(valueOrFailure(() -> Integer.toString(b.charAt(at)))), "charAt range parity");
            for (int j = -1; j <= a.length() + 1; j++) {
                int end = j;
                require(valueOrFailure(() -> a.subSequence(at, end).toString())
                        .equals(valueOrFailure(() -> b.subSequence(at, end).toString())),
                        "subsequence range parity");
            }
        }
    }

    private static List<List<String>> cells(CsvTools.Table table) {
        List<List<String>> result = new ArrayList<>();
        for (CsvTools.Row row : table.rows()) {
            List<String> values = new ArrayList<>();
            for (CsvTools.Cell cell : row.cells()) values.add(cell.toString());
            result.add(values);
        }
        return result;
    }

    private static void encode(StringBuilder out, String field, char delimiter, boolean forceQuote) {
        boolean quote = forceQuote || field.indexOf(delimiter) >= 0 || field.indexOf('"') >= 0
                || field.indexOf('\r') >= 0 || field.indexOf('\n') >= 0;
        if (quote) out.append('"');
        for (int i = 0; i < field.length(); i++) {
            char unit = field.charAt(i);
            out.append(unit);
            if (quote && unit == '"') out.append('"');
        }
        if (quote) out.append('"');
    }

    private static String failure(Runnable action) {
        try { action.run(); return "NO_FAILURE"; }
        catch (RuntimeException expected) { return expected.getClass().getName() + ':' + expected.getMessage(); }
    }

    private static String valueOrFailure(java.util.function.Supplier<String> action) {
        try { return "OK:" + action.get(); }
        catch (RuntimeException expected) { return expected.getClass().getName() + ':' + expected.getMessage(); }
    }

    private static String quoted(String source) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < source.length(); i++) {
            char unit = source.charAt(i);
            if (unit >= 32 && unit < 127) result.append(unit);
            else result.append(String.format("\\u%04x", (int) unit));
        }
        return result.toString();
    }

    private static void require(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static final class CountingSequence implements CharSequence {
        private final String text;
        private long reads;
        private final StringBuilder trace = new StringBuilder();
        private CountingSequence(String text) { this.text = Objects.requireNonNull(text); }
        @Override public int length() { return text.length(); }
        @Override public char charAt(int index) {
            reads++;
            trace.append(index).append(',');
            return text.charAt(index);
        }
        @Override public CharSequence subSequence(int from, int to) {
            throw new AssertionError("source payload may not be sliced");
        }
        @Override public String toString() { throw new AssertionError("source payload may not be flattened"); }
    }
}
