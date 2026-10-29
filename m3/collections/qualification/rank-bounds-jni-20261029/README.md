<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 original long-rank JNI fused-bounds result — 2026-10-29

**M3JDK21 product source, not a layer on top of another JDK.** This product PR receives the exact Java/C/test/CI postimages from canonical [Synexia #13224](https://github.com/hsoliwal/com.synexia/pull/13224), source-sealed convergence commit `2cf9a60042ac913723010b5efed41b188af8911a`. No Synexia recipe or runtime engine enters product code.

Base master for this branch: `7f7accdd54e298d1febbdb70083e4272f25e9a83`. Every existing owner matched the canonical preimage before receipt. Existing lowerBounds0 (JNI + Java) remains unchanged; existing m3rankbatch C library gets one additional fused bounds symbol.

## Exact original-owner custody

| Product Java/C/CI result | Exact Git blob |
| --- | --- |
| `M3LongRankBatchNative.java` | `d100fe7c269ec40ed8cdcac1d9f7985422f0ca54` |
| `M3LongRankIndex.java` | `acb4ab5ee856de814ec5dab5c134b9bf27b15f37` |
| `m3_long_rank_batch_jni.c` | `52b2d518351ceabd87499c840d06d23d79e72439` |
| `M3LongRankBoundsContract.java` | `bcd12bca84aa58879536be2e3dd92e332af17b41` |
| `M3LongRankBoundsTest.java` | `d52d3fc06ad65ab9861233c5142a139e3298caad` |
| `.github/workflows/m3-long-rank-bounds-jni-java21.yml` | `09323ef84608473be09cddf5f387b557bb052279` |

Original M3LongRankIndex.java dual-license header (Apache-2.0 OR EPL-2.0) is retained. Native and helper owners remain Apache-2.0. Original donor code from LeetCode/HackerRank/GFG was not copied; only algorithm categories/specifications were evaluated. Synexia's catalogue records LeetCode #34 (duplicate lower/upper range), #35 (insertion position), #704 (membership), GFG lower/upper bound and rejected HackerRank Pairs donor.

## Primitive storage and contract

The immutable rank snapshot exposes `lowerUpperBounds(long[])` producing exactly `[lo0,hi0,lo1,hi1,...]` and `fillLowerUpperBounds(long[],int[])` for a caller-provided exact-sized pair lane. `count(q)=hi-lo`; signed Long.MIN_VALUE/Long.MAX_VALUE and absent values do not overflow. Java fallback retains the inherited 64-query JNI threshold and works when the opt-in library is absent; large opted-in batches use one JNI transition for both bounds. All long[] inputs are read-only; JNI input copies or pins are released with JNI_ABORT. No per-query entries/objects, callbacks, Java GC addresses, or hidden native memory owners.

## Actually executed local evidence

Strict OpenJDK 21.0.12 source compilation with `javac --release 21 -Xlint:all -Werror`: PASS. GCC C17 `-Wall -Wextra -Werror -fsanitize=undefined -fno-sanitize-recover=undefined` and generated native JNI header: PASS. Both original and added JNI export symbols observed via `nm -D`. Deterministic 56,973-check differential: PASS in Java default, opt-in with missing native library, actual native with `-Xcheck:jni`, and actual native with `-Xint -Xcheck:jni`. The test explicitly requires JNI in real-native modes so fallback cannot be mistaken for native success.

## Qualification and limits

The focused exact-head CI workflow compiles original Java owners and C17 native code, checks SHA-1 Git source blobs, runs actual JNI and Java fallback in four modes, and separately attempts original `com.m3.arrays` and `com.m3.collections` named-module source compilation. These current-head jobs have **not yet passed** until their GitHub Actions results are observed. The new JUnit wrapper also needs the full original collection Maven/JUnit suite. Canonical actual Java/Text OpenRewrite SDK source-replay/fixed-point tests are pending in Synexia.

No benchmark was executed: one JNI transition per eligible batch is structural, not proof of faster throughput or lower RAM use. JNI GetArrayElements may copy. Keep the original small-batch Java path until measured break-even.

**DRAFT / HOLD:** Full Maven/JUnit, original native suite, installed built-JDK runtime, jtreg/JCK, Windows/macOS, release and performance qualification remain pending. No protected-branch write, no merge, no bypass.

## 2026-10-29 successor: native fused-bounds capability negotiation

The prior new fused JNI implementation could load an **older** original
`libm3rankbatch` that exported `lowerBounds0` but not `lowerUpperBounds0`.
The existing `nativeAvailable()` meant the old library was loaded, *not*
that the fused ABI was present. Calling an eligible batch of >=64 queries
against that old native deployment would throw `UnsatisfiedLinkError`.

Canonical reviewed Synexia recipe: [#13224](https://github.com/hsoliwal/com.synexia/pull/13224)
commit `5e183c0b488b4964112dee41fd13432b62185c5e`.
Java recipe `com.synexia.m3.Jdk21FusedRankAbiCapabilityJava20261029`,
native/CI recipe `com.synexia.m3.Jdk21FusedRankAbiCapabilityNative20261029`
reuse the existing Java/Text hash-pinned OpenRewrite engines with exact
preimages. No recipe engine is present in the product branch.

The original Java helper now negotiates `capabilities0()` bit 0 **once** on
class initialization after loading the old library. Missing capability
symbol returns false without breaking the **unchanged** original
`lowerBounds0` JNI entry. `fusedNativeAvailable()` separately reports
the new feature; the fused query path uses Java bisection if an older native
library is loaded. When the library claims a fused capability but omits the
implementation, unresolved linkage remains a qualification failure rather
than silently hiding an inconsistent native binary.

The original C library adds one constant-returning JNI capability symbol
and a **test-only** `M3_RANK_LEGACY_ABI_TEST` compile mode omitting new
symbols. The original `lowerBounds0` definition is unchanged.
The existing exact-head Java21/JNI workflow now compiles both current and
legacy-mode C17 libraries from the same original source, verifies symbol
presence/absence with `nm -D`, and executes all original signed-long
differentials in six modes (Java, opted-in missing native, legacy JIT,
legacy interpreter, current fused JIT, current fused interpreter).
Required-native test modes demand `fusedNativeAvailable()`, while legacy
mode confirms original `nativeAvailable()` is true with fused false.
Existing deterministic assertion count stays 56,973 per product mode.

Exact received Git blobs: `M3LongRankBatchNative.java`
`dd39cdbc0cbea5b44b0755837844aa068c3902f4`;
`M3LongRankBoundsContract.java`
`6f009b204147d86a560599cce959f84822e53287`;
`m3_long_rank_batch_jni.c`
`3596c0a98e615ae4ce1d4902d769ba7cef42af4f`;
workflow `926efcbad9bc923a2bb378ff00324653f9212add`.
These were received **byte-for-byte** from Synexia; no old JDK source
deleted and no original Java/JNI method signature changed.

**Executed locally in a deliberately isolated Java21/JNI ABI fixture:**
GCC C17 -Wall -Wextra -Werror -fsanitize=undefined; Java21 javac
--release 21 -Xlint:all -Werror; 7,416 signed-long bound checks per
Java, native JIT, legacy native -Xint, and current native -Xint mode.
All passed using -Xcheck:jni for native runs. This fixture is not a
substitute for compiling original complete product Java/C owners.

**DRAFT/HOLD:** The product's exact-head six-mode CI, actual Synexia
OpenRewrite SDK, full Maven/JUnit/named-module JNI, Windows/macOS, built
M3JDK21 image, jtreg/JCK and performance gates remain unverified. JNI is
an optional batch accelerator only; do not infer a benchmark speedup.
