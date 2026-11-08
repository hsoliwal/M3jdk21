// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.ConcurrentModificationException;
import java.util.IdentityHashMap;
import java.util.List;

/** Actual-source qualification; reflection observes private capacity only in this fixture. */
//FLLDCIM: RELOCATE - donor blob 21f1abd993fe at 165ceb5a0202; recipe m3/collections/recipe/residency-paged-array-20261107/relocate.py v4
public final class PagedArrayBackingReuseTest {
    private static boolean requireReuse;
    private static int cases;

    private PagedArrayBackingReuseTest() { }

    public static void main(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("resources")) {
            resourceObservation("cold");
            resourceObservation("second");
            return;
        }
        if (args.length > 1 || args.length == 1 && !args[0].equals("behavior"))
            throw new IllegalArgumentException("optional argument: behavior or resources");
        requireReuse = args.length == 0;
        cases = 0;
        run("capacity-and-payload-alias", PagedArrayBackingReuseTest::capacityAndAlias);
        run("partial-page-tail-clear", PagedArrayBackingReuseTest::partialPageTailClear);
        run("page-in-and-repeated-swap", PagedArrayBackingReuseTest::repeatedSwap);
        run("pin-exclusion", PagedArrayBackingReuseTest::pinExclusion);
        run("explicit-trim-release", PagedArrayBackingReuseTest::trimRelease);
        run("pressure-discards-candidate", PagedArrayBackingReuseTest::pressureDiscard);
        run("failed-admission-discards-candidate", PagedArrayBackingReuseTest::admissionFailure);
        run("failed-spill-preserves-source", PagedArrayBackingReuseTest::spillFailure);
        run("failed-decode-is-unpublished", PagedArrayBackingReuseTest::decodeFailure);
        run("corrupt-image-is-unpublished", PagedArrayBackingReuseTest::corruptImage);
        run("factory-reentry-and-failure", PagedArrayBackingReuseTest::factoryLifecycle);
        run("probe-reentry-observations", PagedArrayBackingReuseTest::probeObservations);
        run("codec-reentry-preserves-trim-count", PagedArrayBackingReuseTest::codecReentry);
        System.out.println("PASS M3PagedArrayBackingReuseTest cases=" + cases
                + " mode=" + (requireReuse ? "reuse" : "behavior"));
    }

    private static void capacityAndAlias() throws Exception {
        try (Fixture f = new Fixture(4, 1)) {
            Cell alias = new Cell(17);
            f.array.add(alias);
            f.array.add(new Cell(18));
            f.array.add(null);
            f.array.add(new Cell(20));
            Object[] old = backing(f.array, 0);
            Cell next = new Cell(99);
            f.array.add(next);
            Object[] incoming = backing(f.array, 1);
            if (requireReuse) check(incoming == old, "capacity backing was not transferred");
            check(incoming[0] == next, "new value identity");
            for (int i = 1; i < incoming.length; i++) check(incoming[i] == null, "stale tail");
            check(backing(f.array, 0) == null, "victim stayed resident");
            check(alias.value == 17, "payload was reset");
            alias.value = 77;
            check(f.array.get(0).value == 17, "caller alias changed encoded authority");
            check(alias.value == 77, "caller alias was reassigned");
        }
    }

    private static void partialPageTailClear() throws Exception {
        try (Fixture f = new Fixture(4, 1)) {
            for (int i = 0; i < 5; i++) f.array.add(new Cell(i));
            Object[] partial = backing(f.array, 1);
            // Adversarial unused slot: logical count is one, so it is never encoded.
            partial[3] = new Object();
            f.codec.onNextRead = () -> {
                if (requireReuse)
                    for (Object element : partial) check(element == null, "partial-page tail retained");
            };
            check(f.array.get(0).value == 0, "full page reload");
            if (requireReuse) check(backing(f.array, 0) == partial, "partial backing not transferred");
            check(f.array.get(4).value == 4, "partial page reload");
            Object[] restored = backing(f.array, 1);
            for (int i = 1; i < restored.length; i++) check(restored[i] == null, "decoded partial tail");
        }
    }

    private static void repeatedSwap() throws Exception {
        try (Fixture f = new Fixture(3, 1)) {
            for (int i = 0; i < 7; i++) f.array.add(i == 2 ? null : new Cell(i * 11));
            Object[] capacity = backing(f.array, 2);
            int[] order = {6, 0, 5, 2, 1, 4, 3};
            for (int round = 0; round < 4; round++) {
                for (int index : order) {
                    Cell actual = f.array.get(index);
                    check(index == 2 ? actual == null : actual.value == index * 11, "roundtrip value");
                    if (requireReuse)
                        check(backing(f.array, index / 3) == capacity, "swap allocated another container");
                }
            }
            check(f.array.size() == 7, "swap changed size");
        }
    }

    private static void pinExclusion() throws Exception {
        try (Fixture f = new Fixture(1, 2)) {
            f.array.add(new Cell(0));
            f.array.add(new Cell(1));
            Object[] first = backing(f.array, 0);
            Object[] second = backing(f.array, 1);
            try (var pinned = f.array.pin(0)) {
                f.array.add(new Cell(2));
                check(backing(f.array, 0) == first, "pinned backing changed");
                check(pinned.value().value == 0, "pinned value changed");
                if (requireReuse) check(backing(f.array, 2) == second, "wrong capacity victim");
                try (var other = f.array.pin(2)) {
                    check(other.value().value == 2, "second pin");
                    expect(IllegalStateException.class, () -> f.array.add(new Cell(3)));
                    check(f.array.size() == 3, "failed pinned admission changed size");
                }
            }
        }
    }

    private static void trimRelease() throws Exception {
        try (Fixture f = new Fixture(1, 1)) {
            f.array.add(new Cell(1));
            Object[] retired = backing(f.array, 0);
            check(f.array.trimTo(0) == 1, "trim count");
            check(backing(f.array, 0) == null, "trim retained values");
            f.array.add(new Cell(2));
            check(backing(f.array, 1) != retired, "trim kept an idle backing cache");
        }
    }

    private static void pressureDiscard() throws Exception {
        try (Fixture f = new Fixture(1, 2)) {
            f.array.add(new Cell(0));
            f.array.add(new Cell(1));
            Object[] capacityVictim = backing(f.array, 0);
            Object[] pressureVictim = backing(f.array, 1);
            f.probe.script.add(Probe.LOW);
            f.probe.script.add(Probe.HIGH);
            f.array.add(new Cell(2));
            Object[] incoming = backing(f.array, 2);
            check(incoming != capacityVictim && incoming != pressureVictim, "pressure retained capacity");
            check(backing(f.array, 0) == null && backing(f.array, 1) == null, "pressure victim resident");
            check(f.array.stats().residentPages() == 1, "pressure resident count");
        }
    }

    private static void admissionFailure() throws Exception {
        try (Fixture f = new Fixture(1, 1)) {
            f.array.add(new Cell(1));
            Object[] abandoned = backing(f.array, 0);
            f.probe.script.add(Probe.LOW);
            expect(IllegalStateException.class, () -> f.array.add(new Cell(2)));
            check(f.array.size() == 1 && backing(f.array, 0) == null, "failed admission publication");
            f.array.add(new Cell(2));
            check(backing(f.array, 1) != abandoned, "failed admission retained scratch");
            check(f.array.get(0).value == 1, "failed admission lost disk authority");
        }
    }

    private static void spillFailure() throws Exception {
        try (Fixture f = new Fixture(1, 1)) {
            Cell original = new Cell(1);
            f.array.add(original);
            Object[] values = backing(f.array, 0);
            f.codec.failWrite = true;
            expect(UncheckedIOException.class, () -> f.array.add(new Cell(2)));
            check(backing(f.array, 0) == values && values[0] == original, "failed spill cleared source");
            check(f.array.size() == 1, "failed spill changed size");
            f.codec.failWrite = false;
            f.array.add(new Cell(2));
            if (requireReuse) check(backing(f.array, 1) == values, "spill retry failed transfer");
            check(f.array.get(0).value == 1, "spill retry lost value");
        }
    }

    private static void decodeFailure() throws Exception {
        try (Fixture f = new Fixture(2, 1)) {
            Cell oldThird = new Cell(3);
            Cell oldFourth = new Cell(4);
            f.array.add(new Cell(1));
            f.array.add(new Cell(2));
            f.array.add(oldThird);
            f.array.add(oldFourth);
            Object[] discarded = backing(f.array, 1);
            f.codec.readsUntilFailure = 1;
            expect(UncheckedIOException.class, () -> f.array.get(0));
            check(backing(f.array, 0) == null && backing(f.array, 1) == null, "partial decode published");
            if (requireReuse)
                for (Object element : discarded)
                    check(element != oldThird && element != oldFourth, "failed decode retained old payload");
            f.codec.readsUntilFailure = -1;
            check(f.array.get(0).value == 1 && f.array.get(1).value == 2, "decode retry");
            check(backing(f.array, 0) != discarded, "failed decode array retained by owner");
        }
    }

    private static void corruptImage() throws Exception {
        try (Fixture f = new Fixture(1, 1)) {
            f.array.add(new Cell(1));
            f.array.add(new Cell(2));
            Path disk = (Path) readField(page(f.array, 0), "disk");
            byte[] valid = Files.readAllBytes(disk);
            Files.write(disk, new byte[]{7}, StandardOpenOption.APPEND);
            expect(UncheckedIOException.class, () -> f.array.get(0));
            check(backing(f.array, 0) == null, "corrupt image published");
            Files.write(disk, valid, StandardOpenOption.TRUNCATE_EXISTING);
            check(f.array.get(0).value == 1, "checksum retry");
        }
    }

    private static void factoryLifecycle() throws Exception {
        try (Fixture f = new Fixture(1, 1)) {
            f.array.add(new Cell(1));
            Object[] capacity = backing(f.array, 0);
            f.array.addFromFactory(() -> {
                check(f.array.size() == 1, "factory observes prematurely published size");
                if (requireReuse) check(capacity[0] == null, "factory sees uncleared scratch");
                expect(IllegalStateException.class, () -> f.array.get(0));
                expect(IllegalStateException.class, () -> f.array.add(new Cell(9)));
                expect(IllegalStateException.class, () -> f.array.trimTo(0));
                expect(IllegalStateException.class, f.array::close);
                return new Cell(2);
            });
            if (requireReuse) check(backing(f.array, 1) == capacity, "factory backing transfer");
            Object[] discarded = backing(f.array, 1);
            expect(IllegalArgumentException.class, () -> f.array.addFromFactory(() -> {
                throw new IllegalArgumentException("controlled factory failure");
            }));
            check(f.array.size() == 2 && backing(f.array, 1) == null, "failed factory publication");
            f.array.addFromFactory(() -> new Cell(3));
            check(backing(f.array, 2) != discarded, "failed factory retained scratch");
            check(f.array.get(1).value == 2, "failed factory lost previous authority");
        }
    }

    private static void probeObservations() throws Exception {
        try (Fixture f = new Fixture(1, 1)) {
            f.array.add(new Cell(1));
            Object[] capacity = backing(f.array, 0);
            f.probe.onNextSample = () -> {
                check(f.array.size() == 1, "probe observes uncommitted size");
                check(f.array.residentPageLimit() == 1, "probe observes changed budget");
                check(f.array.estimatedResidentPageBytes() == 1024, "probe observes changed estimate");
                if (requireReuse) check(capacity[0] == null, "probe observes uncleared candidate");
            };
            f.array.add(new Cell(2));
            if (requireReuse) check(backing(f.array, 1) == capacity, "probe stole local candidate");
            for (Field field : PagedArray.class.getDeclaredFields()) {
                if (field.getType() != Object[].class) continue;
                check(Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers()),
                        "shared mutable scratch array field introduced");
                field.setAccessible(true);
                check(((Object[]) field.get(null)).length == 0, "shared retained capacity introduced");
            }
        }
    }

    private static void codecReentry() throws Exception {
        try (Fixture f = new Fixture(1, 2)) {
            f.array.add(new Cell(0));
            f.array.add(new Cell(1));
            f.codec.onNextWrite = () -> check(f.array.get(0).value == 0, "reentrant victim value");
            expect(ConcurrentModificationException.class, () -> f.array.add(new Cell(2)));
            check(f.array.size() == 2, "codec reentry published an append");
            check(backing(f.array, 0) == null, "codec reentry did not commit prior disk authority");
            check(f.array.trimTo(0) == 2, "successful empty-backing eviction lost trim count");
            check(f.array.stats().residentPages() == 0, "codec reentry left resident entries");
        }
    }

    private static void resourceObservation(String phase) throws Exception {
        final int pageCount = 16;
        final int pageSize = 256;
        IdentityHashMap<Object[], Boolean> identities = new IdentityHashMap<>();
        java.lang.management.ThreadMXBean platformBean = ManagementFactory.getThreadMXBean();
        com.sun.management.ThreadMXBean allocationBean =
                platformBean instanceof com.sun.management.ThreadMXBean bean
                        && bean.isThreadAllocatedMemorySupported() ? bean : null;
        if (allocationBean != null && !allocationBean.isThreadAllocatedMemoryEnabled())
            allocationBean.setThreadAllocatedMemoryEnabled(true);
        long threadId = Thread.currentThread().threadId();
        try (Fixture f = new Fixture(pageSize, 1)) {
            Cell value = new Cell(7);
            long bytesBefore = allocationBean == null ? -1L : allocationBean.getThreadAllocatedBytes(threadId);
            long started = System.nanoTime();
            for (int p = 0; p < pageCount; p++) {
                for (int i = 0; i < pageSize; i++) f.array.add(value);
                identities.put(backing(f.array, p), Boolean.TRUE);
            }
            long elapsed = System.nanoTime() - started;
            long bytesAfter = allocationBean == null ? -1L : allocationBean.getThreadAllocatedBytes(threadId);
            long allocated = bytesBefore < 0 || bytesAfter < 0 ? -1L : bytesAfter - bytesBefore;
            int distinct = identities.size();
            identities.clear();
            PagedArray.Stats stats = f.array.stats();
            check(stats.size() == (long) pageCount * pageSize, "resource append count");
            check(stats.spillCount() == pageCount - 1 && stats.residentPages() == 1,
                    "resource spill or residency count");
            check(f.array.get(stats.size() - 1).value == 7, "resource payload");
            System.out.println("RESOURCE phase=" + phase + " pages=" + pageCount + " pageSize=" + pageSize
                    + " appends=" + stats.size() + " spills=" + stats.spillCount()
                    + " distinctBackings=" + distinct + " allocatedBytes=" + allocated
                    + " elapsedNanos=" + elapsed);
        }
    }

    private static Object page(PagedArray<?> array, int index) throws ReflectiveOperationException {
        return ((List<?>) readField(array, "pages")).get(index);
    }

    private static Object[] backing(PagedArray<?> array, int index) throws ReflectiveOperationException {
        return (Object[]) readField(page(array, index), "values");
    }

    private static Object readField(Object owner, String name) throws ReflectiveOperationException {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(owner);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void expect(Class<? extends Throwable> type, Action action) {
        try { action.run(); }
        catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("expected " + type.getName(), failure);
        }
        throw new AssertionError("expected " + type.getName());
    }

    private static void run(String name, Action action) throws Exception {
        action.run();
        cases++;
        System.out.println("PASS " + name);
    }

    @FunctionalInterface
    private interface Action { void run() throws Exception; }

    private static final class Cell {
        int value;
        Cell(int value) { this.value = value; }
    }

    private static final class CellCodec implements PagedArray.Codec<Cell> {
        boolean failWrite;
        int readsUntilFailure = -1;
        Runnable onNextRead;
        Runnable onNextWrite;
        @Override public void write(DataOutput output, Cell value) throws IOException {
            Runnable callback = onNextWrite;
            onNextWrite = null;
            if (callback != null) callback.run();
            if (failWrite) throw new IOException("controlled spill failure");
            output.writeInt(value.value);
        }
        @Override public Cell read(DataInput input) throws IOException {
            Runnable callback = onNextRead;
            onNextRead = null;
            if (callback != null) callback.run();
            int value = input.readInt();
            if (readsUntilFailure == 0) throw new IOException("controlled decode failure");
            if (readsUntilFailure > 0) readsUntilFailure--;
            return new Cell(value);
        }
    }

    private static final class Probe implements MemoryPressure.Probe {
        static final MemoryPressure.Snapshot HIGH =
                new MemoryPressure.Snapshot(1_000_000, 500_000, 100_000, -1, -1);
        static final MemoryPressure.Snapshot LOW =
                new MemoryPressure.Snapshot(1_000_000, 1_000_000, 999_800, -1, -1);
        final ArrayDeque<MemoryPressure.Snapshot> script = new ArrayDeque<>();
        Runnable onNextSample;
        @Override public MemoryPressure.Snapshot sample() {
            Runnable callback = onNextSample;
            onNextSample = null;
            if (callback != null) callback.run();
            return script.isEmpty() ? HIGH : script.removeFirst();
        }
    }

    private static final class Fixture implements AutoCloseable {
        final Path scratch;
        final CellCodec codec = new CellCodec();
        final Probe probe = new Probe();
        final PagedArray<Cell> array;
        Fixture(int pageSize, int residentLimit) throws IOException {
            scratch = Files.createTempDirectory("m3-backing-reuse-");
            array = new PagedArray<>(scratch, pageSize, 1024, 0, 0,
                    1_000_000, codec, probe, residentLimit);
        }
        @Override public void close() throws IOException {
            try { array.close(); }
            finally { Files.deleteIfExists(scratch); }
        }
    }
}
