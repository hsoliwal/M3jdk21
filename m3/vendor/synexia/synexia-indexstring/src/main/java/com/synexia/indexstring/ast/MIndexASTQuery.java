// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.ast;

import com.sun.source.tree.Tree;
import com.synexia.indexstring.MIndexString;
import java.util.Objects;

/**
 * Immutable query compiled against one MIndexAST pool.
 *
 * <p>Cheap precomputed tests run before exact subtree-kind verification. Hash and SimHash matches
 * are candidate filters only; exact content/structure comparisons remain available on MIndexAST.</p>
 */
public final class MIndexASTQuery {
  private final MIndexASTPool pool;
  private final Tree.Kind rootKind;
  private final boolean labelSpecified;
  private final int labelHandle;
  private final long requiredFlags;
  private final long forbiddenFlags;
  private final long requiredCategoryMask;
  private final boolean exactHashSpecified;
  private final long exactHash;
  private final boolean structuralHashSpecified;
  private final long structuralHash;
  private final boolean logicHashSpecified;
  private final long logicHash;
  private final boolean simHashSpecified;
  private final long simHash;
  private final int maxHammingDistance;
  private final int minExpandedNodes;
  private final int maxExpandedNodes;
  private final int minDepth;
  private final int maxDepth;
  private final int minExpandedLeaves;
  private final int maxExpandedLeaves;
  private final Tree.Kind requiredSubtreeKind;

  private MIndexASTQuery(Builder builder) {
    pool = builder.pool;
    rootKind = builder.rootKind;
    labelSpecified = builder.labelSpecified;
    labelHandle = builder.labelHandle;
    requiredFlags = builder.requiredFlags;
    forbiddenFlags = builder.forbiddenFlags;
    requiredCategoryMask = builder.requiredCategoryMask;
    exactHashSpecified = builder.exactHashSpecified;
    exactHash = builder.exactHash;
    structuralHashSpecified = builder.structuralHashSpecified;
    structuralHash = builder.structuralHash;
    logicHashSpecified = builder.logicHashSpecified;
    logicHash = builder.logicHash;
    simHashSpecified = builder.simHashSpecified;
    simHash = builder.simHash;
    maxHammingDistance = builder.maxHammingDistance;
    minExpandedNodes = builder.minExpandedNodes;
    maxExpandedNodes = builder.maxExpandedNodes;
    minDepth = builder.minDepth;
    maxDepth = builder.maxDepth;
    minExpandedLeaves = builder.minExpandedLeaves;
    maxExpandedLeaves = builder.maxExpandedLeaves;
    requiredSubtreeKind = builder.requiredSubtreeKind;
  }

  public static Builder builder(MIndexASTPool pool) {
    return new Builder(pool);
  }

  public MIndexASTPool pool() {
    return pool;
  }

  public boolean matches(MIndexAST atom) {
    Objects.requireNonNull(atom, "atom");
    if (atom.pool() != pool) throw new IllegalArgumentException("atom belongs to different pool");
    return matchesHandle(atom.handle());
  }

  boolean matchesHandle(int handle) {
    if (rootKind != null && pool.kind(handle) != rootKind) return false;
    if (labelSpecified && pool.labelHandle(handle) != labelHandle) return false;

    long flags = pool.flags(handle);
    if ((flags & requiredFlags) != requiredFlags) return false;
    if ((flags & forbiddenFlags) != 0L) return false;
    if (requiredCategoryMask != 0L) {
      long categories = pool.spec().rule(pool.kind(handle)).categoryMask();
      if ((categories & requiredCategoryMask) != requiredCategoryMask) return false;
    }

    if (exactHashSpecified && pool.exactHash64(handle) != exactHash) return false;
    if (structuralHashSpecified && pool.structuralHash64(handle) != structuralHash) return false;
    if (logicHashSpecified && pool.logicHash64(handle) != logicHash) return false;
    if (simHashSpecified
        && MIndexASTPool.hamming64(pool.simHash64(handle), simHash) > maxHammingDistance) {
      return false;
    }

    int nodes = pool.expandedNodeCount(handle);
    if (nodes < minExpandedNodes || nodes > maxExpandedNodes) return false;
    int depth = pool.depth(handle);
    if (depth < minDepth || depth > maxDepth) return false;
    int leaves = pool.expandedLeafCount(handle);
    if (leaves < minExpandedLeaves || leaves > maxExpandedLeaves) return false;

    if (requiredSubtreeKind != null) {
      if (!pool.mayContainKind(handle, requiredSubtreeKind)) return false;
      if (!pool.containsKind(handle, requiredSubtreeKind)) return false;
    }
    return true;
  }

  Tree.Kind rootKindOrNull() {
    return rootKind;
  }

  boolean labelSpecifiedForIndex() {
    return labelSpecified;
  }

  int labelHandleForIndex() {
    return labelHandle;
  }

  boolean exactHashSpecifiedForIndex() {
    return exactHashSpecified;
  }

  long exactHashForIndex() {
    return exactHash;
  }

  boolean structuralHashSpecifiedForIndex() {
    return structuralHashSpecified;
  }

  long structuralHashForIndex() {
    return structuralHash;
  }

  boolean logicHashSpecifiedForIndex() {
    return logicHashSpecified;
  }

  long logicHashForIndex() {
    return logicHash;
  }

  public static final class Builder {
    private final MIndexASTPool pool;
    private Tree.Kind rootKind;
    private boolean labelSpecified;
    private int labelHandle;
    private long requiredFlags;
    private long forbiddenFlags;
    private long requiredCategoryMask;
    private boolean exactHashSpecified;
    private long exactHash;
    private boolean structuralHashSpecified;
    private long structuralHash;
    private boolean logicHashSpecified;
    private long logicHash;
    private boolean simHashSpecified;
    private long simHash;
    private int maxHammingDistance;
    private int minExpandedNodes = 1;
    private int maxExpandedNodes = Integer.MAX_VALUE;
    private int minDepth = 1;
    private int maxDepth = Integer.MAX_VALUE;
    private int minExpandedLeaves = 1;
    private int maxExpandedLeaves = Integer.MAX_VALUE;
    private Tree.Kind requiredSubtreeKind;

    private Builder(MIndexASTPool pool) {
      this.pool = Objects.requireNonNull(pool, "pool");
    }

    public Builder rootKind(Tree.Kind kind) {
      rootKind = Objects.requireNonNull(kind, "kind");
      return this;
    }

    public Builder label(CharSequence label) {
      Objects.requireNonNull(label, "label");
      labelSpecified = true;
      labelHandle = label.length() == 0 ? 0 : pool.internLabel(label);
      return this;
    }

    public Builder label(MIndexString label) {
      Objects.requireNonNull(label, "label");
      labelSpecified = true;
      labelHandle = pool.internLabel(label);
      return this;
    }

    public Builder noLabel() {
      labelSpecified = true;
      labelHandle = 0;
      return this;
    }

    public Builder requireFlags(long mask) {
      requiredFlags |= mask;
      return this;
    }

    public Builder forbidFlags(long mask) {
      forbiddenFlags |= mask;
      return this;
    }

    /** Requires one precomputed language-rule category without traversing the AST. */
    public Builder requireCategory(MIndexSyntaxCategory category) {
      requiredCategoryMask |= Objects.requireNonNull(category, "category").bit();
      return this;
    }

    public Builder exactHash(long hash) {
      exactHashSpecified = true;
      exactHash = hash;
      return this;
    }

    public Builder structuralHash(long hash) {
      structuralHashSpecified = true;
      structuralHash = hash;
      return this;
    }

    public Builder logicHash(long hash) {
      logicHashSpecified = true;
      logicHash = hash;
      return this;
    }

    public Builder simHashNear(long hash, int maximumHammingDistance) {
      if (maximumHammingDistance < 0 || maximumHammingDistance > Long.SIZE) {
        throw new IllegalArgumentException("Hamming distance must be in [0,64]");
      }
      simHashSpecified = true;
      simHash = hash;
      maxHammingDistance = maximumHammingDistance;
      return this;
    }

    public Builder expandedNodeCount(int minimum, int maximum) {
      if (minimum < 1 || maximum < minimum) {
        throw new IllegalArgumentException("invalid expanded-node range");
      }
      minExpandedNodes = minimum;
      maxExpandedNodes = maximum;
      return this;
    }

    public Builder depth(int minimum, int maximum) {
      if (minimum < 1 || maximum < minimum) {
        throw new IllegalArgumentException("invalid depth range");
      }
      minDepth = minimum;
      maxDepth = maximum;
      return this;
    }

    public Builder expandedLeafCount(int minimum, int maximum) {
      if (minimum < 1 || maximum < minimum) {
        throw new IllegalArgumentException("invalid expanded-leaf range");
      }
      minExpandedLeaves = minimum;
      maxExpandedLeaves = maximum;
      return this;
    }

    public Builder containsKind(Tree.Kind kind) {
      requiredSubtreeKind = Objects.requireNonNull(kind, "kind");
      return this;
    }

    public MIndexASTQuery build() {
      if ((requiredFlags & forbiddenFlags) != 0L) {
        throw new IllegalStateException("same flags are both required and forbidden");
      }
      return new MIndexASTQuery(this);
    }
  }
}
