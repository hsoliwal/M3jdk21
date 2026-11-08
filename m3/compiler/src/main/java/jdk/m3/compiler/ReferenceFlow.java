// SPDX-License-Identifier: Apache-2.0
package jdk.m3.compiler;

import com.sun.source.tree.*;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;
import jdk.m3.collections.M3ProgressMonitor;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import javax.lang.model.element.*;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/**
 * Flow-insensitive source-local reference inclusion. Reuses the existing liveness fixed point.
 * Allocation-site bits are NOT runtime objects or owning-reference counts. No result permits free.
 */
public final class ReferenceFlow {
  private ReferenceFlow() { }

  /** Bit zero is unknown/external; null contributes no bit; each allocation site has a positive bit. */
  public record Alias(String name, String type, long position, BitSet origins) {
    public Alias { origins = (BitSet) Objects.requireNonNull(origins, "origins").clone(); }
    @Override public BitSet origins() { return (BitSet) origins.clone(); }
  }
  public record Site(int origin, long position, String type, boolean primitiveArray,
                     List<Long> enclosingLoops, List<String> exposures) {
    public Site { enclosingLoops = List.copyOf(enclosingLoops); exposures = List.copyOf(exposures); }
    public boolean mayEscape() { return !exposures.isEmpty(); }
    public boolean authorizesReclamation() { return false; }
  }
  public record MethodReferences(String source, String signature, long position, List<Alias> aliases,
                                  List<Site> sites, List<String> unresolved) {
    public MethodReferences { aliases = List.copyOf(aliases); sites = List.copyOf(sites); unresolved = List.copyOf(unresolved); }
    public boolean complete() { return unresolved.isEmpty(); }
    public boolean authorizesReclamation() { return false; }
  }
  public record Result(CompilerFactsAudit.Result facts, String proofKey, List<MethodReferences> methods) {
    public Result { Objects.requireNonNull(facts, "facts"); methods = List.copyOf(methods); }
    public boolean authorizesReclamation() { return false; }
    public boolean authorizesSourceMutation() { return false; }
  }
  /** Lost visible local aliases, including potentially escaped origins. Never a release instruction. */
  public record LocalBoundary(String source, String signature, int from, int to, BitSet origins,
                              BitSet exposedOrigins) {
    public LocalBoundary { origins = (BitSet) origins.clone(); exposedOrigins = (BitSet) exposedOrigins.clone(); }
    @Override public BitSet origins() { return (BitSet) origins.clone(); }
    @Override public BitSet exposedOrigins() { return (BitSet) exposedOrigins.clone(); }
    public boolean authorizesReclamation() { return false; }
  }
  public record Projection(List<LocalBoundary> boundaries, List<String> unresolved) {
    public Projection { boundaries = List.copyOf(boundaries); unresolved = List.copyOf(unresolved); }
    public boolean authorizesReclamation() { return false; }
  }

  public static Result analyze(Map<String, String> sources, M3ProgressMonitor monitor) throws IOException {
    return analyze(sources, SourceLiveness.Limits.defaults(), monitor);
  }
  /** Budgets bound constraint vertices, origin bits, syntax nesting and words per matrix, respectively. */
  public static Result analyze(Map<String, String> sources, SourceLiveness.Limits limits,
                                M3ProgressMonitor monitor) throws IOException {
    Objects.requireNonNull(limits, "limits"); check(monitor);
    List<MethodReferences> result = new ArrayList<>();
    var facts = CompilerFactsAudit.analyze(sources, monitor, (unit, trees, progress) -> {
      new TreePathScanner<Void, Void>() {
        @Override public Void scan(Tree tree, Void p) { check(progress); return super.scan(tree, p); }
        @Override public Void visitMethod(MethodTree method, Void p) {
          if (trees.getSourcePositions().getEndPosition(unit, method) >= 0) {
            result.add(new Builder(unit, trees, getCurrentPath(), limits, progress).build());
          }
          return super.visitMethod(method, p);
        }
      }.scan(unit, null);
    });
    result.sort(Comparator.comparing(MethodReferences::source).thenComparingLong(MethodReferences::position)
        .thenComparing(MethodReferences::signature));
    check(monitor);
    return new Result(facts, identity(facts.proofKey(), limits), result);
  }

  public static Projection project(Result references, SourceLiveness.Result flow,
                                    M3ProgressMonitor monitor) {
    return project(references, flow, false, monitor);
  }
  /** Explicit JNI selection uses Liveness.nativeEndingBatch from the existing compiler library. */
  public static Projection projectNative(Result references, SourceLiveness.Result flow,
                                          M3ProgressMonitor monitor) {
    return project(references, flow, true, monitor);
  }
  private static Projection project(Result references, SourceLiveness.Result flow, boolean nativeMasks,
                                     M3ProgressMonitor monitor) {
    Objects.requireNonNull(references, "references"); Objects.requireNonNull(flow, "flow"); check(monitor);
    if (!references.facts().baselineCompiles() || !flow.facts().baselineCompiles()
        || !references.facts().sourceDigest().equals(flow.facts().sourceDigest())
        || !references.facts().proofKey().equals(flow.facts().proofKey())) {
      throw new IllegalArgumentException("source/compiler snapshots differ or failed attribution");
    }
    Map<String, MethodReferences> byMethod = new TreeMap<>();
    for (var m : references.methods()) {
      if (byMethod.put(key(m.source(), m.signature(), m.position()), m) != null) throw new IllegalArgumentException("duplicate method");
    }
    List<LocalBoundary> result = new ArrayList<>(); List<String> unresolved = new ArrayList<>();
    for (var method : flow.methods()) {
      check(monitor);
      var refs = byMethod.remove(key(method.source(), method.signature(), method.position()));
      if (refs == null) throw new IllegalArgumentException("missing reference method");
      if (!method.complete() || !refs.complete()) {
        unresolved.add(method.signature() + ": " + method.unresolved() + refs.unresolved()); continue;
      }
      Map<String, BitSet> aliases = new HashMap<>();
      for (var a : refs.aliases()) aliases.put(key(a.name(), a.type(), a.position()), a.origins());
      BitSet[] slots = new BitSet[method.slots().size()];
      for (var slot : method.slots()) slots[slot.index()] = aliases.remove(key(slot.name(), slot.type(), slot.position()));
      if (!aliases.isEmpty()) throw new IllegalArgumentException("reference/CFG local identities differ");
      BitSet exposed = new BitSet();
      for (var site : refs.sites()) if (site.mayEscape()) exposed.set(site.origin());
      var live = method.liveness().orElseThrow();
      for (var edge : SourceLiveness.boundaries(method, monitor)) {
        BitSet from = live.liveIn(edge.from()); from.or(method.nodes().get(edge.from()).def());
        BitSet before = union(from, slots, monitor), after = union(live.liveIn(edge.to()), slots, monitor);
        // External/unknown is TOP, not a disjoint allocation. Any surviving unknown local may alias every site.
        if (before.get(0)) before.set(1, refs.sites().size() + 1);
        if (after.get(0)) after.set(1, refs.sites().size() + 1);
        int words = (Math.max(before.length(), after.length()) + 63) / 64;
        long[] a = Arrays.copyOf(before.toLongArray(), words), b = Arrays.copyOf(after.toLongArray(), words);
        BitSet ended = BitSet.valueOf(nativeMasks ? Liveness.nativeEndingBatch(a, b, monitor)
            : Liveness.endingBatch(a, b, monitor));
        ended.clear(0);
        if (!ended.isEmpty()) {
          BitSet escaped = (BitSet) ended.clone(); escaped.and(exposed);
          result.add(new LocalBoundary(method.source(), method.signature(), edge.from(), edge.to(), ended, escaped));
        }
      }
    }
    if (!byMethod.isEmpty()) throw new IllegalArgumentException("missing CFG method");
    check(monitor); return new Projection(result, unresolved);
  }
  private static BitSet union(BitSet locals, BitSet[] origins, M3ProgressMonitor monitor) {
    BitSet result = new BitSet();
    for (int n = locals.nextSetBit(0); n >= 0; n = locals.nextSetBit(n + 1)) {
      check(monitor); if (origins[n] != null) result.or(origins[n]);
    }
    return result;
  }
  private static String key(String a, String b, long p) { return a.length() + ":" + a + b.length() + ":" + b + ":" + p; }
  private static void check(M3ProgressMonitor monitor) { if (monitor != null) monitor.checkCanceled(); }
  private static String identity(String parent, SourceLiveness.Limits limits) throws IOException {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      digest.update((parent + "\nreference-flow-v5\n" + limits).getBytes(StandardCharsets.UTF_8));
      Class<?>[] nest = ReferenceFlow.class.getNestMembers(); Arrays.sort(nest, Comparator.comparing(Class::getName));
      for (Class<?> type : nest) {
        String name = "/" + type.getName().replace('.', '/') + ".class";
        try (var in = type.getResourceAsStream(name)) {
          if (in == null) throw new IOException("missing reference analyzer identity " + name);
          digest.update(name.getBytes(StandardCharsets.UTF_8)); digest.update(digestBytes(in.readAllBytes()));
        }
      }
      return HexFormat.of().formatHex(digest.digest());
    } catch (NoSuchAlgorithmException failure) { throw new AssertionError(failure); }
  }
  private static byte[] digestBytes(byte[] bytes) throws NoSuchAlgorithmException {
    return MessageDigest.getInstance("SHA-256").digest(bytes);
  }
  private static final class Unresolved extends RuntimeException {
    private static final long serialVersionUID = 1L;
    Unresolved(String message) { super(message); }
  }
  private record Binding(VariableElement symbol, long position, int node) { }
  private record Allocation(Tree tree, int origin, int node, String type, boolean primitive, List<Long> loops) { }
  private record Exposure(String reason, int node) { }

  private static final class Builder {
    final CompilationUnitTree unit; final Trees trees; final TreePath methodPath; final MethodTree method;
    final SourceLiveness.Limits limits; final M3ProgressMonitor monitor;
    final IdentityHashMap<Tree, TreePath> paths = new IdentityHashMap<>();
    final Map<Element, Integer> variables = new LinkedHashMap<>();
    final List<Binding> bindings = new ArrayList<>(); final List<Allocation> sites = new ArrayList<>();
    final IdentityHashMap<Tree, Integer> siteNodes = new IdentityHashMap<>();
    final List<BitSet> seeds = new ArrayList<>(); final List<TreeSet<Integer>> constraints = new ArrayList<>();
    final List<Exposure> exposures = new ArrayList<>();
    int external; long edgeCount;
    Builder(CompilationUnitTree unit, Trees trees, TreePath path, SourceLiveness.Limits limits, M3ProgressMonitor monitor) {
      this.unit = unit; this.trees = trees; methodPath = path; method = (MethodTree) path.getLeaf();
      this.limits = limits; this.monitor = monitor;
    }
    long position(Tree tree) { return trees.getSourcePositions().getStartPosition(unit, tree); }
    Element element(Tree tree) { TreePath path = paths.get(tree); return path == null ? null : trees.getElement(path); }
    TypeMirror type(Tree tree) { TreePath path = paths.get(tree); return path == null ? null : trees.getTypeMirror(path); }
    static boolean reference(TypeMirror type) {
      return type != null && switch (type.getKind()) {
        case ARRAY, DECLARED, TYPEVAR, WILDCARD, INTERSECTION, NULL -> true;
        default -> false;
      };
    }
    int node() {
      check(monitor);
      if (seeds.size() >= limits.maxNodes()) throw new Unresolved("constraint vertex budget exceeded");
      int id = seeds.size(); seeds.add(new BitSet()); constraints.add(new TreeSet<>()); return id;
    }
    MethodReferences build() {
      String source = unit.getSourceFile().getName(); Element symbol = trees.getElement(methodPath);
      String signature = symbol.getEnclosingElement() + "#" + symbol;
      try {
        if (method.getBody() == null || method.getReturnType() == null) throw new Unresolved("body/constructor effects unavailable");
        if (limits.maxSlots() < 1) throw new Unresolved("unknown-origin bit needs capacity");
        external = node(); seeds.get(external).set(0); index(); collect();
        int width = sites.size() + 1;
        if ((long) seeds.size() * ((width + 63L) / 64L) > limits.maxWordsPerMatrix()) throw new Unresolved("origin matrix budget exceeded");
        BitSet[] use = seeds.toArray(BitSet[]::new), def = new BitSet[use.length];
        Arrays.setAll(def, i -> new BitSet());
        int[][] edges = constraints.stream().map(s -> s.stream().mapToInt(Integer::intValue).toArray()).toArray(int[][]::new);
        // Same monotone union equation as backward may-liveness, with no killing definitions.
        var solved = Liveness.analyze(use, def, edges, width, monitor);
        List<Alias> aliases = bindings.stream().map(b -> new Alias(b.symbol().getSimpleName().toString(),
            b.symbol().asType().toString(), b.position(), solved.liveIn(b.node()))).toList();
        List<Site> allocations = new ArrayList<>();
        for (Allocation site : sites) {
          check(monitor); List<String> reasons = new ArrayList<>();
          for (Exposure exposure : exposures) if (solved.isLiveIn(exposure.node(), site.origin()) || solved.isLiveIn(exposure.node(), 0))
            reasons.add(exposure.reason() + (solved.isLiveIn(exposure.node(), 0) ? "[unknown-origin]" : ""));
          allocations.add(new Site(site.origin(), position(site.tree()), site.type(), site.primitive(), site.loops(),
              reasons.stream().distinct().sorted().toList()));
        }
        return new MethodReferences(source, signature, position(method), aliases, allocations, List.of());
      } catch (Unresolved failure) {
        return new MethodReferences(source, signature, position(method), List.of(), List.of(), List.of(failure.getMessage()));
      }
    }
    void index() {
      paths.put(method, methodPath);
      new TreePathScanner<Void, Void>() {
        int depth;
        @Override public Void scan(Tree tree, Void p) {
          check(monitor); if (tree == null) return null;
          if (++depth > limits.maxNesting()) throw new Unresolved("syntax nesting budget exceeded");
          try {
            paths.put(tree, new TreePath(getCurrentPath(), tree));
            if (tree instanceof ClassTree || tree instanceof LambdaExpressionTree || tree instanceof MemberReferenceTree
                || tree instanceof BindingPatternTree) throw new Unresolved("capture/pattern model unavailable");
            return super.scan(tree, p);
          } finally { depth--; }
        }
        @Override public Void visitVariable(VariableTree tree, Void p) {
          Element e = trees.getElement(getCurrentPath());
          if (e instanceof VariableElement v && reference(v.asType())) {
            int n = node(); variables.put(e, n); bindings.add(new Binding(v, position(tree), n));
            if (v.getKind() == ElementKind.PARAMETER || v.getKind() == ElementKind.EXCEPTION_PARAMETER) seeds.get(n).set(0);
          }
          return super.visitVariable(tree, p);
        }
        void allocation(Tree tree) {
          if (sites.size() + 1 >= limits.maxSlots()) throw new Unresolved("allocation-origin budget exceeded");
          int n = node(), origin = sites.size() + 1; seeds.get(n).set(origin); siteNodes.put(tree, n);
          TypeMirror t = trees.getTypeMirror(getCurrentPath()); List<Long> loops = new ArrayList<>();
          for (TreePath path = getCurrentPath().getParentPath(); path != null && path != methodPath; path = path.getParentPath()) {
            if (path.getLeaf() instanceof ForLoopTree || path.getLeaf() instanceof WhileLoopTree
                || path.getLeaf() instanceof DoWhileLoopTree || path.getLeaf() instanceof EnhancedForLoopTree) loops.add(position(path.getLeaf()));
          }
          Collections.reverse(loops);
          sites.add(new Allocation(tree, origin, n, t.toString(), t instanceof ArrayType a && a.getComponentType().getKind().isPrimitive(), List.copyOf(loops)));
        }
        @Override public Void visitNewArray(NewArrayTree tree, Void p) { allocation(tree); return super.visitNewArray(tree, p); }
        @Override public Void visitNewClass(NewClassTree tree, Void p) { allocation(tree); return super.visitNewClass(tree, p); }
      }.scan(methodPath, null);
    }
    void link(int target, Collection<Integer> sources) {
      for (int source : sources) {
        check(monitor);
        if (!constraints.get(target).contains(source)) {
          if (edgeCount >= limits.maxWordsPerMatrix()) throw new Unresolved("constraint edge budget exceeded");
          constraints.get(target).add(source); edgeCount++;
        }
      }
    }
    Set<Integer> sources(Tree expression) {
      TreeSet<Integer> found = new TreeSet<>();
      if (expression == null) return found;
      Tree value = expression;
      while (value instanceof ParenthesizedTree || value instanceof TypeCastTree) {
        value = value instanceof ParenthesizedTree p ? p.getExpression() : ((TypeCastTree) value).getExpression();
      }
      if (type(value) != null && type(value).getKind() == TypeKind.NULL) return found;
      if (!reference(type(expression))) return found;
      new TreeScanner<Void, Void>() {
        @Override public Void scan(Tree tree, Void p) {
          check(monitor); if (tree == null) return null;
          Integer site = siteNodes.get(tree); if (site != null) found.add(site);
          if (tree instanceof MethodInvocationTree || tree instanceof ArrayAccessTree || tree instanceof LiteralTree
              || tree instanceof MemberSelectTree) {
            if (reference(type(tree)) && type(tree).getKind() != TypeKind.NULL) found.add(external);
          }
          return super.scan(tree, p);
        }
        @Override public Void visitIdentifier(IdentifierTree tree, Void p) {
          Element e = element(tree); Integer local = variables.get(e);
          if (local != null) found.add(local);
          else if (e instanceof VariableElement v && reference(v.asType())) {
            if (v.getKind() == ElementKind.LOCAL_VARIABLE || v.getKind() == ElementKind.PARAMETER
                || v.getKind() == ElementKind.EXCEPTION_PARAMETER) throw new Unresolved("captured enclosing reference");
            found.add(external);
          }
          return null;
        }
      }.scan(expression, null);
      if (found.isEmpty()) found.add(external);
      return found;
    }
    void assign(Integer target, ExpressionTree expression) {
      if (target == null || expression == null) return;
      if (type(expression) != null && type(expression).getKind().isPrimitive()) link(target, List.of(external));
      else link(target, sources(expression));
    }
    void expose(String kind, Tree at, Tree value) {
      Set<Integer> sources = sources(value); if (sources.isEmpty()) return;
      int n = node(); link(n, sources); exposures.add(new Exposure(kind + "@" + position(at), n));
    }
    Tree unwrap(Tree tree) { while (tree instanceof ParenthesizedTree p) tree = p.getExpression(); return tree; }
    void collect() {
      new TreeScanner<Void, Void>() {
        @Override public Void scan(Tree tree, Void p) { check(monitor); return super.scan(tree, p); }
        @Override public Void visitVariable(VariableTree tree, Void p) {
          assign(variables.get(element(tree)), tree.getInitializer()); return super.visitVariable(tree, p);
        }
        @Override public Void visitAssignment(AssignmentTree tree, Void p) {
          Integer target = variables.get(element(unwrap(tree.getVariable())));
          if (target != null) assign(target, tree.getExpression()); else expose("heap-store", tree, tree.getExpression());
          return super.visitAssignment(tree, p);
        }
        @Override public Void visitCompoundAssignment(CompoundAssignmentTree tree, Void p) {
          if (reference(type(tree.getVariable()))) {
            expose("reference-conversion", tree, tree.getVariable()); expose("reference-conversion", tree, tree.getExpression());
            Integer target = variables.get(element(unwrap(tree.getVariable()))); if (target != null) link(target, List.of(external));
          }
          return super.visitCompoundAssignment(tree, p);
        }
        @Override public Void visitReturn(ReturnTree tree, Void p) { expose("return", tree, tree.getExpression()); return super.visitReturn(tree, p); }
        @Override public Void visitThrow(ThrowTree tree, Void p) { expose("throw", tree, tree.getExpression()); return super.visitThrow(tree, p); }
        @Override public Void visitMethodInvocation(MethodInvocationTree tree, Void p) {
          for (ExpressionTree arg : tree.getArguments()) expose("call-argument", tree, arg);
          if (tree.getMethodSelect() instanceof MemberSelectTree select) expose("call-receiver", tree, select.getExpression());
          return super.visitMethodInvocation(tree, p);
        }
        @Override public Void visitNewClass(NewClassTree tree, Void p) {
          int n = node(); link(n, List.of(siteNodes.get(tree))); exposures.add(new Exposure("constructor-unknown@" + position(tree), n));
          for (ExpressionTree arg : tree.getArguments()) expose("constructor-argument", tree, arg);
          expose("enclosing-instance", tree, tree.getEnclosingExpression()); return super.visitNewClass(tree, p);
        }
        @Override public Void visitNewArray(NewArrayTree tree, Void p) {
          if (tree.getInitializers() != null) for (ExpressionTree value : tree.getInitializers()) expose("array-initializer", tree, value);
          return super.visitNewArray(tree, p);
        }
        @Override public Void visitEnhancedForLoop(EnhancedForLoopTree tree, Void p) {
          Integer target = variables.get(element(tree.getVariable()));
          assign(target, tree.getExpression());
          if (target != null) link(target, List.of(external)); // Element origin is not the container origin.
          expose("implicit-traversal", tree, tree.getExpression());
          return super.visitEnhancedForLoop(tree, p);
        }
        @Override public Void visitSynchronized(SynchronizedTree tree, Void p) {
          expose("monitor-observation", tree, tree.getExpression()); return super.visitSynchronized(tree, p);
        }
        @Override public Void visitAssert(AssertTree tree, Void p) {
          expose("assert-detail", tree, tree.getDetail()); return super.visitAssert(tree, p);
        }
        @Override public Void visitBinary(BinaryTree tree, Void p) {
          if (reference(type(tree))) { expose("reference-conversion", tree, tree.getLeftOperand()); expose("reference-conversion", tree, tree.getRightOperand()); }
          return super.visitBinary(tree, p);
        }
      }.scan(method.getBody(), null);
    }
  }
}
