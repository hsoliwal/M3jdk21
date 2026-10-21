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
import org.openrewrite.text.PlainText;

/** Recipe-owned promotion of append-only recipe learning into the canonical ownership gate. */
public final class M3RecipeMemoryPolicyRecipe
    extends ScanningRecipe<M3RecipeMemoryPolicyRecipe.Inventory> {
  private static final String ROOT =
      "/com/m3/rewrite/backport/mop-recipe-memory-policy-20261021/";

  private record Target(String path, String before, String after, String resource, boolean create) {}

  public static final class Inventory {
    final Map<String, SourceFile> seen = new HashMap<>();
    final List<String> conflicts = new ArrayList<>();
    Map<String, SourceFile> after;
  }

  @Override public String getDisplayName() { return "M3 recipe learning memory policy"; }

  @Override public String getDescription() {
    return "Makes append-only recipe correction memory and source-change recipe ownership a canonical PR gate.";
  }

  @Override public Set<String> getTags() {
    return Set.of("m3", "mop", "recipe-first", "memory", "policy", "ci");
  }

  @Override public int maxCycles() { return 1; }

  @Override public Inventory getInitialValue(ExecutionContext context) { return new Inventory(); }

  @Override
  public TreeVisitor<?, ExecutionContext> getScanner(Inventory inventory) {
    return new TreeVisitor<Tree, ExecutionContext>() {
      @Override public Tree preVisit(Tree tree, ExecutionContext context) {
        if (!(tree instanceof SourceFile file)) return tree;
        stopAfterPreVisit();
        String path = normalized(file.getSourcePath());
        for (Target target : targets()) {
          if (!target.path().equals(path)) continue;
          synchronized (inventory) {
            if (inventory.seen.putIfAbsent(path, file) != null) {
              inventory.conflicts.add("duplicate target " + path);
            } else if (target.create()) {
              if (!gitBlobSha1(file.printAll()).equals(target.after())) {
                inventory.conflicts.add("generated policy drift " + path);
              }
            } else if (!gitBlobSha1(file.printAll()).equals(target.before())) {
              inventory.conflicts.add("policy preimage drift " + path);
            }
          }
        }
        return tree;
      }
    };
  }

  @Override
  public Collection<? extends SourceFile> generate(Inventory inventory, ExecutionContext context) {
    prepare(inventory);
    List<SourceFile> generated = new ArrayList<>();
    for (Target target : targets()) {
      if (target.create() && !inventory.seen.containsKey(target.path())) {
        generated.add(inventory.after.get(target.path()));
      }
    }
    return generated;
  }

  @Override
  public TreeVisitor<?, ExecutionContext> getVisitor(Inventory inventory) {
    return new TreeVisitor<Tree, ExecutionContext>() {
      @Override public Tree preVisit(Tree tree, ExecutionContext context) {
        if (!(tree instanceof SourceFile file)) return tree;
        stopAfterPreVisit();
        prepare(inventory);
        String path = normalized(file.getSourcePath());
        for (Target target : targets()) {
          if (!target.create() && target.path().equals(path)) {
            return inventory.after.get(path);
          }
        }
        return tree;
      }
    };
  }

  private static void prepare(Inventory inventory) {
    synchronized (inventory) {
      if (!inventory.conflicts.isEmpty()) {
        throw new IllegalStateException(String.join("; ", inventory.conflicts));
      }
      if (inventory.after != null) return;
      Map<String, SourceFile> parsed = new HashMap<>();
      for (Target target : targets()) {
        String text = resource(target.resource());
        if (!gitBlobSha1(text).equals(target.after())) {
          throw new IllegalStateException("policy postimage drift " + target.path());
        }
        parsed.put(
            target.path(),
            PlainText.builder().sourcePath(Path.of(target.path())).text(text).build());
      }
      inventory.after = Map.copyOf(parsed);
    }
  }

  private static List<Target> targets() {
    return List.of(
        new Target(
            ".github/workflows/m3-recipe-ownership.yml",
            "d757a073ce0a60cd35f7169beea671203324ba16",
            "f82ed0daf49220d7b483bdf8025f5b378f8601eb",
            "m3-recipe-ownership.yml.txt",
            false),
        new Target(
            "m3/runtime-integration/verify-recipe-memory.py",
            "",
            "7d193b6ad53beb25824b856006893c883aa61bf4",
            "verify-recipe-memory.py.txt",
            true));
  }

  private static String resource(String name) {
    try (var stream = M3RecipeMemoryPolicyRecipe.class.getResourceAsStream(ROOT + name)) {
      if (stream == null) throw new IllegalStateException("missing M3 recipe-memory policy resource " + name);
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException failure) {
      throw new IllegalStateException("cannot read M3 recipe-memory policy resource " + name, failure);
    }
  }

  private static String normalized(Path path) {
    return path.normalize().toString().replace('\\', '/');
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
