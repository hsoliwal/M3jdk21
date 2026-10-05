/*
 * @test
 * @summary M3 String searches use canonical storage and preserve String search semantics.
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringPrecomputeSearchTest
 */

public class M3StringPrecomputeSearchTest {
    public static void main(String[] args) {
        String left = "alpha";
        String middle = "\u03b2eta";
        String emoji = "\ud83d\ude42";
        String right = "omega";

        String joined = String.join("|", left, middle, emoji, right);
        String oracle = "alpha|\u03b2eta|\ud83d\ude42|omega";

        check(joined.equals(oracle), "joined content");
        check(joined.startsWith("alpha"), "startsWith");
        check(joined.startsWith("\u03b2eta", 6), "startsWith offset");
        check(joined.endsWith("omega"), "endsWith");

        check(joined.indexOf('a') == oracle.indexOf('a'), "indexOf BMP");
        check(joined.indexOf('\u03b2') == oracle.indexOf('\u03b2'), "indexOf UTF16 BMP");
        check(joined.indexOf(0x1f642) == oracle.indexOf(0x1f642), "indexOf supplementary");
        check(joined.indexOf('z') == -1, "indexOf absent precompute rejection");

        check(joined.lastIndexOf('a') == oracle.lastIndexOf('a'), "lastIndexOf BMP");
        check(joined.lastIndexOf(0x1f642) == oracle.lastIndexOf(0x1f642),
                "lastIndexOf supplementary");

        check(joined.indexOf("\u03b2eta") == oracle.indexOf("\u03b2eta"), "indexOf String");
        check(joined.indexOf("eta", 4) == oracle.indexOf("eta", 4), "indexOf String from");
        check(joined.indexOf("eta", 0, joined.length()) == oracle.indexOf("eta", 0, oracle.length()),
                "indexOf String bounded");
        check(joined.lastIndexOf("omega") == oracle.lastIndexOf("omega"), "lastIndexOf String");
        check(joined.indexOf("not-present") == -1, "absent String");

        String splitSupplementary = String.join("", "\ud83d", "\ude42");
        check(splitSupplementary.equals("\ud83d\ude42"), "split surrogate content");
        check(splitSupplementary.indexOf(0x1f642) == 0,
                "supplementary code point across canonical atom seam");
        check(splitSupplementary.lastIndexOf(0x1f642) == 0,
                "reverse supplementary code point across canonical atom seam");

        String repeated = joined.repeat(3);
        String repeatedOracle = oracle.repeat(3);
        check(repeated.indexOf("\ud83d\ude42|omegaalpha") ==
                repeatedOracle.indexOf("\ud83d\ude42|omegaalpha"), "cross-segment search");
        check(repeated.lastIndexOf("\u03b2eta") == repeatedOracle.lastIndexOf("\u03b2eta"),
                "reverse repeated search");

        String slice = repeated.substring(3, repeated.length() - 4);
        String sliceOracle = repeatedOracle.substring(3, repeatedOracle.length() - 4);
        check(slice.indexOf("\ud83d\ude42") == sliceOracle.indexOf("\ud83d\ude42"),
                "slice supplementary search");
        check(slice.lastIndexOf("alpha") == sliceOracle.lastIndexOf("alpha"),
                "slice reverse search");

        for (int from = -2; from <= joined.length() + 2; from++) {
            check(joined.indexOf('a', from) == oracle.indexOf('a', from), "char from " + from);
            check(joined.lastIndexOf('a', from) == oracle.lastIndexOf('a', from),
                    "char reverse " + from);
            check(joined.indexOf("a", from) == oracle.indexOf("a", from),
                    "String from " + from);
            check(joined.lastIndexOf("a", from) == oracle.lastIndexOf("a", from),
                    "String reverse " + from);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
