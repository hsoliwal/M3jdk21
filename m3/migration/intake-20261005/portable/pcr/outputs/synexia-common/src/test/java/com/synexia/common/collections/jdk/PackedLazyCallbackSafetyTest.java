// SPDX-License-Identifier: Apache-2.0
package com.synexia.common.collections.jdk;

import static com.synexia.common.collections.jdk.LazyValueProvider.FAILED;
import static com.synexia.common.collections.jdk.LazyValueProvider.LOADED;
import static com.synexia.common.collections.jdk.LazyValueProvider.LOADING;
import static com.synexia.common.collections.jdk.LazyValueProvider.UNLOADED;

import com.synexia.common.lazy.event.LazyEventBits;
import com.synexia.common.lazy.event.LazyEventGranularity;
import com.synexia.common.lazy.event.LazyEventPolicy;
import com.synexia.common.lazy.event.LazyEventSink;
import com.synexia.common.lazy.event.LazyOperation;
import com.synexia.common.progress.SystemProgress;
import com.synexia.common.utils.Progress;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

/** Independent callback adversaries against the public packed map and its shared views. */
final class PackedLazyCallbackSafetyTest {
    private static final Runnable NOTHING = () -> { };
    private static final LazyEventPolicy ALL = LazyEventPolicy.of(
            LazyEventBits.ALL, LazyEventGranularity.STATE_TRANSITION);

    private record Phase(
            String name, long bit, short transition, boolean warm, short expected, int loads) {
        boolean matches(long eventBit, short newState) {
            return bit == eventBit
                    && (bit != LazyEventBits.STATE_CHANGED || transition == newState);
        }
    }

    private static final List<Phase> PHASES = List.of(
            new Phase("cold before touch", LazyEventBits.BEFORE_TOUCH, UNLOADED, false, UNLOADED, 0),
            new Phase("cache miss", LazyEventBits.CACHE_MISS, LOADING, false, FAILED, 0),
            new Phase("loading transition", LazyEventBits.STATE_CHANGED, LOADING, false, FAILED, 0),
            new Phase("before load", LazyEventBits.BEFORE_LOAD, LOADING, false, FAILED, 0),
            new Phase("provider begin", LazyEventBits.PROVIDER_BEGIN, LOADING, false, FAILED, 0),
            new Phase("loaded transition", LazyEventBits.STATE_CHANGED, LOADED, false, FAILED, 1),
            new Phase("after load", LazyEventBits.AFTER_LOAD, LOADED, false, FAILED, 1),
            new Phase("cold after touch", LazyEventBits.AFTER_TOUCH, LOADED, false, FAILED, 1),
            new Phase("provider end", LazyEventBits.PROVIDER_END, LOADED, false, LOADED, 1),
            new Phase("warm before touch", LazyEventBits.BEFORE_TOUCH, LOADED, true, LOADED, 0),
            new Phase("cache hit", LazyEventBits.CACHE_HIT, LOADED, true, LOADED, 0),
            new Phase("warm after touch", LazyEventBits.AFTER_TOUCH, LOADED, true, LOADED, 0));

    @Test void policyAndSinkGettersGuardEverySharedMutation() {
        List<Consumer<PackedTreeMap<Integer, Object>>> mutations = List.of(
                map -> map.clear(),
                map -> map.put(3, 30),
                map -> map.put(2, 200),
                map -> map.defer(3),
                map -> map.remove(2),
                map -> map.descendingMap().clear(),
                map -> map.subMap(1, true, 2, true).clear(),
                map -> map.navigableKeySet().pollLast(),
                map -> map.entrySet().iterator().next().setValue(100));
        for (boolean policyGetter : new boolean[] {true, false}) {
            for (boolean range : new boolean[] {false, true}) {
                for (Consumer<PackedTreeMap<Integer, Object>> mutation : mutations) {
                    Model model = new Model();
                    model.map.defer(1);
                    model.map.put(2, 20);
                    Runnable callback = () -> mutation.accept(model.map);
                    if (policyGetter) { model.policyGetter = callback; }
                    else { model.sinkGetter = callback; }
                    failure(IllegalStateException.class, () -> {
                        if (range) { model.map.descendingMap().preload(Progress.NULL); }
                        else { model.map.get(1); }
                    });
                    equal(List.of(1, 2), new ArrayList<>(model.map.keySet()), "shared keys");
                    state(model, 1, UNLOADED, null);
                    state(model, 2, LOADED, 20);
                    equal(0, model.loads, "getter must precede provider");
                    clearAndReuse(model);
                }
            }
        }
    }

    @Test void eventFailuresRetainTheStateOfTheirOwnPhase() {
        for (Phase phase : PHASES) {
            for (int kind = 0; kind < 4; kind++) {
                Model model = new Model();
                if (phase.warm()) { model.map.put(1, 10); }
                else { model.map.defer(1); }
                model.map.put(2, 20);
                Throwable signal = switch (kind) {
                    case 0 -> new IllegalArgumentException(phase.name());
                    case 1 -> new AssertionError(phase.name());
                    case 2 -> new CancellationException(phase.name());
                    default -> null;
                };
                model.events = (bit, slot, oldState, newState, operation, progress, problem) -> {
                    if (phase.matches(bit, newState)) {
                        if (signal == null) { model.map.descendingMap().clear(); }
                        else { raise(signal); }
                    }
                };
                Class<? extends Throwable> expectedType =
                        signal == null ? IllegalStateException.class : signal.getClass();
                Throwable observed = failure(expectedType,
                        () -> {
                            if (phase.bit() == LazyEventBits.PROVIDER_END) { model.map.remove(1); }
                            else { model.map.get(1); }
                        });
                if (signal != null) { same(signal, observed, phase.name()); }
                state(model, 1, phase.expected(), phase.expected() == LOADED ? 10 : null);
                state(model, 2, LOADED, 20);
                equal(2, model.map.size(), "failed read/removal retains mappings");
                equal(phase.loads(), model.loads, phase.name() + " provider count");
                clearAndReuse(model);
            }
        }
    }

    @Test void earlyCallbacksRetainUnloadedFailedAndLoadedStates() {
        for (short prior : new short[] {UNLOADED, FAILED, LOADED}) {
            for (int phase = 0; phase < 3; phase++) {
                Model model = new Model();
                model.map.defer(1);
                if (prior == LOADED) { model.map.clear(); model.map.put(1, 10); }
                if (prior == FAILED) {
                    model.loader = (key, progress) -> { throw new IllegalArgumentException("seed"); };
                    failure(IllegalArgumentException.class, () -> model.map.get(1));
                }
                int before = model.loads;
                AssertionError signal = new AssertionError("early callback failure");
                if (phase == 0) { model.policyGetter = () -> { throw signal; }; }
                if (phase == 1) { model.sinkGetter = () -> { throw signal; }; }
                if (phase == 2) {
                    model.events = (bit, slot, oldState, newState, operation, progress, problem) -> {
                        if (bit == LazyEventBits.BEFORE_TOUCH) { throw signal; }
                    };
                }
                same(signal, failure(AssertionError.class, () -> model.map.get(1)), "early identity");
                state(model, 1, prior, prior == LOADED ? 10 : null);
                equal(before, model.loads, "early callback failure does not start a load");
                clearAndReuse(model);
            }
        }
    }

    @Test void caughtMutationsLeaveTheSelectedSlotStableForOuterRemoval() {
        Model model = new Model();
        model.map.defer(1); model.map.put(2, 20);
        int[] attempts = {0};
        model.events = (bit, slot, oldState, newState, operation, progress, problem) -> {
            if (bit == LazyEventBits.BEFORE_TOUCH || bit == LazyEventBits.PROVIDER_END) {
                failure(IllegalStateException.class, () -> model.map.descendingMap().clear());
                attempts[0]++;
                equal(20, model.map.get(2), "cached read after rejected mutation");
            }
        };
        equal(10, model.map.remove(1), "outer removal returns its own loaded value");
        equal(2, attempts[0], "early and final observer both reject mutation");
        equal(List.of(2), new ArrayList<>(model.map.keySet()), "only selected key removed");
        state(model, 2, LOADED, 20);
        clearAndReuse(model);
    }

    @Test void aSinkGetterFailureAfterPublicationClearsTheFailedPayload() {
        Model model = new Model();
        model.map.defer(1);
        int[] sinks = {0};
        AssertionError signal = new AssertionError("sink acquisition after publication");
        model.sinkGetter = () -> {
            if (++sinks[0] == 6) { throw signal; }
        };
        same(signal, failure(AssertionError.class, () -> model.map.get(1)), "sink identity");
        state(model, 1, FAILED, null);
        equal(1, model.loads, "payload was supplied before sink acquisition failed");
        clearAndReuse(model);
    }

    @Test void cleanupPreservesRuntimeErrorAndCancellationIdentity() {
        for (Throwable primary : List.of(
                new IllegalArgumentException("provider runtime"),
                new AssertionError("provider error"),
                new CancellationException("provider cancellation"))) {
            Model model = new Model();
            model.map.defer(1);
            model.loader = (key, progress) -> { raise(primary); return null; };
            AssertionError transitionFailure = new AssertionError("failed transition observer");
            IllegalStateException failureObserver = new IllegalStateException("failure observer");
            AssertionError endFailure = new AssertionError("provider end observer");
            model.events = (bit, slot, oldState, newState, operation, progress, problem) -> {
                if (problem != null) { same(primary, problem, "notification primary"); }
                if (bit == LazyEventBits.STATE_CHANGED && newState == FAILED) {
                    throw transitionFailure;
                }
                if (bit == LazyEventBits.LOAD_FAILED || bit == LazyEventBits.CANCELLED) {
                    throw failureObserver;
                }
                if (bit == LazyEventBits.PROVIDER_END) { throw endFailure; }
            };
            same(primary, failure(primary.getClass(), () -> model.map.get(1)), "provider identity");
            equal(List.of(transitionFailure, failureObserver, endFailure),
                    List.of(primary.getSuppressed()), "ordered secondary notifications");
            state(model, 1, FAILED, null);
            model.quiet();
            model.loader = (key, progress) -> null;
            equal(null, model.map.get(1), "successful null retry");
            state(model, 1, LOADED, null);
            equal(null, model.map.get(1), "memoized null");
            equal(2, model.loads, "one failure and one successful retry");
            clearAndReuse(model);
        }
        Model self = new Model();
        self.map.defer(1);
        AssertionError sameSignal = new AssertionError("same instance in cleanup");
        self.loader = (key, progress) -> { throw sameSignal; };
        self.events = (bit, slot, oldState, newState, operation, progress, problem) -> {
            if (problem != null) { throw sameSignal; }
        };
        same(sameSignal, failure(AssertionError.class, () -> self.map.get(1)), "no self suppression");
        equal(0, sameSignal.getSuppressed().length, "primary cannot suppress itself");
        state(self, 1, FAILED, null);
        clearAndReuse(self);
    }

    @Test void cachedReadsInsideAllCallbacksDoNotReenterObservers() {
        Model model = new Model();
        model.map.defer(1);
        model.map.put(2, 20);
        model.map.put(3, null);
        List<Long> events = new ArrayList<>();
        int[] policyCalls = {0};
        int[] sinkCalls = {0};
        boolean[] active = new boolean[3];
        Runnable cachedReads = () -> {
            equal(20, model.map.descendingMap().get(2), "nested cached payload");
            equal(null, model.map.get(3), "nested cached null");
            if (model.map.isValueLoaded(1)) { equal(10, model.map.get(1), "current published slot"); }
        };
        model.policyGetter = () -> {
            check(!active[0], "recursive policy getter"); active[0] = true;
            try { policyCalls[0]++; cachedReads.run(); } finally { active[0] = false; }
        };
        model.sinkGetter = () -> {
            check(!active[1], "recursive sink getter"); active[1] = true;
            try { sinkCalls[0]++; cachedReads.run(); } finally { active[1] = false; }
        };
        model.events = (bit, slot, oldState, newState, operation, progress, problem) -> {
            check(!active[2], "recursive observer"); active[2] = true;
            try {
                same(LazyOperation.READ, operation, "external read operation");
                check(progress != null, "non-null callback monitor");
                events.add(bit);
                cachedReads.run();
            } finally { active[2] = false; }
        };
        model.loader = (key, progress) -> { cachedReads.run(); return 10; };
        equal(10, model.map.get(1), "cold outer read");
        equal(List.of(LazyEventBits.BEFORE_TOUCH, LazyEventBits.CACHE_MISS,
                LazyEventBits.STATE_CHANGED, LazyEventBits.BEFORE_LOAD,
                LazyEventBits.PROVIDER_BEGIN, LazyEventBits.STATE_CHANGED,
                LazyEventBits.AFTER_LOAD, LazyEventBits.AFTER_TOUCH, LazyEventBits.PROVIDER_END),
                events, "external cold events");
        events.clear();
        equal(10, model.map.get(1), "warm outer read");
        equal(List.of(LazyEventBits.BEFORE_TOUCH, LazyEventBits.CACHE_HIT, LazyEventBits.AFTER_TOUCH),
                events, "external warm events");
        equal(2, policyCalls[0], "only external reads resolve policy");
        equal(12, sinkCalls[0], "only external events acquire the sink");
        equal(1, model.loads, "cached reads do not invoke the provider");
        state(model, 1, LOADED, 10);
        state(model, 2, LOADED, 20);
        state(model, 3, LOADED, null);
        clearAndReuse(model);
    }

    @Test void differentSlotLoadsRemainNestedAndUnresolvedCyclesFail() {
        Model nested = new Model();
        for (int key = 0; key < 32; key++) { nested.map.defer(key); }
        nested.loader = (key, progress) -> key == 31
                ? 1 : (Integer) nested.map.descendingMap().get(key + 1) + 1;
        equal(32, nested.map.get(0), "different-slot nested load chain");
        equal(32, nested.loads, "each nested slot loads once");
        nested.map.verifyInvariants();
        clearAndReuse(nested);

        Model beforeTouch = new Model();
        beforeTouch.map.defer(1); beforeTouch.map.defer(2);
        beforeTouch.events = (bit, slot, oldState, newState, operation, progress, problem) -> {
            if (bit == LazyEventBits.BEFORE_TOUCH && slot == beforeTouch.map.findSlot(1)) {
                equal(20, beforeTouch.map.get(2), "different-slot read before loading starts");
            }
        };
        equal(10, beforeTouch.map.get(1), "early nested read");
        equal(2, beforeTouch.loads, "early nested provider count");
        beforeTouch.map.verifyInvariants();
        clearAndReuse(beforeTouch);

        for (int phase = 0; phase < 4; phase++) {
            Model cycle = new Model();
            cycle.map.defer(1);
            Runnable recursive = () -> cycle.map.get(1);
            if (phase == 0) { cycle.policyGetter = recursive; }
            if (phase == 1) { cycle.sinkGetter = recursive; }
            if (phase == 2) {
                cycle.events = (bit, slot, oldState, newState, operation, progress, problem) -> {
                    if (bit == LazyEventBits.BEFORE_TOUCH) { recursive.run(); }
                };
            }
            if (phase == 3) { cycle.loader = (key, progress) -> cycle.map.get(1); }
            failure(IllegalStateException.class, () -> cycle.map.get(1));
            state(cycle, 1, phase == 3 ? FAILED : UNLOADED, null);
            equal(phase == 3 ? 1 : 0, cycle.loads, "cyclic provider count");
            clearAndReuse(cycle);
        }
        Model cross = new Model();
        cross.map.defer(1); cross.map.defer(2);
        cross.loader = (key, progress) -> cross.map.get(key == 1 ? 2 : 1);
        failure(IllegalStateException.class, () -> cross.map.get(1));
        state(cross, 1, FAILED, null); state(cross, 2, FAILED, null);
        equal(2, cross.loads, "cross-slot cycle starts each provider once");
        clearAndReuse(cross);
    }

    @Test void cancellationBeforeAndAfterLoadingPreservesItsPrimarySignal() {
        for (boolean duringLoad : new boolean[] {false, true}) {
            Model model = new Model();
            model.map.defer(1);
            Progress monitor = Progress.of(1);
            AssertionError observerFailure = new AssertionError("cancel observer");
            Throwable[] notified = {null};
            model.events = (bit, slot, oldState, newState, operation, progress, problem) -> {
                same(monitor, progress, "borrowed cancellation monitor");
                if (!duringLoad && bit == LazyEventBits.BEFORE_TOUCH) { progress.cancel(); }
                if (bit == LazyEventBits.CANCELLED) {
                    notified[0] = problem;
                    throw observerFailure;
                }
            };
            if (duringLoad) { model.loader = (key, progress) -> { progress.cancel(); return 10; }; }
            CancellationException canceled = failure(
                    CancellationException.class, () -> model.map.touch(1, monitor));
            same(canceled, notified[0], "notified cancellation identity");
            equal(List.of(observerFailure), List.of(canceled.getSuppressed()), "cancel cleanup");
            state(model, 1, duringLoad ? FAILED : UNLOADED, null);
            equal(duringLoad ? 1 : 0, model.loads, "canceled provider count");
            check(!monitor.isDone(), "borrowed canceled work is not completed");
            model.quiet(); model.loader = (key, progress) -> 10;
            equal(10, model.map.touch(1, Progress.of(1)), "retry after cancellation");
            state(model, 1, LOADED, 10);
            clearAndReuse(model);
        }
    }

    @Test void nullMonitorCancellationUsesSystemProgressAndExplicitNoOpCanRetry() {
        boolean previouslyCanceled = SystemProgress.get().isCanceled();
        SystemProgress.get().setCanceled(false);
        try {
            Model model = new Model();
            model.map.defer(1);
            Progress[] seen = {null};
            model.loader = (key, progress) -> {
                seen[0] = progress;
                progress.cancel();
                return 10;
            };
            failure(CancellationException.class, () -> model.map.touch(1, null));
            check(seen[0] != null && seen[0] != Progress.NULL, "null selects a system child");
            check(SystemProgress.get().isCanceled(), "child cancellation reaches the system root");
            state(model, 1, FAILED, null);
            equal(1, model.loads, "canceled result was supplied once");
            model.map.put(2, 20);
            equal(10, model.map.touch(1, Progress.NULL), "explicit no-op retry succeeds");
            same(Progress.NULL, seen[0], "provider receives the requested no-op monitor");
            check(SystemProgress.get().isCanceled(), "no-op retry does not clear system cancellation");
            equal(2, model.loads, "one canceled load and one explicit no-op retry");
            state(model, 1, LOADED, 10); state(model, 2, LOADED, 20);
            model.map.clear();
            model.map.verifyInvariants();
        } finally {
            SystemProgress.get().setCanceled(previouslyCanceled);
        }
    }

    @Test void rangeEventGuardsPreserveViewBoundsAndProviderFailure() {
        for (long phase : new long[] {
                LazyEventBits.RANGE_BEGIN, LazyEventBits.PROGRESS, LazyEventBits.RANGE_END}) {
            Model model = new Model();
            for (int key = 1; key <= 4; key++) { model.map.defer(key); }
            model.events = (bit, slot, oldState, newState, operation, progress, problem) -> {
                if (bit == phase) { model.map.tailMap(1, true).clear(); }
            };
            Progress monitor = Progress.of(2);
            failure(IllegalStateException.class,
                    () -> model.map.subMap(2, true, 3, true).descendingMap().preload(monitor));
            state(model, 1, UNLOADED, null); state(model, 4, UNLOADED, null);
            boolean last = phase == LazyEventBits.RANGE_END;
            boolean first = phase != LazyEventBits.RANGE_BEGIN;
            state(model, 2, last ? LOADED : UNLOADED, last ? 20 : null);
            state(model, 3, first ? LOADED : UNLOADED, first ? 30 : null);
            equal(last ? 2 : first ? 1 : 0, model.loads, "bounded descending loads");
            equal(last, monitor.isDone(), "range phase completion");
            clearAndReuse(model);
        }

        Model failed = new Model();
        failed.map.defer(1); failed.map.defer(2);
        AssertionError primary = new AssertionError("provider during range");
        IllegalStateException secondary = new IllegalStateException("range end observer");
        failed.loader = (key, progress) -> { throw primary; };
        failed.events = (bit, slot, oldState, newState, operation, progress, problem) -> {
            if (bit == LazyEventBits.RANGE_END) { same(primary, problem, "range primary"); throw secondary; }
        };
        same(primary, failure(AssertionError.class, () -> failed.map.preload(Progress.NULL)),
                "range must preserve provider failure");
        equal(List.of(secondary), List.of(primary.getSuppressed()), "range cleanup suppression");
        state(failed, 1, FAILED, null); state(failed, 2, UNLOADED, null);
        clearAndReuse(failed);
    }

    @Test void ordinaryPreloadProgressStillDetectsStructuralInterference() {
        Model model = new Model();
        model.map.defer(1); model.map.defer(2);
        Progress monitor = Progress.of(2, progress -> {
            if (progress.getWorkedSoFar() == 1) { model.map.clear(); }
        });
        failure(ConcurrentModificationException.class, () -> model.map.preload(monitor));
        equal(1, model.loads, "only the completed slot loaded before monitor interference");
        check(model.map.isEmpty(), "ordinary monitor callback may structurally interfere");
        check(!monitor.isDone(), "interrupted traversal must not report completion");
        model.map.verifyInvariants();
        clearAndReuse(model);
    }

    @Test void boundedDescendingReadsKeepNavigationAndMemoizedNulls() {
        Model model = new Model();
        for (int key = 1; key <= 7; key++) {
            if (key == 4) { model.map.put(key, null); }
            else { model.map.defer(key); }
        }
        List<Integer> order = new ArrayList<>();
        model.loader = (key, progress) -> { order.add(key); return key * 10; };
        PackedTreeMap<Integer, Object> view = model.map.subMap(2, true, 6, false).descendingMap();
        view.preload(Progress.of(4));
        equal(List.of(5, 3, 2), order, "range provider encounter order skips explicit null");
        equal(List.of(5, 4, 3, 2), new ArrayList<>(view.navigableKeySet()), "descending keys");
        equal(5, view.firstKey(), "first descending key");
        equal(2, view.lastKey(), "last descending key");
        equal(5, view.lowerKey(4), "descending lower");
        equal(3, view.higherKey(4), "descending higher");
        equal(null, view.get(4), "memoized explicit null");
        state(model, 1, UNLOADED, null); state(model, 6, UNLOADED, null);
        state(model, 7, UNLOADED, null); state(model, 4, LOADED, null);
        equal(3, model.loads, "navigation and null hit do not load more slots");
        model.quiet();
        equal(null, view.remove(4), "null removal result");
        check(!model.map.containsKey(4), "removal propagates to the parent view");
        model.map.verifyInvariants();
        clearAndReuse(model);
    }

    /** The same assertions can also run as an isolated fixture runtime probe. */
    public static void main(String[] args) {
        PackedLazyCallbackSafetyTest test = new PackedLazyCallbackSafetyTest();
        test.policyAndSinkGettersGuardEverySharedMutation();
        test.eventFailuresRetainTheStateOfTheirOwnPhase();
        test.earlyCallbacksRetainUnloadedFailedAndLoadedStates();
        test.caughtMutationsLeaveTheSelectedSlotStableForOuterRemoval();
        test.aSinkGetterFailureAfterPublicationClearsTheFailedPayload();
        test.cleanupPreservesRuntimeErrorAndCancellationIdentity();
        test.cachedReadsInsideAllCallbacksDoNotReenterObservers();
        test.differentSlotLoadsRemainNestedAndUnresolvedCyclesFail();
        test.cancellationBeforeAndAfterLoadingPreservesItsPrimarySignal();
        test.nullMonitorCancellationUsesSystemProgressAndExplicitNoOpCanRetry();
        test.rangeEventGuardsPreserveViewBoundsAndProviderFailure();
        test.ordinaryPreloadProgressStillDetectsStructuralInterference();
        test.boundedDescendingReadsKeepNavigationAndMemoizedNulls();
        System.out.println("Packed lazy callback safety: PASS");
    }

    private static final class Model implements LazyValueProvider<Integer, Object> {
        private final PackedTreeMap<Integer, Object> map = new PackedTreeMap<>(null, this);
        private Runnable policyGetter = NOTHING;
        private Runnable sinkGetter = NOTHING;
        private LazyEventSink<Progress> events = LazyEventSink.noop();
        private BiFunction<Integer, Progress, Object> loader = (key, progress) -> key * 10;
        private int loads;

        @Override public Object load(Integer key, Progress progress) {
            loads++;
            return loader.apply(key, progress);
        }
        @Override public LazyEventPolicy lazyEventPolicy() { policyGetter.run(); return ALL; }
        @Override public LazyEventSink<Progress> lazyEventSink() { sinkGetter.run(); return events; }
        private void quiet() { policyGetter = NOTHING; sinkGetter = NOTHING; events = LazyEventSink.noop(); }
    }

    private static void state(Model model, int key, short expected, Object payload) {
        equal(expected, model.map.valueFlags(key), "state for key " + key);
        Object tree = field(model.map, "tree");
        Object[] values = (Object[]) field(tree, "values");
        equal(payload, values[model.map.findSlot(key)], "raw payload for key " + key);
        model.map.verifyInvariants();
    }

    private static Object field(Object owner, String name) {
        try {
            Field field = owner.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(owner);
        } catch (ReflectiveOperationException problem) {
            throw new AssertionError("inspect incumbent payload owner", problem);
        }
    }

    private static void clearAndReuse(Model model) {
        model.quiet();
        model.loader = (key, progress) -> key * 10;
        model.map.clear();
        check(model.map.defer(9), "guard released for slot reuse");
        equal(90, model.map.get(9), "read depth released for slot reuse");
        state(model, 9, LOADED, 90);
        model.map.clear();
        model.map.verifyInvariants();
    }

    private static void raise(Throwable signal) {
        if (signal instanceof RuntimeException runtime) { throw runtime; }
        if (signal instanceof Error error) { throw error; }
        throw new AssertionError("unsupported test signal", signal);
    }

    private static <T extends Throwable> T failure(Class<T> type, Runnable action) {
        try { action.run(); }
        catch (Throwable problem) {
            if (type.isInstance(problem)) { return type.cast(problem); }
            throw new AssertionError("expected " + type.getName() + " but caught " + problem, problem);
        }
        throw new AssertionError("expected " + type.getName());
    }

    private static void same(Object expected, Object actual, String message) {
        check(expected == actual, message + ": identity changed");
    }

    private static void equal(Object expected, Object actual, String message) {
        check(Objects.equals(expected, actual), message + ": expected " + expected + ", got " + actual);
    }

    private static void check(boolean condition, String message) {
        if (!condition) { throw new AssertionError(message); }
    }
}
