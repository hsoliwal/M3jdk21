// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.corpus;

import com.synexia.algorithms.shapes.AlgorithmShape;
import com.synexia.algorithms.shapes.AlgorithmShapeMask;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Canonical precomputed join from one {@link AlgorithmShape} to challenge taxonomy evidence and
 * the existing optimization plan used by M3 recipe-first review.
 *
 * <p>This index is evidence/planning only. It does not copy donor source, execute native code,
 * mutate source, authorize replacement, or promote a recipe. Those authorities remain in the
 * existing M3 gates.</p>
 */
public final class CompetitiveCapabilityPlanIndex {
    public record Row(
            AlgorithmShape shape,
            AlgorithmShapeMask capabilityMask,
            CompetitiveProblemCategory category,
            List<ChallengeCategoryCapabilityIndex.Row> challengeCategories,
            List<CompetitiveProblemCategoryCatalog.Category> platformCategories,
            Set<ChallengePlatform> representedPlatforms,
            ProblemOptimizationCatalog.Plan optimization,
            String root) {

        public Row {
            shape = Objects.requireNonNull(shape, "shape");
            capabilityMask = Objects.requireNonNull(capabilityMask, "capabilityMask");
            if (capabilityMask.bitCount() != 1 || !capabilityMask.contains(shape)) {
                throw new IllegalArgumentException("capabilityMask must identify exactly one shape");
            }

            category = Objects.requireNonNull(category, "category");
            if (category != CompetitiveProblemCategory.fromShape(shape)) {
                throw new IllegalArgumentException("shape/category drift");
            }

            optimization = Objects.requireNonNull(optimization, "optimization");
            if (optimization.shape() != shape || optimization.category() != category) {
                throw new IllegalArgumentException("shape/optimization drift");
            }

            challengeCategories =
                    Objects.requireNonNull(challengeCategories, "challengeCategories").stream()
                            .sorted(
                                    Comparator.comparing(
                                                    (ChallengeCategoryCapabilityIndex.Row row) ->
                                                            row.category().platform().name())
                                            .thenComparing(row -> row.category().id()))
                            .toList();
            for (ChallengeCategoryCapabilityIndex.Row row : challengeCategories) {
                if (!row.capabilityMask().contains(shape)) {
                    throw new IllegalArgumentException("challenge category does not contain shape");
                }
                if (row.category().platform() == ChallengePlatform.OTHER) {
                    throw new IllegalArgumentException("OTHER challenge category evidence");
                }
            }

            platformCategories =
                    Objects.requireNonNull(platformCategories, "platformCategories").stream()
                            .sorted(
                                    Comparator.comparing(
                                                    (CompetitiveProblemCategoryCatalog.Category row) ->
                                                            row.platform().name())
                                            .thenComparing(
                                                    CompetitiveProblemCategoryCatalog.Category::key))
                            .toList();
            for (CompetitiveProblemCategoryCatalog.Category row : platformCategories) {
                if (row.normalizedCategory() != category) {
                    throw new IllegalArgumentException("platform category normalization drift");
                }
            }

            EnumSet<ChallengePlatform> expectedPlatforms =
                    EnumSet.noneOf(ChallengePlatform.class);
            challengeCategories.forEach(
                    row -> expectedPlatforms.add(row.category().platform()));
            representedPlatforms =
                    representedPlatforms == null || representedPlatforms.isEmpty()
                            ? Set.copyOf(expectedPlatforms)
                            : Set.copyOf(representedPlatforms);
            if (!representedPlatforms.equals(Set.copyOf(expectedPlatforms))) {
                throw new IllegalArgumentException("represented platform drift");
            }

            String expectedRoot = CompetitiveCapabilityPlanIndex.root(
                    shape,
                    capabilityMask,
                    category,
                    challengeCategories,
                    platformCategories,
                    optimization);
            root = root == null || root.isBlank() ? expectedRoot : requireRoot(root);
            if (!root.equals(expectedRoot)) {
                throw new IllegalArgumentException("capability plan row root mismatch");
            }
        }

        public boolean sourceCopyAuthority() {
            return false;
        }

        public boolean replacementAuthority() {
            return false;
        }

        public boolean nativeExecutionAuthority() {
            return false;
        }

        public boolean promotionAuthority() {
            return false;
        }

        public boolean hasPlatform(ChallengePlatform platform) {
            return representedPlatforms.contains(
                    Objects.requireNonNull(platform, "platform"));
        }

        public boolean nativeCandidate() {
            return optimization.nativeEligible();
        }
    }

    private static final CompetitiveCapabilityPlanIndex CANONICAL = compileCanonical();

    private final Map<AlgorithmShape, Row> byShape;
    private final Map<CompetitiveProblemCategory, List<Row>> byCategory;
    private final Map<ProblemOptimizationCatalog.Kernel, List<Row>> byKernel;
    private final Map<ProblemOptimizationCatalog.Surface, List<Row>> bySurface;
    private final Map<ProblemOptimizationCatalog.NativeLane, List<Row>> byNativeLane;
    private final Map<ChallengePlatform, List<Row>> byPlatform;
    private final List<Row> nativeCandidates;
    private final List<Row> rows;
    private final String root;

    private CompetitiveCapabilityPlanIndex(List<Row> rows, String root) {
        ArrayList<Row> stable = new ArrayList<>(Objects.requireNonNull(rows, "rows"));
        stable.sort(Comparator.comparing(row -> row.shape().name()));
        if (stable.size() != AlgorithmShape.values().length) {
            throw new IllegalArgumentException("incomplete capability plan index");
        }

        EnumMap<AlgorithmShape, Row> shapes = new EnumMap<>(AlgorithmShape.class);
        EnumMap<CompetitiveProblemCategory, ArrayList<Row>> categories =
                lists(CompetitiveProblemCategory.class);
        EnumMap<ProblemOptimizationCatalog.Kernel, ArrayList<Row>> kernels =
                lists(ProblemOptimizationCatalog.Kernel.class);
        EnumMap<ProblemOptimizationCatalog.Surface, ArrayList<Row>> surfaces =
                lists(ProblemOptimizationCatalog.Surface.class);
        EnumMap<ProblemOptimizationCatalog.NativeLane, ArrayList<Row>> nativeLanes =
                lists(ProblemOptimizationCatalog.NativeLane.class);
        EnumMap<ChallengePlatform, ArrayList<Row>> platforms =
                lists(ChallengePlatform.class);

        for (Row row : stable) {
            if (shapes.putIfAbsent(row.shape(), row) != null) {
                throw new IllegalArgumentException("duplicate capability shape: " + row.shape());
            }
            categories.get(row.category()).add(row);
            row.optimization().kernels().forEach(kernel -> kernels.get(kernel).add(row));
            row.optimization().surfaces().forEach(surface -> surfaces.get(surface).add(row));
            nativeLanes.get(row.optimization().nativeLane()).add(row);
            row.representedPlatforms().forEach(platform -> platforms.get(platform).add(row));
        }
        if (shapes.size() != AlgorithmShape.values().length) {
            throw new IllegalArgumentException("missing capability shape");
        }

        this.rows = List.copyOf(stable);
        this.byShape = java.util.Collections.unmodifiableMap(shapes);
        this.byCategory = freeze(CompetitiveProblemCategory.class, categories);
        this.byKernel = freeze(ProblemOptimizationCatalog.Kernel.class, kernels);
        this.bySurface = freeze(ProblemOptimizationCatalog.Surface.class, surfaces);
        this.byNativeLane = freeze(ProblemOptimizationCatalog.NativeLane.class, nativeLanes);
        this.byPlatform = freeze(ChallengePlatform.class, platforms);
        this.nativeCandidates = stable.stream().filter(Row::nativeCandidate).toList();
        this.root = requireRoot(root);
    }

    public static CompetitiveCapabilityPlanIndex canonical() {
        return CANONICAL;
    }

    public Row require(AlgorithmShape shape) {
        Row row = byShape.get(Objects.requireNonNull(shape, "shape"));
        if (row == null) throw new IllegalStateException("missing capability plan: " + shape);
        return row;
    }

    public List<Row> rows() {
        return rows;
    }

    public List<Row> byCategory(CompetitiveProblemCategory category) {
        return byCategory.getOrDefault(
                Objects.requireNonNull(category, "category"), List.of());
    }

    public List<Row> byKernel(ProblemOptimizationCatalog.Kernel kernel) {
        return byKernel.getOrDefault(Objects.requireNonNull(kernel, "kernel"), List.of());
    }

    public List<Row> bySurface(ProblemOptimizationCatalog.Surface surface) {
        return bySurface.getOrDefault(Objects.requireNonNull(surface, "surface"), List.of());
    }

    public List<Row> byNativeLane(ProblemOptimizationCatalog.NativeLane lane) {
        return byNativeLane.getOrDefault(Objects.requireNonNull(lane, "lane"), List.of());
    }

    public List<Row> byPlatform(ChallengePlatform platform) {
        return byPlatform.getOrDefault(
                Objects.requireNonNull(platform, "platform"), List.of());
    }

    public List<Row> nativeCandidates() {
        return nativeCandidates;
    }

    public String root() {
        return root;
    }

    public String toTsv() {
        StringBuilder out =
                new StringBuilder(
                        "shape\tcategory\tplatforms\tchallenge_categories\tkernels"
                                + "\tsurfaces\tnative_lane\toptimization_root\trow_root\n");
        for (Row row : rows) {
            out.append(row.shape().name())
                    .append('\t')
                    .append(row.category().name())
                    .append('\t')
                    .append(
                            String.join(
                                    ",",
                                    row.representedPlatforms().stream()
                                            .map(Enum::name)
                                            .sorted()
                                            .toList()))
                    .append('\t')
                    .append(
                            String.join(
                                    ",",
                                    row.challengeCategories().stream()
                                            .map(
                                                    category ->
                                                            category.category().platform().name()
                                                                    + ":"
                                                                    + category.category().id())
                                            .toList()))
                    .append('\t')
                    .append(
                            String.join(
                                    ",",
                                    row.optimization().kernels().stream()
                                            .map(Enum::name)
                                            .sorted()
                                            .toList()))
                    .append('\t')
                    .append(
                            String.join(
                                    ",",
                                    row.optimization().surfaces().stream()
                                            .map(Enum::name)
                                            .sorted()
                                            .toList()))
                    .append('\t')
                    .append(row.optimization().nativeLane().name())
                    .append('\t')
                    .append(row.optimization().root())
                    .append('\t')
                    .append(row.root())
                    .append('\n');
        }
        return out.toString();
    }

    private static CompetitiveCapabilityPlanIndex compileCanonical() {
        ChallengeCategoryCapabilityIndex categoryCapabilities =
                ChallengeCategoryCapabilityIndex.canonical();
        ArrayList<Row> rows = new ArrayList<>(AlgorithmShape.values().length);
        for (AlgorithmShape shape : AlgorithmShape.values()) {
            AlgorithmShapeMask mask = AlgorithmShapeMask.of(shape);
            CompetitiveProblemCategory category = CompetitiveProblemCategory.fromShape(shape);
            List<ChallengeCategoryCapabilityIndex.Row> challengeCategories =
                    categoryCapabilities.intersecting(mask);
            List<CompetitiveProblemCategoryCatalog.Category> platformCategories =
                    CompetitiveProblemCategoryCatalog.byCategory(category);
            ProblemOptimizationCatalog.Plan optimization =
                    ProblemOptimizationCatalog.plan(shape);
            rows.add(
                    new Row(
                            shape,
                            mask,
                            category,
                            challengeCategories,
                            platformCategories,
                            Set.of(),
                            optimization,
                            ""));
        }

        rows.sort(Comparator.comparing(row -> row.shape().name()));
        CatalogueDigest digest =
                new CatalogueDigest("SYNEXIA_COMPETITIVE_CAPABILITY_PLAN_INDEX_V1")
                        .text(AlgorithmShapeMask.schemaRoot())
                        .text(categoryCapabilities.root())
                        .text(ProblemOptimizationCatalog.root())
                        .number(rows.size());
        rows.stream().map(Row::root).forEach(digest::text);
        return new CompetitiveCapabilityPlanIndex(rows, digest.finish());
    }

    private static String root(
            AlgorithmShape shape,
            AlgorithmShapeMask capabilityMask,
            CompetitiveProblemCategory category,
            List<ChallengeCategoryCapabilityIndex.Row> challengeCategories,
            List<CompetitiveProblemCategoryCatalog.Category> platformCategories,
            ProblemOptimizationCatalog.Plan optimization) {
        CatalogueDigest digest =
                new CatalogueDigest("SYNEXIA_COMPETITIVE_CAPABILITY_PLAN_ROW_V1")
                        .text(shape.name())
                        .text(capabilityMask.hex())
                        .text(category.name())
                        .text(optimization.root())
                        .number(challengeCategories.size());
        for (ChallengeCategoryCapabilityIndex.Row row : challengeCategories) {
            digest.text(row.category().platform().name())
                    .text(row.category().id())
                    .text(row.capabilityMask().hex());
        }
        digest.number(platformCategories.size());
        for (CompetitiveProblemCategoryCatalog.Category row : platformCategories) {
            digest.text(row.stableKey())
                    .text(row.label())
                    .text(row.officialUrl());
        }
        return digest.finish();
    }

    private static String requireRoot(String value) {
        String checked = Objects.toString(value, "").strip().toLowerCase(java.util.Locale.ROOT);
        if (!checked.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("root");
        }
        return checked;
    }

    private static <E extends Enum<E>> EnumMap<E, ArrayList<Row>> lists(Class<E> type) {
        EnumMap<E, ArrayList<Row>> result = new EnumMap<>(type);
        for (E value : type.getEnumConstants()) result.put(value, new ArrayList<>());
        return result;
    }

    private static <E extends Enum<E>> Map<E, List<Row>> freeze(
            Class<E> type, EnumMap<E, ArrayList<Row>> source) {
        EnumMap<E, List<Row>> result = new EnumMap<>(type);
        source.forEach(
                (key, values) ->
                        result.put(
                                key,
                                values.stream()
                                        .sorted(Comparator.comparing(row -> row.shape().name()))
                                        .toList()));
        return java.util.Collections.unmodifiableMap(result);
    }
}
