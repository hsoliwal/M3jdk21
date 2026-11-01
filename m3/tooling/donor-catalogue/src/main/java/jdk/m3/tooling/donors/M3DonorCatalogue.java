/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.donors;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Strict, network-free parser and policy validator for the M3 donor catalogue.
 *
 * <p>The catalogue records immutable donor revisions and evidence objects. It does not fetch donor code and does not
 * authorize source import. Import modes are policy inputs to later recipe and atom-admission work.</p>
 */
public final class M3DonorCatalogue {
    /** Exact header required for {@code DONORS.tsv}. */
    public static final String DONORS_HEADER = "donor_id\tproject_name\tfamily\trepository_url\trevision"
            + "\tsource_license\ttarget_license\tdefault_import_mode\tupstream_repository\tupstream_revision"
            + "\tavailability\tstatus";
    /** Exact header required for {@code SOURCES.tsv}. */
    public static final String SOURCES_HEADER = "source_id\tdonor_id\tsource_kind\trepository_url\trevision"
            + "\tpath_or_ref\tgit_object_sha\tdeclared_license\trole";
    /** Exact header required for {@code CAPABILITIES.tsv}. */
    public static final String CAPABILITIES_HEADER = "capability_id\tdonor_id\tcategory\tevidence_source_id"
            + "\timport_mode\ttarget_area\tstatus\tcontract_ref";
    /** Exact header required for {@code IMPORT_MODES.tsv}. */
    public static final String IMPORT_MODES_HEADER = "mode\tdirect_source_allowed\trequires_compatible_lineage"
            + "\trequires_isolation\tdescription";
    /** Header emitted by the deterministic donor plan. */
    public static final String PLAN_HEADER = "capability_id\tdonor_id\timport_mode\tsource_copy_allowed\tdecision"
            + "\tevidence_repository\tevidence_revision\tevidence_object\ttarget_area\tstatus";

    private static final Pattern ID = Pattern.compile("[a-z0-9][a-z0-9._+:/-]{0,255}");
    private static final Pattern GIT_ID = Pattern.compile("(?:[0-9a-f]{40}|[0-9a-f]{64})");
    private static final Pattern SAFE_TEXT = Pattern.compile("[^\\p{Cntrl}\\t]+");

    /** Closed donor-family vocabulary. */
    public enum DonorFamily {
        /** OpenJDK-derived source lineage suitable for exact delta analysis. */
        OPENJDK_DOWNSTREAM,
        /** Independent proprietary runtime available only as documentation or a lawful behavior oracle. */
        PROPRIETARY_RUNTIME,
        /** Public integration source that documents a runtime but is not target-licence-compatible core source. */
        PUBLIC_INTEGRATION
    }

    /** Closed import-mode vocabulary. */
    public enum ImportMode {
        /** Transform compatible source behind an unchanged contract. */
        ADAPT(true, true, false),
        /** Keep differently licensed source behind an explicit independent distribution boundary. */
        ISOLATE(true, false, true),
        /** Use behavior or documentation without importing implementation expression. */
        ORACLE_ONLY(false, false, false),
        /** Apply an exact compatible-lineage source delta after review. */
        PATCH(true, true, false),
        /** Refuse the candidate. */
        REJECT(false, false, false),
        /** Produce a specification and independently implement it. */
        SPEC_REIMPLEMENT(false, false, false);

        private final boolean directSourceAllowed;
        private final boolean compatibleLineageRequired;
        private final boolean isolationRequired;

        ImportMode(boolean directSourceAllowed, boolean compatibleLineageRequired, boolean isolationRequired) {
            this.directSourceAllowed = directSourceAllowed;
            this.compatibleLineageRequired = compatibleLineageRequired;
            this.isolationRequired = isolationRequired;
        }

        /**
         * Returns whether the mode can ever carry source expression.
         *
         * @return whether the mode can ever carry source expression
         */
        public boolean directSourceAllowed() {
            return directSourceAllowed;
        }

        /**
         * Returns whether the mode requires compatible source ancestry and licensing.
         *
         * @return whether compatible lineage is required
         */
        public boolean compatibleLineageRequired() {
            return compatibleLineageRequired;
        }

        /**
         * Returns whether the mode requires an independent distribution boundary.
         *
         * @return whether isolation is required
         */
        public boolean isolationRequired() {
            return isolationRequired;
        }
    }

    /** Source availability declared for a donor. */
    public enum Availability {
        /** Donor source is publicly available at the recorded immutable revision. */
        SOURCE_AVAILABLE,
        /** Only public documentation or lawful black-box behavior is admitted. */
        DOCUMENTATION_ONLY
    }

    /** Donor-catalogue state. */
    public enum DonorStatus {
        /** Immutable source pin is present. */
        PINNED,
        /** Donor is available only as a reference or oracle. */
        REFERENCE_ONLY,
        /** Donor is explicitly rejected. */
        REJECTED
    }

    /** Kind of immutable source evidence. */
    public enum SourceKind {
        /** Repository file at an immutable commit. */
        FILE,
        /** Exact Git ref/tag object plus dereferenced commit. */
        REF,
        /** Public integration source used as architecture evidence for a separate proprietary runtime. */
        PUBLIC_PROXY
    }

    /** Purpose of an evidence object. */
    public enum SourceRole {
        /** Public source that documents an architectural mechanism. */
        ARCHITECTURE_EVIDENCE,
        /** Source implementation surface considered for compatible-lineage review. */
        IMPLEMENTATION_EVIDENCE,
        /** Licence text or file-level licence evidence. */
        LICENSE_EVIDENCE,
        /** Project-maintained description of lineage or supported platforms. */
        PROJECT_DESCRIPTION,
        /** Exact upstream release reference. */
        UPSTREAM_RELEASE_EVIDENCE,
        /** Project version identity. */
        VERSION_EVIDENCE
    }

    /** Capability categories admitted by the initial catalogue. */
    public enum CapabilityCategory {
        /** Native executable compilation of Java application code. */
        AOT_NATIVE_IMAGE,
        /** Profiling image generated for execution on another target system. */
        CROSS_TARGET_PROFILING,
        /** Persistent compiler state supporting incremental native rebuilds. */
        INCREMENTAL_NATIVE_STATE,
        /** Persisted or restored JIT profile state. */
        JIT_PROFILE_PERSISTENCE,
        /** Native application and runtime packaging. */
        NATIVE_PACKAGING,
        /** Exact downstream delta from an OpenJDK ancestor. */
        OPENJDK_VENDOR_DELTA,
        /** Execution-profile-guided optimization. */
        PROFILE_GUIDED_OPTIMIZATION,
        /** Reflection/reachability profile capture. */
        REFLECTIVE_ACCESS_PROFILE,
        /** Dynamic discovery of required runtime components. */
        RUNTIME_COMPONENT_DISCOVERY,
        /** Startup profile collection and consumption. */
        STARTUP_PROFILE
    }

    /** Capability-candidate state. */
    public enum CapabilityStatus {
        /** Evidence exists; no implementation or product admission is claimed. */
        CANDIDATE,
        /** An implementation-independent specification has been reviewed. */
        SPECIFIED,
        /** Candidate is explicitly rejected. */
        REJECTED
    }

    /**
     * Immutable donor row.
     *
     * @param donorId stable donor identity
     * @param projectName display name
     * @param family donor family
     * @param repositoryUrl immutable-source repository or dash
     * @param revision immutable donor commit or dash
     * @param sourceLicense donor licence policy
     * @param targetLicense target licence policy
     * @param defaultImportMode default import treatment
     * @param upstreamRepository upstream repository or dash
     * @param upstreamRevision immutable upstream commit or dash
     * @param availability source availability
     * @param status donor state
     */
    public record Donor(
            String donorId,
            String projectName,
            DonorFamily family,
            String repositoryUrl,
            String revision,
            String sourceLicense,
            String targetLicense,
            ImportMode defaultImportMode,
            String upstreamRepository,
            String upstreamRevision,
            Availability availability,
            DonorStatus status) {
    }

    /**
     * Immutable evidence-source row.
     *
     * @param sourceId stable evidence identity
     * @param donorId owning donor identity
     * @param sourceKind evidence kind
     * @param repositoryUrl evidence repository
     * @param revision immutable commit containing the evidence
     * @param pathOrRef repository path or exact ref
     * @param gitObjectSha Git object identity
     * @param declaredLicense source-specific licence policy
     * @param role evidence role
     */
    public record Source(
            String sourceId,
            String donorId,
            SourceKind sourceKind,
            String repositoryUrl,
            String revision,
            String pathOrRef,
            String gitObjectSha,
            String declaredLicense,
            SourceRole role) {
    }

    /**
     * Immutable donor-capability row.
     *
     * @param capabilityId stable capability identity
     * @param donorId donor that owns the capability
     * @param category capability category
     * @param evidenceSourceId supporting evidence source
     * @param importMode permitted import mode
     * @param targetArea intended M3 owner area
     * @param status candidate state
     * @param contractRef governing contract reference
     */
    public record Capability(
            String capabilityId,
            String donorId,
            CapabilityCategory category,
            String evidenceSourceId,
            ImportMode importMode,
            String targetArea,
            CapabilityStatus status,
            String contractRef) {
    }

    /**
     * Closed import-mode table row.
     *
     * @param mode import mode
     * @param directSourceAllowed whether source expression may be carried
     * @param compatibleLineageRequired whether compatible lineage is mandatory
     * @param isolationRequired whether an independent boundary is mandatory
     * @param description bounded mode description
     */
    public record ImportModePolicy(
            ImportMode mode,
            boolean directSourceAllowed,
            boolean compatibleLineageRequired,
            boolean isolationRequired,
            String description) {
    }

    /**
     * Immutable validated catalogue.
     *
     * @param donors donor rows keyed by stable identity
     * @param sources evidence rows keyed by stable identity
     * @param capabilities capability rows keyed by stable identity
     * @param importModes executable import-mode policy table
     * @param semanticSha256 SHA-256 over canonical catalogue inputs
     */
    public record Catalogue(
            Map<String, Donor> donors,
            Map<String, Source> sources,
            Map<String, Capability> capabilities,
            Map<ImportMode, ImportModePolicy> importModes,
            String semanticSha256) {
    }

    private M3DonorCatalogue() {
    }

    /**
     * CLI: {@code M3DonorCatalogue <validate|plan> <catalogue-root> [plan-output]}.
     *
     * @param args command, module root and optional plan path
     * @throws IOException when catalogue files cannot be read or written
     */
    public static void main(String[] args) throws IOException {
        if (args.length < 2 || args.length > 3) {
            throw new IllegalArgumentException("usage: <validate|plan> <catalogue-root> [plan-output]");
        }
        Catalogue catalogue = load(Path.of(args[1]));
        String command = args[0].toLowerCase(Locale.ROOT);
        switch (command) {
            case "validate" -> {
                if (args.length != 2) {
                    throw new IllegalArgumentException("validate does not accept a plan-output argument");
                }
                printSummary(catalogue);
            }
            case "plan" -> {
                if (args.length != 3) {
                    throw new IllegalArgumentException("plan requires an output path");
                }
                writePlan(catalogue, Path.of(args[2]));
                printSummary(catalogue);
            }
            default -> throw new IllegalArgumentException("command must be validate or plan");
        }
    }

    /**
     * Loads and validates a catalogue module root.
     *
     * @param root directory containing {@code catalogue/} and {@code contract/}
     * @return immutable validated catalogue
     * @throws IOException when an input cannot be read
     */
    public static Catalogue load(Path root) throws IOException {
        Objects.requireNonNull(root, "root");
        Path normalized = root.toAbsolutePath().normalize();
        Path donorsPath = normalized.resolve("catalogue/DONORS.tsv");
        Path sourcesPath = normalized.resolve("catalogue/SOURCES.tsv");
        Path capabilitiesPath = normalized.resolve("catalogue/CAPABILITIES.tsv");
        Path importModesPath = normalized.resolve("contract/IMPORT_MODES.tsv");

        List<String> donorLines = readStrictLines(donorsPath, DONORS_HEADER);
        List<String> sourceLines = readStrictLines(sourcesPath, SOURCES_HEADER);
        List<String> capabilityLines = readStrictLines(capabilitiesPath, CAPABILITIES_HEADER);
        List<String> importModeLines = readStrictLines(importModesPath, IMPORT_MODES_HEADER);

        Map<String, Donor> donors = parseDonors(donorLines, donorsPath);
        Map<String, Source> sources = parseSources(sourceLines, sourcesPath, donors);
        Map<ImportMode, ImportModePolicy> importModes = parseImportModes(importModeLines, importModesPath);
        Map<String, Capability> capabilities = parseCapabilities(
                capabilityLines, capabilitiesPath, donors, sources, importModes);
        validateDonorPolicies(donors);
        validateCapabilityPolicies(capabilities, donors, sources, importModes);

        String digest = semanticDigest(Map.of(
                "CAPABILITIES.tsv", capabilityLines,
                "DONORS.tsv", donorLines,
                "IMPORT_MODES.tsv", importModeLines,
                "SOURCES.tsv", sourceLines));
        return new Catalogue(
                immutableSorted(donors),
                immutableSorted(sources),
                immutableSorted(capabilities),
                immutableEnumMap(importModes),
                digest);
    }

    /**
     * Writes the deterministic candidate plan.
     *
     * @param catalogue validated catalogue
     * @param output output file
     * @throws IOException when output cannot be written
     */
    public static void writePlan(Catalogue catalogue, Path output) throws IOException {
        Objects.requireNonNull(catalogue, "catalogue");
        Objects.requireNonNull(output, "output");
        List<String> lines = new ArrayList<>();
        lines.add(PLAN_HEADER);
        for (Capability capability : catalogue.capabilities().values()) {
            Donor donor = catalogue.donors().get(capability.donorId());
            Source source = catalogue.sources().get(capability.evidenceSourceId());
            boolean sourceCopyAllowed = sourceCopyAllowed(capability, donor, source, catalogue.importModes());
            lines.add(String.join("\t",
                    capability.capabilityId(),
                    capability.donorId(),
                    capability.importMode().name(),
                    Boolean.toString(sourceCopyAllowed),
                    decision(capability.importMode(), sourceCopyAllowed),
                    source.repositoryUrl(),
                    source.revision(),
                    source.gitObjectSha(),
                    capability.targetArea(),
                    capability.status().name()));
        }
        Path parent = output.toAbsolutePath().normalize().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(output, String.join("\n", lines) + "\n", StandardCharsets.UTF_8);
    }

    /**
     * Returns whether the specific candidate may carry source expression into review.
     *
     * @param capability capability row
     * @param donor donor row
     * @param source evidence source
     * @param policies closed import-mode table
     * @return {@code true} only for compatible-lineage direct-source candidates
     */
    public static boolean sourceCopyAllowed(
            Capability capability,
            Donor donor,
            Source source,
            Map<ImportMode, ImportModePolicy> policies) {
        ImportModePolicy policy = policies.get(capability.importMode());
        if (policy == null || !policy.directSourceAllowed()) {
            return false;
        }
        if (capability.importMode() == ImportMode.ISOLATE) {
            return donor.availability() == Availability.SOURCE_AVAILABLE
                    && donor.family() != DonorFamily.PROPRIETARY_RUNTIME
                    && !isGpl3ToGpl2(source.declaredLicense(), donor.targetLicense());
        }
        return donor.family() == DonorFamily.OPENJDK_DOWNSTREAM
                && donor.availability() == Availability.SOURCE_AVAILABLE
                && donor.sourceLicense().equals(donor.targetLicense())
                && !isGpl3ToGpl2(source.declaredLicense(), donor.targetLicense());
    }

    private static Map<String, Donor> parseDonors(List<String> lines, Path path) {
        Map<String, Donor> rows = new TreeMap<>();
        String previous = null;
        for (int lineNumber = 2; lineNumber <= lines.size(); lineNumber++) {
            String[] cells = cells(lines.get(lineNumber - 1), 12, path, lineNumber);
            String id = requireId("donor_id", cells[0], path, lineNumber);
            previous = requireStrictOrder(previous, id, path, lineNumber);
            Donor row = new Donor(
                    id,
                    requireText("project_name", cells[1], path, lineNumber),
                    enumValue(DonorFamily.class, cells[2], path, lineNumber),
                    requireUrlOrDash("repository_url", cells[3], path, lineNumber),
                    requireGitOrDash("revision", cells[4], path, lineNumber),
                    requireText("source_license", cells[5], path, lineNumber),
                    requireText("target_license", cells[6], path, lineNumber),
                    enumValue(ImportMode.class, cells[7], path, lineNumber),
                    requireUrlOrDash("upstream_repository", cells[8], path, lineNumber),
                    requireGitOrDash("upstream_revision", cells[9], path, lineNumber),
                    enumValue(Availability.class, cells[10], path, lineNumber),
                    enumValue(DonorStatus.class, cells[11], path, lineNumber));
            rows.put(id, row);
        }
        requireNonEmpty(rows, path);
        return rows;
    }

    private static Map<String, Source> parseSources(
            List<String> lines, Path path, Map<String, Donor> donors) {
        Map<String, Source> rows = new TreeMap<>();
        String previous = null;
        for (int lineNumber = 2; lineNumber <= lines.size(); lineNumber++) {
            String[] cells = cells(lines.get(lineNumber - 1), 9, path, lineNumber);
            String id = requireId("source_id", cells[0], path, lineNumber);
            previous = requireStrictOrder(previous, id, path, lineNumber);
            String donorId = requireId("donor_id", cells[1], path, lineNumber);
            if (!donors.containsKey(donorId)) {
                throw invalid(path, lineNumber, "unknown donor_id " + donorId);
            }
            SourceKind kind = enumValue(SourceKind.class, cells[2], path, lineNumber);
            String pathOrRef = requireText("path_or_ref", cells[5], path, lineNumber);
            if (kind == SourceKind.REF) {
                if (!pathOrRef.startsWith("refs/")) {
                    throw invalid(path, lineNumber, "REF path_or_ref must start with refs/");
                }
            } else {
                requireRelativePath(pathOrRef, path, lineNumber);
            }
            Source row = new Source(
                    id,
                    donorId,
                    kind,
                    requireUrl("repository_url", cells[3], path, lineNumber),
                    requireGit("revision", cells[4], path, lineNumber),
                    pathOrRef,
                    requireGit("git_object_sha", cells[6], path, lineNumber),
                    requireText("declared_license", cells[7], path, lineNumber),
                    enumValue(SourceRole.class, cells[8], path, lineNumber));
            rows.put(id, row);
        }
        requireNonEmpty(rows, path);
        return rows;
    }

    private static Map<String, Capability> parseCapabilities(
            List<String> lines,
            Path path,
            Map<String, Donor> donors,
            Map<String, Source> sources,
            Map<ImportMode, ImportModePolicy> importModes) {
        Map<String, Capability> rows = new TreeMap<>();
        String previous = null;
        for (int lineNumber = 2; lineNumber <= lines.size(); lineNumber++) {
            String[] cells = cells(lines.get(lineNumber - 1), 8, path, lineNumber);
            String id = requireId("capability_id", cells[0], path, lineNumber);
            previous = requireStrictOrder(previous, id, path, lineNumber);
            String donorId = requireId("donor_id", cells[1], path, lineNumber);
            String sourceId = requireId("evidence_source_id", cells[3], path, lineNumber);
            if (!donors.containsKey(donorId)) {
                throw invalid(path, lineNumber, "unknown donor_id " + donorId);
            }
            if (!sources.containsKey(sourceId)) {
                throw invalid(path, lineNumber, "unknown evidence_source_id " + sourceId);
            }
            ImportMode importMode = enumValue(ImportMode.class, cells[4], path, lineNumber);
            if (!importModes.containsKey(importMode)) {
                throw invalid(path, lineNumber, "import mode missing from IMPORT_MODES.tsv: " + importMode);
            }
            Capability row = new Capability(
                    id,
                    donorId,
                    enumValue(CapabilityCategory.class, cells[2], path, lineNumber),
                    sourceId,
                    importMode,
                    requireRelativePath(cells[5], path, lineNumber),
                    enumValue(CapabilityStatus.class, cells[6], path, lineNumber),
                    requireText("contract_ref", cells[7], path, lineNumber));
            rows.put(id, row);
        }
        requireNonEmpty(rows, path);
        return rows;
    }

    private static Map<ImportMode, ImportModePolicy> parseImportModes(List<String> lines, Path path) {
        Map<ImportMode, ImportModePolicy> rows = new java.util.EnumMap<>(ImportMode.class);
        String previous = null;
        for (int lineNumber = 2; lineNumber <= lines.size(); lineNumber++) {
            String[] cells = cells(lines.get(lineNumber - 1), 5, path, lineNumber);
            ImportMode mode = enumValue(ImportMode.class, cells[0], path, lineNumber);
            previous = requireStrictOrder(previous, mode.name(), path, lineNumber);
            ImportModePolicy policy = new ImportModePolicy(
                    mode,
                    strictBoolean(cells[1], path, lineNumber),
                    strictBoolean(cells[2], path, lineNumber),
                    strictBoolean(cells[3], path, lineNumber),
                    requireText("description", cells[4], path, lineNumber));
            if (policy.directSourceAllowed() != mode.directSourceAllowed()
                    || policy.compatibleLineageRequired() != mode.compatibleLineageRequired()
                    || policy.isolationRequired() != mode.isolationRequired()) {
                throw invalid(path, lineNumber, "IMPORT_MODES.tsv disagrees with executable mode contract for " + mode);
            }
            rows.put(mode, policy);
        }
        if (rows.size() != ImportMode.values().length) {
            throw invalid(path, 1, "IMPORT_MODES.tsv must contain every ImportMode exactly once");
        }
        return rows;
    }

    private static void validateDonorPolicies(Map<String, Donor> donors) {
        for (Donor donor : donors.values()) {
            switch (donor.family()) {
                case OPENJDK_DOWNSTREAM -> {
                    requirePolicy(donor.availability() == Availability.SOURCE_AVAILABLE, donor,
                            "OpenJDK downstream must have source available");
                    requirePolicy(!donor.repositoryUrl().equals("-") && !donor.revision().equals("-"), donor,
                            "OpenJDK downstream requires immutable repository and revision");
                    requirePolicy(!donor.upstreamRepository().equals("-") && !donor.upstreamRevision().equals("-"),
                            donor, "OpenJDK downstream requires immutable upstream reference");
                    requirePolicy(donor.defaultImportMode() == ImportMode.PATCH
                                    || donor.defaultImportMode() == ImportMode.ADAPT,
                            donor, "OpenJDK downstream default must be PATCH or ADAPT");
                    requirePolicy(donor.sourceLicense().equals(donor.targetLicense()), donor,
                            "direct-lineage donor and target licence policies must match");
                }
                case PROPRIETARY_RUNTIME -> {
                    requirePolicy(donor.availability() == Availability.DOCUMENTATION_ONLY, donor,
                            "proprietary runtime must be documentation-only");
                    requirePolicy(donor.repositoryUrl().equals("-") && donor.revision().equals("-"), donor,
                            "proprietary runtime cannot claim a public source revision");
                    requirePolicy(donor.defaultImportMode() == ImportMode.SPEC_REIMPLEMENT
                                    || donor.defaultImportMode() == ImportMode.ORACLE_ONLY
                                    || donor.defaultImportMode() == ImportMode.REJECT,
                            donor, "proprietary runtime cannot default to a source-carrying mode");
                }
                case PUBLIC_INTEGRATION -> {
                    requirePolicy(donor.availability() == Availability.SOURCE_AVAILABLE, donor,
                            "public integration donor requires source pin");
                    requirePolicy(!donor.repositoryUrl().equals("-") && !donor.revision().equals("-"), donor,
                            "public integration donor requires immutable repository and revision");
                    if (isGpl3ToGpl2(donor.sourceLicense(), donor.targetLicense())) {
                        requirePolicy(!donor.defaultImportMode().directSourceAllowed(), donor,
                                "GPLv3 integration source cannot default to direct import into GPLv2 core");
                    }
                }
            }
        }
    }

    private static void validateCapabilityPolicies(
            Map<String, Capability> capabilities,
            Map<String, Donor> donors,
            Map<String, Source> sources,
            Map<ImportMode, ImportModePolicy> importModes) {
        for (Capability capability : capabilities.values()) {
            Donor donor = donors.get(capability.donorId());
            Source source = sources.get(capability.evidenceSourceId());
            Donor sourceDonor = donors.get(source.donorId());
            if (!source.donorId().equals(capability.donorId())) {
                boolean allowedProxy = donor.family() == DonorFamily.PROPRIETARY_RUNTIME
                        && sourceDonor.family() == DonorFamily.PUBLIC_INTEGRATION
                        && source.sourceKind() == SourceKind.PUBLIC_PROXY;
                if (!allowedProxy) {
                    throw new IllegalArgumentException(capability.capabilityId()
                            + ": cross-donor evidence is allowed only from PUBLIC_INTEGRATION to PROPRIETARY_RUNTIME");
                }
            }
            boolean copyAllowed = sourceCopyAllowed(capability, donor, source, importModes);
            if (capability.importMode().directSourceAllowed() && !copyAllowed) {
                throw new IllegalArgumentException(capability.capabilityId()
                        + ": direct-source mode is incompatible with donor family or licence policy");
            }
            if (donor.family() == DonorFamily.PROPRIETARY_RUNTIME
                    && capability.importMode().directSourceAllowed()) {
                throw new IllegalArgumentException(capability.capabilityId()
                        + ": proprietary runtime capability cannot carry source expression");
            }
            if (isGpl3ToGpl2(source.declaredLicense(), donor.targetLicense())
                    && capability.importMode().directSourceAllowed()) {
                throw new IllegalArgumentException(capability.capabilityId()
                        + ": GPLv3 evidence cannot use a direct-source mode for GPLv2 target core");
            }
        }
    }

    private static String decision(ImportMode mode, boolean sourceCopyAllowed) {
        return switch (mode) {
            case PATCH, ADAPT -> sourceCopyAllowed ? "DIRECT_REVIEW" : "REFUSE_DIRECT_SOURCE";
            case SPEC_REIMPLEMENT -> "CLEAN_ROOM_SPEC";
            case ORACLE_ONLY -> "ORACLE_ONLY";
            case ISOLATE -> sourceCopyAllowed ? "ISOLATED_REVIEW" : "REFUSE_ISOLATION";
            case REJECT -> "REJECT";
        };
    }

    private static void printSummary(Catalogue catalogue) {
        System.out.printf(Locale.ROOT,
                "M3_DONOR_CATALOGUE\tdonors=%d\tsources=%d\tcapabilities=%d\tsemantic_sha256=%s%n",
                catalogue.donors().size(), catalogue.sources().size(), catalogue.capabilities().size(),
                catalogue.semanticSha256());
    }

    private static List<String> readStrictLines(Path path, String expectedHeader) throws IOException {
        String text = Files.readString(path, StandardCharsets.UTF_8);
        if (text.startsWith("\uFEFF")) {
            throw invalid(path, 1, "UTF-8 byte-order marks are not permitted");
        }
        if (text.indexOf('\r') >= 0) {
            throw invalid(path, 1, "carriage returns are not permitted; use LF");
        }
        if (!text.endsWith("\n") || text.endsWith("\n\n")) {
            throw invalid(path, 1, "file must end with exactly one LF");
        }
        List<String> lines = List.of(text.substring(0, text.length() - 1).split("\n", -1));
        if (lines.isEmpty() || !expectedHeader.equals(lines.get(0))) {
            throw invalid(path, 1, "expected exact header: " + expectedHeader);
        }
        for (int index = 0; index < lines.size(); index++) {
            if (lines.get(index).isEmpty()) {
                throw invalid(path, index + 1, "blank lines are not permitted");
            }
        }
        return lines;
    }

    private static String[] cells(String line, int expected, Path path, int lineNumber) {
        String[] cells = line.split("\\t", -1);
        if (cells.length != expected) {
            throw invalid(path, lineNumber, "expected " + expected + " cells, found " + cells.length);
        }
        return cells;
    }

    private static String requireStrictOrder(String previous, String current, Path path, int lineNumber) {
        if (previous != null && previous.compareTo(current) >= 0) {
            throw invalid(path, lineNumber, "rows must be strictly sorted by primary identity");
        }
        return current;
    }

    private static String requireId(String name, String value, Path path, int lineNumber) {
        if (!ID.matcher(value).matches()) {
            throw invalid(path, lineNumber, name + " is not a valid stable identity: " + value);
        }
        return value;
    }

    private static String requireText(String name, String value, Path path, int lineNumber) {
        if (value.isBlank() || !SAFE_TEXT.matcher(value).matches()) {
            throw invalid(path, lineNumber, name + " must be nonblank printable text without tabs");
        }
        return value;
    }

    private static String requireUrl(String name, String value, Path path, int lineNumber) {
        requireText(name, value, path, lineNumber);
        if (!value.startsWith("https://") || value.endsWith("/")) {
            throw invalid(path, lineNumber, name + " must be a normalized https URL without trailing slash");
        }
        return value;
    }

    private static String requireUrlOrDash(String name, String value, Path path, int lineNumber) {
        return value.equals("-") ? value : requireUrl(name, value, path, lineNumber);
    }

    private static String requireGit(String name, String value, Path path, int lineNumber) {
        if (!GIT_ID.matcher(value).matches()) {
            throw invalid(path, lineNumber, name + " must be an immutable 40 or 64 hexadecimal Git identity");
        }
        return value;
    }

    private static String requireGitOrDash(String name, String value, Path path, int lineNumber) {
        return value.equals("-") ? value : requireGit(name, value, path, lineNumber);
    }

    private static String requireRelativePath(String value, Path path, int lineNumber) {
        requireText("path", value, path, lineNumber);
        boolean windowsAbsolute = value.length() >= 3 && Character.isLetter(value.charAt(0))
                && value.charAt(1) == ':' && value.charAt(2) == '/';
        if (value.startsWith("/") || value.startsWith("\\") || value.contains("\\") || value.endsWith("/")
                || windowsAbsolute) {
            throw invalid(path, lineNumber, "path must be normalized and repository-relative");
        }
        for (String part : value.split("/", -1)) {
            if (part.isEmpty() || part.equals(".") || part.equals("..")) {
                throw invalid(path, lineNumber, "path contains an empty or traversal segment");
            }
        }
        return value;
    }

    private static boolean strictBoolean(String value, Path path, int lineNumber) {
        if (value.equals("true")) {
            return true;
        }
        if (value.equals("false")) {
            return false;
        }
        throw invalid(path, lineNumber, "boolean value must be true or false");
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String value, Path path, int lineNumber) {
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException exception) {
            throw invalid(path, lineNumber, "invalid " + type.getSimpleName() + " value " + value);
        }
    }

    private static boolean isGpl3ToGpl2(String sourceLicense, String targetLicense) {
        return sourceLicense.startsWith("GPL-3.0") && targetLicense.startsWith("GPL-2.0");
    }

    private static void requirePolicy(boolean condition, Donor donor, String message) {
        if (!condition) {
            throw new IllegalArgumentException(donor.donorId() + ": " + message);
        }
    }

    private static void requireNonEmpty(Map<?, ?> rows, Path path) {
        if (rows.isEmpty()) {
            throw invalid(path, 1, "at least one data row is required");
        }
    }

    private static <V> Map<String, V> immutableSorted(Map<String, V> source) {
        return Collections.unmodifiableMap(new TreeMap<>(source));
    }

    private static Map<ImportMode, ImportModePolicy> immutableEnumMap(Map<ImportMode, ImportModePolicy> source) {
        Map<ImportMode, ImportModePolicy> ordered = new LinkedHashMap<>();
        for (ImportMode mode : ImportMode.values()) {
            ordered.put(mode, source.get(mode));
        }
        return Collections.unmodifiableMap(ordered);
    }

    private static String semanticDigest(Map<String, List<String>> files) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            update(digest, "M3-DONOR-CATALOGUE/1\n");
            files.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
                update(digest, "--" + entry.getKey() + "--\n");
                entry.getValue().forEach(line -> update(digest, line + "\n"));
            });
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError("SHA-256 is required by the JDK", exception);
        }
    }

    private static void update(MessageDigest digest, String value) {
        digest.update(value.getBytes(StandardCharsets.UTF_8));
    }

    private static IllegalArgumentException invalid(Path path, int lineNumber, String message) {
        return new IllegalArgumentException(path + ":" + lineNumber + ": " + message);
    }
}
