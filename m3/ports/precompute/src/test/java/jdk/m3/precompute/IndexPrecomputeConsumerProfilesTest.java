// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import org.junit.jupiter.api.Test;

/** Maven/Surefire bridge; the direct Java21 proof is separately executable offline. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/165ceb5a020291e99ebeb89783b88e8a9686ae5c/synexia-index-precompute/src/test/java/com/synexia/precompute/IndexPrecomputeConsumerProfilesTest.java blob e496c3a5128c; recipe m3/ports/precompute/recipe/consumer-profiles-20261107/relocate.py v3
final class IndexPrecomputeConsumerProfilesTest {
  @Test void immutableProfilesDeterministicAcrossConsumersAndWorkerSchedules() throws Exception {
    IndexPrecomputeConsumerProfilesOracle.main(new String[0]);
  }
}
