/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.synexia.m3;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Source-sealed Maven recipe; generates new Java SDK owners only.
 * Existing Java AST rewrites remain OpenRewrite-owned, not manual target edits.
 * Fail-closed on altered catalogue, templates, occupied output and symlink paths.
 */
public final class M3CollectionSdkNamesRecipe {
    private record Output(String destination, byte[] bytes, String expectedSha) { }
    private static final String SOURCE = "m3/collections/src/main/java/com/m3/collections/";
    private static final String TEST = "m3/collections/src/test/java/com/m3/collections/";
    private static final String CATALOG_SHA = "e986838120c2558a145c6b16942475c8f5ff885b166526d635ef9c10360d8313";
    private static final String CATALOG_TEMPLATE_SHA = "b8167ed1b5c4805433b1dedadd2456f8847466c7498c358b8dfaed23a788c758";
    private static final String CATALOG_OUTPUT_SHA = "9fb8adfdc68a80dcdcb46f08a1dbfc24981292d930c15fd3ea672e6cbc8d0ae0";
    private static final String SDK_SHA = "9a51f5edad8fd58ec9a7cc4dcd17cec23117de1eba486ba1b7a4c631acd8eff0";
    private static final String CATALOG_TEST_SHA = "17abc051756d3cd0101dcaa4436e791d9393b28ddfef2f6d7f932835dbbcbd2a";
    private static final String SDK_TEST_SHA = "eb0ca67291ebd8d41c0d6d7f10f8a171b16d59866a2455ce406958ba4af34792";
    private static final String TOKEN = "/* @M3_FAMILY_ARRAYS@ */";
    private static final String HEADER = "upstream_fqcn\tvendor\texisting_m3_owner\t"
            + "proposed_m3_role_NOT_IMPLEMENTED\tdisposition\tsemantic_contract";

    private M3CollectionSdkNamesRecipe() { }

    public static void main(String[] args) throws Exception {
        if (args.length == 1 && "--selftest".equals(args[0])) {
            selfTest(); return;
        }
        if (args.length != 2 || !("--apply".equals(args[0]) || "--check".equals(args[0]))) {
            throw new IllegalArgumentException("Usage: --selftest | --check|--apply <m3jdk21-repo-root>");
        }
        run(Path.of(args[1]), "--apply".equals(args[0]));
    }

    public static void run(Path rootInput, boolean apply) throws IOException {
        Path root = rootInput.toAbsolutePath().normalize();
        if (!Files.isDirectory(root.resolve("m3/collections"))) {
            throw new IOException("REFUSE_NOT_M3JDK21");
        }
        List<Output> outputs = outputs();
        // Preflight all targets before any write: exact idempotent receiver admission.
        for (Output out : outputs) {
            Path destination = root.resolve(out.destination()).normalize();
            if (!destination.startsWith(root)) throw new IOException("REFUSE_ESCAPE");
            for (Path at = destination; !at.equals(root); at = at.getParent()) {
                if (Files.isSymbolicLink(at)) throw new IOException("REFUSE_SYMLINK:" + at);
            }
            if (Files.exists(destination)) {
                if (!Files.isRegularFile(destination)
                        || !Arrays.equals(Files.readAllBytes(destination), out.bytes())) {
                    throw new IOException("REFUSE_TARGET_DRIFT:" + out.destination());
                }
            } else if (!apply) {
                throw new IOException("REFUSE_TARGET_MISSING:" + out.destination());
            }
        }
        if (apply) {
            for (Output out : outputs) {
                Path destination = root.resolve(out.destination()).normalize();
                if (Files.exists(destination)) continue;
                Files.createDirectories(destination.getParent());
                Path temp = Files.createTempFile(destination.getParent(), ".m3-sdk-", ".tmp");
                try {
                    Files.write(temp, out.bytes());
                    Files.move(temp, destination, StandardCopyOption.ATOMIC_MOVE);
                } finally { Files.deleteIfExists(temp); }
            }
        }
        System.out.println("PASS M3CollectionSdkNamesRecipe mode=" + (apply ? "apply" : "check")
                + " generated=" + outputs.size() + " catalogFamilies=104");
    }

    private static List<Output> outputs() throws IOException {
        byte[] catalogue = checked("/catalog/FAMILY_CATALOG.tsv", CATALOG_SHA);
        byte[] template = checked("/templates/M3CollectionSdkCatalog.java.template", CATALOG_TEMPLATE_SHA);
        String rendered = new String(template, StandardCharsets.UTF_8);
        if (!rendered.contains(TOKEN) || rendered.indexOf(TOKEN) != rendered.lastIndexOf(TOKEN)) {
            throw new IOException("REFUSE_BAD_GENERATOR_MARKER");
        }
        List<String[]> rows = parseCatalog(new String(catalogue, StandardCharsets.UTF_8));
        byte[] generated = rendered.replace(TOKEN, renderArrays(rows)).getBytes(StandardCharsets.UTF_8);
        if (!sha256(generated).equals(CATALOG_OUTPUT_SHA)) {
            throw new IOException("REFUSE_GENERATED_CATALOG_HASH");
        }
        return List.of(
            output(SOURCE + "M3CollectionSdkCatalog.java", generated, CATALOG_OUTPUT_SHA),
            output(SOURCE + "M3CollectionSdk.java", checked("/templates/M3CollectionSdk.java", SDK_SHA), SDK_SHA),
            output(TEST + "M3CollectionSdkCatalogSelfTest.java", checked("/templates/M3CollectionSdkCatalogSelfTest.java", CATALOG_TEST_SHA), CATALOG_TEST_SHA),
            output(TEST + "M3CollectionSdkSelfTest.java", checked("/templates/M3CollectionSdkSelfTest.java", SDK_TEST_SHA), SDK_TEST_SHA)
        );
    }

    private static Output output(String path, byte[] source, String hash) throws IOException {
        if (!sha256(source).equals(hash)) throw new IOException("REFUSE_OUTPUT_MISMATCH:" + path);
        return new Output(path, source, hash);
    }

    private static List<String[]> parseCatalog(String catalog) throws IOException {
        String[] lines = catalog.split("\n", -1);
        if (lines.length < 2 || !HEADER.equals(lines[0])) throw new IOException("REFUSE_SCHEMA");
        List<String[]> rows = new ArrayList<>();
        String previous = "";
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].isEmpty()) {
                if (i == lines.length - 1) continue;
                throw new IOException("REFUSE_EMPTY_ROW:" + i);
            }
            String[] cols = lines[i].split("\t", -1);
            if (cols.length != 6) throw new IOException("REFUSE_ROW_WIDTH:" + i);
            for (String col : cols) {
                if (col.isEmpty() || !col.matches("[A-Za-z0-9._-]+")) {
                    throw new IOException("REFUSE_UNSAFE_IDENTIFIER:" + i);
                }
            }
            if (!cols[0].matches("[a-z][A-Za-z0-9._]*\\.[A-Za-z0-9_$]+")
                    || cols[0].compareTo(previous) <= 0) throw new IOException("REFUSE_UNSORTED_FQCN:" + i);
            previous = cols[0];
            if (!List.of("APACHE", "ECLIPSE", "GUAVA", "JDK", "SPRING").contains(cols[1])) {
                throw new IOException("REFUSE_VENDOR:" + i);
            }
            if (!List.of("JDK_RETAINED", "M3_BACKEND_ONLY", "M3_VIEW_ONLY", "PENDING").contains(cols[4])) {
                throw new IOException("REFUSE_DISPOSITION:" + i);
            }
            if ("PENDING".equals(cols[4]) && !"-".equals(cols[2])) {
                throw new IOException("REFUSE_PRETEND_INSTALLED:" + i);
            }
            if (!"-".equals(cols[2]) && !cols[2].startsWith("com.m3.collections.")) {
                throw new IOException("REFUSE_OTHER_OWNER:" + i);
            }
            rows.add(cols);
        }
        if (rows.size() != 104) throw new IOException("REFUSE_UNEXPECTED_COVERAGE:" + rows.size());
        return rows;
    }

    private static String renderArrays(List<String[]> rows) {
        return String.join("\n\n",
                renderStrings("NAMES", rows, 0),
                renderEnums("VENDORS", "Vendor", rows, 1),
                renderStrings("OWNERS", rows, 2),
                renderStrings("CANDIDATES", rows, 3),
                renderEnums("STATES", "Disposition", rows, 4),
                renderStrings("CONTRACTS", rows, 5));
    }
    private static String renderStrings(String name, List<String[]> rows, int col) {
        StringBuilder b = new StringBuilder("    private static final String[] " + name + " = {\n");
        for (int i = 0; i < rows.size(); i++) {
            String text = rows.get(i)[col];
            if ("-".equals(text)) text = "";
            b.append("        \"").append(text.replace("\\", "\\\\").replace("\"", "\\\""))
                    .append("\"").append(i + 1 == rows.size() ? "" : ",").append('\n');
        }
        return b.append("    };").toString();
    }
    private static String renderEnums(String name, String enumType, List<String[]> rows, int col) {
        StringBuilder b = new StringBuilder("    private static final " + enumType + "[] " + name + " = {\n");
        for (int i = 0; i < rows.size(); i++) {
            b.append("        ").append(enumType).append('.').append(rows.get(i)[col])
                    .append(i + 1 == rows.size() ? "" : ",").append('\n');
        }
        return b.append("    };").toString();
    }
    private static byte[] checked(String resource, String hash) throws IOException {
        try (InputStream input = M3CollectionSdkNamesRecipe.class.getResourceAsStream(resource)) {
            if (input == null) throw new IOException("REFUSE_MISSING_RESOURCE:" + resource);
            byte[] bytes = input.readAllBytes();
            if (!sha256(bytes).equals(hash)) throw new IOException("REFUSE_INPUT_HASH:" + resource);
            return bytes;
        }
    }
    private static String sha256(byte[] bytes) throws IOException {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException failure) { throw new IOException("REFUSE_SHA256_UNAVAILABLE", failure); }
    }
    private static void selfTest() throws Exception {
        Path temp = Files.createTempDirectory("m3-sdk-names-recipe-");
        try {
            Files.createDirectories(temp.resolve("m3/collections"));
            try { run(temp, false); throw new AssertionError("absent target accepted in check mode"); }
            catch (IOException expected) { if (!expected.getMessage().startsWith("REFUSE_TARGET_MISSING")) throw expected; }
            run(temp, true);
            run(temp, true); // repeat generation fixed-point
            run(temp, false);
            Path source = temp.resolve(SOURCE + "M3CollectionSdkCatalog.java");
            Files.writeString(source, "unexpected manual production rewrite");
            try { run(temp, true); throw new AssertionError("drift must not be overwritten"); }
            catch (IOException expected) { if (!expected.getMessage().startsWith("REFUSE_TARGET_DRIFT")) throw expected; }
            if (!Files.readString(source).equals("unexpected manual production rewrite")) {
                throw new AssertionError("drift modified");
            }
            System.out.println("PASS M3CollectionSdkNamesRecipeSelfTest missing/drift/fixed-point");
        } finally {
            try (Stream<Path> paths = Files.walk(temp)) {
                for (Path p : paths.sorted((a,b) -> b.getNameCount() - a.getNameCount()).toList()) {
                    Files.deleteIfExists(p);
                }
            }
        }
    }
}
