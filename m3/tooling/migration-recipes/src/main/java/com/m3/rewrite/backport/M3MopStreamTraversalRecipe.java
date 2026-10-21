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

/**
 * Recipe-owned additive stream/DOOP/delta outputs for M3JDK21.
 *
 * <p>All targets are new files. Existing targets are accepted only when their exact printed source
 * equals the reviewed recipe resource, so later drift must be corrected in the recipe rather than
 * patched in generated output.</p>
 */
public final class M3MopStreamTraversalRecipe
    extends ScanningRecipe<M3MopStreamTraversalRecipe.Inventory> {
  private static final String ROOT =
      "/com/m3/rewrite/backport/mop-stream-traversal-20261021/";

  private record Target(String path, String resource) {}

  public static final class Inventory {
    private final Map<String, SourceFile> seen = new HashMap<>();
    private final List<String> conflicts = new ArrayList<>();
    private Map<String, SourceFile> generated;
  }

  @Override
  public String getDisplayName() {
    return "M3 MOP monitored lazy stream traversal";
  }

  @Override
  public String getDescription() {
    return "Generates monitored one-pass file admission, lazy filesystem traversal, and retained-fact delta projection.";
  }

  @Override
  public Set<String> getTags() {
    return Set.of("m3", "mop", "recipe-first", "stream", "doop", "delta", "file");
  }

  @Override
  public int maxCycles() {
    return 1;
  }

  @Override
  public Inventory getInitialValue(ExecutionContext context) {
    return new Inventory();
  }

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
            if (!(file instanceof J.CompilationUnit)
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
  public Collection<? extends SourceFile> generate(
      Inventory inventory, ExecutionContext context) {
    prepare(inventory, context);
    List<SourceFile> additions = new ArrayList<>();
    for (Target target : targets()) {
      if (!inventory.seen.containsKey(target.path())) {
        additions.add(inventory.generated.get(target.path()));
      }
    }
    return additions;
  }

  @Override
  public TreeVisitor<?, ExecutionContext> getVisitor(Inventory inventory) {
    requireClean(inventory);
    return TreeVisitor.noop();
  }

  private static void prepare(Inventory inventory, ExecutionContext context) {
    synchronized (inventory) {
      requireClean(inventory);
      if (inventory.generated != null) return;
      Map<String, SourceFile> parsed = new HashMap<>();
      for (Target target : targets()) {
        List<SourceFile> files =
            JavaParser.fromJavaVersion()
                .build()
                .parseInputs(
                    List.of(
                        Parser.Input.fromString(
                            Path.of(target.path()), resource(target.resource()))),
                    null,
                    context)
                .toList();
        if (files.size() != 1
            || !(files.getFirst() instanceof J.CompilationUnit)
            || !files.getFirst().printAll().equals(resource(target.resource()))) {
          throw new IllegalStateException("M3 MOP stream recipe Java roundtrip drift");
        }
        parsed.put(target.path(), files.getFirst());
      }
      inventory.generated = Map.copyOf(parsed);
    }
  }

  private static void requireClean(Inventory inventory) {
    synchronized (inventory) {
      if (!inventory.conflicts.isEmpty()) {
        throw new IllegalStateException(String.join("; ", inventory.conflicts));
      }
    }
  }

  private static List<Target> targets() {
    return List.of(
        new Target(
            "m3/ports/file/src/main/java/com/m3/file/M3FileDelta.java",
            "M3FileDelta.java.txt"),
        new Target(
            "m3/ports/file/src/main/java/com/m3/file/M3FileStreamTasks.java",
            "M3FileStreamTasks.java.txt"),
        new Target(
            "m3/ports/file/src/main/java/com/m3/file/M3FileUniverseTraversal.java",
            "M3FileUniverseTraversal.java.txt"));
  }

  private static String resource(String name) {
    try (var stream = M3MopStreamTraversalRecipe.class.getResourceAsStream(ROOT + name)) {
      if (stream == null) throw new IllegalStateException("missing M3 MOP recipe resource " + name);
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException failure) {
      throw new IllegalStateException("cannot read M3 MOP recipe resource " + name, failure);
    }
  }
}
