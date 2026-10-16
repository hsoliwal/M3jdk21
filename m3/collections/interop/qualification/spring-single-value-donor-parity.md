<!-- SPDX-License-Identifier: Apache-2.0 -->
# Pinned Spring null-view behavior

Canonical test-only correction: `hsoliwal/com.synexia@ce510939bde1be4d6f76f014ef6e92e81bbab05b`. The closure template now has SHA-256 `5ece9a369bcf5a223cb163ed0857534600fcd53bcc881f239956be15a4200f40`, Git blob `141661a24e962506a2f1f87106e7d7eab15e5d7c`. Previous preparation identities and the inference-repair receipt remain historical records.

Run `38763088183` at receiver head `75568ac5ed2eda080330e4406a196b60e0d8843a` passed all six recipe tests, generated five exact Java outputs, compiled the unchanged core and all three runtime units, and executed all 42 runtime tests. There was one failure: the test assumed independent Spring single-to-multivalue views containing a null scalar compare equal. Artifact `11987081157` was downloaded and its ZIP SHA-256 verified as `ad3dff06601f96b1393801e459d6104f24edec04fa17ef3de7316afcd6972789`.

Pinned source `spring-projects/spring-framework@v6.2.0/spring-core/src/main/java/org/springframework/util/SingleToMultiValueMapAdapter.java`, blob `f02bb4ee94290adbd492d539fd09db21cfd993d1`, returns null from get for a null scalar but a singleton-null list from entrySet. Its equality loop compares those different forms. Independent ordinary JDK-backed donor peers also compare unequal in that state. This adapter's hashCode delegates to the scalar backing map. The M3 candidate uses the unchanged donor adapter, preserving that behavior.

The revised test checks both equality directions and reflexivity against an independent JDK-backed peer, as well as scalar contents, ordered keys, values, entries, hash, text, null-value containment, get/getFirst/presence, and forEach. It retains all null inputs and all 42 tests. No production template changes or assertion-skipping workaround is used. A new successful exact-head run is required.

The `springSingleValueMap()` API is donor-version-specific, not a universal Map-equality replacement for null-containing data. This limitation does not apply automatically to the separately tested `springMultiValueMap()` map-of-lists implementation. Do not infer whole-library or installed-image compatibility from this narrow qualification.
