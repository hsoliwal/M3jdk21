// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
/*
 * @test id=flagOff
 * @summary Composite search and reverse preserve bounded piece storage and UTF-16 contracts
 * @modules java.base/java.lang:+open
 * @run main/othervm M3CompositeBuilderSearchReverseTest
 * @run main/othervm -XX:-CompactStrings M3CompositeBuilderSearchReverseTest
 * @run main/othervm -Xint M3CompositeBuilderSearchReverseTest
 */
/*
 * @test id=flagOn
 * @summary Composite search and reverse on a matched M3 storage image
 * @modules java.base/java.lang:+open
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3CompositeBuilderSearchReverseTest flagOn
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -Dm3.string.pool.maxBytes=0 M3CompositeBuilderSearchReverseTest flagOn
 */

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;

public class M3CompositeBuilderSearchReverseTest {
    static final Class<?> ASB;
    static final Field LANE, VALUE, COUNT;
    static final Method PIECE;
    static long checks;
    static {
        try {
            ASB = Class.forName("java.lang.AbstractStringBuilder");
            LANE = field(ASB, "m3Lane");
            VALUE = field(ASB, "value");
            COUNT = field(ASB, "count");
            PIECE = ASB.getDeclaredMethod("m3AppendPiece", String.class, int.class, int.class);
            PIECE.setAccessible(true);
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }
    static Field field(Class<?> type, String name) throws NoSuchFieldException {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
    static void check(boolean ok, String where) {
        checks++;
        if (!ok) throw new AssertionError(where);
    }
    static final class Subject {
        final Object raw;
        final StringBuilder oracle = new StringBuilder();
        Subject(boolean buffer) { raw = buffer ? new StringBuffer() : new StringBuilder(); }
        CharSequence text() { return (CharSequence) raw; }
        void flat(char[] units) {
            if (raw instanceof StringBuilder builder) builder.append(units);
            else ((StringBuffer) raw).append(units);
            oracle.append(units);
        }
        void piece(char[] units, int from, int to) throws Exception {
            PIECE.invoke(raw, new String(units), from, to);
            if (raw instanceof StringBuffer buffer) buffer.append("");
            oracle.append(units, from, to - from);
        }
        int capacity() {
            return raw instanceof StringBuilder builder ? builder.capacity() : ((StringBuffer) raw).capacity();
        }
        int search(String needle, int from, boolean backwards) {
            if (raw instanceof StringBuilder builder) return backwards
                    ? builder.lastIndexOf(needle, from) : builder.indexOf(needle, from);
            StringBuffer buffer = (StringBuffer) raw;
            return backwards ? buffer.lastIndexOf(needle, from) : buffer.indexOf(needle, from);
        }
        int search(String needle, boolean backwards) {
            if (raw instanceof StringBuilder builder) return backwards
                    ? builder.lastIndexOf(needle) : builder.indexOf(needle);
            StringBuffer buffer = (StringBuffer) raw;
            return backwards ? buffer.lastIndexOf(needle) : buffer.indexOf(needle);
        }
        Object reverse() {
            return raw instanceof StringBuilder builder ? builder.reverse() : ((StringBuffer) raw).reverse();
        }
    }
    static void content(Subject subject, String where) {
        CharSequence actual = subject.text();
        check(actual.length() == subject.oracle.length(), where + " length");
        for (int i = 0; i < actual.length(); i++) {
            check(actual.charAt(i) == subject.oracle.charAt(i), where + " unit " + i);
        }
        check(subject.capacity() == subject.oracle.capacity(), where + " capacity");
    }
    static void searches(Subject subject, String[] needles) throws Exception {
        Object lane = LANE.get(subject.raw);
        Object flat = VALUE.get(subject.raw);
        byte[] before = ((byte[]) flat).clone();
        int count = COUNT.getInt(subject.raw);
        int length = subject.text().length();
        int[] starts = {Integer.MIN_VALUE, -1, 0, 1, length / 2, length - 1,
                length, length + 1, Integer.MAX_VALUE};
        for (String needle : needles) {
            for (boolean backwards : new boolean[] {false, true}) {
                for (int start : starts) {
                    int wanted = backwards ? subject.oracle.lastIndexOf(needle, start)
                            : subject.oracle.indexOf(needle, start);
                    check(subject.search(needle, start, backwards) == wanted,
                            "search direction=" + backwards + " from=" + start);
                    check(LANE.get(subject.raw) == lane, "search detached composite lane");
                    check(VALUE.get(subject.raw) == flat, "search replaced flat backing");
                }
                int wanted = backwards ? subject.oracle.lastIndexOf(needle) : subject.oracle.indexOf(needle);
                check(subject.search(needle, backwards) == wanted, "search overload");
                check(LANE.get(subject.raw) == lane, "search overload detached lane");
                try {
                    subject.search(null, 0, backwards);
                    throw new AssertionError("null needle accepted");
                } catch (NullPointerException expected) { }
                check(LANE.get(subject.raw) == lane, "null search detached lane");
            }
        }
        check(COUNT.getInt(subject.raw) == count, "search changed flat count");
        check(Arrays.equals(before, (byte[]) flat), "search wrote flat payload");
        content(subject, "search");
    }
    static void reverse(Subject subject) throws Exception {
        Object flat = VALUE.get(subject.raw);
        int capacity = subject.capacity();
        int length = subject.text().length();
        check(subject.reverse() == subject.raw, "reverse identity");
        subject.oracle.reverse();
        check(LANE.get(subject.raw) != null, "reverse detached composite lane");
        check(VALUE.get(subject.raw) == flat, "reverse allocated whole flat backing");
        check(subject.capacity() == capacity, "reverse changed capacity");
        check(subject.text().length() == length, "reverse changed length");
        content(subject, "reverse");
    }
    static void seams(boolean buffer) throws Exception {
        char[] alphabet = {'x', '\u00e9', '\u20ac', '\ud800', '\udc00'};
        for (char first : alphabet) for (char second : alphabet) {
            for (int shape = 0; shape < 4; shape++) {
                Subject subject = new Subject(buffer);
                subject.piece(new char[] {'z'}, 0, 0);
                subject.flat(new char[] {'p'});
                if ((shape & 1) == 0) subject.flat(new char[] {first});
                else subject.piece(new char[] {'!', first, '?'}, 1, 2);
                subject.piece(new char[] {'z'}, 1, 1);
                if ((shape & 2) == 0) subject.flat(new char[] {second});
                else subject.piece(new char[] {'!', second, '?'}, 1, 2);
                subject.flat(new char[] {'q'});
                searches(subject, new String[] {"", "p", "q", new String(new char[] {first, second}), "missing"});
                reverse(subject);
                searches(subject, new String[] {"", "q", new String(new char[] {second, first})});
                reverse(subject);
                subject.flat(new char[] {'!', '\ud800', '\udc00'});
                content(subject, "append after reverse");
            }
        }
    }
    static void random(boolean buffer) throws Exception {
        Random random = new Random(0x4d334255494c44L);
        for (int run = 0; run < 100; run++) {
            Subject subject = new Subject(buffer);
            for (int piece = 0; piece < 20; piece++) {
                char[] units = new char[random.nextInt(100)];
                for (int i = 0; i < units.length; i++) units[i] = switch (random.nextInt(6)) {
                    case 0 -> '\ud800'; case 1 -> '\udc00'; case 2 -> '\u20ac';
                    default -> (char) ('a' + random.nextInt(4));
                };
                if ((piece & 3) == 0) subject.flat(units);
                else subject.piece(units, 0, units.length);
            }
            String expected = subject.oracle.toString();
            String[] needles = new String[12];
            needles[0] = ""; needles[1] = "absent";
            for (int i = 2; i < needles.length; i++) {
                int from = random.nextInt(expected.length() + 1);
                needles[i] = expected.substring(from, Math.min(expected.length(), from + random.nextInt(60)));
            }
            searches(subject, needles);
            reverse(subject);
            searches(subject, needles);
            reverse(subject);
            content(subject, "random twice");
        }
    }
    static void large(boolean buffer) throws Exception {
        Subject subject = new Subject(buffer);
        char[] block = new char[1025];
        Arrays.fill(block, 'a');
        block[0] = '\udc00'; block[block.length - 1] = '\ud800';
        for (int i = 0; i < 128; i++) subject.piece(block, 0, block.length);
        subject.flat(new char[] {'b'});
        searches(subject, new String[] {"a".repeat(9000), "a".repeat(9000) + "b", "\ud800\udc00", ""});
        reverse(subject);
        content(subject, "large reverse");
    }
    static void publicAppend(boolean buffer) throws Exception {
        if ("0".equals(System.getProperty("m3.string.pool.maxBytes"))) return;
        Subject subject = new Subject(buffer);
        char[] units = new char[320];
        Arrays.fill(units, 'r'); units[160] = '\ud800'; units[161] = '\udc00';
        String admitted = new String(units);
        if (subject.raw instanceof StringBuilder builder) builder.append(admitted);
        else ((StringBuffer) subject.raw).append(admitted);
        subject.oracle.append(units);
        check(LANE.get(subject.raw) != null, "public append did not admit M3 piece");
        // Populate the existing canonical composition cache; subsequent search must reuse it.
        check(subject.raw.toString().contentEquals(subject.oracle), "composed snapshot");
        searches(subject, new String[] {"", "rr", "\ud800\udc00", "missing"});
        reverse(subject);
    }
    public static void main(String[] args) throws Exception {
        for (boolean buffer : new boolean[] {false, true}) {
            seams(buffer);
            random(buffer);
            large(buffer);
            if (args.length > 0) publicAppend(buffer);
        }
        System.out.println("M3 composite search/reverse checks=" + checks);
    }
}
