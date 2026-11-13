<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Range-set behavioral atom receiver

This is an additive, code-bearing extension of existing M3JDK21 PR #2170, not a new fork or a replacement for ordinary JDK APIs.

Canonical source-sealed OpenRewrite recipe: [Synexia PR #14952, commit d2eb04180acad6](https://github.com/hsoliwal/com.synexia/commit/d2eb04180acad6ba8d4b8c94864c7b1a31a8d9e6), crate `doop-key-value-range-20261102`, existing `M3HashPinnedJavaSnapshotRecipe` and `DoopKeyValueRecipeTest`.

- `remove`: use existing `M3LaneSearch.lowerBound` to locate the first start at or after an exclusive removal bound. Search O(log r), worst-case array relocation O(r) unchanged.
- `cardinality`: exact unsigned 64-bit accumulation over disjoint long intervals, materialize one final `BigInteger` (O(r) time, O(1) intermediate BigInteger allocations).
- `selectedByState`: identity-index mask selection in the Java backend scatters directly into endpoint arrays, with per-call mutation-consistency check. Existing `NATIVE_REQUIRED` JNI path, no-library refusal, and C selectors are unchanged.

Preimage SHA256 `9656b3a699f638dfe0a175a35546cc4d747f2e2e8caccfa8576f06d3929665cb`; source SHA256 `72d57ce6b78845712336d7a8c44642ee13b327d85d95ca9a89e632fec23dd285`; new Java test SHA256 `b2c6890df96006ef2038f44a4b1e27574dd00a25cebc497bbe5e2edb45037385`.

Isolated Java 21 `javac --release 21 -Xlint:all -Werror` and JIT/-Xint differentials pass 1,492,403 checks with identical original/candidate digest `13976038352403958269`. Three new JUnit method bodies compiled and ran against original and optimized Java owners. Bounded ThreadMXBean allocation diagnostic at 20,000 intervals: `cardinality` 5,119,104 -> 64 bytes/call; `selectedByState(JAVA)` 280,096 -> 160,064 bytes/call. These do NOT establish JNI throughput or JDK-wide performance.

**HOLD until native qualification:** This environment used a compiled test stub for the selector in the isolated runs, not the real JNI library. The new JUnit test calls the actual native path and compares results when available under the receiving module. Run required-native Maven, source-pin/custody and JDK image checks on CI before promoting this draft. No other runtime class, JNI C body, existing checkout or default branch is modified.
