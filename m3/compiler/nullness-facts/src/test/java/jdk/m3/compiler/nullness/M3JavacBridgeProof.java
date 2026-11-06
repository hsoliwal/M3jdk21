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

/** Real javac 21 attributed-tree proof against hostile and benign guards. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/982689e16763e54b01632407d64846fbea5719b6/synexia-m3-recipe/crates/m3-java-nullness-flow-20261031/qualification/JavacBridgeProof.java blob 8ea7b341ae48; recipe m3/compiler/nullness-facts/recipe/relocate.py v1
public final class M3JavacBridgeProof {
  private static int checks;

  private static final String JAVA = """
      class M3NullnessCorpus {
        Object field;
        boolean mutate() { field = null; return true; }
        void sample(Object a, Object b) {
          if (a != null) { }                          // 0
          if (a == null) { }                          // 1
          if (null == a) { }                          // 2
          if (!(a == null)) { }                       // 3
          if ((a != null)) { }                        // 4
          if (a != null && b != null) { }             // 5
          if (a == null || b == null) { }             // 6
          if (a instanceof String) { }                // 7
          if (a != null && field != null) { }         // 8: field access decoy
          if (a != null && mutate()) { }              // 9: callback decoy
          if (a != null && (a = null) == null) { }    // 10: assignment decoy
          if (field != null) { }                      // 11: field decoy
          if (a == b) { }                             // 12: no null literal
          if (a instanceof String s && s.length() > 0) { } // 13
          if (a != null && a != null) { }             // 14
          if (true) { }                               // 15
          if (false) { }                              // 16
          if (a != null && false) { }                 // 17
          if (a != null || a == null) { }             // 18: tautology
          if ((a != null && b != null) || a == null) { } // 19
          if (a != null && b != null && a != null) { } // 20
          String adversarial = "if(a != null) // fake guard";
          String text = \"\"\"
            // if (a == null) { deleteAll(); }
            if (b != null) { }
            \"\"\";
          if (adversarial.equals(text)) { }           // 21: method-call decoy
        }
      }
      """;

  private M3JavacBridgeProof() {}

  public static void main(String[] args) throws Exception {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    require(compiler != null, "JDK javac compiler required");
    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    JavaFileObject source = new SimpleJavaFileObject(
        URI.create("string:///M3NullnessCorpus.java"), JavaFileObject.Kind.SOURCE) {
      @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return JAVA;
      }
    };
    JavacTask task = (JavacTask) compiler.getTask(null, null, diagnostics,
        List.of("--release", "21", "-Xlint:all", "-Werror", "-proc:none"),
        null, List.of(source));
    List<CompilationUnitTree> units = new ArrayList<>();
    task.parse().forEach(units::add);
    task.analyze();
    for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
      if (d.getKind() == Diagnostic.Kind.ERROR) {
        throw new AssertionError("javac error: " + d);
      }
    }
    require(units.size() == 1, "one compilation unit");
    Trees trees = Trees.instance(task);
    var epoch = new M3JavaNullnessFlow.Epoch("0".repeat(64), "1".repeat(64),
        "2".repeat(64), 5);
    var before = M3JavaNullnessFlow.start(epoch, 2);
    int[] expectedTrueA = {2,1,1,2,2,2,3,2,3,3,3,3,3,3,2,3,0,0,3,3,2,3};
    int[] expectedFalseA = {1,2,2,1,1,3,2,3,3,3,3,3,3,3,1,0,3,3,0,2,3,3};
    int[] expectedTrueB = {3,3,3,3,3,2,3,3,3,3,3,3,3,3,3,3,0,0,3,3,2,3};
    int[] expectedFalseB = {3,3,3,3,3,3,2,3,3,3,3,3,3,3,3,0,3,3,0,3,3,3};
    int[] number = {0};
    new TreePathScanner<Void, Void>() {
      private Map<Element,Integer> slot = Map.of();
      @Override public Void visitMethod(MethodTree method, Void unused) {
        if (method.getName().contentEquals("sample")) {
          Map<Element,Integer> mutable = new HashMap<>();
          for (int i = 0; i < method.getParameters().size(); i++) {
            VariableTree parameter = method.getParameters().get(i);
            Element element = trees.getElement(new TreePath(getCurrentPath(), parameter));
            require(element != null, "attributed parameter");
            mutable.put(element, i);
          }
          slot = Map.copyOf(mutable);
          super.visitMethod(method, unused);
          slot = Map.of();
          return null;
        }
        return super.visitMethod(method, unused);
      }
      @Override public Void visitIf(IfTree condition, Void unused) {
        int index = number[0]++;
        if (index >= expectedTrueA.length) throw new AssertionError("unexpected AST if");
        TreePath path = new TreePath(getCurrentPath(), condition.getCondition());
        var yes = M3JavaNullnessJavacBridge.refine(before, path, true, trees,
            el -> slot.getOrDefault(el, -1));
        var no = M3JavaNullnessJavacBridge.refine(before, path, false, trees,
            el -> slot.getOrDefault(el, -1));
        check(yes, 0, expectedTrueA[index], "true a case " + index);
        check(no, 0, expectedFalseA[index], "false a case " + index);
        check(yes, 1, expectedTrueB[index], "true b case " + index);
        check(no, 1, expectedFalseB[index], "false b case " + index);
        require(!yes.automaticMutationAuthority(), "no source mutation authority");
        return super.visitIf(condition, unused);
      }
    }.scan(units.getFirst(), null);
    require(number[0] == expectedTrueA.length, "all attributed guards visited");
    System.out.println("M3_JAVA_NULLNESS_JAVAC_BRIDGE_PASS cases=" + number[0]
        + " checks=" + checks);
  }

  private static void check(M3JavaNullnessFlow.Facts facts, int slot,
      int expected, String message) {
    int actual = switch (facts.nullness(slot)) {
      case UNREACHABLE -> 0;
      case NULL -> 1;
      case NON_NULL -> 2;
      case MAYBE_NULL -> 3;
    };
    require(expected == actual, message + " expected=" + expected + " actual=" + actual);
  }

  private static void require(boolean ok, String message) {
    checks++;
    if (!ok) throw new AssertionError(message);
  }
}
