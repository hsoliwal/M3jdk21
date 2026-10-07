// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.ast;

import com.synexia.indexstring.grammar.MIndexGrammarSymbolKind;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.security.DigestOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * Deterministic versioned compiled image for one immutable AST specification/production plan.
 *
 * <p>The image is a derived acceleration artifact. The canonical grammar/language fingerprint is
 * embedded in the header and remains the authority. A SHA-256 trailer covers the complete header
 * and payload.</p>
 */
public final class MIndexASTSpecImage {
  static final long MAGIC = 0x4d49445841535031L; // MIDXASP1
  static final int LEGACY_FORMAT = 1;
  static final int FORMAT = 2;
  static final int HEADER_BYTES = 128;
  static final int SHA256_BYTES = 32;

  static final int SYMBOL_FLAG_NULLABLE = 1;
  static final int SYMBOL_FLAG_PRODUCTIVE = 1 << 1;
  static final int SYMBOL_FLAG_REACHABLE = 1 << 2;
  static final int SYMBOL_FLAG_LEFT_RECURSIVE = 1 << 3;
  static final int SYMBOL_FLAG_CAN_END = 1 << 4;
  static final int SYMBOL_FLAG_OPERATOR = 1 << 5;

  static final int RHS_FLAG_NULLABLE = 1;
  static final int RHS_FLAG_PREFIX_NULLABLE = 1 << 1;
  static final int RHS_FLAG_SUFFIX_NULLABLE = 1 << 2;
  static final int RHS_FLAG_OPERATOR = 1 << 3;

  private MIndexASTSpecImage() {}

  public static void compile(MIndexASTSpecPrecompute spec, Path output) throws IOException {
    write(new HeapMIndexASTSpecImage(Objects.requireNonNull(spec, "spec")), output);
  }

  public static void write(MIndexASTSpecImageView image, Path output) throws IOException {
    Objects.requireNonNull(image, "image");
    Objects.requireNonNull(output, "output");
    if (image instanceof MappedMIndexASTSpecImage mapped
        && mapped.imageFormat() == LEGACY_FORMAT) {
      throw new IllegalArgumentException(
          "legacy v1 AST spec images must be rebound through the canonical MIndexLang before v2 serialization");
    }

    MIndexASTProductionDispatch dispatch = MIndexASTProductionDispatch.build(image);
    Counts counts = counts(image, dispatch);
    byte[] identity = image.identity().getBytes(StandardCharsets.UTF_8);
    byte[] language = image.language().getBytes(StandardCharsets.UTF_8);
    byte[] version = image.version().getBytes(StandardCharsets.UTF_8);
    int arraysStart =
        Math.addExact(
            HEADER_BYTES,
            Math.addExact(identity.length, Math.addExact(language.length, version.length)));
    Layout layout = layout(arraysStart, counts, FORMAT);
    long expectedFileSize = Math.addExact(layout.payloadEnd(), SHA256_BYTES);
    if (expectedFileSize > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("single AST spec image exceeds 2 GiB; shard the language pack");
    }

    long contentFingerprint = contentFingerprintOf(image);
    if (image.contentFingerprint() != contentFingerprint) {
      throw new IllegalArgumentException(
          "AST spec image content fingerprint changed during serialization");
    }

    Path absolute = output.toAbsolutePath().normalize();
    Path parent = absolute.getParent();
    if (parent != null) Files.createDirectories(parent);

    MessageDigest digest = sha256();
    try (BufferedOutputStream buffered =
            new BufferedOutputStream(Files.newOutputStream(absolute));
        DigestOutputStream digestStream = new DigestOutputStream(buffered, digest);
        DataOutputStream out = new DataOutputStream(digestStream)) {
      writeHeader(
          out,
          image,
          identity.length,
          language.length,
          version.length,
          counts,
          contentFingerprint,
          arraysStart);
      out.write(identity);
      out.write(language);
      out.write(version);

      writeSymbolScalarLanes(out, image);
      writeSymbolPostingLanes(out, image);
      writeProductionScalarLanes(out, image);
      writeProductionPostingLanes(out, image);
      writePlanLanes(out, image, counts);
      writeDispatchLanes(out, dispatch, counts);

      out.flush();
      if (layout.payloadEnd() != Files.size(absolute)) {
        throw new IOException(
            "AST spec image layout mismatch before digest trailer: expected="
                + layout.payloadEnd()
                + ", actual="
                + Files.size(absolute));
      }

      byte[] checksum = digest.digest();
      digestStream.on(false);
      out.write(checksum);
      out.flush();
    }

    long actual = Files.size(absolute);
    if (actual != expectedFileSize) {
      throw new IOException(
          "AST spec image size mismatch: expected=" + expectedFileSize + ", actual=" + actual);
    }
  }

  static long contentFingerprintOf(MIndexASTSpecImageView image) {
    return contentFingerprintOf(image, true);
  }

  static long contentFingerprintOf(MIndexASTSpecImageView image, boolean includeDispatch) {
    Objects.requireNonNull(image, "image");
    long hash = 0xcbf29ce484222325L;
    hash = mixText(hash, image.identity());
    hash = mixText(hash, image.language());
    hash = mixText(hash, image.version());
    hash = mix(hash, image.languageFingerprint());
    hash = mix(hash, image.grammarFingerprint());
    hash = mix(hash, image.astSpecFingerprint());
    hash = mix(hash, image.productionPlanFingerprint());
    hash = mix(hash, image.symbolCount());
    hash = mix(hash, image.productionCount());
    hash = mix(hash, image.operatorCount());
    hash = mix(hash, image.sourceRuleCount());

    for (int symbol = 0; symbol < image.symbolCount(); symbol++) {
      hash = mix(hash, image.symbolKind(symbol).ordinal());
      hash = mix(hash, image.tokenClass(symbol).map(Enum::ordinal).orElse(-1));
      hash = mix(hash, image.literalCase(symbol).ordinal());
      hash = mix(hash, symbolFlags(image, symbol));
      hash = mix(hash, image.minimumTokens(symbol));
      hash = mix(hash, image.symbolHash64(symbol));
      hash = mixArray(hash, image.firstSymbols(symbol));
      hash = mixArray(hash, image.followSymbols(symbol));
      hash = mixArray(hash, image.productionsUsingSymbol(symbol));
      hash = mix(hash, image.operatorIndexForSymbol(symbol));
      hash = mixArray(hash, image.productionsForLhs(symbol));
      hash = mixArray(hash, image.productionsPredicting(symbol));
      hash = mixArray(hash, image.derivableTerminals(symbol));
      hash = mixArray(hash, image.derivableOperators(symbol));
    }

    for (int production = 0; production < image.productionCount(); production++) {
      hash = mix(hash, image.productionLhs(production));
      int rhs = image.productionRhsLength(production);
      hash = mix(hash, rhs);
      for (int ordinal = 0; ordinal < rhs; ordinal++) {
        hash = mix(hash, image.productionRhsSymbol(production, ordinal));
        hash = mix(hash, image.childRole(production, ordinal).ordinal());
        hash = mix(hash, rhsFlags(image, production, ordinal));
        hash = mix(hash, image.operatorIndex(production, ordinal));
        hash = mix(hash, image.minimumTokensBefore(production, ordinal));
        hash = mix(hash, image.minimumTokensAfter(production, ordinal));
      }
      hash = mix(hash, image.productionMinimumTokens(production));
      hash = mix(hash, image.productionCanEndAtEof(production) ? 1 : 0);
      hash = mix(hash, image.productionHash64(production));
      hash = mixArray(hash, image.predictionSymbols(production));
      hash = mix(hash, image.terminalChildCount(production));
      hash = mix(hash, image.nonTerminalChildCount(production));
      hash = mix(hash, image.directOperatorCount(production));
      hash = mixArray(hash, image.directOperatorOrdinals(production));
      hash = mix(hash, image.shapeHash64(production));
      hash = mix(hash, image.planHash64(production));
      hash = mixArray(hash, image.productionDerivableTerminals(production));
      hash = mixArray(hash, image.productionDerivableOperators(production));
    }

    for (int rule = 0; rule < image.sourceRuleCount(); rule++) {
      hash = mix(hash, image.sourceRuleOperatorIndex(rule));
    }
    if (includeDispatch) {
      hash = mix(hash, image.productionDispatchFingerprint());
      hash = mix(hash, image.productionDispatchEntryCount());
      hash = mix(hash, image.productionDispatchUniqueEntryCount());
      hash = mix(hash, image.productionDispatchAmbiguousEntryCount());
    }
    return avalanche(hash);
  }

  static Counts counts(MIndexASTSpecImageView image, MIndexASTProductionDispatch dispatch) {
    int rhsTotal = 0;
    int firstTotal = 0;
    int followTotal = 0;
    int predictionTotal = 0;
    int useTotal = 0;
    int directOperatorTotal = 0;
    int terminalCount = 0;

    for (int symbol = 0; symbol < image.symbolCount(); symbol++) {
      firstTotal = Math.addExact(firstTotal, image.firstSymbols(symbol).length);
      followTotal = Math.addExact(followTotal, image.followSymbols(symbol).length);
      useTotal = Math.addExact(useTotal, image.productionsUsingSymbol(symbol).length);
      if (image.symbolKind(symbol) != MIndexGrammarSymbolKind.NON_TERMINAL) terminalCount++;
    }

    for (int production = 0; production < image.productionCount(); production++) {
      rhsTotal = Math.addExact(rhsTotal, image.productionRhsLength(production));
      predictionTotal =
          Math.addExact(predictionTotal, image.predictionSymbols(production).length);
      directOperatorTotal =
          Math.addExact(directOperatorTotal, image.directOperatorCount(production));
    }

    int terminalWords = Math.max(1, (terminalCount + 63) >>> 6);
    int operatorWords = Math.max(1, (image.operatorCount() + 63) >>> 6);
    if (dispatch.candidateCount() != predictionTotal) {
      throw new IllegalStateException(
          "dispatch candidate count differs from production prediction postings");
    }
    return new Counts(
        image.symbolCount(),
        image.productionCount(),
        image.operatorCount(),
        image.sourceRuleCount(),
        rhsTotal,
        firstTotal,
        followTotal,
        predictionTotal,
        useTotal,
        directOperatorTotal,
        terminalCount,
        terminalWords,
        operatorWords,
        dispatch.entryCount());
  }

  static Layout layout(int arraysStart, Counts counts, int format) {
    return new Layout(arraysStart, counts, format);
  }

  private static void writeHeader(
      DataOutputStream out,
      MIndexASTSpecImageView image,
      int identityLength,
      int languageLength,
      int versionLength,
      Counts counts,
      long contentFingerprint,
      int arraysStart)
      throws IOException {
    out.writeLong(MAGIC);
    out.writeInt(FORMAT);
    out.writeInt(HEADER_BYTES);
    out.writeInt(identityLength);
    out.writeInt(languageLength);
    out.writeInt(versionLength);
    out.writeInt(image.symbolCount());
    out.writeInt(image.productionCount());
    out.writeInt(image.operatorCount());
    out.writeInt(image.sourceRuleCount());
    out.writeInt(counts.rhsTotal());
    out.writeInt(counts.firstTotal());
    out.writeInt(counts.followTotal());
    out.writeInt(counts.predictionTotal());
    out.writeInt(counts.useTotal());
    out.writeInt(counts.directOperatorTotal());
    out.writeInt(counts.terminalCount());
    out.writeInt(counts.terminalWords());
    out.writeInt(counts.operatorWords());
    out.writeLong(image.languageFingerprint());
    out.writeLong(image.grammarFingerprint());
    out.writeLong(image.astSpecFingerprint());
    out.writeLong(image.productionPlanFingerprint());
    out.writeLong(contentFingerprint);
    out.writeInt(arraysStart);
    out.writeInt(counts.dispatchEntryCount());
  }

  private static void writeSymbolScalarLanes(DataOutputStream out, MIndexASTSpecImageView image)
      throws IOException {
    for (int symbol = 0; symbol < image.symbolCount(); symbol++) {
      out.writeByte(image.symbolKind(symbol).ordinal());
    }
    for (int symbol = 0; symbol < image.symbolCount(); symbol++) {
      out.writeByte(image.tokenClass(symbol).map(Enum::ordinal).orElse(-1));
    }
    for (int symbol = 0; symbol < image.symbolCount(); symbol++) {
      out.writeByte(image.literalCase(symbol).ordinal());
    }
    for (int symbol = 0; symbol < image.symbolCount(); symbol++) {
      out.writeByte(symbolFlags(image, symbol));
    }
    for (int symbol = 0; symbol < image.symbolCount(); symbol++) {
      out.writeInt(image.minimumTokens(symbol));
    }
    for (int symbol = 0; symbol < image.symbolCount(); symbol++) {
      out.writeLong(image.symbolHash64(symbol));
    }
  }

  private static void writeSymbolPostingLanes(DataOutputStream out, MIndexASTSpecImageView image)
      throws IOException {
    writeOffsets(out, image.symbolCount(), image::firstSymbols);
    writeValues(out, image.symbolCount(), image::firstSymbols);
    writeOffsets(out, image.symbolCount(), image::followSymbols);
    writeValues(out, image.symbolCount(), image::followSymbols);
    writeOffsets(out, image.symbolCount(), image::productionsUsingSymbol);
    writeValues(out, image.symbolCount(), image::productionsUsingSymbol);
    for (int symbol = 0; symbol < image.symbolCount(); symbol++) {
      out.writeInt(image.operatorIndexForSymbol(symbol));
    }
    for (int rule = 0; rule < image.sourceRuleCount(); rule++) {
      out.writeInt(image.sourceRuleOperatorIndex(rule));
    }
  }

  private static void writeProductionScalarLanes(
      DataOutputStream out, MIndexASTSpecImageView image) throws IOException {
    for (int production = 0; production < image.productionCount(); production++) {
      out.writeInt(image.productionLhs(production));
    }

    int rhsOffset = 0;
    out.writeInt(0);
    for (int production = 0; production < image.productionCount(); production++) {
      rhsOffset = Math.addExact(rhsOffset, image.productionRhsLength(production));
      out.writeInt(rhsOffset);
    }
    for (int production = 0; production < image.productionCount(); production++) {
      for (int ordinal = 0; ordinal < image.productionRhsLength(production); ordinal++) {
        out.writeInt(image.productionRhsSymbol(production, ordinal));
      }
    }
    for (int production = 0; production < image.productionCount(); production++) {
      out.writeInt(image.productionMinimumTokens(production));
    }
    for (int production = 0; production < image.productionCount(); production++) {
      out.writeByte(image.productionCanEndAtEof(production) ? 1 : 0);
    }
    for (int production = 0; production < image.productionCount(); production++) {
      out.writeLong(image.productionHash64(production));
    }
  }

  private static void writeProductionPostingLanes(
      DataOutputStream out, MIndexASTSpecImageView image) throws IOException {
    writeOffsets(out, image.productionCount(), image::predictionSymbols);
    writeValues(out, image.productionCount(), image::predictionSymbols);
  }

  private static void writePlanLanes(
      DataOutputStream out, MIndexASTSpecImageView image, Counts counts) throws IOException {
    for (int production = 0; production < image.productionCount(); production++) {
      for (int ordinal = 0; ordinal < image.productionRhsLength(production); ordinal++) {
        out.writeByte(image.childRole(production, ordinal).ordinal());
      }
    }
    for (int production = 0; production < image.productionCount(); production++) {
      for (int ordinal = 0; ordinal < image.productionRhsLength(production); ordinal++) {
        out.writeByte(rhsFlags(image, production, ordinal));
      }
    }
    for (int production = 0; production < image.productionCount(); production++) {
      for (int ordinal = 0; ordinal < image.productionRhsLength(production); ordinal++) {
        out.writeInt(image.operatorIndex(production, ordinal));
      }
    }
    for (int production = 0; production < image.productionCount(); production++) {
      for (int ordinal = 0; ordinal < image.productionRhsLength(production); ordinal++) {
        out.writeInt(image.minimumTokensBefore(production, ordinal));
      }
    }
    for (int production = 0; production < image.productionCount(); production++) {
      for (int ordinal = 0; ordinal < image.productionRhsLength(production); ordinal++) {
        out.writeInt(image.minimumTokensAfter(production, ordinal));
      }
    }
    for (int production = 0; production < image.productionCount(); production++) {
      out.writeInt(image.terminalChildCount(production));
    }
    for (int production = 0; production < image.productionCount(); production++) {
      out.writeInt(image.nonTerminalChildCount(production));
    }
    for (int production = 0; production < image.productionCount(); production++) {
      out.writeInt(image.directOperatorCount(production));
    }
    for (int production = 0; production < image.productionCount(); production++) {
      out.writeLong(image.shapeHash64(production));
    }
    for (int production = 0; production < image.productionCount(); production++) {
      out.writeLong(image.planHash64(production));
    }

    writeOffsets(out, image.productionCount(), image::directOperatorOrdinals);
    writeValues(out, image.productionCount(), image::directOperatorOrdinals);
    writeOffsets(out, image.symbolCount(), image::productionsForLhs);
    writeValues(out, image.symbolCount(), image::productionsForLhs);
    writeOffsets(out, image.symbolCount(), image::productionsPredicting);
    writeValues(out, image.symbolCount(), image::productionsPredicting);

    int[] terminalOrdinalBySymbol = new int[image.symbolCount()];
    java.util.Arrays.fill(terminalOrdinalBySymbol, -1);
    int[] terminalSymbolByOrdinal = new int[counts.terminalCount()];
    int terminal = 0;
    for (int symbol = 0; symbol < image.symbolCount(); symbol++) {
      if (image.symbolKind(symbol) != MIndexGrammarSymbolKind.NON_TERMINAL) {
        terminalOrdinalBySymbol[symbol] = terminal;
        terminalSymbolByOrdinal[terminal++] = symbol;
      }
    }
    for (int value : terminalOrdinalBySymbol) out.writeInt(value);
    for (int value : terminalSymbolByOrdinal) out.writeInt(value);

    writeTerminalMatrix(out, image, counts, false);
    writeTerminalMatrix(out, image, counts, true);
    writeOperatorMatrix(out, image, counts, false);
    writeOperatorMatrix(out, image, counts, true);
  }

  private static void writeTerminalMatrix(
      DataOutputStream out, MIndexASTSpecImageView image, Counts counts, boolean production)
      throws IOException {
    int rows = production ? image.productionCount() : image.symbolCount();
    for (int row = 0; row < rows; row++) {
      long[] words = new long[counts.terminalWords()];
      int[] terminals =
          production
              ? image.productionDerivableTerminals(row)
              : image.derivableTerminals(row);
      for (int symbol : terminals) {
        int ordinal = terminalOrdinal(image, symbol);
        if (ordinal < 0) {
          throw new IllegalArgumentException("derivation matrix returned non-terminal symbol " + symbol);
        }
        words[ordinal >>> 6] |= 1L << ordinal;
      }
      for (long word : words) out.writeLong(word);
    }
  }

  private static void writeOperatorMatrix(
      DataOutputStream out, MIndexASTSpecImageView image, Counts counts, boolean production)
      throws IOException {
    int rows = production ? image.productionCount() : image.symbolCount();
    for (int row = 0; row < rows; row++) {
      long[] words = new long[counts.operatorWords()];
      int[] operators =
          production ? image.productionDerivableOperators(row) : image.derivableOperators(row);
      for (int operator : operators) {
        Objects.checkIndex(operator, image.operatorCount());
        words[operator >>> 6] |= 1L << operator;
      }
      for (long word : words) out.writeLong(word);
    }
  }

  private static void writeDispatchLanes(
      DataOutputStream out, MIndexASTProductionDispatch dispatch, Counts counts)
      throws IOException {
    if (dispatch.entryCount() != counts.dispatchEntryCount()
        || dispatch.candidateCount() != counts.predictionTotal()
        || dispatch.hashSlotCount()
            != MIndexASTProductionDispatch.hashCapacityForEntries(dispatch.entryCount())) {
      throw new IllegalStateException("dispatch geometry changed during image serialization");
    }

    for (int entry = 0; entry < dispatch.entryCount(); entry++) {
      out.writeLong(dispatch.keyAt(entry));
    }

    out.writeInt(0);
    for (int entry = 0; entry < dispatch.entryCount(); entry++) {
      out.writeInt(dispatch.entryCandidateEnd(entry));
    }

    for (int index = 0; index < dispatch.candidateCount(); index++) {
      out.writeInt(dispatch.candidateAtAbsolute(index));
    }

    for (int symbol = 0; symbol <= counts.symbolCount(); symbol++) {
      out.writeInt(dispatch.entryOffset(symbol));
    }

    for (int slot = 0; slot < dispatch.hashSlotCount(); slot++) {
      out.writeLong(dispatch.hashSlotKey(slot));
    }
    for (int slot = 0; slot < dispatch.hashSlotCount(); slot++) {
      out.writeInt(dispatch.hashSlotEntry(slot));
    }

    for (int symbol = 0; symbol <= counts.symbolCount(); symbol++) {
      out.writeInt(dispatch.eofOffset(symbol));
    }

    int eofCount = dispatch.eofCandidateCount();
    for (int index = 0; index < eofCount; index++) {
      out.writeInt(dispatch.eofCandidateAtAbsolute(index));
    }
    for (int index = eofCount; index < counts.productionCount(); index++) {
      out.writeInt(-1);
    }
  }

  private static int terminalOrdinal(MIndexASTSpecImageView image, int terminalSymbol) {
    int ordinal = 0;
    for (int symbol = 0; symbol < image.symbolCount(); symbol++) {
      if (image.symbolKind(symbol) == MIndexGrammarSymbolKind.NON_TERMINAL) continue;
      if (symbol == terminalSymbol) return ordinal;
      ordinal++;
    }
    return -1;
  }

  private static int symbolFlags(MIndexASTSpecImageView image, int symbol) {
    int flags = 0;
    if (image.nullable(symbol)) flags |= SYMBOL_FLAG_NULLABLE;
    if (image.productive(symbol)) flags |= SYMBOL_FLAG_PRODUCTIVE;
    if (image.reachable(symbol)) flags |= SYMBOL_FLAG_REACHABLE;
    if (image.leftRecursive(symbol)) flags |= SYMBOL_FLAG_LEFT_RECURSIVE;
    if (image.canEndAfter(symbol)) flags |= SYMBOL_FLAG_CAN_END;
    if (image.operatorTerminal(symbol)) flags |= SYMBOL_FLAG_OPERATOR;
    return flags;
  }

  private static int rhsFlags(MIndexASTSpecImageView image, int production, int ordinal) {
    int flags = 0;
    if (image.childNullable(production, ordinal)) flags |= RHS_FLAG_NULLABLE;
    if (image.nullablePrefixBefore(production, ordinal)) flags |= RHS_FLAG_PREFIX_NULLABLE;
    if (image.nullableSuffixAfter(production, ordinal)) flags |= RHS_FLAG_SUFFIX_NULLABLE;
    if (image.directOperator(production, ordinal)) flags |= RHS_FLAG_OPERATOR;
    return flags;
  }

  private static void writeOffsets(DataOutputStream out, int rows, IntArrayLookup lookup)
      throws IOException {
    int offset = 0;
    out.writeInt(0);
    for (int row = 0; row < rows; row++) {
      offset = Math.addExact(offset, lookup.values(row).length);
      out.writeInt(offset);
    }
  }

  private static void writeValues(DataOutputStream out, int rows, IntArrayLookup lookup)
      throws IOException {
    for (int row = 0; row < rows; row++) {
      for (int value : lookup.values(row)) out.writeInt(value);
    }
  }

  private static long mixArray(long hash, int[] values) {
    long result = mix(hash, values.length);
    for (int value : values) result = mix(result, value);
    return result;
  }

  private static long mixText(long hash, CharSequence value) {
    long result = mix(hash, value.length());
    for (int index = 0; index < value.length(); index++) {
      result = mix(result, value.charAt(index));
    }
    return result;
  }

  private static long mix(long left, long right) {
    return avalanche(left ^ Long.rotateLeft(right + 0x9e3779b97f4a7c15L, 23));
  }

  private static long avalanche(long value) {
    long z = value;
    z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
    z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
    return z ^ (z >>> 31);
  }

  private static MessageDigest sha256() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException("SHA-256 unavailable", impossible);
    }
  }

  @FunctionalInterface
  private interface IntArrayLookup {
    int[] values(int row);
  }

  record Counts(
      int symbolCount,
      int productionCount,
      int operatorCount,
      int sourceRuleCount,
      int rhsTotal,
      int firstTotal,
      int followTotal,
      int predictionTotal,
      int useTotal,
      int directOperatorTotal,
      int terminalCount,
      int terminalWords,
      int operatorWords,
      int dispatchEntryCount) {}

  /**
   * Absolute byte layout. Format 2 appends predictive-dispatch sections after the format-1 planes.
   */
  static final class Layout {
    final int arraysStart;
    final Counts counts;
    final int format;
    int cursor;

    final int symbolKinds;
    final int tokenClasses;
    final int literalCases;
    final int symbolFlags;
    final int minimumTokens;
    final int symbolHashes;
    final int firstOffsets;
    final int firstSymbols;
    final int followOffsets;
    final int followSymbols;
    final int useOffsets;
    final int productionsUsing;
    final int operatorIndexBySymbol;
    final int sourceRuleOperatorIndexes;

    final int productionLhs;
    final int rhsStarts;
    final int rhsSymbols;
    final int productionMinimumTokens;
    final int productionCanEndAtEof;
    final int productionHashes;
    final int predictionOffsets;
    final int predictionSymbols;

    final int rhsRoles;
    final int rhsFlags;
    final int rhsOperatorIndexes;
    final int minimumTokensBefore;
    final int minimumTokensAfter;
    final int terminalCounts;
    final int nonTerminalCounts;
    final int operatorCounts;
    final int shapeHashes;
    final int planHashes;
    final int operatorOffsets;
    final int operatorOrdinals;
    final int lhsOffsets;
    final int productionsByLhs;
    final int predictionUseOffsets;
    final int productionsPredicting;
    final int terminalOrdinalBySymbol;
    final int terminalSymbolByOrdinal;
    final int derivableTerminalBits;
    final int productionTerminalBits;
    final int derivableOperatorBits;
    final int productionOperatorBits;

    final int dispatchKeys;
    final int dispatchOffsets;
    final int dispatchCandidates;
    final int dispatchRowOffsets;
    final int dispatchSlotKeys;
    final int dispatchSlotEntries;
    final int eofOffsets;
    final int eofCandidates;

    Layout(int arraysStart, Counts counts, int format) {
      this.arraysStart = arraysStart;
      this.counts = counts;
      this.format = format;
      this.cursor = arraysStart;

      symbolKinds = takeBytes(symbols());
      tokenClasses = takeBytes(symbols());
      literalCases = takeBytes(symbols());
      symbolFlags = takeBytes(symbols());
      minimumTokens = takeInts(symbols());
      symbolHashes = takeLongs(symbols());

      firstOffsets = takeInts(symbols() + 1);
      firstSymbols = takeInts(counts.firstTotal());
      followOffsets = takeInts(symbols() + 1);
      followSymbols = takeInts(counts.followTotal());
      useOffsets = takeInts(symbols() + 1);
      productionsUsing = takeInts(counts.useTotal());
      operatorIndexBySymbol = takeInts(symbols());
      sourceRuleOperatorIndexes = takeInts(sourceRules());

      productionLhs = takeInts(productions());
      rhsStarts = takeInts(productions() + 1);
      rhsSymbols = takeInts(counts.rhsTotal());
      productionMinimumTokens = takeInts(productions());
      productionCanEndAtEof = takeBytes(productions());
      productionHashes = takeLongs(productions());
      predictionOffsets = takeInts(productions() + 1);
      predictionSymbols = takeInts(counts.predictionTotal());

      rhsRoles = takeBytes(counts.rhsTotal());
      rhsFlags = takeBytes(counts.rhsTotal());
      rhsOperatorIndexes = takeInts(counts.rhsTotal());
      minimumTokensBefore = takeInts(counts.rhsTotal());
      minimumTokensAfter = takeInts(counts.rhsTotal());
      terminalCounts = takeInts(productions());
      nonTerminalCounts = takeInts(productions());
      operatorCounts = takeInts(productions());
      shapeHashes = takeLongs(productions());
      planHashes = takeLongs(productions());
      operatorOffsets = takeInts(productions() + 1);
      operatorOrdinals = takeInts(counts.directOperatorTotal());
      lhsOffsets = takeInts(symbols() + 1);
      productionsByLhs = takeInts(productions());
      predictionUseOffsets = takeInts(symbols() + 1);
      productionsPredicting = takeInts(counts.predictionTotal());
      terminalOrdinalBySymbol = takeInts(symbols());
      terminalSymbolByOrdinal = takeInts(counts.terminalCount());
      derivableTerminalBits =
          takeLongs(Math.multiplyExact(symbols(), counts.terminalWords()));
      productionTerminalBits =
          takeLongs(Math.multiplyExact(productions(), counts.terminalWords()));
      derivableOperatorBits =
          takeLongs(Math.multiplyExact(symbols(), counts.operatorWords()));
      productionOperatorBits =
          takeLongs(Math.multiplyExact(productions(), counts.operatorWords()));

      if (format >= FORMAT) {
        int slots =
            MIndexASTProductionDispatch.hashCapacityForEntries(counts.dispatchEntryCount());
        dispatchKeys = takeLongs(counts.dispatchEntryCount());
        dispatchOffsets = takeInts(counts.dispatchEntryCount() + 1);
        dispatchCandidates = takeInts(counts.predictionTotal());
        dispatchRowOffsets = takeInts(symbols() + 1);
        dispatchSlotKeys = takeLongs(slots);
        dispatchSlotEntries = takeInts(slots);
        eofOffsets = takeInts(symbols() + 1);
        eofCandidates = takeInts(productions());
      } else {
        dispatchKeys = -1;
        dispatchOffsets = -1;
        dispatchCandidates = -1;
        dispatchRowOffsets = -1;
        dispatchSlotKeys = -1;
        dispatchSlotEntries = -1;
        eofOffsets = -1;
        eofCandidates = -1;
      }
    }

    int payloadEnd() {
      return cursor;
    }

    private int symbols() {
      return counts.symbolCount();
    }

    private int productions() {
      return counts.productionCount();
    }

    private int sourceRules() {
      return counts.sourceRuleCount();
    }

    private int takeBytes(int count) {
      int start = cursor;
      cursor = Math.addExact(cursor, count);
      return start;
    }

    private int takeInts(int count) {
      int start = cursor;
      cursor = Math.addExact(cursor, Math.multiplyExact(count, Integer.BYTES));
      return start;
    }

    private int takeLongs(int count) {
      int start = cursor;
      cursor = Math.addExact(cursor, Math.multiplyExact(count, Long.BYTES));
      return start;
    }
  }
}
