// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.ast;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.source.tree.Tree;
import com.synexia.indexstring.IndexDictionary;
import com.synexia.indexstring.IndexRuntime;
import com.synexia.indexstring.IndexSpace;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class MIndexASTPrecomputeTest {
  @Test
  void occurrencePrecomputeBuildsIntervalsBinaryLiftingPathHashesAndPostings() throws Exception {
    Fixture f = fixture();
    MIndexASTDocument document =
        f.parser.parse(
            "Sample.java",
            """
            class Sample {
              int f(int x) { return x + 1; }
              int g(int x) { return x + 1; }
            }
            """);

    MIndexASTPrecompute index = document.precompute();

    assertEquals(document.occurrenceCount(), index.occurrenceCount());
    assertEquals(document.occurrenceCount(), index.subtreeSize(document.rootOccurrence()));
    assertEquals(0, index.preorderIndex(document.rootOccurrence()));
    assertEquals(document.rootOccurrence(), index.preorderOccurrence(0));
    assertTrue(index.primitivePayloadBytes() > 0L);

    for (int preorder = 0; preorder < index.occurrenceCount(); preorder++) {
      int occurrence = index.preorderOccurrence(preorder);
      assertEquals(preorder, index.preorderIndex(occurrence));
      assertEquals(
          index.subtreeSize(occurrence),
          index.subtreeOccurrences(occurrence).length);
      int parent = document.parentOccurrence(occurrence);
      if (parent >= 0) {
        assertTrue(index.isAncestor(parent, occurrence));
        assertEquals(parent, index.kthAncestor(occurrence, 1));
        assertTrue(index.childOrdinal(occurrence) >= 0);
      }
    }

    int[] plusOccurrences =
        index.occurrences(Tree.Kind.PLUS);
    assertEquals(2, plusOccurrences.length);

    MIndexAST repeatedPlus = document.atomAtOccurrence(plusOccurrences[0]);
    assertArrayEquals(plusOccurrences, sorted(index.occurrences(repeatedPlus)));

    int firstPlus = plusOccurrences[0];
    int secondPlus = plusOccurrences[1];
    int firstMethod = ancestorOfKind(document, firstPlus, Tree.Kind.METHOD);
    int secondMethod = ancestorOfKind(document, secondPlus, Tree.Kind.METHOD);

    assertTrue(index.isAncestor(firstMethod, firstPlus));
    assertTrue(index.isAncestor(secondMethod, secondPlus));
    assertEquals(firstMethod, index.lowestCommonAncestor(firstMethod, firstPlus));
    assertEquals(secondMethod, index.lowestCommonAncestor(secondMethod, secondPlus));
    assertTrue(index.distance(firstMethod, firstPlus) > 0);

    MIndexASTLcaSparseTable fastLca = index.constantTimeLca();
    assertEquals(
        index.lowestCommonAncestor(firstPlus, secondPlus),
        fastLca.lowestCommonAncestor(firstPlus, secondPlus));
    assertEquals(
        index.lowestCommonAncestor(firstMethod, firstPlus),
        fastLca.lowestCommonAncestor(firstMethod, firstPlus));
    assertEquals(2 * document.occurrenceCount() - 1, fastLca.eulerLength());
    assertTrue(fastLca.primitivePayloadBytes() > 0L);

    assertEquals(
        index.kindPathHash(firstMethod, firstPlus),
        index.kindPathHash(secondMethod, secondPlus));
    assertNotEquals(
        index.exactPathHash(firstMethod, firstPlus),
        index.exactPathHash(secondMethod, secondPlus));

    int[] path = index.pathOccurrences(firstMethod, firstPlus);
    assertEquals(firstMethod, path[0]);
    assertEquals(firstPlus, path[path.length - 1]);
  }

  @Test
  void sourceIndexFindsDeepestOccurrenceAtCursorOffset() throws Exception {
    Fixture f = fixture();
    String source =
        """
        class Sample {
          int f(int x) { return x + 1; }
        }
        """;
    MIndexASTDocument document = f.parser.parse("Sample.java", source);
    MIndexASTSourceIndex sourceIndex = document.sourceIndex();

    int plusOffset = source.indexOf('+');
    assertTrue(plusOffset >= 0);
    int deepest = sourceIndex.deepestContaining(plusOffset);
    assertTrue(deepest >= 0);
    assertEquals(Tree.Kind.PLUS, document.atomAtOccurrence(deepest).kind());

    int[] path = sourceIndex.containingPath(plusOffset);
    assertTrue(path.length >= 2);
    assertEquals(document.rootOccurrence(), path[0]);
    assertEquals(deepest, path[path.length - 1]);
    assertTrue(sourceIndex.indexedOccurrenceCount() > 0);
    assertTrue(sourceIndex.primitivePayloadBytes() > 0L);
    assertTrue(sourceIndex.firstStartingAtOrAfter(0) >= 0);
  }

  @Test
  void poolPrecomputeIndexesKindsLabelsHashesParentsAndQueryCandidates() throws Exception {
    Fixture f = fixture();
    MIndexASTDocument document =
        f.parser.parse(
            "Sample.java",
            """
            class Sample {
              int f(int x) { return x + 1; }
              int g(int x) { return x + 1; }
            }
            """);

    MIndexAST plus =
        document.atomAtOccurrence(
            document.findOccurrences(
                MIndexASTQuery.builder(f.pool).rootKind(Tree.Kind.PLUS).build())[0]);
    MIndexASTPoolPrecompute index = f.pool.precompute();

    assertEquals(f.pool.size(), index.atomCount());
    assertEquals(1, index.handles(Tree.Kind.PLUS).length);
    assertArrayEquals(
        new int[] {plus.handle()},
        index.handlesByExactHash(plus.exactHash64()));
    assertArrayEquals(
        new int[] {plus.handle()},
        index.handlesByStructuralHash(plus.structuralHash64()));
    assertTrue(index.incomingParentCount(plus.handle()) >= 1);
    assertTrue(index.parents(plus.handle()).length >= 1);
    assertTrue(index.primitivePayloadBytes() > 0L);

    int depth = plus.depth();
    int leaves = plus.expandedLeafCount();
    MIndexASTQuery query =
        MIndexASTQuery.builder(f.pool)
            .rootKind(Tree.Kind.PLUS)
            .exactHash(plus.exactHash64())
            .structuralHash(plus.structuralHash64())
            .logicHash(plus.logicHash64())
            .depth(depth, depth)
            .expandedLeafCount(leaves, leaves)
            .build();
    assertArrayEquals(new int[] {plus.handle()}, index.find(query));

    int[] methods = index.handles(Tree.Kind.METHOD);
    assertEquals(2, methods.length);
    assertEquals(
        f.pool.structuralHash64(methods[0]),
        f.pool.structuralHash64(methods[1]));

    boolean methodGroupFound = false;
    for (int[] group : index.structuralHashGroups(2)) {
      if (contains(group, methods[0]) && contains(group, methods[1])) {
        methodGroupFound = true;
        break;
      }
    }
    assertTrue(methodGroupFound);
  }

  @Test
  void kmpAndAhoCorasickScanThePrecomputedKindStream() throws Exception {
    Fixture f = fixture();
    MIndexASTDocument document =
        f.parser.parse(
            "Sample.java",
            """
            class Sample {
              int f(int x) { return x + 1; }
              int g(int x) { return x + 1; }
            }
            """);
    MIndexASTPrecompute index = document.precompute();

    int[] methods = index.occurrences(Tree.Kind.METHOD);
    assertEquals(2, methods.length);

    int firstMethodPreorder = index.preorderIndex(methods[0]);
    Tree.Kind[] prefix =
        new Tree.Kind[] {
          kindAt(index, firstMethodPreorder),
          kindAt(index, firstMethodPreorder + 1),
          kindAt(index, firstMethodPreorder + 2)
        };
    MIndexASTKindPattern methodPrefix = MIndexASTKindPattern.of(prefix);
    int[] starts = methodPrefix.findAll(index);
    assertTrue(starts.length >= 2);

    int plus = index.occurrences(Tree.Kind.PLUS)[0];
    int method = ancestorOfKind(document, plus, Tree.Kind.METHOD);
    int[] methodToPlus = index.pathOccurrences(method, plus);
    Tree.Kind[] pathKinds = new Tree.Kind[methodToPlus.length];
    for (int i = 0; i < pathKinds.length; i++) {
      pathKinds[i] = document.atomAtOccurrence(methodToPlus[i]).kind();
    }
    MIndexASTKindPattern pathPattern = MIndexASTKindPattern.of(pathKinds);
    assertTrue(pathPattern.matchesPath(index, method, plus));

    MIndexASTKindPattern plusPattern =
        MIndexASTKindPattern.of(
            Tree.Kind.PLUS,
            document.atomAtOccurrence(index.subtreeOccurrences(plus)[1]).kind());

    MIndexASTKindAutomaton automaton =
        MIndexASTKindAutomaton.compile(methodPrefix, plusPattern);
    List<MIndexASTKindAutomaton.Match> matches = automaton.scanPreorder(index);
    assertTrue(matches.stream().anyMatch(match -> match.patternIndex() == 0));
    assertTrue(matches.stream().anyMatch(match -> match.patternIndex() == 1));
    assertTrue(automaton.primitivePayloadBytes() > 0L);
  }

  private static Tree.Kind kindAt(MIndexASTPrecompute index, int preorder) {
    return Tree.Kind.values()[index.preorderKindOrdinal(preorder)];
  }

  private static int ancestorOfKind(
      MIndexASTDocument document, int occurrence, Tree.Kind kind) {
    for (int current = occurrence; current >= 0; current = document.parentOccurrence(current)) {
      if (document.atomAtOccurrence(current).kind() == kind) return current;
    }
    fail("No ancestor of kind " + kind + " for occurrence " + occurrence);
    return -1;
  }

  private static boolean contains(int[] values, int target) {
    for (int value : values) if (value == target) return true;
    return false;
  }

  private static int[] sorted(int[] values) {
    int[] copy = values.clone();
    Arrays.sort(copy);
    return copy;
  }

  private static Fixture fixture() {
    IndexDictionary java =
        IndexDictionary.of(
            1,
            "java",
            "v1",
            List.of(
                "Sample.java",
                "Sample",
                "f",
                "g",
                "x",
                "int",
                "1"));
    IndexRuntime runtime = new IndexRuntime(IndexSpace.builder().add(java).build());
    MIndexASTPool pool = new MIndexASTPool(runtime, 1);
    return new Fixture(pool, new MIndexASTParser(pool));
  }

  private record Fixture(MIndexASTPool pool, MIndexASTParser parser) {}
}
