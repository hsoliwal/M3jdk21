// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.compiler.nullness;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

/** Exhaustive generated boolean condition corpus, evaluated against Java semantics. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/982689e16763e54b01632407d64846fbea5719b6/synexia-m3-recipe/crates/m3-java-nullness-flow-20261031/qualification/JavacGuardMatrixProof.java blob c03e73fe6299; recipe m3/compiler/nullness-facts/recipe/relocate.py v1
public final class M3JavacGuardMatrixProof {
  @FunctionalInterface private interface Oracle {
    boolean eval(Object a, Object b);
  }
  private record Guard(String source, Oracle oracle) {}
  private static int checks;
  private M3JavacGuardMatrixProof() {}

  public static void main(String[] args) throws Exception {
    List<Guard> leaves = List.of(
        new Guard("a == null", (a,b) -> a == null),
        new Guard("a != null", (a,b) -> a != null),
        new Guard("null == a", (a,b) -> a == null),
        new Guard("b == null", (a,b) -> b == null),
        new Guard("b != null", (a,b) -> b != null),
        new Guard("null != b", (a,b) -> b != null),
        new Guard("a instanceof String", (a,b) -> a instanceof String),
        new Guard("b instanceof String", (a,b) -> b instanceof String),
        new Guard("true", (a,b) -> true),
        new Guard("false", (a,b) -> false));
    List<Guard> guards = new ArrayList<>(leaves);
    for (Guard g : leaves) {
      guards.add(new Guard("!(" + g.source + ")", (a,b) -> !g.oracle.eval(a,b)));
    }
    for (Guard a : leaves) for (Guard b : leaves) {
      guards.add(new Guard("(" + a.source + ") && (" + b.source + ")",
          (x,y) -> a.oracle.eval(x,y) && b.oracle.eval(x,y)));
      guards.add(new Guard("(" + a.source + ") || (" + b.source + ")",
          (x,y) -> a.oracle.eval(x,y) || b.oracle.eval(x,y)));
    }
    // Pathological but parseable guards are conservative no-ops at the edge.
    String medium = String.join(" && ", java.util.Collections.nCopies(11, "a != null"));
    String longGuard = String.join(" && ", java.util.Collections.nCopies(100, "a != null"));
    guards.add(new Guard(medium, (a,b) -> a != null));
    guards.add(new Guard(longGuard, (a,b) -> a != null));
    StringBuilder generated = new StringBuilder("class GeneratedNullGuards { void sample(Object a,Object b) {\n");
    for (Guard guard : guards) generated.append("if (").append(guard.source)
        .append(") { }\n");
    generated.append("} }");
    JavaFileObject source = new SimpleJavaFileObject(
        URI.create("string:///GeneratedNullGuards.java"), JavaFileObject.Kind.SOURCE) {
      @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return generated;
      }
    };
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    require(compiler != null, "JDK javac missing");
    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    JavacTask task = (JavacTask) compiler.getTask(null, null, diagnostics,
        List.of("--release", "21", "-Xlint:all", "-Werror", "-proc:none"),
        null, List.of(source));
    List<CompilationUnitTree> units = new ArrayList<>();
    task.parse().forEach(units::add);
    task.analyze();
    for (var diagnostic : diagnostics.getDiagnostics()) {
      if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
        throw new AssertionError("javac rejected generated pure guard: " + diagnostic);
      }
    }
    require(units.size() == 1, "one generated unit");
    Trees trees = Trees.instance(task);
    var epoch = new M3JavaNullnessFlow.Epoch("a".repeat(64), "b".repeat(64),
        "c".repeat(64), 17);
    var before = M3JavaNullnessFlow.start(epoch, 2);
    Object[] values = {null, "string", new Object()};
    int[] index = {0};
    new TreePathScanner<Void, Void>() {
      private Map<Element,Integer> symbols = Map.of();
      @Override public Void visitMethod(MethodTree method, Void arg) {
        if (method.getName().contentEquals("sample")) {
          Map<Element,Integer> resolved = new HashMap<>();
          for (int i = 0; i < method.getParameters().size(); i++) {
            VariableTree parameter = method.getParameters().get(i);
            Element el = trees.getElement(new TreePath(getCurrentPath(), parameter));
            require(el != null, "attributed parameter");
            resolved.put(el, i);
          }
          symbols = Map.copyOf(resolved);
          super.visitMethod(method, arg);
          symbols = Map.of();
          return null;
        }
        return super.visitMethod(method, arg);
      }
      @Override public Void visitIf(IfTree statement, Void arg) {
        if (index[0] >= guards.size()) throw new AssertionError("excess if condition");
        Guard guard = guards.get(index[0]++);
        TreePath condition = new TreePath(getCurrentPath(), statement.getCondition());
        var yes = M3JavaNullnessJavacBridge.refine(before, condition, true,
            trees, el -> symbols.getOrDefault(el, -1));
        var no = M3JavaNullnessJavacBridge.refine(before, condition, false,
            trees, el -> symbols.getOrDefault(el, -1));
        if (index[0] == guards.size()) {
          require(yes == before && no == before, "pathological guard must fail closed");
        }
        if (index[0] == guards.size() - 1) {
          var large = M3JavaNullnessFlow.start(epoch, 1_000_000);
          require(M3JavaNullnessJavacBridge.refine(large, condition, true, trees,
              el -> symbols.getOrDefault(el, -1)) == large,
              "large fact arrays must reject expensive branch enumeration");
        }
        for (Object a : values) for (Object b : values) {
          boolean result = guard.oracle.eval(a,b);
          var facts = result ? yes : no;
          require(facts.reachable(), "feasible branch rejected: " + guard.source);
          valid(facts.nullness(0), a, guard.source);
          valid(facts.nullness(1), b, guard.source);
        }
        return super.visitIf(statement, arg);
      }
    }.scan(units.getFirst(), null);
    require(index[0] == guards.size(), "every generated condition visited");
    System.out.println("M3_JAVAC_GUARD_MATRIX_PASS guards=" + index[0]
        + " checks=" + checks);
  }

  private static void valid(M3JavaNullnessFlow.Nullness fact, Object actual,
      String guard) {
    require(fact == M3JavaNullnessFlow.Nullness.MAYBE_NULL
        || (actual == null && fact == M3JavaNullnessFlow.Nullness.NULL)
        || (actual != null && fact == M3JavaNullnessFlow.Nullness.NON_NULL),
        "unsound flow fact for " + guard + " actual=" + actual + " fact=" + fact);
  }
  private static void require(boolean value, String message) {
    checks++;
    if (!value) throw new AssertionError(message);
  }
}
