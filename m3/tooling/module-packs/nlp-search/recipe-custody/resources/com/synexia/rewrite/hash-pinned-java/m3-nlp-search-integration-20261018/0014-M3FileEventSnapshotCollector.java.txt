// Copyright 2026 Hitesh Soliwal and contributors
// Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import com.m3.file.M3FileResolver;
import com.m3.file.M3FileText;
import com.m3.file.M3Hash256;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** One-pass MIDXEVT2 projection back into the canonical M3File builder. */
final class M3FileEventSnapshotCollector implements M3FileStreamEvents.Sink {
  private final M3FileBuilder fileBuilder;
  private final M3FileByteFacts.Builder byteFacts = new M3FileByteFacts.Builder();
  private final M3FileSourceFacts.Builder sourceFacts = new M3FileSourceFacts.Builder();
  private final List<M3FileText> lexicon = new ArrayList<>();
  private M3FileAttributes attributesBefore;
  private M3File file;
  private boolean started;
  private boolean finished;

  M3FileEventSnapshotCollector(M3FileResolver resolver, boolean precomputeRollingIndex) {
    fileBuilder = new M3FileBuilder(Objects.requireNonNull(resolver, "resolver"));
    if (precomputeRollingIndex) fileBuilder.enableRollingIndex();
  }

  @Override
  public void accept(M3FileStreamEvents.Event event) throws IOException {
    Objects.requireNonNull(event, "event");
    try {
      if (event instanceof M3FileStreamEvents.Started startedEvent) {
        if (started) throw invalid("duplicate MIDXEVT2 start");
        started = true;
        attributesBefore = startedEvent.attributes();
      } else if (event instanceof M3FileStreamEvents.Bytes bytes) {
        requireStarted("bytes");
        if (finished) throw invalid("MIDXEVT2 bytes after finish");
        byte[] payload = bytes.payload();
        byteFacts.accept(payload, 0, payload.length);
      } else if (event instanceof M3FileStreamEvents.Lexicon lexiconEvent) {
        requireStarted("lexicon");
        if (finished || lexiconEvent.localHandle() != lexicon.size()) {
          throw invalid("non-contiguous MIDXEVT2 lexicon handle");
        }
        lexicon.add(new M3FileText(fileBuilder.resolver(), lexiconEvent.resolverTokenIds()));
      } else if (event instanceof M3FileStreamEvents.Token token) {
        requireStarted("token");
        if (finished || token.localHandle() < 0 || token.localHandle() >= lexicon.size()) {
          throw invalid("MIDXEVT2 token has no replayable lexicon entry");
        }
        M3FileText value = lexicon.get(token.localHandle());
        if (token.ordinal() != fileBuilder.tokenCount()
            || token.utf16Start() != fileBuilder.utf16Length()
            || token.utf8End() - token.utf8Start() != value.utf8Length()
            || token.codePointEnd() - token.codePointStart() != value.codePointCount()
            || !MessageDigest.isEqual(token.sha256(), sha256(value))) {
          throw invalid("MIDXEVT2 token disagrees with resolver-owned lexicon value");
        }
        int replayedHandle = fileBuilder.addAndGetLocalHandle(value, token.kind());
        if (replayedHandle != token.localHandle()
            || token.utf16End() != fileBuilder.utf16Length()) {
          throw invalid("MIDXEVT2 token changed canonical local geometry");
        }
        sourceFacts.acceptToken(
            replayedHandle,
            Math.toIntExact(token.utf8End() - token.utf8Start()),
            token.codePointEnd() - token.codePointStart(),
            token.sha256());
      } else if (event instanceof M3FileStreamEvents.Finished finishedEvent) {
        finish(finishedEvent);
      } else {
        throw invalid("unknown MIDXEVT2 event type");
      }
    } catch (IOException invalid) {
      throw invalid;
    } catch (RuntimeException invalid) {
      throw invalid("invalid MIDXEVT2 replay projection", invalid);
    }
  }

  M3File file() throws IOException {
    if (!finished || file == null) throw invalid("MIDXEVT2 replay did not finish");
    return file;
  }

  private void finish(M3FileStreamEvents.Finished finishedEvent) throws IOException {
    requireStarted("finish");
    if (finished || attributesBefore == null) throw invalid("duplicate MIDXEVT2 finish");
    M3FileAttributes after = finishedEvent.attributesAfterRead();
    if (!attributesBefore.sameContentIdentity(after)) {
      throw invalid("MIDXEVT2 replay source identity changed");
    }
    M3FileByteFacts byteProjection = byteFacts.finish();
    if (byteProjection.byteCount() != finishedEvent.byteCount()
        || byteProjection.byteCount() != attributesBefore.size()
        || byteProjection.byteCount() != after.size()
        || !byteProjection.sha256().equals(M3Hash256.of(finishedEvent.contentSha256()))
        || fileBuilder.tokenCount() != finishedEvent.tokenCount()
        || fileBuilder.uniqueTokenCount() != finishedEvent.uniqueTokenCount()
        || fileBuilder.uniqueTokenCount() != lexicon.size()
        || sourceFacts.utf8Length() != byteProjection.byteCount()) {
      throw invalid("MIDXEVT2 replay summary disagrees with derived lanes");
    }
    M3FileSourceFacts facts =
        sourceFacts.finish(
            attributesBefore,
            after,
            byteProjection,
            fileBuilder.tokenCount(),
            fileBuilder.uniqueTokenCount());
    if (!facts.stableDuringRead()) throw invalid("MIDXEVT2 replay source facts are unstable");
    file = fileBuilder.build(facts);
    finished = true;
  }

  private void requireStarted(String event) throws IOException {
    if (!started) throw invalid("MIDXEVT2 " + event + " precedes start");
  }

  private static IOException invalid(String message) {
    return new IOException(message);
  }

  private static IOException invalid(String message, RuntimeException cause) {
    return new IOException(message, cause);
  }

  private static byte[] sha256(M3FileText value) {
    try {
      return MessageDigest.getInstance("SHA-256")
          .digest(value.getBytes(StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException impossible) {
      throw new AssertionError("SHA-256 is required by the JDK", impossible);
    }
  }
}
