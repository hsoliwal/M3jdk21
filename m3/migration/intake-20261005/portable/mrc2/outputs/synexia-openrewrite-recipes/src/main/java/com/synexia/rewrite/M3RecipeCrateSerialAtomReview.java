// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.synexia.job.IProgressMonitor;
import com.synexia.m3.contract.M3Invariant;
import com.synexia.m3.contract.SerialAtomReview;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Bounded bridge from an admitted recipe crate to the existing serial atom convergence engine.
 *
 * <p>This class produces candidate source only. It never writes the repository and never grants
 * canonical promotion authority.</p>
 */
public final class M3RecipeCrateSerialAtomReview {
    public record Receipt(
            String taskId,
            String sourcePath,
            String crateRoot,
            String invariantRoot,
            String serialResultRoot,
            int acceptedPasses,
            boolean stable,
            String root) {
        public Receipt {
            taskId = text(taskId, "taskId");
            sourcePath = text(sourcePath, "sourcePath");
            crateRoot = sha(crateRoot, "crateRoot");
            invariantRoot = sha(invariantRoot, "invariantRoot");
            serialResultRoot = sha(serialResultRoot, "serialResultRoot");
            if (acceptedPasses < 0) throw new IllegalArgumentException("acceptedPasses");
            String expected = digest(
                    "M3_RECIPE_CRATE_SERIAL_ATOM_REVIEW_V1",
                    taskId, sourcePath, crateRoot, invariantRoot, serialResultRoot,
                    Integer.toString(acceptedPasses), Boolean.toString(stable),
                    "sourceMutationAuthority=false", "promotionAuthority=false");
            root = root == null || root.isBlank() ? expected : sha(root, "root");
            if (!expected.equals(root)) throw new IllegalArgumentException("receipt root mismatch");
        }
        public boolean sourceMutationAuthority() { return false; }
        public boolean promotionAuthority() { return false; }
    }

    public record ReviewedCandidate(String source, SerialAtomReview.Result serialResult, Receipt receipt) {
        public ReviewedCandidate {
            source = Objects.requireNonNull(source, "source");
            serialResult = Objects.requireNonNull(serialResult, "serialResult");
            receipt = Objects.requireNonNull(receipt, "receipt");
        }
    }

    /** The exact three-site catalogue comparison that preceded one serial file-atom candidate. */
    public record CompetitiveReviewedCandidate(
            M3CompetitiveProblemCrateEvidence evidence,
            ReviewedCandidate candidate,
            String root) {
        public CompetitiveReviewedCandidate {
            evidence = Objects.requireNonNull(evidence, "evidence");
            candidate = Objects.requireNonNull(candidate, "candidate");
            if (!M3LlmTaskRecipeCrateCodec.root(evidence.problemEvidence().crate())
                    .equals(candidate.receipt().crateRoot())) {
                throw new IllegalArgumentException("competitive review belongs to another recipe crate");
            }
            String expected = digest(
                    "M3_COMPETITIVE_SERIAL_ATOM_CANDIDATE_V1",
                    evidence.root(), evidence.review().catalogueRoot(),
                    candidate.receipt().root(),
                    "sourceMutationAuthority=false", "promotionAuthority=false");
            root = root == null || root.isBlank() ? expected : sha(root, "root");
            if (!expected.equals(root)) {
                throw new IllegalArgumentException("competitive serial atom root mismatch");
            }
        }
        public boolean sourceMutationAuthority() { return false; }
        public boolean promotionAuthority() { return false; }
    }

    private final SerialAtomReview serial = new SerialAtomReview();

    /**
     * Executes only admitted OpenRewrite passes, after binding all three catalogue lanes and
     * exact pinned Git donor evidence to this task. It returns a candidate, never a file edit.
     */
    public CompetitiveReviewedCandidate reviewCompetitiveOpenRewrite(
            M3CompetitiveProblemCrateEvidence evidence,
            Path sourcePath,
            String baselineSource,
            M3Invariant invariant,
            List<Path> classpath,
            List<M3TranspilePass> passes,
            int maxPasses,
            IProgressMonitor monitor) throws Exception {
        M3CompetitiveProblemCrateEvidence checked =
                Objects.requireNonNull(evidence, "evidence");
        if (!checked.problemEvidence().donorEvidence().allRepositoriesPinned()) {
            throw new IllegalArgumentException("unresolved Git donors cannot enter atom review");
        }
        ReviewedCandidate candidate = reviewOpenRewrite(
                checked.problemEvidence().crate(), sourcePath, baselineSource, invariant,
                classpath, passes, maxPasses, monitor);
        return new CompetitiveReviewedCandidate(checked, candidate, "");
    }

    public ReviewedCandidate reviewOpenRewrite(
            M3LlmTaskRecipeCrate crate,
            Path sourcePath,
            String baselineSource,
            M3Invariant invariant,
            List<Path> classpath,
            List<M3TranspilePass> passes,
            int maxPasses,
            IProgressMonitor monitor) throws Exception {
        requireAdmittedPasses(crate, passes);
        return review(
                crate, sourcePath, baselineSource, invariant,
                new M3OpenRewriteSerialCandidateProducer(
                        sourcePath.toString().replace('\\', '/'), classpath, passes),
                maxPasses, monitor);
    }

    public ReviewedCandidate review(
            M3LlmTaskRecipeCrate crate,
            Path sourcePath,
            String baselineSource,
            M3Invariant invariant,
            SerialAtomReview.CandidateProducer producer,
            int maxPasses,
            IProgressMonitor monitor) throws Exception {
        M3LlmTaskRecipeCrate checked = Objects.requireNonNull(crate, "crate");
        Path path = Objects.requireNonNull(sourcePath, "sourcePath").normalize();
        String portable = path.toString().replace('\\', '/');
        if (checked.disposition() == M3LlmTaskRecipeCrate.Disposition.CREATE_OR_IMPROVE_RECIPE) {
            throw new IllegalArgumentException(
                    "catalogue miss must improve one reusable recipe before target atom review");
        }
        if (!checked.targetPaths().contains(portable)) {
            throw new IllegalArgumentException("source path is outside the recipe crate target set");
        }
        if (maxPasses < 0 || maxPasses > 1_000) {
            throw new IllegalArgumentException(
                    "maxPasses must be 0 (until stable) or in [1,1000]");
        }

        M3Invariant locked = Objects.requireNonNull(invariant, "invariant");
        SerialAtomReview.Result result = serial.converge(
                path,
                Objects.requireNonNull(baselineSource, "baselineSource"),
                locked,
                Objects.requireNonNull(producer, "producer"),
                maxPasses,
                monitor == null ? IProgressMonitor.noop() : monitor);
        if (!locked.compatibleWith(result.invariant())) {
            throw new IllegalStateException("serial atom review changed the locked M3 invariant");
        }
        int accepted = Math.toIntExact(
                result.decisions().stream().filter(SerialAtomReview.Decision::accepted).count());
        boolean stable = !result.decisions().isEmpty()
                && result.decisions().getLast().status() == SerialAtomReview.Status.STABLE;
        Receipt receipt = new Receipt(
                checked.taskId(), portable, M3LlmTaskRecipeCrateCodec.root(checked),
                locked.root(), result.root(), accepted, stable, "");
        return new ReviewedCandidate(result.source(), result, receipt);
    }

    /** Result of the strict lane. Exhausting a budget is explicitly not a fixed point. */
    public record SourceBoundCandidate(ReviewedCandidate candidate,
            List<M3SourceCoverageGate.Result> coverage, boolean fixedPoint) {
        public SourceBoundCandidate {
            candidate = Objects.requireNonNull(candidate, "candidate");
            coverage = List.copyOf(coverage);
            boolean actual = candidate.receipt().stable() && !coverage.isEmpty()
                    && coverage.getLast().status() == M3SourceCoverageGate.Status.NO_OP;
            if (actual != fixedPoint) throw new IllegalArgumentException("fixed point mismatch");
        }
        public boolean promotionAuthority() { return false; }
        public boolean sourceMutationAuthority() { return false; }
    }

    /**
     * Exact algorithm-atom custody over one source-bound serial review.
     *
     * <p>Catalogued shapes must carry the exact mechanical donor binding. Shapes absent from the
     * mechanical catalogue retain the existing source-bound path with an empty binding.</p>
     */
    public record AtomSourceBoundCandidate(
            M3AlgorithmAtomRecipeCrateBinding atomBinding,
            Optional<M3AlgorithmAtomMechanicalDonorBinding> mechanicalBinding,
            SourceBoundCandidate candidate,
            String coverageRoot,
            String root) {
        public AtomSourceBoundCandidate {
            atomBinding = Objects.requireNonNull(atomBinding, "atomBinding");
            mechanicalBinding =
                    Objects.requireNonNull(mechanicalBinding, "mechanicalBinding");
            candidate = Objects.requireNonNull(candidate, "candidate");
            coverageRoot = sha(coverageRoot, "coverageRoot");
            String expectedCoverageRoot = sourceBoundCoverageRoot(candidate.coverage());
            if (!coverageRoot.equals(expectedCoverageRoot)) {
                throw new IllegalArgumentException("source-bound coverage root mismatch");
            }

            boolean required =
                    M3AlgorithmAtomMechanicalDonorBinding
                            .isMechanicalReviewRequired(atomBinding.shape());
            if (required != mechanicalBinding.isPresent()) {
                throw new IllegalArgumentException(
                        "catalogued algorithm atom requires exact mechanical donor binding");
            }
            if (!candidate.candidate().receipt().crateRoot().equals(atomBinding.crateRoot())
                    || !candidate.candidate().receipt().sourcePath()
                            .equals(atomBinding.atom().sourcePath())) {
                throw new IllegalArgumentException(
                        "source-bound review belongs to another algorithm atom");
            }
            final M3AlgorithmAtomRecipeCrateBinding checkedAtomBinding = atomBinding;
            mechanicalBinding.ifPresent(
                    binding -> {
                        M3RecipeFirstInvariant
                                .requireAlgorithmAtomMechanicalDonorBinding(binding);
                        if (binding.shape() != checkedAtomBinding.shape()
                                || !binding.algorithmAtomBindingRoot().equals(checkedAtomBinding.root())
                                || !binding.atomRoot().equals(checkedAtomBinding.atom().root())
                                || !binding.crateRoot().equals(checkedAtomBinding.crateRoot())) {
                            throw new IllegalArgumentException(
                                    "mechanical donor binding belongs to another algorithm atom");
                        }
                    });

            String expected =
                    digest(
                            "M3_ALGORITHM_ATOM_SOURCE_BOUND_CANDIDATE_V1",
                            atomBinding.root(),
                            mechanicalBinding
                                    .map(M3AlgorithmAtomMechanicalDonorBinding::root)
                                    .orElse("NONE"),
                            candidate.candidate().receipt().root(),
                            coverageRoot,
                            Boolean.toString(candidate.fixedPoint()),
                            "sourceMutationAuthority=false",
                            "donorSourceCopyAuthority=false",
                            "promotionAuthority=false");
            root = root == null || root.isBlank() ? expected : sha(root, "root");
            if (!root.equals(expected)) {
                throw new IllegalArgumentException(
                        "algorithm atom source-bound candidate root mismatch");
            }
        }

        public M3AlgorithmAtomMechanicalExecutedContractReceipt
                mechanicalExecutedContractReceipt() {
            return M3AlgorithmAtomMechanicalExecutedContractReceipt.admit(this);
        }

        public boolean sourceMutationAuthority() { return false; }
        public boolean donorSourceCopyAuthority() { return false; }
        public boolean promotionAuthority() { return false; }
    }

    @FunctionalInterface
    public interface PatternResolver {
        List<M3SourceCoverageGate.PatternBinding> resolve(SerialAtomReview.Request request, String candidate);
    }

    /** Exact class-backed OpenRewrite leaf, followed by the same strict source/proof boundary. */
    public SourceBoundCandidate reviewOpenRewriteSourceBound(
            M3LlmTaskRecipeCrate crate, Path sourcePath, String baselineSource, M3Invariant invariant,
            List<Path> classpath, M3TranspilePass pass, int maxPasses, IProgressMonitor monitor,
            M3SourceCoverageGate.Context context, com.synexia.iop.patterns.PatternCatalog catalogue,
            PatternResolver patterns, M3SourceCoverageGate.Verifier verifier) throws Exception {
        requireAdmittedPasses(crate, List.of(pass));
        if (!pass.recipe().getClass().getName().equals(context.recipeClass())) {
            throw new IllegalArgumentException("SOURCE_BOUND_RECIPE_IDENTITY_MISMATCH");
        }
        return reviewSourceBound(crate, sourcePath, baselineSource, invariant,
                new M3OpenRewriteSerialCandidateProducer(sourcePath.toString(), classpath, List.of(pass)),
                maxPasses, monitor, context, catalogue, patterns, verifier);
    }

    /**
     * Algorithm-atom OpenRewrite lane with exact mechanical donor custody when catalogued.
     */
    public AtomSourceBoundCandidate reviewOpenRewriteSourceBound(
            M3AlgorithmAtomRecipeCrateBinding atomBinding,
            M3LlmTaskAdmissionReceipt admission,
            Path sourcePath,
            String baselineSource,
            M3Invariant invariant,
            List<Path> classpath,
            M3TranspilePass pass,
            int maxPasses,
            IProgressMonitor monitor,
            M3SourceCoverageGate.Context context,
            com.synexia.iop.patterns.PatternCatalog catalogue,
            PatternResolver patterns,
            M3SourceCoverageGate.Verifier verifier) throws Exception {
        M3AlgorithmAtomRecipeCrateBinding atom =
                Objects.requireNonNull(atomBinding, "atomBinding");
        requireAdmittedPasses(atom.crate(), List.of(pass));
        if (!pass.recipe().getClass().getName().equals(context.recipeClass())) {
            throw new IllegalArgumentException("SOURCE_BOUND_RECIPE_IDENTITY_MISMATCH");
        }
        return reviewSourceBound(
                atom,
                admission,
                sourcePath,
                baselineSource,
                invariant,
                new M3OpenRewriteSerialCandidateProducer(
                        sourcePath.toString(), classpath, List.of(pass)),
                maxPasses,
                monitor,
                context,
                catalogue,
                patterns,
                verifier);
    }

    /**
     * Strict recipe lane; existing serial convergence owns iteration/cycles/context.
     * A maxPasses value of 0 is the canonical policy-unbounded mode: execution continues until
     * stability, rejection, cycle detection or cancellation. Positive values remain bounded
     * execution windows.
     * Missing proofs throw before an accepted serial candidate can be returned.
     */
    /**
     * Source-bound serial review for one planned algorithm atom.
     *
     * <p>The expensive review path remains unchanged. This overload adds exact donor-shape custody
     * before execution and binds the resulting source-bound coverage into one immutable receipt.</p>
     */
    public AtomSourceBoundCandidate reviewSourceBound(
            M3AlgorithmAtomRecipeCrateBinding atomBinding,
            M3LlmTaskAdmissionReceipt admission,
            Path sourcePath,
            String baselineSource,
            M3Invariant invariant,
            SerialAtomReview.CandidateProducer producer,
            int maxPasses,
            IProgressMonitor monitor,
            M3SourceCoverageGate.Context context,
            com.synexia.iop.patterns.PatternCatalog catalogue,
            PatternResolver patterns,
            M3SourceCoverageGate.Verifier verifier) throws Exception {
        M3AlgorithmAtomRecipeCrateBinding atom =
                Objects.requireNonNull(atomBinding, "atomBinding");
        atom.requireSerialReviewReady();
        Optional<M3AlgorithmAtomMechanicalDonorBinding> mechanical =
                M3AlgorithmAtomMechanicalDonorBinding.bindIfCatalogued(
                        atom, Objects.requireNonNull(admission, "admission"));
        SourceBoundCandidate reviewed =
                reviewSourceBound(
                        atom.crate(),
                        sourcePath,
                        baselineSource,
                        invariant,
                        producer,
                        maxPasses,
                        monitor,
                        context,
                        catalogue,
                        patterns,
                        verifier);
        AtomSourceBoundCandidate bound =
                new AtomSourceBoundCandidate(
                        atom,
                        mechanical,
                        reviewed,
                        sourceBoundCoverageRoot(reviewed.coverage()),
                        "");
        M3RecipeFirstInvariant.requireAlgorithmAtomSourceBoundCandidate(bound);
        return bound;
    }

    public SourceBoundCandidate reviewSourceBound(
            M3LlmTaskRecipeCrate crate, Path sourcePath, String baselineSource, M3Invariant invariant,
            SerialAtomReview.CandidateProducer producer, int maxPasses, IProgressMonitor monitor,
            M3SourceCoverageGate.Context context, com.synexia.iop.patterns.PatternCatalog catalogue,
            PatternResolver patterns, M3SourceCoverageGate.Verifier verifier) throws Exception {
        Objects.requireNonNull(context, "context");
        if (!crate.recipeIds().contains(context.recipeClass())
                || !crate.catalogueSha256().equals(context.recipeCatalogueRoot())
                || !invariant.equals(context.invariant())) {
            throw new IllegalArgumentException("SOURCE_BOUND_CONTEXT_OUTSIDE_CRATE");
        }
        if (!M3SourceCoverageGate.portable(sourcePath).equals(context.sourcePath())
                || !M3SourceCoverageGate.hash(baselineSource).equals(context.parentSourceRoot())) {
            throw new IllegalArgumentException("STALE_OR_FOREIGN_CRATE_PARENT");
        }
        IProgressMonitor checkedMonitor = monitor == null ? IProgressMonitor.noop() : monitor;
        var results = new java.util.ArrayList<M3SourceCoverageGate.Result>();
        SerialAtomReview.CandidateProducer guarded = request -> {
            var proposal = Objects.requireNonNull(producer.propose(request), "proposal");
            String next = proposal.orElse(request.source());
            var currentContext = new M3SourceCoverageGate.Context(context.sourcePath(),
                    request.sourceSha256(), context.recipeClass(), context.recipeArtifactRoot(),
                    context.recipeCatalogueRoot(), context.producerId(), context.verifierId(),
                    context.runtimeRequired(), context.invariant());
            var result = new M3SourceCoverageGate().review(sourcePath, request.source(), next, currentContext,
                    catalogue, next.equals(request.source()) ? List.of() : patterns.resolve(request, next),
                    verifier, checkedMonitor);
            if (result.status() == M3SourceCoverageGate.Status.BLOCKED) {
                throw new IllegalStateException("SOURCE_BOUND_ADMISSION_BLOCKED: " + result.reason());
            }
            results.add(result);
            return proposal;
        };
        ReviewedCandidate reviewed = review(crate, sourcePath, baselineSource, invariant, guarded,
                maxPasses, checkedMonitor);
        boolean fixed = reviewed.receipt().stable() && !results.isEmpty()
                && results.getLast().status() == M3SourceCoverageGate.Status.NO_OP;
        return new SourceBoundCandidate(reviewed, results, fixed);
    }

    private static String sourceBoundCoverageRoot(
            List<M3SourceCoverageGate.Result> coverage) {
        List<M3SourceCoverageGate.Result> checked =
                List.copyOf(Objects.requireNonNull(coverage, "coverage"));
        if (checked.isEmpty()) {
            throw new IllegalArgumentException("source-bound coverage required");
        }
        java.util.ArrayList<String> fields = new java.util.ArrayList<>();
        fields.add("M3_SOURCE_BOUND_COVERAGE_SET_V1");
        fields.add(Integer.toString(checked.size()));
        for (M3SourceCoverageGate.Result result : checked) {
            fields.add(result.status().name());
            fields.add(result.reason());
            fields.add(result.path());
            fields.add(result.beforeRoot());
            fields.add(result.afterRoot());
        }
        return digest(fields.toArray(String[]::new));
    }

    private static void requireAdmittedPasses(
            M3LlmTaskRecipeCrate crate, List<M3TranspilePass> passes) {
        M3LlmTaskRecipeCrate checked = Objects.requireNonNull(crate, "crate");
        List<M3TranspilePass> checkedPasses = List.copyOf(Objects.requireNonNull(passes, "passes"));
        if (checkedPasses.isEmpty()) throw new IllegalArgumentException("passes must not be empty");
        for (M3TranspilePass pass : checkedPasses) {
            String recipeClass = pass.recipe().getClass().getName();
            if (!checked.recipeIds().contains(recipeClass)) {
                throw new IllegalArgumentException("recipe is outside the admitted crate: " + recipeClass);
            }
        }
    }

    private static String digest(String... values) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String value : values) {
                byte[] bytes = Objects.requireNonNull(value, "value").getBytes(StandardCharsets.UTF_8);
                digest.update((byte) (bytes.length >>> 24));
                digest.update((byte) (bytes.length >>> 16));
                digest.update((byte) (bytes.length >>> 8));
                digest.update((byte) bytes.length);
                digest.update(bytes);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }

    private static String sha(String value, String field) {
        if (value == null || !value.matches("[0-9a-f]{64}")) throw new IllegalArgumentException(field);
        return value;
    }

    private static String text(String value, String field) {
        String checked = Objects.requireNonNull(value, field).strip();
        if (checked.isEmpty() || checked.indexOf('\0') >= 0) throw new IllegalArgumentException(field);
        return checked;
    }
}
