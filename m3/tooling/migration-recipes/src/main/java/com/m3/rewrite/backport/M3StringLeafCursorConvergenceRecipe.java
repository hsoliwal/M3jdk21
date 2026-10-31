// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Modified 2026-10-21: preserve current native-join and copy-path owners during shared cursor convergence.
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
 * Hash-pinned extraction of the duplicated M3String tuple-leaf walker.
 *
 * <p>The recipe owns exact pre/postimages for the two changed owners and the new shared cursor.
 * It refuses unknown source before replacement, so correction belongs here rather than in product
 * files.</p>
 */
public final class M3StringLeafCursorConvergenceRecipe
    extends ScanningRecipe<M3StringLeafCursorConvergenceRecipe.Inventory> {
  private static final String ROOT =
      "/com/m3/rewrite/backport/m3string-leaf-cursor-20261021/";

  private record Target(String path, String before, String previousAfter, String after, boolean required) {}

  public static final class Inventory {
    final Map<String, SourceFile> seen = new HashMap<>();
    final List<String> conflicts = new ArrayList<>();
    Map<String, SourceFile> parsedAfter;
  }

  @Override public String getDisplayName() { return "Converge M3String leaf cursor"; }
  @Override public String getDescription() {
    return "Extracts one shared owner/range cursor with explicit coalescing policy for pool and token regex.";
  }
  @Override public Set<String> getTags() {
    return Set.of("m3", "m3string", "atomize", "patternize", "recipe-first", "cursor");
  }
  @Override public int maxCycles() { return 1; }
  @Override public Inventory getInitialValue(ExecutionContext ctx) { return new Inventory(); }

  @Override
  public TreeVisitor<?, ExecutionContext> getScanner(Inventory inventory) {
    return new TreeVisitor<Tree, ExecutionContext>() {
      @Override public Tree preVisit(Tree tree, ExecutionContext ctx) {
        if (!(tree instanceof SourceFile file)) return tree;
        stopAfterPreVisit();
        String path = file.getSourcePath().normalize().toString().replace('\\', '/');
        for (Target target : targets()) {
          if (!target.path().equals(path)) continue;
          synchronized (inventory) {
            if (inventory.seen.putIfAbsent(path, file) != null) {
              inventory.conflicts.add("duplicate target " + path);
              return tree;
            }
            String blob = gitBlobSha1(file.printAll());
            if (!blob.equals(target.before()) && !blob.equals(target.previousAfter()) && !blob.equals(target.after())) {
              inventory.conflicts.add(
                  "preimage drift " + path + " actual=" + gitBlobSha1(file.printAll()));
            }
          }
        }
        return tree;
      }
    };
  }

  @Override
  public Collection<? extends SourceFile> generate(Inventory inventory, ExecutionContext ctx) {
    prepare(inventory, ctx);
    List<SourceFile> additions = new ArrayList<>();
    for (Target target : targets()) {
      if (!target.required() && !inventory.seen.containsKey(target.path())) {
        additions.add(inventory.parsedAfter.get(target.path()));
      }
    }
    return additions;
  }

  @Override
  public TreeVisitor<?, ExecutionContext> getVisitor(Inventory inventory) {
    prepare(inventory, new org.openrewrite.InMemoryExecutionContext(Throwable::printStackTrace));
    return new TreeVisitor<Tree, ExecutionContext>() {
      @Override public Tree preVisit(Tree tree, ExecutionContext ctx) {
        if (!(tree instanceof SourceFile file)) return tree;
        stopAfterPreVisit();
        String path = file.getSourcePath().normalize().toString().replace('\\', '/');
        for (Target target : targets()) {
          if (target.path().equals(path)) {
            if (gitBlobSha1(file.printAll()).equals(target.after())) return file;
            return inventory.parsedAfter.get(path);
          }
        }
        return tree;
      }
    };
  }

  private static void prepare(Inventory inventory, ExecutionContext ctx) {
    synchronized (inventory) {
      if (!inventory.conflicts.isEmpty()) {
        throw new IllegalStateException(String.join("; ", inventory.conflicts));
      }
      if (inventory.parsedAfter != null) return;
      Map<String, SourceFile> parsed = new HashMap<>();
      for (Target target : targets()) {
        String text = resource(target.path().endsWith("M3StringPool.java")
            ? "M3StringPool.after.java.txt"
            : target.path().endsWith("M3StringTokenRegex.java")
                ? "M3StringTokenRegex.after.java.txt"
                : "M3StringLeafCursor.java.txt");
        List<SourceFile> files =
            JavaParser.fromJavaVersion().build().parseInputs(
                List.of(Parser.Input.fromString(Path.of(target.path()), text)), null, ctx).toList();
        if (files.size() != 1 || !(files.getFirst() instanceof J.CompilationUnit)
            || !files.getFirst().printAll().equals(text)
            || !gitBlobSha1(text).equals(target.after())) {
          throw new IllegalStateException("M3 leaf cursor postimage drift " + target.path());
        }
        parsed.put(target.path(), files.getFirst());
      }
      inventory.parsedAfter = Map.copyOf(parsed);
    }
  }

  private static List<Target> targets() {
    return List.of(
        new Target(
            "src/java.base/share/classes/java/lang/M3StringPool.java",
            "394af2f062e9d99abf73e5a6f15df015a59b8072",
            "15231ee5a2c48153550c700f98722e15c0f4451c",
            "7c05d6ab0082eb26f39e4e6dc93c422237588cae",
            true),
        new Target(
            "src/java.base/share/classes/java/lang/M3StringTokenRegex.java",
            "c06dc1366063d717cc998dae5593d4bf7997b6b6",
            "",
            "ad87db7ed0f7e0490b46245782ec64185fb457d7",
            true),
        new Target(
            "src/java.base/share/classes/java/lang/M3StringLeafCursor.java",
            "",
            "1851d3ce7cd6de9f0a9d9b84ef83999b43de64f4",
            "5ac0eee6e2a1eaf3e24c387c02bcff4e66eb2c0a",
            false));
  }

  private static String resource(String name) {
    try (var stream = M3StringLeafCursorConvergenceRecipe.class.getResourceAsStream(ROOT + name)) {
      if (stream == null) throw new IllegalStateException("missing M3 leaf cursor resource " + name);
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException failure) {
      throw new IllegalStateException("cannot read M3 leaf cursor resource " + name, failure);
    }
  }

  /** Git blob identity, matching repository source receipts rather than a parallel digest domain. */
  private static String gitBlobSha1(String text) {
    try {
      byte[] payload = text.getBytes(StandardCharsets.UTF_8);
      byte[] header = ("blob " + payload.length + "\0").getBytes(StandardCharsets.US_ASCII);
      java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-1");
      digest.update(header);
      digest.update(payload);
      return java.util.HexFormat.of().formatHex(digest.digest());
    } catch (java.security.NoSuchAlgorithmException impossible) {
      throw new ExceptionInInitializerError(impossible);
    }
  }
}
