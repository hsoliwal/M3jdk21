// Copyright 2026 Hitesh Soliwal and contributors.
// Modified 2026-10-17: target package, progress SPI and JNI symbol adaptation.
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;
import com.m3.collections.M3ProgressMonitor;
import java.util.*;
import java.util.stream.*;
/** Standard Collector interoperability; collect remains terminal. Explicit input indices select
 * the source owner, not the filters on the supplied flow. Preferred path is flow.virtualize().
 * Parallel combining preserves encounter order, but copies index metadata and may box stream inputs. */
public final class M3VirtualCollectors {
    private M3VirtualCollectors() {
    }
    public static final class IndexAccumulator<T> {
        private final M3Flow<T> flow;
        private final M3Flow.Snapshot snap;
        private final PrimitiveIndices indices;
        private boolean consumed;
        private IndexAccumulator(M3Flow<T> flow,M3ProgressMonitor m) {
            this.flow=flow;
            snap=flow.snapshot(m);
            indices=new PrimitiveIndices(flow.maxRows);
        }
        private void active() {
            if(consumed)throw new IllegalStateException("consumed accumulator");
            flow.source.guard(snap.version());
        }
        private void add(int index,M3ProgressMonitor m) {
            active();
            M3Flow.check(m);
            Objects.checkIndex(index,snap.size());
            indices.add(index);
            M3Flow.worked(m);
        }
        private IndexAccumulator<T> combine(IndexAccumulator<T> right) {
            active();
            right.active();
            if(flow.source!=right.flow.source||snap.version()!=right.snap.version()||snap.size()!=right.snap.size())throw new ConcurrentModificationException("incompatible owner snapshots");
            indices.append(right.indices);
            right.consumed=true;
            return this;
        }
        private M3IndexView<T> finish() {
            active();
            consumed=true;
            return new M3IndexView<>(flow.source,snap.version(),indices.array());
        }
    }
    public static <T> Collector<Integer,IndexAccumulator<T>,M3IndexView<T>> toView(M3Flow<T> flow) {
        return toView(flow,null);
    }
    public static <T> Collector<Integer,IndexAccumulator<T>,M3IndexView<T>> toView(M3Flow<T> flow,M3ProgressMonitor m) {
        Objects.requireNonNull(flow);
        return Collector.of(()->new IndexAccumulator<>(flow,m),(a,i)->a.add(Objects.requireNonNull(i),m),IndexAccumulator::combine,IndexAccumulator::finish);
    }
    public static <T> M3IndexView<T> collectIndices(M3Flow<T> flow,IntStream indices,M3ProgressMonitor m) {
        Objects.requireNonNull(flow);
        Objects.requireNonNull(indices);
        M3Flow.check(m);
        IndexAccumulator<T> result=indices.collect(()->new IndexAccumulator<>(flow,m),(a,i)->a.add(i,m),(a,b)->a.combine(b));
        M3Flow.check(m);
        return result.finish();
    }
}
