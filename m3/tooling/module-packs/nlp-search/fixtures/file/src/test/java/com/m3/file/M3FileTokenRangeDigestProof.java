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
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalLong;

/**
 * Focused proof that detached token-range SHA-256 delegates to canonical M3FileText semantics.
 *
 * <p>The source is deleted before the detached snapshot is queried. The comparison oracle is the
 * indexed M3FileText itself materialized only for the proof; production range hashing remains
 * atom-backed and does not reopen or flatten the source.</p>
 */
public final class M3FileTokenRangeDigestProof {
  private static int checks;

  private M3FileTokenRangeDigestProof() {}

  public static void main(String[] args) throws Exception {
    Path source = Files.createTempFile("mindex-file-token-range-", ".txt");
    Files.writeString(source, "alpha beta 😀 gamma", StandardCharsets.UTF_8);
    TestResolver resolver = new TestResolver();

    M3File file = M3FileStreamIndexer.plainText(source, resolver, 7);
    M3FileSnapshot snapshot = file.snapshot();
    M3FileText indexed = snapshot.indexedText(resolver);
    int tokenCount = file.tokenCount();
    check(tokenCount > 1, "fixture produces multiple token atoms");

    M3FileTokenRangeSha256 whole = snapshot.tokenRangeSha256(resolver, 0, tokenCount);
    check(
        whole.sha256().equals(
            M3Hash256.of(java.security.MessageDigest.getInstance("SHA-256").digest(indexed.materialize().getBytes(StandardCharsets.UTF_8)))),
        "whole token range equals indexed content digest");
    check(
        whole.byteCount() == snapshot.sourceFactsSnapshot().utf8Length(),
        "whole token range retains detached UTF-8 coordinates");

    M3FileSpan span = file.span(0, tokenCount);
    check(
        span.tokenRangeSha256().sha256().equals(whole.sha256()),
        "live M3FileSpan delegates to the same canonical digest owner");

    int end = Math.max(1, tokenCount - 1);
    M3FileTokenRangeSha256 prefix = snapshot.tokenRangeSha256(resolver, 0, end);
    int beginUtf16 = file.utf16Offset(0);
    int endUtf16 = file.utf16Offset(end);
    M3FileText indexedPrefix = indexed.substring(beginUtf16, endUtf16);
    check(
        prefix.sha256().equals(
            M3Hash256.of(java.security.MessageDigest.getInstance("SHA-256").digest(indexedPrefix.materialize().getBytes(StandardCharsets.UTF_8)))),
        "partial token range equals canonical UTF-16 range digest");
    check(
        prefix.utf8Start() == snapshot.sourceFactsSnapshot().utf8Offset(0)
            && prefix.utf8End() == snapshot.sourceFactsSnapshot().utf8Offset(end),
        "partial range preserves source byte boundaries");

    Files.delete(source);
    check(
        snapshot.tokenRangeSha256(resolver, 0, tokenCount).sha256().equals(whole.sha256()),
        "detached range remains readable after source deletion");

    boolean rejected = false;
    try {
      snapshot.tokenRangeSha256(resolver, tokenCount, 0);
    } catch (IndexOutOfBoundsException expected) {
      rejected = true;
    }
    check(rejected, "invalid range fails closed");

    System.out.println(
        "MINDEX_FILE_TOKEN_RANGE_DIGEST_PASS"
            + "|canonicalOwner=1|span=1|whole=1|partial=1|coordinates=1|detached=1|bounds=1|checks="
            + checks);
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
