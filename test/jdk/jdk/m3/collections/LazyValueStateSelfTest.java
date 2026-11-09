// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type adaptation.
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicReference;
import jdk.internal.m3.collections.bits.LazyEventBits;
import jdk.internal.m3.collections.SystemProgress;

/** Real provider/state owner checks, executable without Maven or test dependencies. */
public final class LazyValueStateSelfTest {
    private LazyValueStateSelfTest() { }

    public static void main(String[] args) {
        int[] calls = {0}; Object identity = new Object(); Object[] values = new Object[4];
        var state = new LazyValueState<Integer, Object>((key, progress) -> {
            require(progress != null, "non-null monitor"); calls[0]++;
            return key == 0 ? null : identity;
        });
        require(state.capacity() == 0, "empty state allocation"); state.grow(4);
        same(null, state.read(0, 0, values, Progress.NULL));
        same(null, state.read(0, 0, values, Progress.NULL));
        require(state.flags(0) == LazyValueProvider.LOADED && calls[0] == 1, "null memoized");
        same(identity, state.read(1, 1, values, Progress.NULL));
        same(identity, state.read(1, 1, values, Progress.NULL));
        require(calls[0] == 2, "warm read avoids callback");
        state.grow(8); values = java.util.Arrays.copyOf(values, 8);
        require(state.flags(1) == LazyValueProvider.LOADED, "growth preserves flags");
        state.verifySlot(0, true, values); state.verifySlot(1, true, values);
        state.erased(1); values[1] = null; state.verifySlot(1, false, values);
        state.admitted(1, false); same(identity, state.read(1, 1, values, Progress.NULL));
        require(calls[0] == 3, "reused slot reloads");
        state.cleared(); java.util.Arrays.fill(values, null);
        require(state.flags(0) == LazyValueProvider.UNLOADED, "clear state");

        RuntimeException original = new IllegalArgumentException("provider failure"); int[] attempts = {0};
        var retry = new LazyValueState<Integer, Object>((key, progress) -> {
            if (++attempts[0] == 1) { throw original; } return identity;
        });
        retry.grow(1); Object[] result = new Object[1];
        same(original, failure(RuntimeException.class, () -> retry.read(0, 0, result, Progress.NULL)));
        require(retry.flags(0) == LazyValueProvider.FAILED && result[0] == null, "failure not published");
        retry.writable(); same(identity, retry.read(0, 0, result, Progress.NULL));
        require(attempts[0] == 2, "failed read can retry");

        AtomicReference<LazyValueState<Integer, Object>> recursive = new AtomicReference<>();
        var cycle = new LazyValueState<Integer, Object>((key, progress) ->
                recursive.get().read(0, key, result, progress));
        recursive.set(cycle); cycle.grow(1); result[0] = null;
        failure(IllegalStateException.class, () -> cycle.read(0, 0, result, Progress.NULL));
        require(cycle.flags(0) == LazyValueProvider.FAILED, "recursive state reset"); cycle.writable();

        AtomicReference<LazyValueState<Integer, Object>> mutating = new AtomicReference<>();
        var guard = new LazyValueState<Integer, Object>((key, progress) -> {
            mutating.get().writable(); return identity;
        });
        mutating.set(guard); guard.grow(1);
        failure(IllegalStateException.class, () -> guard.read(0, 0, result, Progress.NULL));
        guard.writable(); require(result[0] == null, "mutation did not publish");

        Progress canceled = Progress.of(1); canceled.cancel();
        failure(CancellationException.class, () -> guard.read(0, 0, result, canceled));
        var lateCancel = new LazyValueState<Integer, Object>((key, progress) -> {
            progress.cancel(); return identity;
        });
        lateCancel.grow(1); Progress monitored = Progress.of(1);
        failure(CancellationException.class, () -> lateCancel.read(0, 0, result, monitored));
        require(result[0] == null && lateCancel.flags(0) == LazyValueProvider.FAILED,
                "canceled result not published"); lateCancel.writable();
        same(identity, lateCancel.read(0, 0, result, Progress.NULL));

        Progress fallback = LazyValueState.progress(null);
        require(fallback != Progress.NULL, "null monitor uses shared system progress");

        List<Long> events = new ArrayList<>();
        int[] observedCalls = {0};
        LazyValueProvider<Integer, Object> observedProvider = new LazyValueProvider<>() {
            @Override public Object load(Integer key, Progress progress) {
                observedCalls[0]++;
                return identity;
            }
            @Override public LazyEventPolicy lazyEventPolicy() {
                return LazyEventPolicy.of(
                        LazyEventBits.BEFORE_TOUCH | LazyEventBits.CACHE_MISS
                                | LazyEventBits.STATE_CHANGED | LazyEventBits.AFTER_LOAD
                                | LazyEventBits.CANCELLED,
                        LazyEventGranularity.STATE_TRANSITION);
            }
            @Override public LazyEventSink<Progress> lazyEventSink() {
                return (eventBit, slot, oldState, newState, operation, progress, failure) ->
                        events.add(eventBit);
            }
        };
        var observed = new LazyValueState<Integer, Object>(observedProvider);
        observed.grow(1); result[0] = null;
        long systemBefore = SystemProgress.get().state().worked();
        same(identity, observed.read(0, 0, result, null));
        require(SystemProgress.get().state().worked() >= systemBefore + 1L,
                "null monitor credits shared system root");
        require(events.contains(LazyEventBits.BEFORE_TOUCH)
                        && events.contains(LazyEventBits.CACHE_MISS)
                        && events.contains(LazyEventBits.STATE_CHANGED)
                        && events.contains(LazyEventBits.AFTER_LOAD),
                "configured state events");

        observed.admitted(0, false); result[0] = null; events.clear();
        Progress preCanceled = Progress.of(1); preCanceled.cancel();
        int beforeCanceledCalls = observedCalls[0];
        failure(CancellationException.class,
                () -> observed.read(0, 0, result, preCanceled));
        require(observedCalls[0] == beforeCanceledCalls, "pre-cancel avoids provider");
        require(events.contains(LazyEventBits.CANCELLED), "pre-cancel event emitted");

        System.out.println(
                "PACKED_LAZY_STATE_OK null/reuse/growth/failure/recursion/mutation/cancellation/events/system-progress");
    }

    private static void require(boolean valid, String message) {
        if (!valid) { throw new AssertionError(message); }
    }
    private static void same(Object expected, Object actual) {
        if (expected != actual) { throw new AssertionError("identity: " + Objects.toString(actual)); }
    }
    private static <T extends Throwable> T failure(Class<T> type, Runnable action) {
        try { action.run(); }
        catch (Throwable caught) {
            if (type.isInstance(caught)) { return type.cast(caught); }
            throw new AssertionError("unexpected failure", caught);
        }
        throw new AssertionError("expected " + type.getSimpleName());
    }
}
