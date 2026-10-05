// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.corpus;

import com.synexia.algorithms.shapes.AlgorithmShape;
import com.synexia.job.IProgressMonitor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

/**
 * Serial pass-by-pass review ledger for LeetCode, HackerRank and GeeksforGeeks donor evidence.
 *
 * <p>The ledger joins existing challenge metadata, pinned Git provenance, declared license policy,
 * local executable primitives and optional JNI/native mechanics. It never fetches challenge sites,
 * copies donor source, executes native code or grants replacement/promotion authority.</p>
 */
public final class ChallengeDonorPassLedger {

    public enum Pass {
        IDENTITY,
        CATEGORY,
        CLASSIFICATION,
        PROVENANCE,
        LICENSE,
        EVIDENCE_CONSISTENCY,
        LOCAL_REUSE,
        NATIVE_ADMISSION,
        RECIPE_READY
    }

    public enum State {
        PASS,
        REVIEW_REQUIRED,
        BLOCKED,
        NOT_APPLICABLE
    }

    public enum NextAction {
        RESOLVE_CLASSIFICATION_CONFLICT,
        CLASSIFY_SHAPE,
        PIN_DONOR_PROVENANCE,
        VERIFY_LICENSE_BLOB,
        SERIAL_DONOR_ATOM_REVIEW,
        REUSE_LOCAL_EXECUTABLE,
        AUTHOR_CLEAN_ROOM_JAVA_RECIPE
    }

    public record PassResult(Pass pass, State state, String detail, String root) {
        public PassResult {
            pass = Objects.requireNonNull(pass, "pass");
            state = Objects.requireNonNull(state, "state");
            detail = token(detail, "detail");
            String expected =
                    new CatalogueDigest("SYNEXIA_CHALLENGE_DONOR_PASS_RESULT_V1")
                            .text(pass.name())
                            .text(state.name())
                            .text(detail)
                            .finish();
            root = root == null || root.isBlank() ? expected : sha(root, "root");
            if (!root.equals(expected)) throw new IllegalArgumentException("pass root");
        }
    }

    public record DonorOrigin(
            String repository,
            String revision,
            String path,
            String declaredLicense,
            String licenseFile,
            String licenseBlob,
            DonorLicensePromotionPolicy.Decision licenseDecision,
            boolean pinnedManifestEntry,
            boolean licenseBlobVerified,
            String root) {

        public DonorOrigin {
            repository = ChallengeDonorPassLedger.repository(repository);
            revision = sha1(revision, "revision");
            path = token(path, "path");
            declaredLicense = Objects.requireNonNullElse(declaredLicense, "").strip();
            licenseFile = Objects.requireNonNullElse(licenseFile, "").strip();
            licenseBlob = Objects.requireNonNullElse(licenseBlob, "").strip();
            licenseDecision = Objects.requireNonNull(licenseDecision, "licenseDecision");
            if (pinnedManifestEntry) {
                token(declaredLicense, "declaredLicense");
                token(licenseFile, "licenseFile");
                sha1(licenseBlob, "licenseBlob");
            } else if (!declaredLicense.isEmpty()
                    || !licenseFile.isEmpty()
                    || !licenseBlob.isEmpty()
                    || licenseBlobVerified) {
                throw new IllegalArgumentException("unpinned donor cannot carry verified license evidence");
            }
            if (licenseBlobVerified && !pinnedManifestEntry) {
                throw new IllegalArgumentException("license verification requires pinned manifest entry");
            }

            String expected =
                    new CatalogueDigest("SYNEXIA_CHALLENGE_DONOR_ORIGIN_V1")
                            .text(repository)
                            .text(revision)
                            .text(path)
                            .text(declaredLicense)
                            .text(licenseFile)
                            .text(licenseBlob)
                            .text(licenseDecision.name())
                            .text(Boolean.toString(pinnedManifestEntry))
                            .text(Boolean.toString(licenseBlobVerified))
                            .finish();
            root = root == null || root.isBlank() ? expected : sha(root, "root");
            if (!root.equals(expected)) throw new IllegalArgumentException("donor origin root");
        }

        public String identity() {
            return repository + "@" + revision + ":" + path;
        }

        public boolean sourceCopyAuthority() {
            return false;
        }
    }

    public record Row(
            int ordinal,
            String stableId,
            ChallengePlatform platform,
            String title,
            ChallengeProblem.Status challengeStatus,
            String algorithmShape,
            List<String> categoryIds,
            List<DonorOrigin> donors,
            List<String> localPrimitiveIds,
            ProblemOptimizationCatalog.NativeLane nativeLane,
            List<String> nativeDonors,
            List<String> nativeMechanics,
            List<PassResult> passes,
            NextAction nextAction,
            boolean serialPromotionRequired,
            String root) {

        public Row {
            if (ordinal < 0) throw new IllegalArgumentException("ordinal");
            stableId = token(stableId, "stableId");
            platform = concrete(platform);
            title = token(title, "title");
            challengeStatus = Objects.requireNonNull(challengeStatus, "challengeStatus");
            algorithmShape = Objects.requireNonNullElse(algorithmShape, "").strip();
            if (!algorithmShape.isEmpty()) AlgorithmShape.valueOf(algorithmShape);
            categoryIds = stableText(categoryIds, "categoryIds");
            donors =
                    Objects.requireNonNull(donors, "donors").stream()
                            .sorted(Comparator.comparing(DonorOrigin::identity))
                            .toList();
            if (donors.isEmpty()) throw new IllegalArgumentException("donors");
            localPrimitiveIds = stableText(localPrimitiveIds, "localPrimitiveIds");
            nativeLane = Objects.requireNonNull(nativeLane, "nativeLane");
            nativeDonors = stableText(nativeDonors, "nativeDonors");
            nativeMechanics = stableText(nativeMechanics, "nativeMechanics");
            passes = List.copyOf(Objects.requireNonNull(passes, "passes"));
            if (passes.size() != Pass.values().length) {
                throw new IllegalArgumentException("one result per review pass required");
            }
            for (int i = 0; i < Pass.values().length; i++) {
                if (passes.get(i).pass() != Pass.values()[i]) {
                    throw new IllegalArgumentException("review pass order");
                }
            }
            nextAction = Objects.requireNonNull(nextAction, "nextAction");
            if (!serialPromotionRequired) {
                throw new IllegalArgumentException("serial promotion is mandatory");
            }

            CatalogueDigest digest =
                    new CatalogueDigest("SYNEXIA_CHALLENGE_DONOR_PASS_LEDGER_ROW_V2")
                            .text(Integer.toString(ordinal))
                            .text(stableId)
                            .text(platform.name())
                            .text(title)
                            .text(challengeStatus.name())
                            .text(algorithmShape)
                            .text(nativeLane.name())
                            .text(nextAction.name())
                            .text(Boolean.toString(serialPromotionRequired));
            categoryIds.forEach(digest::text);
            donors.forEach(value -> digest.text(value.root()));
            localPrimitiveIds.forEach(digest::text);
            nativeDonors.forEach(digest::text);
            nativeMechanics.forEach(digest::text);
            passes.forEach(value -> digest.text(value.root()));
            String expected = digest.finish();
            root = root == null || root.isBlank() ? expected : sha(root, "root");
            if (!root.equals(expected)) throw new IllegalArgumentException("row root");
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
    }

    public record Ledger(
            List<Row> rows,
            String challengeMetadataRoot,
            String donorManifestRoot,
            String localPrimitiveRoot,
            String optimizationRoot,
            String root) {

        public Ledger {
            rows = List.copyOf(Objects.requireNonNull(rows, "rows"));
            challengeMetadataRoot = sha(challengeMetadataRoot, "challengeMetadataRoot");
            donorManifestRoot = sha(donorManifestRoot, "donorManifestRoot");
            localPrimitiveRoot = sha(localPrimitiveRoot, "localPrimitiveRoot");
            optimizationRoot = sha(optimizationRoot, "optimizationRoot");

            CatalogueDigest digest =
                    new CatalogueDigest("SYNEXIA_CHALLENGE_DONOR_PASS_LEDGER_V2")
                            .text(challengeMetadataRoot)
                            .text(donorManifestRoot)
                            .text(localPrimitiveRoot)
                            .text(optimizationRoot);
            rows.forEach(value -> digest.text(value.root()));
            String expected = digest.finish();
            root = root == null || root.isBlank() ? expected : sha(root, "root");
            if (!root.equals(expected)) throw new IllegalArgumentException("ledger root");
        }

        public boolean sourceCopyAuthority() {
            return false;
        }

        public boolean replacementAuthority() {
            return false;
        }

        public boolean promotionAuthority() {
            return false;
        }
    }

    private ChallengeDonorPassLedger() {}

    public static Ledger reviewAll(IProgressMonitor monitor) {
        return review(null, Map.of(), monitor);
    }

    /**
     * @param verifiedLicenses exact {@code repository@revision -> SPDX-ish license} entries emitted
     *     only after {@link PinnedDonorLicenseVerifier} has verified the declared license Git blob.
     */
    public static Ledger review(
            ChallengePlatform platform,
            Map<String, String> verifiedLicenses,
            IProgressMonitor suppliedMonitor) {
        if (platform == ChallengePlatform.OTHER) {
            throw new IllegalArgumentException("OTHER has no public challenge donor lane");
        }
        Map<String, String> verified =
                Map.copyOf(Objects.requireNonNull(verifiedLicenses, "verifiedLicenses"));
        IProgressMonitor monitor = suppliedMonitor == null ? IProgressMonitor.noop() : suppliedMonitor;

        List<ChallengeProblem> problems =
                (platform == null ? ChallengeCatalog.all() : ChallengeCatalog.byPlatform(platform))
                        .stream()
                        .sorted(Comparator.comparing(value -> value.id().stableId()))
                        .toList();

        monitor.beginTask("serial challenge donor pass ledger", problems.size());
        ArrayList<Row> rows = new ArrayList<>(problems.size());
        try {
            int ordinal = 0;
            for (ChallengeProblem problem : problems) {
                monitor.checkCanceled();
                monitor.subTask(problem.id().stableId());
                rows.add(row(ordinal++, problem, verified));
                monitor.worked(1L);
            }
        } finally {
            monitor.done();
        }

        return new Ledger(
                rows,
                ChallengeMetadataView.root(),
                PinnedAlgorithmDonorCatalog.snapshot().root(),
                ChallengeSearchPrimitiveCatalog.root(),
                ProblemOptimizationCatalog.root(),
                "");
    }

    public static String renderTsv(Ledger ledger) {
        Objects.requireNonNull(ledger, "ledger");
        StringBuilder out =
                new StringBuilder(
                        "ordinal\tstableId\tplatform\ttitle\tchallengeStatus\tshape"
                                + "\tcategories\tdonors\tlicenses\tlocalPrimitives\tnativeLane"
                                + "\tnativeDonors\tnativeMechanics\tpassStates\tnextAction"
                                + "\tsourceCopyAuthority\treplacementAuthority"
                                + "\tnativeExecutionAuthority\tpromotionAuthority"
                                + "\tserialPromotionRequired\trowRoot\n");
        for (Row row : ledger.rows()) {
            field(out, Integer.toString(row.ordinal()));
            field(out, row.stableId());
            field(out, row.platform().name());
            field(out, row.title());
            field(out, row.challengeStatus().name());
            field(out, row.algorithmShape());
            field(out, String.join(",", row.categoryIds()));
            field(out, row.donors().stream().map(DonorOrigin::identity).reduce("", ChallengeDonorPassLedger::comma));
            field(
                    out,
                    row.donors().stream()
                            .map(value ->
                                    value.repository()
                                            + "@"
                                            + value.revision()
                                            + "="
                                            + value.declaredLicense()
                                            + ":"
                                            + value.licenseDecision().name()
                                            + ":verified="
                                            + value.licenseBlobVerified())
                            .reduce("", ChallengeDonorPassLedger::comma));
            field(out, String.join(",", row.localPrimitiveIds()));
            field(out, row.nativeLane().name());
            field(out, String.join(",", row.nativeDonors()));
            field(out, String.join(",", row.nativeMechanics()));
            field(
                    out,
                    row.passes().stream()
                            .map(value -> value.pass().name() + "=" + value.state().name())
                            .reduce("", ChallengeDonorPassLedger::comma));
            field(out, row.nextAction().name());
            field(out, Boolean.toString(row.sourceCopyAuthority()));
            field(out, Boolean.toString(row.replacementAuthority()));
            field(out, Boolean.toString(row.nativeExecutionAuthority()));
            field(out, Boolean.toString(row.promotionAuthority()));
            field(out, Boolean.toString(row.serialPromotionRequired()));
            out.append(row.root()).append('\n');
        }
        return out.toString();
    }

    private static Row row(
            int ordinal, ChallengeProblem problem, Map<String, String> verifiedLicenses) {
        ChallengePlatform platform = concrete(problem.id().platform());
        List<DonorOrigin> donors =
                problem.sources().stream()
                        .map(source -> donor(source, verifiedLicenses))
                        .sorted(Comparator.comparing(DonorOrigin::identity))
                        .toList();

        String shape = problem.shape().map(Enum::name).orElse("");
        List<String> categories =
                problem.shape()
                        .map(
                                value ->
                                        ChallengeCategoryCatalog.forShape(value).stream()
                                                .filter(category -> category.platform() == platform)
                                                .map(ChallengeCategoryCatalog.Category::id)
                                                .sorted()
                                                .toList())
                        .orElse(List.of());
        List<String> primitives =
                problem.shape()
                        .map(
                                value ->
                                        ChallengeSearchPrimitiveCatalog.forShape(value).stream()
                                                .map(ChallengeSearchPrimitiveCatalog.Primitive::id)
                                                .sorted()
                                                .toList())
                        .orElse(List.of());

        ProblemOptimizationCatalog.NativeLane nativeLane =
                problem.shape()
                        .map(value -> ProblemOptimizationCatalog.plan(value).nativeLane())
                        .orElse(ProblemOptimizationCatalog.NativeLane.JAVA_ONLY);

        List<NativeMechanicsDonorCatalog.Match> nativeMatches =
                problem.shape()
                        .map(value -> ProblemOptimizationCatalog.plan(value).nativeMechanicsDonors())
                        .orElse(List.of());
        List<String> nativeDonors =
                nativeMatches.stream()
                        .map(
                                match ->
                                        match.donor().repository()
                                                + "@"
                                                + match.donor().revision())
                        .distinct()
                        .sorted()
                        .toList();
        List<String> nativeMechanics =
                nativeMatches.stream()
                        .flatMap(match -> match.matchedMechanics().stream())
                        .map(Enum::name)
                        .distinct()
                        .sorted()
                        .toList();

        List<PassResult> passes =
                List.of(
                        pass(Pass.IDENTITY, State.PASS, "stable challenge identity is present"),
                        pass(
                                Pass.CATEGORY,
                                categories.isEmpty() ? State.REVIEW_REQUIRED : State.PASS,
                                categories.isEmpty()
                                        ? "no platform category is resolved yet"
                                        : "platform categories=" + String.join(",", categories)),
                        classification(problem),
                        provenance(donors),
                        license(donors),
                        evidenceConsistency(problem.sources(), donors),
                        pass(
                                Pass.LOCAL_REUSE,
                                primitives.isEmpty() ? State.NOT_APPLICABLE : State.PASS,
                                primitives.isEmpty()
                                        ? "no exact local executable primitive is registered"
                                        : "local primitives=" + String.join(",", primitives)),
                        pass(
                                Pass.NATIVE_ADMISSION,
                                nativeLane == ProblemOptimizationCatalog.NativeLane.JAVA_ONLY
                                        ? State.NOT_APPLICABLE
                                        : State.REVIEW_REQUIRED,
                                nativeLane == ProblemOptimizationCatalog.NativeLane.JAVA_ONLY
                                        ? "Java-only optimization lane"
                                        : nativeLane.name()
                                                + " requires Java parity, determinism and setup-inclusive benchmark"),
                        recipeReady(problem, donors, primitives));

        return new Row(
                ordinal,
                problem.id().stableId(),
                platform,
                problem.title(),
                problem.status(),
                shape,
                categories,
                donors,
                primitives,
                nativeLane,
                nativeDonors,
                nativeMechanics,
                passes,
                nextAction(problem, donors, primitives, nativeLane),
                true,
                "");
    }

    private static DonorOrigin donor(
            CorpusSourceEntry source, Map<String, String> verifiedLicenses) {
        String key = source.repository() + "@" + source.commit();
        if (!PinnedAlgorithmDonorCatalog.contains(source.repository(), source.commit())) {
            return new DonorOrigin(
                    source.repository(),
                    source.commit(),
                    source.path(),
                    "",
                    "",
                    "",
                    DonorLicensePromotionPolicy.Decision.UNKNOWN_REVIEW_REQUIRED,
                    false,
                    false,
                    "");
        }

        PinnedAlgorithmDonorCatalog.Pin pin =
                PinnedAlgorithmDonorCatalog.require(source.repository(), source.commit());
        DonorLicensePromotionPolicy.Assessment assessment =
                DonorLicensePromotionPolicy.assess(pin.license());
        boolean verified =
                pin.license().equalsIgnoreCase(verifiedLicenses.getOrDefault(key, ""));
        return new DonorOrigin(
                source.repository(),
                source.commit(),
                source.path(),
                pin.license(),
                pin.licenseFile(),
                pin.licenseBlob(),
                assessment.decision(),
                true,
                verified,
                "");
    }

    private static PassResult classification(ChallengeProblem problem) {
        return switch (problem.status()) {
            case EXECUTABLE ->
                    pass(
                            Pass.CLASSIFICATION,
                            State.PASS,
                            "canonical shape=" + problem.shape().orElseThrow().name());
            case UNCLASSIFIED ->
                    pass(
                            Pass.CLASSIFICATION,
                            State.REVIEW_REQUIRED,
                            "shape unresolved; no implementation promotion allowed");
            case CLASSIFICATION_CONFLICT ->
                    pass(
                            Pass.CLASSIFICATION,
                            State.BLOCKED,
                            "donor implementations disagree on canonical shape");
        };
    }

    private static PassResult provenance(List<DonorOrigin> donors) {
        boolean allPinned = donors.stream().allMatch(DonorOrigin::pinnedManifestEntry);
        return pass(
                Pass.PROVENANCE,
                allPinned ? State.PASS : State.BLOCKED,
                allPinned
                        ? "all donor origins are repository/revision/path pinned"
                        : "one or more donor origins are absent from the pinned donor manifest");
    }

    private static PassResult license(List<DonorOrigin> donors) {
        if (donors.stream().anyMatch(value -> !value.pinnedManifestEntry())) {
            return pass(
                    Pass.LICENSE,
                    State.BLOCKED,
                    "license review cannot proceed until every donor origin is pinned");
        }

        boolean reciprocalOrUnknown =
                donors.stream()
                        .anyMatch(
                                value ->
                                        value.licenseDecision()
                                                != DonorLicensePromotionPolicy.Decision
                                                        .PERMISSIVE_AUTO_PROMOTION);
        if (reciprocalOrUnknown) {
            return pass(
                    Pass.LICENSE,
                    State.REVIEW_REQUIRED,
                    "one or more donor licenses are inspection/clean-room only");
        }

        boolean allVerified = donors.stream().allMatch(DonorOrigin::licenseBlobVerified);
        return pass(
                Pass.LICENSE,
                allVerified ? State.PASS : State.REVIEW_REQUIRED,
                allVerified
                        ? "all permissive donor license blobs verified at pinned revisions"
                        : "declared permissive licenses require pinned blob verification before source promotion");
    }

    private static PassResult evidenceConsistency(
            List<CorpusSourceEntry> sources,
            List<DonorOrigin> donors) {
        List<ChallengeDonorEvidenceAudit.EvidenceRow> evidence =
                Objects.requireNonNull(sources, "sources").stream()
                        .map(ChallengeDonorEvidenceAudit::assess)
                        .sorted(
                                Comparator.comparing(
                                                ChallengeDonorEvidenceAudit.EvidenceRow::repository)
                                        .thenComparing(
                                                ChallengeDonorEvidenceAudit.EvidenceRow::revision)
                                        .thenComparing(
                                                ChallengeDonorEvidenceAudit.EvidenceRow::path))
                        .toList();

        if (evidence.size() != donors.size()) {
            return pass(
                    Pass.EVIDENCE_CONSISTENCY,
                    State.BLOCKED,
                    "donor evidence/source cardinality mismatch");
        }

        boolean unpinned =
                evidence.stream()
                        .anyMatch(
                                row ->
                                        row.disposition()
                                                == ChallengeDonorEvidenceAudit.Disposition
                                                        .EVIDENCE_ONLY_UNPINNED);
        if (unpinned) {
            return pass(
                    Pass.EVIDENCE_CONSISTENCY,
                    State.BLOCKED,
                    "one or more Java donor rows are evidence-only/unpinned; "
                            + "donor source reuse is blocked");
        }

        boolean inspectionOnly =
                evidence.stream()
                        .anyMatch(
                                row ->
                                        row.disposition()
                                                == ChallengeDonorEvidenceAudit.Disposition
                                                        .PINNED_INSPECTION_ONLY);
        if (inspectionOnly) {
            return pass(
                    Pass.EVIDENCE_CONSISTENCY,
                    State.REVIEW_REQUIRED,
                    "one or more pinned donors are inspection/clean-room only");
        }

        boolean allVerified =
                donors.stream().allMatch(DonorOrigin::licenseBlobVerified);
        return pass(
                Pass.EVIDENCE_CONSISTENCY,
                allVerified ? State.PASS : State.REVIEW_REQUIRED,
                allVerified
                        ? "balanced donor evidence is pinned, permissive and license-blob verified"
                        : "pinned permissive donor evidence still requires exact license-blob verification "
                                + "before donor source reuse");
    }

    private static PassResult recipeReady(
            ChallengeProblem problem,
            List<DonorOrigin> donors,
            List<String> primitives) {
        if (problem.status() == ChallengeProblem.Status.CLASSIFICATION_CONFLICT) {
            return pass(
                    Pass.RECIPE_READY,
                    State.BLOCKED,
                    "classification conflict blocks recipe authoring");
        }
        if (problem.status() == ChallengeProblem.Status.UNCLASSIFIED) {
            return pass(
                    Pass.RECIPE_READY,
                    State.REVIEW_REQUIRED,
                    "classify shape before recipe authoring");
        }
        if (donors.stream().anyMatch(value -> !value.pinnedManifestEntry())) {
            return pass(
                    Pass.RECIPE_READY,
                    State.BLOCKED,
                    "pin donor provenance before recipe authoring");
        }
        if (!primitives.isEmpty()) {
            return pass(
                    Pass.RECIPE_READY,
                    State.PASS,
                    "compose existing local executable primitive through a recipe");
        }
        return pass(
                Pass.RECIPE_READY,
                State.PASS,
                "author clean-room Java template recipe from reviewed shape/contract evidence");
    }

    private static NextAction nextAction(
            ChallengeProblem problem,
            List<DonorOrigin> donors,
            List<String> primitives,
            ProblemOptimizationCatalog.NativeLane nativeLane) {
        if (problem.status() == ChallengeProblem.Status.CLASSIFICATION_CONFLICT) {
            return NextAction.RESOLVE_CLASSIFICATION_CONFLICT;
        }
        if (problem.status() == ChallengeProblem.Status.UNCLASSIFIED) {
            return NextAction.CLASSIFY_SHAPE;
        }
        if (donors.stream().anyMatch(value -> !value.pinnedManifestEntry())) {
            return NextAction.PIN_DONOR_PROVENANCE;
        }
        if (!primitives.isEmpty()) {
            return NextAction.REUSE_LOCAL_EXECUTABLE;
        }

        boolean reciprocalOrUnknown =
                donors.stream()
                        .anyMatch(
                                value ->
                                        value.licenseDecision()
                                                != DonorLicensePromotionPolicy.Decision
                                                        .PERMISSIVE_AUTO_PROMOTION);
        if (reciprocalOrUnknown) {
            return NextAction.AUTHOR_CLEAN_ROOM_JAVA_RECIPE;
        }

        boolean allLicenseBlobsVerified =
                donors.stream().allMatch(DonorOrigin::licenseBlobVerified);
        if (!allLicenseBlobsVerified) {
            return NextAction.VERIFY_LICENSE_BLOB;
        }

        // Exact permissive Git/license evidence may proceed only to serial atom review.
        // Source copy/replacement/promotion authority remains false even here.
        return NextAction.SERIAL_DONOR_ATOM_REVIEW;
    }

    private static PassResult pass(Pass pass, State state, String detail) {
        return new PassResult(pass, state, detail, "");
    }

    private static ChallengePlatform concrete(ChallengePlatform platform) {
        ChallengePlatform checked = Objects.requireNonNull(platform, "platform");
        if (checked == ChallengePlatform.OTHER) {
            throw new IllegalArgumentException("concrete public challenge platform required");
        }
        return checked;
    }

    private static List<String> stableText(List<String> values, String field) {
        TreeSet<String> stable = new TreeSet<>();
        for (String value : Objects.requireNonNull(values, field)) {
            stable.add(token(value, field));
        }
        return List.copyOf(stable);
    }

    private static String repository(String value) {
        String checked = token(value, "repository");
        if (!checked.matches("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")) {
            throw new IllegalArgumentException("repository");
        }
        return checked;
    }

    private static String sha1(String value, String field) {
        String checked = token(value, field).toLowerCase(Locale.ROOT);
        if (!checked.matches("[0-9a-f]{40}")) throw new IllegalArgumentException(field);
        return checked;
    }

    private static String sha(String value, String field) {
        String checked = Objects.requireNonNull(value, field);
        if (!checked.matches("[0-9a-f]{64}")) throw new IllegalArgumentException(field);
        return checked;
    }

    private static String token(String value, String field) {
        String checked = Objects.requireNonNull(value, field).strip();
        if (checked.isEmpty()
                || checked.indexOf('\0') >= 0
                || checked.indexOf('\t') >= 0
                || checked.indexOf('\n') >= 0
                || checked.indexOf('\r') >= 0) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static void field(StringBuilder out, String value) {
        out.append(
                        Objects.toString(value, "")
                                .replace('\t', ' ')
                                .replace('\n', ' ')
                                .replace('\r', ' '))
                .append('\t');
    }

    private static String comma(String left, String right) {
        return left.isEmpty() ? right : left + "," + right;
    }
}
