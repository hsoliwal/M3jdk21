// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified for M3JDK: attributed package relocation and product module integration.
// SPDX-License-Identifier: Apache-2.0
package com.m3.llm.projection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;

/** Bounded immutable sparse inventory with exact and namespace-local row indexes. */
public final class InventorySnapshot {
    public static final int MAX_CELLS = 100_000;
    private final NavigableMap<CellKey, TextInventoryCell> cells;
    private final NavigableMap<String, NavigableMap<String, List<TextInventoryCell>>> rows;
    private final BloomHint bloom;
    private final String root;

    public InventorySnapshot(Collection<TextInventoryCell> input) {
        if (input.size() > MAX_CELLS) throw new IllegalArgumentException("CELL_LIMIT");
        TreeMap<CellKey, TextInventoryCell> exact = new TreeMap<>();
        long bytes = 12;
        for (TextInventoryCell cell : input) {
            TextInventoryCell previous = exact.putIfAbsent(cell.key(), cell);
            if (previous != null && !previous.equals(cell)) {
                throw new IllegalArgumentException("CONFLICTING_CELL");
            }
            if (previous == null) {
                CellKey key = cell.key();
                bytes += 32L + EntropyValues.utf8(key.namespace()).length + EntropyValues.utf8(key.row()).length
                        + EntropyValues.utf8(key.family()).length + EntropyValues.utf8(key.qualifier()).length
                        + EntropyValues.utf8(cell.text()).length + 64;
                if (bytes + 32 > SnapshotCodec.MAX_BYTES) throw new IllegalArgumentException("SNAPSHOT_LIMIT");
            }
        }
        cells = Collections.unmodifiableNavigableMap(exact);
        TreeMap<String, NavigableMap<String, List<TextInventoryCell>>> grouped = new TreeMap<>(EntropyValues::compare);
        TreeSet<String> tokens = new TreeSet<>();
        for (TextInventoryCell cell : exact.values()) {
            grouped.computeIfAbsent(cell.key().namespace(), unused -> new TreeMap<>(EntropyValues::compare))
                    .computeIfAbsent(cell.key().row(), unused -> new ArrayList<>()).add(cell);
            tokens.add(cell.key().rowToken());
        }
        grouped.replaceAll((namespace, byRow) -> {
            byRow.replaceAll((row, values) -> List.copyOf(values));
            return Collections.unmodifiableNavigableMap(byRow);
        });
        rows = Collections.unmodifiableNavigableMap(grouped);
        bloom = new BloomHint(tokens);
        root = SnapshotCodec.payloadHash(this);
    }

    public String root() { return root; }
    public int size() { return cells.size(); }
    public List<TextInventoryCell> cells() { return List.copyOf(cells.values()); }

    public Optional<TextInventoryCell> get(CellKey key) {
        if (!bloom.mightContain(key.rowToken())) return Optional.empty();
        return Optional.ofNullable(cells.get(key));
    }

    /** Returns the newest version <= asOf for one family/qualifier per matching row. */
    public List<TextInventoryCell> scan(String namespace, String prefix, String family, String qualifier,
            long asOf, int limit) {
        EntropyValues.text(namespace, 512, false);
        EntropyValues.text(prefix, 512, true);
        EntropyValues.text(family, 512, false);
        EntropyValues.text(qualifier, 512, false);
        if (asOf < 0 || limit < 1 || limit > MAX_CELLS) throw new IllegalArgumentException("SCAN_BOUND");
        NavigableMap<String, List<TextInventoryCell>> namespaceRows = rows.get(namespace);
        if (namespaceRows == null) return List.of();
        List<TextInventoryCell> result = new ArrayList<>();
        for (var entry : namespaceRows.tailMap(prefix, true).entrySet()) {
            if (!entry.getKey().startsWith(prefix)) break;
            for (TextInventoryCell cell : entry.getValue()) {
                CellKey key = cell.key();
                if (key.family().equals(family) && key.qualifier().equals(qualifier) && key.version() <= asOf) {
                    result.add(cell);
                    break;
                }
            }
            if (result.size() == limit) break;
        }
        return List.copyOf(result);
    }
}
