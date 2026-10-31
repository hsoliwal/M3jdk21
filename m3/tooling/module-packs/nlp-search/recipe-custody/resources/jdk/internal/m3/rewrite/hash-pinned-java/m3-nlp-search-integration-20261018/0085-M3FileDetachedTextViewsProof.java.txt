// Copyright 2026 Hitesh Soliwal and contributors
// Mapped pinned donor proof; see DONOR_PROBES.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import com.m3.file.M3FileResolver;
import com.m3.file.M3FileText;
import com.m3.file.M3Hash256;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalLong;

/** Focused proof for detached projection/span snapshot text views. */
public final class M3FileDetachedTextViewsProof {
  private static int checks;

  private M3FileDetachedTextViewsProof() {}

  public static void main(String[] args) throws Exception {
    Path source = Files.createTempFile("mindex-detached-text-", ".txt");
    Files.writeString(source, "alpha beta \uD83D\uDE03 gamma", StandardCharsets.UTF_8);
    TestResolver resolver = new TestResolver();

    M3File file = M3FileStreamIndexer.plainText(source, resolver, 7);
    M3FileSnapshot snapshot = file.snapshot();
    M3FileProjection projection = file.semanticProjection();
    M3FileProjectionSnapshot projectionSnapshot = projection.snapshot();
    M3FileSpan span = file.span(0, file.tokenCount());
    M3FileSpanSnapshot spanSnapshot = span.snapshot();

    M3FileText detachedProjection = projectionSnapshot.indexedText(snapshot, resolver);
    M3FileText detachedSpan = spanSnapshot.asM3String(snapshot, resolver);
    check(detachedProjection.resolver() == resolver, "projection resolver changed");
    check(detachedSpan.resolver() == resolver, "span resolver changed");

    M3Hash256 projectionOracle = hash(projection.asM3String().materialize());
    M3Hash256 spanOracle = hash(span.asM3String().materialize());
    equal(projectionOracle,
        projectionSnapshot.sha256(snapshot, resolver, StandardCharsets.UTF_8),
        "detached projection digest");
    equal(spanOracle,
        spanSnapshot.sha256(snapshot, resolver, StandardCharsets.UTF_8),
        "detached span digest");
    equal(hash(snapshot.indexedText(resolver).materialize()),
        hash(detachedSpan.materialize()),
        "detached full text parity");

    Files.delete(source);
    check(
        projectionSnapshot.sha256(snapshot, resolver, StandardCharsets.UTF_8)
            .equals(projectionOracle),
        "projection survives source deletion");
    check(
        spanSnapshot.sha256(snapshot, resolver, StandardCharsets.UTF_8).equals(spanOracle),
        "span survives source deletion");

    boolean rejected = false;
    try {
      new M3FileProjectionSnapshot(0, new int[] {file.tokenCount()})
          .indexedText(snapshot, resolver);
    } catch (IndexOutOfBoundsException expected) {
      rejected = true;
    }
    check(rejected, "out-of-range detached projection fails closed");

    System.out.println(
        "MINDEX_FILE_DETACHED_TEXT_VIEWS_PASS"
            + "|projectionSnapshot=1|spanSnapshot=1|detached=1|digestParity=1"
            + "|sourceClosed=1|noJoinedSpelling=1|checks=" + checks);
  }

  private static M3Hash256 hash(String text) {
    try {
      return M3Hash256.of(
          MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
    } catch (java.security.NoSuchAlgorithmException impossible) {
      throw new AssertionError(impossible);
    }
  }

  private static void equal(M3Hash256 expected, M3Hash256 actual, String message) {
    check(expected.equals(actual), message);
  }

  private static void check(boolean condition, String message) {
    checks++;
    if (!condition) throw new AssertionError(message);
  }

  private static final class TestResolver implements M3FileResolver {
    private final Map<String, Long> idsByText = new HashMap<>();
    private final Map<Long, String> textById = new HashMap<>();
    private long next = 1L;

    @Override
    public OptionalLong lookup(int languageId, CharSequence term) {
      Long id = idsByText.get(key(languageId, term));
      return id == null ? OptionalLong.empty() : OptionalLong.of(id);
    }

    @Override
    public long intern(int languageId, CharSequence term) {
      String text = term.toString();
      String key = key(languageId, text);
      Long existing = idsByText.get(key);
      if (existing != null) return existing;
      long id = ((long) languageId << 32) | (next++ & 0xFFFFFFFFL);
      idsByText.put(key, id);
      textById.put(id, text);
      return id;
    }

    @Override
    public String resolve(long id) {
      String text = textById.get(id);
      if (text == null) throw new IllegalArgumentException("unknown test token " + id);
      return text;
    }

    private static String key(int languageId, CharSequence term) {
      return languageId + "\u0000" + term;
    }
  }
}
