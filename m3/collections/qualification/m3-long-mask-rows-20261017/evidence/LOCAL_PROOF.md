<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 DOOP mask rows local proof — 2026-10-17

- Java 21 strict `--release 21 -Xlint:all,-module -Werror`: **PASSED**.
- 1,187 deterministic assertions under 16 MB and again under 32 MB: **PASSED**.
- Exact previous `M3LongDag`, `M3VirtualAccumulator`, `M3VirtualCollector`
  source was compiled; several other collection dependencies were represented
  by interface-compatible local test doubles. This is an **isolated test**.
- 250 seeded differential cases, unsigned byte masks, source offset slicing,
  defensive mask cloning, LongStream/virtual collector/sortedBounded bridge,
  cardinality drift rejection and 100,000-row stress: **PASSED**.
- **NOT_RUN:** full checkout `verify.sh`, actual native parity, OpenRewrite Maven
  SDK, full module/JUnit/lint, jtreg, other platforms and throughput benchmarks.
- Parent #1093's existing old owner guard for M3IndexMaskCompactor refers to
  older blob 240bb..., whereas the actual stacked receiver uses blob 958d...;
  this successor pins exact actual source without rewriting parent evidence.
- JNI checks within the contract execute NATIVE_REQUIRED only when the installed
  first-party JNI library is available; receiver `verify.sh` must compile and
  load that binary to prove JNI parity.

**PROMOTION HOLD** until canonical Synexia recipe PR #12100 and receiver gates pass.
