// SPDX-License-Identifier: Apache-2.0
package com.synexia.m3.search;

import com.synexia.algorithms.corpus.ChallengeCategoryCapabilityIndex;
import com.synexia.algorithms.corpus.ChallengeCategoryPrimitiveIndex;
import com.synexia.algorithms.corpus.ChallengeCategoryCatalog;
import com.synexia.algorithms.corpus.ChallengePlatform;
import com.synexia.algorithms.corpus.ChallengeSearchPrimitiveCatalog;
import com.synexia.algorithms.corpus.CompetitiveProblemCategory;
import com.synexia.algorithms.corpus.ProblemAdapter;
import com.synexia.algorithms.corpus.ProblemAdapterCatalog;
import com.synexia.algorithms.corpus.ProblemOptimizationCatalog;
import com.synexia.algorithms.shapes.AlgorithmShape;
import com.synexia.algorithms.shapes.AlgorithmShapeMask;
import com.synexia.fastsearch.problem.ProblemCatalogue;
import com.synexia.fastsearch.problem.ProblemCategory;
import com.synexia.fastsearch.problem.ProblemDescriptor;
import com.synexia.fastsearch.problem.ProblemSource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Cross-site competitive-problem review for M3 recipe work.
 *
 * <p>The review is deliberately evidence-only. LeetCode, HackerRank and GeeksforGeeks taxonomy
 * rows nominate canonical Synexia algorithm shapes and optimization plans. They never grant source
 * copy, rewrite, equivalence or promotion authority. Actual search execution continues to use the
 * existing Java/JNI {@link SearchEngine} providers.</p>
 */
public final class CompetitiveProblemReview {
    private static final List<ChallengePlatform> REVIEW_ORDER = List.of(
            ChallengePlatform.LEETCODE,
            ChallengePlatform.HACKERRANK,
            ChallengePlatform.GEEKSFORGEEKS);

    private static final EnumSet<ProblemOptimizationCatalog.Kernel> FAST_SEARCH_KERNELS =
            EnumSet.of(
                    ProblemOptimizationCatalog.Kernel.ORDERED_RANK_SEARCH,
                    ProblemOptimizationCatalog.Kernel.LINEAR_SCAN,
                    ProblemOptimizationCatalog.Kernel.SLIDING_WINDOW,
                    ProblemOptimizationCatalog.Kernel.HASH_TABLE,
                    ProblemOptimizationCatalog.Kernel.BITMAP_POSTINGS,
                    ProblemOptimizationCatalog.Kernel.TRIE,
                    ProblemOptimizationCatalog.Kernel.AHO_CORASICK,
                    ProblemOptimizationCatalog.Kernel.KMP,
                    ProblemOptimizationCatalog.Kernel.ROLLING_HASH);

    public record Lane(
            ChallengePlatform platform,
            String categoryId,
            String displayName,
            String source,
            List<AlgorithmShape> shapes,
            List<ProblemOptimizationCatalog.Plan> plans,
            String root) {

        public Lane {
            platform = Objects.requireNonNull(platform, "platform");
            if (!REVIEW_ORDER.contains(platform)) {
                throw new IllegalArgumentException("COMPETITIVE_REVIEW_PLATFORM");
            }
            categoryId = token(categoryId, "categoryId");
            displayName = token(displayName, "displayName");
            source = token(source, "source");
            shapes = Objects.requireNonNull(shapes, "shapes").stream()
                    .sorted(Comparator.comparing(Enum::name))
                    .distinct()
                    .toList();
            plans = Objects.requireNonNull(plans, "plans").stream()
                    .sorted(Comparator.comparing(plan -> plan.shape().name()))
                    .toList();
            if (!plans.stream().map(ProblemOptimizationCatalog.Plan::shape).toList().equals(shapes)) {
                throw new IllegalArgumentException("COMPETITIVE_REVIEW_PLAN_SHAPE_DRIFT");
            }
            String expected = digest(
                    "COMPETITIVE-REVIEW-LANE/1",
                    platform.name(),
                    categoryId,
                    displayName,
                    source,
                    shapes.stream().map(Enum::name).toList().toString(),
                    plans.stream().map(ProblemOptimizationCatalog.Plan::root).toList().toString());
            root = root == null || root.isBlank() ? expected : hash(root, "root");
            if (!root.equals(expected)) {
                throw new IllegalArgumentException("COMPETITIVE_REVIEW_LANE_ROOT_MISMATCH");
            }
        }

        public boolean sourceCopyAuthority() {
            return false;
        }

        public boolean substitutionAuthority() {
            return false;
        }
    }


    /** One source-filtered problem-metadata lane; public-site rows remain evidence only. */
    public record ProblemEvidenceLane(
            ChallengePlatform platform,
            ProblemSource source,
            ProblemCategory category,
            String query,
            List<ProblemDescriptor> problems,
            String catalogueRoot,
            String root) {

        public ProblemEvidenceLane {
            platform = Objects.requireNonNull(platform, "platform");
            source = Objects.requireNonNull(source, "source");
            category = Objects.requireNonNull(category, "category");
            query = CompetitiveProblemReview.query(query);
            problems = List.copyOf(Objects.requireNonNull(problems, "problems"));
            catalogueRoot = hash(catalogueRoot, "catalogueRoot");
            if (problemSource(platform) != source) {
                throw new IllegalArgumentException("COMPETITIVE_PROBLEM_SOURCE_PLATFORM_DRIFT");
            }
            final ProblemSource checkedSource = source;
            final ProblemCategory checkedCategory = category;
            if (problems.stream().anyMatch(problem ->
                    problem.source() != checkedSource
                            || !problem.categories().contains(checkedCategory)
                            || !problem.evidenceOnly())) {
                throw new IllegalArgumentException("COMPETITIVE_PROBLEM_EVIDENCE_DRIFT");
            }
            String expected = digest(
                    "COMPETITIVE-PROBLEM-METADATA-LANE/1",
                    platform.name(),
                    source.name(),
                    category.name(),
                    query,
                    catalogueRoot,
                    problems.stream()
                            .map(problem -> problem.canonicalKey() + "@" + problem.uri())
                            .toList()
                            .toString());
            root = root == null || root.isBlank() ? expected : hash(root, "root");
            if (!root.equals(expected)) {
                throw new IllegalArgumentException("COMPETITIVE_PROBLEM_LANE_ROOT_MISMATCH");
            }
        }

        public boolean sourceCopyAuthority() {
            return false;
        }

        public boolean substitutionAuthority() {
            return false;
        }
    }

    /** Fixed-order LeetCode -> HackerRank -> GeeksforGeeks metadata review. */
    public record ProblemEvidenceReview(
            String recipeFamily,
            ProblemCategory category,
            String query,
            String catalogueRoot,
            List<ProblemEvidenceLane> lanes,
            String root) {

        public ProblemEvidenceReview {
            recipeFamily = recipe(recipeFamily);
            category = Objects.requireNonNull(category, "category");
            query = CompetitiveProblemReview.query(query);
            catalogueRoot = hash(catalogueRoot, "catalogueRoot");
            lanes = List.copyOf(Objects.requireNonNull(lanes, "lanes"));
            if (!lanes.stream().map(ProblemEvidenceLane::platform).toList().equals(REVIEW_ORDER)) {
                throw new IllegalArgumentException("COMPETITIVE_PROBLEM_REVIEW_ORDER");
            }
            final ProblemCategory checkedCategory = category;
            final String checkedQuery = query;
            final String checkedCatalogueRoot = catalogueRoot;
            if (lanes.stream().anyMatch(lane ->
                    lane.category() != checkedCategory
                            || !lane.query().equals(checkedQuery)
                            || !lane.catalogueRoot().equals(checkedCatalogueRoot))) {
                throw new IllegalArgumentException("COMPETITIVE_PROBLEM_REVIEW_LANE_DRIFT");
            }
            String expected = digest(
                    "COMPETITIVE-PROBLEM-METADATA-REVIEW/1",
                    recipeFamily,
                    category.name(),
                    query,
                    catalogueRoot,
                    lanes.stream().map(ProblemEvidenceLane::root).toList().toString());
            root = root == null || root.isBlank() ? expected : hash(root, "root");
            if (!root.equals(expected)) {
                throw new IllegalArgumentException("COMPETITIVE_PROBLEM_REVIEW_ROOT_MISMATCH");
            }
        }

        public int problemCount() {
            return lanes.stream().mapToInt(lane -> lane.problems().size()).sum();
        }

        /** Candidate Java adapters from the existing precomputed platform/category postings. */
        public ProblemAdapterReview executableAdapterCandidates() {
            CompetitiveProblemCategory canonical = canonicalProblemCategory(category);
            String indexRoot = ProblemAdapterCatalog.indexSnapshot().root();
            List<ProblemAdapterLane> adapterLanes = REVIEW_ORDER.stream()
                    .map(platform -> problemAdapterLane(
                            platform,
                            category,
                            canonical,
                            indexRoot))
                    .toList();
            return new ProblemAdapterReview(root, indexRoot, canonical, adapterLanes, "");
        }

        /**
         * Existing executable search primitives implied by the actual classified adapter postings.
         *
         * <p>The adapter review stays authoritative for which donor adapters are candidates. This
         * projection only reuses canonical search primitives for those adapter shapes.</p>
         */
        public ProblemPrimitiveReview executablePrimitiveCandidates() {
            ProblemAdapterReview adapters = executableAdapterCandidates();
            List<ProblemPrimitiveLane> primitiveLanes =
                    adapters.lanes().stream()
                            .map(CompetitiveProblemReview::problemPrimitiveLane)
                            .toList();
            return new ProblemPrimitiveReview(
                    root,
                    adapters,
                    ChallengeSearchPrimitiveCatalog.root(),
                    primitiveLanes,
                    "");
        }

        public boolean sourceCopyAuthority() {
            return false;
        }

        public boolean substitutionAuthority() {
            return false;
        }

        public boolean promotionAuthority() {
            return false;
        }
    }

    /** One platform lane over the existing precomputed problem-adapter index. */
    public record ProblemAdapterLane(
            ChallengePlatform platform,
            ProblemSource source,
            ProblemCategory problemCategory,
            CompetitiveProblemCategory canonicalCategory,
            String adapterIndexRoot,
            int adapterCount,
            String root) {

        public ProblemAdapterLane {
            platform = Objects.requireNonNull(platform, "platform");
            source = Objects.requireNonNull(source, "source");
            problemCategory = Objects.requireNonNull(problemCategory, "problemCategory");
            canonicalCategory = Objects.requireNonNull(canonicalCategory, "canonicalCategory");
            adapterIndexRoot = hash(adapterIndexRoot, "adapterIndexRoot");
            if (source != problemSource(platform)
                    || canonicalCategory != canonicalProblemCategory(problemCategory)
                    || !adapterIndexRoot.equals(ProblemAdapterCatalog.indexSnapshot().root())) {
                throw new IllegalArgumentException("COMPETITIVE_PROBLEM_ADAPTER_LANE_DRIFT");
            }
            List<ProblemAdapter> indexed =
                    ProblemAdapterCatalog.byPlatformCategory(platform.name(), canonicalCategory);
            final ChallengePlatform checkedPlatform = platform;
            final CompetitiveProblemCategory checkedCategory = canonicalCategory;
            if (adapterCount != indexed.size()
                    || indexed.stream().anyMatch(adapter ->
                            !adapter.executable()
                                    || !adapter.source().platform().equalsIgnoreCase(checkedPlatform.name())
                                    || CompetitiveProblemCategory.fromShape(
                                            adapter.classification().shape()) != checkedCategory)) {
                throw new IllegalArgumentException("COMPETITIVE_PROBLEM_ADAPTER_POSTING_DRIFT");
            }
            String expected = digest(
                    "COMPETITIVE-PROBLEM-ADAPTER-LANE/1",
                    platform.name(),
                    source.name(),
                    problemCategory.name(),
                    canonicalCategory.name(),
                    adapterIndexRoot,
                    Integer.toString(adapterCount),
                    indexed.stream().map(ProblemAdapter::id).toList().toString());
            root = root == null || root.isBlank() ? expected : hash(root, "root");
            if (!root.equals(expected)) {
                throw new IllegalArgumentException(
                        "COMPETITIVE_PROBLEM_ADAPTER_LANE_ROOT_MISMATCH");
            }
        }

        /** Shared immutable view owned by ProblemAdapterIndex; no per-review adapter copy. */
        public List<ProblemAdapter> adapters() {
            return ProblemAdapterCatalog.byPlatformCategory(platform.name(), canonicalCategory);
        }

        public boolean sourceCopyAuthority() {
            return false;
        }

        public boolean executionAuthority() {
            return false;
        }
    }

    /** Category-level Java adapter candidates for one evidence-only problem review. */
    public record ProblemAdapterReview(
            String problemEvidenceReviewRoot,
            String adapterIndexRoot,
            CompetitiveProblemCategory canonicalCategory,
            List<ProblemAdapterLane> lanes,
            String root) {

        public ProblemAdapterReview {
            problemEvidenceReviewRoot =
                    hash(problemEvidenceReviewRoot, "problemEvidenceReviewRoot");
            adapterIndexRoot = hash(adapterIndexRoot, "adapterIndexRoot");
            canonicalCategory = Objects.requireNonNull(canonicalCategory, "canonicalCategory");
            if (!adapterIndexRoot.equals(ProblemAdapterCatalog.indexSnapshot().root())) {
                throw new IllegalArgumentException(
                        "COMPETITIVE_PROBLEM_ADAPTER_INDEX_ROOT_DRIFT");
            }
            lanes = List.copyOf(Objects.requireNonNull(lanes, "lanes"));
            final CompetitiveProblemCategory checkedCategory = canonicalCategory;
            final String checkedIndexRoot = adapterIndexRoot;
            if (!lanes.stream().map(ProblemAdapterLane::platform).toList().equals(REVIEW_ORDER)
                    || lanes.stream().anyMatch(lane ->
                            lane.canonicalCategory() != checkedCategory
                                    || !lane.adapterIndexRoot().equals(checkedIndexRoot))) {
                throw new IllegalArgumentException("COMPETITIVE_PROBLEM_ADAPTER_REVIEW_DRIFT");
            }
            String expected = digest(
                    "COMPETITIVE-PROBLEM-ADAPTER-REVIEW/1",
                    problemEvidenceReviewRoot,
                    adapterIndexRoot,
                    canonicalCategory.name(),
                    lanes.stream().map(ProblemAdapterLane::root).toList().toString());
            root = root == null || root.isBlank() ? expected : hash(root, "root");
            if (!root.equals(expected)) {
                throw new IllegalArgumentException(
                        "COMPETITIVE_PROBLEM_ADAPTER_REVIEW_ROOT_MISMATCH");
            }
        }

        public int adapterCount() {
            return lanes.stream().mapToInt(ProblemAdapterLane::adapterCount).sum();
        }

        public boolean sourceCopyAuthority() {
            return false;
        }

        public boolean executionAuthority() {
            return false;
        }

        public boolean promotionAuthority() {
            return false;
        }
    }

    /** Canonical executable primitives implied by one problem-adapter lane. */
    public record ProblemPrimitiveLane(
            ProblemAdapterLane adapterLane,
            String primitiveCatalogRoot,
            List<ChallengeSearchPrimitiveCatalog.Primitive> primitives,
            String root) {

        public ProblemPrimitiveLane {
            adapterLane = Objects.requireNonNull(adapterLane, "adapterLane");
            primitiveCatalogRoot = hash(primitiveCatalogRoot, "primitiveCatalogRoot");
            if (!primitiveCatalogRoot.equals(ChallengeSearchPrimitiveCatalog.root())) {
                throw new IllegalArgumentException(
                        "COMPETITIVE_PROBLEM_PRIMITIVE_CATALOG_ROOT_DRIFT");
            }
            primitives = Objects.requireNonNull(primitives, "primitives").stream()
                    .sorted(Comparator.comparing(ChallengeSearchPrimitiveCatalog.Primitive::id))
                    .toList();
            var projection = ProblemAdapterCatalog.primitiveIndex()
                    .require(adapterLane.platform(), adapterLane.canonicalCategory());
            if (projection.adapterCount() != adapterLane.adapterCount()) {
                throw new IllegalArgumentException(
                        "COMPETITIVE_PROBLEM_PRIMITIVE_ADAPTER_COUNT_DRIFT");
            }
            List<ChallengeSearchPrimitiveCatalog.Primitive> expectedPrimitives =
                    projection.primitives();
            if (!primitives.equals(expectedPrimitives)) {
                throw new IllegalArgumentException(
                        "COMPETITIVE_PROBLEM_PRIMITIVE_POSTING_DRIFT");
            }
            String expected = digest(
                    "COMPETITIVE-PROBLEM-PRIMITIVE-LANE/1",
                    adapterLane.root(),
                    primitiveCatalogRoot,
                    primitives.stream()
                            .map(ChallengeSearchPrimitiveCatalog.Primitive::root)
                            .toList()
                            .toString());
            root = root == null || root.isBlank() ? expected : hash(root, "root");
            if (!root.equals(expected)) {
                throw new IllegalArgumentException(
                        "COMPETITIVE_PROBLEM_PRIMITIVE_LANE_ROOT_MISMATCH");
            }
        }

        public ChallengePlatform platform() {
            return adapterLane.platform();
        }

        public boolean sourceCopyAuthority() {
            return false;
        }

        public boolean executionAuthority() {
            return false;
        }
    }

    /** Per-problem adapter evidence joined to existing canonical Java/JNI search primitives. */
    public record ProblemPrimitiveReview(
            String problemEvidenceReviewRoot,
            ProblemAdapterReview adapterReview,
            String primitiveCatalogRoot,
            List<ProblemPrimitiveLane> lanes,
            String root) {

        public ProblemPrimitiveReview {
            problemEvidenceReviewRoot =
                    hash(problemEvidenceReviewRoot, "problemEvidenceReviewRoot");
            adapterReview = Objects.requireNonNull(adapterReview, "adapterReview");
            primitiveCatalogRoot = hash(primitiveCatalogRoot, "primitiveCatalogRoot");
            if (!problemEvidenceReviewRoot.equals(adapterReview.problemEvidenceReviewRoot())
                    || !primitiveCatalogRoot.equals(ChallengeSearchPrimitiveCatalog.root())) {
                throw new IllegalArgumentException(
                        "COMPETITIVE_PROBLEM_PRIMITIVE_REVIEW_PARENT_DRIFT");
            }
            lanes = List.copyOf(Objects.requireNonNull(lanes, "lanes"));
            if (!lanes.stream().map(ProblemPrimitiveLane::platform).toList().equals(REVIEW_ORDER)
                    || lanes.size() != adapterReview.lanes().size()) {
                throw new IllegalArgumentException(
                        "COMPETITIVE_PROBLEM_PRIMITIVE_REVIEW_ORDER");
            }
            for (int index = 0; index < lanes.size(); index++) {
                if (!lanes.get(index).adapterLane().equals(adapterReview.lanes().get(index))) {
                    throw new IllegalArgumentException(
                            "COMPETITIVE_PROBLEM_PRIMITIVE_REVIEW_LANE_DRIFT");
                }
            }
            String expected = digest(
                    "COMPETITIVE-PROBLEM-PRIMITIVE-REVIEW/1",
                    problemEvidenceReviewRoot,
                    adapterReview.root(),
                    primitiveCatalogRoot,
                    lanes.stream().map(ProblemPrimitiveLane::root).toList().toString());
            root = root == null || root.isBlank() ? expected : hash(root, "root");
            if (!root.equals(expected)) {
                throw new IllegalArgumentException(
                        "COMPETITIVE_PROBLEM_PRIMITIVE_REVIEW_ROOT_MISMATCH");
            }
        }

        public int primitiveCount() {
            return lanes.stream().mapToInt(lane -> lane.primitives().size()).sum();
        }

        /** Exact root of the precomputed adapter-to-primitive index used by all lanes. */
        public String adapterPrimitiveIndexRoot() {
            return ProblemAdapterCatalog.primitiveIndexSnapshot().root();
        }

        /** Deterministic Java/JNI execution candidates derived from this exact problem review. */
        public ProblemPrimitiveExecutionPlan executionPlan() {
            List<ChallengeSearchPrimitiveCatalog.Primitive> primitives = lanes.stream()
                    .flatMap(lane -> lane.primitives().stream())
                    .toList();
            List<PrimitiveExecutionStep> steps =
                    PrimitiveExecutionIndex.canonical().forPrimitives(primitives);
            return new ProblemPrimitiveExecutionPlan(root, steps, "");
        }

        /** Content root of the canonical precomputed primitive execution-step index. */
        public String primitiveExecutionIndexRoot() {
            return PrimitiveExecutionIndex.canonical().root();
        }

        public boolean sourceCopyAuthority() {
            return false;
        }

        public boolean executionAuthority() {
            return false;
        }

        public boolean promotionAuthority() {
            return false;
        }
    }

    /** Execution candidate plan for primitives derived from one per-problem adapter review. */
    public record ProblemPrimitiveExecutionPlan(
            String problemPrimitiveReviewRoot,
            List<PrimitiveExecutionStep> steps,
            String root) {

        public ProblemPrimitiveExecutionPlan {
            problemPrimitiveReviewRoot =
                    hash(problemPrimitiveReviewRoot, "problemPrimitiveReviewRoot");
            steps = Objects.requireNonNull(steps, "steps").stream()
                    .sorted(Comparator.comparing(PrimitiveExecutionStep::primitiveId))
                    .toList();
            if (steps.stream()
                    .map(PrimitiveExecutionStep::primitiveId)
                    .distinct()
                    .count() != steps.size()) {
                throw new IllegalArgumentException(
                        "COMPETITIVE_PROBLEM_PRIMITIVE_EXECUTION_DUPLICATE");
            }
            String expected = digest(
                    "COMPETITIVE-PROBLEM-PRIMITIVE-EXECUTION-PLAN/1",
                    problemPrimitiveReviewRoot,
                    steps.stream().map(PrimitiveExecutionStep::root).toList().toString());
            root = root == null || root.isBlank() ? expected : hash(root, "root");
            if (!root.equals(expected)) {
                throw new IllegalArgumentException(
                        "COMPETITIVE_PROBLEM_PRIMITIVE_EXECUTION_ROOT_MISMATCH");
            }
        }

        public boolean sourceCopyAuthority() {
            return false;
        }

        public boolean executionAuthority() {
            return false;
        }

        public boolean promotionAuthority() {
            return false;
        }
    }

    /**
     * Content-addressed executable-candidate projection for one tri-platform taxonomy review.
     *
     * <p>The parent review root is intentionally unchanged for backward compatibility. This
     * separate receipt binds it to the canonical executable primitive catalogue and sorted
     * primitive roots. It remains candidate-only evidence and grants no source-copy, substitution
     * or promotion authority.</p>
     */
    public record ExecutablePrimitiveReview(
            String competitiveReviewRoot,
            String primitiveCatalogRoot,
            List<ChallengeSearchPrimitiveCatalog.Primitive> primitives,
            String root) {

        public ExecutablePrimitiveReview {
            competitiveReviewRoot = hash(competitiveReviewRoot, "competitiveReviewRoot");
            primitiveCatalogRoot = hash(primitiveCatalogRoot, "primitiveCatalogRoot");
            if (!primitiveCatalogRoot.equals(ChallengeSearchPrimitiveCatalog.root())) {
                throw new IllegalArgumentException(
                        "COMPETITIVE_EXECUTABLE_PRIMITIVE_CATALOG_ROOT_DRIFT");
            }
            primitives = Objects.requireNonNull(primitives, "primitives").stream()
                    .sorted(Comparator.comparing(ChallengeSearchPrimitiveCatalog.Primitive::id))
                    .toList();
            if (primitives.stream()
                    .map(ChallengeSearchPrimitiveCatalog.Primitive::id)
                    .distinct()
                    .count() != primitives.size()) {
                throw new IllegalArgumentException("COMPETITIVE_EXECUTABLE_PRIMITIVE_DUPLICATE");
            }
            for (ChallengeSearchPrimitiveCatalog.Primitive primitive : primitives) {
                if (!ChallengeSearchPrimitiveCatalog.require(primitive.id()).equals(primitive)) {
                    throw new IllegalArgumentException(
                            "COMPETITIVE_EXECUTABLE_PRIMITIVE_NON_CANONICAL");
                }
            }
            String expected = digest(
                    "COMPETITIVE-EXECUTABLE-PRIMITIVE-REVIEW/1",
                    competitiveReviewRoot,
                    primitiveCatalogRoot,
                    primitives.stream()
                            .map(ChallengeSearchPrimitiveCatalog.Primitive::root)
                            .toList()
                            .toString());
            root = root == null || root.isBlank() ? expected : hash(root, "root");
            if (!root.equals(expected)) {
                throw new IllegalArgumentException(
                        "COMPETITIVE_EXECUTABLE_PRIMITIVE_ROOT_MISMATCH");
            }
        }

        public boolean sourceCopyAuthority() {
            return false;
        }

        public boolean substitutionAuthority() {
            return false;
        }

        public boolean promotionAuthority() {
            return false;
        }
    }

    /** Explicit execution preference derived only from a canonical primitive's existing ABI. */
    public enum PrimitiveExecutionLane {
        JAVA_ONLY,
        JAVA_PRIMARY_JNI_OPTIONAL
    }

    /** One immutable, canonical Java/JNI execution candidate. */
    public record PrimitiveExecutionStep(
            String primitiveId,
            String primitiveRoot,
            ChallengeSearchPrimitiveCatalog.Representation representation,
            boolean prepared,
            String javaOwner,
            String javaEntryPoint,
            String nativeSymbol,
            PrimitiveExecutionLane lane,
            String root) {

        public PrimitiveExecutionStep {
            primitiveId = token(primitiveId, "primitiveId");
            primitiveRoot = hash(primitiveRoot, "primitiveRoot");
            representation = Objects.requireNonNull(representation, "representation");
            javaOwner = token(javaOwner, "javaOwner");
            javaEntryPoint = token(javaEntryPoint, "javaEntryPoint");
            nativeSymbol = Objects.requireNonNullElse(nativeSymbol, "").strip();
            lane = Objects.requireNonNull(lane, "lane");

            ChallengeSearchPrimitiveCatalog.Primitive primitive =
                    ChallengeSearchPrimitiveCatalog.require(primitiveId);
            if (!primitive.root().equals(primitiveRoot)
                    || primitive.representation() != representation
                    || primitive.prepared() != prepared
                    || !primitive.javaOwner().equals(javaOwner)
                    || !primitive.javaEntryPoint().equals(javaEntryPoint)
                    || !primitive.nativeSymbol().equals(nativeSymbol)) {
                throw new IllegalArgumentException(
                        "COMPETITIVE_PRIMITIVE_EXECUTION_STEP_NON_CANONICAL");
            }
            PrimitiveExecutionLane expectedLane = primitive.nativeOptional()
                    ? PrimitiveExecutionLane.JAVA_PRIMARY_JNI_OPTIONAL
                    : PrimitiveExecutionLane.JAVA_ONLY;
            if (lane != expectedLane) {
                throw new IllegalArgumentException(
                        "COMPETITIVE_PRIMITIVE_EXECUTION_LANE_DRIFT");
            }
            String expected = digest(
                    "COMPETITIVE-PRIMITIVE-EXECUTION-STEP/1",
                    primitiveId,
                    primitiveRoot,
                    representation.name(),
                    Boolean.toString(prepared),
                    javaOwner,
                    javaEntryPoint,
                    nativeSymbol,
                    lane.name());
            root = root == null || root.isBlank() ? expected : hash(root, "root");
            if (!root.equals(expected)) {
                throw new IllegalArgumentException(
                        "COMPETITIVE_PRIMITIVE_EXECUTION_STEP_ROOT_MISMATCH");
            }
        }

        public boolean sourceCopyAuthority() {
            return false;
        }

        public boolean executionAuthority() {
            return false;
        }
    }

    /** Deterministic execution candidate plan over an executable primitive review. */
    public record PrimitiveExecutionPlan(
            String executablePrimitiveReviewRoot,
            List<PrimitiveExecutionStep> steps,
            String root) {

        public PrimitiveExecutionPlan {
            executablePrimitiveReviewRoot =
                    hash(executablePrimitiveReviewRoot, "executablePrimitiveReviewRoot");
            steps = Objects.requireNonNull(steps, "steps").stream()
                    .sorted(Comparator.comparing(PrimitiveExecutionStep::primitiveId))
                    .toList();
            if (steps.stream()
                    .map(PrimitiveExecutionStep::primitiveId)
                    .distinct()
                    .count() != steps.size()) {
                throw new IllegalArgumentException(
                        "COMPETITIVE_PRIMITIVE_EXECUTION_PLAN_DUPLICATE");
            }
            String expected = digest(
                    "COMPETITIVE-PRIMITIVE-EXECUTION-PLAN/1",
                    executablePrimitiveReviewRoot,
                    steps.stream().map(PrimitiveExecutionStep::root).toList().toString());
            root = root == null || root.isBlank() ? expected : hash(root, "root");
            if (!root.equals(expected)) {
                throw new IllegalArgumentException(
                        "COMPETITIVE_PRIMITIVE_EXECUTION_PLAN_ROOT_MISMATCH");
            }
        }

        public boolean sourceCopyAuthority() {
            return false;
        }

        public boolean executionAuthority() {
            return false;
        }

        public boolean promotionAuthority() {
            return false;
        }
    }

    public record Review(
            String recipeFamily,
            List<Lane> lanes,
            List<AlgorithmShape> commonShapes,
            List<ProblemOptimizationCatalog.Plan> commonPlans,
            String root) {

        public Review {
            recipeFamily = recipe(recipeFamily);
            lanes = List.copyOf(Objects.requireNonNull(lanes, "lanes"));
            if (!lanes.stream().map(Lane::platform).toList().equals(REVIEW_ORDER)) {
                throw new IllegalArgumentException("COMPETITIVE_REVIEW_ORDER");
            }
            commonShapes = Objects.requireNonNull(commonShapes, "commonShapes").stream()
                    .sorted(Comparator.comparing(Enum::name))
                    .distinct()
                    .toList();
            commonPlans = Objects.requireNonNull(commonPlans, "commonPlans").stream()
                    .sorted(Comparator.comparing(plan -> plan.shape().name()))
                    .toList();
            if (!commonPlans.stream()
                    .map(ProblemOptimizationCatalog.Plan::shape)
                    .toList()
                    .equals(commonShapes)) {
                throw new IllegalArgumentException("COMPETITIVE_REVIEW_COMMON_PLAN_DRIFT");
            }
            String expected = digest(
                    "COMPETITIVE-REVIEW/1",
                    recipeFamily,
                    ChallengeCategoryCatalog.root(),
                    ProblemOptimizationCatalog.root(),
                    lanes.stream().map(Lane::root).toList().toString(),
                    commonPlans.stream()
                            .map(ProblemOptimizationCatalog.Plan::root)
                            .toList()
                            .toString());
            root = root == null || root.isBlank() ? expected : hash(root, "root");
            if (!root.equals(expected)) {
                throw new IllegalArgumentException("COMPETITIVE_REVIEW_ROOT_MISMATCH");
            }
        }

        /** Precomputed 128-bit intersection of the three canonical category capability masks. */
        public AlgorithmShapeMask commonCapabilityMask() {
            return CompetitiveProblemReview.commonCapabilityMask(lanes);
        }

        /** Content root of the canonical tri-platform category capability index used by this review. */
        public String categoryCapabilityIndexRoot() {
            return ChallengeCategoryCapabilityIndex.canonical().root();
        }

        /** Content root of the canonical precomputed category-to-review-lane index. */
        public String categoryReviewLaneIndexRoot() {
            return CompetitiveReviewLaneIndex.canonical().root();
        }

        /** Content root of the canonical precomputed category-to-primitive join. */
        public String categoryPrimitiveIndexRoot() {
            return ChallengeCategoryPrimitiveIndex.canonical().root();
        }

        /** Plans shared by all three site taxonomies and relevant to indexed/search mechanics. */
        public List<ProblemOptimizationCatalog.Plan> fastSearchPlans() {
            return commonPlans.stream()
                    .filter(CompetitiveProblemReview::isFastSearchPlan)
                    .toList();
        }

        /**
         * Existing executable Java/JNI candidates for common fast-search shapes.
         *
         * <p>Only shapes shared by all three taxonomies participate. A classified shape with no
         * registered primitive remains absent rather than manufacturing another implementation.</p>
         */
        public ExecutablePrimitiveReview executableFastSearchPrimitives() {
            AlgorithmShapeMask common = commonCapabilityMask();
            ChallengeCategoryPrimitiveIndex index = ChallengeCategoryPrimitiveIndex.canonical();
            java.util.TreeMap<String, ChallengeSearchPrimitiveCatalog.Primitive> primitives =
                    new java.util.TreeMap<>();
            java.util.TreeMap<String, Integer> laneCounts = new java.util.TreeMap<>();
            for (Lane lane : lanes) {
                ChallengeCategoryPrimitiveIndex.Row row =
                        index.require(lane.platform(), lane.categoryId());
                for (ChallengeSearchPrimitiveCatalog.Primitive primitive : row.primitives()) {
                    if (primitive.shapes().stream().noneMatch(common::contains)) continue;
                    primitives.putIfAbsent(primitive.id(), primitive);
                    laneCounts.merge(primitive.id(), 1, Math::addExact);
                }
            }
            List<ChallengeSearchPrimitiveCatalog.Primitive> shared =
                    primitives.values().stream()
                            .filter(primitive ->
                                    laneCounts.getOrDefault(primitive.id(), 0) == lanes.size())
                            .toList();
            return new ExecutablePrimitiveReview(
                    root,
                    ChallengeSearchPrimitiveCatalog.root(),
                    shared,
                    "");
        }

        /** Deterministic Java-first plan over the already-admitted executable primitives. */
        public PrimitiveExecutionPlan primitiveExecutionPlan() {
            ExecutablePrimitiveReview executable = executableFastSearchPrimitives();
            List<PrimitiveExecutionStep> steps =
                    PrimitiveExecutionIndex.canonical().forPrimitives(executable.primitives());
            return new PrimitiveExecutionPlan(executable.root(), steps, "");
        }

        /** Content root of the canonical precomputed primitive execution-step index. */
        public String primitiveExecutionIndexRoot() {
            return PrimitiveExecutionIndex.canonical().root();
        }

        /** Existing provider owner: optional JNI first, deterministic Java fallback always present. */
        public SearchEngine preferredSearchEngine() {
            return SearchEngines.best();
        }

        public boolean sourceCopyAuthority() {
            return false;
        }

        public boolean substitutionAuthority() {
            return false;
        }

        public boolean promotionAuthority() {
            return false;
        }
    }

    private CompetitiveProblemReview() {}

    /**
     * Review one semantically intended category through the three independent site taxonomies.
     *
     * <p>Review order is fixed: LeetCode, HackerRank, GeeksforGeeks. The common-shape intersection
     * is candidate evidence only; it is not a proof that the challenge contracts are equivalent.</p>
     */
    public static Review compare(
            String recipeFamily,
            String leetCodeCategory,
            String hackerRankCategory,
            String geeksForGeeksCategory) {
        List<Lane> lanes = List.of(
                lane(ChallengePlatform.LEETCODE, leetCodeCategory),
                lane(ChallengePlatform.HACKERRANK, hackerRankCategory),
                lane(ChallengePlatform.GEEKSFORGEEKS, geeksForGeeksCategory));
        AlgorithmShapeMask common = commonCapabilityMask(lanes);
        List<AlgorithmShape> shapes = common.shapes();
        List<ProblemOptimizationCatalog.Plan> plans =
                shapes.stream().map(ProblemOptimizationCatalog::plan).toList();
        return new Review(recipeFamily, lanes, shapes, plans, "");
    }


    /**
     * Review independently collected per-problem metadata in the same fixed platform order.
     *
     * <p>This is an indexed inventory pass only. Public-site rows are required to remain
     * evidence-only and never grant source-copy, substitution, or promotion authority.</p>
     */
    public static ProblemEvidenceReview reviewProblems(
            String recipeFamily,
            ProblemCatalogue catalogue,
            ProblemCategory category,
            String text,
            int perPlatformLimit) {
        ProblemCatalogue checkedCatalogue = Objects.requireNonNull(catalogue, "catalogue");
        ProblemCategory checkedCategory = Objects.requireNonNull(category, "category");
        if (perPlatformLimit < 1 || perPlatformLimit > 4096) {
            throw new IllegalArgumentException("COMPETITIVE_PROBLEM_REVIEW_LIMIT");
        }
        String checkedQuery = query(text);
        ProblemCatalogue.PreparedQuery prepared =
                prepareProblemReviewQuery(checkedCatalogue, checkedCategory, checkedQuery);
        List<ProblemEvidenceLane> lanes = REVIEW_ORDER.stream()
                .map(platform -> problemLane(
                        platform,
                        checkedCatalogue,
                        checkedCategory,
                        checkedQuery,
                        prepared,
                        perPlatformLimit))
                .toList();
        return new ProblemEvidenceReview(
                recipeFamily,
                checkedCategory,
                checkedQuery,
                checkedCatalogue.root(),
                lanes,
                "");
    }

    public static List<ChallengePlatform> reviewOrder() {
        return REVIEW_ORDER;
    }


    static ProblemCatalogue.PreparedQuery prepareProblemReviewQuery(
            ProblemCatalogue catalogue,
            ProblemCategory category,
            String text) {
        ProblemCatalogue checkedCatalogue = Objects.requireNonNull(catalogue, "catalogue");
        ProblemCategory checkedCategory = Objects.requireNonNull(category, "category");
        return checkedCatalogue.prepare(query(text), Set.of(checkedCategory));
    }

    private static ProblemEvidenceLane problemLane(
            ChallengePlatform platform,
            ProblemCatalogue catalogue,
            ProblemCategory category,
            String query,
            ProblemCatalogue.PreparedQuery prepared,
            int limit) {
        ProblemSource source = problemSource(platform);
        List<ProblemDescriptor> problems =
                catalogue.execute(prepared, source, limit);
        return new ProblemEvidenceLane(
                platform,
                source,
                category,
                query,
                problems,
                catalogue.root(),
                "");
    }

    private static ProblemAdapterLane problemAdapterLane(
            ChallengePlatform platform,
            ProblemCategory problemCategory,
            CompetitiveProblemCategory canonicalCategory,
            String adapterIndexRoot) {
        ProblemSource source = problemSource(platform);
        int count =
                ProblemAdapterCatalog.byPlatformCategory(platform.name(), canonicalCategory).size();
        return new ProblemAdapterLane(
                platform,
                source,
                problemCategory,
                canonicalCategory,
                adapterIndexRoot,
                count,
                "");
    }

    static CompetitiveProblemCategory canonicalProblemCategory(ProblemCategory category) {
        return switch (Objects.requireNonNull(category, "category")) {
            case MULTI_PATTERN_SEARCH, AUTOMATA -> CompetitiveProblemCategory.STRINGS;
            case ANAGRAM_WINDOW, ONE_MISMATCH_WINDOW ->
                    CompetitiveProblemCategory.SLIDING_WINDOW;
            case ANAGRAM_PAIRS -> CompetitiveProblemCategory.HASHING;
            default -> CompetitiveProblemCategory.valueOf(category.name());
        };
    }

    private static ProblemSource problemSource(ChallengePlatform platform) {
        return switch (Objects.requireNonNull(platform, "platform")) {
            case LEETCODE -> ProblemSource.LEETCODE;
            case HACKERRANK -> ProblemSource.HACKERRANK;
            case GEEKSFORGEEKS -> ProblemSource.GEEKSFORGEEKS;
            case OTHER -> throw new IllegalArgumentException(
                    "OTHER has no public problem-metadata lane");
        };
    }

    private static Lane lane(ChallengePlatform platform, String label) {
        try {
            return CompetitiveReviewLaneIndex.canonical().require(platform, label);
        } catch (IllegalArgumentException unknown) {
            throw new IllegalArgumentException(
                    "UNKNOWN_COMPETITIVE_CATEGORY:"
                            + platform.name()
                            + ":"
                            + Objects.toString(label, ""),
                    unknown);
        }
    }

    private static AlgorithmShapeMask commonCapabilityMask(List<Lane> lanes) {
        if (lanes.size() != REVIEW_ORDER.size()) {
            throw new IllegalArgumentException("COMPETITIVE_REVIEW_MASK_LANE_COUNT");
        }
        ChallengeCategoryCapabilityIndex index = ChallengeCategoryCapabilityIndex.canonical();
        AlgorithmShapeMask common = index
                .require(lanes.getFirst().platform(), lanes.getFirst().categoryId())
                .capabilityMask();
        for (Lane lane : lanes.subList(1, lanes.size())) {
            common = common.intersection(
                    index.require(lane.platform(), lane.categoryId()).capabilityMask());
        }
        return common;
    }

    private static ProblemPrimitiveLane problemPrimitiveLane(ProblemAdapterLane adapterLane) {
        var projection = ProblemAdapterCatalog.primitiveIndex()
                .require(adapterLane.platform(), adapterLane.canonicalCategory());
        if (projection.adapterCount() != adapterLane.adapterCount()) {
            throw new IllegalStateException(
                    "COMPETITIVE_PROBLEM_PRIMITIVE_ADAPTER_POSTING_DRIFT");
        }
        return new ProblemPrimitiveLane(
                adapterLane,
                ChallengeSearchPrimitiveCatalog.root(),
                projection.primitives(),
                "");
    }

    private static boolean isFastSearchPlan(ProblemOptimizationCatalog.Plan plan) {
        return plan.kernels().stream().anyMatch(FAST_SEARCH_KERNELS::contains);
    }


    private static String query(String value) {
        String checked = Objects.toString(value, "").strip();
        if (checked.length() > 4096 || checked.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("COMPETITIVE_PROBLEM_QUERY");
        }
        return checked;
    }

    private static String recipe(String value) {
        String checked = token(value, "recipeFamily").toLowerCase(Locale.ROOT);
        if (!checked.matches("[a-z0-9][a-z0-9._-]{0,127}")) {
            throw new IllegalArgumentException("COMPETITIVE_REVIEW_RECIPE_FAMILY");
        }
        return checked;
    }

    private static String token(String value, String field) {
        String checked = Objects.requireNonNull(value, field).trim();
        if (checked.isEmpty()
                || checked.length() > 4096
                || checked.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("COMPETITIVE_REVIEW_" + field);
        }
        return checked;
    }

    private static String hash(String value, String field) {
        if (value == null || !value.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("COMPETITIVE_REVIEW_" + field);
        }
        return value;
    }

    private static String digest(String... fields) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String field : fields) {
                byte[] bytes = Objects.requireNonNull(field, "field").getBytes(StandardCharsets.UTF_8);
                digest.update(Integer.toString(bytes.length).getBytes(StandardCharsets.US_ASCII));
                digest.update((byte) ':');
                digest.update(bytes);
                digest.update((byte) '\n');
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }
}
