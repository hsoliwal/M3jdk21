// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.ast;

import com.synexia.indexstring.grammar.MIndexLang;
import com.synexia.indexstring.grammar.MIndexLangs;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/** JVM-wide canonical registry of frozen AST-spec precompute images. */
public final class MIndexASTSpecs {
  private static final ConcurrentHashMap<String, MIndexASTSpecPrecompute> SPECS =
      new ConcurrentHashMap<>();
  private static final ConcurrentHashMap<String, HeapMIndexASTSpecImage> HEAP_IMAGES =
      new ConcurrentHashMap<>();

  private MIndexASTSpecs() {}

  public static MIndexASTSpecPrecompute forLang(MIndexLang lang) {
    MIndexLang checked = Objects.requireNonNull(lang, "lang");
    return SPECS.compute(
        checked.identity(),
        (identity, existing) -> {
          if (existing == null) return new MIndexASTSpecPrecompute(checked);
          if (existing.lang().fingerprint() != checked.fingerprint()) {
            throw new IllegalStateException(
                "AST spec identity collision for "
                    + identity
                    + ": existing language fingerprint="
                    + Long.toUnsignedString(existing.lang().fingerprint(), 16)
                    + ", candidate="
                    + Long.toUnsignedString(checked.fingerprint(), 16));
          }
          return existing;
        });
  }

  public static MIndexASTSpecPrecompute byIdentity(String identity) {
    return forLang(MIndexLangs.byIdentity(Objects.requireNonNull(identity, "identity")));
  }

  /** Canonical heap image view over the one process-level AST spec. */
  public static HeapMIndexASTSpecImage heapImage(MIndexLang lang) {
    MIndexLang checked = Objects.requireNonNull(lang, "lang");
    return HEAP_IMAGES.compute(
        checked.identity(),
        (identity, existing) -> {
          if (existing == null) return new HeapMIndexASTSpecImage(forLang(checked));
          if (existing.languageFingerprint() != checked.fingerprint()
              || existing.grammarFingerprint() != checked.grammar().fingerprint()) {
            throw new IllegalStateException("AST image identity collision for " + identity);
          }
          return existing;
        });
  }

  public static HeapMIndexASTSpecImage heapImage(String identity) {
    return heapImage(MIndexLangs.byIdentity(Objects.requireNonNull(identity, "identity")));
  }

  /** Writes a deterministic .midxa sidecar for one canonical language AST spec. */
  public static void writeImage(MIndexLang lang, Path output) throws IOException {
    MIndexASTSpecImage.write(heapImage(lang), Objects.requireNonNull(output, "output"));
  }

  /**
   * Opens a read-only mmap AST-spec image and validates it against the supplied language/grammar
   * fingerprints without deriving another heap AST spec.
   */
  public static MappedMIndexASTSpecImage openMappedImage(MIndexLang lang, Path input)
      throws IOException {
    return MappedMIndexASTSpecImage.open(
        Objects.requireNonNull(input, "input"), Objects.requireNonNull(lang, "lang"));
  }

  /** Forces all current built-in language specs into the process-level AST-spec cache. */
  public static MIndexASTSpecPrecompute[] precomputeBuiltIns() {
    String[] identities = MIndexLangs.builtInIdentities();
    MIndexASTSpecPrecompute[] result = new MIndexASTSpecPrecompute[identities.length];
    for (int index = 0; index < identities.length; index++) {
      result[index] = byIdentity(identities[index]);
    }
    return result;
  }

  public static int cachedSpecCount() {
    return SPECS.size();
  }

  public static int cachedHeapImageCount() {
    return HEAP_IMAGES.size();
  }
}
