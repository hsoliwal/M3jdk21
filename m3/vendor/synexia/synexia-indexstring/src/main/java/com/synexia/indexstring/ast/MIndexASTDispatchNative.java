// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.ast;

import com.synexia.indexstring.nativebridge.IndexStringNative;
import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Optional JNI acceleration for immutable AST production-dispatch batches.
 *
 * <p>The Java scalar dispatch remains the semantic oracle. JNI is used only for sufficiently large
 * batches and never retains primitive-array or direct-buffer addresses after the call.</p>
 */
final class MIndexASTDispatchNative {
  private static final int ABI_VERSION = 1;
  private static final int NATIVE_BATCH_THRESHOLD =
      Math.max(
          1,
          Integer.getInteger(
              "synexia.indexstring.ast.dispatch.native.threshold", 256));
  private static volatile Boolean nativeBatchAvailable;

  private MIndexASTDispatchNative() {}

  static void batchHeap(
      MIndexASTProductionDispatch dispatch,
      int[] nonTerminals,
      int[] terminals,
      int[] candidateCounts,
      int[] uniqueProductions,
      byte[] flags) {
    Objects.requireNonNull(dispatch, "dispatch");
    requireBatchLengths(
        nonTerminals, terminals, candidateCounts, uniqueProductions, flags);
    validateSymbols(nonTerminals, dispatch.symbolCountForNative(), "nonTerminal");
    validateSymbols(terminals, dispatch.symbolCountForNative(), "terminal");

    if (nonTerminals.length >= NATIVE_BATCH_THRESHOLD && nativeBatchAvailable()) {
      try {
        int status =
            nativeHeapDispatch(
                dispatch.slotKeysForNative(),
                dispatch.slotEntriesForNative(),
                dispatch.offsetsForNative(),
                dispatch.candidatesForNative(),
                dispatch.symbolCountForNative(),
                nonTerminals,
                terminals,
                candidateCounts,
                uniqueProductions,
                flags);
        if (status == 0) return;
        if (status != -1) {
          throw new IllegalStateException("native AST heap dispatch rejected validated geometry");
        }
      } catch (UnsatisfiedLinkError incompatibleLibrary) {
        nativeBatchAvailable = Boolean.FALSE;
      }
    }

    javaHeapDispatch(
        dispatch, nonTerminals, terminals, candidateCounts, uniqueProductions, flags);
  }

  static void batchHeapEof(
      MIndexASTProductionDispatch dispatch,
      int[] nonTerminals,
      int[] candidateCounts,
      int[] uniqueProductions,
      byte[] flags) {
    Objects.requireNonNull(dispatch, "dispatch");
    requireEofBatchLengths(nonTerminals, candidateCounts, uniqueProductions, flags);
    validateSymbols(nonTerminals, dispatch.symbolCountForNative(), "nonTerminal");

    if (nonTerminals.length >= NATIVE_BATCH_THRESHOLD && nativeBatchAvailable()) {
      try {
        int status =
            nativeHeapEofDispatch(
                dispatch.eofOffsetsForNative(),
                dispatch.eofCandidatesForNative(),
                dispatch.symbolCountForNative(),
                nonTerminals,
                candidateCounts,
                uniqueProductions,
                flags);
        if (status == 0) return;
        if (status != -1) {
          throw new IllegalStateException("native AST heap EOF dispatch rejected validated geometry");
        }
      } catch (UnsatisfiedLinkError incompatibleLibrary) {
        nativeBatchAvailable = Boolean.FALSE;
      }
    }

    javaHeapEof(
        dispatch, nonTerminals, candidateCounts, uniqueProductions, flags);
  }

  static void batchMapped(
      MIndexASTSpecImageView fallback,
      ByteBuffer image,
      int symbolCount,
      int slotKeysBase,
      int slotEntriesBase,
      int offsetsBase,
      int candidatesBase,
      int slotCount,
      int entryCount,
      int candidateCount,
      int[] nonTerminals,
      int[] terminals,
      int[] candidateCounts,
      int[] uniqueProductions,
      byte[] flags) {
    Objects.requireNonNull(fallback, "fallback");
    ByteBuffer checkedImage = Objects.requireNonNull(image, "image");
    requireBatchLengths(
        nonTerminals, terminals, candidateCounts, uniqueProductions, flags);
    validateSymbols(nonTerminals, symbolCount, "nonTerminal");
    validateSymbols(terminals, symbolCount, "terminal");
    requireMappedDispatchLayout(
        checkedImage.capacity(),
        slotKeysBase,
        slotEntriesBase,
        offsetsBase,
        candidatesBase,
        slotCount,
        entryCount,
        candidateCount);

    if (nonTerminals.length >= NATIVE_BATCH_THRESHOLD
        && checkedImage.isDirect()
        && nativeBatchAvailable()) {
      try {
        int status =
            nativeMappedDispatch(
                checkedImage,
                symbolCount,
                slotKeysBase,
                slotEntriesBase,
                offsetsBase,
                candidatesBase,
                slotCount,
                entryCount,
                candidateCount,
                nonTerminals,
                terminals,
                candidateCounts,
                uniqueProductions,
                flags);
        if (status == 0) return;
        if (status != -1) {
          throw new IllegalStateException("native AST mapped dispatch rejected validated geometry");
        }
      } catch (UnsatisfiedLinkError incompatibleLibrary) {
        nativeBatchAvailable = Boolean.FALSE;
      }
    }

    javaViewDispatch(
        fallback, nonTerminals, terminals, candidateCounts, uniqueProductions, flags);
  }

  static void batchMappedEof(
      MIndexASTSpecImageView fallback,
      ByteBuffer image,
      int symbolCount,
      int eofOffsetsBase,
      int eofCandidatesBase,
      int eofCandidateCapacity,
      int[] nonTerminals,
      int[] candidateCounts,
      int[] uniqueProductions,
      byte[] flags) {
    Objects.requireNonNull(fallback, "fallback");
    ByteBuffer checkedImage = Objects.requireNonNull(image, "image");
    requireEofBatchLengths(nonTerminals, candidateCounts, uniqueProductions, flags);
    validateSymbols(nonTerminals, symbolCount, "nonTerminal");
    requireMappedEofLayout(
        checkedImage.capacity(),
        symbolCount,
        eofOffsetsBase,
        eofCandidatesBase,
        eofCandidateCapacity);

    if (nonTerminals.length >= NATIVE_BATCH_THRESHOLD
        && checkedImage.isDirect()
        && nativeBatchAvailable()) {
      try {
        int status =
            nativeMappedEofDispatch(
                checkedImage,
                symbolCount,
                eofOffsetsBase,
                eofCandidatesBase,
                eofCandidateCapacity,
                nonTerminals,
                candidateCounts,
                uniqueProductions,
                flags);
        if (status == 0) return;
        if (status != -1) {
          throw new IllegalStateException("native AST mapped EOF dispatch rejected validated geometry");
        }
      } catch (UnsatisfiedLinkError incompatibleLibrary) {
        nativeBatchAvailable = Boolean.FALSE;
      }
    }

    javaViewEof(
        fallback, nonTerminals, candidateCounts, uniqueProductions, flags);
  }

  static boolean nativeBatchAvailable() {
    Boolean current = nativeBatchAvailable;
    if (current != null) return current;

    synchronized (MIndexASTDispatchNative.class) {
      current = nativeBatchAvailable;
      if (current != null) return current;
      if (!IndexStringNative.available()) {
        nativeBatchAvailable = Boolean.FALSE;
        return false;
      }
      try {
        current = nativeAbiVersion() == ABI_VERSION ? Boolean.TRUE : Boolean.FALSE;
      } catch (UnsatisfiedLinkError incompatibleLibrary) {
        current = Boolean.FALSE;
      }
      nativeBatchAvailable = current;
      return current;
    }
  }

  static void javaHeapDispatch(
      MIndexASTProductionDispatch dispatch,
      int[] nonTerminals,
      int[] terminals,
      int[] candidateCounts,
      int[] uniqueProductions,
      byte[] flags) {
    for (int index = 0; index < nonTerminals.length; index++) {
      int count = dispatch.candidateCount(nonTerminals[index], terminals[index]);
      writeResult(
          count,
          count == 1
              ? dispatch.uniqueProductionOrMinusOne(
                  nonTerminals[index], terminals[index])
              : -1,
          index,
          candidateCounts,
          uniqueProductions,
          flags);
    }
  }

  static void javaHeapEof(
      MIndexASTProductionDispatch dispatch,
      int[] nonTerminals,
      int[] candidateCounts,
      int[] uniqueProductions,
      byte[] flags) {
    for (int index = 0; index < nonTerminals.length; index++) {
      int[] candidates = dispatch.eofCandidateProductions(nonTerminals[index]);
      writeResult(
          candidates.length,
          candidates.length == 1 ? candidates[0] : -1,
          index,
          candidateCounts,
          uniqueProductions,
          flags);
    }
  }

  static void javaViewDispatch(
      MIndexASTSpecImageView view,
      int[] nonTerminals,
      int[] terminals,
      int[] candidateCounts,
      int[] uniqueProductions,
      byte[] flags) {
    for (int index = 0; index < nonTerminals.length; index++) {
      int count = view.dispatchCandidateCount(nonTerminals[index], terminals[index]);
      writeResult(
          count,
          count == 1
              ? view.uniqueProductionOrMinusOne(
                  nonTerminals[index], terminals[index])
              : -1,
          index,
          candidateCounts,
          uniqueProductions,
          flags);
    }
  }

  static void javaViewEof(
      MIndexASTSpecImageView view,
      int[] nonTerminals,
      int[] candidateCounts,
      int[] uniqueProductions,
      byte[] flags) {
    for (int index = 0; index < nonTerminals.length; index++) {
      int[] candidates = view.eofCandidateProductions(nonTerminals[index]);
      writeResult(
          candidates.length,
          candidates.length == 1 ? candidates[0] : -1,
          index,
          candidateCounts,
          uniqueProductions,
          flags);
    }
  }

  private static void writeResult(
      int count,
      int unique,
      int index,
      int[] candidateCounts,
      int[] uniqueProductions,
      byte[] flags) {
    candidateCounts[index] = count;
    uniqueProductions[index] = count == 1 ? unique : -1;
    flags[index] =
        count == 0
            ? 0
            : (byte)
                (MIndexASTSpecImageView.DISPATCH_PRESENT
                    | (count > 1 ? MIndexASTSpecImageView.DISPATCH_AMBIGUOUS : 0));
  }

  private static void validateSymbols(int[] symbols, int symbolCount, String lane) {
    Objects.requireNonNull(symbols, lane + "s");
    for (int index = 0; index < symbols.length; index++) {
      int symbol = symbols[index];
      if (symbol < 0 || symbol >= symbolCount) {
        throw new IndexOutOfBoundsException(
            lane + " at batch index " + index + ": " + symbol);
      }
    }
  }

  private static void requireBatchLengths(
      int[] nonTerminals,
      int[] terminals,
      int[] candidateCounts,
      int[] uniqueProductions,
      byte[] flags) {
    Objects.requireNonNull(nonTerminals, "nonTerminals");
    Objects.requireNonNull(terminals, "terminals");
    Objects.requireNonNull(candidateCounts, "candidateCounts");
    Objects.requireNonNull(uniqueProductions, "uniqueProductions");
    Objects.requireNonNull(flags, "flags");
    int length = nonTerminals.length;
    if (terminals.length != length
        || candidateCounts.length != length
        || uniqueProductions.length != length
        || flags.length != length) {
      throw new IllegalArgumentException("AST dispatch batch lane lengths differ");
    }
  }

  private static void requireEofBatchLengths(
      int[] nonTerminals,
      int[] candidateCounts,
      int[] uniqueProductions,
      byte[] flags) {
    Objects.requireNonNull(nonTerminals, "nonTerminals");
    Objects.requireNonNull(candidateCounts, "candidateCounts");
    Objects.requireNonNull(uniqueProductions, "uniqueProductions");
    Objects.requireNonNull(flags, "flags");
    int length = nonTerminals.length;
    if (candidateCounts.length != length
        || uniqueProductions.length != length
        || flags.length != length) {
      throw new IllegalArgumentException("AST EOF dispatch batch lane lengths differ");
    }
  }

  private static void requireMappedDispatchLayout(
      int capacity,
      int slotKeysBase,
      int slotEntriesBase,
      int offsetsBase,
      int candidatesBase,
      int slotCount,
      int entryCount,
      int candidateCount) {
    if (slotKeysBase < 0
        || slotEntriesBase < 0
        || offsetsBase < 0
        || candidatesBase < 0
        || slotCount < 2
        || Integer.bitCount(slotCount) != 1
        || entryCount < 0
        || candidateCount < 0) {
      throw new IllegalArgumentException("invalid mapped AST dispatch layout");
    }
    checkRange(capacity, slotKeysBase, (long) slotCount * Long.BYTES);
    checkRange(capacity, slotEntriesBase, (long) slotCount * Integer.BYTES);
    checkRange(capacity, offsetsBase, (long) (entryCount + 1) * Integer.BYTES);
    checkRange(capacity, candidatesBase, (long) candidateCount * Integer.BYTES);
  }

  private static void requireMappedEofLayout(
      int capacity,
      int symbolCount,
      int eofOffsetsBase,
      int eofCandidatesBase,
      int eofCandidateCapacity) {
    if (symbolCount < 0
        || eofOffsetsBase < 0
        || eofCandidatesBase < 0
        || eofCandidateCapacity < 0) {
      throw new IllegalArgumentException("invalid mapped AST EOF layout");
    }
    checkRange(capacity, eofOffsetsBase, (long) (symbolCount + 1) * Integer.BYTES);
    checkRange(
        capacity, eofCandidatesBase, (long) eofCandidateCapacity * Integer.BYTES);
  }

  private static void checkRange(int capacity, int start, long length) {
    long end = Math.addExact((long) start, length);
    if (end > capacity) {
      throw new IllegalArgumentException("mapped AST dispatch section exceeds image");
    }
  }

  private static native int nativeAbiVersion();

  private static native int nativeHeapDispatch(
      long[] slotKeys,
      int[] slotEntries,
      int[] offsets,
      int[] candidates,
      int symbolCount,
      int[] nonTerminals,
      int[] terminals,
      int[] candidateCounts,
      int[] uniqueProductions,
      byte[] flags);

  private static native int nativeMappedDispatch(
      ByteBuffer image,
      int symbolCount,
      int slotKeysBase,
      int slotEntriesBase,
      int offsetsBase,
      int candidatesBase,
      int slotCount,
      int entryCount,
      int candidateCount,
      int[] nonTerminals,
      int[] terminals,
      int[] candidateCounts,
      int[] uniqueProductions,
      byte[] flags);

  private static native int nativeHeapEofDispatch(
      int[] eofOffsets,
      int[] eofCandidates,
      int symbolCount,
      int[] nonTerminals,
      int[] candidateCounts,
      int[] uniqueProductions,
      byte[] flags);

  private static native int nativeMappedEofDispatch(
      ByteBuffer image,
      int symbolCount,
      int eofOffsetsBase,
      int eofCandidatesBase,
      int eofCandidateCapacity,
      int[] nonTerminals,
      int[] candidateCounts,
      int[] uniqueProductions,
      byte[] flags);
}
