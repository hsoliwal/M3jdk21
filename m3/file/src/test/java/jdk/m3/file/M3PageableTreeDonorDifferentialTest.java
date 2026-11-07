/*
 * Copyright (c) 2026 Synexia and contributors.
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.file;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import jdk.m3.file.pageabletree.donor.InMemoryPageableTree;
import jdk.m3.file.pageabletree.donor.PageableTreePaths;
import jdk.m3.file.pageabletree.donor.PageableTreeSession;
import jdk.m3.file.pageabletree.donor.PageableTreeWalker;
import jdk.m3.file.pageabletree.donor.StaleTreeCursorException;
import jdk.m3.file.pageabletree.donor.TreeCursor;
import jdk.m3.file.pageabletree.donor.TreeNode;
import jdk.m3.file.pageabletree.donor.TreePage;
import jdk.m3.file.pageabletree.donor.TreePageRequest;
import jdk.m3.file.pageabletree.donor.TreeProgressMonitor;
import jdk.m3.file.pageabletree.donor.VisibleTreeWindow;
import org.junit.jupiter.api.Test;

/**
 * Differential oracle for {@link M3PageableTree}: the m3scale (synexia) {@code m3-pageable-tree} donor (generic
 * {@code K,V} object trees, string snapshot ids, offset cursors, expand/collapse sessions, visible windows, walkers,
 * root paths) is driven with the same forest shapes as the primitive owner and every observable sequence is compared.
 *
 * <p>Mapping: donor node id = owner node coordinate, donor sibling ordinal = node coordinate (so both order siblings by
 * ascending coordinate), donor {@code mayHaveChildren} = owner {@code childCount(node) != 0}. The donor fixtures live
 * under {@code jdk.m3.file.pageabletree.donor} (test tree only, byte-identical to the donor except package).</p>
 */
//FLLDCIM: TOP - m3scale plan rank 6 (ALREADY-verify): no port; donor as oracle, NEC in qualification/pageable-tree-donor-verify-20261107
final class M3PageableTreeDonorDifferentialTest {

  private static final int[] LIMITS = {1, 2, 3, 7, 4096};
  private static final int[] BRANCH_PAGE_SIZES = {1, 3, 4096};
  private static final int[] WINDOW_LIMITS = {1, 2, 5, 4096};
  private static final int[] RANDOM_SIZES = {2, 5, 13, 40, 257};
  private static final int SEEDS = 3;
  private static final TreeProgressMonitor NONE = TreeProgressMonitor.none();
  /** PageableTreeCoreTest sample: a=0 b=1 a1=2 a2=3 a3=4 b1=5 a1x=6. */
  private static final int[] DONOR_SAMPLE = {-1, -1, 0, 0, 0, 1, 2};

  private record Pair(int[] parent, M3PageableTree owner, InMemoryPageableTree<Integer, String> donor) {}

  private static Pair pair(int[] parent, String snapshot) {
    int[] childCount = new int[parent.length];
    for (int p : parent) {
      if (p >= 0) childCount[p]++;
    }
    InMemoryPageableTree.Builder<Integer, String> builder = InMemoryPageableTree.builder(snapshot);
    for (int node = 0; node < parent.length; node++) {
      boolean may = childCount[node] != 0;
      builder.add(parent[node] < 0
          ? TreeNode.root(node, "n" + node, may, node)
          : TreeNode.child(node, parent[node], "n" + node, may, node));
    }
    return new Pair(parent, M3PageableTree.freeze(parent), builder.build());
  }

  private static int[] randomForest(int nodes, long seed) {
    Random random = new Random(seed);
    int[] parent = new int[nodes];
    for (int node = 0; node < nodes; node++) {
      parent[node] = node == 0 || random.nextInt(4) == 0 ? M3PageableTree.NO_PARENT : random.nextInt(node);
    }
    return parent;
  }

  private static List<Pair> shapes() {
    List<Pair> shapes = new ArrayList<>();
    shapes.add(pair(new int[0], "empty"));
    shapes.add(pair(new int[] {-1}, "single"));
    shapes.add(pair(DONOR_SAMPLE, "sample"));
    for (int size : RANDOM_SIZES) {
      for (int seed = 1; seed <= SEEDS; seed++) {
        shapes.add(pair(randomForest(size, 31L * size + seed), "r" + size + "s" + seed));
      }
    }
    return shapes;
  }

  private static List<Integer> ids(TreePage<Integer, String> page) {
    return page.items().stream().map(TreeNode::id).toList();
  }

  private static List<Integer> boxed(int[] nodes) {
    List<Integer> out = new ArrayList<>(nodes.length);
    for (int node : nodes) out.add(node);
    return out;
  }

  @Test
  void donorSampleMapsOntoOwnerCoordinates() {
    M3PageableTree owner = pair(DONOR_SAMPLE, "sample").owner();
    int[] preorder = new int[owner.nodeCount()];
    for (int rank = 0; rank < preorder.length; rank++) preorder[rank] = owner.nodeAtPreorderRank(rank);
    assertArrayEquals(new int[] {0, 2, 6, 3, 4, 1, 5}, preorder, "a a1 a1x a2 a3 b b1");
    assertArrayEquals(new int[] {0, 1}, owner.rootsPage(0, 4096).nodes());
    assertArrayEquals(new int[] {2, 3, 4}, owner.childrenPage(0, 0, 4096).nodes());
  }

  @Test
  void rootAndChildPagesAgreeForEveryParentOffsetCursorAndLimit() {
    for (Pair pair : shapes()) {
      for (int limit : LIMITS) {
        comparePages(pair, Optional.empty(), limit);
        for (int node = 0; node < pair.parent().length; node++) {
          comparePages(pair, Optional.of(node), limit);
        }
      }
    }
  }

  private static void comparePages(Pair pair, Optional<Integer> parent, int limit) {
    M3PageableTree owner = pair.owner();
    String where = pair.donor().snapshotId() + " parent=" + parent + " limit=" + limit;
    Optional<TreeCursor> cursor = Optional.empty();
    int offset = 0;
    List<Integer> donorSequence = new ArrayList<>();
    List<Integer> ownerOffsetSequence = new ArrayList<>();
    List<Integer> ownerCursorSequence = new ArrayList<>();
    while (true) {
      TreePageRequest request = cursor.map(c -> TreePageRequest.after(limit, c)).orElse(TreePageRequest.first(limit));
      TreePage<Integer, String> donorPage = parent.isPresent()
          ? pair.donor().children(parent.orElseThrow(), request, NONE)
          : pair.donor().roots(request, NONE);
      M3PageableTree.Page ownerPage = parent.isPresent()
          ? owner.childrenPage(parent.orElseThrow(), offset, limit)
          : owner.rootsPage(offset, limit);
      M3PageableTreeCursor ownerCursor = parent.isPresent()
          ? owner.childCursor(parent.orElseThrow(), offset)
          : owner.rootsCursor(offset);
      M3PageableTree.Page ownerCursorPage = parent.isPresent()
          ? owner.childrenPage(ownerCursor, limit)
          : owner.rootsPage(ownerCursor, limit);
      assertEquals(ids(donorPage), boxed(ownerPage.nodes()), where + " offset=" + offset);
      assertArrayEquals(ownerPage.nodes(), ownerCursorPage.nodes(), where + " cursor page");
      assertEquals(donorPage.totalChildren().orElseThrow(), ownerPage.total(), where + " total");
      assertEquals(donorPage.hasNext(), ownerPage.hasNext(), where + " hasNext");
      assertEquals(offset, ownerPage.offset(), where + " page offset");
      donorSequence.addAll(ids(donorPage));
      ownerOffsetSequence.addAll(boxed(ownerPage.nodes()));
      ownerCursorSequence.addAll(boxed(ownerCursorPage.nodes()));
      if (!donorPage.hasNext()) break;
      cursor = donorPage.nextCursor();
      assertEquals(ownerPage.nextOffset(), cursor.orElseThrow().position(), where + " next position");
      offset = ownerPage.nextOffset();
    }
    assertEquals(donorSequence, ownerOffsetSequence, where + " full sequence");
    assertEquals(donorSequence, ownerCursorSequence, where + " full cursor sequence");
  }

  @Test
  void staleCursorsFailClosedInBothAndContentFingerprintIsTheOwnerBinding() {
    Pair pair = pair(DONOR_SAMPLE, "v1");
    TreeCursor stale = TreeCursor.offset("v0", 0);
    assertThrows(StaleTreeCursorException.class,
        () -> pair.donor().roots(TreePageRequest.after(1, stale), NONE));
    M3PageableTree otherShape = M3PageableTree.freeze(new int[] {-1, 0, 0});
    assertThrows(IllegalArgumentException.class,
        () -> pair.owner().rootsPage(otherShape.rootsCursor(0), 1));
    // NEC: the owner binds cursors to the content fingerprint, the donor to a caller-chosen snapshot id string;
    // an equal-content tree therefore accepts the owner's cursor (the donor would refuse a different id).
    M3PageableTree sameContent = M3PageableTree.freeze(DONOR_SAMPLE.clone());
    assertEquals(sameContent.fingerprint64(), pair.owner().fingerprint64());
    assertArrayEquals(new int[] {0}, sameContent.rootsPage(pair.owner().rootsCursor(0), 1).nodes());
    assertThrows(StaleTreeCursorException.class, () -> pair(DONOR_SAMPLE, "v2").donor()
        .roots(TreePageRequest.after(1, TreeCursor.offset("v1", 0)), NONE));
  }

  private record VisibleRow(int node, int depth, boolean expanded) {}

  private static List<VisibleRow> ownerVisibleRows(M3PageableTree owner, Set<Integer> expanded, int pageLimit) {
    long[] words = new long[(owner.nodeCount() + 63) >>> 6];
    for (int node : expanded) words[node >>> 6] |= 1L << (node & 63);
    List<VisibleRow> rows = new ArrayList<>();
    M3PageableTree.VisibleCursor cursor = owner.firstVisibleCursor(words);
    while (true) {
      M3PageableTree.VisiblePage page = owner.visiblePage(words, cursor, pageLimit);
      for (int ordinal = 0; ordinal < page.size(); ordinal++) {
        int node = page.nodeAt(ordinal);
        rows.add(new VisibleRow(node, page.depthAt(ordinal), owner.childCount(node) != 0 && expanded.contains(node)));
      }
      if (!page.hasMore()) return rows;
      cursor = page.nextCursor();
    }
  }

  private static List<VisibleRow> donorRows(VisibleTreeWindow.Window<Integer, String> window) {
    return window.rows().stream().map(row -> new VisibleRow(row.node().id(), row.depth(), row.expanded())).toList();
  }

  @Test
  void visibleWindowsAgreeForEveryExpansionSetOffsetAndLimit() {
    for (Pair pair : shapes()) {
      int nodes = pair.parent().length;
      Random random = new Random(nodes * 7919L);
      for (int round = 0; round < 3; round++) {
        Set<Integer> expanded = new TreeSet<>();
        for (int node = 0; node < nodes; node++) {
          if (random.nextInt(3) != 0) expanded.add(node); // includes leaves: donor expands only mayHaveChildren nodes
        }
        for (int branchPageSize : BRANCH_PAGE_SIZES) {
          List<VisibleRow> ownerRows = ownerVisibleRows(pair.owner(), expanded, branchPageSize);
          PageableTreeSession<Integer, String> session = new PageableTreeSession<>(pair.donor(), branchPageSize, 16);
          expanded.forEach(session::expand);
          int total = ownerRows.size();
          for (int offset : new int[] {0, 1, 2, total / 2, Math.max(0, total - 1), total, total + 3}) {
            for (int limit : WINDOW_LIMITS) {
              VisibleTreeWindow.Window<Integer, String> window = VisibleTreeWindow.read(session, offset, limit, NONE);
              String where = pair.donor().snapshotId() + " expanded=" + expanded + " page=" + branchPageSize
                  + " offset=" + offset + " limit=" + limit;
              int from = Math.min(offset, total);
              int to = Math.min(offset + limit, total);
              assertEquals(ownerRows.subList(from, to), donorRows(window), where);
              boolean more = offset + limit < total;
              assertEquals(more, window.hasMore(), where + " hasMore");
              assertEquals(more ? offset + limit + 1L : total, window.scannedVisibleRows(), where + " scanned");
              assertEquals(offset, window.offset(), where + " offset");
            }
          }
        }
      }
    }
  }

  @Test
  void depthFirstWalkIsPreorderAndBreadthFirstWalkIsLevelOrder() {
    for (Pair pair : shapes()) {
      M3PageableTree owner = pair.owner();
      List<Integer> preorder = new ArrayList<>();
      List<Integer> preorderDepths = new ArrayList<>();
      for (int rank = 0; rank < owner.nodeCount(); rank++) {
        int node = owner.nodeAtPreorderRank(rank);
        preorder.add(node);
        preorderDepths.add(owner.depth(node));
      }
      List<Integer> levelOrder = new ArrayList<>();
      List<Integer> levelDepths = new ArrayList<>();
      for (int level = 0; level < owner.levelCount(); level++) {
        for (int node : owner.levelPage(level, 0, 4096).nodes()) {
          levelOrder.add(node);
          levelDepths.add(level);
        }
      }
      for (int pageSize : new int[] {1, 2, 7}) {
        String where = pair.donor().snapshotId() + " pageSize=" + pageSize;
        List<PageableTreeWalker.Visit<Integer, String>> depth =
            PageableTreeWalker.walk(pair.donor(), pageSize, PageableTreeWalker.Order.DEPTH_FIRST, NONE);
        assertEquals(preorder, depth.stream().map(visit -> visit.node().id()).toList(), where + " depth-first");
        assertEquals(preorderDepths, depth.stream().map(PageableTreeWalker.Visit::depth).toList(), where + " depths");
        List<PageableTreeWalker.Visit<Integer, String>> breadth =
            PageableTreeWalker.walk(pair.donor(), pageSize, PageableTreeWalker.Order.BREADTH_FIRST, NONE);
        assertEquals(levelOrder, breadth.stream().map(visit -> visit.node().id()).toList(), where + " breadth-first");
        assertEquals(levelDepths, breadth.stream().map(PageableTreeWalker.Visit::depth).toList(), where + " levels");
      }
    }
  }

  @Test
  void rootPathsAgreeWithOwnerPathFromTreeRoot() {
    for (Pair pair : shapes()) {
      for (int node = 0; node < pair.parent().length; node++) {
        List<Integer> donorPath = PageableTreePaths.pathToRoot(pair.donor(), node, NONE)
            .stream().map(TreeNode::id).toList();
        int[] ownerPath = pair.owner().path(pair.owner().treeRoot(node), node);
        assertEquals(donorPath, boxed(ownerPath), pair.donor().snapshotId() + " node=" + node);
        assertEquals(pair.owner().depth(node) + 1, ownerPath.length);
      }
    }
  }
}
