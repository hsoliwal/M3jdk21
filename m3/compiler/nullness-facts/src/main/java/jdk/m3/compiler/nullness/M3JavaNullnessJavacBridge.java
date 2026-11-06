// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.compiler.nullness;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import java.util.Objects;
import java.util.function.ToIntFunction;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;

/**
 * Read-only, fail-closed bridge from javac's attributed Tree API to the M3
 * primitive nullness lattice. No new parser, Kotlin dependency, class rewriting
 * or compiler global state is introduced. This analyses only pure null tests
 * of locally bound Java identifiers, with optional instanceof type checks.
 *
 * <p>The caller must provide the authoritative attributed {@link TreePath},
 * {@link Trees}, an immutable symbol-to-slot mapping for exactly the given
 * {@code Facts.epoch()}, and a verified control-flow edge. A slot mapping is
 * NOT a compiler certificate. Rejects fields, array elements, method calls,
 * assignments, conditional expressions and any composite guard containing
 * unmodelled effects. The result is a candidate fact, never source mutation
 * authority. Facts must be invalidated by the consumer for later writes and
 * unmodelled control-flow edges.
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/982689e16763e54b01632407d64846fbea5719b6/synexia-m3-recipe/src/main/java/com/synexia/m3/recipe/M3JavaNullnessJavacBridge.java blob 2045b84846cf; recipe m3/compiler/nullness-facts/recipe/relocate.py v1
public final class M3JavaNullnessJavacBridge {
  private static final int MAX_GUARD_NODES = 96;
  private static final int MAX_BRANCH_OPERATORS = 12;
  private static final long MAX_FACT_WORK_WORDS = 2_000_000L;

  private M3JavaNullnessJavacBridge() {}

  /**
   * Refines a known edge of a pure Java condition. Unknown/unsafe conditions
   * yield the original facts without executing or rewriting user code.
   */
  public static M3JavaNullnessFlow.Facts refine(
      M3JavaNullnessFlow.Facts before,
      TreePath condition,
      boolean outcome,
      Trees trees,
      ToIntFunction<? super Element> slotOf) {
    Objects.requireNonNull(before, "before");
    Objects.requireNonNull(condition, "condition");
    Objects.requireNonNull(trees, "trees");
    Objects.requireNonNull(slotOf, "slotOf");
    if (!(condition.getLeaf() instanceof ExpressionTree)) {
      throw new IllegalArgumentException("condition must be an expression tree");
    }
    // A preliminary full-expression check is critical. Refining the first
    // subexpression of a guard whose later operand mutates the same local
    // would otherwise be unsound at the true/false exit edge.
    int[] budget = new int[2]; // nodes, short-circuit branch operators
    if (!pure(condition, trees, slotOf, before.slots(), budget)) return before;
    // Preserve the original conservative admission bound (including the
    // branch fanout estimate). The split transfer below computes both exits
    // once per AST subtree instead of recursively re-evaluating them.
    long words = Math.max(1L, ((long) before.slots() + 31L) >>> 5);
    if ((1L << budget[1]) * words > MAX_FACT_WORK_WORDS) return before;
    Edges result = split(before, condition, trees, slotOf);
    return outcome ? result.whenTrue() : result.whenFalse();
  }

  private static boolean pure(TreePath path, Trees trees,
      ToIntFunction<? super Element> slotOf, int slots, int[] budget) {
    if (++budget[0] > MAX_GUARD_NODES) return false;
    Tree leaf = path.getLeaf();
    return switch (leaf.getKind()) {
      case PARENTHESIZED -> pure(path(path,
          ((ParenthesizedTree) leaf).getExpression()), trees, slotOf, slots, budget);
      case LOGICAL_COMPLEMENT -> pure(path(path,
          ((UnaryTree) leaf).getExpression()), trees, slotOf, slots, budget);
      case CONDITIONAL_AND, CONDITIONAL_OR -> {
        if (++budget[1] > MAX_BRANCH_OPERATORS) yield false;
        BinaryTree binary = (BinaryTree) leaf;
        yield pure(path(path, binary.getLeftOperand()), trees, slotOf, slots, budget)
            && pure(path(path, binary.getRightOperand()), trees, slotOf, slots, budget);
      }
      case EQUAL_TO, NOT_EQUAL_TO -> nullComparedLocal(path, trees, slotOf, slots) >= 0;
      case INSTANCE_OF -> {
        InstanceOfTree check = (InstanceOfTree) leaf;
        yield localSlot(path(path, check.getExpression()), trees, slotOf, slots) >= 0;
      }
      case BOOLEAN_LITERAL -> true;
      default -> false;
    };
  }

  /**
   * Two edge results computed together, in a single traversal of the guard.
   * The old independently recursive true/false requests repeated identical
   * left-operand work under alternating &&/||; reuse the result instead.
   * This is local Boolean CFG dynamic programming, not a new parser or solver.
   */
  private record Edges(M3JavaNullnessFlow.Facts whenTrue,
                       M3JavaNullnessFlow.Facts whenFalse) {}

  private static Edges split(
      M3JavaNullnessFlow.Facts before, TreePath path,
      Trees trees, ToIntFunction<? super Element> slotOf) {
    Tree leaf = path.getLeaf();
    return switch (leaf.getKind()) {
      case PARENTHESIZED -> split(before, path(path,
          ((ParenthesizedTree) leaf).getExpression()), trees, slotOf);
      case LOGICAL_COMPLEMENT -> {
        Edges inner = split(before, path(path,
            ((UnaryTree) leaf).getExpression()), trees, slotOf);
        yield new Edges(inner.whenFalse(), inner.whenTrue());
      }
      case EQUAL_TO, NOT_EQUAL_TO -> {
        int slot = nullComparedLocal(path, trees, slotOf, before.slots());
        M3JavaNullnessFlow.Facts nonNull = M3JavaNullnessFlow.assumeNonNull(before, slot);
        M3JavaNullnessFlow.Facts isNull = M3JavaNullnessFlow.assumeNull(before, slot);
        yield leaf.getKind() == Tree.Kind.NOT_EQUAL_TO
            ? new Edges(nonNull, isNull) : new Edges(isNull, nonNull);
      }
      case INSTANCE_OF -> {
        int slot = localSlot(path(path,
            ((InstanceOfTree) leaf).getExpression()), trees, slotOf, before.slots());
        yield new Edges(M3JavaNullnessFlow.assumeNonNull(before, slot), before);
      }
      case CONDITIONAL_AND -> {
        BinaryTree binary = (BinaryTree) leaf;
        Edges left = split(before, path(path, binary.getLeftOperand()), trees, slotOf);
        Edges right = split(left.whenTrue(), path(path, binary.getRightOperand()),
            trees, slotOf);
        yield new Edges(right.whenTrue(),
            M3JavaNullnessFlow.join(left.whenFalse(), right.whenFalse()));
      }
      case CONDITIONAL_OR -> {
        BinaryTree binary = (BinaryTree) leaf;
        Edges left = split(before, path(path, binary.getLeftOperand()), trees, slotOf);
        Edges right = split(left.whenFalse(), path(path, binary.getRightOperand()),
            trees, slotOf);
        yield new Edges(M3JavaNullnessFlow.join(left.whenTrue(), right.whenTrue()),
            right.whenFalse());
      }
      case BOOLEAN_LITERAL -> {
        boolean value = Boolean.TRUE.equals(((LiteralTree) leaf).getValue());
        M3JavaNullnessFlow.Facts unreachable =
            M3JavaNullnessFlow.unreachable(before.epoch(), before.slots());
        yield value ? new Edges(before, unreachable) : new Edges(unreachable, before);
      }
      default -> new Edges(before, before); // Preflight refused unmodelled syntax.
    };
  }

  private static int nullComparedLocal(TreePath path, Trees trees,
      ToIntFunction<? super Element> slotOf, int slots) {
    BinaryTree binary = (BinaryTree) path.getLeaf();
    if (binary.getLeftOperand().getKind() == Tree.Kind.NULL_LITERAL) {
      return localSlot(path(path, binary.getRightOperand()), trees, slotOf, slots);
    }
    if (binary.getRightOperand().getKind() == Tree.Kind.NULL_LITERAL) {
      return localSlot(path(path, binary.getLeftOperand()), trees, slotOf, slots);
    }
    return -1;
  }

  private static int localSlot(TreePath path, Trees trees,
      ToIntFunction<? super Element> slotOf, int slots) {
    if (!(path.getLeaf() instanceof IdentifierTree)) return -1;
    Element symbol = trees.getElement(path);
    if (symbol == null) return -1;
    ElementKind kind = symbol.getKind();
    if (kind != ElementKind.LOCAL_VARIABLE && kind != ElementKind.PARAMETER
        && kind != ElementKind.EXCEPTION_PARAMETER && kind != ElementKind.RESOURCE_VARIABLE
        && kind != ElementKind.BINDING_VARIABLE) {
      return -1;
    }
    int slot = slotOf.applyAsInt(symbol);
    return slot >= 0 && slot < slots ? slot : -1;
  }

  private static TreePath path(TreePath parent, Tree child) {
    return new TreePath(parent, child);
  }
}
