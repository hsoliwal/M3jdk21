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
public final class M3IndexPrecomputeConsumerProfilesOracle {
  private static int checks;
  private M3IndexPrecomputeConsumerProfilesOracle() { }

  private static void check(boolean yes, String reason) {
    checks++;
    if (!yes) throw new AssertionError(reason);
  }

  private static void failure(Class<? extends Throwable> type, Runnable action) {
    try { action.run(); }
    catch (Throwable cause) { check(type.isInstance(cause), "wrong failure " + cause); return; }
    throw new AssertionError("missing " + type.getName());
  }

  private static void equal(M3IndexPrecomputeWorkload left, M3IndexPrecomputeWorkload right) {
    check(left.buildPenalty() == right.buildPenalty(), "build penalty differs");
    for (M3IndexPrecomputeCapability capability : M3IndexPrecomputeCapability.values())
      check(left.weight(capability) == right.weight(capability), "different weight: " + capability);
  }

  public static void main(String[] args) throws Exception {
    var owners = M3IndexPrecomputeConsumerProfiles.Consumer.values();
    check(owners.length == 7, "missing consumer");
    for (var consumer : owners) {
      Map<M3IndexPrecomputeCapability, Integer> profile =
          M3IndexPrecomputeConsumerProfiles.weights(consumer);
      check(!profile.isEmpty(), "empty profile");
      var single = M3IndexPrecomputeConsumerProfiles.forConsumer(consumer);
      check(single.buildPenalty() == 1, "default penalty");
      for (var capability : M3IndexPrecomputeCapability.values()) {
        int score = single.weight(capability);
        check(score == profile.getOrDefault(capability, 0), "profile mismatch");
        check(score >= 0 && score <= 200, "profile score range");
      }
      failure(UnsupportedOperationException.class,
          () -> profile.put(M3IndexPrecomputeCapability.EXACT_IDENTITY, 1));
    }
    var regex = M3IndexPrecomputeConsumerProfiles.forConsumer(
        M3IndexPrecomputeConsumerProfiles.Consumer.REGEX);
    var coll = M3IndexPrecomputeConsumerProfiles.forConsumer(
        M3IndexPrecomputeConsumerProfiles.Consumer.COLLECTIONS);
    check(regex.weight(M3IndexPrecomputeCapability.PATTERN_PLAN) == 200, "regex priority");
    check(regex.weight(M3IndexPrecomputeCapability.REGEX_SET) == 190, "regex set priority");
    check(regex.weight(M3IndexPrecomputeCapability.PATTERN_PLAN) >
          regex.weight(M3IndexPrecomputeCapability.EXACT_IDENTITY), "not regex first");
    check(coll.weight(M3IndexPrecomputeCapability.DOMAIN_MEMBERSHIP) == 190, "collection membership");
    check(coll.weight(M3IndexPrecomputeCapability.PATTERN_PLAN) == 0, "collection avoids regex plan");
    check(M3IndexPrecomputeConsumerProfiles.forConsumer(
          M3IndexPrecomputeConsumerProfiles.Consumer.SWING).weight(
          M3IndexPrecomputeCapability.SEGMENT_LAYOUT) == 200, "viewport geometry nomination");
    check(M3IndexPrecomputeConsumerProfiles.forConsumer(
          M3IndexPrecomputeConsumerProfiles.Consumer.READER).weight(
          M3IndexPrecomputeCapability.UTF16_LENGTH) > 0, "Reader UTF16 geometry");
    check(M3IndexPrecomputeConsumerProfiles.forConsumer(
          M3IndexPrecomputeConsumerProfiles.Consumer.WRITER).weight(
          M3IndexPrecomputeCapability.UTF8_LENGTH) > 0, "Writer UTF8 geometry");
    check(M3IndexPrecomputeConsumerProfiles.forConsumer(
          M3IndexPrecomputeConsumerProfiles.Consumer.UTIL).weight(
          M3IndexPrecomputeCapability.HASH64) > 0, "Util hashing");

    failure(NullPointerException.class, () -> M3IndexPrecomputeConsumerProfiles.forConsumer(null));
    failure(IllegalArgumentException.class,
        () -> M3IndexPrecomputeConsumerProfiles.blend(Map.of(), 1));
    failure(IllegalArgumentException.class,
        () -> M3IndexPrecomputeConsumerProfiles.blend(Map.of(owners[0], 0), 1));
    failure(IllegalArgumentException.class,
        () -> M3IndexPrecomputeConsumerProfiles.blend(Map.of(owners[0], -1), 1));
    failure(IllegalArgumentException.class,
        () -> M3IndexPrecomputeConsumerProfiles.blend(Map.of(owners[0], 1025), 1));
    failure(IllegalArgumentException.class,
        () -> M3IndexPrecomputeConsumerProfiles.blend(Map.of(owners[0], 1), -1));
    HashMap<M3IndexPrecomputeConsumerProfiles.Consumer, Integer> invalid = new HashMap<>();
    invalid.put(null, 1);
    failure(NullPointerException.class, () -> M3IndexPrecomputeConsumerProfiles.blend(invalid, 1));
    invalid.clear(); invalid.put(owners[0], null);
    failure(NullPointerException.class, () -> M3IndexPrecomputeConsumerProfiles.blend(invalid, 1));

    Random random = new Random(0x4d335ec7L);
    for (int epoch = 0; epoch < 1024; epoch++) {
      List<M3IndexPrecomputeConsumerProfiles.Consumer> reordered = new ArrayList<>(List.of(owners));
      Collections.shuffle(reordered, random);
      EnumMap<M3IndexPrecomputeConsumerProfiles.Consumer, Integer> shares =
          new EnumMap<>(M3IndexPrecomputeConsumerProfiles.Consumer.class);
      for (var consumer : owners) shares.put(consumer, 1 + random.nextInt(1024));
      var baseline = M3IndexPrecomputeConsumerProfiles.blend(shares, epoch & 7);
      Map<M3IndexPrecomputeConsumerProfiles.Consumer, Integer> permuted = new HashMap<>();
      for (var consumer : reordered) permuted.put(consumer, shares.get(consumer));
      equal(baseline, M3IndexPrecomputeConsumerProfiles.blend(permuted, epoch & 7));
      for (var capability : M3IndexPrecomputeCapability.values()) {
        int expected = 0;
        for (var consumer : owners) expected += shares.get(consumer) *
            M3IndexPrecomputeConsumerProfiles.weights(consumer).getOrDefault(capability, 0);
        check(baseline.weight(capability) == expected, "blending lost weight");
      }
      check(baseline.weight(M3IndexPrecomputeCapability.PATTERN_PLAN) > 0, "positive regex demand");
      check(baseline.weights().equals(M3IndexPrecomputeConsumerProfiles.blend(shares, epoch & 7).weights()),
          "immutable vector differs");
    }
    var maximal = new EnumMap<M3IndexPrecomputeConsumerProfiles.Consumer, Integer>(
        M3IndexPrecomputeConsumerProfiles.Consumer.class);
    for (var c : owners) maximal.put(c, 1024);
    var wide = M3IndexPrecomputeConsumerProfiles.blend(maximal, 0);
    for (var cap : M3IndexPrecomputeCapability.values())
      check(wide.weight(cap) >= 0 && wide.weight(cap) <= 7 * 1024 * 200,
          "bound arithmetic");
    failure(UnsupportedOperationException.class,
        () -> wide.weights().put(M3IndexPrecomputeCapability.PATTERN_PLAN, 0));

    try (var executor = Executors.newFixedThreadPool(4)) {
      List<Callable<M3IndexPrecomputeWorkload>> jobs = new ArrayList<>();
      for (int i = 0; i < 24; i++) jobs.add(() -> M3IndexPrecomputeConsumerProfiles.blend(maximal, 0));
      for (var result : executor.invokeAll(jobs)) equal(wide, result.get());
    }
    // Verify that this additive class did not mutate the existing balanced source policy.
    check(M3IndexPrecomputeWorkload.languageRuntime().weight(
        M3IndexPrecomputeCapability.EXACT_IDENTITY) == 100, "incumbent policy intact");
    check(M3IndexPrecomputeWorkload.languageRuntime().weight(
        M3IndexPrecomputeCapability.PATTERN_PLAN) == 45, "incumbent regex policy intact");
    System.out.println("PASS M3IndexPrecomputeConsumerProfilesOracle checks=" + checks);
  }
}
