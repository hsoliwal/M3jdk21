// Copyright 2026 Hitesh Soliwal and contributors
// Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.IOException;
import java.util.Objects;

/**
 * Events emitted by one physical M3File payload traversal.
 *
 * <p>The event stream is observational: it cannot replace the canonical M3File builder,
 * resolver, or source stability gate. A sink may serialize bytes, token coordinates and metadata
 * while the source is read; it must not mutate the canonical index.</p>
 */
public final class M3FileStreamEvents {
  private M3FileStreamEvents() {
    throw new AssertionError("No instances");
  }

  @FunctionalInterface
  public interface Sink {
    void accept(Event event) throws IOException;
  }

  public sealed interface Event permits Started, Lexicon, Bytes, Token, Finished {}

  public record Started(M3FileAttributes attributes) implements Event {
    public Started {
      attributes = Objects.requireNonNull(attributes, "attributes");
    }
  }

  /**
   * The resolver-native token sequence for one newly admitted file-local handle.
   *
   * <p>The event makes the observational stream self-describing without copying canonical text
   * out of the resolver. Handles are emitted once, before their first token occurrence.</p>
   */
  public record Lexicon(int localHandle, long[] resolverTokenIds) implements Event {
    public Lexicon {
      if (localHandle < 0) throw new IllegalArgumentException("negative local handle");
      resolverTokenIds = Objects.requireNonNull(resolverTokenIds, "resolverTokenIds").clone();
      if (resolverTokenIds.length == 0) {
        throw new IllegalArgumentException("lexicon entry must contain a resolver token");
      }
    }

    @Override
    public long[] resolverTokenIds() {
      return resolverTokenIds.clone();
    }
  }

  public record Bytes(long offset, byte[] payload) implements Event {
    public Bytes {
      if (offset < 0L) throw new IllegalArgumentException("negative byte offset");
      payload = Objects.requireNonNull(payload, "payload").clone();
      if (payload.length == 0) throw new IllegalArgumentException("empty byte event");
    }

    /** Number of retained bytes without exposing or cloning the immutable payload. */
    public int byteCount() {
      return payload.length;
    }

    @Override
    public byte[] payload() {
      return payload.clone();
    }
  }

  public record Token(
      int ordinal,
      int localHandle,
      M3FileTokenKind kind,
      int utf16Start,
      int utf16End,
      long utf8Start,
      long utf8End,
      int codePointStart,
      int codePointEnd,
      byte[] sha256)
      implements Event {
    public Token {
      if (ordinal < 0) throw new IllegalArgumentException("negative token ordinal");
      if (localHandle < 0) throw new IllegalArgumentException("negative local handle");
      kind = Objects.requireNonNull(kind, "kind");
      if (utf16Start < 0 || utf16End < utf16Start) {
        throw new IllegalArgumentException("invalid UTF-16 span");
      }
      if (utf8Start < 0L || utf8End < utf8Start) {
        throw new IllegalArgumentException("invalid UTF-8 span");
      }
      if (codePointStart < 0 || codePointEnd < codePointStart) {
        throw new IllegalArgumentException("invalid code-point span");
      }
      sha256 = Objects.requireNonNull(sha256, "sha256").clone();
      if (sha256.length != 32) throw new IllegalArgumentException("SHA-256 must be 32 bytes");
    }

    @Override
    public byte[] sha256() {
      return sha256.clone();
    }
  }

  public record Finished(
      M3FileAttributes attributesAfterRead,
      int tokenCount,
      int uniqueTokenCount,
      long byteCount,
      byte[] contentSha256)
      implements Event {
    public Finished {
      attributesAfterRead =
          Objects.requireNonNull(attributesAfterRead, "attributesAfterRead");
      if (tokenCount < 0 || uniqueTokenCount < 0 || uniqueTokenCount > tokenCount) {
        throw new IllegalArgumentException("invalid token counts");
      }
      if (byteCount < 0L) throw new IllegalArgumentException("negative byte count");
      contentSha256 = Objects.requireNonNull(contentSha256, "contentSha256").clone();
      if (contentSha256.length != 32) {
        throw new IllegalArgumentException("SHA-256 must be 32 bytes");
      }
    }

    @Override
    public byte[] contentSha256() {
      return contentSha256.clone();
    }
  }

  public static Sink noOp() {
    return ignored -> {};
  }
}
