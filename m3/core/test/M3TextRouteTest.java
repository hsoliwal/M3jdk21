import com.m3.text.M3Text;
import com.m3.text.M3TextCompilerRuntime;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.regex.Pattern;

public final class M3TextRouteTest {
    private static int checks;

    private static void check(boolean value) {
        checks++;
        if (!value) throw new AssertionError("check " + checks);
    }

    private static void eq(Object actual, Object expected) {
        checks++;
        if (!java.util.Objects.equals(actual, expected)) {
            throw new AssertionError("expected=" + expected + " actual=" + actual);
        }
    }

    private static void scalarAndCanonicalization() {
        String source = new String(new char[]{'a','\u0000','\ud83d','\ude00','\ud800'});
        M3Text one = M3Text.fromString(source);
        M3Text two = M3Text.fromString(new String(source));
        check(one == two);
        eq(one.asString(), source);
        eq(one.hashCode(), source.hashCode());
        char[] escaped = one.toCharArray();
        escaped[0] = 'z';
        eq(one.charAt(0), 'a');
        check(Arrays.equals(
                one.getBytes(StandardCharsets.UTF_8),
                source.getBytes(StandardCharsets.UTF_8)));
    }

    private static void concatSliceRepeatAndRegex() {
        M3Text left = M3Text.fromString("A\ud83d");
        M3Text right = M3Text.fromString("\ude00B");
        M3Text joined = left.concat(right);
        String oracle = "A\ud83d\ude00B";
        eq(joined.asString(), oracle);
        eq(joined.hashCode(), oracle.hashCode());
        check(joined == left.concat(right));
        for (int i = 0; i <= oracle.length(); i++) {
            for (int j = i; j <= oracle.length(); j++) {
                M3Text slice = joined.substring(i, j);
                eq(slice.asString(), oracle.substring(i, j));
                eq(slice.hashCode(), oracle.substring(i, j).hashCode());
            }
        }
        eq(joined.repeat(7).asString(), oracle.repeat(7));
        eq(joined.indexOf("\ud83d\ude00"), oracle.indexOf("\ud83d\ude00"));
        eq(joined.lastIndexOf("B"), oracle.lastIndexOf("B"));
        check(joined.startsWith("A\ud83d"));
        check(joined.endsWith("\ude00B"));
        check(Pattern.compile("A(?=\\x{1F600})\\x{1F600}B").matcher(joined).matches());
    }

    private static void compilerBoundary() {
        StringBuilder trace = new StringBuilder();
        Object first = new Object() {
            @Override public String toString() {
                trace.append('1');
                return "x";
            }
        };
        Object second = new Object() {
            @Override public String toString() {
                trace.append('2');
                return "y";
            }
        };
        M3Text result = M3TextCompilerRuntime.concatObjects(first, null, second);
        eq(trace.toString(), "12");
        eq(result.asString(), "xnully");
        eq(M3TextCompilerRuntime.concatObjectsToString("a", 1, true), "a1true");
    }

    public static void main(String[] args) {
        scalarAndCanonicalization();
        concatSliceRepeatAndRegex();
        compilerBoundary();
        System.out.println("M3_TEXT_ROUTE_PASS checks=" + checks);
    }
}
