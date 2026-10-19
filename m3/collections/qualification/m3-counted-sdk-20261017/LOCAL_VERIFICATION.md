<!-- SPDX-License-Identifier: Apache-2.0 -->
# Local verification receipt — limits stated explicitly

Java runtime: OpenJDK 21.0.12.1, strict `javac --release 21 -Xlint:all -Werror`.

- `M3CountedBagSelfTest` succeeded: 165184 deterministic assertions against `LinkedHashMap` counts; 5300 randomized mutations, exact long totals, null keys, failure cases, reusable index-sorted snapshot and one-evaluation filtering.
- Source-sealed `M3CountedSdkRecipe --selftest` succeeded: six template hashes matched; missing/drift refusal, repeated application, fixed point.
- `M3GuavaMultiset` and `M3ApacheMultiSet` passed strict compile against `local-stubs/` **only** because actual provider jars could not be fetched locally. Those stubs are NOT product code, NOT provider compatibility proof and are NOT included in the GitHub PR. The GitHub CI job is required to compile against the real Maven dependencies and run `M3CountedProviderContractTest`.
- The core test used a compile-only stub for existing `M3LinkedHashMap` and `M3IndexMaskCompactor`; therefore actual M3 core dependency, JNI, Maven and installed-JDK integration remain pending.
- No CPU/memory speedup claimed. No JDK, external API provider, or whole SDK parity claimed.
