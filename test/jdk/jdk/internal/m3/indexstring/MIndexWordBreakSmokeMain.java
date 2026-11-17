// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.indexstring;

import java.util.Arrays;
import java.util.List;

/**
 * Smoke main of the relocated word-break plan (recipe wordbreak-20261107): fixed vocabularies whose answers can be
 * checked by hand. The donor differential is {@code recipe/wordbreak-20261107/Ix2Diff.java}.
 */
public final class MIndexWordBreakSmokeMain {
    private MIndexWordBreakSmokeMain() { }

    public static void main(String[] args) {
        MIndexWordBreakPlan plan = MIndexWordBreakPlan.compile(
                MIndexVocabularySearchTree.compile(List.of("apple", "pen", "applepen", "pine", "pineapple", "cats", "dog", "sand", "and", "cat")));
        MIndexWordBreakPlan.Result legacy = plan.segment("pineapplepenapple");
        MIndexWordBreakPlan.Result shortest = plan.minimumSegments("pineapplepenapple");
        check(plan.canSegment("applepenapple") && !plan.canSegment("catsandog"), "feasibility");
        check(shortest.segmentCount() == 3, "minimum segments of pineapplepenapple: " + shortest.segmentViews());
        check(legacy.segmentable() && String.join("", strings(legacy)).equals("pineapplepenapple"), "legacy covers the input");
        check(Arrays.equals(shortest.boundaries(), new int[] {0, 4, 12, 17}), "boundaries " + Arrays.toString(shortest.boundaries()));
        check(shortest.vocabularyValue(0).toString().equals("pine"), "row value (3-segment tie: stable first)");
        check(!plan.segment("catsandog").segmentable() && plan.maximumWordLength() == 9, "unsegmentable / max word");
        System.out.println("WORD_BREAK_SMOKE_PASS legacy=" + strings(legacy) + " minimum=" + strings(shortest)
                + " rows=" + Arrays.toString(shortest.vocabularyRows()));
    }

    private static List<String> strings(MIndexWordBreakPlan.Result result) {
        return result.segmentViews().stream().map(CharSequence::toString).toList();
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }
}
