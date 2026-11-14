/* SPDX-License-Identifier: Apache-2.0 */
// Relocated from com.synexia m3-fastsearch/jdk/internal/m3/fastsearch/knowledge/index/ReciprocalRankFusion.java (blob ae744cd46b0c) by M3_SEARCH_RELOCATE v3.
package jdk.m3.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Query-local RRF over candidate identities, with bounded final selection. */
final class ReciprocalRankFusion {
    private static final int RRF_K = 60;

    private ReciprocalRankFusion() {
    }

    static List<RankedCandidate> rank(
            int[] lexical, int[] vector, HybridSearchOptions options, TopKSelector selector) {
        Map<Integer, Float> fused = new HashMap<>();
        addLane(fused, lexical, options.lexicalWeight());
        addLane(fused, vector, options.vectorWeight());
        int[] ids = new int[fused.size()];
        float[] scores = new float[fused.size()];
        int position = 0;
        for (var entry : fused.entrySet()) {
            ids[position] = entry.getKey();
            scores[position] = entry.getValue();
            position++;
        }
        int[] selected = selector.select(ids, scores, options.limit());
        List<RankedCandidate> result = new ArrayList<>(selected.length);
        for (int index : selected) {
            result.add(new RankedCandidate(ids[index], scores[index]));
        }
        return result;
    }

    private static void addLane(Map<Integer, Float> fused, int[] ids, float weight) {
        if (weight == 0.0f) {
            return;
        }
        for (int rank = 0; rank < ids.length; rank++) {
            // Preserve the old float operation order for weight=1; widen the
            // integer sum before conversion, rather than overflow at huge ranks.
            float reciprocal = 1.0f / (RRF_K + (long) rank + 1.0f);
            fused.merge(ids[rank], weight * reciprocal, Float::sum);
        }
    }

    record RankedCandidate(int documentId, float score) {
    }
}
