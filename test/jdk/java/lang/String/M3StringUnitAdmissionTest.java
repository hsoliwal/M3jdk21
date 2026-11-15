// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
/*
 * @test
 * @summary Unit admission shares existing canonical owners across public constructors and pool lanes
 * @modules java.base/java.lang:+open
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringUnitAdmissionTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -Dm3.string.immediate=false M3StringUnitAdmissionTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3StringUnitAdmissionTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:-UseM3StringStorage -Dm3.string.pool.maxBytes=0 M3StringUnitAdmissionTest zero
 */

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class M3StringUnitAdmissionTest {
    private static final Class<?> POOL;
    private static final Field OWNER;
    private static final Field STRING_M3;
    private static final Field CODER;
    private static final Method UNIT;
    private static final Method CHARS;
    private static final Method LATIN1;
    private static final Method COMPACT;
    private static final Method POINTS;
    private static final Method BYTES;
    private static final boolean COMPACT_STRINGS;
    private static final boolean IMMEDIATE;
    private static final List<Object> HOLDS = new ArrayList<>();
    private static int checks;

    static {
        try {
            POOL = Class.forName("java.lang.M3StringPool");
            Class<?> value = Class.forName("java.lang.M3String");
            Class<?> owner = Class.forName("java.lang.M3StringOwner");
            OWNER = field(value, "owner");
            STRING_M3 = field(String.class, "m3");
            CODER = field(owner, "coder");
            UNIT = method(POOL, "internUnit", char.class);
            CHARS = method(POOL, "internChars", char[].class, int.class, int.class);
            LATIN1 = method(POOL, "internLatin1Bytes", byte[].class, int.class, int.class);
            COMPACT = method(POOL, "internCompactBytes", byte[].class, int.class, int.class, byte.class);
            POINTS = method(POOL, "internCodePoints", int[].class, int.class, int.class);
            BYTES = method(POOL, "localNativeBytes");
            COMPACT_STRINGS = field(String.class, "COMPACT_STRINGS").getBoolean(null);
            IMMEDIATE = field(POOL, "IMMEDIATE_STRINGS").getBoolean(null);
        } catch (ReflectiveOperationException error) {
            throw new ExceptionInInitializerError(error);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("zero")) {
            zeroNativeBudget();
        } else {
            check(field(Class.forName("java.lang.M3String"), "ready").getBoolean(null),
                    "public String admission enabled");
            for (int unit = 0; unit <= 0xff; unit++) constructors((char) unit);
            for (char unit : new char[] {'\u0100', '\ud7ff', '\ud800', '\udfff', '\uffff'}) {
                constructors(unit);
            }
            check(new String(new char[0]).isEmpty(), "empty constructor preserved");
            expect(NullPointerException.class, () -> new String((char[]) null));
            expect(IndexOutOfBoundsException.class, () -> new String(new char[] {'x'}, -1, 1));
            expect(IndexOutOfBoundsException.class, () -> new String(new char[] {'x'}, 0, 2));
        }
        System.out.println("M3StringUnitAdmissionTest checks=" + checks
                + " compact=" + COMPACT_STRINGS + " immediate=" + IMMEDIATE);
    }

    private static void constructors(char unit) throws Exception {
        String scalar = String.valueOf(unit);
        Object canonical = owner(scalar);
        String repeated = String.valueOf(unit);
        check(owner(repeated) == canonical, "repeated valueOf owner identity");
        char[] source = {'!', unit, '?'};
        String ranged = new String(source, 1, 1);
        String direct = new String(new char[] {unit});
        String points = new String(new int[] {unit}, 0, 1);
        check(owner(ranged) == canonical, "ranged char constructor owner identity");
        check(owner(direct) == canonical, "char constructor owner identity");
        check(owner(points) == canonical, "code point constructor owner identity");
        source[1] = (char) (unit ^ 1);
        check(ranged.length() == 1 && ranged.charAt(0) == unit, "mutable char input snapshotted");
        check(scalar.equals(direct) && scalar.hashCode() == unit, "unit content and Java hash exact");
        check(scalar.intern() == direct.intern(), "unit String intern identity");
        check((byte) CODER.get(canonical) == (COMPACT_STRINGS && unit <= 0xff ? 0 : 1),
                "canonical coder preserved");
        check(canonical.getClass().getName().equals(IMMEDIATE && COMPACT_STRINGS && unit <= 0xff
                ? "java.lang.M3StringImmediate" : "java.lang.M3StringAtom"),
                "documented immediate/native lane selected");
        if (unit <= 0xff) {
            byte[] bytes = {(byte) unit};
            String decoded = new String(bytes, StandardCharsets.ISO_8859_1);
            check(owner(decoded) == canonical, "Latin-1 byte constructor owner identity");
            bytes[0] ^= 1;
            check(decoded.length() == 1 && decoded.charAt(0) == unit, "mutable byte input snapshotted");
        }
        HOLDS.add(scalar);
        HOLDS.add(direct);
        HOLDS.add(ranged);
        HOLDS.add(points);
    }

    private static void zeroNativeBudget() throws Exception {
        check(COMPACT_STRINGS && IMMEDIATE, "zero budget proof selects immediate lane");
        check(!field(Class.forName("java.lang.M3String"), "ready").getBoolean(null),
                "direct pool proof excludes incidental automatic String admission");
        check(field(POOL, "MAX_LOCAL_BYTES").getLong(null) == 0, "zero native budget active");
        Map<?, ?> locals = (Map<?, ?>) field(POOL, "LOCAL").get(null);
        long beforeBytes = (long) BYTES.invoke(null);
        int beforeBuckets = locals.size();
        for (int number = 0; number <= 0xff; number++) {
            char unit = (char) number;
            Object value = UNIT.invoke(null, unit);
            check(value != null, "unit admission never refuses");
            Object canonical = OWNER.get(value);
            Object chars = CHARS.invoke(null, new char[] {unit}, 0, 1);
            Object latin = LATIN1.invoke(null, new byte[] {(byte) unit}, 0, 1);
            Object compact = COMPACT.invoke(null, new byte[] {(byte) unit}, 0, 1, (byte) 0);
            Object points = POINTS.invoke(null, new int[] {unit}, 0, 1);
            for (Object peer : new Object[] {chars, latin, compact, points}) {
                check(peer != null && OWNER.get(peer) == canonical, "zero-cost ingress shares unit owner");
            }
            check(((CharSequence) value).length() == 1 && ((CharSequence) value).charAt(0) == unit,
                    "all Latin-1 unit values exact");
            HOLDS.add(value);
        }
        check((long) BYTES.invoke(null) == beforeBytes, "immediate units charge no native bytes");
        check(locals.size() == beforeBuckets, "immediate units retain no native buckets");
        for (char unit : new char[] {'\u0100', '\ud800', '\uffff'}) {
            Object value = UNIT.invoke(null, unit);
            check(value != null && ((CharSequence) value).charAt(0) == unit,
                    "wide unit lane remains non-refusing at zero native budget");
            check(OWNER.get(value) == OWNER.get(UNIT.invoke(null, unit)), "wide unit owner reused");
            HOLDS.add(value);
        }
    }

    private static Object owner(String value) throws Exception {
        value.length();
        Object representation = STRING_M3.get(value);
        check(representation != null, "String has admitted representation");
        return OWNER.get(representation);
    }

    private static Field field(Class<?> owner, String name) throws ReflectiveOperationException {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static Method method(Class<?> owner, String name, Class<?>... parameters)
            throws ReflectiveOperationException {
        Method method = owner.getDeclaredMethod(name, parameters);
        method.setAccessible(true);
        return method;
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try {
            action.run();
        } catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("wrong exception", failure);
        }
        throw new AssertionError("expected exception missing");
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
