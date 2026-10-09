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
    public static void main(String[] args) throws Exception {
        M3LexiconPrecompute.AcronymPrecompute http =
                new M3LexiconPrecompute.AcronymPrecompute(
                        "HTTP", "Hypertext Transfer Protocol", "networking");
        check("HTTP".equals(http.acronym()), "acronym identity");
        check("Hypertext Transfer Protocol".equals(http.expansion()), "expansion preservation");
        check("networking".equals(http.domain()), "domain preservation");

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
        check("acronym,expansion,domain".equals(columns[8]),
                "acronym manifest precompute fields");

        SharedLexiconPrecomputeCatalog.AcronymIdentity identity =
                new SharedLexiconPrecomputeCatalog.AcronymIdentity(
                        "dictlang.acronyms", "HTTP");
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
                identity,
                new M3LexiconPrecompute.AcronymPrecompute(
                        "TLS", "Transport Layer Security", "networking")),
                "identity mismatch");
        expectIllegal(() -> SharedLexiconPrecomputeCatalog.builder().acronym(identity, http).acronym(identity, http),
                "duplicate identity");

        check(catalog.acronymAt(new SharedLexiconPrecomputeCatalog.AcronymIdentity(
                "dictlang.acronyms", "TLS")).equals(Optional.empty()),
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
