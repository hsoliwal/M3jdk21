/*
 * @test
 * @summary Compiler concat receiver folds directly into balanced M3 reference DAG
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringConcatReferenceDagTest
 */

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class M3StringConcatReferenceDagTest {
    private static final Method CONCAT;
    private static final Field STRING_M3;
    private static final Field M3_OWNER;
    private static final Field TUPLE_HEIGHT;
    private static long checks;

    static {
        try {
            Class<?> helper = Class.forName("java.lang.StringConcatHelper");
            CONCAT = helper.getDeclaredMethod("m3Concat", String[].class, Object[].class);
            CONCAT.setAccessible(true);
            STRING_M3 = String.class.getDeclaredField("m3");
            STRING_M3.setAccessible(true);
            Class<?> m3 = Class.forName("java.lang.M3String");
            M3_OWNER = m3.getDeclaredField("owner");
            M3_OWNER.setAccessible(true);
            Class<?> tuple = Class.forName("java.lang.M3StringTuple");
            TUPLE_HEIGHT = tuple.getDeclaredField("height");
            TUPLE_HEIGHT.setAccessible(true);
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    public static void main(String[] args) throws Exception {
        check(concat(new String[]{"alpha-", "-", "-omega"}, new Object[]{42, "beta"})
                .equals("alpha-42-beta-omega"), "mixed content");
        check(concat(new String[]{null, null, null}, new Object[]{"left", "right"})
                .equals("leftright"), "null constants");

        StringBuilder trace = new StringBuilder();
        String ordered = concat(new String[]{"<", "|", ">"},
                new Object[]{new Probe("A", trace, false), new Probe("B", trace, true)});
        check(ordered.equals("<A|null>"), "null toString conversion");
        check(trace.toString().equals("AB"), "left-to-right conversion");
        check(concat(new String[]{"x", "y"}, new Object[]{null}).equals("xnully"),
                "null argument conversion");

        String empty1 = concat(new String[]{""}, new Object[0]);
        String empty2 = concat(new String[]{""}, new Object[0]);
        check(empty1.isEmpty(), "empty content");
        check(empty1 != empty2, "fresh empty wrapper");

        int count = 512;
        String[] constants = new String[count + 1];
        Object[] values = new Object[count];
        for (int i = 0; i < count; i++) {
            constants[i] = i == 0 ? "" : "|";
            values[i] = "x";
        }
        constants[count] = "";
        String many = concat(constants, values);
        check(many.length() == count * 2 - 1, "many-piece length");
        Object body = STRING_M3.get(many);
        check(body != null, "many-piece M3 body");
        Object owner = M3_OWNER.get(body);
        check(owner.getClass().getName().equals("java.lang.M3StringTuple"), "tuple owner");
        int height = TUPLE_HEIGHT.getInt(owner);
        check(height <= 16, "balanced tuple height=" + height);

        System.out.println("M3_STRING_CONCAT_REFERENCE_DAG_PASS|checks=" + checks
                + "|height=" + height);
    }

    private static String concat(String[] constants, Object[] args) throws Exception {
        return (String) CONCAT.invoke(null, constants, args);
    }

    private record Probe(String value, StringBuilder trace, boolean returnNull) {
        @Override public String toString() {
            trace.append(value);
            return returnNull ? null : value;
        }
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
