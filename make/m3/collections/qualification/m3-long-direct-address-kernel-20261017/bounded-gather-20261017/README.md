<!-- SPDX-License-Identifier: Apache-2.0 -->
# Bounded gather receiver — PR #1092 continuation

Copyright 2026 Hitesh Soliwal and contributors. 2026-10-17.

The product remains M3JDK21. Synexia owns the reusable recipe recorded in
RECIPE_PROVENANCE.json. Its actual Maven/OpenRewrite results refine this existing
native kernel and matching receiver templates; no manually edited target body or
new collection/string/payload hierarchy is introduced. Original Java owners and
original test remain byte-identical. Four existing files change (native, template,
workflow, workflow template); two generated Java tests are added.

## Array / offset / mask model retained

    value = payload[addresses[viewOffset + logicalOrdinal]]

Filtering compacts the ordered physical IDs using the existing unsigned-byte mask
owner. Stable sort permutes IDs, not payload. Nested slices compose offsets over
immutable arrays. The new regression uses those exact current owners, every mask,
duplicates, signed-long extremes and nonzero address/output offsets. It does not
claim that detached projections are live collections or replace java.util.

## Native refinement

The original pins three arrays for the full batch. The new code uses 256-index
chunks with 3072 bytes fixed own stack scratch. Contiguous forward IDs read via
GetLongArrayRegion. Other chunks hold only the payload pointer for <=256 gather
iterations; output is published after release. No pointer survives a chunk/call.
Duplicate/order and valid-prefix-before-invalid-index behavior are preserved.
Caller arrays must remain stable. VM copies, resource-exhaustion timing and GC pause
bounds are not proven; this is a bound on own scratch and critical-loop work.

## Actual checks

- Six real OpenRewrite/JUnit tests; 128 partial/order schedules; 70 cancellation
  checkpoints; exact named generation; six Results/four KEEP; no-change replay.
- Strict Java21 compilation and actual Maven discovery of the new JUnit method in
  Java-only and native-required processes: zero failures/errors/skips.
- Java-only finite corpus: 11020139 checks; native-enabled corpus: 21975732 checks.
  Exact original and new C produce identical native fingerprint -8730037866241443045.
- Original regression remains: 321298 Java-only /384942 native checks.
- Both C versions compiled with C17 warnings-as-errors, generated JNI headers and
  fatal UBSan. Real -Xcheck:jni runs require the native library, not fallback.
- Separate 48-case JNI function-table observer measures old max pins=3 and new=1,
  max region=256. It is instrumented transport, not a JVM/GC substitute.
- An independent complete portable rerun and separate source emission agree.

The hosted workflow now selects M3LongAddressBoundedTest as well as the original
JUnit adapter and requires -Dm3.address.required=true in its JNI stage. Workflow
configuration is not proof that hosted execution ran. The earlier receiver source
is unchanged; its two updated template bodies exactly equal installed outputs.
Its own Maven self-test/check, full collection module, source-built JDK/jtreg/JCK,
all collectors/platforms and live SWT/OSGi/pods remain separate NOT_RUN gates.

## Performance boundary

Exploratory medians of three fresh processes: 200 gathers of 262144 values after
warmup, same machine and fatal UBSan builds for old/new native code (milliseconds):

| Shape | Java | Old JNI | Bounded JNI |
| --- | ---: | ---: | ---: |
| Contiguous | 23.814 | 118.435 | 104.350 |
| Reverse | 23.265 | 122.087 | 175.558 |
| Strided | 49.571 | 129.129 | 213.438 |

The finer pin bound has irregular-gather overhead. Java is faster in these samples
and remains the unchanged default. These are diagnostics, not JMH or generalized
speedup/latency guarantees. Native pointer access may trigger VM-internal copies.
No automatic per-element native dispatch or programmer memory-budget work added.

## Reproduction and evidence

The canonical pinned task contains the exact preimages, postimages, source-changing
recipe, all six SDK tests and a portable verifier. Its command takes an approved
Maven3.9.9/offline repository, dependency manifest, canonical engine source root and
fresh output directory. All759 dependency hashes were verified. It emits actual
SDK Results before compiling targets; it never writes over a product checkout.
The accompanying conversation packet retains full commands, compiler/JUnit/native
logs, protocol source, diagnostic source, binaries' hashes and a review patch.
Keep this PR draft until its wider receiving gates independently pass.
