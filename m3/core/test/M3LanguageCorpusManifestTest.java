// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
import com.m3.text.M3CorpusProjectionDigest;
import com.m3.text.M3CorpusSourceReceipt;
import com.m3.text.M3LanguageCorpusManifest;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public final class M3LanguageCorpusManifestTest {
    private static int checks;
    private static final String DICT = "a".repeat(64);
    private static final String OUTPUT = "b".repeat(64);
    private static final String COMMIT = "c".repeat(40);

    private static void check(boolean passed) {
        checks++;
        if (!passed) throw new AssertionError("corpus gate check " + checks);
    }

    private static void rejected(Class<? extends Throwable> expected, Runnable operation) {
        try {
            operation.run();
        } catch (Throwable failure) {
            check(expected.isInstance(failure));
            return;
        }
        throw new AssertionError("expected rejection " + expected.getName());
    }

    private static M3LanguageCorpusManifest.Artifact row(
            M3LanguageCorpusManifest.Plane plane, int order, String dictionary) {
        return new M3LanguageCorpusManifest.Artifact(
                plane, "hi", "fixture-only", "word", "word-v1", "nfc-v1",
                7L, 99L, dictionary, OUTPUT,
                new M3CorpusSourceReceipt("HUGGING_FACE",
                        URI.create("https://huggingface.co/datasets/example/synthetic"),
                        COMMIT, "synthetic-test-license", DICT),
                "synthetic fixture only; no external corpus",
                URI.create("https://example.org/terms"), order, "2026");
    }

    public static void main(String[] args) throws Exception {
        List<M3LanguageCorpusManifest.Artifact> fixtures = new ArrayList<>();
        for (var plane : M3LanguageCorpusManifest.Plane.values()) {
            fixtures.add(row(plane,
                    plane == M3LanguageCorpusManifest.Plane.WORD_NGRAM ? 3 : 0, DICT));
        }
        var manifest = M3LanguageCorpusManifest.of(fixtures);
        check(manifest.artifacts().size() == 5);
        check(manifest.forLanguage("hi", 7L).size() == 5);
        check(manifest.forLanguage("de", 7L).isEmpty());
        check(manifest.requireBoundArtifactsForPlanes(
                "hi", 7L, DICT, 99L, "word-v1", "nfc-v1",
                EnumSet.allOf(M3LanguageCorpusManifest.Plane.class)).size() == 5);
        check(manifest.artifacts().stream().allMatch(artifact ->
                artifact.bindsTo(DICT, 99L, 7L, "hi", "word-v1", "nfc-v1")));
        rejected(IllegalArgumentException.class, () -> manifest.requireBoundArtifacts(
                "hi", 7L, OUTPUT, 99L, "word-v1", "nfc-v1"));
        rejected(IllegalArgumentException.class, () -> manifest.requireBoundArtifacts(
                "hi", 7L, DICT, 98L, "word-v1", "nfc-v1"));
        rejected(IllegalArgumentException.class, () -> manifest.requireBoundArtifacts(
                "hi", 8L, DICT, 99L, "word-v1", "nfc-v1"));
        rejected(IllegalArgumentException.class, () -> manifest.requireBoundArtifacts(
                "hi", 7L, DICT, 99L, "word-v2", "nfc-v1"));
        rejected(IllegalArgumentException.class, () -> manifest.requireBoundArtifacts(
                "hi", 7L, DICT, 99L, "word-v1", "nfd-v1"));
        rejected(IllegalArgumentException.class, () ->
                manifest.requireBoundArtifactsForPlanes("hi", 7L, DICT, 99L,
                        "word-v1", "nfc-v1", EnumSet.noneOf(
                                M3LanguageCorpusManifest.Plane.class)));
        rejected(IllegalArgumentException.class, () -> M3LanguageCorpusManifest.of(
                List.of(row(M3LanguageCorpusManifest.Plane.WORD_FACTS, 0, DICT),
                        row(M3LanguageCorpusManifest.Plane.WORD_FACTS, 0, DICT))));
        rejected(IllegalArgumentException.class, () -> row(
                M3LanguageCorpusManifest.Plane.WORD_NGRAM, 0, DICT));
        rejected(IllegalArgumentException.class, () -> row(
                M3LanguageCorpusManifest.Plane.MORPHOLOGY, 3, DICT));
        rejected(IllegalArgumentException.class, () -> new M3CorpusSourceReceipt(
                "HUGGING_FACE", URI.create("https://example.org"), "not-a-commit",
                "synthetic", DICT));
        check(manifest.root().matches("[0-9a-f]{64}"));
        check(manifest.canonicalBytes().length > 0);

        Path directory = Files.createTempDirectory("m3corpus");
        try {
            Path target = manifest.writeNew(directory);
            check(Files.isRegularFile(target));
            check(manifest.root().equals(M3LanguageCorpusManifest.read(target).root()));
            check(manifest.writeNew(directory).equals(target));
            Files.writeString(target, Files.readString(target) + "\n");
            try {
                M3LanguageCorpusManifest.read(target);
                throw new AssertionError("noncanonical input accepted");
            } catch (IOException expected) {
                check(true);
            }
            Files.delete(target);
        } finally {
            Files.deleteIfExists(directory);
        }
        byte[] sample = "alpha".getBytes(StandardCharsets.UTF_8);
        var receipt = new M3CorpusSourceReceipt("HUGGING_FACE",
                URI.create("https://huggingface.co/datasets/example/synthetic"),
                COMMIT, "synthetic", M3CorpusProjectionDigest.bytes(sample));
        receipt.verify(sample);
        check(true);
        rejected(IllegalArgumentException.class, () -> receipt.verify(
                "alphA".getBytes(StandardCharsets.UTF_8)));
        System.out.println("M3_HF_CORPUS_MANIFEST_PASS checks=" + checks
                + " planes=5 external_dataset_bytes=0");
    }
}
