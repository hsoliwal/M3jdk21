<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 regex workspace reuse receiver — draft

This receiver carries a source-sealed Java 21 jtreg compatibility gate, **not** a claim of completed java.base/HotSpot/JNI runtime optimization.

- Canonical Synexia Java/JNI donor and algorithm catalogue: https://github.com/hsoliwal/com.synexia/pull/11308
- Canonical Synexia recipe owner: `M3HashPinnedJavaSnapshotRecipe`, crate `m3jdk21-regex-workspace-20261014`; receipt source branch commit `848d1cdafb55e0e9ee1ca0a36640153ca3f7e253`.
- Exact receiver test source Git blob: `23ba01162600811c9e687d932939de4530d1839e` (same blob as canonical recipe postimage).
- Target master before checkout: `f9b3a0ce84dcdbd0cc75df956def7710ac5d04fd`. This file is receiver-side status and does not create a new recipe authority.
- Product runtime ownership: `java.lang.String` backed by **M3 String**; reuse and precompute layers internal to M3JDK21, without a Synexia runtime dependency.
- OpenJDK `Pattern`/`Matcher` API and existing M3TQ facts caching remain canonical target behavior. Mutable Matcher identities are not globally pooled; each operation retains exclusive mutable state.

## Verified host-JDK qualification

OpenJDK 21 `javac --release 21 -Xlint:all -Werror` compiled the receiver contract, then `java -Xcheck:jni` and `java -Xint -Xcheck:jni` each passed **16,065** matcher/region/capture observations. This confirms only host-JDK reference behavior. The source tests Unicode, isolated surrogate code units, lazy and greedy forms, named/backreference captures, regions and boundary flags, zero-width results and independent concurrent requests.

## Promotion gates (not yet executed)

The **rebuilt M3JDK21** must run:
```bash
make run-test TEST=test/jdk/java/util/regex/M3RegexWorkspaceContractTest.java
make run-test TEST=test/jdk/java/util/regex/M3RegexReuseTest.java
```

Also require CI Maven/OpenRewrite recipe replay and fixed-point/refusal; C++ JNI compiled/sanitized prepared-program tests; M3 String native composite mode, compiled HotSpot tests, JMH allocation/GC results and Windows/ARM compatibility. A host JDK check is **not** evidence of those tests passing.

The target jtreg source retains its GPLv2-with-Classpath-Exception header. Canonical Synexia recipe metadata stays Apache-2.0 and all donor licenses/attribution remain intact. No challenge-platform solutions or third-party source are copied.
