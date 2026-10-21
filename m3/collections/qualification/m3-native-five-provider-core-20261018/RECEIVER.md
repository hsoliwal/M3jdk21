<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 native five-family collection receiver

**Canonical source-first recipe:** [Synexia #12248](https://github.com/hsoliwal/com.synexia/pull/12248), commit `b6e0e1e49f16e0f88f15ee7cd03ca967f80d2864`. **Product receiver:** `hsoliwal/M3jdk21/m3/collections`, pinned `master@f29b0afe7ad253cc0562bc8dfdd48fc1acec710e`.

This **does not write to** `m3/collections/interop`, does not substitute third-party facades for native M3 types and does not copy Guava/Eclipse/Apache/Spring source. The independently maintained `m3/collections/interop` remains unchanged; the earlier merged source-admission PR #1212 is not counted as M3 native implementation.

One canonical SHA256-sealed recipe generates four product classes and one deterministic differential oracle: `M3IndexedBiMap`, `M3IndexedSetMultiMap`, `M3ListMultiValueMap`, `M3NativeFiveLibrarySdk` and `M3NativeFiveLibrarySelfTest`. The target branch receives the recipe/CI/catalogue first, source templates second. All five product/test files are Git-blob-identical to canonical templates in both repos.

**Local proof:** strict Java21 compilation and 1,653,019 random differential/metamorphic checks PASS **using test-only stand-ins** for incumbent M3 owners. Do not describe these as actual M3 core integration. Direct Java Maven-bound recipe SHA256/missing/drift/fixed-point PASS. Maven unavailable locally. CI requires the real M3 module, real owner instances and real C17 JNI checked runtime. No new C/JNI entrypoint.

**Provider disposition:** correctly named M3 analogue by capability, NOT full inherited provider interface equivalence, not JDK `java.util` replacement. String → arrays → collections → compiler receiving order preserved. Native, JDK image/JTReg/GC, full provider contract and benchmark gates pending; keep draft until proven.
