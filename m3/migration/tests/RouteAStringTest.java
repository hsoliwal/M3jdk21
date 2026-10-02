/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
import com.m3.text.M3String;
import java.util.Random;
import java.util.regex.Pattern;

public final class RouteAStringTest {
    private static int checks;

    private static void check(boolean value) {
        checks++;
        if (!value) throw new AssertionError("check " + checks);
    }

    private static String units(int... values) {
        char[] result = new char[values.length];
        for (int index = 0; index < values.length; index++) result[index] = (char) values[index];
        return new String(result);
    }

    private static void fixedCases() {
        String[] samples = {
            "", "abc", units(0), units(0xd800, 'x', 0xdc00), units(0xd83d, 0xde00),
            "A" + units(0xd83d, 0xde00) + "B", "dog dogma dogmatic"
        };
        for (String sample : samples) {
            M3String first = M3String.fromString(sample);
            M3String second = M3String.fromString(new String(sample));
            check(first == second);
            check(first.asString().equals(sample));
            check(first.hashCode() == sample.hashCode());
            check(first.length() == sample.length());
            for (int index = 0; index < sample.length(); index++) {
                check(first.charAt(index) == sample.charAt(index));
            }
            for (int start = 0; start <= sample.length(); start++) {
                for (int end = start; end <= sample.length(); end++) {
                    M3String range = first.substring(start, end);
                    String expected = sample.substring(start, end);
                    check(range.asString().equals(expected));
                    check(range.hashCode() == expected.hashCode());
                }
            }
        }
    }

    private static void seamCases() {
        M3String left = M3String.fromString("A" + units(0xd83d));
        M3String right = M3String.fromString(units(0xde00) + "B");
        M3String joined = M3String.join(left, right);
        String expected = "A" + units(0xd83d, 0xde00) + "B";
        check(joined.asString().equals(expected));
        check(joined.hashCode() == expected.hashCode());
        check(joined.codePointAt(1) == expected.codePointAt(1));
        check(joined.codePointCount(0, joined.length()) == expected.codePointCount(0, expected.length()));
        check(joined.indexOf(M3String.fromString(units(0xd83d, 0xde00))) == 1);
        check(joined.matches(Pattern.compile("A.B")));
        char[] writable = joined.toCharArray();
        writable[0] = 'X';
        check(joined.charAt(0) == 'A');
    }

    private static void randomizedDifferential() {
        Random random = new Random(20261002);
        for (int test = 0; test < 5000; test++) {
            int size = random.nextInt(80);
            char[] value = new char[size];
            for (int index = 0; index < size; index++) value[index] = (char) random.nextInt(65536);
            String expected = new String(value);
            int split = random.nextInt(size + 1);
            M3String joined = M3String.fromString(expected.substring(0, split))
                    .concat(M3String.fromString(expected.substring(split)));
            check(joined.asString().equals(expected));
            check(joined.hashCode() == expected.hashCode());
            int needleStart = random.nextInt(size + 1);
            M3String needle = M3String.fromString(expected.substring(needleStart));
            check(joined.indexOf(needle) == expected.indexOf(needle.asString()));
        }
    }

    public static void main(String[] args) {
        fixedCases();
        seamCases();
        randomizedDifferential();
        System.out.println("ROUTE_A_PASS checks=" + checks);
    }
}
