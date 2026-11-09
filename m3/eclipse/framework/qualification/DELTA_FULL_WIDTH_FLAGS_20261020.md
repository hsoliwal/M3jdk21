<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21-native Eclipse Resource deltas: full-width physical flag lanes

M3JDK21 is the native SDK product, not an M3+JDK wrapper. The original Eclipse public `IResourceDelta` authority is preserved. This change refines the existing `M3EclipseResourceDeltaSnapshot` object rather than adding a new resource graph.

## Three-stage recipe source custody
1. Original eight-file framework source receipt: `m3/eclipse/framework/recipe/src/main/resources/com/synexia/rewrite/hash-pinned-java/eclipse-framework-m3-index-forward`. `M3EclipseResourceDeltaSnapshot` originally SHA-256 `1ce2e7fac96924f307ab150a21e93303d2d0f1ecd422c68d6ab20b35039b276c`.
2. Existing identity-set successor: delta snapshot became SHA-256 `21a893d7f976e5bf2d42998335d99556edfffb266016cce15d5784ebc1c19274`, and additional `M3EclipseDeltaIdentitySetTest.java` SHA-256 `44192b4116f99413f62dc5bed280d065de8bab0efffdd5441af628d43ade51da`.
3. New canonical Synexia PR **#12586**, recipe `com.synexia.rewrite.M3EclipseFullDeltaFlags20261020`, has exact two existing-file preimage/postimage seals. Delta production: before `21a893d7f976e5bf2d42998335d99556edfffb266016cce15d5784ebc1c19274`, after `e8b21233209c42233f3aa9cbeafb3debdfbd8df25585c48007311596e421d1cd`. Existing test: before `2e5a91ddb11677caf49bf344f1d5ac733408803339aed8b9e7f631b48260100f`, after `8c050e3fde8c9a5012d6caa26b7dfb03227eaab666e8cef76877c765f1ce16d1`.

The new `M3LongArrayList flags` stores unsigned-in-long 32-bit Eclipse flags per retained original physical BFS coordinate. `capturedFlags` and `selectByFlags` provide high-bit exact required/excluded filtering, capture-time semantics, original reference identity and immutable result. No second payload cache, map Entry row objects, JNI Java callbacks or new filesystem authority.

## Acceptance status
Source and tests are generated from the canonical recipe and reviewable as this PR. The updated existing GitHub workflow compares each stage's source hashes; runs original and new real OpenRewrite/JUnit recipe harnesses; compiles full M3 collections; and executes the real Eclipse framework JUnit tests, expecting 11 tests (6 snapshot tests, 4 identity tests, 1 activator test). A *workflow configuration* is not proof of a passing run. Product/full OSGi, JNI/matched HotSpot and performance evidence remain pending.

Java21 test command: `mvn -B -ntp -f m3/eclipse/framework/pom.xml verify`; recipe commands: `mvn -B -ntp -f m3/eclipse/framework/recipe/pom.xml test` and `mvn -B -ntp -f m3/eclipse/framework/recipe/flags-20261020/pom.xml test`.

Donor/algorithm category: single-pass primitive array masks, branch-free bit operations where lawful; no LeetCode/HackerRank/GeeksforGeeks source copied. No new JNI path is needed here.
