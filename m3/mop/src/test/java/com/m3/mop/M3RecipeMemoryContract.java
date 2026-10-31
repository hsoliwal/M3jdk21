// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.mop;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/** CPU-only original recipe-memory schema and refusal differential contracts. */
public final class M3RecipeMemoryContract {
    private static int checks;
    private static final String RECIPE = "com.synexia.rewrite.M3Java21ConvergenceRecipe";
    private static final String LEDGER = "recipes/prefix/RECIPE_MEMORY.tsv";

    private M3RecipeMemoryContract() { }

    private static void check(boolean condition, String reason) {
        checks++;
        if (!condition) throw new AssertionError(reason);
    }

    @FunctionalInterface private interface Checked { void run() throws Exception; }
    private static void rejects(Class<? extends Throwable> kind, Checked action, String reason) throws Exception {
        checks++;
        try { action.run(); }
        catch (Throwable problem) {
            if (kind.isInstance(problem)) return;
            throw new AssertionError(reason + " wrong exception " + problem, problem);
        }
        throw new AssertionError(reason + " did not refuse");
    }

    private static final class Fixture {
        final Path root, patch, guard, receipt, ledger;
        Fixture() throws IOException {
            root = Files.createTempDirectory("m3-recipe-memory");
            patch = root.resolve("patches/prefix.diff");
            guard = root.resolve("guards/PrefixedStringTest.java");
            receipt = root.resolve("proof/proof.tsv");
            ledger = root.resolve(LEDGER);
            Files.createDirectories(patch.getParent());
            Files.createDirectories(guard.getParent());
            Files.createDirectories(receipt.getParent());
            Files.writeString(patch, "@@ -1 +1 @@\n-before\n+after\n", StandardCharsets.UTF_8);
            Files.writeString(guard, "class PrefixedStringTest {}\n", StandardCharsets.UTF_8);
            proof();
        }
        void proof() throws IOException {
            Files.writeString(receipt, "M3_RECIPE_PROOF_V1\t" + RECIPE + "\t"
                    + M3RecipeMemory.sha256(patch) + "\t" + M3RecipeMemory.sha256(guard)
                    + "\ncompile\tPASS\ntest\tPASS\nfixed_point\tPASS\n", StandardCharsets.UTF_8);
        }
        M3RecipeMemory.Entry row(Instant timestamp) throws IOException {
            return new M3RecipeMemory.Entry(timestamp, RECIPE, "failing-file-row",
                    "M3_TEST_ASSERTION_FAILED: prefix seam", M3RecipeMemory.correction(root,
                    "patches/prefix.diff"), "coding-agent", "guards/PrefixedStringTest.java",
                    "Preserve UTF-16 source seams when normalizing a composite prefix");
        }
    }

    private static void appendAndRemember() throws Exception {
        Fixture f = new Fixture();
        check(M3RecipeMemory.read(f.ledger).isEmpty(), "absent ledger has no rows");
        Instant t = Instant.parse("2026-10-20T10:00:00Z");
        M3RecipeMemory.Entry entry = f.row(t);
        M3RecipeMemory.append(f.root, LEDGER, entry, f.receipt);
        check(M3RecipeMemory.read(f.ledger).equals(List.of(entry)), "one exact proven row");
        check(Files.readString(f.ledger).startsWith(M3RecipeMemory.HEADER), "exact schema header");
        byte[] former = Files.readAllBytes(f.ledger);
        rejects(IOException.class, () -> M3RecipeMemory.append(f.root, LEDGER, entry, f.receipt),
                "duplicate corrected recipe is refused");
        Files.writeString(f.patch, "@@ -1 +1 @@\n-old\n+new code\n", StandardCharsets.UTF_8);
        f.proof();
        M3RecipeMemory.Entry second = f.row(t.plusSeconds(3600));
        M3RecipeMemory.append(f.root, LEDGER, second, f.receipt);
        byte[] current = Files.readAllBytes(f.ledger);
        M3RecipeMemory.verifyAppendOnly(former, current);
        check(M3RecipeMemory.read(f.ledger).equals(List.of(entry, second)),
                "append, not rewrite or delete prior lesson");
        check(second.correction().startsWith("SELF@patches/prefix.diff#"),
                "self-containing commit uses source-sealed correction reference");
        rejects(IOException.class, () -> M3RecipeMemory.append(f.root, LEDGER,
                f.row(t.minusSeconds(1)), f.receipt), "backdated entry refused");
        rejects(IllegalArgumentException.class,
                () -> M3RecipeMemory.verifyAppendOnly(current, former), "truncation refused");
        byte[] corrupted = current.clone(); corrupted[20] ^= 1;
        rejects(IllegalArgumentException.class,
                () -> M3RecipeMemory.verifyAppendOnly(current, corrupted), "prior row rewrite refused");
    }

    private static void proofAndChangeRefusal() throws Exception {
        Fixture f = new Fixture();
        final var stale = f.row(Instant.parse("2026-10-20T10:00:00Z"));
        Files.writeString(f.patch, "tampered\n");
        rejects(IOException.class, () -> M3RecipeMemory.append(f.root, LEDGER, stale, f.receipt),
                "changed patch must refuse before any ledger write");
        check(!Files.exists(f.ledger), "no ledger published on patch drift");
        final var fresh = f.row(Instant.parse("2026-10-20T11:00:00Z"));
        rejects(IOException.class, () -> M3RecipeMemory.append(f.root, LEDGER, fresh, f.receipt),
                "old receipt cannot qualify changed patch");
        f.proof();
        Files.writeString(f.guard, "class DifferentGuard {}\n");
        rejects(IOException.class, () -> M3RecipeMemory.append(f.root, LEDGER, fresh, f.receipt),
                "old receipt cannot qualify different regression fixture");
        f.proof();
        Files.writeString(f.receipt, Files.readString(f.receipt).replace("fixed_point\tPASS", "fixed_point\tFAIL"));
        rejects(IOException.class, () -> M3RecipeMemory.append(f.root, LEDGER, fresh, f.receipt),
                "failed exact fixed-point gate refused");
        f.proof();
        M3RecipeMemory.append(f.root, LEDGER, fresh, f.receipt);
        check(M3RecipeMemory.read(f.ledger).size() == 1, "corrected guard and proof accepted");
        check(!Files.readString(f.ledger).contains("M3_RECIPE_PROOF_V1"),
                "proof receipt itself is not repeated in every memory row");
    }

    private static void schemaAndPathRefusal() throws Exception {
        Fixture f = new Fixture();
        var t = Instant.parse("2026-10-20T10:00:00Z");
        rejects(IllegalArgumentException.class,
                () -> new M3RecipeMemory.Entry(t, "bad", "event", "observed", "correction",
                        "agent", "fixture", "lesson"), "recipe must have qualified package identity");
        rejects(IllegalArgumentException.class,
                () -> new M3RecipeMemory.Entry(t, RECIPE, "evt\tbad", "observed", "correction",
                        "agent", "fixture", "lesson"), "tabs cannot forge extra TSV columns");
        rejects(IOException.class, () -> M3RecipeMemory.append(f.root, "../escape.tsv", f.row(t), f.receipt),
                "path traversal rejected");
        rejects(IOException.class, () -> M3RecipeMemory.append(f.root, "/absolute.tsv", f.row(t), f.receipt),
                "absolute location rejected");
        Path link = f.root.resolve("alias");
        try {
            Files.createSymbolicLink(link, f.patch);
            rejects(IOException.class,
                    () -> M3RecipeMemory.append(f.root, "alias/destination.tsv", f.row(t), f.receipt),
                    "symlinked path rejected");
        } catch (UnsupportedOperationException | IOException | SecurityException ignored) {
            // Some filesystems do not provide symbolic links; skip only this provider-specific test.
        }
        Files.createDirectories(f.ledger.getParent());
        Files.writeString(f.ledger, M3RecipeMemory.HEADER + "\n");
        rejects(IOException.class, () -> M3RecipeMemory.read(f.ledger),
                "empty internal record refused");
        Files.writeString(f.ledger, M3RecipeMemory.HEADER + "unrelated\n");
        rejects(IOException.class, () -> M3RecipeMemory.read(f.ledger),
                "wrong field count refused");
        Files.writeString(f.ledger, M3RecipeMemory.HEADER + f.row(t).line().replace("\n", "\r\n"));
        rejects(IOException.class, () -> M3RecipeMemory.read(f.ledger),
                "noncanonical CRLF row refused");
        Files.writeString(f.ledger, M3RecipeMemory.HEADER + f.row(t).line() + f.row(t).line());
        rejects(IOException.class, () -> M3RecipeMemory.read(f.ledger),
                "duplicate correction refused");
    }

    private static void concurrentDuplicate() throws Exception {
        Fixture f = new Fixture();
        var entry = f.row(Instant.parse("2026-10-20T10:00:00Z"));
        CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger(), rejected = new AtomicInteger();
        Runnable action = () -> {
            ready.countDown();
            try {
                start.await();
                M3RecipeMemory.append(f.root, LEDGER, entry, f.receipt);
                success.incrementAndGet();
            } catch (Exception expected) {
                rejected.incrementAndGet();
            }
        };
        Thread first = Thread.ofVirtual().start(action), second = Thread.ofVirtual().start(action);
        ready.await(); start.countDown(); first.join(); second.join();
        check(success.get() == 1 && rejected.get() == 1,
                "concurrent duplicate may be admitted exactly once");
        check(M3RecipeMemory.read(f.ledger).size() == 1, "ledger remains serial and valid");
    }

    public static void main(String[] args) throws Exception {
        appendAndRemember(); proofAndChangeRefusal(); schemaAndPathRefusal(); concurrentDuplicate();
        System.out.println("M3_MOP_RECIPE_MEMORY_PASS checks=" + checks);
    }
}
