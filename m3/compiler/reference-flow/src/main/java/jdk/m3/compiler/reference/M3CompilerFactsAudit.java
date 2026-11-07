// SPDX-License-Identifier: Apache-2.0
package jdk.m3.compiler.reference;

import com.sun.source.tree.*;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import jdk.m3.collections.M3ProgressMonitor;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import javax.lang.model.element.*;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.tools.*;

/**
 * Read-only, javac-attributed loop and allocation evidence in the existing compiler owner.
 * No annotation, lexical last use, or dead local grants mutation/reclamation authority.
 * This bounded frontend accepts explicit Java sources and platform classes, not an ambient classpath.
 */
public final class M3CompilerFactsAudit {
  private M3CompilerFactsAudit() {}

  public record Loop(String source, long position, String induction, M3LoopFacts.Kind kind,
                     long count, String expression, String conditions) {}
  public record Allocation(String source, long position, String type, String escape,
                           long lastLexicalReference, String obligations) {}
  public record Result(boolean baselineCompiles, String sourceDigest, String engineDigest,
                       String proofKey, List<Loop> loops, List<Allocation> allocations,
                       List<String> diagnostics) {
    public Result {
      loops = List.copyOf(loops);
      allocations = List.copyOf(allocations);
      diagnostics = List.copyOf(diagnostics);
    }
    public boolean authorizesReclamation() { return false; }
    public boolean authorizesSourceMutation() { return false; }
  }

  /** Read-only extension point; invoked only after the entire source set attributes successfully. */
  @FunctionalInterface
  public interface AttributedVisitor {
    void visit(CompilationUnitTree unit, Trees trees, M3ProgressMonitor monitor);
  }

  public static Result analyze(Map<String, String> sources, M3ProgressMonitor monitor) throws IOException {
    return analyze(sources, monitor, (unit, trees, progress) -> { });
  }

  /** Shares one javac attribution pass with additional analyses, without ambient classpath access. */
  public static Result analyze(Map<String, String> sources, M3ProgressMonitor monitor,
                               AttributedVisitor visitor) throws IOException {
    Objects.requireNonNull(visitor, "visitor");
    Objects.requireNonNull(sources, "sources");
    TreeMap<String, String> snapshot = new TreeMap<>();
    sources.forEach((name, text) -> {
      Objects.requireNonNull(name, "source name");
      Objects.requireNonNull(text, "source text");
      if (!name.endsWith(".java") || name.startsWith("/") || name.contains("\\")
          || Arrays.asList(name.split("/", -1)).stream().anyMatch(p -> p.isEmpty() || p.equals("..") || p.equals("."))) {
        throw new IllegalArgumentException("relative normalized Java source name required: " + name);
      }
      snapshot.put(name, text);
    });
    if (monitor != null) monitor.checkCanceled();
    String sourceDigest = digest(snapshot);
    String engineDigest = engineDigest();
    String proof = digest(Map.of("sources", sourceDigest, "engine", engineDigest,
        "platform", System.getProperty("java.runtime.version"), "policy", "release21;proc:none;classpath:empty;v1"));
    if (snapshot.isEmpty()) return new Result(true, sourceDigest, engineDigest, proof, List.of(), List.of(), List.of());
    JavaCompiler compiler = Objects.requireNonNull(ToolProvider.getSystemJavaCompiler(), "full JDK required");
    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    List<Loop> loops = new ArrayList<>();
    List<Allocation> allocations = new ArrayList<>();
    try (StandardJavaFileManager files = compiler.getStandardFileManager(diagnostics, Locale.ROOT, StandardCharsets.UTF_8)) {
      files.setLocationFromPaths(StandardLocation.CLASS_PATH, List.of());
      files.setLocationFromPaths(StandardLocation.SOURCE_PATH, List.of());
      List<Source> inputs = snapshot.entrySet().stream().map(e -> new Source(e.getKey(), e.getValue())).toList();
      JavacTask task = (JavacTask) compiler.getTask(new StringWriter(), files, diagnostics,
          List.of("--release", "21", "-proc:none", "-Xmaxerrs", "200"), null, inputs);
      List<CompilationUnitTree> units = new ArrayList<>();
      task.parse().forEach(units::add);
      task.analyze();
      if (monitor != null) monitor.checkCanceled();
      List<String> messages = diagnostics.getDiagnostics().stream().map(d -> d.getKind() + ":"
          + (d.getSource() == null ? "" : d.getSource().getName()) + ":" + d.getPosition() + ":"
          + d.getCode()).sorted().toList();
      boolean valid = diagnostics.getDiagnostics().stream().noneMatch(d -> d.getKind() == Diagnostic.Kind.ERROR);
      if (valid) {
        Trees trees = Trees.instance(task);
        for (CompilationUnitTree unit : units) {
          if (monitor != null) monitor.checkCanceled();
          new Scanner(unit, trees, monitor, loops, allocations).scan(unit, null);
          visitor.visit(unit, trees, monitor);
        }
      }
      loops.sort(Comparator.comparing(Loop::source).thenComparingLong(Loop::position));
      allocations.sort(Comparator.comparing(Allocation::source).thenComparingLong(Allocation::position));
      return new Result(valid, sourceDigest, engineDigest, proof, loops, allocations, messages);
    }
  }

  private static final class Source extends SimpleJavaFileObject {
    private final String text;
    Source(String name, String text) { super(uri(name), JavaFileObject.Kind.SOURCE); this.text = text; }
    @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) { return text; }
    private static URI uri(String name) {
      try { return new URI("m3", null, "/" + name, null); }
      catch (URISyntaxException e) { throw new IllegalArgumentException("source name", e); }
    }
  }

  private static final class Scanner extends TreePathScanner<Void, Void> {
    final CompilationUnitTree unit;
    final Trees trees;
    final M3ProgressMonitor monitor;
    final List<Loop> loops;
    final List<Allocation> allocations;
    Scanner(CompilationUnitTree unit, Trees trees, M3ProgressMonitor monitor,
            List<Loop> loops, List<Allocation> allocations) {
      this.unit = unit; this.trees = trees; this.monitor = monitor;
      this.loops = loops; this.allocations = allocations;
    }
    @Override public Void scan(Tree tree, Void unused) {
      if (monitor != null) monitor.checkCanceled();
      return super.scan(tree, unused);
    }
    String source() { return unit.getSourceFile().getName(); }
    long position(Tree tree) { return trees.getSourcePositions().getStartPosition(unit, tree); }
    Element element(Tree tree) {
      TreePath path = TreePath.getPath(unit, tree);
      return path == null ? null : trees.getElement(path);
    }
    Integer constant(ExpressionTree tree) {
      if (tree instanceof ParenthesizedTree p) return constant(p.getExpression());
      if (tree instanceof LiteralTree l) {
        if (l.getValue() instanceof Integer n) return n;
        if (l.getValue() instanceof Character c) return (int)c;
      }
      if (tree instanceof UnaryTree u && (u.getKind() == Tree.Kind.UNARY_MINUS || u.getKind() == Tree.Kind.UNARY_PLUS)) {
        Integer n = constant(u.getExpression());
        return n == null ? null : u.getKind() == Tree.Kind.UNARY_MINUS ? -n : n;
      }
      if (tree instanceof IdentifierTree || tree instanceof MemberSelectTree) {
        // Static constants through an expression receiver may still evaluate that receiver.
        if (tree instanceof MemberSelectTree s && !(element(s.getExpression()) instanceof TypeElement)
            && !(element(s.getExpression()) instanceof PackageElement)) return null;
        if (element(tree) instanceof VariableElement v) {
          Object value = v.getConstantValue();
          if (value instanceof Integer n) return n;
          if (value instanceof Character c) return (int)c;
        }
      }
      return null;
    }
    @Override public Void visitForLoop(ForLoopTree loop, Void unused) {
      loops.add(loop(loop));
      return super.visitForLoop(loop, unused);
    }
    @Override public Void visitWhileLoop(WhileLoopTree loop, Void unused) {
      unresolvedLoop(loop, "while loop needs a control-flow/induction proof");
      return super.visitWhileLoop(loop, unused);
    }
    @Override public Void visitDoWhileLoop(DoWhileLoopTree loop, Void unused) {
      unresolvedLoop(loop, "do-while loop needs a control-flow/induction proof");
      return super.visitDoWhileLoop(loop, unused);
    }
    @Override public Void visitEnhancedForLoop(EnhancedForLoopTree loop, Void unused) {
      unresolvedLoop(loop, "enhanced-for traversal not admitted by this counted-loop subset");
      return super.visitEnhancedForLoop(loop, unused);
    }
    void unresolvedLoop(Tree loop, String reason) {
      loops.add(new Loop(source(), position(loop), "", M3LoopFacts.Kind.UNKNOWN, -1, "", reason));
    }
    Loop unknown(ForLoopTree loop, String reason) {
      return new Loop(source(), position(loop), "", M3LoopFacts.Kind.UNKNOWN, -1, "", reason);
    }
    Loop loop(ForLoopTree loop) {
      if (loop.getInitializer().size() != 1 || !(loop.getInitializer().get(0) instanceof VariableTree v)
          || !(element(v) instanceof VariableElement induction) || induction.asType().getKind() != TypeKind.INT
          || !(loop.getCondition() instanceof BinaryTree condition) || loop.getUpdate().size() != 1
          || !induction.equals(element(condition.getLeftOperand()))) return unknown(loop, "unsupported counted-loop header");
      Integer start = constant(v.getInitializer());
      Integer step = null;
      ExpressionTree update = loop.getUpdate().get(0).getExpression();
      if (update instanceof UnaryTree u && induction.equals(element(u.getExpression()))) {
        step = switch (u.getKind()) {
          case PREFIX_INCREMENT, POSTFIX_INCREMENT -> 1;
          case PREFIX_DECREMENT, POSTFIX_DECREMENT -> -1;
          default -> null;
        };
      } else if (update instanceof CompoundAssignmentTree c && induction.equals(element(c.getVariable()))) {
        Integer n = constant(c.getExpression());
        if (n != null) {
          if (c.getKind() == Tree.Kind.PLUS_ASSIGNMENT) step = n;
          if (c.getKind() == Tree.Kind.MINUS_ASSIGNMENT && n != Integer.MIN_VALUE) step = -n;
        }
      }
      M3LoopFacts.Relation relation = switch (condition.getKind()) {
        case LESS_THAN -> M3LoopFacts.Relation.LT;
        case LESS_THAN_EQUAL -> M3LoopFacts.Relation.LE;
        case GREATER_THAN -> M3LoopFacts.Relation.GT;
        case GREATER_THAN_EQUAL -> M3LoopFacts.Relation.GE;
        default -> null;
      };
      if (start == null || step == null || relation == null) return unknown(loop, "unresolved induction or relation");
      ExpressionTree bound = condition.getRightOperand();
      Integer limit = constant(bound);
      Element boundOwner = element(bound);
      boolean arrayLength = bound instanceof MemberSelectTree s && s.getIdentifier().contentEquals("length")
          && s.getExpression() instanceof IdentifierTree && element(s.getExpression()) instanceof VariableElement a
          && a.asType().getKind() == TypeKind.ARRAY;
      if (arrayLength) boundOwner = element(((MemberSelectTree)bound).getExpression());
      if (induction.equals(boundOwner)) return unknown(loop, "bound depends on changing induction");
      boolean dynamic = limit == null && (bound instanceof IdentifierTree || arrayLength)
          && boundOwner instanceof VariableElement b
          && (b.getKind() == ElementKind.LOCAL_VARIABLE || b.getKind() == ElementKind.PARAMETER)
          && (arrayLength || b.asType().getKind() == TypeKind.INT);
      Body body = new Body(this, induction, boundOwner);
      body.scan(TreePath.getPath(unit, loop.getStatement()), null);
      if (body.mutates) return unknown(loop, "body changes induction or bound binding");
      String contract = "header fact only; no source mutation or reclamation authority";
      if (limit != null) {
        M3LoopFacts.Count count = M3LoopFacts.count(start, limit, step, relation, monitor);
        M3LoopFacts.Kind kind = count.kind();
        if (body.abrupt && kind == M3LoopFacts.Kind.EXACT_CONSTANT && count.count() != 0) kind = M3LoopFacts.Kind.UPPER_BOUND;
        return new Loop(source(), position(loop), v.getName().toString(), kind, count.count(),
            count.count() < 0 ? "" : Long.toString(count.count()), count.reason() + "; " + contract);
      }
      if (dynamic && start == 0 && step == 1 && relation == M3LoopFacts.Relation.LT) {
        return new Loop(source(), position(loop), v.getName().toString(),
            body.abrupt ? M3LoopFacts.Kind.UPPER_BOUND : M3LoopFacts.Kind.EXACT_SYMBOLIC,
            -1, "max(0," + bound + ")", "stable local bound; after successful bound evaluation; " + contract);
      }
      return unknown(loop, "dynamic header outside proven symbolic subset");
    }
    @Override public Void visitNewArray(NewArrayTree tree, Void unused) {
      allocation(tree, true);
      return super.visitNewArray(tree, unused);
    }
    @Override public Void visitNewClass(NewClassTree tree, Void unused) {
      allocation(tree, false);
      return super.visitNewClass(tree, unused);
    }
    void allocation(ExpressionTree tree, boolean array) {
      TreePath allocationPath = getCurrentPath();
      TreePath parent = allocationPath.getParentPath();
      String escape = "UNKNOWN";
      long last = -1;
      String type = trees.getTypeMirror(allocationPath).toString();
      if (array && parent != null && parent.getLeaf() instanceof VariableTree v && v.getInitializer() == tree
          && element(v) instanceof VariableElement variable && variable.getKind() == ElementKind.LOCAL_VARIABLE
          && variable.asType() instanceof ArrayType a && a.getComponentType().getKind().isPrimitive()) {
        TreePath method = parent;
        while (method != null && !(method.getLeaf() instanceof MethodTree)) method = method.getParentPath();
        if (method != null) {
          Uses uses = new Uses(this, variable, method.getLeaf());
          uses.scan(method, null);
          last = uses.last;
          if (!uses.unknown) escape = "LOCAL_PRIMITIVE_ARRAY_REFERENCE_ONLY";
        }
      }
      allocations.add(new Allocation(source(), position(tree), type, escape, last,
          "lexical position is not CFG last use; runtime owning count unresolved; no allocation elimination; "
          + "no GC/free/eviction authority; constructors/aliases/JNI/threads/identity need separate proof"));
    }
  }

  private static final class Body extends TreePathScanner<Void, Void> {
    final Scanner owner; final Element induction; final Element bound;
    boolean mutates; boolean abrupt;
    Body(Scanner owner, Element induction, Element bound) { this.owner = owner; this.induction = induction; this.bound = bound; }
    boolean watched(Tree t) { Element e = owner.element(t); return e != null && (e.equals(induction) || e.equals(bound)); }
    @Override public Void scan(Tree tree, Void unused) {
      if (owner.monitor != null) owner.monitor.checkCanceled();
      if (tree != null) {
        switch (tree.getKind()) {
          case BREAK, CONTINUE, RETURN, THROW, TRY, SWITCH, SWITCH_EXPRESSION, METHOD_INVOCATION, NEW_CLASS, NEW_ARRAY, ARRAY_ACCESS,
               MEMBER_SELECT, TYPE_CAST, DIVIDE, REMAINDER, DIVIDE_ASSIGNMENT, REMAINDER_ASSIGNMENT, SYNCHRONIZED,
               FOR_LOOP, ENHANCED_FOR_LOOP, WHILE_LOOP, DO_WHILE_LOOP, ASSERT -> abrupt = true;
          default -> { }
        }
        if (tree instanceof IdentifierTree) {
          Element e = owner.element(tree);
          if (e instanceof VariableElement v && (!v.asType().getKind().isPrimitive()
              || v.getKind() == ElementKind.FIELD)) abrupt = true;
        }
        if (tree instanceof AssignmentTree a && watched(a.getVariable())) mutates = true;
        if (tree instanceof CompoundAssignmentTree a && watched(a.getVariable())) mutates = true;
        if (tree instanceof UnaryTree u && switch (u.getKind()) {
          case PREFIX_INCREMENT, POSTFIX_INCREMENT, PREFIX_DECREMENT, POSTFIX_DECREMENT -> true;
          default -> false;
        } && watched(u.getExpression())) mutates = true;
      }
      return super.scan(tree, unused);
    }
  }

  private static final class Uses extends TreePathScanner<Void, Void> {
    final Scanner owner; final Element variable; final Tree method;
    boolean unknown; long last = -1;
    Uses(Scanner owner, Element variable, Tree method) { this.owner = owner; this.variable = variable; this.method = method; }
    @Override public Void visitIdentifier(IdentifierTree tree, Void unused) {
      if (owner.monitor != null) owner.monitor.checkCanceled();
      if (variable.equals(owner.trees.getElement(getCurrentPath()))) {
        last = Math.max(last, owner.position(tree));
        TreePath parent = getCurrentPath().getParentPath();
        Tree p = parent.getLeaf();
        boolean scalarAccess = p instanceof ArrayAccessTree a && a.getExpression() == tree;
        boolean lengthAccess = p instanceof MemberSelectTree s && s.getExpression() == tree && s.getIdentifier().contentEquals("length");
        if (!scalarAccess && !lengthAccess) unknown = true;
        for (TreePath path = parent; path != null; path = path.getParentPath()) {
          if (path.getLeaf() == method) break;
          if (path.getLeaf() instanceof LambdaExpressionTree || path.getLeaf() instanceof ClassTree
              || path.getLeaf() instanceof MethodTree) { unknown = true; break; }
        }
      }
      return super.visitIdentifier(tree, unused);
    }
  }

  private static String engineDigest() throws IOException {
    TreeMap<String, String> bytes = new TreeMap<>();
    ArrayDeque<Class<?>> pending = new ArrayDeque<>(List.of(M3CompilerFactsAudit.class, M3LoopFacts.class,
        M3Liveness.class, M3SourceLiveness.class));
    Set<Class<?>> visited = new HashSet<>();
    while (!pending.isEmpty()) {
      Class<?> type = pending.removeFirst();
      if (!visited.add(type)) continue;
      String name = "/" + type.getName().replace('.', '/') + ".class";
      try (var in = type.getResourceAsStream(name)) {
        if (in == null) throw new IOException("missing analyzer identity " + name);
        bytes.put(name, HexFormat.of().formatHex(sha256().digest(in.readAllBytes())));
      }
      pending.addAll(Arrays.asList(type.getNestMembers()));
    }
    return digest(bytes);
  }
  private static String digest(Map<String, String> values) {
    MessageDigest hash = sha256();
    new TreeMap<>(values).forEach((key, value) -> {
      for (String part : List.of(key, value)) {
        byte[] bytes = part.getBytes(StandardCharsets.UTF_8);
        hash.update(java.nio.ByteBuffer.allocate(4).putInt(bytes.length).array());
        hash.update(bytes);
      }
    });
    return HexFormat.of().formatHex(hash.digest());
  }
  private static MessageDigest sha256() {
    try { return MessageDigest.getInstance("SHA-256"); }
    catch (NoSuchAlgorithmException e) { throw new AssertionError(e); }
  }
}
