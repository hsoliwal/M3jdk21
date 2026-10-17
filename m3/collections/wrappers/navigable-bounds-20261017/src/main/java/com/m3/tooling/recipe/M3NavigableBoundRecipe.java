// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.recipe;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/** Exact, idempotent, fail-closed Maven transformation of four navigable methods. */
public final class M3NavigableBoundRecipe {
    private static final String OWNER = "public final class M3FilteredNavigableSet<E>";
    public static final String BEFORE = """
                @Override
                public E lower(E e) {
                    return firstOrNull(filteredIterator(backing.headSet(e, false).descendingIterator()));
                }

                @Override
                public E floor(E e) {
                    return firstOrNull(filteredIterator(backing.headSet(e, true).descendingIterator()));
                }

                @Override
                public E ceiling(E e) {
                    return firstOrNull(filteredIterator(backing.tailSet(e, true).iterator()));
                }

                @Override
                public E higher(E e) {
                    return firstOrNull(filteredIterator(backing.tailSet(e, false).iterator()));
                }
            """;

    public static final String AFTER = """
                /** Resolve the first stored predecessor before creating an in-range cursor. */
                private E predecessor(E probe, boolean inclusive) {
                    E first = inclusive ? backing.floor(probe) : backing.lower(probe);
                    if (first == null) return null;
                    return firstOrNull(filteredIterator(backing.headSet(first, true).descendingIterator()));
                }

                /** Resolve the first stored successor before creating an in-range cursor. */
                private E successor(E probe, boolean inclusive) {
                    E first = inclusive ? backing.ceiling(probe) : backing.higher(probe);
                    if (first == null) return null;
                    return firstOrNull(filteredIterator(backing.tailSet(first, true).iterator()));
                }

                @Override
                public E lower(E e) {
                    return predecessor(e, false);
                }

                @Override
                public E floor(E e) {
                    return predecessor(e, true);
                }

                @Override
                public E ceiling(E e) {
                    return successor(e, true);
                }

                @Override
                public E higher(E e) {
                    return successor(e, false);
                }
            """;

    private M3NavigableBoundRecipe() {}

    public static String transform(String source) {
        Objects.requireNonNull(source, "source");
        if (!source.contains("package com.m3.collections;") || !source.contains(OWNER)) {
            throw new IllegalArgumentException("M3_NAV_BOUND_OWNER_MISMATCH");
        }
        int beforeCount = occurrences(source, BEFORE);
        int afterCount = occurrences(source, AFTER);
        if (beforeCount == 0 && afterCount == 1) return source; // fixed point
        if (beforeCount != 1 || afterCount != 0) {
            throw new IllegalArgumentException("M3_NAV_BOUND_PREIMAGE_MISMATCH");
        }
        final String generated = source.replace(BEFORE, AFTER);
        if (!generated.contains(AFTER) || occurrences(generated, AFTER) != 1) {
            throw new IllegalStateException("M3_NAV_BOUND_GENERATION_FAILED");
        }
        return generated;
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2 || !("--apply".equals(args[0]) || "--verify".equals(args[0]))) {
            throw new IllegalArgumentException("usage: --apply|--verify path-to-receiver");
        }
        Path sourceFile = Path.of(args[1]).toAbsolutePath().normalize();
        String before = Files.readString(sourceFile, StandardCharsets.UTF_8);
        String after = transform(before);
        if ("--verify".equals(args[0])) {
            if (!before.equals(after)) {
                throw new IllegalStateException("M3_NAV_BOUND_RECIPE_OUTPUT_NOT_INSTALLED");
            }
            System.out.println("M3_NAV_BOUND_RECEIVER_FIXED_POINT_PASS");
            return;
        }
        if (before.equals(after)) {
            System.out.println("M3_NAV_BOUND_ALREADY_APPLIED");
            return;
        }
        Path staged = Files.createTempFile(sourceFile.getParent(), ".m3-nav-recipe-", ".tmp");
        try {
            Files.writeString(staged, after, StandardCharsets.UTF_8);
            if (!Files.readString(sourceFile, StandardCharsets.UTF_8).equals(before)) {
                throw new IllegalStateException("M3_NAV_BOUND_SOURCE_CHANGED");
            }
            Files.move(staged, sourceFile, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(staged);
        }
        System.out.println("M3_NAV_BOUND_APPLIED");
    }

    private static int occurrences(String source, String part) {
        int hits = 0;
        for (int from = 0; (from = source.indexOf(part, from)) >= 0;
                from += part.length()) hits++;
        return hits;
    }
}
