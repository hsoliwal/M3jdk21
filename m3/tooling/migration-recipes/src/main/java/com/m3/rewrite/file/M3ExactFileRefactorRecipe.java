// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.file;

import com.m3.rewrite.scope.M3FileScopedRecipe;
import com.m3.rewrite.scope.M3ScopeFence;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

/**
 * Exact, source-bound FILE refactor replay.
 *
 * <p>The recipe never claims semantic equivalence. A separately reviewed before/after pair and its
 * tests are the proof candidate. This class only guarantees exact preimage admission, exact
 * postimage materialization, file-scope fencing and fixed-point replay.
 */
public final class M3ExactFileRefactorRecipe extends Recipe implements M3FileScopedRecipe {
  private final String targetPath;
  private final String beforeSha256;
  private final String afterSha256;
  private final String afterSource;
  private final String atomId;
  private final String patternId;
  private final String iopRole;

  public M3ExactFileRefactorRecipe(
      String targetPath,
      String beforeSource,
      String afterSource,
      String atomId,
      String patternId,
      String iopRole) {
    this.targetPath = M3ScopeFence.file(targetPath).roots().getFirst();
    this.afterSource = Objects.requireNonNull(afterSource, "afterSource");
    this.beforeSha256 = hash(Objects.requireNonNull(beforeSource, "beforeSource"));
    this.afterSha256 = hash(afterSource);
    this.atomId = semanticId(atomId, "atomId");
    this.patternId = semanticId(patternId, "patternId");
    this.iopRole = semanticId(iopRole, "iopRole");
    if (beforeSha256.equals(afterSha256)) {
      throw new IllegalArgumentException("M3_FILE_RECIPE_NO_DELTA");
    }
  }

  @Override
  public String targetPath() {
    return targetPath;
  }

  public String beforeSha256() {
    return beforeSha256;
  }

  public String afterSha256() {
    return afterSha256;
  }

  public String atomId() {
    return atomId;
  }

  public String patternId() {
    return patternId;
  }

  public String iopRole() {
    return iopRole;
  }

  @Override
  public String getDisplayName() {
    return "Replay one exact M3 file refactor";
  }

  @Override
  public String getDescription() {
    return "Apply one hash-pinned atomization/patternization postimage to exactly one source file.";
  }

  @Override
  public TreeVisitor<?, ExecutionContext> getVisitor() {
    return new JavaIsoVisitor<ExecutionContext>() {
      @Override
      public J.CompilationUnit visitCompilationUnit(
          J.CompilationUnit cu, ExecutionContext context) {
        String path = normalizedPath(cu);
        if (!targetPath.equals(path)) {
          return cu;
        }
        requireWritable(path);

        String currentSha256 = hash(cu.printAll());
        if (afterSha256.equals(currentSha256)) {
          return cu;
        }
        if (!beforeSha256.equals(currentSha256)) {
          throw new IllegalStateException("M3_FILE_RECIPE_PREIMAGE_DRIFT:" + path);
        }
        if (!afterSha256.equals(hash(afterSource))) {
          throw new IllegalStateException("M3_FILE_RECIPE_POSTIMAGE_DRIFT:" + path);
        }

        List<SourceFile> parsed =
            JavaParser.fromJavaVersion().build().parse(context, afterSource).toList();
        if (parsed.size() != 1 || !(parsed.getFirst() instanceof J.CompilationUnit replacement)) {
          throw new IllegalStateException("M3_FILE_RECIPE_POSTIMAGE_PARSE:" + path);
        }
        if (!afterSha256.equals(hash(replacement.printAll()))) {
          throw new IllegalStateException("M3_FILE_RECIPE_POSTIMAGE_ROUNDTRIP:" + path);
        }
        return replacement
            .withId(cu.getId())
            .withSourcePath(cu.getSourcePath())
            .withMarkers(cu.getMarkers());
      }
    };
  }

  private static String normalizedPath(SourceFile sourceFile) {
    return sourceFile.getSourcePath().toString().replace((char) 92, '/');
  }

  private static String semanticId(String value, String name) {
    Objects.requireNonNull(value, name);
    if (!value.matches("[A-Za-z0-9_.:/-]{1,200}")) {
      throw new IllegalArgumentException("M3_FILE_RECIPE_" + name.toUpperCase());
    }
    return value;
  }

  private static String hash(String text) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest(text.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException(impossible);
    }
  }
}
