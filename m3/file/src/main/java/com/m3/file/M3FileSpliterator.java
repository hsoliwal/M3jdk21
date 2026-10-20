// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/** Facade view of an id spliterator: ids become {@link M3File} only at emission. */
final class M3FileSpliterator implements Spliterator<M3File> {
    private final M3FileTree tree;
    private final Spliterator.OfInt ids;
    private final IntConsumer emit = this::emit;
    private Consumer<? super M3File> sink;

    M3FileSpliterator(M3FileTree tree, Spliterator.OfInt ids) {
        this.tree = tree;
        this.ids = ids;
    }

    private void emit(int id) {
        sink.accept(tree.file(id));
    }

    @Override
    public boolean tryAdvance(Consumer<? super M3File> action) {
        sink = action;
        return ids.tryAdvance(emit);
    }

    @Override
    public void forEachRemaining(Consumer<? super M3File> action) {
        sink = action;
        ids.forEachRemaining(emit);
    }

    @Override
    public Spliterator<M3File> trySplit() {
        Spliterator.OfInt prefix = ids.trySplit();
        return prefix == null ? null : new M3FileSpliterator(tree, prefix);
    }

    @Override
    public long estimateSize() {
        return ids.estimateSize();
    }

    @Override
    public int characteristics() {
        return ids.characteristics();
    }
}
