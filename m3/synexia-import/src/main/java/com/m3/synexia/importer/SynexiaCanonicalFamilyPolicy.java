// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
// Modified 2026-10-07: classify the existing Synexia collection and algorithm custody prefixes.
package com.m3.synexia.importer;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Receiver-only classification for canonical Synexia families imported into M3JDK21.
 *
 * <p>This class does not define canonical ownership; Synexia does. It only fails closed when an
 * automatic Apache vendor manifest labels an M3Index-family source inconsistently with the
 * canonical Synexia coordinate expected by this receiver.</p>
 */
public final class SynexiaCanonicalFamilyPolicy {
    public enum Family {
        M3INDEX_ALIAS("synexia-m3index/", "com.synexia:synexia-m3index-core"),
        INDEXSTRING(
                "synexia-indexstring/",
                "com.synexia:synexia-m3index-jdk-bridge"),
        M3INDEX_COMPILER(
                "synexia-mindex/compiler/",
                "com.synexia:synexia-m3index-compiler"),
        M3INDEX_DATA_STRUCTURE(
                "synexia-mindex/data-structure/",
                "com.synexia:synexia-m3index-data-structure"),
        M3INDEX_PRECOMPUTE_API(
                "synexia-mindex/precompute-api/",
                "com.synexia:synexia-m3index-precompute-api"),
        M3INDEX_DB(
                "synexia-mindex/db/",
                "com.synexia:synexia-m3index-db"),
        M3INDEX_COLLECTIONS(
                "synexia-mindex/collections/",
                "com.synexia:synexia-m3index-collections"),
        M3INDEX_ALGORITHM(
                "synexia-mindex/algorithm/",
                "com.synexia:synexia-m3index-algorithm"),
        OPENREWRITE(
                "synexia-openrewrite-recipes/",
                "com.synexia:synexia-openrewrite-recipes"),
        CODE_CONVERGENCE(
                "synexia-code-convergence/",
                "com.synexia:synexia-code-convergence"),
        M3_RECIPE(
                "synexia-m3-recipe/",
                "com.synexia:synexia-m3-recipe");

        private final String sourcePrefix;
        private final String canonicalOwner;

        Family(String sourcePrefix, String canonicalOwner) {
            this.sourcePrefix = sourcePrefix;
            this.canonicalOwner = canonicalOwner;
        }

        public String sourcePrefix() {
            return sourcePrefix;
        }

        public String canonicalOwner() {
            return canonicalOwner;
        }
    }

    private static final List<Family> FAMILIES = List.of(Family.values());

    private SynexiaCanonicalFamilyPolicy() {}

    public static Optional<Family> classify(String sourcePath) {
        String path = relative(sourcePath, "sourcePath");
        return FAMILIES.stream()
                .filter(family -> path.startsWith(family.sourcePrefix()))
                .findFirst();
    }

    public static Family requireFamily(String sourcePath) {
        return classify(sourcePath)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "unclassified canonical Synexia family: " + sourcePath));
    }

    public static void requireMirrorTarget(String sourcePath, String targetPath) {
        String source = relative(sourcePath, "sourcePath");
        String target = relative(targetPath, "targetPath");
        String expected = "m3/vendor/synexia/" + source;
        if (!expected.equals(target)) {
            throw new IllegalArgumentException(
                    "Synexia vendor target must mirror source path: expected "
                            + expected
                            + ", found "
                            + target);
        }
    }

    public static void requireCategoryFamily(String category, String sourcePath) {
        String checkedCategory = token(category, "category");
        Optional<Family> family = classify(sourcePath);

        if (checkedCategory.startsWith("m3index-")
                || checkedCategory.startsWith("indexstring-")
                || "m3index-family".equals(checkedCategory)) {
            if (family.isEmpty()) {
                throw new IllegalArgumentException(
                        "M3Index import category requires classified Synexia family");
            }
            Family value = family.get();
            if (checkedCategory.startsWith("indexstring-")
                    && value != Family.INDEXSTRING) {
                throw new IllegalArgumentException(
                        "indexstring category/source mismatch");
            }
            if (checkedCategory.startsWith("m3index-")
                    && value == Family.INDEXSTRING) {
                throw new IllegalArgumentException(
                        "M3Index category cannot claim IndexString family");
            }
        }
    }

    private static String relative(String value, String field) {
        String checked = token(value, field).replace('\\', '/');
        if (checked.startsWith("/")
                || checked.contains("//")
                || checked.equals("..")
                || checked.startsWith("../")
                || checked.contains("/../")
                || checked.endsWith("/..")) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static String token(String value, String field) {
        String checked = Objects.toString(value, "").strip();
        if (checked.isEmpty()
                || checked.indexOf('\0') >= 0
                || checked.indexOf('\t') >= 0
                || checked.indexOf('\r') >= 0
                || checked.indexOf('\n') >= 0) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }
}
