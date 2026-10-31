// Copyright 2026 Hitesh Soliwal and contributors
// Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Deterministic binary serializer for the one-pass M3File stream events.
 *
 * <p>Format MIDXEVT2: an ASCII magic followed by big-endian length-delimited frames.
 * Frame payloads retain the raw byte chunks, exact token spans/hashes and both filesystem
 * metadata snapshots. This is an event artifact, not a replacement for M3FileImage.</p>
 */
public final class M3FileEventWriter
    implements M3FileStreamEvents.Sink, AutoCloseable {
  private static final byte[] MAGIC = "MIDXEVT2".getBytes(StandardCharsets.US_ASCII);
  private static final int STARTED = 1;
  private static final int BYTES = 2;
  private static final int LEXICON = 3;
  private static final int TOKEN = 4;
  private static final int FINISHED = 5;

  private final DataOutputStream output;
  private final boolean writeMagic;
  private final MessageDigest contentDigest = digest();
  private boolean started;
  private boolean finished;
  private long byteCount;
  private int tokenCount;
  private long utf8End;
  private int utf16End;
  private int codePointEnd;
  private final Set<Integer> lexiconHandles = new HashSet<>();

  public M3FileEventWriter(OutputStream output) throws IOException {
    this(output, true);
  }

  /** Construct an event writer whose frames are embedded in an outer stream envelope. */
  static M3FileEventWriter embedded(OutputStream output) throws IOException {
    return new M3FileEventWriter(output, false);
  }

  private M3FileEventWriter(OutputStream output, boolean writeMagic) throws IOException {
    this.output = new DataOutputStream(Objects.requireNonNull(output, "output"));
    this.writeMagic = writeMagic;
    if (writeMagic) this.output.write(MAGIC);
  }

  @Override
  public void accept(M3FileStreamEvents.Event event) throws IOException {
    Objects.requireNonNull(event, "event");
    if (event instanceof M3FileStreamEvents.Started startedEvent) {
      if (started || finished) throw new IOException("duplicate MIDXEVT2 start");
      started = true;
      writeMetadataFrame(STARTED, startedEvent.attributes());
    } else if (event instanceof M3FileStreamEvents.Bytes bytes) {
      requireStarted();
      if (finished) throw new IOException("MIDXEVT2 bytes after finish");
      if (bytes.offset() != byteCount) throw new IOException("non-contiguous MIDXEVT2 bytes");
      byte[] payload = bytes.payload();
      contentDigest.update(payload);
      byteCount = Math.addExact(byteCount, payload.length);
      ByteArrayOutputStream frame = new ByteArrayOutputStream(12 + payload.length);
      DataOutputStream data = new DataOutputStream(frame);
      data.writeLong(bytes.offset());
      data.writeInt(payload.length);
      data.write(payload);
      data.flush();
      writeFrame(BYTES, frame.toByteArray());
    } else if (event instanceof M3FileStreamEvents.Lexicon lexicon) {
      requireStarted();
      if (finished) throw new IOException("MIDXEVT2 lexicon after finish");
      if (lexicon.localHandle() != lexiconHandles.size()
          || !lexiconHandles.add(lexicon.localHandle())) {
        throw new IOException("non-contiguous or duplicate MIDXEVT2 lexicon handle");
      }
      long[] ids = lexicon.resolverTokenIds();
      ByteArrayOutputStream frame = new ByteArrayOutputStream(12 + 8 * ids.length);
      DataOutputStream data = new DataOutputStream(frame);
      data.writeInt(lexicon.localHandle());
      data.writeInt(ids.length);
      for (long id : ids) data.writeLong(id);
      data.flush();
      writeFrame(LEXICON, frame.toByteArray());
    } else if (event instanceof M3FileStreamEvents.Token token) {
      requireStarted();
      if (finished) throw new IOException("MIDXEVT2 token after finish");
      if (!lexiconHandles.contains(token.localHandle())) {
        throw new IOException("MIDXEVT2 token has no lexicon entry");
      }
      if (token.ordinal() != tokenCount) throw new IOException("non-contiguous MIDXEVT2 token");
      if (token.utf8Start() != utf8End
          || token.utf16Start() != utf16End
          || token.codePointStart() != codePointEnd) {
        throw new IOException("non-contiguous MIDXEVT2 token coordinates");
      }
      ByteArrayOutputStream frame = new ByteArrayOutputStream(80);
      DataOutputStream data = new DataOutputStream(frame);
      data.writeInt(token.ordinal());
      data.writeInt(token.localHandle());
      data.writeByte(token.kind().code());
      data.writeInt(token.utf16Start());
      data.writeInt(token.utf16End());
      data.writeLong(token.utf8Start());
      data.writeLong(token.utf8End());
      data.writeInt(token.codePointStart());
      data.writeInt(token.codePointEnd());
      data.write(token.sha256());
      data.flush();
      writeFrame(TOKEN, frame.toByteArray());
      tokenCount = Math.incrementExact(tokenCount);
      utf8End = token.utf8End();
      utf16End = token.utf16End();
      codePointEnd = token.codePointEnd();
    } else if (event instanceof M3FileStreamEvents.Finished finishedEvent) {
      requireStarted();
      if (finished) throw new IOException("duplicate MIDXEVT2 finish");
      if (finishedEvent.tokenCount() != tokenCount
          || finishedEvent.uniqueTokenCount() != lexiconHandles.size()
          || finishedEvent.byteCount() != byteCount
          || finishedEvent.attributesAfterRead().size() != byteCount
          || !MessageDigest.isEqual(finishedEvent.contentSha256(), contentDigest.digest())) {
        throw new IOException("MIDXEVT2 stream summary mismatch");
      }
      writeFinishedFrame(finishedEvent);
      finished = true;
    } else {
      throw new IOException("unknown MIDXEVT2 event: " + event.getClass().getName());
    }
  }

  public boolean isFinished() {
    return finished;
  }

  @Override
  public void close() throws IOException {
    output.close();
  }

  private void requireStarted() throws IOException {
    if (!started) throw new IOException("MIDXEVT2 event precedes start");
  }

  private void writeMetadataFrame(int type, M3FileAttributes attributes) throws IOException {
    ByteArrayOutputStream frame = new ByteArrayOutputStream(512);
    DataOutputStream data = new DataOutputStream(frame);
    writeMetadata(data, attributes);
    data.flush();
    writeFrame(type, frame.toByteArray());
  }

  private void writeFinishedFrame(M3FileStreamEvents.Finished event) throws IOException {
    ByteArrayOutputStream frame = new ByteArrayOutputStream(512);
    DataOutputStream data = new DataOutputStream(frame);
    writeMetadata(data, event.attributesAfterRead());
    data.writeInt(event.tokenCount());
    data.writeInt(event.uniqueTokenCount());
    data.writeLong(event.byteCount());
    writeBytes(data, event.contentSha256());
    data.flush();
    writeFrame(FINISHED, frame.toByteArray());
  }

  private void writeFrame(int type, byte[] payload) throws IOException {
    output.writeByte(type);
    output.writeInt(payload.length);
    output.write(payload);
  }

  private static void writeMetadata(DataOutputStream data, M3FileAttributes attributes)
      throws IOException {
    writeString(data, attributes.normalizedPath());
    data.writeLong(attributes.size());
    writeString(data, attributes.creationTime() == null ? null : attributes.creationTime().toString());
    writeString(data, attributes.lastModifiedTime() == null ? null : attributes.lastModifiedTime().toString());
    writeString(data, attributes.lastAccessTime() == null ? null : attributes.lastAccessTime().toString());
    data.writeBoolean(attributes.isRegularFile());
    data.writeBoolean(attributes.isDirectory());
    data.writeBoolean(attributes.isSymbolicLinkPath());
    data.writeBoolean(attributes.isOther());
    writeString(data, attributes.fileKey());
    writeString(data, attributes.owner());
    writeString(data, attributes.group());
    data.writeBoolean(attributes.posixAvailable());
    List<String> permissions =
        attributes.posixPermissions().stream().map(Enum::name).sorted().toList();
    data.writeInt(permissions.size());
    for (String permission : permissions) writeString(data, permission);
    data.writeBoolean(attributes.dosAvailable());
    data.writeBoolean(attributes.dosArchive());
    data.writeBoolean(attributes.dosHidden());
    data.writeBoolean(attributes.dosReadOnly());
    data.writeBoolean(attributes.dosSystem());
    writeStrings(data, attributes.aclEntries());
    writeMap(data, attributes.userDefinedAttributesBase64());
    writeMap(data, attributes.providerAttributes());
  }

  private static void writeStrings(DataOutputStream data, List<String> values) throws IOException {
    data.writeInt(values.size());
    for (String value : values) writeString(data, value);
  }

  private static void writeMap(DataOutputStream data, Map<String, String> values)
      throws IOException {
    Map<String, String> sorted = new TreeMap<>(values);
    data.writeInt(sorted.size());
    for (Map.Entry<String, String> entry : sorted.entrySet()) {
      writeString(data, entry.getKey());
      writeString(data, entry.getValue());
    }
  }

  private static void writeString(DataOutputStream data, String value) throws IOException {
    if (value == null) {
      data.writeInt(-1);
      return;
    }
    byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    data.writeInt(bytes.length);
    data.write(bytes);
  }

  private static void writeBytes(DataOutputStream data, byte[] bytes) throws IOException {
    data.writeInt(bytes.length);
    data.write(bytes);
  }

  private static MessageDigest digest() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException impossible) {
      throw new ExceptionInInitializerError(impossible);
    }
  }
}
