/* Copyright 2026 Hitesh Soliwal, Synexia.
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.text;

import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Post-admission cache atom proof. Reads an existing immutable M3LEX v2 image.
 * Reflection composes the private admitted state, bypassing manifest admission.
 * This is not a proof of open/openLazy, sidecar parsing or OS sharing.
 */
public final class SharedLexiconCatalogSiWarmTest {
    private static final String SOURCE = "dictlang.si-units";
    private static final String PAYLOAD = "{\"si_decimal_exponent\":-6,\"si_dimension_packed\":42,"
            + "\"si_offset\":-0.0,\"si_prefixable\":true}";
    private static int checks;
    private static volatile Object sink;
    private SharedLexiconCatalogSiWarmTest() { }

    private static void check(boolean value, String detail) {
        checks++;
        if (!value) throw new AssertionError(detail);
    }

    private static final class CountingPayloads
            extends ConcurrentHashMap<SharedLexiconCatalog.SourceIdentity, M3PrecomputePayload> {
        private static final long serialVersionUID = 1L;
        final transient AtomicInteger gets = new AtomicInteger();
        final transient AtomicInteger computes = new AtomicInteger();
        final transient AtomicInteger loads = new AtomicInteger();
        volatile transient CountDownLatch entered;
        volatile transient CountDownLatch release;
        @Override public M3PrecomputePayload get(Object key) {
            gets.incrementAndGet();
            return super.get(key);
        }
        @Override public M3PrecomputePayload computeIfAbsent(SharedLexiconCatalog.SourceIdentity key,
                Function<? super SharedLexiconCatalog.SourceIdentity, ? extends M3PrecomputePayload> loader) {
            computes.incrementAndGet();
            return super.computeIfAbsent(key, identity -> {
                loads.incrementAndGet();
                if (entered != null) {
                    entered.countDown();
                    await(release);
                }
                return loader.apply(identity);
            });
        }
    }

    private record Fixture(SharedLexiconCatalog catalog, Object slot, SharedLexiconImage image) { }

    private static Field field(Class<?> type, String name) throws ReflectiveOperationException {
        Field result = type.getDeclaredField(name);
        result.setAccessible(true);
        return result;
    }

    private static Fixture fixture(Path imagePath, boolean lazy, boolean missing)
            throws Exception {
        SharedLexiconImage image = SharedLexiconImage.open(imagePath);
        check(image.version() == 2 && image.size() > 0, "v2 existing image");
        Class<?> slotType = Class.forName("jdk.m3.text.SharedLexiconCatalog$ShardSlot");
        Constructor<?> slotConstructor = slotType.getDeclaredConstructor(Path.class, String.class,
                String.class, String.class, int.class, long.class, byte[].class);
        slotConstructor.setAccessible(true);
        Path admittedPath = missing ? imagePath.resolveSibling("M3_SI_WARM_MISSING_NO_WRITE.m3lex") : imagePath;
        check(!missing || !Files.exists(admittedPath), "missing image fixture remains absent");
        Object slot = slotConstructor.newInstance(admittedPath, imagePath.getFileName().toString(),
                image.recordText(0), image.recordText(image.size() - 1), image.size(), image.utf16Units(),
                MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(imagePath)));
        if (!lazy) field(slotType, "image").set(slot, image);
        var mappings = new HashMap<SharedLexiconCatalog.Coordinate,
                List<SharedLexiconCatalog.SourceMapping>>();
        var facts = new HashMap<SharedLexiconCatalog.Coordinate, SharedLexiconCatalog.PrecomputeFacts>();
        for (int row = 0; row < image.size(); row++) {
            String text = image.recordText(row);
            var coordinate = new SharedLexiconCatalog.Coordinate(0, row);
            var rows = new ArrayList<SharedLexiconCatalog.SourceMapping>();
            for (String record : List.of("U" + row, "ALIAS" + row)) {
                rows.add(mapping(SOURCE, record, text, coordinate, PAYLOAD));
            }
            rows.add(mapping("dictlang.acronyms", "U" + row, text, coordinate, "{"));
            if (row == 0) {
                rows.add(mapping(SOURCE, "BAD_JSON", text, coordinate, "{"));
                rows.add(mapping(SOURCE, "MISSING", text, coordinate, "{}"));
                rows.add(mapping(SOURCE, "WRONG_TYPE", text, coordinate, PAYLOAD.replace("-6", "\"x\"")));
                rows.add(mapping(SOURCE, "RANGE", text, coordinate, PAYLOAD.replace("-6", "101")));
            }
            mappings.put(coordinate, rows);
            facts.put(coordinate, new SharedLexiconCatalog.PrecomputeFacts(coordinate, text.length(),
                    Integer.toUnsignedLong(text.hashCode()), text.codePointCount(0, text.length()),
                    0, 0, true, true, false, "si"));
        }
        Constructor<?> constructor = SharedLexiconCatalog.class.getDeclaredConstructors()[0];
        check(constructor.getParameterCount() == 10, "pinned constructor shape");
        constructor.setAccessible(true);
        SharedLexiconCatalog catalog = (SharedLexiconCatalog) constructor.newInstance(
                List.of(slot), List.of(imagePath.getFileName().toString()), List.of(image.recordText(0)),
                List.of(image.recordText(image.size() - 1)), mappings, facts, List.of(),
                Optional.empty(), Optional.empty(), (long) image.size());
        return new Fixture(catalog, slot, image);
    }

    private static SharedLexiconCatalog.SourceMapping mapping(String source, String id, String text,
            SharedLexiconCatalog.Coordinate coordinate, String payload) {
        return new SharedLexiconCatalog.SourceMapping(source, "units", "si-unit", "und", id, text,
                coordinate, "preserved-" + id, "UNIT_" + id, "-", "si", payload);
    }

    private static CountingPayloads install(SharedLexiconCatalog catalog) throws Exception {
        var cache = new CountingPayloads();
        field(SharedLexiconCatalog.class, "payloads").set(catalog, cache);
        return cache;
    }

    private static void unit(M3LexiconPrecompute.SiUnitPrecompute value) {
        check(value.decimalExponent() == -6 && value.dimensionPacked() == 42L, "typed integers");
        check(Double.doubleToRawLongBits(value.offset()) == Double.doubleToRawLongBits(-0.0),
                "signed zero preserved");
        check(value.prefixable(), "typed boolean");
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        try { action.run(); } catch (Throwable failure) {
            check(type.isInstance(failure), "expected " + type + " got " + failure);
            return;
        }
        throw new AssertionError("missing " + type);
    }

    public static void semantics(Path path) throws Exception {
        for (boolean lazy : new boolean[]{false, true}) {
            Fixture fixture = fixture(path, lazy, false);
            SharedLexiconCatalog catalog = fixture.catalog();
            CountingPayloads cache = install(catalog);
            try {
                var first = catalog.findSiUnitPrecompute(SOURCE, "U0").orElseThrow();
                unit(first);
                var second = catalog.findSiUnitPrecompute(SOURCE, "U0").orElseThrow();
                unit(second);
                check(first != second, "typed results remain fresh");
                var generic = catalog.findPrecomputePayload(SOURCE, "U0").orElseThrow();
                check(cache.loads.get() == 1, "generic and SI share parsed payload");
                check(catalog.findPrecomputePayload(SOURCE, "U0").orElseThrow() == generic,
                        "generic cached instance");
                for (int row = 0; row < fixture.image().size(); row++) {
                    unit(catalog.findSiUnitPrecompute(SOURCE, "U" + row).orElseThrow());
                    unit(catalog.findSiUnitPrecompute(SOURCE, "ALIAS" + row).orElseThrow());
                    check(catalog.findMapping(SOURCE, "U" + row).orElseThrow().recordId().equals("U" + row),
                            "donor record IDs preserved");
                }
                int loads = cache.loads.get();
                check(catalog.findSiUnitPrecompute("dictlang.acronyms", "U0").isEmpty(), "source family");
                check(catalog.findSiUnitPrecompute(SOURCE, "unknown").isEmpty(), "unknown record");
                check(catalog.findSiUnitPrecompute("", "").isEmpty(), "empty unknown identity");
                check(cache.loads.get() == loads, "non-SI and misses never parse");
                expect(NullPointerException.class, () -> catalog.findSiUnitPrecompute(null, "U0"));
                expect(NullPointerException.class, () -> catalog.findSiUnitPrecompute(SOURCE, null));
                for (String bad : List.of("BAD_JSON", "MISSING", "WRONG_TYPE", "RANGE")) {
                    expect(IllegalArgumentException.class, () -> catalog.findSiUnitPrecompute(SOURCE, bad));
                    expect(IllegalArgumentException.class, () -> catalog.findSiUnitPrecompute(SOURCE, bad));
                }
                check(!cache.containsKey(new SharedLexiconCatalog.SourceIdentity(SOURCE, "BAD_JSON")),
                        "parse failure not cached");
                check(cache.containsKey(new SharedLexiconCatalog.SourceIdentity(SOURCE, "RANGE")),
                        "projection failure retains successfully parsed payload");
            } finally { catalog.close(); }
            check(cache.isEmpty(), "close clears payloads");
            expect(IllegalStateException.class, () -> catalog.findSiUnitPrecompute(null, null));
        }
        Fixture missing = fixture(path, true, true);
        CountingPayloads cache = install(missing.catalog());
        cache.put(new SharedLexiconCatalog.SourceIdentity(SOURCE, "U0"), M3PrecomputePayload.parse(PAYLOAD));
        try {
            expect(UncheckedIOException.class, () -> missing.catalog().findSiUnitPrecompute(SOURCE, "U0"));
            expect(UncheckedIOException.class, () -> missing.catalog().findSiUnitPrecompute(SOURCE, "U0"));
            check(cache.gets.get() == 0, "image failure precedes warm cache read");
            check(field(missing.slot().getClass(), "image").get(missing.slot()) == null,
                    "failed image not published");
            field(missing.slot().getClass(), "image").set(missing.slot(), missing.image());
            unit(missing.catalog().findSiUnitPrecompute(SOURCE, "U0").orElseThrow());
        } finally { missing.catalog().close(); }
    }

    public static void warm(Path path) throws Exception {
        Fixture fixture = fixture(path, true, false);
        CountingPayloads cache = install(fixture.catalog());
        try {
            for (int call = 0; call < 101; call++)
                unit(fixture.catalog().findSiUnitPrecompute(SOURCE, "U0").orElseThrow());
            check(cache.loads.get() == 1, "one loader");
            check(cache.computes.get() == 1, "warm compute count=" + cache.computes.get());
            check(cache.gets.get() == 101, "warm get count=" + cache.gets.get());
        } finally { fixture.catalog().close(); }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) throw new AssertionError("latch timeout");
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new AssertionError(failure);
        }
    }

    public static void concurrent(Path path) throws Exception {
        Fixture fixture = fixture(path, true, false);
        CountingPayloads cache = install(fixture.catalog());
        var errors = new java.util.concurrent.ConcurrentLinkedQueue<Throwable>();
        var start = new CountDownLatch(1);
        var workers = new ArrayList<Thread>();
        for (int at = 0; at < 8; at++) {
            Thread worker = new Thread(() -> {
                await(start);
                try {
                    for (int call = 0; call < 250; call++) {
                        var value = fixture.catalog().findSiUnitPrecompute(SOURCE, "U0").orElseThrow();
                        if (value.decimalExponent() != -6 || value.dimensionPacked() != 42L)
                            throw new AssertionError("concurrent value");
                    }
                } catch (Throwable error) { errors.add(error); }
            });
            workers.add(worker); worker.start();
        }
        start.countDown();
        try {
            for (Thread worker : workers) { worker.join(15000); check(!worker.isAlive(), "worker reaped"); }
            check(errors.isEmpty(), "concurrent errors " + errors);
            check(cache.loads.get() == 1 && cache.size() == 1, "one concurrent publication");
        } finally { fixture.catalog().close(); }

        Fixture closing = fixture(path, true, false);
        CountingPayloads blocked = install(closing.catalog());
        blocked.entered = new CountDownLatch(1); blocked.release = new CountDownLatch(1);
        var lookup = new Thread(() -> {
            try { closing.catalog().findSiUnitPrecompute(SOURCE, "U0").orElseThrow(); }
            catch (Throwable error) { errors.add(error); }
        });
        var closed = new CountDownLatch(1);
        var closerStarted = new CountDownLatch(1);
        var closer = new Thread(() -> { closerStarted.countDown(); closing.catalog().close(); closed.countDown(); });
        lookup.start(); await(blocked.entered); closer.start(); await(closerStarted);
        try { check(!closed.await(100, TimeUnit.MILLISECONDS), "close waits for active reader"); }
        finally {
            blocked.release.countDown(); lookup.join(15000); closer.join(15000);
            check(!lookup.isAlive() && !closer.isAlive(), "lifecycle threads reaped");
        }
        check(errors.isEmpty() && blocked.isEmpty(), "close clears published payload");
        expect(IllegalStateException.class, () -> closing.catalog().findSiUnitPrecompute(SOURCE, "U0"));
    }

    private static void batch(SharedLexiconCatalog catalog, String[] ids, int calls) {
        for (int call = 0; call < calls; call++)
            sink = catalog.findSiUnitPrecompute(SOURCE, ids[call & 3]).orElseThrow();
    }

    public static void measure(Path path, String label) throws Exception {
        var bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        check(bean.isThreadAllocatedMemorySupported(), "allocation counter");
        bean.setThreadAllocatedMemoryEnabled(true);
        Fixture fixture = fixture(path, true, false);
        try {
            String[] ids = {"U0", "ALIAS0", "U1", "ALIAS1"};
            check(fixture.image().size() >= 2, "mixed-ID fixture");
            for (String id : ids) fixture.catalog().findSiUnitPrecompute(SOURCE, id).orElseThrow();
            for (int pass = 0; pass < 20; pass++) batch(fixture.catalog(), ids, 100000);
            for (int sample = 0; sample < 5; sample++) {
                long bytes = bean.getThreadAllocatedBytes(Thread.currentThread().threadId());
                long began = System.nanoTime();
                batch(fixture.catalog(), ids, 200000);
                long elapsed = System.nanoTime() - began;
                long allocated = bean.getThreadAllocatedBytes(Thread.currentThread().threadId()) - bytes;
                System.out.println("M3_SI_WARM_MEASURE label=" + label + " sample=" + sample
                        + " calls=200000 bytes=" + allocated + " nanos=" + elapsed);
            }
        } finally { fixture.catalog().close(); }
    }

    public static void main(String[] args) throws Exception {
        Path path = Path.of(args[1]);
        byte[] before = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path));
        switch (args[0]) {
            case "semantics" -> { semantics(path); concurrent(path); }
            case "warm" -> warm(path);
            case "measure" -> measure(path, args[2]);
            default -> throw new IllegalArgumentException("mode");
        }
        check(MessageDigest.isEqual(before,
                MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path))), "image unchanged");
        System.out.println("M3_SI_WARM_PASS mode=" + args[0] + " checks=" + checks
                + " image_sha256=" + HexFormat.of().formatHex(before));
    }
}
