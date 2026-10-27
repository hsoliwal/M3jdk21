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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Exact-root, fail-closed census proof for source-first M3 five-family collection work.
 *
 * <p>Every row is a Git blob and a candidate only, never an admitted source
 * transformation. No donor Java source, protected java.base, runtime API or
 * JNI method is changed. This verifier remains separate from the existing
 * OpenRewrite ChangePackage transformer.
 */
public final class M3FiveProviderInventoryProof {
    private static final String HEADER =
            "provider\trepository\tpinned_commit\troot_git_tree_sha1\tpath"
            + "\tgit_blob_sha1\trole\tsemantic_category\tm3_backend_candidate"
            + "\tdisposition\tlicense";
    private static final Map<String, Integer> EXPECTED = Map.of(
            "GUAVA", 450, "ECLIPSE", 1516, "APACHE", 678,
            "SPRING", 192, "JDK", 378);

    private M3FiveProviderInventoryProof() { }

    /** Validate a complete five-provider inventory; never write input files. */
    public static int verify(Path directory) throws IOException {
        if (directory == null) throw new NullPointerException("directory");
        int total = 0;
        Set<String> identities = new HashSet<>();
        Map<String, Integer> categoryCounts = new HashMap<>();
        for (Map.Entry<String, Integer> required : EXPECTED.entrySet()) {
            String provider = required.getKey();
            Path file = directory.resolve(provider + "_INVENTORY.tsv");
            if (Files.isSymbolicLink(file) || !Files.isRegularFile(file)) {
                throw new IOException("missing or linked census: " + file);
            }
            var lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            if (lines.isEmpty() || !HEADER.equals(lines.get(0))) {
                throw new IOException("invalid census schema: " + provider);
            }
            if (lines.size() != required.getValue() + 1) {
                throw new IOException("incomplete census: " + provider);
            }
            String prior = null;
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i);
                String[] cell = line.split("\t", -1);
                if (cell.length != 11 || !provider.equals(cell[0])) {
                    throw new IOException("incorrect provider or field count: " + provider + ":" + i);
                }
                if (prior != null && prior.compareTo(line) >= 0) {
                    throw new IOException("duplicate or unordered census: " + provider + ":" + i);
                }
                prior = line;
                if (!sha(cell[2]) || !sha(cell[3]) || !sha(cell[5])) {
                    throw new IOException("unsealed Git identity: " + provider + ":" + i);
                }
                if (!cell[1].contains("/")
                        || !cell[4].endsWith(".java")
                        || cell[4].startsWith("/")
                        || cell[4].contains("\\")
                        || cell[4].contains("../")
                        || cell[4].contains("/./")
                        || cell[4].contains("\t")
                        || cell[7].isBlank()
                        || cell[10].isBlank()) {
                    throw new IOException("invalid Java path, category or rights: " + provider + ":" + i);
                }
                if (!identities.add(cell[1] + ":" + cell[4])) {
                    throw new IOException("duplicate repository:path: " + cell[4]);
                }
                if (!cell[8].equals("-") && !cell[8].startsWith("M3")) {
                    throw new IOException("unexpected unverified M3 backend: " + cell[8]);
                }
                if (!allowedDisposition(cell[9])) {
                    throw new IOException("unreviewed status: " + cell[9]);
                }
                if (provider.equals("SPRING") && !cell[1].equals("spring-projects/spring-framework")) {
                    throw new IOException("Spring upstream source authority changed");
                }
                if (provider.equals("JDK") && !cell[4].startsWith(
                        "src/java.base/share/classes/java/util/")) {
                    throw new IOException("outside locked JDK collection tree");
                }
                String role = cell[6];
                if (role.contains("TEST") && !cell[7].equals("TEST_ORACLE")) {
                    throw new IOException("test oracle accidentally classified as production");
                }
                if (role.equals("GENERATOR") && !cell[7].equals("GENERATOR_SOURCE")) {
                    throw new IOException("generator source improperly admitted");
                }
                categoryCounts.merge(cell[7], 1, Integer::sum);
                total++;
            }
        }
        if (total != 3214
                || categoryCounts.getOrDefault("TEST_ORACLE", 0) != 1213) {
            throw new IOException("incorrect full census cardinality");
        }
        return total;
    }

    private static boolean sha(String value) {
        return value.matches("[0-9a-f]{40}");
    }

    private static boolean allowedDisposition(String value) {
        return switch (value) {
            case "RECIPE_CANDIDATE_NOT_APPLIED",
                 "RETAIN_UNMODIFIED_ORACLE",
                 "PUBLIC_CONTRACT_LOCKED",
                 "GENERATOR_DEPENDENCY_PROOF_REQUIRED",
                 "FORK_MISSING_READ_ONLY_CONTRACT",
                 "READ_ONLY_ORACLE",
                 "TYPED_EXCLUSION",
                 "ISOLATED_COPY_ONLY_ORIGINAL_JAVA_BASE_LOCKED" -> true;
            default -> false;
        };
    }

    /** Standalone Java 21 gate, independent of Maven availability. */
    public static void main(String[] args) throws IOException {
        if (args.length != 1) throw new IllegalArgumentException("census directory required");
        int files = verify(Path.of(args[0]));
        System.out.println("PASS_M3_FIVE_PROVIDER_SOURCE_CENSUS java_files=" + files
                + " providers=" + EXPECTED.size()
                + " source_edits=0");
    }
}
