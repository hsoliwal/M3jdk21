// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified for M3JDK: attributed package relocation and product module integration.
// SPDX-License-Identifier: Apache-2.0
package com.m3.llm.projection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

/** Byte-budgeted whole-passage selection. Omitted evidence remains recoverable. */
public final class ExtractiveConsolidator {
    public record Projection(String text, List<TextInventoryCell> selected, List<TextInventoryCell> omitted,
            int utf8Bytes, String root) {
        public Projection {
            selected = List.copyOf(selected);
            omitted = List.copyOf(omitted);
        }
    }
    private ExtractiveConsolidator() {}

    public static Projection consolidate(List<Bm25Index.Hit> ranked, int budgetBytes, Set<CellKey> mandatory) {
        if (budgetBytes < 0 || budgetBytes > 1_048_576 || ranked.size() > 4096) {
            throw new IllegalArgumentException("PROJECTION_BOUND");
        }
        List<Bm25Index.Hit> snapshot = List.copyOf(ranked);
        Set<CellKey> pins = Set.copyOf(mandatory);
        Set<CellKey> available = new HashSet<>();
        for (Bm25Index.Hit hit : snapshot) {
            if (!available.add(hit.cell().key())) throw new IllegalArgumentException("DUPLICATE_PROJECTION_KEY");
        }
        if (!available.containsAll(pins)) throw new IllegalArgumentException("MANDATORY_SOURCE_MISSING");
        Set<CellKey> chosen = new HashSet<>();
        long used = 0;
        for (Bm25Index.Hit hit : snapshot) {
            if (pins.contains(hit.cell().key())) {
                used += EntropyValues.utf8(hit.cell().text()).length + (chosen.isEmpty() ? 0 : 2);
                chosen.add(hit.cell().key());
            }
        }
        if (used > budgetBytes) throw new IllegalArgumentException("MANDATORY_BUDGET_EXCEEDED");
        for (Bm25Index.Hit hit : snapshot) {
            if (chosen.contains(hit.cell().key())) continue;
            long cost = EntropyValues.utf8(hit.cell().text()).length + (chosen.isEmpty() ? 0 : 2);
            if (used + cost <= budgetBytes) {
                chosen.add(hit.cell().key());
                used += cost;
            }
        }
        List<TextInventoryCell> selected = new ArrayList<>();
        List<TextInventoryCell> omitted = new ArrayList<>();
        for (Bm25Index.Hit hit : snapshot) {
            (chosen.contains(hit.cell().key()) ? selected : omitted).add(hit.cell());
        }
        String text = String.join("\n\n", selected.stream().map(TextInventoryCell::text).toList());
        String root = EntropyValues.framed("projection/v1", Integer.toString(budgetBytes), text,
                new InventorySnapshot(selected).root(), new InventorySnapshot(omitted).root());
        return new Projection(text, selected, omitted, EntropyValues.utf8(text).length, root);
    }

    /** Rehydrates all leaf evidence; hierarchy cannot silently make omitted facts disappear. */
    public static Projection combine(List<Projection> children, int budgetBytes, Set<CellKey> mandatory) {
        if (children.size() > 4096) throw new IllegalArgumentException("CHILD_LIMIT");
        TreeMap<CellKey, TextInventoryCell> union = new TreeMap<>();
        for (Projection child : children) {
            for (List<TextInventoryCell> part : List.of(child.selected(), child.omitted())) {
                for (TextInventoryCell cell : part) {
                    TextInventoryCell previous = union.putIfAbsent(cell.key(), cell);
                    if (previous != null && !previous.equals(cell)) throw new IllegalArgumentException("CONFLICTING_LEAF");
                    if (union.size() > 4096) throw new IllegalArgumentException("LEAF_LIMIT");
                }
            }
        }
        return consolidate(union.values().stream().map(cell -> new Bm25Index.Hit(cell, 0)).toList(), budgetBytes, mandatory);
    }
}
