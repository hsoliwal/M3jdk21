// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Deterministic, reference-only fast-search challenge/donor review catalogue. */
public final class NebulaM3FastSearchReviewPolicy {
    public enum Category {
        BINARY_SEARCH,
        ADAPTIVE_ORDER,
        PREFIX_FUZZY_SEARCH,
        TOP_K,
        PRIMITIVE_LOOKUP,
        BOUNDED_CACHE,
        AUTOMATON_TRIE
    }

    public enum EvidenceSource {
        LEETCODE(1),
        HACKERRANK(2),
        GEEKSFORGEEKS(3),
        GITHUB_DONOR(4);

        private final int passOrder;

        EvidenceSource(int passOrder) {
            this.passOrder = passOrder;
        }

        public int passOrder() {
            return passOrder;
        }
    }

    public record ReviewPass(
            Category category,
            EvidenceSource source,
            String reference,
            String donorRepository,
            String revision,
            String license,
            String disposition,
            String nextAction) {
        public ReviewPass {
            category = Objects.requireNonNull(category, "category");
            source = Objects.requireNonNull(source, "source");
            reference = required(reference, "reference");
            donorRepository = Objects.toString(donorRepository, "").strip();
            revision = Objects.toString(revision, "").strip();
            license = Objects.toString(license, "").strip();
            disposition = required(disposition, "disposition");
            nextAction = required(nextAction, "nextAction");
            if (source == EvidenceSource.GITHUB_DONOR
                    && (donorRepository.isEmpty() || revision.isEmpty() || license.isEmpty())) {
                throw new IllegalArgumentException(
                        "GitHub donor pass requires repository, revision, and license");
            }
            if (source != EvidenceSource.GITHUB_DONOR
                    && (!donorRepository.isEmpty() || !revision.isEmpty() || !license.isEmpty())) {
                throw new IllegalArgumentException(
                        "challenge reference pass cannot carry donor custody");
            }
        }

        public int passOrder() {
            return source.passOrder();
        }
    }

    private static final Map<Category, List<ReviewPass>> PASSES = create();

    private NebulaM3FastSearchReviewPolicy() {
        throw new AssertionError("No instances");
    }

    public static List<Category> categoriesFor(NebulaM3InventoryRecipe.SourceFacts facts) {
        Objects.requireNonNull(facts, "facts");
        LinkedHashSet<Category> categories = new LinkedHashSet<>();
        if (facts.binarySearchCalls() > 0) {
            categories.add(Category.BINARY_SEARCH);
        }
        if (facts.sortCalls() > 0) {
            categories.add(Category.ADAPTIVE_ORDER);
        }
        if (facts.indexOfCalls() > 0) {
            categories.add(Category.PREFIX_FUZZY_SEARCH);
        }
        if (facts.containsCalls() > 0) {
            categories.add(Category.PRIMITIVE_LOOKUP);
        }
        return List.copyOf(categories);
    }

    public static List<ReviewPass> passes(Category category) {
        List<ReviewPass> result = PASSES.get(Objects.requireNonNull(category, "category"));
        if (result == null) {
            throw new IllegalArgumentException("unknown category: " + category);
        }
        return result;
    }

    public static List<ReviewPass> allPasses() {
        ArrayList<ReviewPass> result = new ArrayList<>();
        for (Category category : Category.values()) {
            result.addAll(passes(category));
        }
        return List.copyOf(result);
    }

    public static String renderTsv() {
        StringBuilder out = new StringBuilder(
                "category\tpassOrder\tsource\treference\tgithubDonor\trevision\tlicense"
                        + "\tdisposition\tnextAction\n");
        for (ReviewPass pass : allPasses()) {
            cells(
                    out,
                    pass.category().name(),
                    Integer.toString(pass.passOrder()),
                    pass.source().name(),
                    pass.reference(),
                    pass.donorRepository(),
                    pass.revision(),
                    pass.license(),
                    pass.disposition(),
                    pass.nextAction());
        }
        return out.toString();
    }

    private static Map<Category, List<ReviewPass>> create() {
        EnumMap<Category, List<ReviewPass>> result = new EnumMap<>(Category.class);
        add(
                result,
                Category.BINARY_SEARCH,
                "https://leetcode.com/tag/binary-search/",
                "https://www.hackerrank.com/domains/algorithms/search",
                "https://www.geeksforgeeks.org/dsa/searching-algorithms/",
                "vigna/fastutil",
                "cbf3c2ec706d16c56e6896b13ef0b5361be981c8",
                "Apache-2.0",
                "MECHANICS_EVIDENCE",
                "VERIFY_ORDERING_AND_DUPLICATE_SEMANTICS_THEN_CREATE_OR_IMPROVE_RECIPE");
        add(
                result,
                Category.ADAPTIVE_ORDER,
                "https://leetcode.com/tag/sorting/",
                "https://www.hackerrank.com/domains/algorithms/arrays-and-sorting",
                "https://www.geeksforgeeks.org/dsa/sorting-algorithms/",
                "openjdk/jdk",
                "f3701c80216900f3ded26f9de1befe43813be95c",
                "GPL-2.0 family",
                "ARCHITECTURE_ONLY_NO_COPY",
                "VERIFY_STABILITY_COMPARATOR_AND_MUTATION_THEN_CREATE_OR_IMPROVE_RECIPE");
        add(
                result,
                Category.PREFIX_FUZZY_SEARCH,
                "https://leetcode.com/tag/trie/",
                "https://www.hackerrank.com/domains/algorithms/strings",
                "https://www.geeksforgeeks.org/dsa/trie-insert-and-search/",
                "apache/lucene",
                "90c638746d7e510a361e041a55616c52fccf9afb",
                "Apache-2.0",
                "MECHANICS_EVIDENCE",
                "VERIFY_TEXT_COORDINATES_MATCH_SEMANTICS_AND_BOUNDS_THEN_CREATE_OR_IMPROVE_RECIPE");
        add(
                result,
                Category.TOP_K,
                "https://leetcode.com/tag/heap-priority-queue/",
                "https://www.hackerrank.com/domains/data-structures/heap",
                "https://www.geeksforgeeks.org/dsa/heap-data-structure/",
                "apache/lucene",
                "90c638746d7e510a361e041a55616c52fccf9afb",
                "Apache-2.0",
                "MECHANICS_EVIDENCE",
                "VERIFY_TIE_ORDER_AND_BOUNDED_RESULT_CONTRACT_THEN_CREATE_OR_IMPROVE_RECIPE");
        add(
                result,
                Category.PRIMITIVE_LOOKUP,
                "https://leetcode.com/tag/hash-table/",
                "https://www.hackerrank.com/domains/algorithms/search",
                "https://www.geeksforgeeks.org/dsa/hashing-data-structure/",
                "vigna/fastutil",
                "cbf3c2ec706d16c56e6896b13ef0b5361be981c8",
                "Apache-2.0",
                "MECHANICS_EVIDENCE",
                "VERIFY_EQUALS_IDENTITY_NULL_AND_BOXING_CONTRACT_THEN_CREATE_OR_IMPROVE_RECIPE");
        add(
                result,
                Category.BOUNDED_CACHE,
                "https://leetcode.com/problems/lru-cache/",
                "https://www.hackerrank.com/domains/data-structures",
                "https://www.geeksforgeeks.org/dsa/lru-cache-implementation/",
                "ben-manes/caffeine",
                "e972fb0ee497fa1d0aa9c4a931b037782d951707",
                "Apache-2.0",
                "MECHANICS_EVIDENCE",
                "VERIFY_DERIVED_STATE_BOUNDS_EVICTION_AND_INVALIDATION_THEN_CREATE_OR_IMPROVE_RECIPE");
        add(
                result,
                Category.AUTOMATON_TRIE,
                "https://leetcode.com/tag/trie/",
                "https://www.hackerrank.com/domains/algorithms/strings",
                "https://www.geeksforgeeks.org/dsa/trie-insert-and-search/",
                "apache/lucene",
                "90c638746d7e510a361e041a55616c52fccf9afb",
                "Apache-2.0",
                "MECHANICS_EVIDENCE",
                "VERIFY_PREFIX_LITERAL_AND_FUZZY_SEMANTICS_THEN_CREATE_OR_IMPROVE_RECIPE");
        return Map.copyOf(result);
    }

    private static void add(
            Map<Category, List<ReviewPass>> target,
            Category category,
            String leetCode,
            String hackerRank,
            String geeksForGeeks,
            String donor,
            String revision,
            String license,
            String donorDisposition,
            String nextAction) {
        ArrayList<ReviewPass> passes = new ArrayList<>();
        passes.add(
                new ReviewPass(
                        category,
                        EvidenceSource.LEETCODE,
                        leetCode,
                        "",
                        "",
                        "",
                        "REFERENCE_ONLY",
                        nextAction));
        passes.add(
                new ReviewPass(
                        category,
                        EvidenceSource.HACKERRANK,
                        hackerRank,
                        "",
                        "",
                        "",
                        "REFERENCE_ONLY",
                        nextAction));
        passes.add(
                new ReviewPass(
                        category,
                        EvidenceSource.GEEKSFORGEEKS,
                        geeksForGeeks,
                        "",
                        "",
                        "",
                        "REFERENCE_ONLY",
                        nextAction));
        passes.add(
                new ReviewPass(
                        category,
                        EvidenceSource.GITHUB_DONOR,
                        "https://github.com/" + donor + "/commit/" + revision,
                        donor,
                        revision,
                        license,
                        donorDisposition,
                        nextAction));
        passes.sort(Comparator.comparingInt(ReviewPass::passOrder));
        target.put(category, List.copyOf(passes));
    }

    private static String required(String value, String field) {
        String checked = Objects.requireNonNull(value, field).strip();
        if (checked.isEmpty() || checked.indexOf('\0') >= 0) {
            throw new IllegalArgumentException(field + " required");
        }
        return checked;
    }

    private static void cells(StringBuilder out, String... values) {
        for (int index = 0; index < values.length; index++) {
            if (index > 0) {
                out.append('\t');
            }
            out.append(values[index].replace('\t', ' ').replace('\n', ' ').replace('\r', ' '));
        }
        out.append('\n');
    }
}
