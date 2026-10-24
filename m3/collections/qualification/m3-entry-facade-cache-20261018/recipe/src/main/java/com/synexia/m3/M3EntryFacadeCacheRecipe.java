/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package com.synexia.m3;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/**
 * Source-sealed receiver recipe for array-backed, cached public Map.Entry facades.
 *
 * <p>The production transformations are exact, named source atoms with Git-blob SHA-1 pre/post
 * seals. Inputs are all preflighted before any write. A second apply is a fixed point. The cache
 * itself owns primitive/reference array lanes; its facade values are bounded API objects, not
 * mapping storage. Weak-key maps explicitly bypass the strong facade cache.</p>
 */
public final class M3EntryFacadeCacheRecipe {
    private static final String BASE =
            "m3/collections/src/main/java/com/m3/collections/";
    private static final String TEST =
            "m3/collections/src/test/java/com/m3/collections/";

    private record SourceAtom(String name, String target, String beforeSha, String afterSha) { }
    private record Plan(Path target, byte[] output, boolean write) { }

    private static final List<SourceAtom> ATOMS = List.of(
        new SourceAtom("slot-map", BASE + "M3SlotMap.java",
            "a7d20300717661c26897f0b69e1e5a83400a62ad",
            "5f31da47a35009b8e2e4a44f9817fa6e2b8ffdc1"),
        new SourceAtom("ordered-slot-map", BASE + "M3OrderedSlotMap.java",
            "e7be2297d19f22cf4dd4ef8b7d1424fe963063d9",
            "4d5ed2b1494e3f252e92ca98b4b84072b2c17dfe"),
        new SourceAtom("linked-hash-map", BASE + "M3LinkedHashMap.java",
            "ade59880b8c9b2bee1b32ae67ca9d29b7382d998",
            "a294b998fcf73ff4cde94f912d2998c91c2a9a0f"),
        new SourceAtom("tree-map", BASE + "M3TreeMap.java",
            "8a85b86c027bc031ef096ad94de2747b8fd28f9c",
            "9ac4b66f4e1618281f3c88077cf816a69062fdef"),
        new SourceAtom("weak-hash-map", BASE + "M3WeakHashMap.java",
            "5d7eb065aeca2cecc1d939532d154c87bbb01c2c", 
            "589eff9e622c1d532c6c043ac5a107723231156c"));
    private static final List<SourceAtom> NEW_FILES = List.of(
        new SourceAtom("lfu-cache", BASE + "M3LFUCache.java", "MISSING",
            "dc1dc5167efb5ed859c1aa3c41a07d6a5332c4cb"),
        new SourceAtom("entry-facade-tests", TEST + "M3EntryFacadeCacheTest.java", "MISSING",
            "cbf4e91fd5d6b517d410df794022972052627493"));

    private M3EntryFacadeCacheRecipe() { }

    public static void main(String[] args) throws Exception {
        if (args.length == 1 && "--selftest".equals(args[0])) {
            selfTest();
            return;
        }
        if (args.length != 2 || !("--apply".equals(args[0]) || "--check".equals(args[0]))) {
            throw new IllegalArgumentException(
                    "Usage: --selftest | --apply|--check <M3JDK21-root>");
        }
        run(Path.of(args[1]), "--apply".equals(args[0]));
    }

    public static void run(Path repository, boolean apply) throws IOException {
        Path root = repository.toAbsolutePath().normalize();
        if (!Files.isDirectory(root.resolve("m3/collections"))) {
            throw new IOException("REFUSE_NOT_COLLECTIONS_RECEIVER");
        }
        List<Plan> plans = new ArrayList<>();
        // Complete preflight first: a drifted file must never leave a partial multi-file rewrite.
        for (SourceAtom atom : ATOMS) {
            Path target = safeTarget(root, atom.target());
            if (!Files.isRegularFile(target)) {
                throw new IOException("REFUSE_MISSING_SOURCE:" + atom.name());
            }
            byte[] current = Files.readAllBytes(target);
            String currentSha = gitBlobSha(current);
            if (atom.afterSha().equals(currentSha)) {
                plans.add(new Plan(target, current, false));
            } else if (atom.beforeSha().equals(currentSha)) {
                if (!apply) {
                    throw new IOException("REFUSE_UNAPPLIED_SOURCE:" + atom.name());
                }
                byte[] output = transform(atom.name(), current);
                requireSha(atom.name(), output, atom.afterSha());
                plans.add(new Plan(target, output, true));
            } else {
                throw new IOException("REFUSE_SOURCE_DRIFT:" + atom.name() + ":" + currentSha);
            }
        }
        for (SourceAtom atom : NEW_FILES) {
            Path target = safeTarget(root, atom.target());
            byte[] template = resource("/after/" + leaf(atom.target()) + ".txt");
            requireSha(atom.name(), template, atom.afterSha());
            if (Files.exists(target)) {
                if (!Files.isRegularFile(target)) {
                    throw new IOException("REFUSE_NON_FILE_OUTPUT:" + atom.name());
                }
                byte[] current = Files.readAllBytes(target);
                requireShaOrDrift(atom.name(), current, atom.afterSha());
                plans.add(new Plan(target, current, false));
            } else {
                if (!apply) {
                    throw new IOException("REFUSE_MISSING_OUTPUT:" + atom.name());
                }
                plans.add(new Plan(target, template, true));
            }
        }
        if (apply) {
            for (Plan plan : plans) {
                if (!plan.write()) {
                    continue;
                }
                Files.createDirectories(plan.target().getParent());
                Path tmp = Files.createTempFile(plan.target().getParent(), ".m3-entry-facade-", ".tmp");
                try {
                    Files.write(tmp, plan.output());
                    Files.move(tmp, plan.target(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } finally {
                    Files.deleteIfExists(tmp);
                }
            }
        }
        System.out.println("PASS M3EntryFacadeCacheRecipe mode="
                + (apply ? "apply" : "check") + " verified=" + plans.size());
    }

    private static byte[] transform(String name, byte[] input) throws IOException {
        String source = new String(input, StandardCharsets.UTF_8);
        String output = switch (name) {
            case "slot-map" -> slotMap(source);
            case "ordered-slot-map" -> orderedSlotMap(source);
            case "linked-hash-map" -> linkedHashMap(source);
            case "tree-map" -> treeMap(source);
            case "weak-hash-map" -> weakHashMap(source);
            default -> throw new IOException("REFUSE_UNKNOWN_ATOM:" + name);
        };
        return output.getBytes(StandardCharsets.UTF_8);
    }

    private static String slotMap(String source) throws IOException {
        source = replaceExactly(source,
            "/** Shared JDK semantics over integer slots; internal traversal never constructs entries. */",
            "/**\n * Shared JDK semantics over integer slots. Internal operations use slots and arrays;\n"
            + " * public entry views alone request live facades, bounded and reused through M3LFUCache.\n"
            + " */");
        source = replaceExactly(source,
            "abstract class M3SlotMap<K, V> extends AbstractMap<K, V> {\n",
            "abstract class M3SlotMap<K, V> extends AbstractMap<K, V> {\n"
            + "    private static final int ENTRY_FACADE_CACHE_CAPACITY = 64;\n"
            + "    private M3LFUCache<K, LiveEntry> entryFacades;\n");
        source = replaceExactly(source,
            "    final void check(int expected) {\n"
            + "        if (revision() != expected) { throw new ConcurrentModificationException(); }\n"
            + "    }\n",
            "    final void check(int expected) {\n"
            + "        if (revision() != expected) { throw new ConcurrentModificationException(); }\n"
            + "    }\n"
            + "    /** Weak-key owners opt out so facade caching cannot keep keys reachable. */\n"
            + "    protected boolean cacheEntryFacades() { return true; }\n"
            + "    final Entry<K, V> entryFacade(K key) {\n"
            + "        if (!cacheEntryFacades()) { return new LiveEntry(key); }\n"
            + "        M3LFUCache<K, LiveEntry> cache = entryFacades;\n"
            + "        if (cache == null) {\n"
            + "            cache = new M3LFUCache<>(ENTRY_FACADE_CACHE_CAPACITY);\n"
            + "            entryFacades = cache;\n"
            + "        }\n"
            + "        return cache.getOrCreate(key, candidate -> new LiveEntry(candidate));\n"
            + "    }\n"
            + "    final void forgetEntryFacade(K key) {\n"
            + "        M3LFUCache<K, LiveEntry> cache = entryFacades;\n"
            + "        if (cache != null) { cache.removeIdentity(key); }\n"
            + "    }\n"
            + "    final void clearEntryFacades() {\n"
            + "        M3LFUCache<K, LiveEntry> cache = entryFacades;\n"
            + "        if (cache != null) {\n"
            + "            cache.clear();\n"
            + "            entryFacades = null;\n"
            + "        }\n"
            + "    }\n");
        return replaceExactly(source, "new LiveEntry(heldKey)", "entryFacade(heldKey)");
    }

    private static String orderedSlotMap(String source) throws IOException {
        return replaceExactly(source,
            "remove ? new SimpleEntry<>(keyAt(s), valueAt(s)) : new LiveEntry(keyAt(s))",
            "remove ? new SimpleEntry<>(keyAt(s), valueAt(s)) : entryFacade(keyAt(s))");
    }

    private static String linkedHashMap(String source) throws IOException {
        source = replaceExactly(source,
            "@Override V eraseAt(int slot) { V old = valueAt(slot); table.erase(slot); return old; }",
            "@Override V eraseAt(int slot) { V old = valueAt(slot); forgetEntryFacade(keyAt(slot)); table.erase(slot); return old; }");
        return replaceExactly(source,
            "@Override public void clear() { table.clear(); }",
            "@Override public void clear() { table.clear(); clearEntryFacades(); }");
    }

    private static String treeMap(String source) throws IOException {
        source = replaceExactly(source,
            "@Override V eraseAt(int slot) { tree.writable(); V old = valueAt(slot); tree.erase(slot); return old; }",
            "@Override V eraseAt(int slot) { tree.writable(); V old = valueAt(slot); forgetEntryFacade(keyAt(slot)); tree.erase(slot); return old; }");
        return replaceExactly(source,
            "    @Override public void clear() {\n        tree.writable();",
            "    @Override public void clear() {\n        tree.writable();\n        clearEntryFacades();");
    }

    private static String weakHashMap(String source) throws IOException {
        source = replaceExactly(source,
            "public final class M3WeakHashMap<K, V> extends M3SlotMap<K, V> {",
            "public final class M3WeakHashMap<K, V> extends M3SlotMap<K, V> {\n"
            + "    @Override protected boolean cacheEntryFacades() { return false; }");
        return replaceExactly(source,
            "@Override V eraseAt(int s) { return erase(s, true); }",
            "@Override V eraseAt(int s) { forgetEntryFacade(keyAt(s)); return erase(s, true); }");
    }

    private static String replaceExactly(String source, String before, String after) throws IOException {
        int first = source.indexOf(before);
        if (first < 0 || source.indexOf(before, first + before.length()) >= 0) {
            throw new IOException("REFUSE_ATOM_PRECONDITION:" + before);
        }
        return source.substring(0, first) + after + source.substring(first + before.length());
    }

    private static Path safeTarget(Path root, String relative) throws IOException {
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root)) {
            throw new IOException("REFUSE_ESCAPING_TARGET:" + relative);
        }
        for (Path cursor = target; !cursor.equals(root); cursor = cursor.getParent()) {
            if (Files.isSymbolicLink(cursor)) {
                throw new IOException("REFUSE_SYMLINK:" + cursor);
            }
        }
        return target;
    }

    private static String leaf(String path) {
        return path.substring(path.lastIndexOf('/') + 1);
    }

    private static byte[] resource(String name) throws IOException {
        try (InputStream input = M3EntryFacadeCacheRecipe.class.getResourceAsStream(name)) {
            if (input == null) {
                throw new IOException("REFUSE_MISSING_TEMPLATE:" + name);
            }
            return input.readAllBytes();
        }
    }

    private static void requireSha(String name, byte[] bytes, String expected) throws IOException {
        String actual = gitBlobSha(bytes);
        if (!expected.equals(actual)) {
            throw new IOException("REFUSE_POSTIMAGE_SHA:" + name + ":" + actual);
        }
    }

    private static void requireShaOrDrift(String name, byte[] bytes, String expected) throws IOException {
        String actual = gitBlobSha(bytes);
        if (!expected.equals(actual)) {
            throw new IOException("REFUSE_OUTPUT_DRIFT:" + name + ":" + actual);
        }
    }

    private static String gitBlobSha(byte[] bytes) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(("blob " + bytes.length + "\0").getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException unavailable) {
            throw new IOException("SHA1_UNAVAILABLE", unavailable);
        }
    }

    private static void selfTest() throws IOException {
        // The exact frozen preimages are the fixtures used to prove every source transformation.
        for (SourceAtom atom : ATOMS) {
            byte[] pre = resource("/before/" + leaf(atom.target()) + ".txt");
            requireSha(atom.name() + "-preimage", pre, atom.beforeSha());
            requireSha(atom.name() + "-postimage", transform(atom.name(), pre), atom.afterSha());
        }

        Path root = Files.createTempDirectory("m3-entry-facade-recipe-").toAbsolutePath().normalize();
        try {
            Files.createDirectories(root.resolve("m3/collections"));
            for (SourceAtom atom : ATOMS) {
                Path file = root.resolve(atom.target());
                Files.createDirectories(file.getParent());
                Files.write(file, resource("/before/" + leaf(atom.target()) + ".txt"));
            }
            try {
                run(root, false);
                throw new AssertionError("check should refuse preimage inputs and missing outputs");
            } catch (IOException expected) {
                if (!expected.getMessage().startsWith("REFUSE_UNAPPLIED_SOURCE")) {
                    throw expected;
                }
            }

            // Drift one late output while an earlier source still requires transformation.
            Path first = root.resolve(ATOMS.get(0).target());
            Path late = root.resolve(NEW_FILES.get(1).target());
            Files.createDirectories(late.getParent());
            Files.writeString(late, "drift must prevent every write");
            try {
                run(root, true);
                throw new AssertionError("expected all-target preflight refusal");
            } catch (IOException expected) {
                if (!expected.getMessage().startsWith("REFUSE_OUTPUT_DRIFT")) {
                    throw expected;
                }
            }
            requireSha("no-partial-write", Files.readAllBytes(first), ATOMS.get(0).beforeSha());

            Files.delete(late);
            run(root, true);
            run(root, false);
            run(root, true); // second application must be an unchanged fixed point

            for (SourceAtom atom : ATOMS) {
                requireSha(atom.name(), Files.readAllBytes(root.resolve(atom.target())), atom.afterSha());
            }
            for (SourceAtom atom : NEW_FILES) {
                requireSha(atom.name(), Files.readAllBytes(root.resolve(atom.target())), atom.afterSha());
            }
            // Modified targets are never overwritten, even by a repeated apply.
            Files.writeString(late, "manual edit");
            try {
                run(root, true);
                throw new AssertionError("expected modified generated-source refusal");
            } catch (IOException expected) {
                if (!expected.getMessage().startsWith("REFUSE_OUTPUT_DRIFT")) {
                    throw expected;
                }
            }
            System.out.println("PASS M3EntryFacadeCacheRecipe selftest "
                    + "exact-postimages all-preflight no-partial-write drift fixed-point");
        } finally {
            try (var files = Files.walk(root)) {
                for (Path path : files.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }
}
