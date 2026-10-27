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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Source-sealed, serial M3 SDK contract-atom correction.
 * The fixed inputs and output Git blob IDs are part of the admission contract.
 * No donor implementation, payload-array owner or JNI symbol is introduced.
 */
public final class M3SdkSortedContractRecipe {
    private record Atom(String path, String beforeBlob, String afterBlob,
                        UnaryOperator<String> transform) { }
    private record Write(Path path, String content) { }

    private static final String MAIN = "m3/collections/src/main/java/com/m3/collections/";
    private static final String TEST = "m3/collections/src/test/java/com/m3/collections/";

    private static final List<Atom> ATOMS = List.of(
        new Atom(MAIN + "M3CollectionSdk.java",
                "042069cd167191e1faa1710fb9a9e65bca14983c",
                "0962823301c3f80dbe4956600fe572af2067439b",
                M3SdkSortedContractRecipe::sdk),
        new Atom(TEST + "M3CollectionSdkSelfTest.java",
                "a62b63a3399d926dabe6645f342381d2a28107ca",
                "8d01cb9fbefb76f65a70759d93153cfc20a20008",
                M3SdkSortedContractRecipe::oracle)
    );

    private M3SdkSortedContractRecipe() { }

    public static void main(String[] args) throws Exception {
        if (args.length == 1 && "--selftest".equals(args[0])) {
            selfTest();
            return;
        }
        if (args.length != 2 || !("--apply".equals(args[0]) || "--check".equals(args[0]))) {
            throw new IllegalArgumentException("Usage: --selftest | --apply|--check <receiver-root>");
        }
        run(Path.of(args[1]), "--apply".equals(args[0]), ATOMS);
        System.out.println("PASS M3SdkSortedContractRecipe " + args[0] + " atoms=" + ATOMS.size());
    }

    private static String lines(String... lines) {
        return String.join("\n", lines) + "\n";
    }

    private static String once(String source, String before, String after) {
        int first = source.indexOf(before);
        if (first < 0 || source.indexOf(before, first + before.length()) >= 0) {
            throw new IllegalStateException("REFUSE_MISSING_OR_REPEATED_ATOM: " + before);
        }
        return source.substring(0, first) + after + source.substring(first + before.length());
    }

    private static String sdk(String source) {
        String result = once(source, "import java.util.Set;\n",
                "import java.util.Set;\nimport java.util.SortedMap;\nimport java.util.SortedSet;\n");
        String methods = lines(
            "    /** Live, read-only, comparator-preserving SortedSet over the original storage. */",
            "    public static <E> SortedSet<E> readOnlyFilteredSortedSet(",
            "            SortedSet<E> source, Predicate<? super E> predicate) {",
            "        return M3FilteredViews.filter(source, predicate);",
            "    }",
            "",
            "    /** Live, read-only, comparator-preserving SortedMap with immutable entries. */",
            "    public static <K, V> SortedMap<K, V> readOnlyFilteredSortedMap(",
            "            SortedMap<K, V> source, BiPredicate<? super K, ? super V> predicate) {",
            "        return M3FilteredViews.filterEntries(source, predicate);",
            "    }",
            ""
        );
        return once(result, "    /** Live NavigableSet wrapper",
                methods + "    /** Live NavigableSet wrapper");
    }

    private static String oracle(String source) {
        String result = once(source, "import java.util.Random;\n",
                "import java.util.Random;\nimport java.util.SortedMap;\nimport java.util.SortedSet;\n");
        String tests = lines(
            "        SortedSet<Integer> simpleSorted = new TreeSet<>(List.of(1, 2, 3, 4, 5, 6));",
            "        SortedSet<Integer> simpleView = M3CollectionSdk.readOnlyFilteredSortedSet(",
            "                simpleSorted, x -> (x & 1) == 0);",
            "        check(simpleView.equals(new TreeSet<>(List.of(2, 4, 6))), \"SortedSet visibility\");",
            "        check(simpleView.first() == 2 && simpleView.last() == 6, \"SortedSet navigation\");",
            "        check(simpleView.subSet(2, 6).equals(new TreeSet<>(List.of(2, 4))),",
            "                \"SortedSet bounded projection\");",
            "        simpleSorted.add(8);",
            "        check(simpleView.last() == 8, \"SortedSet live updates\");",
            "        expect(UnsupportedOperationException.class, () -> simpleView.add(10));",
            "        expect(UnsupportedOperationException.class, () -> simpleView.subSet(2, 8).clear());",
            "        SortedMap<Integer, Integer> simpleSortedMap =",
            "                new TreeMap<>(Map.of(1, 10, 2, 20, 3, 30));",
            "        SortedMap<Integer, Integer> simpleMapView =",
            "                M3CollectionSdk.readOnlyFilteredSortedMap(simpleSortedMap,",
            "                        (k, v) -> k % 2 == 0 && v >= 20);",
            "        check(simpleMapView.size() == 1 && simpleMapView.firstKey() == 2,",
            "                \"SortedMap entry visibility\");",
            "        check(simpleMapView.get(1) == null && !simpleMapView.containsKey(1),",
            "                \"SortedMap hidden keys\");",
            "        simpleSortedMap.put(4, 40);",
            "        check(simpleMapView.lastKey() == 4 && simpleMapView.size() == 2,",
            "                \"SortedMap live updates\");",
            "        expect(UnsupportedOperationException.class, () -> simpleMapView.put(6, 60));",
            "        expect(UnsupportedOperationException.class, () ->",
            "                simpleMapView.entrySet().iterator().next().setValue(0));",
            "        expect(UnsupportedOperationException.class, () ->",
            "                simpleMapView.headMap(4).clear());"
        );
        return once(result, "        Random r = new Random(0x3efaac71L);",
                tests + "        Random r = new Random(0x3efaac71L);");
    }

    private static void run(Path inputRoot, boolean apply, List<Atom> atoms) throws IOException {
        Path root = inputRoot.toAbsolutePath().normalize();
        if (!Files.isDirectory(root.resolve("m3/collections"))) {
            throw new IOException("REFUSE_NOT_M3JDK21");
        }
        List<Write> staged = new ArrayList<>();
        for (Atom atom : atoms) {
            Path dest = root.resolve(atom.path()).normalize();
            if (!dest.startsWith(root)) throw new IOException("REFUSE_ESCAPE");
            for (Path walk = dest; !walk.equals(root); walk = walk.getParent()) {
                if (Files.isSymbolicLink(walk)) throw new IOException("REFUSE_SYMLINK");
            }
            if (!Files.isRegularFile(dest)) throw new IOException("REFUSE_MISSING: " + atom.path());
            String existing = Files.readString(dest, StandardCharsets.UTF_8);
            String blob = blobId(existing);
            if (blob.equals(atom.afterBlob())) continue;
            if (!apply || !blob.equals(atom.beforeBlob())) {
                throw new IOException("REFUSE_DRIFT: " + atom.path() + ": " + blob);
            }
            final String revised;
            try { revised = atom.transform().apply(existing); }
            catch (RuntimeException e) { throw new IOException("REFUSE_ATOM: " + atom.path(), e); }
            if (!atom.afterBlob().equals(blobId(revised))) {
                throw new IOException("REFUSE_UNQUALIFIED_POSTIMAGE: " + atom.path());
            }
            staged.add(new Write(dest, revised));
        }
        // All postimages and filenames are validated before any target is modified.
        if (apply) {
            for (Write write : staged) {
                Path temp = Files.createTempFile(write.path().getParent(), ".m3-recipe-", ".tmp");
                try {
                    Files.writeString(temp, write.content(), StandardCharsets.UTF_8);
                    Files.move(temp, write.path(), StandardCopyOption.ATOMIC_MOVE,
                            StandardCopyOption.REPLACE_EXISTING);
                } finally { Files.deleteIfExists(temp); }
            }
        }
    }

    private static String blobId(String text) throws IOException {
        byte[] body = text.getBytes(StandardCharsets.UTF_8);
        byte[] header = ("blob " + body.length + "\0").getBytes(StandardCharsets.US_ASCII);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(header);
            return HexFormat.of().formatHex(digest.digest(body));
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("REFUSE_GIT_HASH_UNAVAILABLE", e);
        }
    }

    private static void selfTest() throws Exception {
        Path root = Files.createTempDirectory("m3-sdk-sorted-recipe-");
        Path file = root.resolve(MAIN + "SelfTestFixture.java");
        try {
            Files.createDirectories(file.getParent());
            String before = "start\n";
            String after = "done\n";
            Atom fixture = new Atom(MAIN + "SelfTestFixture.java",
                    blobId(before), blobId(after), s -> once(s, "start", "done"));
            List<Atom> fixtures = List.of(fixture);
            try {
                run(root, true, fixtures);
                throw new AssertionError("missing target admitted");
            } catch (IOException expected) {
                if (!expected.getMessage().startsWith("REFUSE_MISSING")) throw expected;
            }
            Files.writeString(file, before);
            try {
                run(root, false, fixtures);
                throw new AssertionError("preimage admitted as postimage");
            } catch (IOException expected) {
                if (!expected.getMessage().startsWith("REFUSE_DRIFT")) throw expected;
            }
            run(root, true, fixtures);
            run(root, true, fixtures);
            run(root, false, fixtures);
            if (!Files.readString(file).equals(after)) throw new AssertionError("not idempotent");
            Files.writeString(file, "manual drift");
            try {
                run(root, true, fixtures);
                throw new AssertionError("unknown drift overwritten");
            } catch (IOException expected) {
                if (!expected.getMessage().startsWith("REFUSE_DRIFT")) throw expected;
            }
            if (!Files.readString(file).equals("manual drift")) throw new AssertionError("drift changed");
            try {
                once("aa", "a", "b");
                throw new AssertionError("ambiguous atom accepted");
            } catch (IllegalStateException expected) {
                // Correct refusal.
            }
            System.out.println("PASS M3SdkSortedContractRecipe selftest fixed-point/missing/drift");
        } finally {
            // Synthetic fixture root is isolated; remove files after proof.
            Files.deleteIfExists(root.resolve(MAIN + "SelfTestFixture.java"));
            for (Path p = root.resolve(MAIN + "SelfTestFixture.java").getParent(); !p.equals(root); p = p.getParent()) {
                Files.deleteIfExists(p);
            }
            Files.deleteIfExists(root);
        }
    }
}
