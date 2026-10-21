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

/** Hash-pinned additive fact extraction for immutable M3File byte events. */
public final class M3FileByteEventFactRecipe
    extends ScanningRecipe<M3FileByteEventFactRecipe.Inventory> {
  private static final String ROOT =
      "/com/m3/rewrite/backport/m3file-byte-event-fact-20261021/";

  private record Target(String path, String before, String after, String resource) {}

  public static final class Inventory {
    final Map<String, SourceFile> seen = new HashMap<>();
    final List<String> conflicts = new ArrayList<>();
    Map<String, SourceFile> after;
  }

  @Override public String getDisplayName() { return "M3File byte event fact"; }
  @Override public String getDescription() {
    return "Adds a non-copying byteCount fact while preserving defensive payload access.";
  }
  @Override public Set<String> getTags() {
    return Set.of("m3", "m3file", "stream", "allocation", "recipe-first");
  }
  @Override public int maxCycles() { return 1; }
  @Override public Inventory getInitialValue(ExecutionContext context) { return new Inventory(); }

  @Override
  public TreeVisitor<?, ExecutionContext> getScanner(Inventory inventory) {
    return new TreeVisitor<Tree, ExecutionContext>() {
      @Override public Tree preVisit(Tree tree, ExecutionContext context) {
        if (!(tree instanceof SourceFile file)) return tree;
        stopAfterPreVisit();
        String path = file.getSourcePath().normalize().toString().replace('\\', '/');
        for (Target target : targets()) {
          if (!target.path().equals(path)) continue;
          synchronized (inventory) {
            if (inventory.seen.putIfAbsent(path, file) != null) {
              inventory.conflicts.add("duplicate target " + path);
            } else if (!gitBlobSha1(file.printAll()).equals(target.before())) {
              inventory.conflicts.add("preimage drift " + path);
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
    return List.of();
  }

  @Override
  public TreeVisitor<?, ExecutionContext> getVisitor(Inventory inventory) {
    return new TreeVisitor<Tree, ExecutionContext>() {
      @Override public Tree preVisit(Tree tree, ExecutionContext context) {
        if (!(tree instanceof SourceFile file)) return tree;
        stopAfterPreVisit();
        prepare(inventory, context);
        String path = file.getSourcePath().normalize().toString().replace('\\', '/');
        SourceFile replacement = inventory.after.get(path);
        return replacement == null ? tree : replacement;
      }
    };
  }

  private static void prepare(Inventory inventory, ExecutionContext context) {
    synchronized (inventory) {
      if (!inventory.conflicts.isEmpty()) {
        throw new IllegalStateException(String.join("; ", inventory.conflicts));
      }
      if (inventory.after != null) return;
      Map<String, SourceFile> parsed = new HashMap<>();
      for (Target target : targets()) {
        String text = resource(target.resource());
        List<SourceFile> files =
            JavaParser.fromJavaVersion().build().parseInputs(
                List.of(Parser.Input.fromString(Path.of(target.path()), text)), null, context).toList();
        if (files.size() != 1
            || !(files.getFirst() instanceof J.CompilationUnit)
            || !files.getFirst().printAll().equals(text)
            || !gitBlobSha1(text).equals(target.after())) {
          throw new IllegalStateException("postimage drift " + target.path());
        }
        parsed.put(target.path(), files.getFirst());
      }
      inventory.after = Map.copyOf(parsed);
    }
  }

  private static List<Target> targets() {
    return List.of(
        new Target(
            "m3/ports/file/src/main/java/com/m3/file/M3FileStreamEvents.java",
            "a8a9e03383b6c1e44c2c7ce3cc33f4e923d5310e",
            "68999f0e8a2596ca5745b9c12db04ca3c164f0d4",
            "M3FileStreamEvents.after.java.txt"),
        new Target(
            "m3/ports/file/src/main/java/com/m3/file/M3FileStreamTasks.java",
            "d8d2ebb7f59ef7ecafea2eed5259419869d86747",
            "f56e33f66a04430b192d840c7f7ad591e2a6aacc",
            "M3FileStreamTasks.after.java.txt"));
  }

  private static String resource(String name) {
    try (var stream = M3FileByteEventFactRecipe.class.getResourceAsStream(ROOT + name)) {
      if (stream == null) throw new IllegalStateException("missing M3File fact resource " + name);
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException failure) {
      throw new IllegalStateException("cannot read M3File fact resource " + name, failure);
    }
  }

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
