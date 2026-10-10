/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import com.m3.text.M3LanguageGrammarSupport;

/** Contract proof for the Synexia grammar-support mapping. */
public final class M3LanguageGrammarSupportTest {
    private static int checks;

    private static void check(boolean condition) {
        checks++;
        if (!condition) throw new AssertionError("check " + checks);
    }

    public static void main(String[] args) throws Exception {
        check(M3LanguageGrammarSupport.SOURCE_ID.equals("translate.rows"));
        check(M3LanguageGrammarSupport.TARGET_FIELD.equals("translation_grammar_supported"));
        check(M3LanguageGrammarSupport.supportedCodes().size() == 16);
        check(M3LanguageGrammarSupport.supports(null) == false);
        check(M3LanguageGrammarSupport.supports(" ") == false);
        check(M3LanguageGrammarSupport.supports("zh") == false);
        check(M3LanguageGrammarSupport.supports("auto") == false);
        for (String code : M3LanguageGrammarSupport.supportedCodes()) {
            check(M3LanguageGrammarSupport.supports(code));
            check(M3LanguageGrammarSupport.supports("  " + code.toUpperCase(Locale.ROOT) + "  "));
        }

        String manifest = Files.readString(
                Path.of("lexicon/synexia-source-manifest.tsv"), StandardCharsets.UTF_8);
        String row = Arrays.stream(manifest.split("\\n", -1))
                .filter(line -> line.startsWith("translate.rows\t"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("translation manifest row missing"));
        String[] columns = row.split("\\t", -1);
        check(columns.length == 9);
        check(columns[5].equals("M3StringFacts + M3LexiconPrecompute.TranslationProjection + SharedLexiconPrecomputeCatalog.TranslationIdentity + M3LanguageGrammarSupport"));
        check(columns[8].equals("translation_grammar_supported"));
        System.out.println("M3JDK_GRAMMAR_SUPPORT_PASS checks=" + checks + " languages=16");
    }
}
