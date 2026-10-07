// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.ast;

import com.sun.source.tree.BreakTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ContinueTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.LabeledStatementTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;
import com.synexia.indexstring.MIndexString;
import com.synexia.indexstring.MatIndexString;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import javax.tools.DiagnosticCollector;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javax.lang.model.element.Modifier;
import javax.tools.Diagnostic;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

/**
 * Java 21 parser that atomizes javac trees directly into canonical MIndexAST handles.
 *
 * <p>No attribution is required. Syntax-error trees are retained as partial AST atoms and compiler
 * diagnostics are attached to the document. JDK textual names are admitted into the shared
 * MIndexString label pool immediately; large subtree text is never retained.</p>
 */
public final class MIndexASTParser {
  private final MIndexASTPool pool;

  public MIndexASTParser(MIndexASTPool pool) {
    this.pool = Objects.requireNonNull(pool, "pool");
  }

  public MIndexASTPool pool() {
    return pool;
  }

  public MIndexASTDocument parse(String path, CharSequence source) throws IOException {
    Objects.requireNonNull(path, "path");
    Objects.requireNonNull(source, "source");
    CharSequence stable = stableSource(source);
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    if (compiler == null) throw new IllegalStateException("MIndexAST parsing requires a JDK compiler");

    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    JavaFileObject input = new SourceFile(path, stable);
    JavacTask task =
        (JavacTask)
            compiler.getTask(
                null,
                null,
                diagnostics,
                List.of("--release", Integer.toString(pool.spec().release()), "-proc:none"),
                null,
                List.of(input));

    Iterator<? extends CompilationUnitTree> units = task.parse().iterator();
    if (!units.hasNext()) throw new IllegalStateException("javac returned no compilation unit");
    CompilationUnitTree unit = units.next();
    if (units.hasNext()) throw new IllegalStateException("single source produced multiple units");

    Trees trees = Trees.instance(task);
    Builder builder = new Builder(pool, unit, trees.getSourcePositions());
    BuildResult root = builder.scan(unit, null);
    if (root == null) throw new IllegalStateException("javac returned an empty syntax tree");

    MatIndexString indexedPath = pool.labels().internText(pool.languageId(), path);
    return builder.occurrences.build(
        indexedPath,
        sourceUtf16Sha256(stable),
        pool.value(root.atomHandle()),
        root.occurrence(),
        indexDiagnostics(diagnostics.getDiagnostics()));
  }

  /** Parses directly from immutable indexed source without materializing a whole source String. */
  public MIndexASTDocument parse(MIndexString path, MIndexString source) throws IOException {
    Objects.requireNonNull(path, "path");
    Objects.requireNonNull(source, "source");
    return parse(path.asString(), source);
  }

  private List<MIndexASTDiagnostic> indexDiagnostics(
      List<Diagnostic<? extends JavaFileObject>> diagnostics) {
    List<MIndexASTDiagnostic> result = new ArrayList<>(diagnostics.size());
    for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics) {
      MatIndexString code =
          pool.labels().internText(pool.languageId(), Objects.toString(diagnostic.getCode(), ""));
      MatIndexString message =
          pool.labels().internText(pool.languageId(), diagnostic.getMessage(Locale.ROOT));
      result.add(
          new MIndexASTDiagnostic(
              diagnostic.getKind(),
              diagnostic.getLineNumber(),
              diagnostic.getColumnNumber(),
              diagnostic.getStartPosition(),
              diagnostic.getEndPosition(),
              code,
              message));
    }
    return List.copyOf(result);
  }

  private static CharSequence stableSource(CharSequence source) {
    return source instanceof String
            || source instanceof MIndexString
            || source instanceof MatIndexString
        ? source
        : source.toString();
  }

  private static byte[] sourceUtf16Sha256(CharSequence source) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      for (int i = 0; i < source.length(); i++) {
        char value = source.charAt(i);
        digest.update((byte) (value >>> 8));
        digest.update((byte) value);
      }
      return digest.digest();
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException("SHA-256 unavailable", impossible);
    }
  }

  private static final class Builder extends TreeScanner<BuildResult, Void> {
    private final MIndexASTPool pool;
    private final CompilationUnitTree unit;
    private final SourcePositions positions;
    private final ArrayDeque<Frame> frames = new ArrayDeque<>();
    private final OccurrenceBuilder occurrences = new OccurrenceBuilder();

    Builder(MIndexASTPool pool, CompilationUnitTree unit, SourcePositions positions) {
      this.pool = pool;
      this.unit = unit;
      this.positions = positions;
    }

    @Override
    public BuildResult scan(Tree tree, Void unused) {
      if (tree == null) return null;

      Frame frame = new Frame();
      int depth = frames.size();
      frames.push(frame);
      super.scan(tree, unused);
      frames.pop();

      int labelHandle = labelHandle(tree);
      long flags = flags(tree);
      MIndexAST atom = pool.atom(tree.getKind(), labelHandle, flags, frame.atomHandles());
      long start = positions.getStartPosition(unit, tree);
      long end = positions.getEndPosition(unit, tree);
      int occurrence =
          occurrences.add(atom.handle(), start, end, depth, frame.occurrences());

      BuildResult result = new BuildResult(atom.handle(), occurrence);
      if (!frames.isEmpty()) frames.peek().add(result);
      return result;
    }

    private int labelHandle(Tree tree) {
      CharSequence label = null;
      if (tree instanceof IdentifierTree value) {
        label = value.getName();
      } else if (tree instanceof MemberSelectTree value) {
        label = value.getIdentifier();
      } else if (tree instanceof MethodTree value) {
        label = value.getName();
      } else if (tree instanceof ClassTree value) {
        label = value.getSimpleName();
      } else if (tree instanceof VariableTree value) {
        label = value.getName();
      } else if (tree instanceof TypeParameterTree value) {
        label = value.getName();
      } else if (tree instanceof LabeledStatementTree value) {
        label = value.getLabel();
      } else if (tree instanceof BreakTree value) {
        label = value.getLabel();
      } else if (tree instanceof ContinueTree value) {
        label = value.getLabel();
      } else if (tree instanceof MemberReferenceTree value) {
        label = value.getName();
      } else if (tree instanceof PrimitiveTypeTree value) {
        label = value.getPrimitiveTypeKind().toString();
      } else if (tree instanceof LiteralTree value) {
        Object literal = value.getValue();
        label = literal == null ? "null" : String.valueOf(literal);
      }
      return label == null || label.length() == 0 ? 0 : pool.internLabel(label);
    }

    private static long flags(Tree tree) {
      if (tree instanceof ModifiersTree modifiers) {
        long flags = 0L;
        for (Modifier modifier : modifiers.getFlags()) {
          int bit = modifier.ordinal();
          if (bit >= 56) {
            throw new IllegalStateException("Modifier ordinal exceeds MIndexAST flag lane");
          }
          flags |= 1L << bit;
        }
        return flags;
      }
      if (tree instanceof ImportTree value) return value.isStatic() ? 1L : 0L;
      if (tree instanceof MemberReferenceTree value) return value.getMode().ordinal() + 1L;
      if (tree instanceof CaseTree value) return value.getCaseKind().ordinal() + 1L;
      if (tree instanceof LambdaExpressionTree value) return value.getBodyKind().ordinal() + 1L;
      if (tree instanceof com.sun.source.tree.ModuleTree value) {
        return value.getModuleType().ordinal() + 1L;
      }
      return 0L;
    }
  }

  private record BuildResult(int atomHandle, int occurrence) {}

  private static final class Frame {
    private int[] atomHandles = new int[4];
    private int[] occurrences = new int[4];
    private int size;

    void add(BuildResult result) {
      if (size == atomHandles.length) {
        int next = atomHandles.length + (atomHandles.length >>> 1) + 1;
        atomHandles = Arrays.copyOf(atomHandles, next);
        occurrences = Arrays.copyOf(occurrences, next);
      }
      atomHandles[size] = result.atomHandle();
      occurrences[size] = result.occurrence();
      size++;
    }

    int[] atomHandles() {
      return Arrays.copyOf(atomHandles, size);
    }

    int[] occurrences() {
      return Arrays.copyOf(occurrences, size);
    }
  }

  private static final class OccurrenceBuilder {
    private int[] atomHandles = new int[64];
    private long[] starts = new long[64];
    private long[] ends = new long[64];
    private int[] parents = filled(64, -1);
    private int[] firstChildren = filled(64, -1);
    private int[] nextSiblings = filled(64, -1);
    private int[] depths = new int[64];
    private int size;

    int add(int atomHandle, long start, long end, int depth, int[] children) {
      reserve(size + 1);
      int occurrence = size++;
      atomHandles[occurrence] = atomHandle;
      starts[occurrence] = start;
      ends[occurrence] = end;
      depths[occurrence] = depth;
      if (children.length != 0) {
        firstChildren[occurrence] = children[0];
        for (int i = 0; i < children.length; i++) {
          int child = children[i];
          parents[child] = occurrence;
          nextSiblings[child] = i + 1 < children.length ? children[i + 1] : -1;
        }
      }
      return occurrence;
    }

    MIndexASTDocument build(
        MatIndexString path,
        byte[] sourceHash,
        MIndexAST root,
        int rootOccurrence,
        List<MIndexASTDiagnostic> diagnostics) {
      return new MIndexASTDocument(
          path,
          sourceHash,
          root,
          rootOccurrence,
          Arrays.copyOf(atomHandles, size),
          Arrays.copyOf(starts, size),
          Arrays.copyOf(ends, size),
          Arrays.copyOf(parents, size),
          Arrays.copyOf(firstChildren, size),
          Arrays.copyOf(nextSiblings, size),
          Arrays.copyOf(depths, size),
          diagnostics);
    }

    private void reserve(int required) {
      if (required <= atomHandles.length) return;
      int old = atomHandles.length;
      int next = Math.max(required, old + (old >>> 1) + 1);
      atomHandles = Arrays.copyOf(atomHandles, next);
      starts = Arrays.copyOf(starts, next);
      ends = Arrays.copyOf(ends, next);
      parents = growFilled(parents, next, -1);
      firstChildren = growFilled(firstChildren, next, -1);
      nextSiblings = growFilled(nextSiblings, next, -1);
      depths = Arrays.copyOf(depths, next);
    }

    private static int[] filled(int size, int value) {
      int[] result = new int[size];
      Arrays.fill(result, value);
      return result;
    }

    private static int[] growFilled(int[] values, int size, int fill) {
      int old = values.length;
      int[] result = Arrays.copyOf(values, size);
      Arrays.fill(result, old, size, fill);
      return result;
    }
  }

  private static final class SourceFile extends SimpleJavaFileObject {
    private final CharSequence source;

    SourceFile(String path, CharSequence source) {
      super(uri(path), Kind.SOURCE);
      this.source = source;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
      return source;
    }

    private static URI uri(String path) {
      String normalized = path.replace('\\', '/');
      if (!normalized.startsWith("/")) normalized = "/" + normalized;
      try {
        return new URI("mindex", null, normalized, null);
      } catch (URISyntaxException invalid) {
        throw new IllegalArgumentException("invalid source path " + path, invalid);
      }
    }
  }
}
