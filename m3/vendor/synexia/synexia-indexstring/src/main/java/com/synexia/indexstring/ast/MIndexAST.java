// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.ast;

import com.sun.source.tree.Tree;
import com.synexia.indexstring.MIndexString;
import com.synexia.indexstring.MatIndexString;
import com.synexia.indexstring.grammar.MIndexFormalLanguageSpec;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;
import javax.lang.model.element.Modifier;

/**
 * Compact immutable handle for a canonical Java AST atom.
 *
 * <p>The object itself carries only a pool reference and a stable integer handle. Node kind,
 * label, flags, ordered children and all precomputed metadata live in flat primitive arenas owned
 * by {@link MIndexASTPool}. Source positions intentionally do not participate in atom identity;
 * they live in {@link MIndexASTDocument} occurrence tables so identical subtrees can be shared
 * across files and positions.</p>
 */
public final class MIndexAST {
  private final MIndexASTPool pool;
  private final int handle;

  MIndexAST(MIndexASTPool pool, int handle) {
    this.pool = Objects.requireNonNull(pool, "pool");
    this.handle = handle;
  }

  public MIndexASTPool pool() {
    return pool;
  }

  public int handle() {
    return handle;
  }

  /** One immutable process-level language spec; never loaded per node or source file. */
  public MIndexLanguageSpec spec() {
    return pool.spec();
  }

  /** Rule ID is exactly the Tree.Kind ordinal; no per-node rule field exists. */
  public int ruleIndex() {
    return kind().ordinal();
  }

  public MIndexLanguageRule rule() {
    return spec().rule(ruleIndex());
  }

  /** Universal language-neutral AST specification shared with every MIndexLang. */
  public MIndexASTSpecPrecompute astSpec() {
    return spec().astSpec();
  }

  /** Process-level grammar production plan; no plan reference is retained by this AST atom. */
  public MIndexASTProductionPlans astProductionPlans() {
    return astSpec().productionPlans();
  }

  /** Process-level predictive production dispatch; no dispatch state is retained by this atom. */
  public MIndexASTProductionDispatch astProductionDispatch() {
    return astSpec().productionDispatch();
  }

  /** Process-level formal expression grammar reachable without adding AST instance state. */
  public MIndexFormalLanguageSpec formalExpressionSpec() {
    return spec().formalExpressionSpec();
  }

  public boolean is(MIndexSyntaxCategory category) {
    return rule().is(Objects.requireNonNull(category, "category"));
  }

  public MIndexLabelPolicy labelPolicy() {
    return rule().labelPolicy();
  }

  public MIndexFlagPolicy flagPolicy() {
    return rule().flagPolicy();
  }

  public MIndexOperatorRule operatorRule() {
    return rule().operator();
  }

  public boolean isOperator() {
    return operatorRule().isOperator();
  }

  public int precedence() {
    return operatorRule().precedence();
  }

  public MIndexAssociativity associativity() {
    return operatorRule().associativity();
  }

  public int minimumLanguageRelease() {
    return rule().minimumRelease();
  }

  public boolean isPreviewRule() {
    return rule().preview();
  }

  /**
   * Tests one Java modifier bit on a MODIFIERS atom.
   *
   * <p>Other AST kinds reuse the flags lane for kind-specific compact state, so treating those bits
   * as modifiers is rejected rather than silently misinterpreted.</p>
   */
  public boolean hasModifier(Modifier modifier) {
    if (flagPolicy() != MIndexFlagPolicy.MODIFIER_BITS) {
      throw new IllegalStateException("this AST rule does not use modifier bits");
    }
    return spec().hasModifier(flags(), Objects.requireNonNull(modifier, "modifier"));
  }

  public boolean booleanFlag() {
    if (flagPolicy() != MIndexFlagPolicy.BOOLEAN) {
      throw new IllegalStateException("this AST rule does not use a boolean flag");
    }
    return flags() != 0L;
  }

  /**
   * Returns an enum-mode ordinal, or -1 when the compact lane contains the zero/unset sentinel.
   */
  public int enumFlagOrdinal() {
    if (flagPolicy() != MIndexFlagPolicy.ENUM_ORDINAL_PLUS_ONE) {
      throw new IllegalStateException("this AST rule does not use an enum ordinal flag");
    }
    long encoded = flags();
    return encoded == 0L ? -1 : Math.toIntExact(encoded - 1L);
  }

  public Tree.Kind kind() {
    return pool.kind(handle);
  }

  public long flags() {
    return pool.flags(handle);
  }

  public boolean hasLabel() {
    return pool.labelHandle(handle) != 0;
  }

  public Optional<MatIndexString> label() {
    int labelHandle = pool.labelHandle(handle);
    return labelHandle == 0
        ? Optional.empty()
        : Optional.of(pool.labels().value(labelHandle));
  }

  /**
   * Returns the atom label through the compact MIndexString view.
   *
   * <p>This allocates only the tiny MIndexString wrapper. The underlying immutable token storage is
   * shared with the pool's canonical MatIndexString label.</p>
   */
  public Optional<MIndexString> labelMIndexString() {
    return label().map(value -> new MIndexString(value.toTuple()));
  }

  public int childCount() {
    return pool.childCount(handle);
  }

  public MIndexAST childAt(int ordinal) {
    return pool.value(pool.childHandle(handle, ordinal));
  }

  public IntStream childHandles() {
    return IntStream.range(0, childCount()).map(i -> pool.childHandle(handle, i));
  }

  /** Exact canonical content hash: kind + flags + label + ordered child contents. */
  public long exactHash64() {
    return pool.exactHash64(handle);
  }

  /** Rename-insensitive shape hash: kind + flags + ordered child shapes. */
  public long structuralHash64() {
    return pool.structuralHash64(handle);
  }

  /**
   * Normalized logic hash.
   *
   * <p>Identifier/member/class/method names are ignored while literal and primitive-type labels are
   * retained. This is a fast candidate key, not proof of semantic equivalence.</p>
   */
  public long logicHash64() {
    return pool.logicHash64(handle);
  }

  /** Locality-sensitive candidate fingerprint for approximate subtree matching. */
  public long simHash64() {
    return pool.simHash64(handle);
  }

  /** Primitive candidate identity for cross-file structural work reuse. */
  public MIndexASTReuseKey reuseKey() {
    return MIndexASTReuseKey.from(this);
  }

  /** Bloom-like subtree kind signal. False is definitive; true may be a collision. */
  public long kindSignal64() {
    return pool.kindSignal64(handle);
  }

  public int expandedNodeCount() {
    return pool.expandedNodeCount(handle);
  }

  public int depth() {
    return pool.depth(handle);
  }

  public int expandedLeafCount() {
    return pool.expandedLeafCount(handle);
  }

  public boolean mayContainKind(Tree.Kind kind) {
    return pool.mayContainKind(handle, Objects.requireNonNull(kind, "kind"));
  }

  public boolean containsKind(Tree.Kind kind) {
    return pool.containsKind(handle, Objects.requireNonNull(kind, "kind"));
  }

  public boolean contentEquals(MIndexAST other) {
    Objects.requireNonNull(other, "other");
    return pool.contentEquals(handle, other.pool, other.handle);
  }

  public boolean structurallyEquals(MIndexAST other) {
    Objects.requireNonNull(other, "other");
    return pool.structurallyEquals(handle, other.pool, other.handle);
  }

  public boolean normalizedLogicEquals(MIndexAST other) {
    Objects.requireNonNull(other, "other");
    return pool.normalizedLogicEquals(handle, other.pool, other.handle);
  }

  /** Computes a stable SHA-256 tree digest on demand without retaining 32 bytes per atom. */
  public byte[] sha256() {
    return pool.sha256(handle);
  }

  @Override
  public boolean equals(Object other) {
    return this == other
        || other instanceof MIndexAST that && pool == that.pool && handle == that.handle;
  }

  @Override
  public int hashCode() {
    return 31 * System.identityHashCode(pool) + handle;
  }

  @Override
  public String toString() {
    return "MIndexAST["
        + handle
        + ","
        + kind()
        + (hasLabel() ? ",label=" + label().orElseThrow().materialize() : "")
        + ",children="
        + childCount()
        + "]";
  }
}
