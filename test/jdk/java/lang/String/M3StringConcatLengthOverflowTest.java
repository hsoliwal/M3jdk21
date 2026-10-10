/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 *
 * @test
 * @summary M3 String concat/join overflow must throw OutOfMemoryError, not ArithmeticException
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -Xmx512m M3StringConcatLengthOverflowTest
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings -Xmx512m M3StringConcatLengthOverflowTest
 */
import java.lang.reflect.Field;
import java.util.StringJoiner;

public class M3StringConcatLengthOverflowTest {
    private static final Field M3;
    private static int checks;

    static {
        try {
            M3 = String.class.getDeclaredField("m3");
            M3.setAccessible(true);
        } catch (ReflectiveOperationException problem) {
            throw new ExceptionInInitializerError(problem);
        }
    }

    public static void main(String[] args) throws Exception {
        // Deliberately create a large *logical* M3 range, not a 512 MiB byte[].
        // The bounded memory budget makes unintended flattening observable.
        String seed = new String(new char[] {'Q'});
        check(M3.get(seed) != null, "seed must use canonical M3 storage");
        String halfBillion = seed.repeat(1 << 29);
        check(halfBillion.length() == 536_870_912, "large logical repetition length");
        check(M3.get(halfBillion) != null, "repetition must retain no-copy M3 geometry");

        String billion = halfBillion.concat(halfBillion);
        check(billion.length() == (1 << 30), "legal concatenated logical length");
        check(billion.charAt(0) == 'Q' && billion.charAt(billion.length() - 1) == 'Q',
                "legal large concat still observes exact UTF-16 units");

        String withinLimit = String.join("", billion, halfBillion);
        check(withinLimit.length() == 1_610_612_736, "legal join may exceed flat heap");
        check(withinLimit.charAt(withinLimit.length() - 1) == 'Q',
                "legal large join has correct trailing UTF-16 unit");

        expectLogicalLengthLimit(() -> billion.concat(billion), "String.concat");
        expectLogicalLengthLimit(() -> String.join("", billion, billion),
                "String.join no delimiter");
        expectLogicalLengthLimit(() -> String.join(",", billion, billion),
                "String.join with delimiter");

        StringJoiner joiner = new StringJoiner(":", "[", "]");
        joiner.add(billion).add(billion);
        expectLogicalLengthLimit(joiner::toString, "StringJoiner");
        System.out.println("M3_STRING_CONCAT_LENGTH_OVERFLOW_PASS|checks=" + checks);
    }

    private static void expectLogicalLengthLimit(Runnable operation, String label) {
        checks++;
        try {
            operation.run();
        } catch (OutOfMemoryError expected) {
            if (!"Required length exceeds implementation limit".equals(expected.getMessage())) {
                throw new AssertionError("unrelated OOME instead of logical length guard: " + label,
                        expected);
            }
            return;
        }
        throw new AssertionError("missing OutOfMemoryError for " + label);
    }

    private static void check(boolean okay, String explanation) {
        checks++;
        if (!okay) throw new AssertionError(explanation);
    }
}
