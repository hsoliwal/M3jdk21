/*
 * @test
 * @summary Prepared M3 range facts bulk-copy Latin1 slices into StringBuilder without premature inflation
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringBuilderRangeBulkTest
 */

import java.lang.reflect.Field;

public class M3StringBuilderRangeBulkTest {
    private static final Field CODER;
    private static long checks;

    static {
        try {
            Class<?> asb = Class.forName("java.lang.AbstractStringBuilder");
            CODER = asb.getDeclaredField("coder");
            CODER.setAccessible(true);
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    public static void main(String[] args) throws Exception {
        preparedLatin1Range();
        coldLatin1Range();
        nonLatin1RangeInflates();
        utf16Destination();
        latin1SourceIntoUtf16Destination();
        System.out.println("M3_STRING_BUILDER_RANGE_BULK_PASS|checks=" + checks);
    }

    private static void preparedLatin1Range() throws Exception {
        String body = "latin-range-0123456789";
        String source = String.join("", "\u0100", body, "\u03a9");
        int begin = 1;
        int end = begin + body.length();

        String range = source.substring(begin, end);
        check(range.codePointCount(0, range.length()) == body.length(), "prepare range facts");

        StringBuilder builder = new StringBuilder(">");
        builder.append(source, begin, end);
        check(builder.toString().equals(">" + body), "prepared range content");
        check(coder(builder) == 0, "prepared Latin1 range keeps LATIN1 builder");
    }

    private static void coldLatin1Range() throws Exception {
        String body = "cold-latin-range";
        String source = String.join("", "\u0100", body, "\u0401");
        StringBuilder builder = new StringBuilder();
        builder.append(source, 1, 1 + body.length());
        check(builder.toString().equals(body), "cold range fallback content");
        check(coder(builder) == 0, "cold Latin1 range keeps LATIN1 builder");
    }

    private static void nonLatin1RangeInflates() throws Exception {
        String source = String.join("", "a", "\u0100", "b");
        StringBuilder builder = new StringBuilder(">");
        builder.append(source, 0, source.length());
        check(builder.toString().equals(">a\u0100b"), "UTF16 range content");
        check(coder(builder) == 1, "non-Latin1 range inflates builder");
    }

    private static void utf16Destination() throws Exception {
        String source = String.join("", "\u0100", "bulk-ascii", "\u03a9");
        StringBuilder builder = new StringBuilder("\u0100");
        builder.append(source, 1, 11);
        check(builder.toString().equals("\u0100bulk-ascii"), "UTF16 destination content");
        check(coder(builder) == 1, "UTF16 destination remains UTF16");
    }

    private static void latin1SourceIntoUtf16Destination() throws Exception {
        String source = String.join("", "prefix-", "café", "-suffix");
        StringBuilder builder = new StringBuilder("\u0100");
        builder.append(source, 7, 11);
        check(builder.toString().equals("\u0100café"),
                "M3 Latin1 source bulk-inflates into UTF16 destination");
        check(coder(builder) == 1, "UTF16 destination coder preserved");
        builder.append(source, 0, 7);
        check(builder.toString().equals("\u0100caféprefix-"),
                "multiple M3 bulk ranges preserve order");
    }

    private static int coder(StringBuilder builder) throws IllegalAccessException {
        return CODER.getByte(builder);
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
