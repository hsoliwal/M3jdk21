// SPDX-License-Identifier: Apache-2.0
package com.synexia.handoff;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static java.nio.charset.StandardCharsets.UTF_8;

/** Read-only packet proof; deliberately not a license scanner or copy authority. */
public final class ApacheReuseProof {
    private ApacheReuseProof() { }
    private static final String HEADER = "asset_kind\tcoverage\tlicense_basis\tqualification";
    private static final Set<String> KINDS = Set.of(
        "BENCHMARK_WORKLOAD", "CATALOGUE_METADATA", "DOCUMENTATION", "GENERATED_POSTIMAGE",
        "INDEX_IMAGE_DATA", "JAVA_IMPLEMENTATION", "MAVEN_BUILD_WIRING", "NATIVE_JNI_IMPLEMENTATION",
        "OTHER_OWNED_ASSET", "PROOF_REPLAY_RECEIPT", "RECIPE_COMPOSITION_DAG", "RECIPE_IMPLEMENTATION",
        "RECIPE_TEMPLATE", "SCHEMA_DATA_FORMAT", "TEST_FIXTURE");
    private static final String POLICY = """
        schema=SYNEXIA_APACHE_REUSE_V1
        workspace=hsoliwal/com.synexia
        targets=TASK_BOUND_PUBLIC_REPOSITORY
        explicit_target=hsoliwal/M3jdk21
        source_role=DONOR_CONVERGENCE_WORKSPACE
        target_role=PRODUCT_RUNTIME_OWNER
        owned_asset_license=Apache-2.0
        recipes=FIRST_CLASS_REUSABLE_ASSETS
        covered_use_royalty=NONE
        covered_use_extra_permission=NOT_REQUIRED
        upstream_notices=PRESERVE
        modified_file_notices=REQUIRED
        source_license_evidence=PATH_SPECIFIC
        output_license_evidence=TARGET_AND_TEMPLATE_SPECIFIC
        unknown_provenance=BLOCK
        incompatible_combination=BLOCK
        jdk_inlining=SEPARATE_COMPATIBILITY_EVIDENCE_REQUIRED
        classpath_exception=NOT_BLANKET_RELICENSING_AUTHORITY
        runtime_synexia_dependency=NOT_REQUIRED
        source_mutation_authority=false
        runtime_admission_authority=false
        target_names=EXISTING_TARGET_REGISTRY
        """;
    private static final Set<String> TEMPLATES = Set.of(
        "policy.properties.txt", "scope.tsv.txt", "reuse.md.txt");

    public record Packet(String manifest, Map<String, String> templates) {
        public Packet { templates = Map.copyOf(templates); }
    }

    public static Packet read(Path directory) throws Exception {
        var files = new HashMap<String, String>();
        for (String name : TEMPLATES) files.put(name, readFile(directory.resolve(name)));
        return new Packet(readFile(directory.resolve("manifest.tsv")), files);
    }

    private static String readFile(Path path) throws Exception {
        require(!Files.isSymbolicLink(path), "symbolic link");
        require(Files.size(path) <= 65536, "resource budget");
        return Files.readString(path, UTF_8);
    }

    public static void verify(Packet packet) {
        require(packet.templates().keySet().equals(TEMPLATES), "template inventory");
        var paths = new HashSet<String>();
        var names = new HashSet<String>();
        String previous = "";
        for (String line : packet.manifest().lines().filter(s -> !s.startsWith("#")).toList()) {
            String[] row = line.split("\t", -1);
            require(row.length == 4, "manifest columns");
            require(safePath(row[0]) && previous.compareTo(row[0]) < 0, "target path/order");
            previous = row[0];
            require(paths.add(row[0]) && names.add(row[3]), "duplicate target/template");
            require(row[1].equals("ABSENT"), "create-only packet");
            require(TEMPLATES.contains(row[3]), "unrecognized template");
            require(row[2].equals(hash(packet.templates().get(row[3]))), "template digest");
        }
        require(names.equals(TEMPLATES), "complete manifest");
        scope(packet.templates().get("scope.tsv.txt"));
        require(packet.templates().get("policy.properties.txt").equals(POLICY), "policy drift");
        require(packet.templates().get("reuse.md.txt").contains("SYNEXIA_APACHE_REUSE_V1"), "document identity");
    }

    private static boolean safePath(String path) {
        return !path.isBlank() && !path.startsWith("/") && !path.contains("\\")
            && !path.contains(":") && !path.startsWith(".git/")
            && path.chars().noneMatch(Character::isISOControl)
            && List.of(path.split("/", -1)).stream()
                .noneMatch(p -> p.isBlank() || p.equals(".") || p.equals(".."))
            && (path.endsWith(".md") || path.endsWith(".tsv") || path.endsWith(".properties"));
    }

    private static void scope(String text) {
        List<String> lines = text.lines().toList();
        require(!lines.isEmpty() && lines.getFirst().equals(HEADER), "scope header");
        var seen = new HashSet<String>();
        String previous = "";
        for (String line : lines.subList(1, lines.size())) {
            String[] row = line.split("\t", -1);
            require(row.length == 4 && KINDS.contains(row[0]), "scope columns/kind");
            require(previous.compareTo(row[0]) < 0 && seen.add(row[0]), "scope order/duplicate");
            previous = row[0];
            require(row[1].equals("IN_SCOPE") && row[2].equals("PATH_SPECIFIC")
                && row[3].equals("TARGET_PROOF_REQUIRED"), "scope boundary");
        }
        require(seen.equals(KINDS), "missing asset kind");
    }

    public static String hash(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    private static Packet changed(Packet packet, String name, String value, boolean reseal) {
        var files = new HashMap<>(packet.templates());
        files.put(name, value);
        String manifest = packet.manifest();
        if (reseal) manifest = manifest.replace(hash(packet.templates().get(name)), hash(value));
        return new Packet(manifest, files);
    }

    public static int selfTest(Packet packet) {
        verify(packet);
        var refused = new ArrayList<Runnable>();
        for (String kind : KINDS) {
            String old = packet.templates().get("scope.tsv.txt");
            String row = kind + "\tIN_SCOPE\tPATH_SPECIFIC\tTARGET_PROOF_REQUIRED\n";
            refused.add(() -> verify(changed(packet, "scope.tsv.txt", old.replace(row, ""), true)));
        }
        for (String line : POLICY.lines().toList()) {
            String bad = POLICY.replace(line + "\n", line + "_DRIFT\n");
            refused.add(() -> verify(changed(packet, "policy.properties.txt", bad, true)));
        }
        for (String name : TEMPLATES) {
            refused.add(() -> verify(changed(packet, name, packet.templates().get(name) + "drift", false)));
        }
        List<String> rows = packet.manifest().lines().filter(s -> !s.startsWith("#")).toList();
        refused.add(() -> verify(new Packet(packet.manifest() + rows.getFirst() + "\n", packet.templates())));
        refused.add(() -> verify(new Packet(rows.getFirst() + "\n", packet.templates())));
        for (String bad : List.of("../outside.md", "/tmp/absolute.md", "C:/absolute.md", ".git/config.md")) {
            String original = rows.getFirst().split("\t")[0];
            refused.add(() -> verify(new Packet(packet.manifest().replace(original, bad), packet.templates())));
        }
        refused.add(() -> verify(new Packet(packet.manifest().replace("ABSENT", "0".repeat(64)),
            packet.templates())));
        String scope = packet.templates().get("scope.tsv.txt");
        refused.add(() -> verify(changed(packet, "scope.tsv.txt", scope.replace("IN_SCOPE", "EXCLUDED"), true)));
        refused.add(() -> verify(changed(packet, "scope.tsv.txt", scope.replace("PATH_SPECIFIC", "ROOT_ONLY"), true)));
        refused.add(() -> verify(changed(packet, "scope.tsv.txt", scope + "UNKNOWN\tIN_SCOPE\tPATH_SPECIFIC"
            + "\tTARGET_PROOF_REQUIRED\n", true)));
        for (Runnable candidate : refused) {
            boolean rejected = false;
            try { candidate.run(); } catch (IllegalArgumentException expected) { rejected = true; }
            require(rejected, "mutation was accepted");
        }
        verify(packet);
        return refused.size();
    }

    private static void require(boolean value, String why) {
        if (!value) throw new IllegalArgumentException(why);
    }

    public static void main(String[] args) throws Exception {
        require(args.length == 1, "usage: ApacheReuseProof <resource-directory>");
        Packet packet = read(Path.of(args[0]));
        int refused = selfTest(packet);
        System.out.println("{\"packetIntegrity\":\"PASS\",\"assetKinds\":15,\"manifestRows\":3,"
            + "\"refusalCases\":" + refused + ",\"openRewriteExecution\":\"NOT_RUN_BY_THIS_PROBE\","
            + "\"runtimeAdmission\":false}");
    }
}
