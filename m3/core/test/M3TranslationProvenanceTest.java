/* Copyright 2026 Hitesh Soliwal <hitesh.soliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
import com.m3.text.M3LexiconPrecompute;
import com.m3.text.SharedLexiconPrecomputeCatalog;

import java.util.Arrays;

public final class M3TranslationProvenanceTest {
    private static final String SOURCE_REVISION =
            "64a2ea61c73b548413fed6686a9daeeb0b9b0564";
    private static final String SOURCE_BLOB_SHA =
            "bb72c00e36f1835d824a34ab398b1fe5aadb1cb3";
    private static int checks;

    private static void check(boolean value) {
        checks++;
        if (!value) throw new AssertionError("check " + checks);
    }

    private static void expect(Class<? extends Throwable> type, Runnable body) {
        try {
            body.run();
            throw new AssertionError("missing " + type.getName());
        } catch (Throwable failure) {
            check(type.isInstance(failure));
        }
    }

    private static M3LexiconPrecompute.TranslationProjection boundProjection() {
        return new M3LexiconPrecompute.TranslationProjection(
                "lex-1", "en", "hi", "src-1", new int[]{11, 0, 19}, 2,
                SOURCE_REVISION, SOURCE_BLOB_SHA);
    }

    private static SharedLexiconPrecomputeCatalog.TranslationIdentity identity(
            String revision, String blobSha) {
        return new SharedLexiconPrecomputeCatalog.TranslationIdentity(
                "translate.rows", "row-1", "en", "hi", "lex-1", "src-1",
                revision, blobSha);
    }

    public static void main(String[] args) {
        M3LexiconPrecompute.TranslationProjection projection = boundProjection();
        check(projection.sourceBound());
        check(projection.sourceRevision().equals(SOURCE_REVISION));
        check(projection.sourceBlobSha().equals(SOURCE_BLOB_SHA));
        check(projection.mappedTokenCount() == 2);
        check(projection.appliesTo("lex-1", "en", "hi", "src-1"));
        check(!projection.appliesTo("lex-1", "hi", "en", "src-1"));

        int[] copiedIds = projection.translatedTokenIds();
        copiedIds[0] = 999;
        check(projection.translatedTokenIdAt(0) == 11);
        check(Arrays.equals(projection.translatedTokenIds(), new int[]{11, 0, 19}));

        SharedLexiconPrecomputeCatalog.TranslationIdentity matching =
                identity(SOURCE_REVISION, SOURCE_BLOB_SHA);
        SharedLexiconPrecomputeCatalog catalog =
                SharedLexiconPrecomputeCatalog.builder()
                        .translation(matching, projection)
                        .build();
        check(catalog.translationAt(matching).orElseThrow().sourceBound());

        expect(IllegalArgumentException.class, () ->
                SharedLexiconPrecomputeCatalog.builder().translation(
                        identity("0000000000000000000000000000000000000000",
                                SOURCE_BLOB_SHA), projection));
        expect(IllegalArgumentException.class, () ->
                SharedLexiconPrecomputeCatalog.builder().translation(
                        identity(SOURCE_REVISION,
                                "0000000000000000000000000000000000000000"), projection));

        expect(IllegalArgumentException.class, () ->
                new M3LexiconPrecompute.TranslationProjection(
                        "lex-1", "en", "hi", "src-1", new int[]{11}, 1,
                        "ABCDEFABCDEFABCDEFABCDEFABCDEFABCDEFABCD",
                        SOURCE_BLOB_SHA));
        expect(IllegalArgumentException.class, () ->
                new SharedLexiconPrecomputeCatalog.TranslationIdentity(
                        "translate.rows", "row-1", "en", "hi", "lex-1", "src-1",
                        SOURCE_REVISION, "not-a-blob"));

        expect(IllegalArgumentException.class, () ->
                new M3LexiconPrecompute.TranslationProjection(
                        "lex-1", "en", "hi", "src-1", new int[]{7}, 1,
                        "UNPINNED", SOURCE_BLOB_SHA));
        expect(IllegalArgumentException.class, () ->
                new SharedLexiconPrecomputeCatalog.TranslationIdentity(
                        "translate.rows", "row-partial", "en", "hi", "lex-1", "src-1",
                        "UNPINNED", SOURCE_BLOB_SHA));
        expect(IllegalArgumentException.class, () ->
                new M3LexiconPrecompute.TranslationProjection(
                        "lex-1", "en", "hi", "src-1", new int[]{7}, 1,
                        SOURCE_REVISION, "UNPINNED"));
        expect(IllegalArgumentException.class, () ->
                new SharedLexiconPrecomputeCatalog.TranslationIdentity(
                        "translate.rows", "row-partial-reverse", "en", "hi", "lex-1", "src-1",
                        SOURCE_REVISION, "UNPINNED"));

        M3LexiconPrecompute.TranslationProjection legacy =
                new M3LexiconPrecompute.TranslationProjection(
                        "lex-1", "en", "hi", "src-1", new int[]{7});
        check(!legacy.sourceBound());
        SharedLexiconPrecomputeCatalog.TranslationIdentity legacyIdentity =
                new SharedLexiconPrecomputeCatalog.TranslationIdentity(
                        "translate.rows", "legacy", "en", "hi", "lex-1", "src-1");
        check(SharedLexiconPrecomputeCatalog.builder()
                .translation(legacyIdentity, legacy)
                .build().translationAt(legacyIdentity).isPresent());

        System.out.println("M3JDK_TRANSLATION_PROVENANCE_PASS checks=" + checks);
    }
}
