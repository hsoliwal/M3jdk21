/*
 * @test
 * @summary Byte-backed String/Builder search consumes canonical M3 needles without owner-coder false negatives
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringByteSourceNeedleTest
 */

public class M3StringByteSourceNeedleTest {
    private static long checks;

    public static void main(String[] args) {
        latin1RangeFromUtf16Owner();
        oneUnitRange();
        utf16Source();
        preparedLongNeedle();
        oversizedFallback();
        System.out.println("M3_STRING_BYTE_SOURCE_NEEDLE_PASS|checks=" + checks);
    }

    private static void latin1RangeFromUtf16Owner() {
        String body = "latin-range";
        String owner = String.join("", "\u0100", body, "\u03a9");
        String needle = owner.substring(1, 1 + body.length());

        StringBuilder source = new StringBuilder("xx" + body + "--" + body + "yy");
        check(source.indexOf(needle) == 2, "Latin1 builder finds UTF16-owner range");
        check(source.indexOf(needle, 3) == 2 + body.length() + 2, "from-index search");
        check(source.lastIndexOf(needle) == 2 + body.length() + 2, "reverse search");
        check(source.lastIndexOf(needle, body.length()) == 2, "reverse from-index search");

        String nonLatin = owner.substring(0, 2);
        check(source.indexOf(nonLatin) == -1, "Latin1 source rejects real non-Latin1 needle");
        check(source.lastIndexOf(nonLatin) == -1, "reverse non-Latin1 reject");
    }

    private static void oneUnitRange() {
        String owner = String.join("", "\u0100", "x", "\u03a9");
        String needle = owner.substring(1, 2);
        StringBuilder source = new StringBuilder("--x--x");
        check(source.indexOf(needle) == 2, "single-unit M3 range index");
        check(source.lastIndexOf(needle) == 5, "single-unit M3 range reverse");
    }

    private static void utf16Source() {
        String owner = String.join("", "\u0100", "ascii-needle", "\u03a9");
        String needle = owner.substring(1, 13);
        StringBuilder source = new StringBuilder("\u0100--ascii-needle--\u03a9");
        check(source.indexOf(needle) == 3, "UTF16 builder forward");
        check(source.lastIndexOf(needle) == 3, "UTF16 builder reverse");
    }

    private static void preparedLongNeedle() {
        String body = "abcdefghij".repeat(40) + "XYZ";
        String owner = String.join("", "\u0100", body, "\u03a9");
        String needle = owner.substring(1, 1 + body.length());

        StringBuilder source = new StringBuilder("prefix-" + body + "-suffix-" + body);
        check(source.indexOf(needle) == 7, "prepared long forward");
        check(source.lastIndexOf(needle) == 7 + body.length() + 8, "prepared long reverse");
    }

    private static void oversizedFallback() {
        String body = "abcdefgh".repeat(1_100);
        String owner = String.join("", "\u0100", body, "\u03a9");
        String needle = owner.substring(1, 1 + body.length());
        StringBuilder source = new StringBuilder("!" + body + "!");
        check(source.indexOf(needle) == 1, "oversized exact fallback forward");
        check(source.lastIndexOf(needle) == 1, "oversized exact fallback reverse");
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
