// SPDX-License-Identifier: Apache-2.0
package com.m3.a3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class A3AlgTest {

    @TempDir
    Path root;

    @Test
    void validatesTriPlatformEvidenceAndLicenseBoundLineage() throws Exception {
        writeCatalogue(
                row(
                        "ALG-BIN",
                        "SEARCH",
                        "BINARY_EXACT",
                        "admitted",
                        "FILE",
                        "src/java.base/share/classes/java/util/Arrays.java",
                        "https://leetcode.com/problems/binary-search/",
                        "https://www.hackerrank.com/challenges/tutorial-intro/problem",
                        "https://www.geeksforgeeks.org/dsa/what-is-binary-search-algorithm/",
                        "https://github.com/openjdk/jdk",
                        "GPL-2.0-WITH-CLASSPATH-EXCEPTION",
                        "JDK_OWNED",
                        "inventory owner"),
                row(
                        "ALG-Z",
                        "STRING_SEARCH",
                        "Z_PREFIX",
                        "candidate-adapted",
                        "FILE",
                        "m3/algorithms/src/com/m3/algorithm/M3PrefixZ.java",
                        "https://leetcode.com/problems/sum-of-scores-of-built-strings/",
                        "https://www.hackerrank.com/challenges/string-similarity/problem",
                        "https://www.geeksforgeeks.org/dsa/z-algorithm-linear-time-pattern-searching-algorithm/",
                        "https://github.com/hsoliwal/com.synexia",
                        "Apache-2.0",
                        "ADAPT_PERMISSIVE",
                        "prove UTF-16"),
                row(
                        "ALG-FIB",
                        "SEARCH",
                        "FIBONACCI_SEARCH",
                        "hold-evidence",
                        "FILE",
                        "NONE",
                        "NONE",
                        "NONE",
                        "https://www.geeksforgeeks.org/dsa/fibonacci-search-in-python/",
                        "NONE",
                        "NONE",
                        "REFERENCE_ONLY",
                        "complete evidence"));

        List<A3Alg.Row> rows = A3Alg.load(root);

        assertEquals(3, rows.size());
        A3Alg.Row binary = require(rows, "ALG-BIN");
        assertTrue(binary.triPlatform());
        assertTrue(binary.implementationLineageAvailable());
        assertFalse(binary.challengeSourceCopyAuthority());

        A3Alg.Row z = require(rows, "ALG-Z");
        assertTrue(z.triPlatform());
        assertTrue(z.implementationLineageAvailable());

        A3Alg.Row fib = require(rows, "ALG-FIB");
        assertFalse(fib.triPlatform());
        assertFalse(fib.implementationLineageAvailable());
        assertFalse(fib.challengeSourceCopyAuthority());

        A3Alg.write(root, Path.of("m3/build/a3/algorithms.tsv"));
        String written =
                Files.readString(root.resolve("m3/build/a3/algorithms.tsv"));
        assertTrue(written.contains("ALG-BIN"));
        assertTrue(written.contains("\ttrue\ttrue\tfalse\t"));
        assertTrue(written.contains("ALG-FIB"));
    }

    @Test
    void strictCandidateRequiresAllThreeChallengePlatforms() throws Exception {
        writeCatalogue(
                row(
                        "ALG-BAD",
                        "SEARCH",
                        "FIBONACCI_SEARCH",
                        "candidate",
                        "FILE",
                        "NONE",
                        "NONE",
                        "NONE",
                        "https://www.geeksforgeeks.org/dsa/fibonacci-search-in-python/",
                        "NONE",
                        "NONE",
                        "REFERENCE_ONLY",
                        "missing evidence"));

        assertThrows(IllegalArgumentException.class, () -> A3Alg.load(root));
    }

    @Test
    void permissiveAdaptationRequiresReviewedGithubLicense() throws Exception {
        writeCatalogue(
                row(
                        "ALG-BAD-LICENSE",
                        "STRING_SEARCH",
                        "Z_PREFIX",
                        "candidate-adapted",
                        "FILE",
                        "m3/algorithms/src/com/m3/algorithm/M3PrefixZ.java",
                        "https://leetcode.com/problems/sum-of-scores-of-built-strings/",
                        "https://www.hackerrank.com/challenges/string-similarity/problem",
                        "https://www.geeksforgeeks.org/dsa/z-algorithm-linear-time-pattern-searching-algorithm/",
                        "https://example.invalid/donor",
                        "UNKNOWN",
                        "ADAPT_PERMISSIVE",
                        "review license"));

        assertThrows(IllegalArgumentException.class, () -> A3Alg.load(root));
    }

    private void writeCatalogue(String... rows) throws Exception {
        Path backports = root.resolve("m3/backports");
        Files.createDirectories(backports);
        String header =
                "atom_id\tcategory\ttechnique\tdisposition\tscope\ttarget_owner\t"
                        + "leetcode_ref\thackerrank_ref\tgeeksforgeeks_ref\tgithub_ref\t"
                        + "github_license\treuse_policy\tnext_proof\n";
        Files.writeString(
                backports.resolve("ALGORITHM_CATALOGUE.tsv"),
                header + String.join("", rows));
    }

    private static String row(String... cells) {
        return String.join("\t", cells) + "\n";
    }

    private static A3Alg.Row require(List<A3Alg.Row> rows, String id) {
        return rows.stream()
                .filter(row -> row.atomId().equals(id))
                .findFirst()
                .orElseThrow();
    }
}
