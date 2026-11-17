/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 * @test
 * @summary Static ASCII specification effects preserve exact owners, ranges and unit domains
 * @modules java.base/jdk.internal.mindex
 * @run main/othervm -Xcheck:jni M3RX7StaticPrecomputeTest
 * @run main/othervm -Xint -Xcheck:jni M3RX7StaticPrecomputeTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3RX7StaticPrecomputeTest require-m3
 */

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import jdk.internal.mindex.M3RX7;

public class M3RX7StaticPrecomputeTest {
    private static final String ROOT = "a".repeat(64);
    private static long checks;

    public static void main(String[] args) throws Exception {
        Fixture identifier = identifier();
        Fixture number = jsonNumber();
        for (Fixture fixture : new Fixture[] {identifier, number}) {
            grammarParity(fixture);
            compositionParity(fixture);
            repetitionParity(fixture);
        }
        sourceAndRangeContracts(identifier);
        ownerAndDomainContracts(identifier);
        resourceContracts();
        concurrentReuse(identifier);
        m3PlanTransfer(args.length != 0 && args[0].equals("require-m3"));
        System.out.println("M3RX7StaticPrecomputeTest checks=" + checks + " PASS");
    }

    private record Fixture(M3RX7 rx, short[] rows, byte[] accepting, Pattern oracle) { }

    private static Fixture identifier() {
        short[] rows = deadRows(2);
        for (int unit = 0; unit < 128; unit++) {
            boolean first = unit == '_' || unit >= 'a' && unit <= 'z'
                    || unit >= 'A' && unit <= 'Z';
            if (first) rows[unit] = 1;
            if (first || unit >= '0' && unit <= '9') rows[128 + unit] = 1;
        }
        byte[] accepting = {0, 1};
        return fixture(rows, accepting, "[A-Za-z_][A-Za-z_0-9]*");
    }

    // RFC 8259 number syntax; the table is an independently authored test oracle.
    private static Fixture jsonNumber() {
        short[] rows = deadRows(9);
        rows['-'] = 1;
        rows['0'] = 2;
        rows[128 + '0'] = 2;
        for (int unit = '1'; unit <= '9'; unit++) {
            rows[unit] = 3;
            rows[128 + unit] = 3;
        }
        for (int state : new int[] {2, 3}) rows[state * 128 + '.'] = 4;
        for (int state : new int[] {2, 3, 5}) {
            rows[state * 128 + 'e'] = 6;
            rows[state * 128 + 'E'] = 6;
        }
        rows[6 * 128 + '+'] = 7;
        rows[6 * 128 + '-'] = 7;
        for (int unit = '0'; unit <= '9'; unit++) {
            rows[3 * 128 + unit] = 3;
            rows[4 * 128 + unit] = 5;
            rows[5 * 128 + unit] = 5;
            rows[6 * 128 + unit] = 8;
            rows[7 * 128 + unit] = 8;
            rows[8 * 128 + unit] = 8;
        }
        byte[] accepting = {0, 0, 1, 1, 0, 1, 0, 0, 1};
        return fixture(rows, accepting,
                "-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?");
    }

    private static Fixture fixture(short[] rows, byte[] accepting, String pattern) {
        return new Fixture(M3RX7.fromAsciiDfa(ROOT, 0, rows, accepting),
                rows, accepting, Pattern.compile(pattern));
    }

    private static short[] deadRows(int states) {
        short[] rows = new short[states * 128];
        Arrays.fill(rows, (short) -1);
        return rows;
    }

    private static void grammarParity(Fixture fixture) {
        String[] cases = {"", "x", "_", "alpha9", "0", "00", "-0", "-12.5e+07",
                "1.", ".1", "1e", "1e-2", "01", "+1", "1E000", "--1", "a-b",
                " true ", "null", "\0", "\t", "\n", "a\r\nb"};
        for (String input : cases) verify(fixture, input);
        Random random = new Random(0x20261117);
        String alphabet = "azAZ_019.+-eE! \t\r\n\0";
        for (int sample = 0; sample < 2000; sample++) {
            StringBuilder input = new StringBuilder();
            for (int i = 0, count = random.nextInt(32); i < count; i++) {
                input.append(alphabet.charAt(random.nextInt(alphabet.length())));
            }
            verify(fixture, input.toString());
        }
        for (int unit = 0; unit < 128; unit++) verify(fixture, String.valueOf((char) unit));
    }

    private static void verify(Fixture fixture, String input) {
        M3RX7.Transfer effect = fixture.rx.transferAscii(input);
        check(effect != null, "ASCII admitted");
        check(effect.length() == input.length(), "UTF-16 length");
        check(effect.matches() == fixture.oracle.matcher(input).matches(), "JDK regex parity");
        check(effect.nextState(-1) == -1, "dead-state identity");
        for (int state = 0; state < fixture.accepting.length; state++) {
            check(effect.nextState(state) == advance(fixture, state, input),
                    "all-entry-state differential");
        }
    }

    private static int advance(Fixture fixture, int state, CharSequence input) {
        for (int i = 0; i < input.length() && state >= 0; i++) {
            state = fixture.rows[state * 128 + input.charAt(i)];
        }
        return state;
    }

    private static void compositionParity(Fixture fixture) {
        String[] cases = {"", "abc_09", "-125.75e-09", "0", "-0.12", "01", "a!b", "1e+"};
        for (String input : cases) {
            String wrapped = "\u0100" + input + "\u0100";
            M3RX7.Transfer whole = fixture.rx.transferAscii(wrapped, 1, input.length());
            for (int a = 0; a <= input.length(); a++) {
                for (int b = a; b <= input.length(); b++) {
                    M3RX7.Transfer left = fixture.rx.transferAscii(wrapped, 1, a);
                    M3RX7.Transfer middle = fixture.rx.transferAscii(wrapped, a + 1, b - a);
                    M3RX7.Transfer right = fixture.rx.transferAscii(wrapped, b + 1, input.length() - b);
                    M3RX7.Transfer first = left.then(middle).then(right);
                    M3RX7.Transfer second = left.then(middle.then(right));
                    check(first.length() == input.length(), "composed range length");
                    check(first.matches() == whole.matches(), "seam acceptance");
                    for (int state = 0; state < fixture.accepting.length; state++) {
                        check(first.nextState(state) == whole.nextState(state), "seam state");
                        check(second.nextState(state) == whole.nextState(state), "associativity");
                    }
                }
            }
        }
    }

    private static void repetitionParity(Fixture fixture) {
        for (String atom : new String[] {"", "a", "0", "1", "12", "e1", "!"}) {
            M3RX7.Transfer effect = fixture.rx.transferAscii(atom);
            for (int count : new int[] {0, 1, 2, 7, 32, 257}) {
                M3RX7.Transfer repeated = effect.repeated(count);
                M3RX7.Transfer direct = fixture.rx.transferAscii(atom.repeat(count));
                check(repeated.length() == (long) atom.length() * count, "repeated length");
                for (int state = 0; state < fixture.accepting.length; state++) {
                    check(repeated.nextState(state) == direct.nextState(state), "power parity");
                }
            }
        }
        M3RX7.Transfer two = fixture.rx.transferAscii("aa");
        expect(ArithmeticException.class, () -> two.repeated(Long.MAX_VALUE));
        M3RX7.Transfer maximum = fixture.rx.transferAscii("a").repeated(Long.MAX_VALUE);
        expect(ArithmeticException.class, () -> maximum.then(fixture.rx.transferAscii("a")));
        expect(IllegalArgumentException.class, () -> two.repeated(-1));
    }

    private static void sourceAndRangeContracts(Fixture fixture) {
        CountingSequence input = new CountingSequence("_alpha9");
        M3RX7.Transfer effect = fixture.rx.transferAscii(input, 1, 5);
        check(effect != null && effect.matches(), "selected source range");
        check(input.reads == 5, "one charAt call per source unit");
        input.value.setCharAt(1, '!');
        check(effect.matches(), "no mutable source retention");
        for (int i = 0; i < 100; i++) check(effect.matches(), "reused immutable result");
        check(input.reads == 5, "warm result performs no source reads");
        check(fixture.rx.transferAscii("a\u00e9") == null, "non-ASCII refusal");
        check(fixture.rx.transferAscii("!\ud83d\ude42") == null, "surrogate refusal after dead state");
        check(fixture.rx.transferAscii("\ud83d") == null, "unpaired surrogate refusal");
        check(fixture.rx.transferAscii("\u0100a\u0100", 1, 1).matches(),
                "outside-range Unicode ignored");
        expect(NullPointerException.class, () -> fixture.rx.transferAscii(null));
        expect(IndexOutOfBoundsException.class, () -> fixture.rx.transferAscii("a", -1, 1));
        expect(IndexOutOfBoundsException.class, () -> fixture.rx.transferAscii("a", 0, 2));
        expect(IndexOutOfBoundsException.class, () -> fixture.rx.transferAscii("a", 1, -1));
        expect(IndexOutOfBoundsException.class, () -> fixture.rx.transferAscii("a", Integer.MAX_VALUE, 1));
        expect(IndexOutOfBoundsException.class, () -> effect.nextState(-2));
        expect(IndexOutOfBoundsException.class, () -> effect.nextState(fixture.accepting.length));
    }

    private static final class CountingSequence implements CharSequence {
        final StringBuilder value;
        int reads;
        CountingSequence(String value) { this.value = new StringBuilder(value); }
        public int length() { return value.length(); }
        public char charAt(int index) { reads++; return value.charAt(index); }
        public CharSequence subSequence(int start, int end) { throw new AssertionError("flattening"); }
        public String toString() { throw new AssertionError("flattening"); }
    }

    private static void ownerAndDomainContracts(Fixture fixture) {
        M3RX7.Transfer text = fixture.rx.transferAscii("a");
        M3RX7.TokenIndex index = fixture.rx.prepareTokenIndex(
                new byte[] {'a'}, new int[] {0}, new int[] {1});
        M3RX7.Transfer tokens = index.transfer(new int[] {0});
        expect(IllegalArgumentException.class, () -> text.then(tokens));
        expect(IllegalArgumentException.class, () -> tokens.then(text));
        expect(IllegalArgumentException.class, () -> text.repeated(0).then(tokens.repeated(0)));
        expect(IllegalArgumentException.class, () -> text.repeated(3).then(tokens.repeated(3)));
        M3RX7 other = M3RX7.fromAsciiDfa(ROOT, 0, fixture.rows, fixture.accepting);
        expect(IllegalArgumentException.class, () -> text.then(other.transferAscii("a")));
        expect(NullPointerException.class, () -> text.then(null));
        check(tokens.then(tokens).length() == 2, "old token domain retained");
        check(tokens.repeated(100).matches(), "old token power retained");
    }

    private static void resourceContracts() {
        short[] rows = new short[4096 * 128];
        byte[] accepting = new byte[4096];
        M3RX7 rx = M3RX7.fromAsciiDfa(ROOT, 0, rows, accepting);
        CountingSequence overWork = new CountingSequence("a".repeat(8193));
        check(rx.transferAscii(overWork) == null, "state/unit work budget");
        check(overWork.reads == 0, "budget refused before source scan");
        CharSequence overText = new CharSequence() {
            public int length() { return 1_000_001; }
            public char charAt(int index) { throw new AssertionError("oversized source read"); }
            public CharSequence subSequence(int start, int end) { throw new AssertionError(); }
        };
        check(rx.transferAscii(overText) == null, "text budget");
        check(rx.transferAscii(overText, 0, 0).length() == 0, "empty selected range");
        check(rx.transferAscii("").nextState(4095) == 4095, "maximum-state identity");
    }

    private static void concurrentReuse(Fixture fixture) throws Exception {
        M3RX7.Transfer effect = fixture.rx.transferAscii("alpha9");
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread[] workers = new Thread[4];
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new Thread(() -> {
                try {
                    for (int j = 0; j < 1000; j++) {
                        if (!effect.matches() || !effect.then(effect).matches()) {
                            throw new AssertionError("shared effect changed");
                        }
                    }
                } catch (Throwable error) { failure.compareAndSet(null, error); }
            });
            workers[i].start();
        }
        for (Thread worker : workers) worker.join();
        if (failure.get() != null) throw new AssertionError("concurrent reuse", failure.get());
        check(effect.matches(), "immutable concurrent effect");
    }

    private static void m3PlanTransfer(boolean required) {
        Fixture fixture = identifier();
        String input = new String("alpha").concat(new String("_beta")).concat(new String("9"));
        M3RX7.M3TokenPlan plan = fixture.rx.prepareM3TokenPlan(input);
        if (plan == null) {
            check(!required, "M3 token plan required by storage-enabled run");
            System.out.println("M3 token plan unavailable in this VM mode");
            return;
        }
        M3RX7.Transfer effect = plan.transfer();
        check(effect != null, "M3 plan collapsed to effect");
        check(effect.length() == plan.tokenSequenceLength(), "M3 plan token length");
        check(effect.matches() == plan.matches(), "M3 plan effect parity");
        check(effect.matches() == fixture.oracle.matcher(input).matches(), "M3 storage JDK parity");
        check(effect.retainedPrimitiveBytes() <= 3L * fixture.accepting.length,
                "collapsed effect retains state rows and accepting flags only");
        expect(IllegalArgumentException.class, () -> effect.then(fixture.rx.transferAscii(input)));
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try { action.run(); }
        catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("unexpected failure", failure);
        }
        throw new AssertionError("missing " + type.getName());
    }
}
