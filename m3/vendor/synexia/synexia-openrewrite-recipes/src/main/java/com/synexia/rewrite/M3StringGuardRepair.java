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
    static final String BEFORE = "7e74fa0660657e0d08799d6ef72f635a2f08f3e4a4bf7b6d907b45ec3c09a8d6";
    static final String STRING_SHA = "eed88205feda9f64b03515603e5807e3f83961ddeb99dae4ef84bd2034d6f568";
    static final String RECEIPT_BEFORE_SHA = "afcf8bfed80d74640c16300cc474f2596dc8e1b41717b08a0474499222a20be6";
    static final String CHECKER = "m3/runtime-integration/check-m3string-invariants.py";
    static final String JAVA_TARGET = "m3/runtime-integration/src/main/java/M3StringReplacementRoutes.java";
    static final String PY_TARGET = "m3/runtime-integration/test_string_replacement_routes.py";
    static final String RECEIPT = "m3/runtime-integration/string-replacement-guard-receipt.tsv";
    static final String JAVA_SHA = "098da0581258b416bbf7546aec9d0f3d2783643c69d122c460fa96d56b7e9475";
    static final String PY_SHA = "1f4fa3977092afd5b44b2df231828497ed954bd3cc777c905550294b9bac7af3";
    record Atom(String before, String after) {}
    private static final List<Atom> ATOMS = List.of(
        new Atom("\"replace(CharSequence,CharSequence)\": \"M3String replaced = sourceM3.replace(targetM3, replacementM3);\",", "\"replace(CharSequence,CharSequence)\": \"String replaced = m3Replace(storage, trgtStr, targetM3, replStr, replacementM3);\","),
        new Atom("\"replaceFirst(String,String)\": \"return storage.replaceLiteralRegex(this, regex, replacement, true);\",", "\"replaceFirst(String,String)\": \"String replaced = storage.replaceLiteralRegex(this, regex, replacement, true);\","),
        new Atom("\"replaceAll(String,String)\": \"return storage.replaceLiteralRegex(this, regex, replacement, false);\",", "\"replaceAll(String,String)\": \"String replaced = storage.replaceLiteralRegex(this, regex, replacement, false);\","),
        new Atom("\"return new String(storage.replaceEmptyTarget(replacementM3));\",", "\"return new String(sourceM3.replaceEmptyTarget(replacementM3));\","),
        new Atom("\ncheck_m3string_layout()\n", "\ncheck_m3string_layout()\n\n# Compiler-parsed replacement routes supplement the cheap spelling markers below.\n# This proves source structure only; runtime/jtreg remain semantic authority.\ndef check_m3string_replacement_routes() -> None:\n    java = str(Path(os.environ[\"JAVA_HOME\"]) / \"bin/java\") if os.environ.get(\"JAVA_HOME\") else \"java\"\n    command = [java, \"--source\", \"21\",\n               str(ROOT / \"m3/runtime-integration/src/main/java/M3StringReplacementRoutes.java\"),\n               str(ROOT / \"src/java.base/share/classes/java/lang/String.java\")]\n    try:\n        result = subprocess.run(command, cwd=ROOT, capture_output=True, text=True, timeout=45)\n    except (OSError, subprocess.TimeoutExpired) as error:\n        fail(f\"M3 replacement route compiler unavailable or timed out: {error}\")\n    if result.returncode != 0 or result.stdout.strip() != \"M3_STRING_REPLACEMENT_ROUTES_OK\":\n        fail(f\"M3 replacement route check failed: {result.stderr.strip()} {result.stdout.strip()}\")\n\n\ncheck_m3string_replacement_routes()\n")
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
