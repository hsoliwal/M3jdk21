// Copyright 2026 Hitesh Soliwal and contributors.
// Modified 2026-10-17: target package, progress SPI and JNI symbol adaptation.
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;
import com.m3.collections.M3ProgressMonitor;
import java.util.*;
import java.util.function.*;
/** Primitive intermediate stages. scan is ordered and sequential, never implicitly associative. */
public final class M3LongFlow {
    interface Sink {
        boolean accept(long value);
    }
    interface Plan {
        void run(Sink sink,M3ProgressMonitor m);
    }
    private final Plan plan;
    private final int maximum;
    M3LongFlow(Plan plan,int maximum) {
        this.plan=plan;
        this.maximum=maximum;
    }
    public M3LongFlow mapLong(LongUnaryOperator op) {
        Objects.requireNonNull(op);
        return new M3LongFlow((s,m)->plan.run(v-> {
            long x=op.applyAsLong(v);
            M3Flow.check(m);
            return s.accept(x);
        }
        ,m),maximum);
    }
    public M3LongFlow filterLong(LongPredicate p) {
        Objects.requireNonNull(p);
        return new M3LongFlow((s,m)->plan.run(v-> {
            boolean keep=p.test(v);
            M3Flow.check(m);
            return !keep||s.accept(v);
        }
        ,m),maximum);
    }
    public M3LongFlow scan(long identity,LongBinaryOperator op) {
        Objects.requireNonNull(op);
        return new M3LongFlow((s,m)-> {
            long[] a= {
                identity
            }
            ;
            plan.run(v-> {
                a[0]=op.applyAsLong(a[0],v);
                M3Flow.check(m);
                return s.accept(a[0]);
            }
            ,m);
        }
        ,maximum);
    }
/** Emits n-1 adjacent-pair results; no pair objects. */
    public M3LongFlow pairwise(LongBinaryOperator op) {
        Objects.requireNonNull(op);
        return new M3LongFlow((s,m)-> {
            long[] previous= {
                0
            }
            ;
            boolean[] first= {
                true
            }
            ;
            plan.run(v-> {
                if(first[0]) {
                    first[0]=false;
                    previous[0]=v;
                    return true;
                }
                long result=op.applyAsLong(previous[0],v);
                previous[0]=v;
                M3Flow.check(m);
                return s.accept(result);
            }
            ,m);
        }
        ,maximum);
    }
    public M3LongFlow limit(long count) {
        if(count<0)throw new IllegalArgumentException("negative limit");
        return new M3LongFlow((s,m)-> {
            M3Flow.check(m);
            if(count==0)return;
            long[] remaining= {
                count
            }
            ;
            plan.run(v-> {
                boolean more=s.accept(v);
                remaining[0]--;
                return more&&remaining[0]>0;
            }
            ,m);
        }
        ,maximum);
    }
/** The right array is borrowed and must be stable during a terminal. Stops at the shorter input. */
    public M3LongFlow zipShortest(long[] right,LongBinaryOperator op) {
        Objects.requireNonNull(right);
        Objects.requireNonNull(op);
        return new M3LongFlow((sink,m)-> {
            M3Flow.check(m);
            if(right.length==0)return;
            int[] i= {
                0
            }
            ;
            plan.run(v-> {
                long result=op.applyAsLong(v,right[i[0]++]);
                M3Flow.check(m);
                return sink.accept(result)&&i[0]<right.length;
            }
            ,m);
        }
        ,Math.min(maximum,right.length));
    }
    public M3LongFlow takeWhile(LongPredicate p) {
        Objects.requireNonNull(p);
        return new M3LongFlow((s,m)->plan.run(v-> {
            boolean keep=p.test(v);
            M3Flow.check(m);
            return keep&&s.accept(v);
        }
        ,m),maximum);
    }
    public void forEach(LongConsumer action) {
        forEach(action,null);
    }
    public void forEach(LongConsumer action,M3ProgressMonitor m) {
        Objects.requireNonNull(action);
        M3Flow.check(m);
        plan.run(v-> {
            M3Flow.check(m);
            action.accept(v);
            M3Flow.check(m);
            return true;
        }
        ,m);
        M3Flow.check(m);
    }
    public long sum() {
        return sum(null);
    }
    public long sum(M3ProgressMonitor m) {
        long[] value= {
            0
        }
        ;
        forEach(v->value[0]+=v,m);
        return value[0];
    }
    public long count() {
        return count(null);
    }
    public long count(M3ProgressMonitor m) {
        long[] n= {
            0
        }
        ;
        forEach(v->n[0]++,m);
        return n[0];
    }
    public long[] toArray() {
        return toArray(null);
    }
    public long[] toArray(M3ProgressMonitor m) {
        LongBuffer b=new LongBuffer(maximum);
        forEach(b::add,m);
        return Arrays.copyOf(b.values,b.size);
    }
    private static final class LongBuffer {
        long[] values=new long[0];
        int size;
        final int max;
        LongBuffer(int max) {
            this.max=max;
        }
        void add(long v) {
            if(size==max)throw new IllegalArgumentException("output budget");
            if(size==values.length)values=Arrays.copyOf(values,(int)Math.min(max,Math.max(8L,2L*size)));
            values[size++]=v;
        }
    }

    /** Borrows the array; values must remain stable during a terminal operation. */
    public static M3LongFlow of(long[] values) {
        Objects.requireNonNull(values, "values");
        return new M3LongFlow((sink, monitor) -> {
            M3Flow.check(monitor);
            for (long value : values) {
                M3Flow.check(monitor);
                boolean more = sink.accept(value);
                M3Flow.check(monitor);
                M3Flow.worked(monitor);
                if (!more) break;
            }
            M3Flow.check(monitor);
        }, values.length);
    }

    /**
     * Intermediate ordered left fold: emits one value after upstream completion.
     * Empty input emits identity. No associativity or parallel safety is inferred.
     */
    public M3LongFlow accumulate(long identity, LongBinaryOperator operation) {
        Objects.requireNonNull(operation, "operation");
        return new M3LongFlow((sink, monitor) -> {
            M3Flow.check(monitor);
            long[] state = {identity};
            plan.run(value -> {
                M3Flow.check(monitor);
                state[0] = operation.applyAsLong(state[0], value);
                M3Flow.check(monitor);
                return true;
            }, monitor);
            M3Flow.check(monitor);
            sink.accept(state[0]);
            M3Flow.check(monitor);
        }, 1);
    }

    /**
     * Intermediate terminal-scoped accumulation. Factory, update and finisher do
     * not run during pipeline assembly. State is released before emitting the
     * finished primitive. Each evaluation requires fresh caller-supplied state.
     */
    public <A> M3LongFlow accumulate(Supplier<? extends A> factory,
            ObjLongConsumer<? super A> update, ToLongFunction<? super A> finish,
            Consumer<? super A> release) {
        Objects.requireNonNull(factory, "factory");
        Objects.requireNonNull(update, "update");
        Objects.requireNonNull(finish, "finish");
        Objects.requireNonNull(release, "release");
        return new M3LongFlow((sink, monitor) -> {
            M3Flow.check(monitor);
            A state = Objects.requireNonNull(factory.get(), "accumulator state");
            long result;
            try (var lease = new AccumulatorLease<>(state, release)) {
                try {
                    M3Flow.check(monitor);
                    plan.run(value -> {
                        M3Flow.check(monitor);
                        update.accept(lease.state(), value);
                        M3Flow.check(monitor);
                        return true;
                    }, monitor);
                    M3Flow.check(monitor);
                    result = finish.applyAsLong(lease.state());
                    M3Flow.check(monitor);
                } catch (Throwable primary) {
                    lease.failed(primary);
                    throw primary;
                }
            }
            M3Flow.check(monitor);
            sink.accept(result);
            M3Flow.check(monitor);
        }, 1);
    }

    /** Intermediate scoped accumulation for ordinary non-resource state. */
    public <A> M3LongFlow accumulate(Supplier<? extends A> factory,
            ObjLongConsumer<? super A> update, ToLongFunction<? super A> finish) {
        return accumulate(factory, update, finish, state -> { });
    }

    private static final class AccumulatorLease<A> implements AutoCloseable {
        private final A state;
        private final Consumer<? super A> release;
        private Throwable primary;

        AccumulatorLease(A state, Consumer<? super A> release) {
            this.state = state;
            this.release = release;
        }
        A state() { return state; }
        void failed(Throwable failure) { primary = failure; }

        @Override public void close() {
            try {
                release.accept(state);
            } catch (Throwable failure) {
                // Self-suppression is illegal. Preserve the first failure's identity;
                // try-with-resources suppresses a distinct cleanup failure normally.
                if (failure != primary) throw failure;
            }
        }
    }

    /** Primitive rolling sums; no materialized window or member list. */
    public M3LongFlow windowSum(int width, int step, M3Windows.Tail tail) {
        return M3LongWindows.create(plan, maximum, width, step, tail, M3LongWindows.Kind.SUM);
    }

    /** Signed rolling minima using a monotone primitive deque. */
    public M3LongFlow windowMin(int width, int step, M3Windows.Tail tail) {
        return M3LongWindows.create(plan, maximum, width, step, tail, M3LongWindows.Kind.MIN);
    }

    /** Signed rolling maxima using a monotone primitive deque. */
    public M3LongFlow windowMax(int width, int step, M3Windows.Tail tail) {
        return M3LongWindows.create(plan, maximum, width, step, tail, M3LongWindows.Kind.MAX);
    }

    public M3LongFlow skip(long count) {
        if (count < 0) throw new IllegalArgumentException("negative skip");
        return new M3LongFlow((sink, monitor) -> {
            long[] remaining = {count};
            plan.run(value -> {
                if (remaining[0] > 0) { remaining[0]--; return true; }
                return sink.accept(value);
            }, monitor);
        }, maximum);
    }

    public M3LongFlow dropWhile(LongPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        return new M3LongFlow((sink, monitor) -> {
            boolean[] dropping = {true};
            plan.run(value -> {
                if (dropping[0]) {
                    dropping[0] = predicate.test(value);
                    M3Flow.check(monitor);
                    if (dropping[0]) return true;
                }
                return sink.accept(value);
            }, monitor);
        }, maximum);
    }

    public long fold(long identity, LongBinaryOperator operation) {
        return fold(identity, operation, null);
    }

    /** Terminal ordered left fold, including nonassociative operators. */
    public long fold(long identity, LongBinaryOperator operation, M3ProgressMonitor monitor) {
        return accumulate(identity, operation).sum(monitor);
    }

    public boolean anyMatch(LongPredicate predicate) { return anyMatch(predicate, null); }

    public boolean anyMatch(LongPredicate predicate, M3ProgressMonitor monitor) {
        Objects.requireNonNull(predicate, "predicate");
        M3Flow.check(monitor);
        boolean[] found = {false};
        plan.run(value -> {
            M3Flow.check(monitor);
            found[0] = predicate.test(value);
            M3Flow.check(monitor);
            return !found[0];
        }, monitor);
        M3Flow.check(monitor);
        return found[0];
    }

    public boolean allMatch(LongPredicate predicate) { return allMatch(predicate, null); }

    public boolean allMatch(LongPredicate predicate, M3ProgressMonitor monitor) {
        Objects.requireNonNull(predicate, "predicate");
        return !anyMatch(predicate.negate(), monitor);
    }

    public boolean noneMatch(LongPredicate predicate) { return noneMatch(predicate, null); }

    public boolean noneMatch(LongPredicate predicate, M3ProgressMonitor monitor) {
        return !anyMatch(predicate, monitor);
    }

    public OptionalLong findFirst() { return findFirst(null); }

    public OptionalLong findFirst(M3ProgressMonitor monitor) {
        M3Flow.check(monitor);
        long[] value = {0};
        boolean[] found = {false};
        plan.run(v -> { value[0] = v; found[0] = true; return false; }, monitor);
        M3Flow.check(monitor);
        return found[0] ? OptionalLong.of(value[0]) : OptionalLong.empty();
    }

    public M3LongSummary summary() { return summary(null); }

    /** One summary result, not one summary object per input. Empty min/max use JDK sentinels. */
    public M3LongSummary summary(M3ProgressMonitor monitor) {
        long[] state = {0, 0, Long.MAX_VALUE, Long.MIN_VALUE};
        forEach(value -> {
            state[0]++;
            state[1] += value;
            state[2] = Math.min(state[2], value);
            state[3] = Math.max(state[3], value);
        }, monitor);
        return new M3LongSummary(state[0], state[1], state[2], state[3]);
    }
}
