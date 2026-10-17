// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CancellationException;

/** Qualification harness, not an alternative product recipe engine. Emits only to a fresh directory. */
public final class NativeIndexRangeReplay {
    private static final String ROOT = "/com/synexia/rewrite/native-index-range-20261017/";
    private static int checks;
    private static void check(boolean value) { checks++; if (!value) throw new AssertionError("replay check " + checks); }
    private static void refuses(Runnable action) {
        try { action.run(); } catch (IllegalStateException expected) { checks++; return; }
        throw new AssertionError("expected refusal");
    }
    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("repository-root fresh-candidate-directory");
        Path repo = Path.of(args[0]).toRealPath();
        Path output = Path.of(args[1]).toAbsolutePath().normalize();
        if (Files.exists(output)) throw new IllegalStateException("candidate directory must not exist");
        Path resources = repo.resolve("m3/collections/recipes/native-index-range-20261017/src/main/resources");
        M3GpuToolJoinManifest.Resources loader = name -> Files.newInputStream(resources.resolve(name.substring(1)));
        var manifest = M3GpuToolJoinManifest.load(ROOT, loader, M3GpuToolJoinManifest.NOOP);
        var scan = manifest.scan();
        Map<String, String> observed = new LinkedHashMap<>(), finalHashes = new LinkedHashMap<>();
        for (var target : manifest.targets()) {
            Path source = repo.resolve(target.path());
            if (Files.exists(source)) {
                String hash = M3GpuToolJoinManifest.sha256(Files.readString(source));
                observed.put(target.path(), hash); scan.observe(target.path(), hash);
            }
            finalHashes.put(target.path(), target.after());
        }
        scan.requireAdmissible();
        int additions = 0;
        // Validate the complete source and template batch before emitting any candidate.
        for (var target : manifest.targets()) {
            if (target.text() != null) {
                check(M3GpuToolJoinManifest.sha256(target.text()).equals(target.after()));
                additions++;
            }
        }
        for (int permutation = 0; permutation < 32; permutation++) {
            var order = new ArrayList<>(finalHashes.entrySet());
            Collections.shuffle(order, new Random(permutation));
            var replay = manifest.scan();
            for (var entry : order) replay.observe(entry.getKey(), entry.getValue());
            replay.requireAdmissible();
            for (var target : manifest.targets()) {
                check(!replay.missing(target.path()));
                check(replay.editing(target.path(), target.after()).equals(target));
            }
        }
        var owner = manifest.targets().stream().filter(t -> !t.addition()).findFirst().orElseThrow();
        var added = manifest.targets().stream().filter(t -> t.addition()).findFirst().orElseThrow();
        refuses(() -> { var x = manifest.scan(); x.requireAdmissible(); });
        refuses(() -> { var x = manifest.scan(); finalHashes.forEach(x::observe); x.observe(owner.path(), owner.after()); x.requireAdmissible(); });
        refuses(() -> { var x = manifest.scan(); finalHashes.forEach((p,h) -> x.observe(p, p.equals(owner.path()) ? "0".repeat(64) : h)); x.requireAdmissible(); });
        refuses(() -> { var x = manifest.scan(); finalHashes.forEach(x::observe); x.requireAdmissible(); x.editing(added.path(), "0".repeat(64)); });
        refuses(() -> M3GpuToolJoinManifest.load(ROOT, loader, () -> { throw new CancellationException(); }));
        refuses(() -> M3GpuToolJoinManifest.load(ROOT, name -> name.endsWith("manifest.tsv") ? loader.open(name) : new ByteArrayInputStream("corrupt".getBytes(StandardCharsets.UTF_8)), () -> {}));
        refuses(() -> M3GpuToolJoinManifest.load(ROOT, name -> null, () -> {}));
        Files.createDirectories(output);
        StringBuilder receipts = new StringBuilder("target\tsha256\n");
        for (var target : manifest.targets()) if (target.text() != null) {
            Path destination = output.resolve(target.path());
            Files.createDirectories(destination.getParent());
            Files.writeString(destination, target.text(), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            receipts.append(target.path()).append('\t').append(target.after()).append('\n');
        }
        Files.writeString(output.resolve("GENERATED_SHA256.tsv"), receipts, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        System.out.println("NativeIndexRangeReplay checks=" + checks + " generated=" + additions + " fixedPointOrderings=32");
    }
}
