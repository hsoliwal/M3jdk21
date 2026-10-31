// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring;

import com.synexia.indexstring.fs.M3DirectoryContentStore;
import com.synexia.indexstring.fs.M3ObjectKind;
import com.synexia.indexstring.fs.M3ObjectRef;
import com.synexia.indexstring.fs.M3PrecomputeCache;
import com.synexia.indexstring.fs.M3PrecomputeKey;
import com.synexia.indexstring.fs.M3PrecomputeResult;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

/** Persisted complete execution tables using the existing immutable M3 content store. */
public final class MIndexRegexPrecompute {
  private MIndexRegexPrecompute() {}

  /** Revisions must bind the compiler/codec implementation and all behavior-bearing dependencies. */
  public record Request(String expression, MIndexRegexProgram.Semantics semantics, int maxStates,
                        String compilerRevision, String codecRevision) {
    public Request {
      Objects.requireNonNull(expression, "expression");
      Objects.requireNonNull(semantics, "semantics");
      if (maxStates < 1) throw new IllegalArgumentException("maxStates must be positive");
      compilerRevision = MIndexRegexPrecomputeKey.revision(compilerRevision);
      codecRevision = MIndexRegexPrecomputeKey.revision(codecRevision);
    }
  }

  /** Null is an explicit unsupported-expression result; exceptions are never cached. */
  @FunctionalInterface
  public interface Compiler {
    MIndexRegexProgram compile(Request request);
  }

  /** Decoded programs own immutable primitive arrays; the codec does not retain mapped kernels. */
  public record Result(Optional<MIndexRegexProgram> program, boolean reused,
                       Optional<M3ObjectRef> output) {
    public Result {
      Objects.requireNonNull(program, "program");
      Objects.requireNonNull(output, "output");
      if (program.isPresent() != output.isPresent()) {
        throw new IllegalArgumentException("program/output presence mismatch");
      }
    }
  }

  public static M3PrecomputeKey key(Request request) {
    return MIndexRegexPrecomputeKey.key(Objects.requireNonNull(request, "request"));
  }

  public static Result restoreOrCompile(M3DirectoryContentStore store, Request request,
                                        int maxImageBytes) throws IOException {
    return restoreOrCompile(store, request, maxImageBytes,
        value -> MIndexRegexProgram.tryCompile(value.expression(), value.semantics(),
            value.maxStates()));
  }

  /**
   * A hit validates the store hash, codec checksum, format, exact expression and semantics, then
   * calls fromPrecomputedImage directly. Corrupt or mismatched hits fail without invoking compiler.
   * The mapping budget limits encoded bytes per invocation, not total process memory.
   */
  public static Result restoreOrCompile(M3DirectoryContentStore store, Request request,
                                        int maxImageBytes, Compiler compiler) throws IOException {
    Objects.requireNonNull(store, "store");
    Objects.requireNonNull(request, "request");
    Objects.requireNonNull(compiler, "compiler");
    if (maxImageBytes < 1) throw new IllegalArgumentException("image budget must be positive");
    M3PrecomputeKey key = key(request);
    M3PrecomputeCache cache = new M3PrecomputeCache(store);
    Optional<M3PrecomputeResult> hit = cache.lookup(key);
    if (hit.isPresent()) {
      if (hit.get().isEmpty()) return new Result(Optional.empty(), true, Optional.empty());
      M3ObjectRef output = hit.get().output();
      if (output.kind() != M3ObjectKind.PRECOMPUTE_OUTPUT) {
        throw new IOException("regex precompute output kind mismatch");
      }
      var mapped = store.openMapped(output, maxImageBytes);
      final MIndexRegexProgram.PrecomputedImage image;
      try {
        image = MIndexRegexProgramImageCodec.decodePrecomputed(mapped.bytes(0, mapped.size()));
      } catch (IllegalArgumentException invalid) {
        throw new IOException("invalid persisted regex image", invalid);
      }
      requireCompatible(image.execution().program(), request);
      return new Result(Optional.of(MIndexRegexProgram.fromPrecomputedImage(image)), true,
          Optional.of(output));
    }
    MIndexRegexProgram program = compiler.compile(request);
    if (program == null) {
      cache.publish(M3PrecomputeResult.empty(key));
      return new Result(Optional.empty(), false, Optional.empty());
    }
    var image = program.precomputedImage();
    requireCompatible(image.execution().program(), request);
    byte[] encoded = MIndexRegexProgramImageCodec.encode(image);
    if (encoded.length > maxImageBytes) throw new IOException("regex image exceeds budget");
    M3ObjectRef output = store.put(encoded, M3ObjectKind.PRECOMPUTE_OUTPUT);
    cache.publish(M3PrecomputeResult.value(key, output));
    return new Result(Optional.of(program), false, Optional.of(output));
  }

  private static void requireCompatible(MIndexRegexProgram.Image image, Request request)
      throws IOException {
    if (!image.expression().equals(request.expression()) || image.semantics() != request.semantics()
        || image.op().length > request.maxStates()) {
      throw new IOException("regex precompute request/image mismatch");
    }
  }
}
