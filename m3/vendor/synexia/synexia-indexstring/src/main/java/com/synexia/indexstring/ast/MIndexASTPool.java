// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.ast;

import com.sun.source.tree.Tree;
import com.synexia.indexstring.IndexResolver;
import com.synexia.indexstring.MIndexString;
import com.synexia.indexstring.MatIndexString;
import com.synexia.indexstring.MatIndexStringPool;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;

/**
 * Append-only canonical arena for immutable AST atoms.
 *
 * <p>The design deliberately mirrors the MIndexString family: tiny external handles, canonical
 * interning, flat primitive payload arenas, exact collision verification and precomputed metadata.
 * Atom identity excludes source position so one immutable subtree can be reused by arbitrarily many
 * occurrences.</p>
 */
public final class MIndexASTPool {
  private static final int MAX_ARRAY = Integer.MAX_VALUE - 8;
  private static final double LOAD = 0.65d;
  private static final int META_KIND_BITS = 8;
  private static final long META_KIND_MASK = 0xffL;
  private static final int META_FLAGS_BITS = Long.SIZE - META_KIND_BITS;
  private static final long MAX_FLAGS = (1L << META_FLAGS_BITS) - 1L;
  private static final long HASH_SEED = 0x9e3779b97f4a7c15L;

  static {
    if (Tree.Kind.values().length > 256) {
      throw new ExceptionInInitializerError("Tree.Kind no longer fits compact 8-bit metadata");
    }
  }

  private final MatIndexStringPool labels;
  private final int languageId;

  /** Low eight bits are Tree.Kind ordinal; high 56 bits are named node flags. */
  private volatile long[] metadata;
  private volatile int[] labelHandles;
  private volatile int[] childStarts;
  private volatile int[] childCounts;
  private volatile int[] childArena;

  private volatile long[] exactHashes;
  private volatile long[] structuralHashes;
  private volatile long[] logicHashes;
  private volatile long[] simHashes;
  private volatile long[] kindSignals;

  private volatile int[] expandedNodeCounts;
  private volatile int[] depths;
  private volatile int[] expandedLeafCounts;

  private volatile int[] slots;
  private volatile int size;
  private volatile int childSize;

  public MIndexASTPool(IndexResolver resolver, int languageId) {
    this(new MatIndexStringPool(Objects.requireNonNull(resolver, "resolver")), languageId);
  }

  public MIndexASTPool(MatIndexStringPool labels, int languageId) {
    this.labels = Objects.requireNonNull(labels, "labels");
    if (languageId < 0) throw new IllegalArgumentException("negative languageId");
    this.languageId = languageId;

    int initial = 16;
    metadata = new long[initial];
    labelHandles = new int[initial];
    childStarts = new int[initial];
    childCounts = new int[initial];
    exactHashes = new long[initial];
    structuralHashes = new long[initial];
    logicHashes = new long[initial];
    simHashes = new long[initial];
    kindSignals = new long[initial];
    expandedNodeCounts = new int[initial];
    depths = new int[initial];
    expandedLeafCounts = new int[initial];
    childArena = new int[32];
    slots = new int[32];
  }

  public MatIndexStringPool labels() {
    return labels;
  }

  /**
   * Process-level immutable Java language specification.
   *
   * <p>No spec reference is retained per AST atom; all nodes reach the one frozen image through
   * their pool.</p>
   */
  public MIndexLanguageSpec spec() {
    return MIndexLanguageSpecs.java21();
  }

  public int languageId() {
    return languageId;
  }

  public int size() {
    return size;
  }

  /** Builds an immutable opt-in search snapshot over the current canonical atom DAG. */
  public MIndexASTPoolPrecompute precompute() {
    return MIndexASTPoolPrecompute.snapshot(this);
  }

  public long retainedChildHandleCount() {
    return childSize;
  }

  public MIndexAST value(int handle) {
    checkHandle(handle);
    return new MIndexAST(this, handle);
  }

  public int internLabel(CharSequence text) {
    Objects.requireNonNull(text, "text");
    return labels.internText(languageId, text).handle();
  }

  public int internLabel(MIndexString text) {
    Objects.requireNonNull(text, "text");
    if (text.resolver() != labels.resolver()) {
      throw new IllegalArgumentException("MIndexString label belongs to a different resolver");
    }
    return labels.intern(text.tuple()).handle();
  }

  public MIndexAST atom(Tree.Kind kind, MIndexAST... children) {
    return atom(kind, 0, 0L, childHandles(children));
  }

  public MIndexAST atom(Tree.Kind kind, CharSequence label, MIndexAST... children) {
    int labelHandle = label == null || label.length() == 0 ? 0 : internLabel(label);
    return atom(kind, labelHandle, 0L, childHandles(children));
  }

  public MIndexAST atom(
      Tree.Kind kind, MIndexString label, long flags, MIndexAST... children) {
    int labelHandle = label == null ? 0 : internLabel(label);
    return atom(kind, labelHandle, flags, childHandles(children));
  }

  public MIndexAST atom(
      Tree.Kind kind, MatIndexString label, long flags, MIndexAST... children) {
    int labelHandle = 0;
    if (label != null) {
      if (label.pool() != labels) {
        throw new IllegalArgumentException("MatIndexString label belongs to a different pool");
      }
      labelHandle = label.handle();
    }
    return atom(kind, labelHandle, flags, childHandles(children));
  }

  public MIndexAST atom(
      Tree.Kind kind, CharSequence label, long flags, MIndexAST... children) {
    int labelHandle = label == null || label.length() == 0 ? 0 : internLabel(label);
    return atom(kind, labelHandle, flags, childHandles(children));
  }

  synchronized MIndexAST atom(Tree.Kind kind, int labelHandle, long flags, int[] children) {
    Objects.requireNonNull(kind, "kind");
    Objects.requireNonNull(children, "children");
    MIndexLanguageRule languageRule = spec().rule(kind);
    if (languageRule.minimumRelease() > spec().release()) {
      throw new IllegalArgumentException(
          "AST kind " + kind + " requires Java " + languageRule.minimumRelease());
    }
    if ((flags & ~MAX_FLAGS) != 0L) {
      throw new IllegalArgumentException("flags exceed compact 56-bit AST metadata lane");
    }
    if (labelHandle < 0 || labelHandle > labels.size()) {
      throw new IllegalArgumentException("unknown label handle " + labelHandle);
    }
    validateRulePayload(languageRule, labelHandle, flags);
    for (int child : children) checkHandle(child);

    long meta = encodeMetadata(kind, flags);
    long exact = computeExactHash(meta, labelHandle, children);
    int existing = find(meta, labelHandle, children, exact);
    if (existing != 0) return new MIndexAST(this, existing);

    reserveNodes(size + 2);
    reserveTable(size + 1);
    reserveChildren(Math.addExact(childSize, children.length));

    int handle = size + 1;
    int start = childSize;
    System.arraycopy(children, 0, childArena, childSize, children.length);
    childSize += children.length;

    metadata[handle] = meta;
    labelHandles[handle] = labelHandle;
    childStarts[handle] = start;
    childCounts[handle] = children.length;
    exactHashes[handle] = exact;
    structuralHashes[handle] = computeStructuralHash(meta, children);
    logicHashes[handle] = computeLogicHash(meta, labelHandle, children);
    simHashes[handle] = computeSimHash(meta, labelHandle, children);
    kindSignals[handle] = computeKindSignal(kind, children);
    expandedNodeCounts[handle] = computeExpandedNodeCount(children);
    depths[handle] = computeDepth(children);
    expandedLeafCounts[handle] = computeExpandedLeafCount(children);

    int mask = slots.length - 1;
    int slot = mixToInt(exact) & mask;
    while (slots[slot] != 0) slot = (slot + 1) & mask;
    slots[slot] = handle;
    size = handle;
    return new MIndexAST(this, handle);
  }

  Tree.Kind kind(int handle) {
    checkHandle(handle);
    int code = (int) (metadata[handle] & META_KIND_MASK);
    return Tree.Kind.values()[code];
  }

  long flags(int handle) {
    checkHandle(handle);
    return metadata[handle] >>> META_KIND_BITS;
  }

  int labelHandle(int handle) {
    checkHandle(handle);
    return labelHandles[handle];
  }

  int childCount(int handle) {
    checkHandle(handle);
    return childCounts[handle];
  }

  int childHandle(int handle, int ordinal) {
    int count = childCount(handle);
    Objects.checkIndex(ordinal, count);
    return childArena[childStarts[handle] + ordinal];
  }

  long exactHash64(int handle) {
    checkHandle(handle);
    return exactHashes[handle];
  }

  long structuralHash64(int handle) {
    checkHandle(handle);
    return structuralHashes[handle];
  }

  long logicHash64(int handle) {
    checkHandle(handle);
    return logicHashes[handle];
  }

  long simHash64(int handle) {
    checkHandle(handle);
    return simHashes[handle];
  }

  long kindSignal64(int handle) {
    checkHandle(handle);
    return kindSignals[handle];
  }

  int expandedNodeCount(int handle) {
    checkHandle(handle);
    return expandedNodeCounts[handle];
  }

  int depth(int handle) {
    checkHandle(handle);
    return depths[handle];
  }

  int expandedLeafCount(int handle) {
    checkHandle(handle);
    return expandedLeafCounts[handle];
  }

  boolean mayContainKind(int handle, Tree.Kind kind) {
    long bit = kindBit(kind);
    return (kindSignal64(handle) & bit) == bit;
  }

  boolean containsKind(int handle, Tree.Kind kind) {
    if (!mayContainKind(handle, kind)) return false;
    int[] stack = new int[32];
    int top = 0;
    stack[top++] = handle;
    while (top != 0) {
      int current = stack[--top];
      if (kind(current) == kind) return true;
      int count = childCounts[current];
      if (top + count > stack.length) {
        stack = Arrays.copyOf(stack, grownLength(stack.length, top + count));
      }
      int start = childStarts[current];
      for (int i = 0; i < count; i++) stack[top++] = childArena[start + i];
    }
    return false;
  }

  boolean contentEquals(int left, MIndexASTPool other, int right) {
    return same(left, other, right, Comparison.EXACT);
  }

  boolean structurallyEquals(int left, MIndexASTPool other, int right) {
    return same(left, other, right, Comparison.STRUCTURAL);
  }

  boolean normalizedLogicEquals(int left, MIndexASTPool other, int right) {
    return same(left, other, right, Comparison.LOGIC);
  }

  byte[] sha256(int handle) {
    checkHandle(handle);
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      updateDigest(digest, handle);
      return digest.digest();
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException("SHA-256 unavailable", impossible);
    }
  }

  private void updateDigest(MessageDigest digest, int handle) {
    updateUtf8(digest, kind(handle).name());
    updateLong(digest, flags(handle));
    int labelHandle = labelHandles[handle];
    if (labelHandle == 0) {
      digest.update((byte) 0);
    } else {
      digest.update((byte) 1);
      updateUtf8(digest, labels.value(labelHandle).materialize());
    }
    int count = childCounts[handle];
    updateInt(digest, count);
    int start = childStarts[handle];
    for (int i = 0; i < count; i++) {
      byte[] childDigest = sha256(childArena[start + i]);
      digest.update(childDigest);
    }
  }

  private boolean same(int left, MIndexASTPool other, int right, Comparison comparison) {
    Objects.requireNonNull(other, "other");
    checkHandle(left);
    other.checkHandle(right);

    long leftHash = comparison.hash(this, left);
    long rightHash = comparison.hash(other, right);
    if (leftHash != rightHash) return false;
    if (this == other && left == right) return true;

    int[] leftStack = new int[32];
    int[] rightStack = new int[32];
    int top = 0;
    leftStack[top] = left;
    rightStack[top++] = right;

    while (top != 0) {
      int leftNode = leftStack[--top];
      int rightNode = rightStack[top];

      if (metadata[leftNode] != other.metadata[rightNode]) return false;
      Tree.Kind currentKind = kind(leftNode);
      if (comparison.compareLabel(currentKind)) {
        if (!labelsEqual(labelHandles[leftNode], other, other.labelHandles[rightNode])) return false;
      }

      int leftCount = childCounts[leftNode];
      int rightCount = other.childCounts[rightNode];
      if (leftCount != rightCount) return false;
      if (top + leftCount > leftStack.length) {
        int next = grownLength(leftStack.length, top + leftCount);
        leftStack = Arrays.copyOf(leftStack, next);
        rightStack = Arrays.copyOf(rightStack, next);
      }
      int leftStart = childStarts[leftNode];
      int rightStart = other.childStarts[rightNode];
      for (int i = leftCount - 1; i >= 0; i--) {
        leftStack[top] = childArena[leftStart + i];
        rightStack[top] = other.childArena[rightStart + i];
        top++;
      }
    }
    return true;
  }

  private boolean labelsEqual(int leftLabel, MIndexASTPool other, int rightLabel) {
    if (leftLabel == 0 || rightLabel == 0) return leftLabel == rightLabel;
    if (this == other) return leftLabel == rightLabel;
    MatIndexString left = labels.value(leftLabel);
    MatIndexString right = other.labels.value(rightLabel);
    return stableLabelHash64(left) == stableLabelHash64(right)
        && left.materialize().equals(right.materialize());
  }

  private long stableLabelHash64(int labelHandle) {
    return stableLabelHash64(labels.value(labelHandle));
  }

  private static long stableLabelHash64(MatIndexString value) {
    long compact =
        ((long) value.hashCode() << Integer.SIZE)
            ^ ((long) value.length() << 16)
            ^ value.codePointCount();
    return avalanche(compact ^ 0x4d494e4445584c42L);
  }

  private void validateRulePayload(MIndexLanguageRule rule, int labelHandle, long flags) {
    if (labelHandle != 0 && !rule.labelPolicy().isLabelled()) {
      throw new IllegalArgumentException(
          "AST kind " + rule.kind() + " does not define a canonical label lane");
    }

    switch (rule.flagPolicy()) {
      case NONE -> {
        if (flags != 0L) {
          throw new IllegalArgumentException(
              "AST kind " + rule.kind() + " does not define flag payload");
        }
      }
      case BOOLEAN -> {
        if (flags != 0L && flags != 1L) {
          throw new IllegalArgumentException(
              "AST kind " + rule.kind() + " requires boolean flag 0/1");
        }
      }
      case MODIFIER_BITS -> {
        long unknown = flags & ~spec().modifierMask();
        if (unknown != 0L) {
          throw new IllegalArgumentException(
              "AST modifier flags contain unknown bits 0x" + Long.toHexString(unknown));
        }
      }
      case ENUM_ORDINAL_PLUS_ONE -> {
        if (flags > Integer.MAX_VALUE) {
          throw new IllegalArgumentException(
              "AST enum flag ordinal does not fit int: " + flags);
        }
      }
    }
  }

  private int[] childHandles(MIndexAST[] children) {
    Objects.requireNonNull(children, "children");
    int[] handles = new int[children.length];
    for (int i = 0; i < children.length; i++) {
      MIndexAST child = Objects.requireNonNull(children[i], "child");
      if (child.pool() != this) throw new IllegalArgumentException("child belongs to different pool");
      handles[i] = child.handle();
    }
    return handles;
  }

  private int find(long meta, int labelHandle, int[] children, long hash) {
    int[] table = slots;
    int mask = table.length - 1;
    int slot = mixToInt(hash) & mask;
    for (int probe = 0; probe < table.length; probe++) {
      int handle = table[slot];
      if (handle == 0) return 0;
      if (exactHashes[handle] == hash
          && metadata[handle] == meta
          && labelHandles[handle] == labelHandle
          && childrenEqual(handle, children)) {
        return handle;
      }
      slot = (slot + 1) & mask;
    }
    return 0;
  }

  private boolean childrenEqual(int handle, int[] children) {
    if (childCounts[handle] != children.length) return false;
    int start = childStarts[handle];
    for (int i = 0; i < children.length; i++) {
      if (childArena[start + i] != children[i]) return false;
    }
    return true;
  }

  private long computeExactHash(long meta, int labelHandle, int[] children) {
    long hash = combine(HASH_SEED, stableMetadataFeature(meta));
    hash = combine(hash, labelHandle == 0 ? 0L : stableLabelHash64(labelHandle));
    for (int child : children) hash = combine(hash, exactHashes[child]);
    return avalanche(hash ^ children.length);
  }

  private long computeStructuralHash(long meta, int[] children) {
    long hash = combine(HASH_SEED ^ 0x5354525543545552L, stableMetadataFeature(meta));
    for (int child : children) hash = combine(hash, structuralHashes[child]);
    return avalanche(hash ^ children.length);
  }

  private long computeLogicHash(long meta, int labelHandle, int[] children) {
    Tree.Kind kind = decodeKind(meta);
    long hash = combine(HASH_SEED ^ 0x4c4f4749434d4154L, stableMetadataFeature(meta));
    if (labelHandle != 0 && logicLabelRelevant(kind)) {
      hash = combine(hash, stableLabelHash64(labelHandle));
    }
    for (int child : children) hash = combine(hash, logicHashes[child]);
    return avalanche(hash ^ children.length);
  }

  private long computeSimHash(long meta, int labelHandle, int[] children) {
    long kindFeature = avalanche(stableMetadataFeature(meta) ^ 0x4153544b494e4401L);
    long labelFeature =
        labelHandle == 0
            ? 0L
            : avalanche(stableLabelHash64(labelHandle) ^ 0x4c4142454c000001L);
    long result = 0L;
    for (int bit = 0; bit < Long.SIZE; bit++) {
      int score = ((kindFeature >>> bit) & 1L) != 0L ? 1 : -1;
      if (labelHandle != 0) score += ((labelFeature >>> bit) & 1L) != 0L ? 1 : -1;
      for (int child : children) {
        score += ((simHashes[child] >>> bit) & 1L) != 0L ? 1 : -1;
      }
      if (score >= 0) result |= 1L << bit;
    }
    return result;
  }

  private long computeKindSignal(Tree.Kind kind, int[] children) {
    long signal = kindBit(kind);
    for (int child : children) signal |= kindSignals[child];
    return signal;
  }

  private int computeExpandedNodeCount(int[] children) {
    int count = 1;
    for (int child : children) count = Math.addExact(count, expandedNodeCounts[child]);
    return count;
  }

  private int computeDepth(int[] children) {
    int depth = 1;
    for (int child : children) depth = Math.max(depth, Math.addExact(1, depths[child]));
    return depth;
  }

  private int computeExpandedLeafCount(int[] children) {
    if (children.length == 0) return 1;
    int leaves = 0;
    for (int child : children) leaves = Math.addExact(leaves, expandedLeafCounts[child]);
    return leaves;
  }

  private void reserveNodes(int required) {
    if (required <= metadata.length) return;
    int next = grownLength(metadata.length, required);
    metadata = Arrays.copyOf(metadata, next);
    labelHandles = Arrays.copyOf(labelHandles, next);
    childStarts = Arrays.copyOf(childStarts, next);
    childCounts = Arrays.copyOf(childCounts, next);
    exactHashes = Arrays.copyOf(exactHashes, next);
    structuralHashes = Arrays.copyOf(structuralHashes, next);
    logicHashes = Arrays.copyOf(logicHashes, next);
    simHashes = Arrays.copyOf(simHashes, next);
    kindSignals = Arrays.copyOf(kindSignals, next);
    expandedNodeCounts = Arrays.copyOf(expandedNodeCounts, next);
    depths = Arrays.copyOf(depths, next);
    expandedLeafCounts = Arrays.copyOf(expandedLeafCounts, next);
  }

  private void reserveChildren(int required) {
    if (required <= childArena.length) return;
    childArena = Arrays.copyOf(childArena, grownLength(childArena.length, required));
  }

  private void reserveTable(int expected) {
    if (expected <= (int) (slots.length * LOAD)) return;
    int capacity = tableCapacity(expected);
    int[] replacement = new int[capacity];
    int mask = capacity - 1;
    for (int handle = 1; handle <= size; handle++) {
      int slot = mixToInt(exactHashes[handle]) & mask;
      while (replacement[slot] != 0) slot = (slot + 1) & mask;
      replacement[slot] = handle;
    }
    slots = replacement;
  }

  private void checkHandle(int handle) {
    if (handle < 1 || handle > size) {
      throw new IndexOutOfBoundsException("MIndexAST handle " + handle);
    }
  }

  private static long encodeMetadata(Tree.Kind kind, long flags) {
    return (flags << META_KIND_BITS) | (kind.ordinal() & META_KIND_MASK);
  }

  private static Tree.Kind decodeKind(long meta) {
    return Tree.Kind.values()[(int) (meta & META_KIND_MASK)];
  }

  /**
   * Stable hash input for persisted identities.
   *
   * <p>The packed runtime lane intentionally stores the compact Tree.Kind ordinal, but persisted
   * hashes must not depend on ordinal ordering. Kind names are part of the public compiler-tree API
   * and let a cache fail or replay by semantic kind rather than silently changing meaning if enum
   * declaration order changes.</p>
   */
  private static long stableMetadataFeature(long meta) {
    String name = decodeKind(meta).name();
    long hash = 0xcbf29ce484222325L;
    for (int index = 0; index < name.length(); index++) {
      hash ^= name.charAt(index);
      hash *= 0x100000001b3L;
    }
    long flags = meta >>> META_KIND_BITS;
    return avalanche(hash ^ Long.rotateLeft(flags, 17));
  }

  private static boolean logicLabelRelevant(Tree.Kind kind) {
    return kind == Tree.Kind.PRIMITIVE_TYPE || kind.name().endsWith("_LITERAL");
  }

  static int hamming64(long left, long right) {
    return Long.bitCount(left ^ right);
  }

  static long kindBit(Tree.Kind kind) {
    int bit = (int) (avalanche(kind.name().hashCode()) & 63L);
    return 1L << bit;
  }

  private static long combine(long hash, long value) {
    return Long.rotateLeft(hash ^ avalanche(value + 0x9e3779b97f4a7c15L), 27)
        * 0x94d049bb133111ebL
        + 0x52dce729L;
  }

  private static long avalanche(long value) {
    long z = value;
    z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
    z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
    return z ^ (z >>> 31);
  }

  private static int mixToInt(long value) {
    long mixed = avalanche(value);
    return (int) (mixed ^ (mixed >>> 32));
  }

  private static int tableCapacity(int expected) {
    long required = Math.max(2L, (long) Math.ceil(Math.max(1, expected) / LOAD));
    int capacity = 2;
    while (capacity < required) {
      if (capacity >= (1 << 30)) throw new IllegalArgumentException("AST pool table too large");
      capacity <<= 1;
    }
    return capacity;
  }

  private static int grownLength(int current, int required) {
    if (required < 0 || required > MAX_ARRAY) {
      throw new IllegalStateException("AST arena capacity");
    }
    long next = Math.max(required, Math.max(2L, current + (current >>> 1) + 1L));
    return (int) Math.min(MAX_ARRAY, next);
  }

  private static void updateUtf8(MessageDigest digest, String text) {
    byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
    updateInt(digest, bytes.length);
    digest.update(bytes);
  }

  private static void updateInt(MessageDigest digest, int value) {
    digest.update((byte) (value >>> 24));
    digest.update((byte) (value >>> 16));
    digest.update((byte) (value >>> 8));
    digest.update((byte) value);
  }

  private static void updateLong(MessageDigest digest, long value) {
    for (int shift = 56; shift >= 0; shift -= Byte.SIZE) {
      digest.update((byte) (value >>> shift));
    }
  }

  private enum Comparison {
    EXACT {
      @Override long hash(MIndexASTPool pool, int handle) {
        return pool.exactHashes[handle];
      }
      @Override boolean compareLabel(Tree.Kind kind) {
        return true;
      }
    },
    STRUCTURAL {
      @Override long hash(MIndexASTPool pool, int handle) {
        return pool.structuralHashes[handle];
      }
      @Override boolean compareLabel(Tree.Kind kind) {
        return false;
      }
    },
    LOGIC {
      @Override long hash(MIndexASTPool pool, int handle) {
        return pool.logicHashes[handle];
      }
      @Override boolean compareLabel(Tree.Kind kind) {
        return logicLabelRelevant(kind);
      }
    };

    abstract long hash(MIndexASTPool pool, int handle);
    abstract boolean compareLabel(Tree.Kind kind);
  }
}
