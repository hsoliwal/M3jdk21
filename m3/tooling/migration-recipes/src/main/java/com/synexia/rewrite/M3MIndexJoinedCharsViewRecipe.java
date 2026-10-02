// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.m3.rewrite.scope.M3ScopeFence;
import com.m3.rewrite.scope.M3ScopedRecipe;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

/** Exact-source owner extension, not a compiler-lowering or semantic-equivalence oracle. */
public final class M3MIndexJoinedCharsViewRecipe
    extends ScanningRecipe<M3MIndexJoinedCharsViewRecipe.State> implements M3ScopedRecipe {
  private static final M3ScopeFence EDIT_SCOPE = M3ScopeFence.module("synexia-indexstring");
  private static final String ROOT = "/com/synexia/rewrite/m3port/";
  private static final String OWNER = "MIndexJoinedChars.java";
  private static final String PREFIX = "src/main/java/com/synexia/indexstring/";
  private static final String[] NAMES = {"FrozenBytes.java", "FrozenChars.java",
      "MIndexJoinedBytes.java", OWNER, "MIndexJoinedStorageIntern.java", "MIndexJoinedStreams.java"};

  public static final class State {
    final Map<String, String> seen = new HashMap<>();
    final Map<String, String> paths = new HashMap<>();
    final Properties plan = properties();
  }
  @Override public M3ScopeFence editScope() { return EDIT_SCOPE; }
  @Override public String getDisplayName() { return "Extend the pinned retained IndexString view"; }
  @Override public String getDescription() {
    return "Require the exact six-file owner closure, add retained concat and direct copy, and refuse source/dependency drift.";
  }
  @Override public State getInitialValue(ExecutionContext ctx) { return new State(); }

  @Override public TreeVisitor<?, ExecutionContext> getScanner(State state) {
    return new TreeVisitor<Tree, ExecutionContext>() {
      @Override public Tree visit(Tree tree, ExecutionContext ctx) {
        if (tree instanceof SourceFile file) {
          String name = managed(file);
          if (name != null) {
            String hash = hash(file.printAll());
            String path = file.getSourcePath().toString().replace((char) 92, '/');
            String previousPath = state.paths.putIfAbsent(name, path);
            if (previousPath != null && !previousPath.equals(path)) {
              throw new IllegalStateException("ambiguous owner path: " + name);
            }
            state.seen.put(name, hash);
            if (!hash.equals(state.plan.getProperty(name + ".before"))
                && !(name.equals(OWNER) && hash.equals(state.plan.getProperty(name + ".after")))) {
              throw new IllegalStateException("source/dependency drift: " + name);
            }
          }
        }
        return tree;
      }
    };
  }

  @Override public Collection<? extends SourceFile> generate(State state, ExecutionContext ctx) {
    for (String name : NAMES) {
      if (!state.seen.containsKey(name)) throw new IllegalStateException("missing owner/dependency: " + name);
    }
    return List.of();
  }

  @Override public TreeVisitor<?, ExecutionContext> getVisitor(State state) {
    return new JavaIsoVisitor<ExecutionContext>() {
      @Override public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
        if (!OWNER.equals(managed(cu))) return cu;
        String after = resource(OWNER + ".after.txt");
        String afterHash = state.plan.getProperty(OWNER + ".after");
        if (!hash(after).equals(afterHash)) throw new IllegalStateException("replacement resource drift");
        String current = hash(cu.printAll());
        if (current.equals(afterHash)) return cu;
        if (!current.equals(state.plan.getProperty(OWNER + ".before"))) {
          throw new IllegalStateException("source changed after scan");
        }
        String[] sources = new String[NAMES.length];
        for (int i = 0; i < NAMES.length; i++) {
          sources[i] = NAMES[i].equals(OWNER) ? after : resource(NAMES[i] + ".before.txt");
          String expected = state.plan.getProperty(NAMES[i] + (NAMES[i].equals(OWNER) ? ".after" : ".before"));
          if (!hash(sources[i]).equals(expected)) throw new IllegalStateException("dependency resource drift");
        }
        for (SourceFile parsed : JavaParser.fromJavaVersion().build().parse(ctx, sources).toList()) {
          if (parsed instanceof J.CompilationUnit replacement && hash(parsed.printAll()).equals(afterHash)) {
            return replacement.withId(cu.getId()).withSourcePath(cu.getSourcePath());
          }
        }
        throw new IllegalStateException("exact Java replacement did not parse/round-trip");
      }
    };
  }

  private static String managed(SourceFile file) {
    String path = file.getSourcePath().toString().replace((char) 92, '/');
    if (path.startsWith("synexia-indexstring/")) path = path.substring("synexia-indexstring/".length());
    for (String name : NAMES) if (path.equals(PREFIX + name)) return name;
    return null;
  }
  private static Properties properties() {
    Properties properties = new Properties();
    try (InputStream input = M3MIndexJoinedCharsViewRecipe.class.getResourceAsStream(ROOT + "source.properties")) {
      if (input == null) throw new IllegalStateException("missing source plan projection");
      properties.load(input);
      if (!"m3-source-port/1".equals(properties.getProperty("schema"))) throw new IllegalStateException("bad source plan");
      return properties;
    } catch (IOException error) { throw new IllegalStateException("cannot read source plan", error); }
  }
  private static String resource(String name) {
    try (InputStream input = M3MIndexJoinedCharsViewRecipe.class.getResourceAsStream(ROOT + name)) {
      if (input == null) throw new IllegalStateException("missing source image: " + name);
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException error) { throw new IllegalStateException("cannot read source image", error); }
  }
  private static String hash(String text) {
    try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8))); }
    catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
  }
}
