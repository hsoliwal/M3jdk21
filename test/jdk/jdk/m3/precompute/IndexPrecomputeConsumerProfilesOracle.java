// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

/** Standalone actual-Java21 oracle; no JUnit stand-ins or third-party runtime. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/165ceb5a020291e99ebeb89783b88e8a9686ae5c/synexia-index-precompute/src/test/java/com/synexia/precompute/IndexPrecomputeConsumerProfilesContract.java blob e83acacb4e47; recipe m3/ports/precompute/recipe/consumer-profiles-20261107/relocate.py v3
public final class IndexPrecomputeConsumerProfilesOracle {
  private static int checks;
  private IndexPrecomputeConsumerProfilesOracle() { }

  private static void check(boolean yes, String reason) {
    checks++;
    if (!yes) throw new AssertionError(reason);
  }

  private static void failure(Class<? extends Throwable> type, Runnable action) {
    try { action.run(); }
    catch (Throwable cause) { check(type.isInstance(cause), "wrong failure " + cause); return; }
    throw new AssertionError("missing " + type.getName());
  }

  private static void equal(IndexPrecomputeWorkload left, IndexPrecomputeWorkload right) {
    check(left.buildPenalty() == right.buildPenalty(), "build penalty differs");
    for (IndexPrecomputeCapability capability : IndexPrecomputeCapability.values())
      check(left.weight(capability) == right.weight(capability), "different weight: " + capability);
  }

  public static void main(String[] args) throws Exception {
    var owners = IndexPrecomputeConsumerProfiles.Consumer.values();
    check(owners.length == 7, "missing consumer");
    for (var consumer : owners) {
      Map<IndexPrecomputeCapability, Integer> profile =
          IndexPrecomputeConsumerProfiles.weights(consumer);
      check(!profile.isEmpty(), "empty profile");
      var single = IndexPrecomputeConsumerProfiles.forConsumer(consumer);
      check(single.buildPenalty() == 1, "default penalty");
      for (var capability : IndexPrecomputeCapability.values()) {
        int score = single.weight(capability);
        check(score == profile.getOrDefault(capability, 0), "profile mismatch");
        check(score >= 0 && score <= 200, "profile score range");
      }
      failure(UnsupportedOperationException.class,
          () -> profile.put(IndexPrecomputeCapability.EXACT_IDENTITY, 1));
    }
    var regex = IndexPrecomputeConsumerProfiles.forConsumer(
        IndexPrecomputeConsumerProfiles.Consumer.REGEX);
    var coll = IndexPrecomputeConsumerProfiles.forConsumer(
        IndexPrecomputeConsumerProfiles.Consumer.COLLECTIONS);
    check(regex.weight(IndexPrecomputeCapability.PATTERN_PLAN) == 200, "regex priority");
    check(regex.weight(IndexPrecomputeCapability.REGEX_SET) == 190, "regex set priority");
    check(regex.weight(IndexPrecomputeCapability.PATTERN_PLAN) >
          regex.weight(IndexPrecomputeCapability.EXACT_IDENTITY), "not regex first");
    check(coll.weight(IndexPrecomputeCapability.DOMAIN_MEMBERSHIP) == 190, "collection membership");
    check(coll.weight(IndexPrecomputeCapability.PATTERN_PLAN) == 0, "collection avoids regex plan");
    check(IndexPrecomputeConsumerProfiles.forConsumer(
          IndexPrecomputeConsumerProfiles.Consumer.SWING).weight(
          IndexPrecomputeCapability.SEGMENT_LAYOUT) == 200, "viewport geometry nomination");
    check(IndexPrecomputeConsumerProfiles.forConsumer(
          IndexPrecomputeConsumerProfiles.Consumer.READER).weight(
          IndexPrecomputeCapability.UTF16_LENGTH) > 0, "Reader UTF16 geometry");
    check(IndexPrecomputeConsumerProfiles.forConsumer(
          IndexPrecomputeConsumerProfiles.Consumer.WRITER).weight(
          IndexPrecomputeCapability.UTF8_LENGTH) > 0, "Writer UTF8 geometry");
    check(IndexPrecomputeConsumerProfiles.forConsumer(
          IndexPrecomputeConsumerProfiles.Consumer.UTIL).weight(
          IndexPrecomputeCapability.HASH64) > 0, "Util hashing");

    failure(NullPointerException.class, () -> IndexPrecomputeConsumerProfiles.forConsumer(null));
    failure(IllegalArgumentException.class,
        () -> IndexPrecomputeConsumerProfiles.blend(Map.of(), 1));
    failure(IllegalArgumentException.class,
        () -> IndexPrecomputeConsumerProfiles.blend(Map.of(owners[0], 0), 1));
    failure(IllegalArgumentException.class,
        () -> IndexPrecomputeConsumerProfiles.blend(Map.of(owners[0], -1), 1));
    failure(IllegalArgumentException.class,
        () -> IndexPrecomputeConsumerProfiles.blend(Map.of(owners[0], 1025), 1));
    failure(IllegalArgumentException.class,
        () -> IndexPrecomputeConsumerProfiles.blend(Map.of(owners[0], 1), -1));
    HashMap<IndexPrecomputeConsumerProfiles.Consumer, Integer> invalid = new HashMap<>();
    invalid.put(null, 1);
    failure(NullPointerException.class, () -> IndexPrecomputeConsumerProfiles.blend(invalid, 1));
    invalid.clear(); invalid.put(owners[0], null);
    failure(NullPointerException.class, () -> IndexPrecomputeConsumerProfiles.blend(invalid, 1));

    Random random = new Random(0x4d335ec7L);
    for (int epoch = 0; epoch < 1024; epoch++) {
      List<IndexPrecomputeConsumerProfiles.Consumer> reordered = new ArrayList<>(List.of(owners));
      Collections.shuffle(reordered, random);
      EnumMap<IndexPrecomputeConsumerProfiles.Consumer, Integer> shares =
          new EnumMap<>(IndexPrecomputeConsumerProfiles.Consumer.class);
      for (var consumer : owners) shares.put(consumer, 1 + random.nextInt(1024));
      var baseline = IndexPrecomputeConsumerProfiles.blend(shares, epoch & 7);
      Map<IndexPrecomputeConsumerProfiles.Consumer, Integer> permuted = new HashMap<>();
      for (var consumer : reordered) permuted.put(consumer, shares.get(consumer));
      equal(baseline, IndexPrecomputeConsumerProfiles.blend(permuted, epoch & 7));
      for (var capability : IndexPrecomputeCapability.values()) {
        int expected = 0;
        for (var consumer : owners) expected += shares.get(consumer) *
            IndexPrecomputeConsumerProfiles.weights(consumer).getOrDefault(capability, 0);
        check(baseline.weight(capability) == expected, "blending lost weight");
      }
      check(baseline.weight(IndexPrecomputeCapability.PATTERN_PLAN) > 0, "positive regex demand");
      check(baseline.weights().equals(IndexPrecomputeConsumerProfiles.blend(shares, epoch & 7).weights()),
          "immutable vector differs");
    }
    var maximal = new EnumMap<IndexPrecomputeConsumerProfiles.Consumer, Integer>(
        IndexPrecomputeConsumerProfiles.Consumer.class);
    for (var c : owners) maximal.put(c, 1024);
    var wide = IndexPrecomputeConsumerProfiles.blend(maximal, 0);
    for (var cap : IndexPrecomputeCapability.values())
      check(wide.weight(cap) >= 0 && wide.weight(cap) <= 7 * 1024 * 200,
          "bound arithmetic");
    failure(UnsupportedOperationException.class,
        () -> wide.weights().put(IndexPrecomputeCapability.PATTERN_PLAN, 0));

    try (var executor = Executors.newFixedThreadPool(4)) {
      List<Callable<IndexPrecomputeWorkload>> jobs = new ArrayList<>();
      for (int i = 0; i < 24; i++) jobs.add(() -> IndexPrecomputeConsumerProfiles.blend(maximal, 0));
      for (var result : executor.invokeAll(jobs)) equal(wide, result.get());
    }
    // Verify that this additive class did not mutate the existing balanced source policy.
    check(IndexPrecomputeWorkload.languageRuntime().weight(
        IndexPrecomputeCapability.EXACT_IDENTITY) == 100, "incumbent policy intact");
    check(IndexPrecomputeWorkload.languageRuntime().weight(
        IndexPrecomputeCapability.PATTERN_PLAN) == 45, "incumbent regex policy intact");
    System.out.println("PASS IndexPrecomputeConsumerProfilesOracle checks=" + checks);
  }
}
