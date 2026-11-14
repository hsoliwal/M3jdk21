// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.projection;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

/** Dependency-free executable contract tests; uses the real identity implementation. */
public final class M3SemanticIdentitySelfTest {
  private static final String A = "1".repeat(64);
  private static final String B = "2".repeat(64);
  private static final String C = "3".repeat(64);
  private static final String D = "4".repeat(64);
  private static final String E = "5".repeat(64);

  private M3SemanticIdentitySelfTest() {}

  public static void main(String[] args) throws Exception {
    exactUtf16DoesNotReplaceSurrogates();
    typedFieldsHaveUnambiguousFraming();
    segmentBoundariesAndSlicesInvalidate();
    everyModelAndSchemaRevisionInvalidates();
    sourceAndContextCoordinatesAreBound();
    malformedAndIncompleteIdentitiesRefuse();
    builderConsumesTextWithoutRetainingOrFlattening();
    repeatedAndConcurrentBuildsAgree();
    identitiesHaveValueEquality();
    segmentMetadataRevisionsInvalidate();
    failedTextReadClosesPartialNullableField();
    System.out.println("PASS: 11 semantic identity contract groups");
  }

  private static M3SemanticIdentity.Model model() {
    return new M3SemanticIdentity.Model(A, B, C, D);
  }

  private static M3SemanticIdentity value(CharSequence... segments) {
    var builder = M3SemanticIdentity.builder("model", model(), "backend", E, segments.length);
    for (CharSequence segment : segments) builder.segment(segment);
    return builder.build();
  }

  private static void exactUtf16DoesNotReplaceSurrogates() {
    different(value("\uD800").rootHash(), value("\uD801").rootHash(), "high surrogate");
    different(value("\uDC00").rootHash(), value("\uDC01").rootHash(), "low surrogate");
    different(value("\uD800").rootHash(), value("?").rootHash(), "replacement marker");
    different(value("\uD800").rootHash(), value("\uFFFD").rootHash(), "Unicode replacement");
    different(value("\uD83D\uDE00").rootHash(), value("\uDE00\uD83D").rootHash(), "pair order");
    different(value("a\u0000b").rootHash(), value("a", "b").rootHash(), "embedded NUL");
    equal(value("\uD83D\uDE00").rootHash(), value(new StringBuilder("\uD83D\uDE00")).rootHash(),
        "equivalent CharSequence");
  }

  private static void typedFieldsHaveUnambiguousFraming() {
    different(M3SemanticIdentity.hasher("test").text("ab").text("c").finish(),
        M3SemanticIdentity.hasher("test").text("a").text("bc").finish(), "field boundaries");
    different(M3SemanticIdentity.hasher("test").nullableText(null).finish(),
        M3SemanticIdentity.hasher("test").nullableText("").finish(), "null versus empty");
    different(M3SemanticIdentity.hasher("test").integer(1).finish(),
        M3SemanticIdentity.hasher("test").text("1").finish(), "integer versus text");
    different(M3SemanticIdentity.hasher("test").integer(1).finish(),
        M3SemanticIdentity.hasher("test").longValue(1).finish(), "integer width");
    different(M3SemanticIdentity.hasher("left").text("same").finish(),
        M3SemanticIdentity.hasher("right").text("same").finish(), "domain separation");
    different(M3SemanticIdentity.hasher("test").root(A).finish(),
        M3SemanticIdentity.hasher("test").text(A).finish(), "root versus spelling");
    equal(M3SemanticIdentity.hasher("test").text("abc").finish(),
        M3SemanticIdentity.hasher("test").text("xabcx", 1, 4).finish(), "text range contents");
  }

  private static void segmentBoundariesAndSlicesInvalidate() {
    different(value("ab", "c").rootHash(), value("a", "bc").rootHash(), "segment seams");
    different(value("abc").rootHash(), value("ab", "c").rootHash(), "segment count");
    different(value("").rootHash(), value().rootHash(), "empty segment count");
    var sliced = M3SemanticIdentity.builder("model", model(), "backend", E, 1)
        .segment("xabx", 1, 3).build();
    different(sliced.rootHash(), value("ab").rootHash(), "segment source range");
    equal(2L, sliced.utf16Length(), "slice width");
    equal(1, sliced.segmentCount(), "slice count");
  }

  private static void everyModelAndSchemaRevisionInvalidates() {
    String baseline = value("same").rootHash();
    List<M3SemanticIdentity.Model> changed = List.of(
        new M3SemanticIdentity.Model(E, B, C, D),
        new M3SemanticIdentity.Model(A, E, C, D),
        new M3SemanticIdentity.Model(A, B, E, D),
        new M3SemanticIdentity.Model(A, B, C, E));
    for (var revision : changed) {
      different(baseline, M3SemanticIdentity.builder("model", revision, "backend", E, 1)
          .segment("same").build().rootHash(), "model metadata revision");
    }
    different(baseline, M3SemanticIdentity.builder("other-model", model(), "backend", E, 1)
        .segment("same").build().rootHash(), "model name");
    different(baseline, M3SemanticIdentity.builder("model", model(), "other-backend", E, 1)
        .segment("same").build().rootHash(), "backend identifier");
    different(baseline, M3SemanticIdentity.builder("model", model(), "backend", A, 1)
        .segment("same").build().rootHash(), "schema identity");
    different(value("one").rootHash(), value("two").rootHash(), "actual evaluated text");
  }

  private static void sourceAndContextCoordinatesAreBound() {
    var scope = new M3SemanticIdentity.Scope(A, B, C, 11, 12, 10, 13);
    String baseline = scoped(scope);
    for (var differentScope : List.of(
        new M3SemanticIdentity.Scope(D, B, C, 11, 12, 10, 13),
        new M3SemanticIdentity.Scope(A, D, C, 11, 12, 10, 13),
        new M3SemanticIdentity.Scope(A, B, D, 11, 12, 10, 13),
        new M3SemanticIdentity.Scope(A, B, C, 10, 12, 10, 13),
        new M3SemanticIdentity.Scope(A, B, C, 11, 13, 10, 13),
        new M3SemanticIdentity.Scope(A, B, C, 21, 22, 20, 23))) {
      different(baseline, scoped(differentScope), "owner/snapshot/context/source range");
    }
    var large = new M3SemanticIdentity.Scope(A, B, C,
        Long.MAX_VALUE - 2, Long.MAX_VALUE - 1, Long.MAX_VALUE - 3, Long.MAX_VALUE);
    different(baseline, scoped(large), "long coordinate width");
    expect(IllegalArgumentException.class,
        () -> scoped(new M3SemanticIdentity.Scope(A, B, C, 0, 1, 0, 4)),
        "context range must match complete evaluated state");
  }

  private static String scoped(M3SemanticIdentity.Scope scope) {
    return M3SemanticIdentity.builder("model", model(), "backend", E, 1, scope)
        .segment("abc").build().rootHash();
  }

  private static void malformedAndIncompleteIdentitiesRefuse() {
    for (String malformed : List.of("", "a".repeat(63), "a".repeat(65), "A".repeat(64), "z".repeat(64))) {
      expect(IllegalArgumentException.class,
          () -> new M3SemanticIdentity.Model(malformed, B, C, D), "invalid model SHA");
    }
    expect(IllegalArgumentException.class,
        () -> new M3SemanticIdentity.Scope(A, B, C, -1, 1, 0, 3), "negative source");
    expect(IllegalArgumentException.class,
        () -> new M3SemanticIdentity.Scope(A, B, C, 2, 1, 0, 3), "reversed source");
    expect(IllegalArgumentException.class,
        () -> new M3SemanticIdentity.Scope(A, B, C, 0, 4, 0, 3), "source outside context");
    expect(IllegalArgumentException.class,
        () -> M3SemanticIdentity.builder("model", model(), "backend", E, -1), "negative count");
    expect(IllegalArgumentException.class,
        () -> M3SemanticIdentity.builder(" ", model(), "backend", E, 1), "blank model");
    expect(IllegalStateException.class,
        () -> M3SemanticIdentity.builder("model", model(), "backend", E, 2).segment("a").build(),
        "missing segment");
    expect(IllegalStateException.class,
        () -> M3SemanticIdentity.builder("model", model(), "backend", E, 0).segment("a"),
        "excess segment");
    var finished = M3SemanticIdentity.builder("model", model(), "backend", E, 1).segment("a");
    finished.build();
    expect(IllegalStateException.class, finished::build, "builder is one-shot");
    expect(IllegalStateException.class, () -> finished.segment("a"), "closed builder");
    var hasher = M3SemanticIdentity.hasher("test");
    hasher.finish();
    expect(IllegalStateException.class, () -> hasher.text("late"), "closed digest");
    expect(IndexOutOfBoundsException.class,
        () -> M3SemanticIdentity.hasher("test").text("x", 0, 2), "invalid source slice");
  }

  private static void builderConsumesTextWithoutRetainingOrFlattening() {
    StringBuilder mutable = new StringBuilder("abc");
    var pending = M3SemanticIdentity.builder("model", model(), "backend", E, 1).segment(mutable);
    mutable.setCharAt(0, 'z');
    equal(value("abc").rootHash(), pending.build().rootHash(), "consumed text cannot drift later");
    CharSequence streaming = new CharSequence() {
      @Override public int length() { return 3; }
      @Override public char charAt(int index) { return "abc".charAt(index); }
      @Override public CharSequence subSequence(int start, int end) {
        throw new AssertionError("must stream charAt without retaining/copying a slice");
      }
      @Override public String toString() {
        throw new AssertionError("must not flatten source text");
      }
    };
    equal(value("abc").rootHash(), value(streaming).rootHash(), "streamed source");
  }

  private static void repeatedAndConcurrentBuildsAgree() throws Exception {
    String expected = value("\uD800", "context", "\uD83D\uDE00").rootHash();
    try (var workers = Executors.newFixedThreadPool(4)) {
      List<Callable<String>> jobs = new ArrayList<>();
      for (int i = 0; i < 32; i++) {
        jobs.add(() -> value("\uD800", "context", "\uD83D\uDE00").rootHash());
      }
      for (var future : workers.invokeAll(jobs)) equal(expected, future.get(), "worker-independent root");
    }
  }

  private static void different(Object left, Object right, String label) {
    if (left.equals(right)) throw new AssertionError(label + " did not invalidate identity");
  }

  private static void identitiesHaveValueEquality() {
    var first = value("same", "context");
    var second = value("same", "context");
    equal(first, second, "independent identical values");
    equal(first.hashCode(), second.hashCode(), "equal identities have equal hash codes");
    equal(1, new java.util.HashSet<>(List.of(first, second)).size(), "identity set admission");
    different(first, value("changed", "context"), "different identity value");
    if (first.equals(null) || first.equals(first.rootHash())) {
      throw new AssertionError("identity equality accepted a different type");
    }
  }

  private static void segmentMetadataRevisionsInvalidate() {
    var first = M3SemanticIdentity.builder("model", model(), "backend", E, 1)
        .segment("abc", 0, 3, A).build();
    var second = M3SemanticIdentity.builder("model", model(), "backend", E, 1)
        .segment("abc", 0, 3, B).build();
    different(first, second, "segment metadata revision");
    different(first, value("abc"), "metadata presence");
    equal(first, M3SemanticIdentity.builder("model", model(), "backend", E, 1)
        .segment("abc", 0, 3, A).build(), "same segment metadata");
    expect(IllegalArgumentException.class,
        () -> M3SemanticIdentity.builder("model", model(), "backend", E, 1)
            .segment("abc", 0, 3, "invalid"), "metadata hash validation");
  }

  private static void failedTextReadClosesPartialNullableField() {
    CharSequence unreadable = new CharSequence() {
      @Override public int length() { throw new IllegalStateException("unavailable source"); }
      @Override public char charAt(int index) { throw new AssertionError("length must be read first"); }
      @Override public CharSequence subSequence(int start, int end) { throw new AssertionError(); }
    };
    var hasher = M3SemanticIdentity.hasher("failure");
    expect(IllegalStateException.class, () -> hasher.nullableText(unreadable), "source read failure");
    expect(IllegalStateException.class, hasher::finish, "partial nullable field cannot be finished");
    expect(IllegalStateException.class, () -> hasher.integer(1), "partial digest cannot be reused");
  }

  private static void equal(Object expected, Object actual, String label) {
    if (!expected.equals(actual)) throw new AssertionError(label + ": " + expected + " != " + actual);
  }

  private static void expect(Class<? extends Throwable> type, Runnable operation, String label) {
    try {
      operation.run();
    } catch (Throwable failure) {
      if (type.isInstance(failure)) return;
      throw new AssertionError(label + " wrong refusal", failure);
    }
    throw new AssertionError(label + " was accepted");
  }
}
