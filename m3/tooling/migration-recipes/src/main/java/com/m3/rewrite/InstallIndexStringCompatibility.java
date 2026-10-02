// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
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
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

/** Explicit additive compatibility installation, not java.base or automatic caller lowering. */
public final class InstallIndexStringCompatibility extends ScanningRecipe<InstallIndexStringCompatibility.State> {
  private static final String ROOT = "/com/m3/rewrite/port/";
  private static final String MODULE = "m3/ports/indexstring/";
  private static final String[] NAMES = {"FrozenBytes.java", "FrozenChars.java", "MIndexJoinedBytes.java",
      "MIndexJoinedChars.java", "MIndexJoinedStorageIntern.java", "MIndexJoinedStreams.java", "M3Text.java"};
  public static final class State { final Map<String, SourceFile> files = new HashMap<>(); }
  @Override public String getDisplayName() { return "Install the pinned IndexString compatibility port"; }
  @Override public String getDescription() {
    return "Require the exact standalone POM, generate all seven sources, and refuse partial or divergent target state.";
  }
  @Override public State getInitialValue(ExecutionContext ctx) { return new State(); }
  @Override public TreeVisitor<?, ExecutionContext> getScanner(State state) {
    return new TreeVisitor<Tree, ExecutionContext>() {
      @Override public Tree visit(Tree tree, ExecutionContext ctx) {
        if (tree instanceof SourceFile file) {
          String path = file.getSourcePath().toString().replace((char) 92, '/');
          state.files.put(path, file);
        }
        return tree;
      }
    };
  }
  @Override public Collection<? extends SourceFile> generate(State state, ExecutionContext ctx) {
    Properties plan = plan();
    String prefix = null;
    for (String candidate : List.of("", MODULE)) {
      SourceFile pom = state.files.get(candidate + "pom.xml");
      if (pom != null && hash(pom.printAll()).equals(plan.getProperty("pom.sha256"))) {
        if (prefix != null) throw new IllegalStateException("ambiguous port POM");
        prefix = candidate;
      }
    }
    if (prefix == null) throw new IllegalStateException("missing or divergent compatibility POM");
    int present = 0;
    String[] sources = new String[NAMES.length];
    Map<String, String> expected = new HashMap<>();
    for (int i = 0; i < NAMES.length; i++) {
      String name = NAMES[i];
      sources[i] = resource(name + ".txt");
      String digest = plan.getProperty(name + ".sha256");
      if (!hash(sources[i]).equals(digest)) throw new IllegalStateException("port resource drift: " + name);
      String path = prefix + relative(name);
      expected.put(digest, path);
      SourceFile existing = state.files.get(path);
      if (existing != null) {
        if (!hash(existing.printAll()).equals(digest)) throw new IllegalStateException("target drift: " + path);
        present++;
      }
    }
    if (present == NAMES.length) return List.of();
    if (present != 0) throw new IllegalStateException("partial target state; explicit rollback/recovery required");
    List<SourceFile> generated = new ArrayList<>();
    for (SourceFile parsed : JavaParser.fromJavaVersion().build().parse(ctx, sources).toList()) {
      String path = expected.remove(hash(parsed.printAll()));
      if (!(parsed instanceof J.CompilationUnit) || path == null) {
        throw new IllegalStateException("port source failed exact Java parse/round-trip");
      }
      generated.add(parsed.withSourcePath(Path.of(path)));
    }
    if (!expected.isEmpty()) throw new IllegalStateException("missing parsed source");
    return generated;
  }
  private static String relative(String name) {
    return "src/main/java/" + (name.equals("M3Text.java") ? "com/m3/text/compat/" : "com/synexia/indexstring/") + name;
  }
  private static Properties plan() {
    Properties result = new Properties();
    try (InputStream in = InstallIndexStringCompatibility.class.getResourceAsStream(ROOT + "port.properties")) {
      if (in == null) throw new IllegalStateException("missing port projection");
      result.load(in); return result;
    } catch (IOException error) { throw new IllegalStateException(error); }
  }
  private static String resource(String name) {
    try (InputStream in = InstallIndexStringCompatibility.class.getResourceAsStream(ROOT + name)) {
      if (in == null) throw new IllegalStateException("missing resource: " + name);
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException error) { throw new IllegalStateException(error); }
  }
  private static String hash(String text) {
    try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8))); }
    catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
  }
}
