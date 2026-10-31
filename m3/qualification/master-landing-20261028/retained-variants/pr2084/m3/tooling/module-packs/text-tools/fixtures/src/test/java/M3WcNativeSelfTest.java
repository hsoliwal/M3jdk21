// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
import jdk.m3.text.Limits;
import jdk.m3.text.TextFrame;
import jdk.m3.text.TextLine;
import jdk.m3.text.Wc;
import org.unix4j.Unix4j;
import org.unix4j.io.BufferedInput;
import org.unix4j.io.StringInput;
import org.unix4j.line.Line;
import org.unix4j.line.SimpleLine;
import org.unix4j.unix.wc.WcOption;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Differential self-test for jdk.m3.text.Wc: the Java reference, the libm3textio
 * stream kernel (UTF-16 LE/BE and Latin-1 windows) and the SoA lane kernel against
 * the Unix4j wc oracle (unix4j-command/unix4j-base 0.6 jars on the class path).
 * Prints one PASS/FAIL line per gate; exit status 1 on any failure.
 * Argument {@code unsatisfied-link} instead proves the native path fails loudly
 * when libm3textio is absent.
 */
public final class M3WcNativeSelfTest {
    private static final String[] OPTION_NAMES = {"wc", "wc -l", "wc -w", "wc -m"};
    private static final int[] WINDOWS = {1, 2, 3, 7, 64, 1 << 20};
    private static final int[] LANE_CALLS = {1, 3, Integer.MAX_VALUE};
    private static final Limits LIMITS = Limits.standard();
    private static int failures;
    private static long comparisons;
    private static final List<String> FIRST_ERRORS = new ArrayList<>();

    private M3WcNativeSelfTest() { }

    public static void main(String[] args) {
        if (args.length == 1 && args[0].equals("unsatisfied-link")) {
            unsatisfiedLink();
        } else {
            table();
            differential(cases());
            splits();
            errors();
            modes();
            System.out.println("M3_WC_NATIVE_SELFTEST " + (failures == 0 ? "PASS" : "FAIL")
                    + " failures=" + failures + " comparisons=" + comparisons);
        }
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void gate(String name, boolean pass, String detail) {
        if (!pass) failures++;
        System.out.println((pass ? "PASS " : "FAIL ") + name + " " + detail);
    }

    private static boolean same(String what, Object expected, Object actual) {
        comparisons++;
        if (expected.equals(actual)) return true;
        if (FIRST_ERRORS.size() < 8) FIRST_ERRORS.add(what + " expected=[" + expected + "] actual=[" + actual + "]");
        return false;
    }

    private static void unsatisfiedLink() {
        boolean available = Wc.nativeAvailable();
        String thrown = "none";
        try {
            Wc.countNative("a b\n", 8, false);
        } catch (UnsatisfiedLinkError expected) {
            thrown = expected.getClass().getName();
        }
        boolean javaStillWorks = Wc.countJava("a b\n").equals(new Wc.Counts(1, 2, 4));
        gate("discriminator-unsatisfied-link", !available && thrown.equals("java.lang.UnsatisfiedLinkError") && javaStillWorks,
                "nativeAvailable=" + available + " error=" + thrown + " javaReference=" + javaStillWorks);
    }

    // Every UTF-16 unit and every Latin-1 byte between two letters: 2 words iff whitespace.
    private static void table() {
        int whitespace = 0;
        int mismatches = 0;
        String first = "none";
        for (int unit = 0; unit <= 0xFFFF; unit++) {
            boolean expected = Character.isWhitespace((char) unit);
            if (expected) whitespace++;
            for (ByteOrder order : new ByteOrder[] {ByteOrder.LITTLE_ENDIAN, ByteOrder.BIG_ENDIAN}) {
                ByteBuffer window = ByteBuffer.allocateDirect(6).order(order);
                window.putChar('a').putChar((char) unit).putChar('a');
                long[] state = Wc.newState();
                Wc.feed(window, 0, 3, 2, order == ByteOrder.BIG_ENDIAN, state);
                if ((Wc.finish(state).words() == 2) != expected) {
                    mismatches++;
                    if (first.equals("none")) first = String.format("U+%04X", unit);
                }
            }
            if (unit <= 0xFF) {
                ByteBuffer window = ByteBuffer.allocateDirect(3);
                window.put((byte) 'a').put((byte) unit).put((byte) 'a');
                long[] state = Wc.newState();
                Wc.feed(window, 0, 3, 1, false, state);
                if ((Wc.finish(state).words() == 2) != expected) {
                    mismatches++;
                    if (first.equals("none")) first = String.format("U+%04X/latin1", unit);
                }
            }
        }
        comparisons += 65536L * 2 + 256;
        gate("table-exhaustive", mismatches == 0, "units=65536 whitespace=" + whitespace
                + " lookups=" + (65536 * 2 + 256) + " mismatches=" + mismatches + " first=" + first);
    }

    private static Map<String, String> cases() {
        Map<String, String> cases = new LinkedHashMap<>();
        cases.put("cr", "a\rb c\r");
        cases.put("lf", "a\nb c\n");
        cases.put("crlf", "a b\r\nc d\r\n");
        cases.put("lfcr", "a\n\rb\n\r");
        cases.put("mixed-endings", "\r\n\n\r\r\r\n\n\nx");
        cases.put("unterminated-last", "x y\nlast line no end");
        cases.put("empty", "");
        cases.put("single-empty-lf", "\n");
        cases.put("single-empty-cr", "\r");
        cases.put("single-empty-crlf", "\r\n");
        cases.put("single-empty-lfcr", "\n\r");
        cases.put("two-empty", "\n\n");
        cases.put("spaces-only", "   \t ");
        cases.put("nbsp-figure-narrow", "a\u00a0b c\u2007d e\u202ff");
        cases.put("ogham-u1680", "a\u1680b\u1680");
        StringBuilder quads = new StringBuilder("w");
        for (char c = '\u2000'; c <= '\u200a'; c++) quads.append(c).append('w');
        cases.put("u2000-u200a", quads.toString());
        cases.put("line-para-sep-u2028-u2029", "a\u2028b\u2029c");
        cases.put("mmsp-u205f", "a\u205fb");
        cases.put("ideographic-u3000", "\u3000a\u3000b\u3000");
        cases.put("info-separators-u001c-u001f", "a\u001cb\u001dc\u001ed\u001fe");
        cases.put("nel-u0085-not-space", "a\u0085b \u0085");
        cases.put("supplementary-pairs", "\uD83D\uDE00 \uD835\uDC00x\n\uD83D\uDE00\uD83D\uDE00 \uDBFF\uDFFF");
        cases.put("lone-surrogates", "a\uD800 b\uDC00c\uDFFF\uD800");
        cases.put("latin1-high", "caf\u00e9 \u00a0 na\u00efve\u00ff\n\u00a0\n\u0085");
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 700; i++) longText.append(i % 5 == 0 ? "word\r\n" : i % 3 == 0 ? "x\n\r" : "ab cd\u3000");
        cases.put("long-reader-buffer", longText.toString());
        char[] alphabet = {'a', 'b', ' ', '\t', '\r', '\n', '\u00a0', '\u2007', '\u202f', '\u1680', '\u2000',
            '\u2005', '\u200a', '\u2028', '\u2029', '\u205f', '\u3000', '\u001c', '\u001f', '\u0085', '\u00e9', '\u00ff'};
        Random random = new Random(20261029L);
        for (int c = 0; c < 300; c++) {
            StringBuilder text = new StringBuilder();
            int length = random.nextInt(c < 20 ? 6 : 200);
            for (int i = 0; i < length; i++) {
                if (random.nextInt(25) == 0) text.append("\uD83D\uDE00");
                else text.append(alphabet[random.nextInt(alphabet.length)]);
            }
            cases.put("random-" + c, text.toString());
        }
        return cases;
    }

    private static String oracle(String text, int option) {
        var builder = Unix4j.fromString(text);
        return switch (option) {
            case 0 -> builder.wc().toStringResult();
            case 1 -> builder.wc(WcOption.lines).toStringResult();
            case 2 -> builder.wc(WcOption.words).toStringResult();
            default -> builder.wc(WcOption.chars).toStringResult();
        };
    }

    private static String oracle(TextFrame frame, int option) {
        LinkedList<Line> lines = new LinkedList<>();
        for (TextLine line : frame.lines()) lines.add(new SimpleLine(line.content(), line.ending()));
        var builder = Unix4j.from(new BufferedInput(lines));
        return switch (option) {
            case 0 -> builder.wc().toStringResult();
            case 1 -> builder.wc(WcOption.lines).toStringResult();
            case 2 -> builder.wc(WcOption.words).toStringResult();
            default -> builder.wc(WcOption.chars).toStringResult();
        };
    }

    private static String readerOracle(String text, int option) {
        var builder = Unix4j.from(new StringReader(text));
        return switch (option) {
            case 0 -> builder.wc().toStringResult();
            case 1 -> builder.wc(WcOption.lines).toStringResult();
            case 2 -> builder.wc(WcOption.words).toStringResult();
            default -> builder.wc(WcOption.chars).toStringResult();
        };
    }

    private static String format(Wc.Counts counts, int option) {
        return counts.format(option == 1, option == 2, option == 3);
    }

    private static TextFrame unix4jFrame(String text) {
        List<TextLine> lines = new ArrayList<>();
        StringInput input = new StringInput(text);
        while (input.hasMoreLines()) {
            Line line = input.readLine();
            lines.add(new TextLine(line.getContent(), line.getLineEnding()));
        }
        return new TextFrame(lines);
    }

    private static Wc.Counts latin1(String text, int window) {
        byte[] bytes = new byte[text.length()];
        for (int i = 0; i < bytes.length; i++) bytes[i] = (byte) text.charAt(i);
        ByteBuffer direct = ByteBuffer.allocateDirect(Math.max(1, bytes.length));
        direct.put(bytes);
        long[] state = Wc.newState();
        for (int at = 0; at < bytes.length; ) {
            int units = Math.min(window, bytes.length - at);
            Wc.feed(direct, at, units, 1, false, state);
            at += units;
        }
        return Wc.finish(state);
    }

    private static boolean isLatin1(String text) {
        return text.chars().allMatch(c -> c <= 0xFF);
    }

    private static boolean validUnicode(String text) {
        try {
            TextFrame.fromString(text);
            return true;
        } catch (IllegalArgumentException invalid) {
            return false;
        }
    }

    private static void differential(Map<String, String> cases) {
        int caseFailures = 0;
        int latin1Cases = 0;
        int frameCases = 0;
        for (Map.Entry<String, String> entry : cases.entrySet()) {
            String name = entry.getKey();
            String text = entry.getValue();
            boolean ok = true;
            boolean latin = isLatin1(text);
            boolean frames = validUnicode(text);
            if (latin) latin1Cases++;
            if (frames) frameCases++;
            Wc.Counts java = Wc.countJava(text);
            for (int option = 0; option < 4; option++) {
                String expected = oracle(text, option);
                String tag = name + " " + OPTION_NAMES[option];
                ok &= same(tag + " oracle-reader", expected, readerOracle(text, option));
                ok &= same(tag + " java", expected, format(java, option));
                for (int window : WINDOWS) {
                    ok &= same(tag + " native-le w" + window, expected, format(Wc.countNative(text, window, false), option));
                    ok &= same(tag + " native-be w" + window, expected, format(Wc.countNative(text, window, true), option));
                    if (latin) ok &= same(tag + " latin1 w" + window, expected, format(latin1(text, window), option));
                }
                for (int calls : LANE_CALLS) {
                    ok &= same(tag + " lanes-le c" + calls, expected, format(Wc.countNativeLanes(text, calls, false), option));
                    ok &= same(tag + " lanes-be c" + calls, expected, format(Wc.countNativeLanes(text, calls, true), option));
                }
                if (frames) {
                    TextFrame unix4j = unix4jFrame(text);
                    TextFrame pack = TextFrame.fromString(text);
                    ok &= same(tag + " frame-java", expected, format(Wc.countJava(unix4j, LIMITS), option));
                    for (int calls : LANE_CALLS) {
                        ok &= same(tag + " frame-native c" + calls, expected,
                                format(Wc.countNative(unix4j, calls, false, LIMITS), option));
                    }
                    String packExpected = oracle(pack, option);
                    ok &= same(tag + " pack-frame-java", packExpected, format(Wc.countJava(pack, LIMITS), option));
                    ok &= same(tag + " pack-frame-native", packExpected,
                            format(Wc.countNative(pack, 2, true, LIMITS), option));
                }
            }
            if (!ok) caseFailures++;
        }
        gate("differential-unix4j", caseFailures == 0, "cases=" + cases.size() + " options=4 latin1Cases=" + latin1Cases
                + " frameCases=" + frameCases + " failedCases=" + caseFailures + " comparisons=" + comparisons
                + (FIRST_ERRORS.isEmpty() ? "" : " first=" + FIRST_ERRORS.get(0).replace("\n", "\\n").replace("\r", "\\r")));
        for (String name : List.of("cr", "lf", "crlf", "unterminated-last", "empty", "single-empty-lf",
                "nbsp-figure-narrow", "ogham-u1680", "u2000-u200a", "line-para-sep-u2028-u2029", "mmsp-u205f",
                "ideographic-u3000", "info-separators-u001c-u001f", "supplementary-pairs", "latin1-high")) {
            String text = cases.get(name);
            System.out.println("CASE " + name + " oracle=[" + oracle(text, 0) + "] native=["
                    + format(Wc.countNative(text, 3, false), 0) + "]");
        }
    }

    // Every split point of a window: two feeds over one direct buffer, UTF-16 LE/BE and Latin-1.
    private static void splits() {
        String[] texts = {"alpha beta\r\ngamma\n\rdelta", "caf\u00e9 na\u00efve\r\n\u00a0x\u0085y", "\uD83D\uDE00ab\u3000cd\u2028e"};
        int checks = 0;
        int midWord = 0;
        boolean ok = true;
        for (String text : texts) {
            String expected = oracle(text, 0);
            for (ByteOrder order : new ByteOrder[] {ByteOrder.LITTLE_ENDIAN, ByteOrder.BIG_ENDIAN}) {
                ByteBuffer direct = ByteBuffer.allocateDirect(text.length() * 2).order(order);
                for (int i = 0; i < text.length(); i++) direct.putChar(text.charAt(i));
                for (int k = 0; k <= text.length(); k++) {
                    long[] state = Wc.newState();
                    Wc.feed(direct, 0, k, 2, order == ByteOrder.BIG_ENDIAN, state);
                    Wc.feed(direct, k * 2, text.length() - k, 2, order == ByteOrder.BIG_ENDIAN, state);
                    ok &= same("split " + k, expected, format(Wc.finish(state), 0));
                    checks++;
                    if (k > 0 && k < text.length() && !Character.isWhitespace(text.charAt(k - 1))
                            && !Character.isWhitespace(text.charAt(k))) midWord++;
                }
            }
            if (isLatin1(text)) {
                for (int k = 0; k <= text.length(); k++) {
                    ok &= same("latin1 split " + k, expected, format(latin1Split(text, k), 0));
                    checks++;
                }
            }
        }
        gate("window-split-mid-word", ok && midWord > 0, "splits=" + checks + " midWordSplits=" + midWord);
    }

    private static Wc.Counts latin1Split(String text, int k) {
        ByteBuffer direct = ByteBuffer.allocateDirect(text.length());
        for (int i = 0; i < text.length(); i++) direct.put((byte) text.charAt(i));
        long[] state = Wc.newState();
        Wc.feed(direct, 0, k, 1, false, state);
        Wc.feed(direct, k, text.length() - k, 1, false, state);
        return Wc.finish(state);
    }

    private interface Action { void run(); }

    private static boolean throwsType(Class<? extends Throwable> type, Action action) {
        try {
            action.run();
            return false;
        } catch (Throwable thrown) {
            return type.isInstance(thrown);
        }
    }

    private static void errors() {
        ByteBuffer direct = ByteBuffer.allocateDirect(8);
        long[] finished = Wc.newState();
        Wc.finish(finished);
        long[] forged = Wc.newState();
        forged[4] = 0;
        boolean[] results = {
            throwsType(IllegalArgumentException.class, () -> Wc.feed(ByteBuffer.allocate(8), 0, 1, 2, false, Wc.newState())),
            throwsType(IllegalArgumentException.class, () -> Wc.feed(direct, 0, 1, 3, false, Wc.newState())),
            throwsType(IllegalArgumentException.class, () -> Wc.feed(direct, 0, -1, 2, false, Wc.newState())),
            throwsType(IndexOutOfBoundsException.class, () -> Wc.feed(direct, 0, 5, 2, false, Wc.newState())),
            throwsType(IndexOutOfBoundsException.class, () -> Wc.feed(direct, 7, 2, 1, false, Wc.newState())),
            throwsType(IndexOutOfBoundsException.class, () -> Wc.feed(direct, -1, 1, 1, false, Wc.newState())),
            throwsType(IllegalArgumentException.class, () -> Wc.feed(direct, 0, 1, 2, false, new long[4])),
            throwsType(IllegalArgumentException.class, () -> Wc.feed(direct, 0, 1, 2, false, forged)),
            throwsType(IllegalStateException.class, () -> Wc.feed(direct, 0, 1, 2, false, finished)),
            throwsType(NullPointerException.class, () -> Wc.feed(direct, 0, 1, 2, false, null)),
            throwsType(NullPointerException.class, () -> Wc.feed(null, 0, 1, 2, false, Wc.newState())),
            throwsType(IllegalArgumentException.class, () -> Wc.countNative("x", 0, false)),
        };
        int passed = 0;
        for (boolean result : results) if (result) passed++;
        gate("error-contracts", passed == results.length, "checks=" + results.length + " passed=" + passed);
    }

    private static void modes() {
        String text = "mode\u001fswitch\r\n\u3000oracle\n";
        Wc.Counts expected = Wc.countJava(text);
        String previous = System.getProperty(Wc.MODE_PROPERTY);
        int passed = 0;
        String detail = "";
        try {
            for (String mode : new String[] {"java", "native", "oracle", "ORACLE"}) {
                System.setProperty(Wc.MODE_PROPERTY, mode);
                try {
                    if (Wc.count(text).equals(expected)
                            && Wc.count(TextFrame.fromString(text), LIMITS).equals(Wc.countJava(TextFrame.fromString(text), LIMITS))) {
                        passed++;
                    }
                } catch (IllegalStateException disagreement) {
                    detail = " disagreement=" + disagreement.getMessage();
                }
            }
            System.setProperty(Wc.MODE_PROPERTY, "bogus");
            if (throwsType(IllegalArgumentException.class, () -> Wc.count(text))) passed++;
        } finally {
            if (previous == null) System.clearProperty(Wc.MODE_PROPERTY);
            else System.setProperty(Wc.MODE_PROPERTY, previous);
        }
        gate("mode-switch-oracle-equality", passed == 5, "modes=java,native,oracle,ORACLE,bogus passed=" + passed + detail);
    }
}
