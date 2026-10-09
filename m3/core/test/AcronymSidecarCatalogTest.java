/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
import static java.nio.charset.StandardCharsets.UTF_8;

import com.m3.text.AcronymSidecarCatalog;
import com.m3.text.M3LexiconPrecompute;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

public final class AcronymSidecarCatalogTest {
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("m3-acronym-sidecar-");
        try {
            String owner = "0123456789abcdef".repeat(4);
            String header = "source_id\trecord_id\tsource_manifest_revision\towner_fingerprint"
                    + "\tacronym\texpansion\tdomain\n";
            String rows = "dictlang.acronyms\tAPI\tsynexia-acronym-1\t" + owner
                    + "\tAPI\tapplication programming interface\tcomputing\n"
                    + "dictlang.acronyms\tHTTP\tsynexia-acronym-1\t" + owner
                    + "\tHTTP\thypertext transfer protocol\tnetworking\n";
            byte[] data = (header + rows).getBytes(UTF_8);
            Files.write(directory.resolve(AcronymSidecarCatalog.DATA_FILE), data);
            String indexText = "schema_version\tfile\trows\tsha256\n"
                    + "m3lex-acronym-v1\tsynexia.acronyms.tsv\t2\t"
                    + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data))
                    + "\n";
            Files.writeString(directory.resolve(AcronymSidecarCatalog.INDEX_FILE), indexText, UTF_8);

            AcronymSidecarCatalog catalog = AcronymSidecarCatalog.open(directory);
            check(catalog.size() == 2, "row count");
            check(catalog.find("API").orElseThrow().value().equals(
                    new M3LexiconPrecompute.AcronymPrecompute(
                            "API", "application programming interface", "computing")),
                    "lookup");
            check(catalog.scope().sourceManifestRevision().equals("synexia-acronym-1"),
                    "scope");
            check(catalog.find("MISSING").isEmpty(), "missing lookup");

            Files.writeString(directory.resolve(AcronymSidecarCatalog.INDEX_FILE),
                    indexText + "m3lex-acronym-v1\tsynexia.acronyms.tsv\t2\t"
                            + "0000000000000000000000000000000000000000000000000000000000000000\n",
                    UTF_8);
            expectIllegal(() -> AcronymSidecarCatalog.open(directory), "extra index row");
            Files.writeString(directory.resolve(AcronymSidecarCatalog.INDEX_FILE), indexText, UTF_8);

            Files.write(directory.resolve(AcronymSidecarCatalog.DATA_FILE), (header + rows
                    + "dictlang.acronyms\tAPI\tsynexia-acronym-1\t" + owner
                    + "\tAPI\tduplicate\tcomputing\n").getBytes(UTF_8));
            expectIllegal(() -> AcronymSidecarCatalog.open(directory), "checksum drift");

            System.out.println("M3_ACRONYM_SIDECAR_CATALOG_PASS rows=2 checks=6");
        } finally {
            Files.walk(directory)
                    .sorted(java.util.Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (java.io.IOException failure) {
                            throw new RuntimeException(failure);
                        }
                    });
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @FunctionalInterface
    private interface ThrowingSupplier {
        Object get() throws java.io.IOException;
    }

    private static void expectIllegal(ThrowingSupplier action, String message) {
        try {
            action.get();
            throw new AssertionError(message + " accepted");
        } catch (IllegalArgumentException | java.io.IOException expected) {
            // expected
        }
    }
}
