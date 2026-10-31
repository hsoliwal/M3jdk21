// Copyright 2026 Hitesh Soliwal and contributors
// Full donor dependency receiver; source custody in JSON_CLOSURE_PROVENANCE.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import com.m3.file.M3ProgressMonitor;
import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable primitive pageable-tree substrate.
 *
 * <p>The canonical representation is structure-of-arrays: parent coordinates, CSR children,
 * preorder/Euler coordinates, depth/height/subtree-size lanes, depth buckets, and a flattened
 * binary-lifting table. No per-node Java object is retained.</p>
 *
 * <p>The nested {@link Source} contract is intentionally smaller than this frozen tree. It allows
 * lazy/on-demand pagers to expose roots and direct children without materializing the hierarchy.
 * {@link #freeze(int[], M3ProgressMonitor)} is used only when global tree algorithms are required.</p>
 */
public final class M3PageableTree {
  public static final int NO_PARENT = -1;
  public static final int DEFAULT_MAX_PAGE_SIZE = 4096;

  /** Retained-state policy for the frozen tree. */
  public enum Profile {
    /** Minimize retained memory; ancestor/LCA queries fall back to parent walking. */
    PAGING,
    /** Retain binary-lifting ancestors for O(log depth) ancestor/LCA queries. */
    ALGORITHMS
  }

  /** Minimal lazy hierarchy contract; suitable for ASTs, DAG projections, repositories and SCC DAGs. */
  public interface Source {
    int rootCount();
    int rootAt(int ordinal);
    int childCount(int parentNode);
    int childAt(int parentNode, int ordinal);

    /** Bulk root read with scalar fallback for existing sources. */
    default void readRoots(
        int firstOrdinal, int count, int[] target, int targetOffset, M3ProgressMonitor monitor) {
      Objects.requireNonNull(target, "target");
      M3ProgressMonitor active = monitor == null ? M3ProgressMonitor.noop() : monitor;
      if (firstOrdinal < 0 || count < 0 || targetOffset < 0
          || (long) targetOffset + count > target.length
          || (long) firstOrdinal + count > rootCount()) {
        throw new IndexOutOfBoundsException("invalid root bulk-read range");
      }
      for (int index = 0; index < count; index++) {
        active.checkCanceled();
        target[targetOffset + index] = rootAt(firstOrdinal + index);
      }
    }

    /** Bulk child read with scalar fallback for existing sources. */
    default void readChildren(
        int parentNode,
        int firstOrdinal,
        int count,
        int[] target,
        int targetOffset,
        M3ProgressMonitor monitor) {
      Objects.requireNonNull(target, "target");
      M3ProgressMonitor active = monitor == null ? M3ProgressMonitor.noop() : monitor;
      if (firstOrdinal < 0 || count < 0 || targetOffset < 0
          || (long) targetOffset + count > target.length
          || (long) firstOrdinal + count > childCount(parentNode)) {
        throw new IndexOutOfBoundsException("invalid child bulk-read range");
      }
      for (int index = 0; index < count; index++) {
        active.checkCanceled();
        target[targetOffset + index] = childAt(parentNode, firstOrdinal + index);
      }
    }

    default void readRoots(int firstOrdinal, int count, int[] target, int targetOffset) {
      readRoots(firstOrdinal, count, target, targetOffset, M3ProgressMonitor.noop());
    }

    default void readChildren(
        int parentNode, int firstOrdinal, int count, int[] target, int targetOffset) {
      readChildren(
          parentNode, firstOrdinal, count, target, targetOffset, M3ProgressMonitor.noop());
    }
  }

  /** Spring-Data-style offset/limit page without object-per-row overhead. */
  public static final class Page {
    private final int parentNode;
    private final int offset;
    private final int total;
    private final int[] nodes;

    private Page(int parentNode, int offset, int total, int[] nodes) {
      this.parentNode = parentNode;
      this.offset = offset;
      this.total = total;
      this.nodes = nodes;
    }

    /** {@link #NO_PARENT} for a root/level/subtree page. */
    public int parentNode() { return parentNode; }
    public int offset() { return offset; }
    public int total() { return total; }
    public int size() { return nodes.length; }
    public boolean isEmpty() { return nodes.length == 0; }
    public boolean hasPrevious() { return offset > 0; }
    public boolean hasNext() { return offset + nodes.length < total; }
    public int nextOffset() { return offset + nodes.length; }
    public int previousOffset() { return Math.max(0, offset - nodes.length); }
    public int nodeAt(int ordinal) { return nodes[Objects.checkIndex(ordinal, nodes.length)]; }
    public int[] nodes() { return nodes.clone(); }
  }

  /**
   * Cursor for expansion-aware virtual scrolling.
   *
   * <p>The expansion fingerprint prevents reusing a cursor after the expanded-node bitset changed.
   * Callers may restart at {@link #firstVisibleCursor(long[])} after expansion/collapse.</p>
   */
  public record VisibleCursor(int nextPreorderRank, long expansionFingerprint) {
    public VisibleCursor {
      if (nextPreorderRank < 0) throw new IllegalArgumentException("negative preorder rank");
    }
  }

  public static final class VisiblePage {
    private final int[] nodes;
    private final int[] depths;
    private final VisibleCursor nextCursor;
    private final boolean hasMore;

    private VisiblePage(
        int[] nodes,
        int[] depths,
        VisibleCursor nextCursor,
        boolean hasMore) {
      this.nodes = nodes;
      this.depths = depths;
      this.nextCursor = nextCursor;
      this.hasMore = hasMore;
    }

    public int size() { return nodes.length; }
    public int nodeAt(int ordinal) { return nodes[Objects.checkIndex(ordinal, nodes.length)]; }
    public int depthAt(int ordinal) { return depths[Objects.checkIndex(ordinal, depths.length)]; }
    public int[] nodes() { return nodes.clone(); }
    public int[] depths() { return depths.clone(); }
    public boolean hasMore() { return hasMore; }
    public VisibleCursor nextCursor() { return nextCursor; }
  }

  private final int[] parent;
  private final int[] roots;
  private final int[] childOffsets;
  private final int[] children;
  private final int[] preorder;
  private final int[] preorderRank;
  private final int[] rootByNode;
  private final int[] depth;
  private final int[] height;
  private final int[] subtreeSize;
  private final int[] levelOffsets;
  private final int[] levelNodes;
  private final int[] jump;
  private final int jumpLevels;
  private final Profile profile;
  private final int leafCount;
  private final int diameterEdges;
  private final int maxWidth;
  private final long fingerprint64;

  private M3PageableTree(
      int[] parent,
      int[] roots,
      int[] childOffsets,
      int[] children,
      int[] preorder,
      int[] preorderRank,
      int[] rootByNode,
      int[] depth,
      int[] height,
      int[] subtreeSize,
      int[] levelOffsets,
      int[] levelNodes,
      int[] jump,
      int jumpLevels,
      Profile profile,
      int leafCount,
      int diameterEdges,
      int maxWidth) {
    this.parent = parent;
    this.roots = roots;
    this.childOffsets = childOffsets;
    this.children = children;
    this.preorder = preorder;
    this.preorderRank = preorderRank;
    this.rootByNode = rootByNode;
    this.depth = depth;
    this.height = height;
    this.subtreeSize = subtreeSize;
    this.levelOffsets = levelOffsets;
    this.levelNodes = levelNodes;
    this.jump = jump;
    this.jumpLevels = jumpLevels;
    this.profile = profile;
    this.leafCount = leafCount;
    this.diameterEdges = diameterEdges;
    this.maxWidth = maxWidth;
    this.fingerprint64 = computeFingerprint(parent);
  }

  /**
   * Freezes a strict forest from one parent coordinate per node.
   *
   * <p>Parents use {@code -1} for roots. Children are ordered by ascending node coordinate.
   * Cycles, self-parenting, out-of-range parents and rootless inputs fail closed.</p>
   */
  public static M3PageableTree freeze(int[] parentByNode) {
    return freeze(parentByNode, Profile.ALGORITHMS, M3ProgressMonitor.noop());
  }

  public static M3PageableTree freezePaging(int[] parentByNode) {
    return freeze(parentByNode, Profile.PAGING, M3ProgressMonitor.noop());
  }

  public static M3PageableTree freeze(
      int[] parentByNode,
      M3ProgressMonitor monitor) {
    return freeze(parentByNode, Profile.ALGORITHMS, monitor);
  }

  public static M3PageableTree freeze(
      int[] parentByNode,
      Profile profile,
      M3ProgressMonitor monitor) {
    Objects.requireNonNull(parentByNode, "parentByNode");
    Objects.requireNonNull(profile, "profile");
    M3ProgressMonitor progress = monitor == null ? M3ProgressMonitor.noop() : monitor;
    int[] parent = parentByNode.clone();
    int nodes = parent.length;
    if (nodes == 0) {
      return new M3PageableTree(
          parent, new int[0], new int[] {0}, new int[0],
          new int[0], new int[0], new int[0], new int[0], new int[0], new int[0],
          new int[] {0}, new int[0], new int[0], 0, profile, 0, 0, 0);
    }

    progress.beginTask("Freeze primitive pageable tree", M3ProgressMonitor.UNKNOWN);
    try {
      int rootCount = 0;
      int[] childCounts = new int[nodes];
      for (int node = 0; node < nodes; node++) {
        progress.checkCanceled();
        int p = parent[node];
        if (p == NO_PARENT) {
          rootCount++;
        } else {
          if (p < 0 || p >= nodes) {
            throw new IllegalArgumentException("parent outside tree: node=" + node + " parent=" + p);
          }
          if (p == node) throw new IllegalArgumentException("node parents itself: " + node);
          childCounts[p]++;
        }
        progress.worked(1L);
      }
      if (rootCount == 0) throw new IllegalArgumentException("forest has no root");

      int[] roots = new int[rootCount];
      int rootCursor = 0;
      for (int node = 0; node < nodes; node++) {
        if (parent[node] == NO_PARENT) roots[rootCursor++] = node;
      }

      int[] childOffsets = new int[nodes + 1];
      for (int node = 0; node < nodes; node++) {
        childOffsets[node + 1] = Math.addExact(childOffsets[node], childCounts[node]);
      }
      int[] children = new int[nodes - rootCount];
      int[] fill = childOffsets.clone();
      for (int child = 0; child < nodes; child++) {
        int p = parent[child];
        if (p != NO_PARENT) children[fill[p]++] = child;
      }
      for (int node = 0; node < nodes; node++) {
        Arrays.sort(children, childOffsets[node], childOffsets[node + 1]);
      }

      int[] preorder = new int[nodes];
      int[] preorderRank = new int[nodes];
      int[] rootByNode = new int[nodes];
      int[] depth = new int[nodes];
      Arrays.fill(rootByNode, NO_PARENT);

      int[] stack = new int[nodes];
      int stackSize = 0;
      for (int root = roots.length - 1; root >= 0; root--) stack[stackSize++] = roots[root];
      int orderSize = 0;
      while (stackSize != 0) {
        progress.checkCanceled();
        int node = stack[--stackSize];
        if (rootByNode[node] != NO_PARENT) {
          throw new IllegalArgumentException("cycle or duplicate parent reachability at node " + node);
        }
        int p = parent[node];
        if (p == NO_PARENT) {
          rootByNode[node] = node;
          depth[node] = 0;
        } else {
          if (rootByNode[p] == NO_PARENT) {
            throw new IllegalArgumentException("cycle detected around node " + node);
          }
          rootByNode[node] = rootByNode[p];
          depth[node] = Math.addExact(depth[p], 1);
        }
        preorderRank[node] = orderSize;
        preorder[orderSize++] = node;

        for (int row = childOffsets[node + 1] - 1; row >= childOffsets[node]; row--) {
          stack[stackSize++] = children[row];
        }
        progress.worked(1L);
      }
      if (orderSize != nodes) {
        throw new IllegalArgumentException("forest contains an unreachable cycle");
      }

      int maxDepth = 0;
      for (int value : depth) maxDepth = Math.max(maxDepth, value);
      int[] levelCounts = new int[maxDepth + 1];
      for (int value : depth) levelCounts[value]++;
      int[] levelOffsets = new int[maxDepth + 2];
      for (int level = 0; level < levelCounts.length; level++) {
        levelOffsets[level + 1] = Math.addExact(levelOffsets[level], levelCounts[level]);
      }
      int[] levelNodes = new int[nodes];
      int[] levelCursor = levelOffsets.clone();
      for (int node : preorder) levelNodes[levelCursor[depth[node]]++] = node;

      int[] subtreeSize = new int[nodes];
      int[] height = new int[nodes];
      int leaves = 0;
      int diameter = 0;
      for (int position = nodes - 1; position >= 0; position--) {
        int node = preorder[position];
        int size = 1;
        int best = -1;
        int second = -1;
        int start = childOffsets[node];
        int end = childOffsets[node + 1];
        if (start == end) leaves++;
        for (int row = start; row < end; row++) {
          int child = children[row];
          size = Math.addExact(size, subtreeSize[child]);
          int childHeight = height[child];
          if (childHeight > best) {
            second = best;
            best = childHeight;
          } else if (childHeight > second) {
            second = childHeight;
          }
        }
        subtreeSize[node] = size;
        height[node] = best < 0 ? 0 : Math.addExact(best, 1);
        int through = best < 0 ? 0 : Math.addExact(best + 1, second < 0 ? 0 : second + 1);
        diameter = Math.max(diameter, through);
      }

      int maxWidth = 0;
      for (int count : levelCounts) maxWidth = Math.max(maxWidth, count);

      int jumpLevels = 0;
      int[] jump = new int[0];
      if (profile == Profile.ALGORITHMS) {
        jumpLevels = 1;
        while ((1L << jumpLevels) <= maxDepth && jumpLevels < 31) jumpLevels++;
        jump = new int[Math.multiplyExact(jumpLevels, nodes)];
        Arrays.fill(jump, NO_PARENT);
        for (int node = 0; node < nodes; node++) jump[node] = parent[node];
        for (int level = 1; level < jumpLevels; level++) {
          int previousBase = (level - 1) * nodes;
          int base = level * nodes;
          for (int node = 0; node < nodes; node++) {
            int halfway = jump[previousBase + node];
            jump[base + node] =
                halfway == NO_PARENT ? NO_PARENT : jump[previousBase + halfway];
          }
        }
      }

      return new M3PageableTree(
          parent, roots, childOffsets, children, preorder, preorderRank, rootByNode,
          depth, height, subtreeSize, levelOffsets, levelNodes, jump, jumpLevels, profile,
          leaves, diameter, maxWidth);
    } finally {
      progress.done();
    }
  }

  public int nodeCount() { return parent.length; }
  public int rootCount() { return roots.length; }
  public int rootAt(int ordinal) { return roots[Objects.checkIndex(ordinal, roots.length)]; }

  public int parent(int node) { return parent[checkNode(node)]; }
  public boolean isRoot(int node) { return parent(node) == NO_PARENT; }
  public boolean isLeaf(int node) { return childCount(node) == 0; }
  public int childCount(int parentNode) {
    int node = checkNode(parentNode);
    return childOffsets[node + 1] - childOffsets[node];
  }
  public int childAt(int parentNode, int ordinal) {
    int node = checkNode(parentNode);
    int count = childOffsets[node + 1] - childOffsets[node];
    return children[childOffsets[node] + Objects.checkIndex(ordinal, count)];
  }

  /** Returns the direct-child ordinal, or -1 when child is not directly below parent. */
  public int childOrdinal(int parentNode, int childNode) {
    int parentChecked = checkNode(parentNode);
    int childChecked = checkNode(childNode);
    if (parent[childChecked] != parentChecked) return -1;
    int start = childOffsets[parentChecked];
    int end = childOffsets[parentChecked + 1];
    int found = Arrays.binarySearch(children, start, end, childChecked);
    return found < 0 ? -1 : found - start;
  }

  public int depth(int node) { return depth[checkNode(node)]; }
  public int height(int node) { return height[checkNode(node)]; }
  public int subtreeSize(int node) { return subtreeSize[checkNode(node)]; }
  public int preorderRank(int node) { return preorderRank[checkNode(node)]; }
  public int nodeAtPreorderRank(int rank) { return preorder[Objects.checkIndex(rank, preorder.length)]; }
  public int treeRoot(int node) { return rootByNode[checkNode(node)]; }
  public int levelCount() { return levelOffsets.length - 1; }
  public int leafCount() { return leafCount; }
  public int diameterEdges() { return diameterEdges; }
  public int maxWidth() { return maxWidth; }
  public Profile profile() { return profile; }
  public boolean binaryLiftingEnabled() { return jumpLevels != 0; }
  public long fingerprint64() { return fingerprint64; }

  /** Lightweight adapter when a frozen tree must be consumed through the lazy Source contract. */
  public Source source() {
    return new Source() {
      @Override public int rootCount() { return M3PageableTree.this.rootCount(); }
      @Override public int rootAt(int ordinal) { return M3PageableTree.this.rootAt(ordinal); }
      @Override public int childCount(int parentNode) {
        return M3PageableTree.this.childCount(parentNode);
      }
      @Override public int childAt(int parentNode, int ordinal) {
        return M3PageableTree.this.childAt(parentNode, ordinal);
      }
    };
  }

  /** O(1) Euler-interval ancestor check. A node is considered its own ancestor. */
  public boolean isAncestor(int ancestor, int descendant) {
    int a = checkNode(ancestor);
    int d = checkNode(descendant);
    if (rootByNode[a] != rootByNode[d]) return false;
    int start = preorderRank[a];
    int candidate = preorderRank[d];
    return candidate >= start && candidate < start + subtreeSize[a];
  }

  /** O(log depth) kth ancestor. k=0 returns the node itself; -1 means no such ancestor. */
  public int kthAncestor(int node, int k) {
    int current = checkNode(node);
    if (k < 0) throw new IllegalArgumentException("negative ancestor distance");
    if (jumpLevels == 0) {
      for (int step = 0; step < k && current != NO_PARENT; step++) {
        current = parent[current];
      }
      return current;
    }

    int bit = 0;
    int remaining = k;
    while (remaining != 0 && current != NO_PARENT) {
      if ((remaining & 1) != 0) {
        if (bit >= jumpLevels) return NO_PARENT;
        current = jump[bit * nodeCount() + current];
      }
      remaining >>>= 1;
      bit++;
    }
    return current;
  }

  /** O(log depth) lowest common ancestor; returns -1 for nodes in different roots. */
  public int lowestCommonAncestor(int left, int right) {
    int a = checkNode(left);
    int b = checkNode(right);
    if (rootByNode[a] != rootByNode[b]) return NO_PARENT;
    if (depth[a] < depth[b]) b = kthAncestor(b, depth[b] - depth[a]);
    else if (depth[b] < depth[a]) a = kthAncestor(a, depth[a] - depth[b]);
    if (a == b) return a;

    if (jumpLevels == 0) {
      while (a != b) {
        a = parent[a];
        b = parent[b];
      }
      return a;
    }

    for (int level = jumpLevels - 1; level >= 0; level--) {
      int upA = jump[level * nodeCount() + a];
      int upB = jump[level * nodeCount() + b];
      if (upA != upB) {
        a = upA;
        b = upB;
      }
    }
    return parent[a];
  }

  /** Number of edges on the unique tree path, or -1 when nodes are in different roots. */
  public int distance(int left, int right) {
    int lca = lowestCommonAncestor(left, right);
    return lca == NO_PARENT ? NO_PARENT
        : Math.addExact(depth[left] - depth[lca], depth[right] - depth[lca]);
  }

  /** Unique tree path including both endpoints; empty when nodes are in different roots. */
  public int[] path(int left, int right) {
    int lca = lowestCommonAncestor(left, right);
    if (lca == NO_PARENT) return new int[0];
    int leftCount = depth[left] - depth[lca];
    int rightCount = depth[right] - depth[lca];
    int[] result = new int[Math.addExact(Math.addExact(leftCount, rightCount), 1)];

    int at = left;
    for (int index = 0; index < leftCount; index++) {
      result[index] = at;
      at = parent[at];
    }
    result[leftCount] = lca;

    at = right;
    for (int index = result.length - 1; index > leftCount; index--) {
      result[index] = at;
      at = parent[at];
    }
    return result;
  }

  public Page rootsPage(int offset, int limit) {
    return pageArray(NO_PARENT, roots, offset, limit);
  }

  public Page childrenPage(int parentNode, int offset, int limit) {
    int parentNodeChecked = checkNode(parentNode);
    requirePage(offset, limit);
    int start = childOffsets[parentNodeChecked];
    int total = childOffsets[parentNodeChecked + 1] - start;
    int from = Math.min(offset, total);
    int size = Math.min(limit, total - from);
    return new Page(
        parentNodeChecked,
        from,
        total,
        Arrays.copyOfRange(children, start + from, start + from + size));
  }

  public Page siblingsPage(int node, int offset, int limit) {
    int checked = checkNode(node);
    int p = parent[checked];
    return p == NO_PARENT ? rootsPage(offset, limit) : childrenPage(p, offset, limit);
  }

  /**
   * Returns the bounded page containing one direct child.
   *
   * <p>This is the snapshot-safe path used by REST/viewer navigation. Legacy offset/limit methods
   * remain unbounded for compatibility.
   */
  public Page pageContaining(int parentNode, int childNode, int limit) {
    requireBoundedLimit(limit);
    int ordinal = childOrdinal(parentNode, childNode);
    if (ordinal < 0) {
      throw new IllegalArgumentException("node is not a direct child of parent");
    }
    return childrenPage(parentNode, (ordinal / limit) * limit, limit);
  }

  public M3PageableTreeCursor rootsCursor(int nextOrdinal) {
    if (nextOrdinal < 0 || nextOrdinal > roots.length) {
      throw new IllegalArgumentException("invalid root cursor ordinal");
    }
    return new M3PageableTreeCursor(fingerprint64, NO_PARENT, nextOrdinal);
  }

  public M3PageableTreeCursor childCursor(int parentNode, int nextOrdinal) {
    int checked = checkNode(parentNode);
    int total = childCount(checked);
    if (nextOrdinal < 0 || nextOrdinal > total) {
      throw new IllegalArgumentException("invalid child cursor ordinal");
    }
    return new M3PageableTreeCursor(fingerprint64, checked, nextOrdinal);
  }

  public Page rootsPage(M3PageableTreeCursor cursor, int limit) {
    requireBoundedLimit(limit);
    M3PageableTreeCursor checked = requireCursor(cursor);
    if (!checked.roots()) {
      throw new IllegalArgumentException("cursor is not a root-page cursor");
    }
    if (checked.nextOrdinal() > roots.length) {
      throw new IllegalArgumentException("root cursor exceeds root total");
    }
    return rootsPage(checked.nextOrdinal(), limit);
  }

  public Page childrenPage(M3PageableTreeCursor cursor, int limit) {
    requireBoundedLimit(limit);
    M3PageableTreeCursor checked = requireCursor(cursor);
    if (checked.roots()) {
      throw new IllegalArgumentException("root cursor cannot page children");
    }
    int parentNode = checkNode(checked.parentNode());
    int total = childCount(parentNode);
    if (checked.nextOrdinal() > total) {
      throw new IllegalArgumentException("child cursor exceeds child total");
    }
    return childrenPage(parentNode, checked.nextOrdinal(), limit);
  }

  /**
   * Copies a bounded direct-child window into caller-owned primitive storage.
   *
   * @return number of child coordinates written
   */
  public int readChildren(
      int parentNode,
      int offset,
      int[] destination,
      int destinationOffset,
      int maximum) {
    int checked = checkNode(parentNode);
    Objects.requireNonNull(destination, "destination");
    if (maximum < 0 || maximum > DEFAULT_MAX_PAGE_SIZE) {
      throw new IllegalArgumentException("invalid child copy budget");
    }
    int total = childCount(checked);
    if (offset < 0 || offset > total) {
      throw new IndexOutOfBoundsException("tree child offset: " + offset);
    }
    int count = Math.min(maximum, total - offset);
    Objects.checkFromIndexSize(destinationOffset, count, destination.length);
    int source = childOffsets[checked] + offset;
    System.arraycopy(children, source, destination, destinationOffset, count);
    return count;
  }

  /** Preorder-contiguous subtree page. The subtree root is row zero. */
  public Page subtreePage(int node, int offset, int limit) {
    int checked = checkNode(node);
    requirePage(offset, limit);
    int total = subtreeSize[checked];
    int from = Math.min(offset, total);
    int size = Math.min(limit, total - from);
    int absolute = preorderRank[checked] + from;
    return new Page(
        NO_PARENT,
        from,
        total,
        Arrays.copyOfRange(preorder, absolute, absolute + size));
  }

  public int levelSize(int level) {
    if (level < 0 || level + 1 >= levelOffsets.length) throw new IndexOutOfBoundsException(level);
    return levelOffsets[level + 1] - levelOffsets[level];
  }

  public Page levelPage(int level, int offset, int limit) {
    if (level < 0 || level + 1 >= levelOffsets.length) throw new IndexOutOfBoundsException(level);
    requirePage(offset, limit);
    int start = levelOffsets[level];
    int total = levelOffsets[level + 1] - start;
    int from = Math.min(offset, total);
    int size = Math.min(limit, total - from);
    return new Page(
        NO_PARENT,
        from,
        total,
        Arrays.copyOfRange(levelNodes, start + from, start + from + size));
  }

  /** Generic lazy paging over any source, without freezing descendants. */
  public static Page rootsPage(Source source, int offset, int limit) {
    return rootsPage(source, offset, limit, M3ProgressMonitor.noop());
  }

  public static Page rootsPage(
      Source source, int offset, int limit, M3ProgressMonitor monitor) {
    Objects.requireNonNull(source, "source");
    requirePage(offset, limit);
    int total = source.rootCount();
    int from = Math.min(offset, total);
    int size = Math.min(limit, total - from);
    int[] nodes = new int[size];
    source.readRoots(from, size, nodes, 0, monitor);
    return new Page(NO_PARENT, from, total, nodes);
  }

  /** Generic on-demand child page; only requested rows are touched. */
  public static Page childrenPage(Source source, int parentNode, int offset, int limit) {
    return childrenPage(source, parentNode, offset, limit, M3ProgressMonitor.noop());
  }

  public static Page childrenPage(
      Source source,
      int parentNode,
      int offset,
      int limit,
      M3ProgressMonitor monitor) {
    Objects.requireNonNull(source, "source");
    requirePage(offset, limit);
    int total = source.childCount(parentNode);
    int from = Math.min(offset, total);
    int size = Math.min(limit, total - from);
    int[] nodes = new int[size];
    source.readChildren(parentNode, from, size, nodes, 0, monitor);
    return new Page(parentNode, from, total, nodes);
  }

  /**
   * Starts virtual scrolling for a fixed expanded-node bitset.
   *
   * <p>A set bit means the node's children are visible. Root rows are always visible.</p>
   */
  public VisibleCursor firstVisibleCursor(long[] expandedWords) {
    return new VisibleCursor(0, expansionFingerprint(expandedWords));
  }

  /**
   * Returns the next virtual-scroll window without materializing all visible rows.
   *
   * <p>Collapsed subtrees are skipped in O(1) using precomputed subtree size/Euler coordinates.</p>
   */
  public VisiblePage visiblePage(
      long[] expandedWords,
      VisibleCursor cursor,
      int limit) {
    Objects.requireNonNull(expandedWords, "expandedWords");
    Objects.requireNonNull(cursor, "cursor");
    if (limit <= 0) throw new IllegalArgumentException("limit must be positive");
    long fingerprint = expansionFingerprint(expandedWords);
    if (cursor.expansionFingerprint() != fingerprint) {
      throw new IllegalArgumentException("expanded-node state changed; restart visible cursor");
    }

    int rank = Math.min(cursor.nextPreorderRank(), nodeCount());
    int[] nodes = new int[Math.min(limit, Math.max(0, nodeCount() - rank))];
    int[] depths = new int[nodes.length];
    int size = 0;
    while (rank < nodeCount() && size < limit) {
      int node = preorder[rank];
      nodes[size] = node;
      depths[size] = depth[node];
      size++;
      if (childCount(node) != 0 && !bit(expandedWords, node)) {
        rank = Math.addExact(rank, subtreeSize[node]);
      } else {
        rank++;
      }
    }
    boolean more = rank < nodeCount();
    return new VisiblePage(
        Arrays.copyOf(nodes, size),
        Arrays.copyOf(depths, size),
        new VisibleCursor(rank, fingerprint),
        more);
  }

  public long retainedPrimitivePayloadBytes() {
    return ((long) parent.length + roots.length + childOffsets.length + children.length
            + preorder.length + preorderRank.length + rootByNode.length + depth.length
            + height.length + subtreeSize.length + levelOffsets.length + levelNodes.length
            + jump.length) * Integer.BYTES + Long.BYTES;
  }

  private int checkNode(int node) {
    return Objects.checkIndex(node, parent.length);
  }

  private M3PageableTreeCursor requireCursor(M3PageableTreeCursor cursor) {
    M3PageableTreeCursor checked = Objects.requireNonNull(cursor, "cursor");
    if (checked.snapshotFingerprint64() != fingerprint64) {
      throw new IllegalArgumentException("stale pageable-tree cursor");
    }
    return checked;
  }

  private static void requireBoundedLimit(int limit) {
    if (limit < 1 || limit > DEFAULT_MAX_PAGE_SIZE) {
      throw new IllegalArgumentException("page size exceeds configured default budget");
    }
  }

  private static Page pageArray(int parentNode, int[] source, int offset, int limit) {
    requirePage(offset, limit);
    int from = Math.min(offset, source.length);
    int size = Math.min(limit, source.length - from);
    return new Page(parentNode, from, source.length, Arrays.copyOfRange(source, from, from + size));
  }

  private static void requirePage(int offset, int limit) {
    if (offset < 0) throw new IllegalArgumentException("offset must be nonnegative");
    if (limit <= 0) throw new IllegalArgumentException("limit must be positive");
  }

  private static boolean bit(long[] words, int bit) {
    int word = bit >>> 6;
    return word < words.length && (words[word] & (1L << (bit & 63))) != 0L;
  }

  private static long expansionFingerprint(long[] words) {
    Objects.requireNonNull(words, "words");
    long hash = 0xcbf29ce484222325L;
    for (long word : words) {
      hash ^= word;
      hash *= 0x100000001b3L;
    }
    hash ^= words.length;
    hash *= 0x9e3779b97f4a7c15L;
    hash ^= hash >>> 33;
    return hash;
  }

  private static long computeFingerprint(int[] parent) {
    long hash = mix64(0x4d49505452454531L ^ Integer.toUnsignedLong(parent.length));
    for (int value : parent) {
      hash = mix64(hash ^ Integer.toUnsignedLong(value));
    }
    return hash == 0L ? 1L : hash;
  }

  private static long mix64(long value) {
    long z = value;
    z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
    z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
    return z ^ (z >>> 31);
  }
}
