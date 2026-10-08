/*
 * @test
 * @summary Literal replacement stays canonical when target or replacement is M3-backed
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringMixedLiteralReplaceTest
 */

public class M3StringMixedLiteralReplaceTest {
    private static long checks;

    public static void main(String[] args) {
        mixedTargetAndReplacement();
        absentPreservesIdentity();
        deletion();
        emptyTargetWithM3Replacement();
        System.out.println("M3_STRING_MIXED_LITERAL_REPLACE_PASS|checks=" + checks);
    }

    private static void mixedTargetAndReplacement() {
        String targetOwner = String.join("", "\u0100", "TARGET", "\u03a9");
        String target = targetOwner.substring(1, 7);
        String replacementOwner = String.join("", "\u0401", "rep", "\u03a9");
        String replacement = replacementOwner.substring(1, 4);

        String source = "xxTARGETyyTARGET";
        String actual = source.replace(target, replacement);
        check(actual.equals("xxrepyyrep"), "mixed target/replacement content");
    }

    private static void absentPreservesIdentity() {
        String targetOwner = String.join("", "\u0100", "MISSING", "\u03a9");
        String target = targetOwner.substring(1, 8);
        String source = "plain-source";
        check(source.replace(target, "x") == source, "absent replacement preserves identity");
    }

    private static void deletion() {
        String targetOwner = String.join("", "\u0100", "cut", "\u03a9");
        String target = targetOwner.substring(1, 4);
        String source = "cut--cut";
        check(source.replace(target, "").equals("--"), "mixed M3 target deletion");
    }

    private static void emptyTargetWithM3Replacement() {
        String replacementOwner = String.join("", "\u0100", "R", "\u03a9");
        String replacement = replacementOwner.substring(1, 2);
        String source = "ab";
        check(source.replace("", replacement).equals("RaRbR"),
                "empty target with M3 replacement");
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
