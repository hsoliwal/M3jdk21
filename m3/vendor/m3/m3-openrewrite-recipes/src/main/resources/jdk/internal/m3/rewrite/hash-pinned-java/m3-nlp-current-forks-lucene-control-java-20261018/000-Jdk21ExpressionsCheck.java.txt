// SPDX-License-Identifier: Apache-2.0
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.text.ParseException;
import java.util.Map;
import org.apache.lucene.expressions.Expression;
import org.apache.lucene.expressions.js.JavascriptCompiler;
import org.apache.lucene.search.DoubleValues;

/** Exercises the actual current expression compiler after the package/API backport. */
public final class Jdk21ExpressionsCheck {
  public static double twice(double value) { return value * 2; }

  public static void main(String[] args) throws Exception {
    check(JavascriptCompiler.compile("sqrt(9) + 2 * 4").evaluate(new DoubleValues[0]) == 11d,
        "arithmetic or generated class version");
    var handle = MethodHandles.lookup().findStatic(
        Jdk21ExpressionsCheck.class, "twice", MethodType.methodType(double.class, double.class));
    check(JavascriptCompiler.compile("twice(5) + 1", Map.of("twice", handle))
        .evaluate(new DoubleValues[0]) == 11d, "custom MethodHandle linkage");
    Expression variables = JavascriptCompiler.compile("alpha * 2 + beta");
    check(java.util.Arrays.equals(variables.variables, new String[] {"alpha", "beta"}),
        "variable encounter order");
    check(variables.evaluate(new DoubleValues[] {constant(4), constant(3)}) == 11d,
        "external values");
    check(Double.isInfinite(JavascriptCompiler.compile("1 / 0").evaluate(new DoubleValues[0])),
        "JavaScript double division");
    try {
      JavascriptCompiler.compile("1 +");
      throw new AssertionError("malformed expression was accepted");
    } catch (ParseException expected) {
      check(expected.getErrorOffset() >= 0, "parse error offset");
    }
    var invalid = MethodHandles.lookup().findStatic(
        Integer.class, "parseInt", MethodType.methodType(int.class, String.class));
    try {
      JavascriptCompiler.compile("bad(1)", Map.of("bad", invalid));
      throw new AssertionError("invalid custom function was accepted");
    } catch (IllegalArgumentException expected) {
      check(expected.getMessage() != null, "invalid function diagnostic");
    }
    System.out.println("Current Lucene ClassFile compiler: arithmetic, variables, functions and errors passed");
  }

  private static DoubleValues constant(double value) {
    return new DoubleValues() {
      public double doubleValue() { return value; }
      public boolean advanceExact(int doc) { return true; }
    };
  }

  private static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }
}

