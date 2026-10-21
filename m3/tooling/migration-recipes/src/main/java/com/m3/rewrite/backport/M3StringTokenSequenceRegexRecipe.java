// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.text.PlainText;

/** Exact additive recipe for the M3String token-sequence regex Java/JNI lane. */
public final class M3StringTokenSequenceRegexRecipe
    extends ScanningRecipe<M3StringTokenSequenceRegexRecipe.Inventory> {
  private static final String ROOT =
      "/com/m3/rewrite/backport/m3string-token-sequence-regex-20261021/";

  private record Target(String path, String resource, boolean java) {}

  public static final class Inventory {
    private final Map<String, SourceFile> seen = new HashMap<>();
    private final List<String> conflicts = new ArrayList<>();
    private Map<String, SourceFile> generated;
  }

  @Override public String getDisplayName() { return "M3String token-sequence regex"; }
  @Override public String getDescription() {
    return "Generates the package-private token-ID/range regex lane, JNI candidate kernel, and jtreg parity proof.";
  }
  @Override public Set<String> getTags() {
    return Set.of("m3", "m3string", "regex", "token-sequence", "jni", "recipe-first");
  }
  @Override public int maxCycles() { return 1; }
  @Override public Inventory getInitialValue(ExecutionContext context) { return new Inventory(); }

  @Override
  public TreeVisitor<?, ExecutionContext> getScanner(Inventory inventory) {
    return new TreeVisitor<Tree, ExecutionContext>() {
      @Override
      public Tree preVisit(Tree tree, ExecutionContext context) {
        if (!(tree instanceof SourceFile file)) return tree;
        stopAfterPreVisit();
        String path = file.getSourcePath().normalize().toString().replace('\\', '/');
        for (Target target : targets()) {
          if (!target.path().equals(path)) continue;
          synchronized (inventory) {
            if (inventory.seen.putIfAbsent(path, file) != null) {
              inventory.conflicts.add("duplicate target " + path);
            }
            if ((target.java() && !(file instanceof J.CompilationUnit))
                || (!target.java() && !(file instanceof PlainText))
                || !file.printAll().equals(resource(target.resource()))) {
              inventory.conflicts.add("generated target drift " + path);
            }
          }
        }
        return tree;
      }
    };
  }

  @Override
  public Collection<? extends SourceFile> generate(Inventory inventory, ExecutionContext context) {
    prepare(inventory, context);
    List<SourceFile> additions = new ArrayList<>();
    for (Target target : targets()) {
      if (!inventory.seen.containsKey(target.path())) additions.add(inventory.generated.get(target.path()));
    }
    return additions;
  }

  @Override public TreeVisitor<?, ExecutionContext> getVisitor(Inventory inventory) {
    requireClean(inventory);
    return TreeVisitor.noop();
  }

  private static void prepare(Inventory inventory, ExecutionContext context) {
    synchronized (inventory) {
      requireClean(inventory);
      if (inventory.generated != null) return;
      Map<String, SourceFile> result = new HashMap<>();
      for (Target target : targets()) {
        String text = resource(target.resource());
        SourceFile file;
        if (target.java()) {
          List<SourceFile> parsed =
              JavaParser.fromJavaVersion().build().parseInputs(
                  List.of(Parser.Input.fromString(Path.of(target.path()), text)), null, context).toList();
          if (parsed.size() != 1 || !(parsed.getFirst() instanceof J.CompilationUnit)
              || !parsed.getFirst().printAll().equals(text)) {
            throw new IllegalStateException("token regex Java roundtrip drift " + target.path());
          }
          file = parsed.getFirst();
        } else {
          file = PlainText.builder().sourcePath(Path.of(target.path())).text(text).build();
        }
        result.put(target.path(), file);
      }
      inventory.generated = Map.copyOf(result);
    }
  }

  private static void requireClean(Inventory inventory) {
    synchronized (inventory) {
      if (!inventory.conflicts.isEmpty()) throw new IllegalStateException(String.join("; ", inventory.conflicts));
    }
  }

  private static List<Target> targets() {
    return List.of(
        new Target("src/java.base/share/classes/java/lang/M3StringTokenRegex.java",
            "M3StringTokenRegex.java.txt", true),
        new Target("src/java.base/share/native/libjava/M3StringTokenRegex.c",
            "M3StringTokenRegex.c.txt", false),
        new Target("test/jdk/java/lang/String/M3StringTokenRegexTest.java",
            "M3StringTokenRegexTest.java.txt", true));
  }

  private static String resource(String name) {
    try (var stream = M3StringTokenSequenceRegexRecipe.class.getResourceAsStream(ROOT + name)) {
      if (stream == null) throw new IllegalStateException("missing M3 token regex resource " + name);
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException failure) {
      throw new IllegalStateException("cannot read M3 token regex resource " + name, failure);
    }
  }
}
