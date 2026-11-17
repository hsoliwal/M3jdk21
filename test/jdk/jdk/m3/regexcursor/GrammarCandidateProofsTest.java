// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.regexcursor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * JUnit/Maven entry point for the language-spec grammar precompute proofs (com.synexia #18253):
 * each proof runs as it does standalone, and the image tool regenerates the example grammar and
 * image byte for byte, as the candidate's verify.sh does.
 */
class GrammarCandidateProofsTest {
    private static Path examples() {
        return Path.of(System.getProperty("m3.regexcursor.examples", "examples"));
    }

    @Test
    void specification() {
        GrammarSpecificationProof.main(new String[0]);
    }

    @Test
    void ebnf() {
        GrammarEbnfProof.main(new String[0]);
    }

    @Test
    void image() {
        GrammarImageProof.main(new String[0]);
    }

    @Test
    void imageUpgradeAndLegacyReadback() throws Exception {
        GrammarImageV2Proof.main(new String[0]);
    }

    @Test
    void streaming() {
        GrammarStreamingProof.main(new String[0]);
    }

    @Test
    void rfc8259Json() {
        Rfc8259JsonProof.main(new String[0]);
    }

    @Test
    void imageToolRegeneratesTheExamples(@TempDir Path out) throws Exception {
        Path grammar = out.resolve("expression.m3grammar");
        Path image = out.resolve("expression.m3image");
        GrammarImageTool.main(new String[] {
                examples().resolve("expression.lexicon.tsv").toString(),
                examples().resolve("expression.ebnf").toString(), "expression",
                grammar.toString(), image.toString()});
        assertArrayEquals(Files.readAllBytes(examples().resolve("expression.m3grammar")),
                Files.readAllBytes(grammar));
        assertArrayEquals(Files.readAllBytes(examples().resolve("expression.m3image")),
                Files.readAllBytes(image));
    }
}
