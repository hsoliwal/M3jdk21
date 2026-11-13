// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
/*
 * @test id=file
 * @summary Exact source memo keys, durable namespace invalidation and explicit validation
 * provenance
 * @modules jdk.m3.io/jdk.m3.optane
 * @run main/othervm -Xmx96m test.PmemMemoStoreSmokeTest
 */
/*
 * @test id=posix
 * @summary Memo contracts through the existing POSIX mapping owner
 * @requires (os.family == "linux")
 * @modules jdk.m3.io/jdk.m3.optane
 * @run main/othervm -Xcheck:jni -Xmx96m test.PmemMemoStoreSmokeTest --require-posix
 */
package test;

import jdk.m3.optane.PmemAccess;
import jdk.m3.optane.PmemBackend;
import jdk.m3.optane.PmemBlockStore;
import jdk.m3.optane.PmemGranularity;
import jdk.m3.optane.PmemMemoKey;
import jdk.m3.optane.PmemMemoStore;
import jdk.m3.optane.PmemRegion;
import jdk.m3.optane.PmemSdk;
import jdk.m3.optane.PmemSourceImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Finite behavioral proof; injected persistence failures do not simulate physical power loss. */
public final class PmemMemoStoreSmokeTest {
    private static final String SCHEMA = "a".repeat(64);
    private static final String OTHER = "b".repeat(64);
    private static final String THIRD = "c".repeat(64);
    private static final int CAPACITY = 32;
    private static final int MAXIMUM = 4096;
    private static int assertions;
    private static int simulatedFailures;

    private PmemMemoStoreSmokeTest() { }

    public static void main(String[] args) throws Exception {
        boolean posix = args.length == 1 && "--require-posix".equals(args[0]);
        if (args.length > 1 || args.length == 1 && !posix)
            throw new IllegalArgumentException("unknown test option");
        canonicalKeys();
        sourceKeys();
        realBackend(PmemBackend.FILE);
        if (posix) realBackend(PmemBackend.POSIX);
        exactEnvelopeRefusal();
        epochRefusal();
        persistenceFailures();
        concurrentInvalidation();
        bounds();
        System.out.println("PMEM_MEMO_PASS file=true posix=" + posix
                + " simulatedFailures=" + simulatedFailures + " assertions=" + assertions);
    }

    private static PmemMemoKey key() {
        return PmemMemoKey.of("analyze", Map.of("a", "1", "b", "2"));
    }

    private static void canonicalKeys() throws Exception {
        PmemMemoKey first = key();
        check(first.sha256().equals(
            "f71bce72cfd9ea93fe5e1f97178909d8e4deed19cb0754e649fc3f095dc70354"),
                "independently encoded canonical golden SHA-256");
        check(HexFormat.of().formatHex(first.canonicalBytes()).equals(
                "4d33504d454d5f4d454d4f5f4b45595f563100000007616e616c797a6500000002"
                + "0000000161000000013100000001620000000132"),
                "canonical wire-format golden bytes");
        LinkedHashMap<String, String> reversed = new LinkedHashMap<>();
        reversed.put("b", "2"); reversed.put("a", "1");
        PmemMemoKey copied = PmemMemoKey.of("analyze", reversed);
        check(first.equals(copied) && first.hashCode() == copied.hashCode(),
            "dimension order is irrelevant");
        reversed.put("a", "changed");
        byte[] returned = copied.canonicalBytes(); returned[0] ^= 1;
        check(first.equals(copied), "maps and returned arrays do not mutate canonical identity");
        check(!first.equals(PmemMemoKey.of("other", Map.of("a", "1", "b", "2"))),
            "operation binds identity");
        check(!PmemMemoKey.of("x", Map.of("a", "bc")).equals(
                PmemMemoKey.of("x", Map.of("ab", "c"))),
                "length framing separates ambiguous concatenations");
        check(!PmemMemoKey.of("x", Map.of("a", "")).equals(
                PmemMemoKey.of("x", Map.of())), "empty dimension differs from absent dimension");
        check(!PmemMemoKey.of("x", Map.of("a", "\u00e9")).equals(
                PmemMemoKey.of("x", Map.of("a", "e\u0301"))),
                "Unicode normalization is not implicit");
        check(PmemMemoKey.of("\ud83d\ude00", Map.of("a", "\ud83d\ude00")).canonicalLength() > 0,
                "valid supplementary Unicode accepted");
        for (String malformed : new String[] { "\ud800", "\udc00", "a\ud800b" }) {
            expect(IllegalArgumentException.class, () -> PmemMemoKey.of(malformed, Map.of()));
            expect(IllegalArgumentException.class, () -> PmemMemoKey.of("x", Map.of(malformed,
                "v")));
            expect(IllegalArgumentException.class, () -> PmemMemoKey.of("x", Map.of("a",
                malformed)));
            expect(IllegalArgumentException.class, () -> new PmemMemoStore.Snapshot(malformed, 0));
        }
        expect(IllegalArgumentException.class, () -> PmemMemoKey.of("", Map.of()));
        expect(IllegalArgumentException.class, () -> PmemMemoKey.of("x", Map.of("", "v")));
        expect(IllegalArgumentException.class, () -> PmemMemoKey.of("x", Map.of("a",
            "\u00e9".repeat(2049))));
        Map<String, String> many = new LinkedHashMap<>();
        for (int i = 0; i < 65; i++) many.put("field" + i, "v");
        expect(IllegalArgumentException.class, () -> PmemMemoKey.of("x", many));
        many.clear();
        for (int i = 0; i < 17; i++) many.put("field" + i, "v".repeat(4096));
        expect(IllegalArgumentException.class, () -> PmemMemoKey.of("x", many));
        expect(IllegalArgumentException.class,
                () -> new PmemMemoKey.SourceContext("A".repeat(64), OTHER, OTHER, OTHER, OTHER));
    }

    private static void sourceKeys() throws Exception {
        Path directory = Files.createTempDirectory("m3-memo-source-");
        MemoryRegion memoRegion = new MemoryRegion(CAPACITY, MAXIMUM);
        PmemBlockStore blocks = PmemBlockStore.create(memoRegion, 0, CAPACITY, MAXIMUM, SCHEMA);
        PmemMemoStore memo = new PmemMemoStore(blocks, "source-analysis");
        PmemMemoStore.Snapshot generation = memo.snapshot();
        try {
            Files.writeString(directory.resolve("A.java"), "class A {}\n");
            Files.writeString(directory.resolve("B.java"), "class B {}\n");
            PmemSourceImage.Limits limits = new PmemSourceImage.Limits(4, 4096, 1024, 256);
            PmemSourceImage.pack(directory, List.of("B.java", "A.java"), directory.resolve("first"),
                limits);
            PmemMemoKey.SourceContext context = new PmemMemoKey.SourceContext(SCHEMA, OTHER, THIRD,
                SCHEMA, OTHER);
            PmemMemoKey original;
            PmemMemoKey incremental;
            try (PmemSourceImage image = PmemSourceImage.open(directory.resolve("first"),
                PmemBackend.FILE, limits)) {
                int index = image.fileIndexOf("A.java");
                original = PmemMemoKey.source(image, index, context);
                memo.putCandidate(generation, original, new byte[] { 17, 23 }, 0, 2);
                incremental = PmemMemoKey.source(image, index, context,
                PmemMemoKey.SourceScope.FILE_DEPENDENCIES);
                memo.putCandidate(generation, incremental, new byte[] { 29, 31 }, 0, 2);
                check(!original.equals(incremental),
                "source validity scopes occupy separate domains");
                check(original.equals(PmemMemoKey.source(image, index, context,
                        PmemMemoKey.SourceScope.IMAGE_GENERATION)),
                "default scope remains conservative");
                check(original.equals(PmemMemoKey.source(image, index, 0, image.lengthAt(index),
                context)),
                        "whole-file source identity equals explicit complete range");
                check(!original.equals(PmemMemoKey.source(image, index, 1, image.lengthAt(index) -
                1, context)),
                        "atom range binds exact identity");
                check(!original.equals(PmemMemoKey.source(image, image.fileIndexOf("B.java"),
                context)),
                        "source path and bytes bind identity");
                List<PmemMemoKey.SourceContext> mutations = List.of(
                        new PmemMemoKey.SourceContext(THIRD, OTHER, THIRD, SCHEMA, OTHER),
                        new PmemMemoKey.SourceContext(SCHEMA, THIRD, THIRD, SCHEMA, OTHER),
                        new PmemMemoKey.SourceContext(SCHEMA, OTHER, SCHEMA, SCHEMA, OTHER),
                        new PmemMemoKey.SourceContext(SCHEMA, OTHER, THIRD, THIRD, OTHER),
                        new PmemMemoKey.SourceContext(SCHEMA, OTHER, THIRD, SCHEMA, THIRD));
                for (PmemMemoKey.SourceContext changed : mutations) {
                    check(!original.equals(PmemMemoKey.source(image, index, changed)),
                            "analysis context mutation misses");
                    check(!incremental.equals(PmemMemoKey.source(image, index, changed,
                            PmemMemoKey.SourceScope.FILE_DEPENDENCIES)),
    "incremental scope still binds every declared dependency and analysis context");
                }
                check(!incremental.equals(PmemMemoKey.source(image, index, 1, image.lengthAt(index)
                - 1,
                        context, PmemMemoKey.SourceScope.FILE_DEPENDENCIES)),
                        "incremental scope binds exact atom range");
                expect(NullPointerException.class, () -> PmemMemoKey.source(image, index, context,
                null));
                expect(IndexOutOfBoundsException.class,
                        () -> PmemMemoKey.source(image, index, Long.MAX_VALUE, 1, context));
            }
            Files.writeString(directory.resolve("B.java"), "class B { int x; }\n");
            PmemSourceImage.pack(directory, List.of("A.java", "B.java"), directory.resolve(
                "second"), limits);
            try (PmemSourceImage image = PmemSourceImage.open(directory.resolve("second"),
                PmemBackend.FILE, limits)) {
                PmemMemoKey changed = PmemMemoKey.source(image, image.fileIndexOf("A.java"),
                context);
                check(!original.equals(changed),
                        "dependency-generation root changes even when this file is unchanged");
                check(memo.getCandidate(generation, changed, new byte[2], 0) == -1,
                        "source generation change cannot reuse earlier analysis bytes");
                PmemMemoKey unchangedFile = PmemMemoKey.source(image, image.fileIndexOf("A.java"),
                        context, PmemMemoKey.SourceScope.FILE_DEPENDENCIES);
                check(incremental.equals(unchangedFile),
                "unrelated file change permits explicit incremental reuse");
                byte[] result = new byte[2];
                check(memo.getCandidate(generation, unchangedFile, result, 0) == 2
                        && Arrays.equals(result, new byte[] { 29, 31 }),
                        "same exact file and declared dependencies reuse prior persisted analysis");
            }
            Files.writeString(directory.resolve("A.java"), "class A { int changed; }\n");
            PmemSourceImage.pack(directory, List.of("A.java", "B.java"), directory.resolve("third"),
                limits);
            try (PmemSourceImage image = PmemSourceImage.open(directory.resolve("third"),
                PmemBackend.FILE, limits)) {
                PmemMemoKey changedFile = PmemMemoKey.source(image, image.fileIndexOf("A.java"),
                        context, PmemMemoKey.SourceScope.FILE_DEPENDENCIES);
                check(!incremental.equals(changedFile)
                        && memo.getCandidate(generation, changedFile, new byte[2], 0) == -1,
                        "changed source bytes refuse incremental reuse");
            }
            memoRegion.crash();
            PmemMemoStore reopened = new PmemMemoStore(
                    PmemBlockStore.open(memoRegion, 0, CAPACITY, MAXIMUM, SCHEMA),
                "source-analysis");
            byte[] restored = new byte[2];
            check(reopened.getCandidate(reopened.snapshot(), original, restored, 0) == 2
                    && Arrays.equals(restored, new byte[] { 17, 23 }),
                    "exact source-image analysis identity survives durable-store reopen");
        } finally {
            try (var paths = Files.walk(directory)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(
                path);
            }
        }
    }

    private static void realBackend(PmemBackend backend) throws Exception {
        Path file = Files.createTempFile("m3-memo-", ".bin");
        long length = PmemBlockStore.requiredBytes(CAPACITY, MAXIMUM);
        PmemMemoStore.Snapshot stale;
        byte[] value = { 3, 5, 7 };
        PmemMemoStore.Validation validation = new PmemMemoStore.Validation(SCHEMA, OTHER, hash(
            value));
        try {
            try (FileChannel channel = FileChannel.open(file, StandardOpenOption.WRITE)) {
                channel.position(length - 1); channel.write(ByteBuffer.wrap(new byte[] { 0 }));
            }
            try (PmemRegion region = PmemSdk.open(file, 0, length, PmemAccess.READ_WRITE, backend))
                {
                check(region.backend() == backend, "actual requested mapping backend");
                PmemBlockStore store = PmemBlockStore.create(region, 0, CAPACITY, MAXIMUM, SCHEMA);
                PmemMemoStore memo = new PmemMemoStore(store, "source-analysis");
                PmemMemoStore otherFacade = new PmemMemoStore(store, "source-analysis");
                PmemMemoStore otherNamespace = new PmemMemoStore(store, "other-analysis");
                stale = memo.snapshot();
                check(stale.generation() == 0 && stale.equals(otherFacade.snapshot()),
                "initial epoch shared");
                byte[] source = { 9, 3, 5, 7, 9 };
                memo.putCandidate(stale, key(), source, 1, 3);
                source[1] = 99;
                byte[] target = filled(8);
                check(otherFacade.getCandidate(stale, key(), target, 2) == 3,
                        "second facade finds candidate from same owner");
                check(Arrays.equals(Arrays.copyOfRange(target, 2, 5), value)
                        && target[0] == 77 && target[7] == 77,
                "owned source and bounded destination bytes");
                memo.putValidated(stale, key(), validation, value, 0, value.length);
                check(memo.getValidated(stale, key(), validation, target, 0) == 3,
                "exact validated read");
                check(memo.getCandidate(stale, key(), target, 0) == 3,
                "candidate remains separately addressable");
                for (PmemMemoStore.Validation changed : List.of(
                        new PmemMemoStore.Validation(THIRD, OTHER, hash(value)),
                        new PmemMemoStore.Validation(SCHEMA, THIRD, hash(value)),
                        new PmemMemoStore.Validation(SCHEMA, OTHER, hash(new byte[] { 8 })))) {
                    check(memo.getValidated(stale, key(), changed, target, 0) == -1,
                            "validation contract, evidence and result identity all required");
                }
                long before = store.committedCount();
                expect(IllegalArgumentException.class,
                        () -> memo.putValidated(stale, key(), validation, new byte[] { 8 }, 0, 1));
                check(store.committedCount() == before,
                "invalid validation digest consumes no slot");
                check(otherNamespace.getCandidate(otherNamespace.snapshot(), key(), target, 0) ==
                -1,
                        "namespace isolation");
                otherNamespace.putCandidate(otherNamespace.snapshot(), key(), value, 0,
                value.length);
                PmemMemoStore.Snapshot fresh = otherFacade.invalidate(stale);
                check(fresh.generation() == 1 && memo.snapshot().equals(fresh),
                "durable epoch visible across facades");
                expect(IllegalStateException.class, () -> memo.getCandidate(stale, key(), target,
                0));
                expect(IllegalStateException.class, () -> memo.putCandidate(stale, key(), value, 0,
                value.length));
                expect(IllegalStateException.class, () -> memo.invalidate(stale));
                check(memo.getCandidate(fresh, key(), target, 0) == -1,
                "old generation is a fresh-epoch miss");
                check(otherNamespace.getCandidate(otherNamespace.snapshot(), key(), target, 0) == 3,
                        "another namespace remains usable");
                memo.putCandidate(fresh, key(), value, 0, value.length);
                memo.putValidated(fresh, key(), validation, value, 0, value.length);
                PmemMemoKey empty = PmemMemoKey.of("empty", Map.of());
                memo.putCandidate(fresh, empty, new byte[0], 0, 0);
                check(memo.getCandidate(fresh, empty, new byte[0], 0) == 0,
                "empty value is distinct from a miss");
                byte[] unchanged = filled(2);
                expect(IndexOutOfBoundsException.class, () -> memo.getCandidate(fresh, key(),
                unchanged, 0));
                check(Arrays.equals(unchanged, filled(2)),
                "undersized destination remains untouched");
            }
            try (PmemRegion region = PmemSdk.open(file, 0, length, PmemAccess.READ_WRITE, backend))
                {
                PmemBlockStore store = PmemBlockStore.open(region, 0, CAPACITY, MAXIMUM, SCHEMA);
                PmemMemoStore memo = new PmemMemoStore(store, "source-analysis");
                PmemMemoStore.Snapshot current = memo.snapshot();
                check(current.generation() == 1, "epoch survives actual close/reopen");
                expect(IllegalStateException.class, () -> memo.candidateLength(stale, key()));
                byte[] restored = new byte[3];
                check(memo.getCandidate(current, key(), restored, 0) == 3 && Arrays.equals(restored,
                value),
                        "candidate survives actual close/reopen");
                check(memo.validatedLength(current, key(), validation) == 3
                        && memo.getValidated(current, key(), validation, restored, 0) == 3,
                        "validation identity survives actual close/reopen");
            }
        } finally { Files.deleteIfExists(file); }
    }

    private static void exactEnvelopeRefusal() throws Exception {
        MemoryRegion region = new MemoryRegion(CAPACITY, MAXIMUM);
        PmemBlockStore store = PmemBlockStore.create(region, 0, CAPACITY, MAXIMUM, SCHEMA);
        PmemMemoStore memo = new PmemMemoStore(store, "ns");
        PmemMemoStore.Snapshot snapshot = memo.snapshot();
        long ordinal = memo.putCandidate(snapshot, key(), new byte[] { 11, 13 }, 0, 2);
        PmemBlockStore.Metadata metadata = store.metadata(ordinal);
        byte[] original = new byte[metadata.length()]; store.read(ordinal, original, 0);
        int[] mutations = { 0, 11, 15, 19, 23, 31, 35, 39, 40, 136, 138, original.length - 3 };
        for (int index : mutations) {
            byte[] forged = original.clone(); forged[index] ^= 1;
            store.append(metadata.keySha256(), forged, 0, forged.length);
            byte[] target = filled(8);
            expect(IOException.class, () -> memo.getCandidate(snapshot, key(), target, 0));
            check(Arrays.equals(target, filled(8)),
                "forged valid block cannot bypass exact memo envelope");
        }
        store.append(metadata.keySha256(), original, 0, original.length);
        check(memo.getCandidate(snapshot, key(), new byte[2], 0) == 2,
            "valid exact envelope accepted");
        PmemMemoStore.Validation validation = new PmemMemoStore.Validation(SCHEMA, OTHER, hash(new
            byte[] { 11 }));
        ordinal = memo.putValidated(snapshot, key(), validation, new byte[] { 11 }, 0, 1);
        metadata = store.metadata(ordinal);
        byte[] forged = new byte[metadata.length()]; store.read(ordinal, forged, 0);
        forged[forged.length - 1] = 12;
        store.append(metadata.keySha256(), forged, 0, forged.length);
        byte[] target = filled(4);
        expect(IOException.class, () -> memo.getValidated(snapshot, key(), validation, target, 0));
        check(Arrays.equals(target, filled(4)),
            "validated payload mismatch refuses before destination write");
    }

    private static void epochRefusal() throws Exception {
        MemoryRegion region = new MemoryRegion(CAPACITY, MAXIMUM);
        PmemBlockStore store = PmemBlockStore.create(region, 0, CAPACITY, MAXIMUM, SCHEMA);
        PmemMemoStore memo = new PmemMemoStore(store, "ns");
        memo.invalidate(memo.snapshot());
        PmemBlockStore.Metadata metadata = store.metadata(store.committedCount() - 1);
        byte[] frame = new byte[metadata.length()]; store.read(metadata.ordinal(), frame, 0);
        byte[] wrongNamespace = frame.clone(); wrongNamespace[24] ^= 1;
        store.append(metadata.keySha256(), wrongNamespace, 0, wrongNamespace.length);
        expect(IOException.class, memo::snapshot);
        ByteBuffer.wrap(frame).putLong(16, Long.MAX_VALUE);
        store.append(metadata.keySha256(), frame, 0, frame.length);
        PmemMemoStore.Snapshot exhausted = memo.snapshot();
        check(exhausted.generation() == Long.MAX_VALUE, "maximum epoch remains readable");
        long count = store.committedCount();
        expect(IllegalStateException.class, () -> memo.invalidate(exhausted));
        check(store.committedCount() == count, "epoch overflow refuses before mutation");
    }

    private static void persistenceFailures() throws Exception {
        for (int stage = 1; stage <= 3; stage++) {
            for (boolean copyBeforeFailure : new boolean[] { false, true }) {
                MemoryRegion region = new MemoryRegion(CAPACITY, MAXIMUM);
                PmemBlockStore store = PmemBlockStore.create(region, 0, CAPACITY, MAXIMUM, SCHEMA);
                PmemMemoStore memo = new PmemMemoStore(store, "ns");
                PmemMemoStore.Snapshot prior = memo.snapshot();
                memo.putCandidate(prior, key(), new byte[] { 19 }, 0, 1);
                region.failPersist(stage, copyBeforeFailure);
                expect(IOException.class, () -> memo.invalidate(prior));
                simulatedFailures++;
                check(store.requiresReopen(), "uncertain epoch publication poisons borrowed store");
                expect(IllegalStateException.class, memo::snapshot);
                region.crash();
                PmemBlockStore reopened = PmemBlockStore.open(region, 0, CAPACITY, MAXIMUM, SCHEMA);
                PmemMemoStore recovered = new PmemMemoStore(reopened, "ns");
                long expected = stage == 3 && copyBeforeFailure ? 1 : 0;
                check(recovered.snapshot().generation() == expected,
                "recovery reflects actual committed marker");
                if (expected == 0) {
                    check(recovered.getCandidate(prior, key(), new byte[1], 0) == 1,
                            "prior value usable when invalidation did not commit");
                } else {
                    expect(IllegalStateException.class,
                            () -> recovered.getCandidate(prior, key(), new byte[1], 0));
                    check(recovered.getCandidate(recovered.snapshot(), key(), new byte[1], 0) == -1,
    "committed invalidation hides prior value after uncertain acknowledgement");
                }
            }
        }
    }

    private static void bounds() throws Exception {
        MemoryRegion region = new MemoryRegion(1, 512);
        PmemBlockStore store = PmemBlockStore.create(region, 0, 1, 512, SCHEMA);
        PmemMemoStore memo = new PmemMemoStore(store, "ns");
        PmemMemoStore.Snapshot current = memo.snapshot();
        expect(IllegalArgumentException.class,
                () -> memo.putCandidate(current, key(), new byte[512], 0, 512));
        expect(IllegalArgumentException.class,
                () -> memo.getCandidate(new PmemMemoStore.Snapshot("another", 0), key(), new
                byte[1], 0));
        check(store.committedCount() == 0, "prevalidation consumes no blocks");
        memo.putCandidate(current, key(), new byte[] { 1 }, 0, 1);
        expect(IOException.class, () -> memo.invalidate(current));
        check(!store.requiresReopen() && memo.snapshot().equals(current),
            "capacity refusal preserves current epoch");
        check(memo.getCandidate(current, key(), new byte[1], 0) == 1,
            "capacity failure preserves existing memo");
        expect(IllegalArgumentException.class, () -> new PmemMemoStore.Snapshot("ns", -1));
        expect(IllegalArgumentException.class, () -> new PmemMemoStore(store, "\ud800"));
    }

    private static void concurrentInvalidation() throws Exception {
        MemoryRegion region = new MemoryRegion(CAPACITY, MAXIMUM);
        PmemBlockStore store = PmemBlockStore.create(region, 0, CAPACITY, MAXIMUM, SCHEMA);
        PmemMemoStore workerFacade = new PmemMemoStore(store, "ns");
        PmemMemoStore invalidator = new PmemMemoStore(store, "ns");
        CountDownLatch captured = new CountDownLatch(1), resume = new CountDownLatch(1);
        var executor = Executors.newSingleThreadExecutor();
        try {
            var worker = executor.submit(() -> {
                PmemMemoStore.Snapshot startingGeneration = workerFacade.snapshot();
                captured.countDown();
                if (!resume.await(5, TimeUnit.SECONDS)) throw new AssertionError(
                "worker release timeout");
                try {
                    workerFacade.putCandidate(startingGeneration, key(), new byte[] { 41 }, 0, 1);
                    return false;
                } catch (IllegalStateException expected) {
                    return true;
                }
            });
            check(captured.await(5, TimeUnit.SECONDS),
                "worker captured pre-invalidation generation");
            PmemMemoStore.Snapshot fresh = invalidator.invalidate(invalidator.snapshot());
            long committed = store.committedCount();
            resume.countDown();
            check(worker.get(5, TimeUnit.SECONDS),
                    "delayed computation cannot publish after another thread invalidates");
            check(store.committedCount() == committed
                    && invalidator.getCandidate(fresh, key(), new byte[1], 0) == -1,
                    "stale concurrent publication consumes no slot or visible result");
        } finally {
            resume.countDown();
            executor.shutdownNow();
            check(executor.awaitTermination(5, TimeUnit.SECONDS),
                "worker terminates within test budget");
        }
    }

    private static byte[] filled(int count) {
        byte[] bytes = new byte[count]; Arrays.fill(bytes, (byte) 77); return bytes;
    }

    private static String hash(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }

    @FunctionalInterface private interface Checked { void run() throws Exception; }

    private static void expect(Class<? extends Throwable> type, Checked action) throws Exception {
        assertions++;
        try { action.run(); }
        catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("expected " + type.getSimpleName() + ", got " + failure,
                failure);
        }
        throw new AssertionError("expected " + type.getSimpleName());
    }

    /**
     * Independent volatile/durable byte arrays, with failure before or after whole-range
     * persistence.
     */
    private static final class MemoryRegion implements PmemRegion {
        private final byte[] bytes;
        private final byte[] durable;
        private int persistCount;
        private int failAt = -1;
        private boolean copyBeforeFailure;
        private boolean open = true;

        MemoryRegion(int capacity, int maximum) {
            bytes = new byte[Math.toIntExact(PmemBlockStore.requiredBytes(capacity, maximum))];
            durable = new byte[bytes.length];
        }
        void failPersist(int relative, boolean copyFirst) {
            failAt = persistCount + relative; copyBeforeFailure = copyFirst;
        }
        void crash() { System.arraycopy(durable, 0, bytes, 0, bytes.length); failAt = -1; }
        @Override public long length() { return bytes.length; }
        @Override public long fileOffset() { return 0; }
        @Override public Path path() { return Path.of("simulated-memo-region"); }
        @Override public PmemAccess access() { return PmemAccess.READ_WRITE; }
        @Override public PmemBackend backend() { return PmemBackend.FILE; }
        @Override public PmemGranularity persistenceGranularity() { return PmemGranularity.PAGE; }
        @Override public boolean isOpen() { return open; }
        @Override public void close() { open = false; }
        @Override public void read(long offset, byte[] target, int arrayOffset, int count) {
            System.arraycopy(bytes, index(offset, count), target, arrayOffset, count);
        }
        @Override public void write(long offset, byte[] source, int arrayOffset, int count) {
            System.arraycopy(source, arrayOffset, bytes, index(offset, count), count);
        }
        @Override public void persist(long offset, long count) throws IOException {
            int length = Math.toIntExact(count), start = index(offset, length);
            boolean fail = ++persistCount == failAt;
            if (!fail || copyBeforeFailure) System.arraycopy(bytes, start, durable, start, length);
            if (fail) throw new IOException("injected memo persistence failure");
        }
        private int index(long offset, int length) {
            if (!open) throw new IllegalStateException("region closed");
            return Objects.checkFromIndexSize(Math.toIntExact(offset), length, bytes.length);
        }
    }
}
