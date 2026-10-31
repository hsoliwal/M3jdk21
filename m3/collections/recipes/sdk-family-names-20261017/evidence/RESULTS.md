# Execution evidence — 17 October 2026

## Actually executed locally

- Existing real Synexia source-seal kernel: **1,236 checks**, 32 fixed-point input orderings,
  **13 exact generated files**. Missing/drifted/duplicate source, corrupt resources,
  cancellation and late edits are refused. The harness is not the OpenRewrite scheduler.
- Actual Java21 binding/query implementation: **137 checks** at both Xmx32m and Xmx128m,
  five correctly spelled library families, 22 named factories, defensive immutable metadata,
  malformed/duplicate/oversized/invalid-UTF8 resource refusal, locale independence, cancellation, and distinct overload descriptors.
- JDK compiler tree audit: **all 18 original public factory method trees unchanged**;
  all 22 names, erased return and parameter type names mapped. All new Java sources parse.
  **Parsing is not donor-resolved compilation or type attribution.**
- Actual unchanged M3LongIndexProjection and M3IndexMaskCompactor with actual unchanged C17 JNI:
  **167,145 checks**, 4,096 deterministic cases at each of Xmx32m/Xmx128m under -Xcheck:jni.
  Java/missing-native run: **167,146 checks**, including refusal of native-required empty input.
  Repeated runs are the same finite corpus, not additive independent proofs.
- Two negative controls detected: a wrong factory name and a separately compiled native
  high-bit-mask corruption. The first qualification attempt stopped because the native
  negative witness raised IndexOutOfBoundsException before the intended assertion. The
  test was tightened with a pre-access cardinality assertion and rerun. No product output
  was promoted from the failed run; initial-failure.log is retained.
- Generated source/product/resource bytes reproduce exactly. No existing M3 core/native
  source is modified. No native speedup or throughput benchmark is claimed.

Commands use javac --release 21 -proc:none -Xlint:all -Werror; C17 -Wall -Wextra -Werror
-Wpedantic -fsanitize=undefined -fno-sanitize-recover=undefined; actual JNI with -Xcheck:jni.

## Authored, NOT_RUN locally

Maven/OpenRewrite SDK (four tests), actual Apache/Guava/Eclipse/Spring dependency-resolved
compilation, seven new real-donor JUnit methods, original interop regression suite, full
core build and installed SDK/JDK integration. Maven is not installed and an outbound
repository request failed DNS in this container. The workflow and POM are runnable gates,
not evidence of success. No donor-runtime stubs were used to claim these tests passed.

The added source composes real donor APIs. Until its external-runtime tests execute, the
new adapters remain draft. Full library API superset, concurrency/serialization/primitive
closure, all allocating-return M3 ownership, full JDK jtreg/platform/GC tests, earlier
web/Nebula/RWT/GWT conversion and comparative performance selection remain open.
