// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package com.m3.text;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Immutable binding for optional language evidence planes.
 *
 * <p>A dictionary, word-frequency table, morphology table, translation projection, and
 * word-sequence n-gram image are different observations. This manifest keeps them separate while
 * requiring every plane to bind to the exact dictionary bytes, ordered coordinate fingerprint,
 * generation, language, and tokenization policy that it was built against. It records custody
 * metadata only; it neither downloads nor interprets a third-party dataset.</p>
 */
public final class M3LanguageCorpusManifest {
  public static final String HEADER = "SYNEXIA-MINDEX-LANGUAGE-CORPUS-1";
  private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");

  public enum Plane {
    DICTIONARY,
    WORD_FACTS,
    MORPHOLOGY,
    TRANSLATION,
    WORD_NGRAM
  }

  /** One source-scoped immutable output plane for one language generation. */
  public record Artifact(
      Plane plane,
      String languageTag,
      String corpusId,
      String tokenKind,
      String tokenizerVersion,
      String normalizationVersion,
      long generation,
      long coordinateFingerprint,
      String dictionarySha256,
      String outputSha256,
      M3CorpusSourceReceipt source,
      String attribution,
      URI rightsEvidence,
      int order,
      String period) {
    public Artifact {
      Objects.requireNonNull(plane, "plane");
      languageTag = text(languageTag, "languageTag");
      corpusId = text(corpusId, "corpusId");
      tokenKind = text(tokenKind, "tokenKind");
      tokenizerVersion = text(tokenizerVersion, "tokenizerVersion");
      normalizationVersion = text(normalizationVersion, "normalizationVersion");
      if (generation < 0) throw new IllegalArgumentException("generation");
      dictionarySha256 = sha256(dictionarySha256, "dictionarySha256");
      outputSha256 = sha256(outputSha256, "outputSha256");
      Objects.requireNonNull(source, "source");
      attribution = text(attribution, "attribution");
      rightsEvidence = safeHttps(rightsEvidence);
      period = period == null ? "" : singleLine(period, "period");
      if (order < 0 || order > 64) throw new IllegalArgumentException("order");
      if (plane == Plane.WORD_NGRAM ? order < 1 : order != 0) {
        throw new IllegalArgumentException("word n-gram order must be 1..64; other planes use 0");
      }
    }

    /** True only when this output can be attached to the requested immutable dictionary. */
    public boolean bindsTo(
        String expectedDictionarySha256,
        long expectedCoordinateFingerprint,
        long expectedGeneration,
        String expectedLanguageTag,
        String expectedTokenizerVersion,
        String expectedNormalizationVersion) {
      return dictionarySha256.equals(expectedDictionarySha256)
          && coordinateFingerprint == expectedCoordinateFingerprint
          && generation == expectedGeneration
          && languageTag.equals(expectedLanguageTag)
          && tokenizerVersion.equals(expectedTokenizerVersion)
          && normalizationVersion.equals(expectedNormalizationVersion);
    }

    private String identity() {
      return plane + "\u0000" + languageTag + "\u0000" + corpusId + "\u0000"
          + source.provider() + "\u0000" + source.uri() + "\u0000" + source.revision();
    }
  }

  private final List<Artifact> artifacts;
  private final byte[] canonicalBytes;
  private final String root;

  private M3LanguageCorpusManifest(List<Artifact> input) {
    ArrayList<Artifact> sorted = new ArrayList<>(Objects.requireNonNull(input, "artifacts"));
    if (sorted.isEmpty()) throw new IllegalArgumentException("manifest needs an artifact");
    sorted.sort(Comparator.comparing(Artifact::identity));
    HashSet<String> identities = new HashSet<>();
    for (Artifact artifact : sorted) {
      if (!identities.add(artifact.identity())) {
        throw new IllegalArgumentException("duplicate corpus artifact identity");
      }
    }
    artifacts = List.copyOf(sorted);
    canonicalBytes = canonical(artifacts);
    root = digest(canonicalBytes);
  }

  public static M3LanguageCorpusManifest of(List<Artifact> artifacts) {
    return new M3LanguageCorpusManifest(artifacts);
  }

  public List<Artifact> artifacts() {
    return artifacts;
  }

  public String root() {
    return root;
  }

  public byte[] canonicalBytes() {
    return canonicalBytes.clone();
  }

  /** Return only artifacts for one immutable language generation. */
  public List<Artifact> forLanguage(String languageTag, long generation) {
    return artifacts.stream()
        .filter(a -> a.languageTag().equals(languageTag) && a.generation() == generation)
        .toList();
  }

  /**
   * Return all planes that can attach to one exact immutable language scope, or fail closed.
   *
   * <p>This is the hand-off used by first-touch loaders: a caller cannot accidentally select a
   * subset that contains a changed dictionary, coordinate order, tokenizer, or normalization
   * policy. The returned list retains the manifest's canonical artifact order.</p>
   */
  public List<Artifact> requireBoundArtifacts(
      String languageTag,
      long generation,
      String dictionarySha256,
      long coordinateFingerprint,
      String tokenizerVersion,
      String normalizationVersion) {
    String checkedLanguage = text(languageTag, "languageTag");
    if (generation < 0) throw new IllegalArgumentException("generation");
    String checkedDictionary = sha256(dictionarySha256, "dictionarySha256");
    String checkedTokenizer = text(tokenizerVersion, "tokenizerVersion");
    String checkedNormalization = text(normalizationVersion, "normalizationVersion");
    List<Artifact> selected = forLanguage(checkedLanguage, generation);
    if (selected.isEmpty()) throw new IllegalArgumentException("no corpus artifacts for language scope");
    for (Artifact artifact : selected) {
      if (!artifact.bindsTo(checkedDictionary, coordinateFingerprint, generation,
          checkedLanguage, checkedTokenizer, checkedNormalization)) {
        throw new IllegalArgumentException("corpus artifact does not bind to requested scope");
      }
    }
    return selected;
  }

  /**
   * Require a non-empty plane set in addition to the exact immutable language binding.
   *
   * <p>All compatible artifacts remain returned in canonical order; the required set is a
   * completeness gate, not an implicit filter that could hide an optional plane.</p>
   */
  public List<Artifact> requireBoundArtifactsForPlanes(
      String languageTag,
      long generation,
      String dictionarySha256,
      long coordinateFingerprint,
      String tokenizerVersion,
      String normalizationVersion,
      Set<Plane> requiredPlanes) {
    Set<Plane> checkedRequired =
        Set.copyOf(Objects.requireNonNull(requiredPlanes, "requiredPlanes"));
    if (checkedRequired.isEmpty()) throw new IllegalArgumentException("requiredPlanes");
    List<Artifact> selected = requireBoundArtifacts(
        languageTag,
        generation,
        dictionarySha256,
        coordinateFingerprint,
        tokenizerVersion,
        normalizationVersion);
    EnumSet<Plane> present = EnumSet.noneOf(Plane.class);
    selected.forEach(artifact -> present.add(artifact.plane()));
    if (!present.containsAll(checkedRequired)) {
      EnumSet<Plane> missing = EnumSet.copyOf(checkedRequired);
      missing.removeAll(present);
      throw new IllegalArgumentException("missing required corpus planes: " + missing);
    }
    return selected;
  }

  public Path writeNew(Path directory) throws IOException {
    Path targetDirectory = Objects.requireNonNull(directory, "directory")
        .toAbsolutePath().normalize();
    Files.createDirectories(targetDirectory);
    Path output = targetDirectory.resolve("corpus-" + root + ".m3corpus");
    try {
      Files.write(output, canonicalBytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
    } catch (java.nio.file.FileAlreadyExistsException race) {
      if (!java.util.Arrays.equals(Files.readAllBytes(output), canonicalBytes)) {
        throw new IOException("corpus manifest root collision", race);
      }
    }
    return output;
  }

  public static M3LanguageCorpusManifest read(Path path) throws IOException {
    byte[] bytes = Files.readAllBytes(Objects.requireNonNull(path, "path"));
    String text = new String(bytes, StandardCharsets.UTF_8);
    String[] lines = text.split("\\n", -1);
    if (lines.length == 0 || !HEADER.equals(lines[0])) {
      throw new IOException("unsupported language corpus manifest");
    }
    ArrayList<Artifact> parsed = new ArrayList<>();
    for (int line = 1; line < lines.length; line++) {
      if (lines[line].isEmpty()) continue;
      String[] fields = lines[line].split("\\t", -1);
      if (fields.length != 20 || !"artifact".equals(fields[0])) {
        throw new IOException("invalid corpus manifest line " + (line + 1));
      }
      try {
        parsed.add(new Artifact(
            Plane.valueOf(fields[1]), decode(fields[2]), decode(fields[3]), decode(fields[4]),
            decode(fields[5]), decode(fields[6]), Long.parseUnsignedLong(fields[7], 16),
            Long.parseUnsignedLong(fields[8], 16), fields[9], fields[10],
            new M3CorpusSourceReceipt(fields[11], URI.create(decode(fields[12])), decode(fields[13]),
                decode(fields[14]), fields[15]),
            decode(fields[16]), URI.create(decode(fields[17])), Integer.parseInt(fields[18]),
            decode(fields[19])));
      } catch (IllegalArgumentException malformed) {
        throw new IOException("invalid corpus manifest line " + (line + 1), malformed);
      }
    }
    M3LanguageCorpusManifest manifest;
    try {
      manifest = of(parsed);
    } catch (IllegalArgumentException malformed) {
      throw new IOException("invalid corpus manifest", malformed);
    }
    if (!java.util.Arrays.equals(bytes, manifest.canonicalBytes)) {
      throw new IOException("corpus manifest is not canonical");
    }
    return manifest;
  }

  private static byte[] canonical(List<Artifact> artifacts) {
    StringBuilder out = new StringBuilder(256 + artifacts.size() * 400);
    out.append(HEADER).append('\n');
    for (Artifact artifact : artifacts) {
      M3CorpusSourceReceipt source = artifact.source();
      out.append("artifact").append('\t').append(artifact.plane()).append('\t')
          .append(encode(artifact.languageTag())).append('\t').append(encode(artifact.corpusId()))
          .append('\t').append(encode(artifact.tokenKind())).append('\t')
          .append(encode(artifact.tokenizerVersion())).append('\t')
          .append(encode(artifact.normalizationVersion())).append('\t')
          .append(Long.toUnsignedString(artifact.generation(), 16)).append('\t')
          .append(Long.toUnsignedString(artifact.coordinateFingerprint(), 16)).append('\t')
          .append(artifact.dictionarySha256()).append('\t').append(artifact.outputSha256())
          .append('\t').append(source.provider()).append('\t').append(encode(source.uri().toString()))
          .append('\t').append(encode(source.revision())).append('\t').append(encode(source.license()))
          .append('\t').append(source.sha256()).append('\t').append(encode(artifact.attribution()))
          .append('\t').append(encode(artifact.rightsEvidence().toString())).append('\t')
          .append(artifact.order()).append('\t').append(encode(artifact.period())).append('\n');
    }
    return out.toString().getBytes(StandardCharsets.UTF_8);
  }

  private static String encode(String value) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
  }

  private static String decode(String value) {
    return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
  }

  private static String digest(byte[] bytes) {
    try {
      return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException("SHA-256 unavailable", impossible);
    }
  }

  private static String sha256(String value, String name) {
    String checked = text(value, name).toLowerCase(Locale.ROOT);
    if (!SHA256.matcher(checked).matches()) throw new IllegalArgumentException(name + " must be SHA-256");
    return checked;
  }

  private static String text(String value, String name) {
    return singleLine(Objects.requireNonNull(value, name), name);
  }

  private static String singleLine(String value, String name) {
    if (value.isBlank() || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0
        || value.indexOf('\t') >= 0) throw new IllegalArgumentException(name + " must be single-line");
    return value;
  }

  private static URI safeHttps(URI value) {
    URI checked = Objects.requireNonNull(value, "rightsEvidence");
    if (!"https".equalsIgnoreCase(checked.getScheme()) || checked.getHost() == null
        || checked.getRawUserInfo() != null || checked.isOpaque()) {
      throw new IllegalArgumentException("rightsEvidence must be credential-free HTTPS");
    }
    return checked;
  }
}
