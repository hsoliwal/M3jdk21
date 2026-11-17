// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.indexstring;

import java.util.Arrays;

/**
 * Smoke main of the relocated trie core (recipe trie-core-20261107): a fixed vocabulary through
 * {@link MIndexLiteralSearchTree} and its {@link MIndexTrieNavigation}. The full donor differential is
 * {@code recipe/trie-core-20261107/Ix1Diff.java}.
 */
public final class MIndexTrieCoreSmokeMain {
    private MIndexTrieCoreSmokeMain() { }

    public static void main(String[] args) {
        MIndexLiteralSearchTree tree = MIndexLiteralSearchTree.builder()
                .add("apple", 0).add("apply", 1).add("ape", 2).add("banana", 3).freeze();
        MIndexTrieNavigation navigation = tree.navigation();
        MIndexTrieNavigation.Selection near = navigation.select(MIndexTrieNavigation.boundedLevenshtein("appla", 1));
        int prefix = navigation.longestPrefixPayload("applesauce", 0, 10, MIndexTrieNavigation.Direction.FORWARD, null);
        int reverse = navigation.stateFor("ana", 0, 3, MIndexTrieNavigation.Direction.REVERSE, null);
        check(tree.exact("apply") == 1 && tree.exact("appl") == -1, "exact");
        check(tree.findFirst("xxbananax").equals(new MIndexLiteralSearchTree.Match(3, 2, 8)), "findFirst");
        check(Arrays.equals(near.payloads(), new int[] {0, 1}), "bounded Levenshtein 1 of appla");
        check(prefix == 0, "longest prefix of applesauce");
        check(reverse == MIndexTrieNavigation.DEAD, "reverse ana is not a forward prefix");
        check(navigation.terminalCount() == 4 && navigation.maximumLength(MIndexTrieNavigation.ROOT) == 6, "shape");
        System.out.println("TRIE_CORE_SMOKE_PASS states=" + navigation.stateCount() + " terminals="
                + navigation.terminalCount() + " near=" + Arrays.toString(near.payloads()) + " " + near.stats());
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }
}
