/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
import com.m3.text.M3LexiconPrecompute;
import com.m3.text.SharedLexiconPrecomputeCatalog;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

public final class M3AcronymPrecomputeTest {
    private static final String SOURCE_REVISION = "9a990b710bf173258c29474083383d4c6b1e7d4e";
    private static final String SOURCE_BLOB_SHA = "5bb16c13b6be693153a0656b8beb61af860cb52f";
    private static final String SNAPSHOT_SHA256 = "4dd2d943eff28044362f843d9e42988e036d5e649910471949ae90666b29e27a";

    public static void main(String[] args) throws Exception {
        M3LexiconPrecompute.AcronymPrecompute http =
                new M3LexiconPrecompute.AcronymPrecompute(
                        "HTTP", "Hypertext Transfer Protocol", "networking",
                        SOURCE_REVISION, SOURCE_BLOB_SHA, SNAPSHOT_SHA256);
        check("HTTP".equals(http.acronym()), "acronym identity");
        check("Hypertext Transfer Protocol".equals(http.expansion()), "expansion preservation");
        check("networking".equals(http.domain()), "domain preservation");
        check(SOURCE_REVISION.equals(http.sourceRevision()), "source revision");
        check(SOURCE_BLOB_SHA.equals(http.sourceBlobSha()), "source blob");
        check(SNAPSHOT_SHA256.equals(http.snapshotSha256()), "snapshot digest");
        check(http.sourceBound(), "source bound");

        String manifest = Files.readString(
                Path.of("lexicon/synexia-source-manifest.tsv"), StandardCharsets.UTF_8);
        String row = Arrays.stream(manifest.split("\\n", -1))
                .filter(line -> line.startsWith("dictlang.acronyms\t"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("acronym manifest row missing"));
        String[] columns = row.split("\\t", -1);
        check(columns.length == 9, "acronym manifest shape");
        check("M3LexiconPrecompute.AcronymPrecompute".equals(columns[5]),
                "acronym manifest owner");
        check("acronym,domain,expansion,source_revision,source_blob_sha,snapshot_sha256".equals(columns[8]),
                "acronym manifest precompute fields");

        SharedLexiconPrecomputeCatalog.AcronymIdentity identity =
                new SharedLexiconPrecomputeCatalog.AcronymIdentity(
                        "dictlang.acronyms", "HTTP", SOURCE_REVISION,
                        SOURCE_BLOB_SHA, SNAPSHOT_SHA256);
        SharedLexiconPrecomputeCatalog catalog =
                SharedLexiconPrecomputeCatalog.builder().acronym(identity, http).build();
        check(catalog.acronymAt(identity).orElseThrow().equals(http), "catalog lookup");

        expectIllegal(() -> new M3LexiconPrecompute.AcronymPrecompute(
                "1HTTP", "Hypertext Transfer Protocol", "networking"),
                "malformed acronym");
        expectIllegal(() -> new M3LexiconPrecompute.AcronymPrecompute(
                "HTTP", "   ", "networking"),
                "blank expansion");
        expectIllegal(() -> new M3LexiconPrecompute.AcronymPrecompute(
                "HTTP", "Hypertext Transfer Protocol", "Networking"),
                "malformed domain");
        expectIllegal(() -> new SharedLexiconPrecomputeCatalog.AcronymIdentity(
                "dictlang.dictionary", "HTTP"),
                "wrong source family");
        expectIllegal(() -> SharedLexiconPrecomputeCatalog.builder().acronym(
                new SharedLexiconPrecomputeCatalog.AcronymIdentity(
                        "dictlang.acronyms", "HTTP"), http),
                "unpinned identity");
        expectIllegal(() -> new M3LexiconPrecompute.AcronymPrecompute(
                "HTTP", "Hypertext Transfer Protocol", "networking",
                SOURCE_REVISION, "000000000000000000000000000000000000000A",
                SNAPSHOT_SHA256),
                "invalid source blob");
        expectIllegal(() -> SharedLexiconPrecomputeCatalog.builder().acronym(
                identity,
                new M3LexiconPrecompute.AcronymPrecompute(
                        "TLS", "Transport Layer Security", "networking",
                    SOURCE_REVISION, SOURCE_BLOB_SHA, SNAPSHOT_SHA256)),
                "identity mismatch");
        expectIllegal(() -> SharedLexiconPrecomputeCatalog.builder().acronym(identity, http).acronym(identity, http),
                "duplicate identity");

        check(catalog.acronymAt(new SharedLexiconPrecomputeCatalog.AcronymIdentity(
                "dictlang.acronyms", "TLS", SOURCE_REVISION,
                SOURCE_BLOB_SHA, SNAPSHOT_SHA256)).equals(Optional.empty()),
                "missing acronym is empty");
        System.out.println("M3_ACRONYM_PRECOMPUTE_PASS source=dictlang.acronyms entries=1");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void expectIllegal(Runnable action, String message) {
        try {
            action.run();
            throw new AssertionError(message + " accepted");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }
}
