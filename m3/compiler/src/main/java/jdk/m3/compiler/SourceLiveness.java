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
import java.util.function.IntUnaryOperator;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;

/**
 * Conservative source-local may-liveness in the existing attributed compiler pipeline.
 * This is NOT heap reachability, ownership, alias, reclamation, or source-mutation authority.
 * Unsupported constructs/budgets yield an explicit unresolved method, never a partial CFG proof.
 */
public final class SourceLiveness {
  private SourceLiveness() {}

  /** Configurable admission budgets, not semantic limits on program decomposition. */
  public record Limits(int maxNodes, int maxSlots, int maxNesting, long maxWordsPerMatrix) {
    public Limits {
      if (maxNodes < 2 || maxSlots < 0 || maxNesting < 1 || maxWordsPerMatrix < 1) {
        throw new IllegalArgumentException("positive analysis budgets required");
      }
    }
    public static Limits defaults() { return new Limits(10_000, 4096, 256, 262_144); }
  }

  public record Slot(int index, String name, String type, long position) {}
  public record Node(String kind, long position, BitSet use, BitSet def, int[] successors) {
    public Node {
      Objects.requireNonNull(kind, "kind");
      use = (BitSet) Objects.requireNonNull(use, "use").clone();
      def = (BitSet) Objects.requireNonNull(def, "def").clone();
      successors = Objects.requireNonNull(successors, "successors").clone();
    }
    @Override public BitSet use() { return (BitSet) use.clone(); }
    @Override public BitSet def() { return (BitSet) def.clone(); }
    @Override public int[] successors() { return successors.clone(); }
  }
  public record MethodFlow(String source, String signature, long position, List<Slot> slots,
                           List<Node> nodes, int entry, int normalExit, int exceptionalExit,
                           Optional<Liveness.WideResult> liveness, List<String> unresolved) {
    public MethodFlow {
      slots = List.copyOf(slots); nodes = List.copyOf(nodes); unresolved = List.copyOf(unresolved);
      Objects.requireNonNull(liveness, "liveness");
      if (liveness.isPresent()) {
        if (!unresolved.isEmpty() || liveness.get().nodeCount() != nodes.size()
            || liveness.get().slotCount() != slots.size()) throw new IllegalArgumentException("flow dimensions");
        Objects.checkIndex(entry, nodes.size()); Objects.checkIndex(normalExit, nodes.size());
        Objects.checkIndex(exceptionalExit, nodes.size());
      } else if (!nodes.isEmpty() || unresolved.isEmpty()) throw new IllegalArgumentException("partial flow refused");
    }
    public boolean complete() { return liveness.isPresent(); }
    public boolean authorizesReclamation() { return false; }
    public boolean authorizesSourceMutation() { return false; }
  }
  public record Result(CompilerFactsAudit.Result facts, Limits limits, String proofKey,
                       List<MethodFlow> methods) {
    public Result {
      Objects.requireNonNull(facts, "facts"); Objects.requireNonNull(limits, "limits");
      Objects.requireNonNull(proofKey, "proofKey"); methods = List.copyOf(methods);
      if (!facts.baselineCompiles() && !methods.isEmpty()) throw new IllegalArgumentException("unattributed facts");
    }
    public boolean authorizesReclamation() { return false; }
    public boolean authorizesSourceMutation() { return false; }
  }

  public static Result analyze(Map<String, String> sources, M3ProgressMonitor monitor) throws IOException {
    return analyze(sources, Limits.defaults(), monitor);
  }
  public static Result analyze(Map<String, String> sources, Limits limits, M3ProgressMonitor monitor) throws IOException {
    return analyzeModel(sources, limits, monitor, false);
  }

  /** Additive extended model. Legacy analyze overloads keep their v2 admission policy. */
  public static Result analyzeExtended(Map<String, String> sources, Limits limits,
                                        M3ProgressMonitor monitor) throws IOException {
    return analyzeModel(sources, limits, monitor, true);
  }

  public static Result analyzeExtended(Map<String, String> sources, M3ProgressMonitor monitor) throws IOException {
    return analyzeExtended(sources, Limits.defaults(), monitor);
  }

  private static Result analyzeModel(Map<String, String> sources, Limits limits,
                                     M3ProgressMonitor monitor, boolean extended) throws IOException {
    Objects.requireNonNull(limits, "limits"); check(monitor);
    List<MethodFlow> methods = new ArrayList<>();
    CompilerFactsAudit.Result facts = CompilerFactsAudit.analyze(sources, monitor, (unit, trees, progress) -> {
      new TreePathScanner<Void, Void>() {
        @Override public Void scan(Tree tree, Void unused) { check(progress); return super.scan(tree, unused); }
        @Override public Void visitMethod(MethodTree method, Void unused) {
          // javac inserts synthetic constructors; source-negative end positions are not source methods.
          if (trees.getSourcePositions().getEndPosition(unit, method) >= 0) {
            methods.add(new Builder(unit, trees, getCurrentPath(), limits, progress, extended).build());
          }
          return super.visitMethod(method, unused);
        }
      }.scan(unit, null);
    });
    methods.sort(Comparator.comparing(MethodFlow::source).thenComparingLong(MethodFlow::position)
        .thenComparing(MethodFlow::signature));
    check(monitor);
    return new Result(facts, limits, proofKey(facts.proofKey(), limits, extended), methods);
  }

  /** Slot-liveness transition, never a reference count or permission to release an object. */
  public record Boundary(int from, int to, BitSet noLongerLive) {
    public Boundary {
      if (from < 0 || to < 0) throw new IllegalArgumentException("negative edge");
      noLongerLive = (BitSet) Objects.requireNonNull(noLongerLive, "noLongerLive").clone();
    }
    @Override public BitSet noLongerLive() { return (BitSet) noLongerLive.clone(); }
    public boolean authorizesReclamation() { return false; }
  }

  /**
   * On-demand selected-source-CFG edge facts: (IN[from] | DEF[from]) minus IN[to].
   * Only nodes reachable from method.entry are returned; implicit VM temporaries are excluded.
   * Returned slots may still denote escaped/shared/returned objects. Never emit releases from this alone.
   */
  public static List<Boundary> boundaries(MethodFlow method, M3ProgressMonitor monitor) {
    Objects.requireNonNull(method, "method"); check(monitor);
    if (!method.complete()) throw new IllegalArgumentException("unresolved method has no boundary proof");
    Liveness.WideResult live = method.liveness().orElseThrow();
    int n = method.nodes().size();
    for (int i = 0; i < n; i++) {
      check(monitor);
      Node node = method.nodes().get(i);
      if (node.use.length() > live.slotCount() || node.def.length() > live.slotCount()) {
        throw new IllegalArgumentException("slot outside declared width");
      }
      BitSet out = new BitSet();
      for (int successor : node.successors) {
        Objects.checkIndex(successor, n);
        out.or(live.liveIn(successor));
      }
      BitSet in = (BitSet) out.clone(); in.andNot(node.def); in.or(node.use);
      if (!out.equals(live.liveOut(i)) || !in.equals(live.liveIn(i))) {
        throw new IllegalArgumentException("graph and liveness do not match");
      }
    }
    BitSet reachable = new BitSet(n);
    ArrayDeque<Integer> queue = new ArrayDeque<>(); queue.add(method.entry()); reachable.set(method.entry());
    while (!queue.isEmpty()) {
      check(monitor);
      for (int target : method.nodes().get(queue.removeFirst()).successors) {
        if (!reachable.get(target)) { reachable.set(target); queue.addLast(target); }
      }
    }
    List<Boundary> result = new ArrayList<>();
    for (int from = reachable.nextSetBit(0); from >= 0; from = reachable.nextSetBit(from + 1)) {
      check(monitor);
      BitSet before = live.liveIn(from); before.or(method.nodes().get(from).def);
      for (int to : Arrays.stream(method.nodes().get(from).successors).sorted().distinct().toArray()) {
        BitSet ended = (BitSet) before.clone(); ended.andNot(live.liveIn(to));
        if (!ended.isEmpty()) result.add(new Boundary(from, to, ended));
      }
    }
    check(monitor); return List.copyOf(result);
  }

  private static String proofKey(String base, Limits limits, boolean extended) {
    try {
      // Base is fixed-length SHA-256; limits are unambiguous bounded integral fields.
      String input = base + (extended ? "\nsource-local-flow-v4\n" : "\nsource-local-flow-v2\n") + limits.maxNodes() + ":" + limits.maxSlots()
          + ":" + limits.maxNesting() + ":" + limits.maxWordsPerMatrix();
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) { throw new AssertionError(e); }
  }
  private static void check(M3ProgressMonitor monitor) { if (monitor != null) monitor.checkCanceled(); }
  private static final class Unresolved extends RuntimeException {
    private static final long serialVersionUID = 1L;
    Unresolved(String message) { super(message); }
  }
  private record Jump(String label, int breakTarget, int continueTarget, boolean unlabelled) {}
  private record Context(int returnTarget, int[] exceptions, List<Jump> jumps) {
    Context withJump(Jump jump) {
      List<Jump> copy = new ArrayList<>(); copy.add(jump); copy.addAll(jumps);
      return new Context(returnTarget, exceptions, List.copyOf(copy));
    }
    Context withExceptions(int[] targets) { return new Context(returnTarget, targets, jumps); }
    Context through(IntUnaryOperator route) {
      return new Context(route.applyAsInt(returnTarget), Arrays.stream(exceptions).map(route).toArray(),
          jumps.stream().map(j -> new Jump(j.label(), route.applyAsInt(j.breakTarget()),
              j.continueTarget() < 0 ? -1 : route.applyAsInt(j.continueTarget()), j.unlabelled())).toList());
    }
    int jump(String label, boolean continuing) {
      for (Jump jump : jumps) {
        if (label == null ? jump.unlabelled() : label.equals(jump.label())) {
          int target = continuing ? jump.continueTarget() : jump.breakTarget();
          if (target >= 0) return target;
          if (label != null) break;
        }
      }
      throw new Unresolved("unresolved " + (continuing ? "continue" : "break") + " target");
    }
  }
  private static final class MutableNode {
    final String kind; final long position; final BitSet use; final BitSet def;
    int[] successors;
    MutableNode(String kind, long position, BitSet use, BitSet def, int[] successors) {
      this.kind = kind; this.position = position; this.use = use; this.def = def; this.successors = successors;
    }
    Node freeze() { return new Node(kind, position, use, def, successors); }
  }

  private static final class Builder {
    final CompilationUnitTree unit; final Trees trees; final TreePath path; final MethodTree method;
    final Limits limits; final M3ProgressMonitor monitor; final boolean extended;
    final LinkedHashMap<Element, Integer> symbols = new LinkedHashMap<>();
    final List<Slot> slots = new ArrayList<>();
    final IdentityHashMap<Tree, TreePath> paths = new IdentityHashMap<>();
    final List<MutableNode> nodes = new ArrayList<>();
    int depth;
    Builder(CompilationUnitTree unit, Trees trees, TreePath path, Limits limits, M3ProgressMonitor monitor, boolean extended) {
      this.unit = unit; this.trees = trees; this.path = path; this.method = (MethodTree) path.getLeaf();
      this.limits = limits; this.monitor = monitor; this.extended = extended;
    }
    MethodFlow build() {
      String source = unit.getSourceFile().getName();
      Element e = trees.getElement(path);
      String signature = e == null ? method.getName().toString() : e.getEnclosingElement() + "#" + e;
      long position = position(method);
      try {
        if (method.getBody() == null) throw new Unresolved("abstract/native body unavailable");
        if (method.getReturnType() == null) {
          throw new Unresolved("constructor initialization requires a separate effects model");
        }
        if (method.getModifiers().getFlags().contains(Modifier.SYNCHRONIZED)) {
          throw new Unresolved("synchronized method requires monitor-lifetime modeling");
        }
        index();
        int normal = add("NORMAL_EXIT", null, bits(), bits());
        int exceptional = add("EXCEPTIONAL_EXIT", null, bits(), bits());
        Context context = new Context(normal, new int[]{exceptional}, List.of());
        int entry = statement(method.getBody(), normal, context, null);
        List<Node> frozen = nodes.stream().map(MutableNode::freeze).toList();
        Liveness.WideResult result = Liveness.analyze(
            frozen.stream().map(Node::use).toArray(BitSet[]::new),
            frozen.stream().map(Node::def).toArray(BitSet[]::new),
            frozen.stream().map(Node::successors).toArray(int[][]::new), slots.size(), monitor);
        return new MethodFlow(source, signature, position, slots, frozen, entry, normal, exceptional,
            Optional.of(result), List.of());
      } catch (Unresolved unresolved) {
        return new MethodFlow(source, signature, position, List.of(), List.of(), -1, -1, -1,
            Optional.empty(), List.of(unresolved.getMessage()));
      }
    }
    long position(Tree tree) { return tree == null ? -1 : trees.getSourcePositions().getStartPosition(unit, tree); }
    Element element(Tree tree) {
      TreePath p = paths.get(tree); return p == null ? null : trees.getElement(p);
    }
    void index() {
      paths.put(method, path);
      new TreePathScanner<Void, Void>() {
        int nesting;
        @Override public Void scan(Tree tree, Void unused) {
          check(monitor);
          if (tree == null) return null;
          if (++nesting > limits.maxNesting()) throw new Unresolved("syntax nesting budget exceeded");
          try {
            paths.put(tree, new TreePath(getCurrentPath(), tree));
            switch (tree.getKind()) {
              case CLASS, INTERFACE, ENUM, RECORD, ANNOTATION_TYPE, LAMBDA_EXPRESSION, MEMBER_REFERENCE ->
                  throw new Unresolved("closure/local class requires a capture model");
              case SWITCH, ENHANCED_FOR_LOOP -> {
                if (!extended) throw new Unresolved("unsupported control-flow construct: " + tree.getKind());
              }
              case SWITCH_EXPRESSION, YIELD, SYNCHRONIZED ->
                  throw new Unresolved("unsupported control-flow construct: " + tree.getKind());
              default -> { }
            }
            if (tree instanceof TryTree t && !t.getResources().isEmpty()) {
              throw new Unresolved("try-with-resources requires close/suppression modeling");
            }
            return super.scan(tree, unused);
          } finally { nesting--; }
        }
        @Override public Void visitVariable(VariableTree tree, Void unused) {
          Element variable = trees.getElement(getCurrentPath());
          if (variable instanceof VariableElement v && (v.getKind() == ElementKind.PARAMETER
              || v.getKind() == ElementKind.LOCAL_VARIABLE || v.getKind() == ElementKind.EXCEPTION_PARAMETER
              || v.getKind() == ElementKind.BINDING_VARIABLE)) {
            if (slots.size() >= limits.maxSlots()) throw new Unresolved("local slot budget exceeded");
            symbols.put(variable, slots.size());
            slots.add(new Slot(slots.size(), v.getSimpleName().toString(), v.asType().toString(), position(tree)));
          }
          return super.visitVariable(tree, unused);
        }
      }.scan(path, null);
    }
    BitSet bits() { return new BitSet(slots.size()); }
    BitSet definition(Tree tree) {
      Integer slot = symbols.get(element(tree));
      if (slot == null) throw new Unresolved("unresolved local definition");
      BitSet set = bits(); set.set(slot); return set;
    }
    BitSet reads(Tree tree) {
      BitSet set = bits();
      if (tree != null) new TreeScanner<Void, Void>() {
        @Override public Void scan(Tree t, Void unused) { check(monitor); return super.scan(t, unused); }
        @Override public Void visitIdentifier(IdentifierTree identifier, Void unused) {
          Element referenced = element(identifier);
          Integer slot = symbols.get(referenced);
          if (slot != null) set.set(slot);
          else if (referenced instanceof VariableElement v && (v.getKind() == ElementKind.PARAMETER
              || v.getKind() == ElementKind.LOCAL_VARIABLE || v.getKind() == ElementKind.EXCEPTION_PARAMETER
              || v.getKind() == ElementKind.BINDING_VARIABLE)) {
            throw new Unresolved("captured enclosing local requires a capture model");
          }
          return null;
        }
      }.scan(tree, null);
      return set;
    }
    int add(String kind, Tree tree, BitSet use, BitSet def, int... successors) {
      check(monitor);
      if (nodes.size() >= limits.maxNodes()
          || (long) (nodes.size() + 1) * ((slots.size() + 63L) / 64L) > limits.maxWordsPerMatrix()) {
        throw new Unresolved("CFG node/bit-matrix budget exceeded");
      }
      int id = nodes.size();
      nodes.add(new MutableNode(kind, position(tree), use, def, normalize(successors)));
      return id;
    }
    static int[] normalize(int[] values) { return Arrays.stream(values).sorted().distinct().toArray(); }
    static int[] union(int[] a, int... b) {
      int[] merged = Arrays.copyOf(a, a.length + b.length); System.arraycopy(b, 0, merged, a.length, b.length);
      return normalize(merged);
    }
    void edges(int node, int[] exceptions, int... successors) { nodes.get(node).successors = union(exceptions, successors); }
    int evaluate(String kind, Tree tree, int next, Context context) {
      return add(kind, tree, reads(tree), bits(), union(context.exceptions(), next));
    }
    int commit(Tree variable, Tree expression, int next, Context context) {
      int commit = add("COMMIT", variable, bits(), definition(variable), next);
      return evaluate("EVALUATE", expression, commit, context);
    }
    int expression(ExpressionTree expression, int next, Context context) {
      if (expression instanceof AssignmentTree assignment && symbols.containsKey(element(assignment.getVariable()))) {
        return commit(assignment.getVariable(), assignment.getExpression(), next, context);
      }
      Tree variable = null;
      if (expression instanceof CompoundAssignmentTree assignment) variable = assignment.getVariable();
      if (expression instanceof UnaryTree unary && switch (unary.getKind()) {
        case PREFIX_INCREMENT, POSTFIX_INCREMENT, PREFIX_DECREMENT, POSTFIX_DECREMENT -> true;
        default -> false;
      }) variable = unary.getExpression();
      if (variable != null && symbols.containsKey(element(variable))) {
        return commit(variable, expression, next, context);
      }
      // Nested/conditional assignments deliberately do NOT kill any prior local binding.
      return evaluate("EXPRESSION", expression, next, context);
    }
    int statement(StatementTree tree, int next, Context context, String label) {
      check(monitor);
      if (tree == null) return next;
      if (++depth > limits.maxNesting()) throw new Unresolved("CFG nesting budget exceeded");
      try {
        if (tree instanceof BlockTree block) {
          List<? extends StatementTree> statements = block.getStatements();
          for (int i = statements.size() - 1; i >= 0; i--) next = statement(statements.get(i), next, context, null);
          return next;
        }
        if (tree instanceof EmptyStatementTree) return next;
        if (tree instanceof VariableTree variable) {
          return variable.getInitializer() == null ? next : commit(variable, variable.getInitializer(), next, context);
        }
        if (tree instanceof ExpressionStatementTree expression) return expression(expression.getExpression(), next, context);
        if (tree instanceof IfTree branch) {
          int yes = statement(branch.getThenStatement(), next, context, null);
          int no = statement(branch.getElseStatement(), next, context, null);
          return add("IF", branch.getCondition(), reads(branch.getCondition()), bits(), union(context.exceptions(), yes, no));
        }
        if (tree instanceof WhileLoopTree loop) {
          int condition = add("WHILE", loop.getCondition(), reads(loop.getCondition()), bits());
          Context inner = context.withJump(new Jump(label, next, condition, true));
          int body = statement(loop.getStatement(), condition, inner, null);
          edges(condition, context.exceptions(), body, next);
          return condition;
        }
        if (tree instanceof DoWhileLoopTree loop) {
          int condition = add("DO_WHILE", loop.getCondition(), reads(loop.getCondition()), bits());
          Context inner = context.withJump(new Jump(label, next, condition, true));
          int body = statement(loop.getStatement(), condition, inner, null);
          edges(condition, context.exceptions(), body, next);
          return body;
        }
        if (tree instanceof ForLoopTree loop) {
          int condition = add("FOR", loop.getCondition(), reads(loop.getCondition()), bits());
          int update = condition;
          List<? extends ExpressionStatementTree> updates = loop.getUpdate();
          for (int i = updates.size() - 1; i >= 0; i--) update = expression(updates.get(i).getExpression(), update, context);
          Context inner = context.withJump(new Jump(label, next, update, true));
          int body = statement(loop.getStatement(), update, inner, null);
          edges(condition, loop.getCondition() == null ? new int[0] : context.exceptions(),
              loop.getCondition() == null ? new int[]{body} : new int[]{body, next});
          int entry = condition;
          List<? extends StatementTree> initializers = loop.getInitializer();
          for (int i = initializers.size() - 1; i >= 0; i--) entry = statement(initializers.get(i), entry, context, null);
          return entry;
        }
        if (tree instanceof EnhancedForLoopTree loop) return enhancedFor(loop, next, context, label);
        if (tree instanceof SwitchTree branch) return switched(branch, next, context);
        if (tree instanceof BreakTree jump) return add("BREAK", tree, bits(), bits(),
            context.jump(jump.getLabel() == null ? null : jump.getLabel().toString(), false));
        if (tree instanceof ContinueTree jump) return add("CONTINUE", tree, bits(), bits(),
            context.jump(jump.getLabel() == null ? null : jump.getLabel().toString(), true));
        if (tree instanceof ReturnTree exit) return evaluate("RETURN", exit.getExpression(), context.returnTarget(), context);
        if (tree instanceof ThrowTree exit) return add("THROW", exit.getExpression(), reads(exit.getExpression()), bits(), context.exceptions());
        if (tree instanceof LabeledStatementTree labelled) {
          if (label != null) throw new Unresolved("nested labels need explicit target normalization");
          StatementTree body = labelled.getStatement(); String name = labelled.getLabel().toString();
          if (body instanceof ForLoopTree || body instanceof WhileLoopTree || body instanceof DoWhileLoopTree
              || (extended && body instanceof EnhancedForLoopTree)) {
            return statement(body, next, context, name);
          }
          return statement(body, next, context.withJump(new Jump(name, next, -1, false)), null);
        }
        if (tree instanceof TryTree tried) return tried(tried, next, context);
        if (tree instanceof AssertTree asserted) {
          // Assertion enablement is dynamic. Union reads, preserve all bindings, include exceptional exit.
          return evaluate("ASSERT", asserted, next, context);
        }
        throw new Unresolved("unsupported statement: " + tree.getKind());
      } finally { depth--; }
    }
    /** Hidden iterator/array holder is NOT a source local or an owning-reference proof. */
    int enhancedFor(EnhancedForLoopTree loop, int next, Context context, String label) {
      if (!extended) throw new Unresolved("enhanced-for requires extended model");
      boolean array = trees.getTypeMirror(paths.get(loop.getExpression())).getKind() == TypeKind.ARRAY;
      String prefix = array ? "ARRAY" : "ITERATOR";
      int condition = add(prefix + "_HAS_NEXT", loop, bits(), bits());
      Context inner = context.withJump(new Jump(label, next, condition, true));
      int body = statement(loop.getStatement(), condition, inner, null);
      int bind = add("ITERATION_BIND", loop.getVariable(), bits(), definition(loop.getVariable()), body);
      // Fetch/conversion may throw before the per-iteration variable assignment completes.
      int fetch = add(prefix + "_FETCH", loop.getVariable(), bits(), bits(), union(context.exceptions(), bind));
      edges(condition, context.exceptions(), fetch, next);
      // Evaluate the iterable expression exactly once; future steps access hidden traversal state.
      return evaluate(prefix + "_INIT", loop.getExpression(), condition, context);
    }

    /** Constant/null/default labels only; pattern bindings and guard effects remain unresolved. */
    int switched(SwitchTree branch, int next, Context context) {
      if (!extended) throw new Unresolved("switch statement requires extended model");
      Context inner = context.withJump(new Jump(null, next, -1, true));
      List<? extends CaseTree> cases = branch.getCases();
      int[] entries = new int[cases.size()];
      int fallThrough = next;
      boolean hasDefault = false;
      for (int i = cases.size() - 1; i >= 0; i--) {
        check(monitor);
        CaseTree arm = cases.get(i);
        if (arm.getGuard() != null) throw new Unresolved("switch guard needs a pattern/effects model");
        for (CaseLabelTree caseLabel : arm.getLabels()) {
          switch (caseLabel.getKind()) {
            case DEFAULT_CASE_LABEL -> hasDefault = true;
            case CONSTANT_CASE_LABEL -> { }
            default -> throw new Unresolved("switch pattern needs binding/guard modeling");
          }
        }
        int entry;
        if (arm.getCaseKind() == CaseTree.CaseKind.STATEMENT) {
          entry = fallThrough;
          List<? extends StatementTree> statements = arm.getStatements();
          for (int j = statements.size() - 1; j >= 0; j--) entry = statement(statements.get(j), entry, inner, null);
        } else if (arm.getBody() instanceof StatementTree statement) {
          entry = statement(statement, next, inner, null);
        } else if (arm.getBody() instanceof ExpressionTree expression) {
          entry = expression(expression, next, inner);
        } else throw new Unresolved("unsupported switch rule body");
        entries[i] = entry;
        fallThrough = entry;
      }
      // Selector evaluation chooses one arm. Exceptional route covers unboxing/null/match failure.
      // With no default retain a conservative unmatched-selector normal edge.
      int[] exits = hasDefault ? entries : union(entries, next);
      return add("SWITCH", branch.getExpression(), reads(branch.getExpression()), bits(), union(context.exceptions(), exits));
    }

    int tried(TryTree tried, int next, Context outer) {
      BlockTree finalizer = tried.getFinallyBlock();
      Map<Integer, Integer> routes = new LinkedHashMap<>();
      IntUnaryOperator route = target -> {
        if (finalizer == null || finalizer.getStatements().isEmpty()) return target;
        Integer known = routes.get(target); if (known != null) return known;
        int entry = statement(finalizer, target, outer, null);
        routes.put(target, entry); return entry;
      };
      // Only targets that predate this try are wrapped. Inner-loop breaks stay inside the try.
      Context afterFinally = outer.through(route);
      int normal = route.applyAsInt(next);
      List<Integer> handlers = new ArrayList<>();
      for (CatchTree handler : tried.getCatches()) {
        int body = statement(handler.getBlock(), normal, afterFinally, null);
        handlers.add(add("CATCH_BIND", handler.getParameter(), bits(), definition(handler.getParameter()), body));
      }
      int[] exceptions = union(afterFinally.exceptions(), handlers.stream().mapToInt(Integer::intValue).toArray());
      // Catch type/order is over-approximated, including an unmatched-exception route through finally.
      return statement(tried.getBlock(), normal, afterFinally.withExceptions(exceptions), null);
    }
  }
}
