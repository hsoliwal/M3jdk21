// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
/*
 * @test
 * @summary Exact shared child coordinates avoid expanding repeated String tuple DAGs
 * @modules java.base/java.lang:+open
 * @run main/othervm/timeout=30 -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -Xmx256m M3StringTupleReuseTest
 * @run main/othervm/timeout=30 -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings -Xmx256m M3StringTupleReuseTest
 */
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class M3StringTupleReuseTest {
    private static final Field STORAGE;
    private static final Field OWNER;
    private static final Method SAME;
    static {
        try {
            STORAGE = String.class.getDeclaredField("m3");
            STORAGE.setAccessible(true);
            Class<?> body = Class.forName("java.lang.M3String");
            OWNER = body.getDeclaredField("owner");
            OWNER.setAccessible(true);
            SAME = Class.forName("java.lang.M3StringPool").getDeclaredMethod(
                    "sameLeafSequence", body, body, body);
            SAME.setAccessible(true);
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }
    public static void main(String[] args) throws Exception {
        String seed = new String(new char[] {'\u0800'});
        String first = seed.repeat(1 << 27);
        String second = seed.repeat(1 << 27);
        check(first.length() == 134217728 && second.length() == first.length(), "logical length");
        check(OWNER.get(body(first)) == OWNER.get(body(second)), "canonical repeated owner");
        check(first.charAt(0) == '\u0800' && first.charAt(first.length() - 1) == '\u0800', "endpoints");
        String source = new String(new char[] {'\u0100','a','\u0101','b','\u0102','c','\u0103','d'});
        String left = source.substring(0, 2);
        String right = source.substring(4, 6);
        String joined = left.concat(right);
        check(same(joined, left, right), "exact coordinate pair");
        check(!same(joined, source.substring(2, 4), right), "shifted left coordinate");
        check(!same(joined, left, source.substring(6, 8)), "shifted right coordinate");
        check(!same(joined.substring(1), left, right), "partial existing range");
        String a = new String(new char[] {'\u0100'});
        String b = new String(new char[] {'\u0101'});
        String c = new String(new char[] {'\u0102'});
        String grouped = a.concat(b.concat(c));
        check(same(grouped, a.concat(b), c), "different parenthesization retains leaf fallback");
        String aa = new String(new char[] {'A','a'});
        String bb = new String(new char[] {'B','B'});
        check(aa.hashCode() == bb.hashCode(), "collision precondition");
        check(!same(a.concat(aa), a, bb), "hash collision is not identity");
        System.out.println("M3_STRING_TUPLE_REUSE_PASS");
    }
    private static Object body(String value) throws Exception {
        Object body = STORAGE.get(value);
        if (body == null) throw new AssertionError("missing M3 storage");
        return body;
    }
    private static boolean same(String value, String left, String right) throws Exception {
        return (boolean) SAME.invoke(null, body(value), body(left), body(right));
    }
    private static void check(boolean okay, String label) {
        if (!okay) throw new AssertionError(label);
    }
}
