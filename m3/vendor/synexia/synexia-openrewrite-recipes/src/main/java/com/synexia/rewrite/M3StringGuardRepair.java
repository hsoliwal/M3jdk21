// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.synexia.rewrite;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Narrow Java/Maven source-sealed task recipe, canonically owned in Synexia.
 * Only this reviewed Python guard preimage and two new proof files are admitted.
 * It is KERNEL_REPLAY, not OpenRewrite SDK execution or JDK runtime proof.
 */
public final class M3StringGuardRepair {
    static final String MANIFEST_SHA = "56bd61be29eae76b3f468519677424450448751d94ca032956b330d7b3dd1999";
    static final String BEFORE = "0eaa95f80d96be8040aa9584b0ddf3eee3d1e2da40d1bb9d5beef11f3a7e61bb";
    static final String STRING_SHA = "b7a074c6f366397847538b6b1bc6a5e5fecfb94809ab70fda40ef58406595b13";
    static final String RECEIPT_BEFORE_SHA = "22cc4f60884a25f57a341b99f3332b92ae3dca4d83f5ef13d34d77904174011d";
    static final String CHECKER = "m3/runtime-integration/check-m3string-invariants.py";
    static final String JAVA_TARGET = "m3/runtime-integration/src/main/java/M3StringReplacementRoutes.java";
    static final String PY_TARGET = "m3/runtime-integration/test_string_replacement_routes.py";
    static final String RECEIPT = "m3/runtime-integration/string-replacement-guard-receipt.tsv";
    static final String JAVA_SHA = "098da0581258b416bbf7546aec9d0f3d2783643c69d122c460fa96d56b7e9475";
    static final String PY_SHA = "1f4fa3977092afd5b44b2df231828497ed954bd3cc777c905550294b9bac7af3";
    record Atom(String before, String after) {}
    private static final List<Atom> ATOMS = List.of(
        new Atom("    \"LOCAL_NATIVE_BYTES.addAndGet(-retainedBytes)\",", "    \"AtomicLong LOCAL_NATIVE_BYTES\",\n    \"final long retainedBytes;\",\n    \"reference.releaseNative()\",\n    \"created.nativePayloadBytes()\",\n]:\n    if fragment not in pool:\n        fail(f\"VM-local M3 atom lifecycle missing: {fragment}\")\nif \"ArrayList<M3StringAtom> values\" in pool:\n    fail(\"VM-local M3 atom pool strongly retains native atoms\")\n\nlocal_release_start = pool.find(\"void releaseNative()\")\nlocal_release_end = pool.find(\"\\n        }\", local_release_start)\nif local_release_start < 0 or local_release_end < 0:\n    fail(\"VM-local M3 atom native release method missing\")\nlocal_release = pool[local_release_start:local_release_end]\nfor fragment in [\n    \"if (!released) {\",\n    \"released = true;\",\n    \"if (address != 0L) UNSAFE.freeMemory(address);\",\n    \"release(retainedBytes);\",\n]:\n    if fragment not in local_release:\n        fail(f\"VM-local M3 atom release path missing: {fragment}\")\nif (local_release.count(\"if (!released) {\") != 1\n        or local_release.count(\"released = true;\") != 1\n        or local_release.count(\"UNSAFE.freeMemory(address)\") != 1\n        or local_release.count(\"release(retainedBytes);\") != 1):\n    fail(\"VM-local M3 atom must free and release native payload exactly once\")\nif not (local_release.find(\"released = true;\")\n        < local_release.find(\"UNSAFE.freeMemory(address)\")\n        < local_release.find(\"release(retainedBytes);\")):\n    fail(\"VM-local M3 atom release order is unsafe\")\n\naccounting_start = pool.find(\"private static void release(long bytes)\")\naccounting_end = pool.find(\"\\n    }\", accounting_start)\nif accounting_start < 0 or accounting_end < 0:\n    fail(\"VM-local M3 native budget release helper missing\")\naccounting = pool[accounting_start:accounting_end]\nif accounting.count(\"LOCAL_NATIVE_BYTES.addAndGet(-bytes);\") != 1:\n    fail(\"VM-local M3 native budget release accounting missing or duplicated\")")
    );
    private M3StringGuardRepair() {}

    public static void main(String[] args) throws IOException {
        if (args.length != 3 || !(args[0].equals("--apply") || args[0].equals("--check"))) {
            throw new IllegalArgumentException("usage: --apply|--check TARGET_ROOT CRATE_ROOT");
        }
        Path root = Path.of(args[1]).toRealPath();
        Path crate = Path.of(args[2]).toRealPath();
        Path module = crate.getParent().getParent();
        requireHash(Files.readString(safe(module,
                "src/main/java/com/synexia/rewrite/M3GpuToolJoinManifest.java")),
                MANIFEST_SHA, "canonical manifest kernel drift");
        requireHash(Files.readString(safe(root, "src/java.base/share/classes/java/lang/String.java")),
                    STRING_SHA, "runtime String source changed; review a new task");
        String current = Files.readString(safe(root, CHECKER));
        String repaired = replay(current, BEFORE, ATOMS);
        String java = Files.readString(safe(crate, "templates/M3StringReplacementRoutes.java"));
        String python = Files.readString(safe(crate, "templates/test_string_replacement_routes.py"));
        requireHash(java, JAVA_SHA, "Java proof template drift");
        requireHash(python, PY_SHA, "Python proof template drift");
        String kernel = Files.readString(safe(module,
                "src/main/java/com/synexia/rewrite/M3StringGuardRepair.java"));
        Map<String, String> desired = new LinkedHashMap<>();
        desired.put(CHECKER, repaired);
        desired.put(JAVA_TARGET, java);
        desired.put(PY_TARGET, python);
        desired.put(RECEIPT, "key\tvalue\n"
                + "canonical_repository\thsoliwal/com.synexia\n"
                + "canonical_crate\tsynexia-openrewrite-recipes/crates/m3-string-replacement-guard\n"
                + "target_preimage_revision\t2554bdea3ae51671d7a7bd1b5f520f2568315061\n"
                + "checker_before_sha256\t" + BEFORE + "\n"
                + "checker_after_sha256\t" + sha256(repaired) + "\n"
                + "runtime_String_unchanged_sha256\t" + STRING_SHA + "\n"
                + "java_proof_sha256\t" + JAVA_SHA + "\n"
                + "python_tests_sha256\t" + PY_SHA + "\n"
                + "recipe_kernel_sha256\t" + sha256(kernel) + "\n"
                + "manifest_kernel_sha256\t" + MANIFEST_SHA + "\n"
                + "execution_plane\tKERNEL_REPLAY\n"
                + "runtime_build_jtreg\tNOT_PROVED_BY_THIS_RECEIPT\n"
                + "promotion\tNOT_AUTHORIZED\n");
        // Build a task-local packet; existing Synexia manifest/scan remains admission authority.
        Map<String, String> resources = new LinkedHashMap<>();
        String prefix = "/m3-string-guard/";
        StringBuilder rows = new StringBuilder();
        int templateIndex = 0;
        for (var entry : desired.entrySet()) {
            String resource = "post-" + templateIndex++ + ".txt";
            resources.put(prefix + resource, entry.getValue());
            String before = entry.getKey().equals(CHECKER) ? BEFORE
                    : entry.getKey().equals(RECEIPT) ? RECEIPT_BEFORE_SHA : "ABSENT";
            rows.append(entry.getKey()).append('\t').append(before).append('\t')
                    .append(sha256(entry.getValue())).append('\t').append(resource).append('\n');
        }
        String runtimePath = "src/java.base/share/classes/java/lang/String.java";
        rows.append(runtimePath).append('\t').append(STRING_SHA).append('\t')
                .append(STRING_SHA).append("\tKEEP\n");
        resources.put(prefix + "manifest.tsv", rows.toString());
        var manifest = M3GpuToolJoinManifest.load(prefix, name -> {
            String value = resources.get(name);
            return value == null ? null : new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
        }, M3GpuToolJoinManifest.NOOP);
        var scan = manifest.scan();
        for (var target : manifest.targets()) {
            Path path = safe(root, target.path());
            if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
                scan.observe(target.path(), sha256(Files.readString(path)));
            }
        }
        scan.requireAdmissible();
        List<String> changes = new ArrayList<>();
        for (var entry : desired.entrySet()) {
            Path path = safe(root, entry.getKey());
            if (scan.missing(entry.getKey())) {
                changes.add(entry.getKey());
            } else {
                String existing = Files.readString(path);
                scan.editing(entry.getKey(), sha256(existing));
                if (!existing.equals(entry.getValue())) changes.add(entry.getKey());
            }
        }
        if (args[0].equals("--check") && !changes.isEmpty()) {
            throw new IllegalArgumentException("recipe not at fixed point: " + changes);
        }
        scan.editing(runtimePath, sha256(Files.readString(safe(root, runtimePath))));
        for (String relative : changes) {
            Path path = safe(root, relative);
            if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
                scan.editing(relative, sha256(Files.readString(path)));
            } else if (!scan.missing(relative)) {
                throw new IllegalStateException("target disappeared after scan: " + relative);
            }
            Files.createDirectories(path.getParent());
            Path temporary = Files.createTempFile(path.getParent(), ".m3-guard-", ".tmp");
            try {
                Files.writeString(temporary, desired.get(relative), StandardCharsets.UTF_8);
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temporary);
            }
        }
        for (var entry : desired.entrySet()) {
            requireHash(Files.readString(safe(root, entry.getKey())), sha256(entry.getValue()), "postimage readback drift");
        }
        System.out.println("M3_STRING_GUARD_REPLAY_PASS changed=" + changes.size()
                + " checker_after_sha256=" + sha256(repaired) + " runtime_changed=false");
    }

    static String replay(String input, String beforeSha, List<Atom> atoms) {
        if (beforeSha == null || !beforeSha.matches("[0-9a-f]{64}") || atoms.isEmpty()) {
            throw new IllegalArgumentException("invalid task seal");
        }
        if (sha256(input).equals(beforeSha)) {
            String result = input;
            for (Atom atom : atoms) result = replaceOnce(result, atom.before(), atom.after());
            return result;
        }
        // A postimage is accepted only if reversing exactly these atoms reconstructs
        // the sealed full preimage, and replay yields exactly the supplied postimage.
        String original = input;
        for (int index = atoms.size() - 1; index >= 0; index--) {
            Atom atom = atoms.get(index);
            original = replaceOnce(original, atom.after(), atom.before());
        }
        requireHash(original, beforeSha, "source drift");
        String reconstructed = original;
        for (Atom atom : atoms) reconstructed = replaceOnce(reconstructed, atom.before(), atom.after());
        if (!reconstructed.equals(input)) throw new IllegalArgumentException("postimage drift");
        return input;
    }

    private static String replaceOnce(String source, String before, String after) {
        if (before.isEmpty() || after.isEmpty() || before.equals(after)) {
            throw new IllegalArgumentException("invalid replacement atom");
        }
        int at = source.indexOf(before);
        if (at < 0 || source.indexOf(before, at + before.length()) >= 0) {
            throw new IllegalArgumentException("missing or duplicated source atom");
        }
        return source.substring(0, at) + after + source.substring(at + before.length());
    }

    private static Path safe(Path root, String relative) throws IOException {
        Path path = root.resolve(relative).normalize();
        if (!path.startsWith(root)) throw new IllegalArgumentException("path outside task root");
        for (Path cursor = path; !cursor.equals(root); cursor = cursor.getParent()) {
            if (Files.isSymbolicLink(cursor)) throw new IllegalArgumentException("symlink refused: " + cursor);
        }
        return path;
    }

    static String sha256(String value) {
        return M3GpuToolJoinManifest.sha256(value);
    }

    private static void requireHash(String value, String expected, String message) {
        if (!sha256(value).equals(expected)) throw new IllegalArgumentException(message);
    }
}
