// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.ast;

import com.synexia.mindex.experiment.MIndexString;
import java.util.List;
import java.util.Objects;

/** Immutable pool image; rows are shared safely and later pool appends cannot change this snapshot. */
public final class MIndexASTSnapshot extends MIndexASTStore {
    final List<MIndexASTNode> rows;
    final List<MIndexString> symbols;

    MIndexASTSnapshot(List<MIndexASTNode> rows, List<MIndexString> symbols) {
        this.rows = List.copyOf(rows);
        this.symbols = List.copyOf(symbols);
    }

    @Override public int size() { return rows.size(); }
    @Override MIndexASTNode node(int id) { return rows.get(id); }
    @Override MIndexString symbol(int id) { return symbols.get(id); }
    public MIndexAST ref(int id) { return new MIndexAST(this, Objects.checkIndex(id, rows.size())); }
    /** Explicit new domain preserving the base node coordinates. */
    public MIndexASTPool fork(MIndexASTLimits limits) { return new MIndexASTPool(this, limits); }
}
