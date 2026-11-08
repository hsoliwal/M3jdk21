// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tooling.atomjit;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** JDK-wide read-only dispatch to canonical Synexia Atomizer and Patternizer. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/b65df53330e3d82f6b2d5509ded971f19883f4ca/synexia-openrewrite-recipes/crates/m3jdk21-openjdk-atom-pattern-20261031/src/main/java/com/synexia/m3/openjdk/M3OpenJdkAtomPatternGate.java blob 6b82daff2a9b; recipe m3/tooling/atom-jit-gate/recipe/atom-jit-gate-20261106/relocate.py v4
public final class OpenJdkAtomPatternGate {
    private static final String EXPECTED_HEADER = String.join("\t", "path", "git_blob_sha1",
            "mode", "file_kind", "owner_family", "phase", "candidate_category",
            "challenge_id", "technique_hint", "source_evidence", "source_sha256",
            "api_abi_jni_gate", "method_level_recipe_gate", "target_runtime_gate", "status");
    private static final String ORDER = "ATOMIZE>APPLY>JIT_OR_NATIVE_PROOF>" +
            "PATTERNIZE>FIXED_POINT>REVERIFY";
    private static final String HOLD = "HOLD_UNTIL_EXECUTED_PER_ATOM_TESTS";
    private static final String HINT = "FILE_NAME_HINT_ONLY_NOT_AST";
    private static final String SOURCE_NOT_READ = "SOURCE_SHA256_NOT_READ";
    private static final String RECIPE_NOT_EXECUTED =
            "CANONICAL_M3_METHOD_RECIPE_NOT_EXECUTED";
    private static final String RUNTIME_NOT_EXECUTED =
            "INSTALLED_M3JDK21_TEST_NOT_EXECUTED";
    private static final int MAX_SOURCE_RECORDS = 1_000_000;
    private static final Set<String> ADMITTED_DISPOSITIONS = Set.of(
            "VENDORED_PROVIDER_CONTRACT_HOLD", "ORACLE_AND_TEST_OWNER_ONLY",
            "LEGACY_RECIPE_CUSTODY_REVIEW", "NATIVE_ABI_JNI_HOLD",
            "SCRIPT_OR_GENERATOR_SOURCE_REVIEW_ONLY", "NON_JAVA_DONOR_REVIEW_ONLY",
            "NEEDS_AST_CONTRACT_REVIEW");

    private OpenJdkAtomPatternGate() { }

    public enum Lane {
        JDK_BOOTSTRAP, JDK_MODULE, HOTSPOT_NATIVE, JDK_NATIVE,
        M3_JAVA, M3_NATIVE, RECEIVED_PROVIDER, OTHER_JAVA, OTHER_NATIVE, TEST_OR_ORACLE,
        RECIPE_CUSTODY,
        GENERATOR_OR_DONOR
    }

    public record Order(String repo, String path, String blob, Lane lane,
                        String atomizer, String prePatternProof,
                        String patternizer, String postPatternProof, String status) {
        public String tsv() {
            return String.join("\t", repo, path, blob, lane.name(), ORDER, atomizer,
                    prePatternProof, patternizer, postPatternProof, status);
        }
    }

    public static Order plan(String repo, String path, String blob, String kind) {
        if (!repo.matches("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+") ||
                !blob.matches("[a-f0-9]{40}")) {
            throw new IllegalArgumentException("invalid repository or source blob");
        }
        checkPath(path);
        if ((kind.equals("JAVA") && !path.endsWith(".java")) ||
                (kind.equals("NATIVE_C") && !path.endsWith(".c")) ||
                (kind.equals("NATIVE_OBJC") && !path.endsWith(".m"))) {
            throw new IllegalArgumentException("source extension and declared language disagree");
        }
        boolean jdk = repo.equals("hsoliwal/M3jdk21");
        boolean java = kind.equals("JAVA") && path.endsWith(".java");
        boolean nativeCode = Set.of("NATIVE_C", "NATIVE_CPP", "NATIVE_HEADER",
                "NATIVE_OBJC", "ASSEMBLY", "ACCEL_KERNEL").contains(kind);
        Lane lane;
        if (path.startsWith("m3/vendor/") ||
                path.startsWith("m3/collections/implementations/") ||
                path.contains("/third_party/"))
            lane = Lane.RECEIVED_PROVIDER;
        else if (path.startsWith("test/") || path.contains("/src/test/") ||
                path.contains("/tests/") || path.contains("/test/") ||
                path.startsWith("m3/qualification/") ||
                path.startsWith("m3/collections/qualification/")) lane = Lane.TEST_OR_ORACLE;
        else if (jdk && (path.startsWith("m3/recipes/") ||
                path.startsWith("m3/collections/recipe/") ||
                path.startsWith("m3/mop/") || path.startsWith(".m3/")))
            lane = Lane.RECIPE_CUSTODY;
        else if (path.startsWith("make/"))
            lane = Lane.GENERATOR_OR_DONOR;
        else if (jdk && java && path.startsWith("src/java.base/share/classes/java/lang/"))
            lane = Lane.JDK_BOOTSTRAP;
        else if (jdk && java && path.startsWith("src/")) lane = Lane.JDK_MODULE;
        else if (jdk && nativeCode && path.startsWith("src/hotspot/"))
            lane = Lane.HOTSPOT_NATIVE;
        else if (jdk && nativeCode && path.startsWith("src/")) lane = Lane.JDK_NATIVE;
        else if (jdk && java && path.startsWith("m3/")) lane = Lane.M3_JAVA;
        else if (jdk && nativeCode && path.startsWith("m3/")) lane = Lane.M3_NATIVE;
        else if (java) lane = Lane.OTHER_JAVA;
        else if (nativeCode) lane = Lane.OTHER_NATIVE;
        else if (Set.of("SCRIPT", "CODE_TEMPLATE", "OTHER_LANGUAGE").contains(kind))
            lane = Lane.GENERATOR_OR_DONOR;
        else throw new IllegalArgumentException("unreviewed file kind");
        String atomizer, patternizer, gate, repeat;
        switch (lane) {
            case JDK_BOOTSTRAP -> {
                atomizer = "M3AtomizeRecipe"; patternizer = "M3PatternizeRecipe";
                gate = "JDK_IMAGE_JTREG_XINT_C1_C2_JIT";
                repeat = "JDK_IMAGE_JTREG_JIT_AND_ABI";
            }
            case JDK_MODULE -> {
                atomizer = "M3AtomizeRecipe"; patternizer = "M3PatternizeRecipe";
                gate = "JDK_IMAGE_JMOD_JTREG_JIT_CALLSITE";
                repeat = "JDK_IMAGE_JTREG_JIT_AND_ABI";
            }
            case RECEIVED_PROVIDER -> {
                atomizer = java ? "M3AtomizeRecipe" : nativeCode
                        ? "M3HashPinnedTextSnapshotRecipe" : "REVIEW_ONLY";
                patternizer = java ? "M3PatternizeRecipe" : nativeCode
                        ? "M3HashPinnedTextSnapshotRecipe" : "REVIEW_ONLY";
                gate = java ? "DONOR_LICENCE_PROVIDER_API_JUNIT_AND_JAVA_JIT"
                        : nativeCode ? "DONOR_LICENCE_NATIVE_ABI_JNI_GATE"
                        : "NO_PRODUCTION_ATOM";
                repeat = nativeCode ? "PROVIDER_NATIVE_ABI_JNI_REVERIFY"
                        : java ? "PROVIDER_JUNIT_ABI_JIT_CONTRACT_REPLAY"
                        : "SOURCE_ORACLE_REVIEW_ONLY";
            }
            case M3_JAVA, OTHER_JAVA -> {
                atomizer = "M3AtomizeRecipe"; patternizer = "M3PatternizeRecipe";
                gate = "JAVA21_JUNIT_XINT_C1_C2_JIT";
                repeat = "JAVA21_JUNIT_JIT_AND_API";
            }
            case HOTSPOT_NATIVE, JDK_NATIVE, M3_NATIVE, OTHER_NATIVE -> {
                atomizer = "M3HashPinnedTextSnapshotRecipe";
                patternizer = "M3HashPinnedTextSnapshotRecipe";
                gate = "C17_CPP20_ABI_JNI_JDK_IMAGE_AND_JAVA_CALLSITE_JIT";
                repeat = "NATIVE_ABI_JNI_GC_CALLSITE_PARITY";
            }
            case TEST_OR_ORACLE, GENERATOR_OR_DONOR, RECIPE_CUSTODY -> {
                atomizer = "REVIEW_ONLY"; patternizer = "REVIEW_ONLY";
                gate = "NO_PRODUCTION_ATOM"; repeat = "SOURCE_ORACLE_REVIEW_ONLY";
            }
            default -> throw new AssertionError("unknown source lane");
        }
        return new Order(repo, path, blob, lane, atomizer, gate, patternizer, repeat, HOLD);
    }

    public static List<Order> read(String repo, Path inventory) throws IOException {
        if (Files.isSymbolicLink(inventory) || !Files.isRegularFile(inventory))
            throw new IllegalArgumentException("missing or linked source inventory");
        return readSnapshot(repo, Files.readAllBytes(inventory));
    }

    /** The caller hashes exactly these immutable input bytes; no second filesystem read. */
    static List<Order> readSnapshot(String repo, byte[] bytes) {
        String source;
        try {
            source = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException invalid) {
            throw new IllegalArgumentException("malformed UTF-8 source inventory", invalid);
        }
        if (source.indexOf('\0') >= 0 || source.indexOf('\r') >= 0)
            throw new IllegalArgumentException("invalid source inventory bytes");
        List<Order> work = new ArrayList<>();
        Set<String> paths = new HashSet<>();
        boolean header = false;
        for (String line : source.split("\n")) {
            // Git tracked pathnames can begin with '#': do not skip a source leaf.
            if (line.isEmpty()) continue;
            if (!header) {
                if (!EXPECTED_HEADER.equals(line))
                    throw new IllegalArgumentException("inventory schema changed");
                header = true;
                continue;
            }
            String[] cells = line.split("\t", -1);
            if (cells.length != 15 || !Set.of("100644", "100755").contains(cells[2]))
                throw new IllegalArgumentException("invalid Git source row");
            for (String field : cells)
                if (field.isBlank()) throw new IllegalArgumentException("empty source review field");
            if (!HINT.equals(cells[9]) || !SOURCE_NOT_READ.equals(cells[10]) ||
                    !RECIPE_NOT_EXECUTED.equals(cells[12]) ||
                    !RUNTIME_NOT_EXECUTED.equals(cells[13]) ||
                    !ADMITTED_DISPOSITIONS.contains(cells[14])) {
                throw new IllegalArgumentException("unverified or forged source admission");
            }
            boolean javaSource = "JAVA".equals(cells[3]);
            boolean nativeSource = Set.of("NATIVE_C", "NATIVE_CPP", "NATIVE_HEADER",
                    "NATIVE_OBJC", "ASSEMBLY", "ACCEL_KERNEL").contains(cells[3]);
            String requiredGate = javaSource ? "JAVAC_21_API_ABI_JTREG_REQUIRED"
                    : nativeSource ? "NATIVE_HEADERS_C17_CPP20_ABI_JNI_REQUIRED"
                    : "LANGUAGE_SPECIFIC_SOURCE_BUILD_REQUIRED";
            if (!requiredGate.equals(cells[11]))
                throw new IllegalArgumentException("wrong Java/JNI source gate");
            if (!paths.add(cells[0])) throw new IllegalArgumentException("duplicate source path");
            if (work.size() >= MAX_SOURCE_RECORDS)
                throw new IllegalArgumentException("complete source queue budget exceeded");
            Order candidate = plan(repo, cells[0], cells[1], cells[3]);
            requireConsistentDisposition(candidate.lane(), cells[14]);
            work.add(candidate);
        }
        if (!header || work.isEmpty()) throw new IllegalArgumentException("empty Git source queue");
        work.sort(Comparator.comparing(Order::path));
        return List.copyOf(work);
    }

    private static void requireConsistentDisposition(Lane lane, String disposition) {
        boolean consistent = switch (disposition) {
            case "VENDORED_PROVIDER_CONTRACT_HOLD" ->
                    lane == Lane.RECEIVED_PROVIDER || lane == Lane.GENERATOR_OR_DONOR;
            case "ORACLE_AND_TEST_OWNER_ONLY" -> lane == Lane.TEST_OR_ORACLE;
            case "LEGACY_RECIPE_CUSTODY_REVIEW" -> lane == Lane.RECIPE_CUSTODY;
            case "NATIVE_ABI_JNI_HOLD" ->
                    lane == Lane.HOTSPOT_NATIVE || lane == Lane.JDK_NATIVE ||
                    lane == Lane.M3_NATIVE || lane == Lane.OTHER_NATIVE;
            case "SCRIPT_OR_GENERATOR_SOURCE_REVIEW_ONLY",
                    "NON_JAVA_DONOR_REVIEW_ONLY" ->
                    lane == Lane.GENERATOR_OR_DONOR || lane == Lane.RECIPE_CUSTODY;
            case "NEEDS_AST_CONTRACT_REVIEW" -> true;
            default -> false;
        };
        if (!consistent) throw new IllegalArgumentException(
                "source review disposition disagrees with target owner lane");
    }

    private static void checkPath(String path) {
        if (path.isBlank() || path.startsWith("/") || path.contains("\\") ||
                path.chars().anyMatch(ch -> ch < 32 || ch == 127))
            throw new IllegalArgumentException("invalid source path");
        for (String part : path.split("/", -1))
            if (part.isEmpty() || part.equals(".") || part.equals(".."))
                throw new IllegalArgumentException("unsafe path segment");
    }
}
