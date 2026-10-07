// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.precompute;

import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.ToLongFunction;

/** Factory helpers for exposing existing immutable precomputers through the pass API. */
public final class MIndexPrecomputePasses {
  private MIndexPrecomputePasses() {}

  public static <T> MIndexPrecomputePass<T> of(
      MIndexPrecomputeKey<T> output,
      String implementationRevision,
      List<MIndexPrecomputeKey<?>> dependencies,
      Function<MIndexPrecomputeContext, T> compute,
      ToLongFunction<T> retainedBytes,
      Function<T, String> contentSha256) {
    return new FunctionalPass<>(
        output,
        implementationRevision,
        dependencies,
        compute,
        retainedBytes,
        contentSha256);
  }

  public static <T> MIndexPrecomputePass<T> fixed(
      MIndexPrecomputeKey<T> output,
      String implementationRevision,
      Function<MIndexPrecomputeContext, T> compute,
      ToLongFunction<T> retainedBytes,
      Function<T, String> contentSha256) {
    return of(
        output,
        implementationRevision,
        List.of(),
        compute,
        retainedBytes,
        contentSha256);
  }

  public static String sha256(byte[] bytes) {
    return MIndexPrecomputeScope.sha256(Objects.requireNonNull(bytes, "bytes"));
  }

  public static String sha256Utf16(CharSequence text) {
    Objects.requireNonNull(text, "text");
    java.security.MessageDigest digest = MIndexPrecomputeScope.digest();
    MIndexPrecomputeScope.putLong(digest, text.length());
    for (int index = 0; index < text.length(); index++) {
      char unit = text.charAt(index);
      digest.update((byte) (unit >>> 8));
      digest.update((byte) unit);
    }
    return HexFormat.of().formatHex(digest.digest());
  }

  private record FunctionalPass<T>(
      MIndexPrecomputeKey<T> output,
      String implementationRevision,
      List<MIndexPrecomputeKey<?>> dependencies,
      Function<MIndexPrecomputeContext, T> computer,
      ToLongFunction<T> retainedBytesFunction,
      Function<T, String> contentSha256Function)
      implements MIndexPrecomputePass<T> {

    private FunctionalPass {
      Objects.requireNonNull(output, "output");
      implementationRevision = MIndexPrecomputeScope.required(implementationRevision);
      dependencies = List.copyOf(Objects.requireNonNull(dependencies, "dependencies"));
      Objects.requireNonNull(computer, "computer");
      Objects.requireNonNull(retainedBytesFunction, "retainedBytesFunction");
      Objects.requireNonNull(contentSha256Function, "contentSha256Function");
    }

    @Override
    public T compute(MIndexPrecomputeContext context) {
      return output.type().cast(
          Objects.requireNonNull(computer.apply(context), "precompute result"));
    }

    @Override
    public long retainedBytes(T value) {
      long bytes = retainedBytesFunction.applyAsLong(output.type().cast(value));
      if (bytes < 0) throw new IllegalArgumentException("negative retained bytes");
      return bytes;
    }

    @Override
    public String contentSha256(T value) {
      return MIndexPrecomputeScope.checkedDigest(
          contentSha256Function.apply(output.type().cast(value)));
    }
  }
}
