/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
import com.m3.text.M3NumberSpace;
import com.m3.text.LocalM3StringPiece;

public final class M3NumberSpaceTest {
    private static int checks;

    private static void check(boolean condition) {
        checks++;
        if (!condition) throw new AssertionError("check " + checks);
    }

    private static void expect(Class<? extends Throwable> kind, Runnable body) {
        try {
            body.run();
            throw new AssertionError("missing " + kind.getSimpleName());
        } catch (Throwable failure) {
            check(kind.isInstance(failure));
        }
    }

    public static void main(String[] args) {
        M3NumberSpace space = M3NumberSpace.INSTANCE;
        check(space.precomputedValueCount() == 10_001);
        LocalM3StringPiece first = space.number(0);
        LocalM3StringPiece last = space.number(10_000);
        check(first == space.number(0));
        check(last == space.number(10_000));
        check(first.storageIdentity().equals(last.storageIdentity()));
        check(space.storageIdentity().equals(first.storageIdentity()));
        for (int value = M3NumberSpace.MIN_VALUE; value <= M3NumberSpace.MAX_VALUE; value++) {
            check(space.spelling(value).equals(Integer.toString(value)));
            check(space.number(value).flatten().equals(Integer.toString(value)));
        }
        check(space.parse(new View("10000")) == last);
        var typed = space.precompute(10000, "und");
        check(typed.value() == 10_000);
        check(typed.spelling().equals("10000"));
        check(typed.languageTag().equals("und"));
        expect(NumberFormatException.class, () -> space.parse(new View("01")));
        expect(NumberFormatException.class, () -> space.parse(new View("10001")));
        expect(NumberFormatException.class, () -> space.parse(new View("10x")));
        expect(NumberFormatException.class, () -> space.parse(new View("")));
        expect(IndexOutOfBoundsException.class, () -> space.number(-1));
        expect(IndexOutOfBoundsException.class, () -> space.number(10_001));
        check(M3NumberSpace.SOURCE_ID.equals("dictlang.numbers.0-10000"));
        check(M3NumberSpace.RECORD_ID.equals("number"));
        System.out.println("M3JDK_NUMBER_SPACE_PASS checks=" + checks);
    }

    private static final class View implements CharSequence {
        private final String value;
        View(String value) { this.value = value; }
        @Override public int length() { return value.length(); }
        @Override public char charAt(int index) { return value.charAt(index); }
        @Override public CharSequence subSequence(int start, int end) { return value.subSequence(start, end); }
        @Override public String toString() { throw new AssertionError("number parser materialized view"); }
    }
}

