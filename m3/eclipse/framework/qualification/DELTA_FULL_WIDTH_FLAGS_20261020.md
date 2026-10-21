<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21-native Eclipse Resource deltas: full-width physical flag lanes

M3JDK21 is the native SDK product, not an M3+JDK wrapper. The original Eclipse public `IResourceDelta` authority is preserved. This change refines the existing `M3EclipseResourceDeltaSnapshot` object rather than adding a new resource graph.

## Three-stage recipe source custody
1. Original eight-file framework source receipt: `m3/eclipse/framework/recipe/src/main/resources/com/synexia/rewrite/hash-pinned-java/eclipse-framework-m3-index-forward`. `M3EclipseResourceDeltaSnapshot` originally SHA-256 `3e499ab33152d8a79c3171ce96a20ae35968c8e573e1ac48b999ede35a7581ec`.
2. Existing identity-set successor: delta snapshot became SHA-256 `94fc5f6ac7d87439758e8dc6544754e0a77ebbff52d36bbb8ae18a6447aa555c`, and additional `M3EclipseDeltaIdentitySetTest.java` SHA-256 `2805f91b08f95e42f3fb9e0e6390de3174e5bf71fc761bc09210bb9ca9b6d781`.
3. New canonical Synexia PR **#12586**, recipe `com.synexia.rewrite.M3EclipseFullDeltaFlags20261020`, has exact two existing-file preimage/postimage seals. Delta production: before `94fc5f6ac7d87439758e8dc6544754e0a77ebbff52d36bbb8ae18a6447aa555c`, after `a30151abcd75fb020109ba9e7da0c2f335eceaa2079ce9c78a81526d423d645a`. Existing test: before `cafda481d205e514d3ceb2187f2219301379b7075113e7b6e72946b012384a51`, after `d12864a200f0d345aa54b52470560ddb8e587a78a1ec2f91917e27ddcbdff957`.

The new `M3LongArrayList flags` stores unsigned-in-long 32-bit Eclipse flags per retained original physical BFS coordinate. `capturedFlags` and `selectByFlags` provide high-bit exact required/excluded filtering, capture-time semantics, original reference identity and immutable result. No second payload cache, map Entry row objects, JNI Java callbacks or new filesystem authority.

## Acceptance status
Source and tests are generated from the canonical recipe and reviewable as this PR. The updated existing GitHub workflow compares each stage's source hashes; runs original and new real OpenRewrite/JUnit recipe harnesses; compiles full M3 collections; and executes the real Eclipse framework JUnit tests, expecting 11 tests (6 snapshot tests, 4 identity tests, 1 activator test). A *workflow configuration* is not proof of a passing run. Product/full OSGi, JNI/matched HotSpot and performance evidence remain pending.

Java21 test command: `mvn -B -ntp -f m3/eclipse/framework/pom.xml verify`; recipe commands: `mvn -B -ntp -f m3/eclipse/framework/recipe/pom.xml test` and `mvn -B -ntp -f m3/eclipse/framework/recipe/flags-20261020/pom.xml test`.

Donor/algorithm category: single-pass primitive array masks, branch-free bit operations where lawful; no LeetCode/HackerRank/GeeksforGeeks source copied. No new JNI path is needed here.
