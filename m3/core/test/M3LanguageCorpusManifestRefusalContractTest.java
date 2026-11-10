/* Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
import jdk.m3.text.M3CorpusProjectionDigest;
import jdk.m3.text.M3CorpusSourceReceipt;
import jdk.m3.text.M3LanguageCorpusManifest;
import jdk.m3.text.M3LanguageCorpusManifest.Artifact;
import jdk.m3.text.M3LanguageCorpusManifest.Plane;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

/**
 * Refusal and canonical-identity contracts of the language-corpus manifest, its source receipt and
 * the framed projection digest.
 *
 * <p>Each malformed field, receipt or manifest line is refused with the exact reason, and every
 * binding dimension is rejected independently.
 */
public final class M3LanguageCorpusManifestRefusalContractTest {
    private static final String DICT = "d".repeat(64);
    private static final String OUTPUT = "e".repeat(64);
    private static final String COMMIT = "c".repeat(40);
    private static final URI DATASET =
            URI.create("https://huggingface.co/datasets/example/synthetic");
    private static final URI TERMS = URI.create("https://example.org/terms");
    private static int checks;

    private M3LanguageCorpusManifestRefusalContractTest() {}

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private interface Action {
        void run() throws Exception;
    }

    private static Throwable refused(
            Class<? extends Throwable> type, String reason, Action action) {
        checks++;
        try {
            action.run();
        } catch (Throwable thrown) {
            if (type.isInstance(thrown) && (reason == null || reason.equals(thrown.getMessage()))) {
                return thrown;
            }
            throw new AssertionError(
                    "expected " + type.getSimpleName() + " '" + reason + "'", thrown);
        }
        throw new AssertionError("accepted, expected refusal: " + reason);
    }

    private static M3CorpusSourceReceipt source() {
        return new M3CorpusSourceReceipt(
                "HUGGING_FACE", DATASET, COMMIT, "synthetic-license", DICT);
    }

    /** One valid artifact; tests override exactly one argument at a time. */
    private static Artifact artifact(
            Plane plane,
            String language,
            long generation,
            String dictionary,
            URI rights,
            int order,
            String period) {
        return new Artifact(
                plane,
                language,
                "fixture",
                "word",
                "word-v1",
                "nfc-v1",
                generation,
                99L,
                dictionary,
                OUTPUT,
                source(),
                "synthetic fixture",
                rights,
                order,
                period);
    }

    private static Artifact artifact(Plane plane) {
        return artifact(plane, "hi", 7L, DICT, TERMS, plane == Plane.WORD_NGRAM ? 2 : 0, "2026");
    }

    private static void artifactFields() {
        Artifact noPeriod = artifact(Plane.DICTIONARY, "hi", 7L, DICT, TERMS, 0, null);
        check(noPeriod.period().isEmpty(), "absent period normalizes to empty");
        Artifact upper =
                artifact(
                        Plane.DICTIONARY,
                        "hi",
                        7L,
                        DICT.toUpperCase(java.util.Locale.ROOT),
                        TERMS,
                        0,
                        "2026");
        check(upper.dictionarySha256().equals(DICT), "SHA-256 normalizes to lowercase");
        check(
                artifact(Plane.WORD_NGRAM, "hi", 7L, DICT, TERMS, 64, "2026").order() == 64,
                "maximum n-gram order admitted");
        refused(
                IllegalArgumentException.class,
                "period must be single-line",
                () -> artifact(Plane.DICTIONARY, "hi", 7L, DICT, TERMS, 0, ""));
        refused(
                IllegalArgumentException.class,
                "generation",
                () -> artifact(Plane.DICTIONARY, "hi", -1L, DICT, TERMS, 0, "2026"));
        refused(
                IllegalArgumentException.class,
                "order",
                () -> artifact(Plane.DICTIONARY, "hi", 7L, DICT, TERMS, -1, "2026"));
        refused(
                IllegalArgumentException.class,
                "order",
                () -> artifact(Plane.WORD_NGRAM, "hi", 7L, DICT, TERMS, 65, "2026"));
        refused(
                IllegalArgumentException.class,
                "dictionarySha256 must be SHA-256",
                () -> artifact(Plane.DICTIONARY, "hi", 7L, "d".repeat(63), TERMS, 0, "2026"));
        refused(
                NullPointerException.class,
                "languageTag",
                () -> artifact(Plane.DICTIONARY, null, 7L, DICT, TERMS, 0, "2026"));
        refused(
                NullPointerException.class,
                "plane",
                () -> artifact(null, "hi", 7L, DICT, TERMS, 0, "2026"));
        refused(
                NullPointerException.class,
                "source",
                () ->
                        new Artifact(
                                Plane.DICTIONARY,
                                "hi",
                                "fixture",
                                "word",
                                "word-v1",
                                "nfc-v1",
                                7L,
                                99L,
                                DICT,
                                OUTPUT,
                                null,
                                "a",
                                TERMS,
                                0,
                                ""));
        for (String bad :
                new String[] {" \t ", "two\nlines", "carriage\rreturn", "tab\tseparated"}) {
            refused(
                    IllegalArgumentException.class,
                    "languageTag must be single-line",
                    () -> artifact(Plane.DICTIONARY, bad, 7L, DICT, TERMS, 0, "2026"));
        }
        refused(
                IllegalArgumentException.class,
                "period must be single-line",
                () -> artifact(Plane.DICTIONARY, "hi", 7L, DICT, TERMS, 0, "20\n26"));
        String https = "rightsEvidence must be credential-free HTTPS";
        for (String uri :
                new String[] {
                    "http://example.org/terms",
                    "https:///no-host",
                    "https://user:secret@example.org/terms",
                    "https:opaque-terms",
                    "/relative/terms"
                }) {
            refused(
                    IllegalArgumentException.class,
                    https,
                    () -> artifact(Plane.DICTIONARY, "hi", 7L, DICT, URI.create(uri), 0, "2026"));
        }
        check(
                artifact(
                                Plane.DICTIONARY,
                                "hi",
                                7L,
                                DICT,
                                URI.create("HTTPS://Example.org/t"),
                                0,
                                "x")
                        .rightsEvidence()
                        .getHost()
                        .equals("Example.org"),
                "scheme comparison ignores case");
        refused(
                NullPointerException.class,
                "rightsEvidence",
                () -> artifact(Plane.DICTIONARY, "hi", 7L, DICT, null, 0, "2026"));
    }

    private static void binding() {
        Artifact a = artifact(Plane.WORD_FACTS);
        check(a.bindsTo(DICT, 99L, 7L, "hi", "word-v1", "nfc-v1"), "exact scope binds");
        check(!a.bindsTo(OUTPUT, 99L, 7L, "hi", "word-v1", "nfc-v1"), "dictionary dimension");
        check(!a.bindsTo(DICT, 98L, 7L, "hi", "word-v1", "nfc-v1"), "coordinate dimension");
        check(!a.bindsTo(DICT, 99L, 8L, "hi", "word-v1", "nfc-v1"), "generation dimension");
        check(!a.bindsTo(DICT, 99L, 7L, "de", "word-v1", "nfc-v1"), "language dimension");
        check(!a.bindsTo(DICT, 99L, 7L, "hi", "word-v2", "nfc-v1"), "tokenizer dimension");
        check(!a.bindsTo(DICT, 99L, 7L, "hi", "word-v1", "nfd-v1"), "normalization dimension");
    }

    private static void scopes() {
        refused(
                IllegalArgumentException.class,
                "manifest needs an artifact",
                () -> M3LanguageCorpusManifest.of(List.of()));
        refused(NullPointerException.class, "artifacts", () -> M3LanguageCorpusManifest.of(null));
        M3LanguageCorpusManifest manifest =
                M3LanguageCorpusManifest.of(
                        List.of(artifact(Plane.WORD_FACTS), artifact(Plane.DICTIONARY)));
        check(manifest.artifacts().get(0).plane() == Plane.DICTIONARY, "canonical identity order");
        refused(
                IllegalArgumentException.class,
                "generation",
                () -> manifest.requireBoundArtifacts("hi", -1L, DICT, 99L, "word-v1", "nfc-v1"));
        refused(
                IllegalArgumentException.class,
                "no corpus artifacts for language scope",
                () -> manifest.requireBoundArtifacts("de", 7L, DICT, 99L, "word-v1", "nfc-v1"));
        refused(
                IllegalArgumentException.class,
                "dictionarySha256 must be SHA-256",
                () -> manifest.requireBoundArtifacts("hi", 7L, "nope", 99L, "word-v1", "nfc-v1"));
        check(
                manifest.requireBoundArtifactsForPlanes(
                                        "hi",
                                        7L,
                                        DICT,
                                        99L,
                                        "word-v1",
                                        "nfc-v1",
                                        EnumSet.of(Plane.DICTIONARY))
                                .size()
                        == 2,
                "required plane is a gate, not a filter");
        refused(
                IllegalArgumentException.class,
                "missing required corpus planes: [TRANSLATION, WORD_NGRAM]",
                () ->
                        manifest.requireBoundArtifactsForPlanes(
                                "hi",
                                7L,
                                DICT,
                                99L,
                                "word-v1",
                                "nfc-v1",
                                Set.of(Plane.WORD_NGRAM, Plane.DICTIONARY, Plane.TRANSLATION)));
        refused(
                NullPointerException.class,
                "requiredPlanes",
                () ->
                        manifest.requireBoundArtifactsForPlanes(
                                "hi", 7L, DICT, 99L, "word-v1", "nfc-v1", null));
    }

    private static void persistence() throws Exception {
        M3LanguageCorpusManifest manifest =
                M3LanguageCorpusManifest.of(
                        List.of(artifact(Plane.DICTIONARY), artifact(Plane.WORD_NGRAM)));
        Path dir = Files.createTempDirectory("m3-corpus-refusal-");
        try {
            Path occupied = dir.resolve("corpus-" + manifest.root() + ".m3corpus");
            Files.writeString(occupied, "another manifest with the same root name");
            Throwable collision =
                    refused(
                            IOException.class,
                            "corpus manifest root collision",
                            () -> manifest.writeNew(dir));
            check(
                    collision.getCause() instanceof java.nio.file.FileAlreadyExistsException,
                    "race cause");
            Files.delete(occupied);
            Path canonical = manifest.writeNew(dir);
            check(
                    M3LanguageCorpusManifest.read(canonical).root().equals(manifest.root()),
                    "round trip");
            String text = Files.readString(canonical, StandardCharsets.UTF_8);
            String header = M3LanguageCorpusManifest.HEADER + "\n";
            String first = text.substring(header.length(), text.indexOf('\n', header.length()) + 1);
            corrupt(dir, "", "unsupported language corpus manifest", null);
            corrupt(
                    dir,
                    "M3-MINDEX-LANGUAGE-CORPUS-0\n" + text.substring(header.length()),
                    "unsupported language corpus manifest",
                    null);
            corrupt(
                    dir,
                    header + first.replaceFirst("\t[^\t]*\n$", "\n"),
                    "invalid corpus manifest line 2",
                    null);
            corrupt(
                    dir,
                    header + first.replaceFirst("^artifact", "artefact"),
                    "invalid corpus manifest line 2",
                    null);
            corrupt(
                    dir,
                    header + first.replaceFirst("^artifact\tDICTIONARY", "artifact\tLEXEMES"),
                    "invalid corpus manifest line 2",
                    IllegalArgumentException.class);
            corrupt(
                    dir,
                    header + first + first,
                    "invalid corpus manifest",
                    IllegalArgumentException.class);
            corrupt(dir, header, "invalid corpus manifest", IllegalArgumentException.class);
            Files.delete(canonical);
        } finally {
            try (var files = Files.list(dir)) {
                for (Path file : files.toList()) Files.delete(file);
            }
            Files.delete(dir);
        }
    }

    private static void corrupt(Path dir, String text, String reason, Class<?> cause)
            throws IOException {
        Path file =
                Files.writeString(dir.resolve("corrupt.m3corpus"), text, StandardCharsets.UTF_8);
        Throwable refusal =
                refused(IOException.class, reason, () -> M3LanguageCorpusManifest.read(file));
        check(
                cause == null ? refusal.getCause() == null : cause.isInstance(refusal.getCause()),
                "cause of " + reason);
        Files.delete(file);
    }

    private static void receipts() {
        String sha = M3CorpusProjectionDigest.bytes(new byte[] {1, 2, 3});
        URI uri = URI.create("https://www.kaggle.com/datasets/example/synthetic");
        check(
                new M3CorpusSourceReceipt("KAGGLE", uri, "12", "cc0", sha).revision().equals("12"),
                "Kaggle dataset version");
        check(
                new M3CorpusSourceReceipt("FIXTURE", uri, "any-label", "cc0", sha)
                        .provider()
                        .equals("FIXTURE"),
                "other providers keep their revision label");
        refused(
                IllegalArgumentException.class,
                "Kaggle requires an explicit dataset version number",
                () -> new M3CorpusSourceReceipt("KAGGLE", uri, "0", "cc0", sha));
        refused(
                IllegalArgumentException.class,
                "Hugging Face requires a full commit revision",
                () -> new M3CorpusSourceReceipt("HUGGING_FACE", uri, "main", "cc0", sha));
        refused(
                IllegalArgumentException.class,
                "source URI",
                () ->
                        new M3CorpusSourceReceipt(
                                "FIXTURE", URI.create("datasets/relative"), "1", "cc0", sha));
        refused(
                IllegalArgumentException.class,
                "source URI",
                () ->
                        new M3CorpusSourceReceipt(
                                "FIXTURE",
                                URI.create("https://user@example.org/data"),
                                "1",
                                "cc0",
                                sha));
        refused(
                NullPointerException.class,
                "uri",
                () -> new M3CorpusSourceReceipt("FIXTURE", null, "1", "cc0", sha));
        refused(
                IllegalArgumentException.class,
                "lowercase SHA-256 required",
                () ->
                        new M3CorpusSourceReceipt(
                                "FIXTURE",
                                uri,
                                "1",
                                "cc0",
                                sha.toUpperCase(java.util.Locale.ROOT)));
        refused(
                NullPointerException.class,
                "sha256",
                () -> new M3CorpusSourceReceipt("FIXTURE", uri, "1", "cc0", null));
        refused(
                NullPointerException.class,
                "value",
                () -> new M3CorpusSourceReceipt(null, uri, "1", "cc0", sha));
        String single = "nonblank single-line metadata required";
        for (String bad : new String[] {"", "   ", "a\nb", "a\tb", "a\rb"}) {
            refused(
                    IllegalArgumentException.class,
                    single,
                    () -> new M3CorpusSourceReceipt("FIXTURE", uri, bad, "cc0", sha));
        }
        refused(
                IllegalArgumentException.class,
                single,
                () -> new M3CorpusSourceReceipt("FIXTURE", uri, "1", " ", sha));
        M3CorpusSourceReceipt receipt = new M3CorpusSourceReceipt("FIXTURE", uri, "1", "cc0", sha);
        receipt.verify(new byte[] {1, 2, 3});
        refused(NullPointerException.class, "bytes", () -> receipt.verify(null));
        refused(
                IllegalArgumentException.class,
                "input exceeds 16 MiB",
                () -> receipt.verify(new byte[16 * 1024 * 1024 + 1]));
        refused(
                IllegalArgumentException.class,
                "source hash mismatch",
                () -> receipt.verify(new byte[] {1, 2}));
    }

    /** Framing: 64-bit big-endian numbers; text = UTF-16 length then big-endian code units. */
    private static void projectionDigest() throws Exception {
        String text = "Aé😀";
        ByteBuffer frame = ByteBuffer.allocate(8 + 8 + text.length() * 2 + 8);
        frame.putLong(0x0102030405060708L)
                .putLong(text.length())
                .put(text.getBytes(StandardCharsets.UTF_16BE))
                .putLong(-1L);
        String expected =
                HexFormat.of()
                        .formatHex(MessageDigest.getInstance("SHA-256").digest(frame.array()));
        M3CorpusProjectionDigest digest = new M3CorpusProjectionDigest();
        check(
                digest.number(0x0102030405060708L) == digest && digest.text(text) == digest,
                "fluent framing");
        check(
                expected.equals(digest.number(-1L).finish()),
                "framed digest matches independent oracle");
        String empty =
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(new byte[8]));
        check(
                empty.equals(new M3CorpusProjectionDigest().text("").finish()),
                "empty text frames its zero length");
        check(
                !new M3CorpusProjectionDigest()
                        .text("ab")
                        .finish()
                        .equals(new M3CorpusProjectionDigest().text("a").text("b").finish()),
                "length framing separates fields");
        byte[] raw = "raw".getBytes(StandardCharsets.UTF_8);
        check(
                M3CorpusProjectionDigest.bytes(raw)
                        .equals(
                                HexFormat.of()
                                        .formatHex(
                                                MessageDigest.getInstance("SHA-256").digest(raw))),
                "raw byte digest");
    }

    public static void main(String[] args) throws Exception {
        artifactFields();
        binding();
        scopes();
        persistence();
        receipts();
        projectionDigest();
        System.out.println("M3_CORPUS_MANIFEST_REFUSAL_PASS checks=" + checks);
    }
}
