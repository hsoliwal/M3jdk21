<!-- SPDX-License-Identifier: Apache-2.0 -->
# Current-head source-sealed build grammar repair

Authoritative Synexia text recipe `com.synexia.m3.Jdk21StrictIndexedReadOnlySourcepath20261018` mirrors the exact first-head M3JDK21 workflow source from public [#1260](https://github.com/hsoliwal/M3jdk21/pull/1260), replacing only the `javac -sourcepath` value.

Java package `com.m3.collections` sources live under module root `m3/collections/src/main/java`. The original invocation used the leaf `.../com/m3/collections` as the sourcepath, preventing javac's package-relative discovery of transitive source owners (`M3IndexMaskWindow`, `M3SignalOrder`, etc.). Preserve explicit pinned product source/test inputs, `--release 21`, `-Werror`, and all other Java21/JNI/JUnit steps unchanged.

This is **not** permission to patch Java runtime outputs or weaken tests. Exact source hash, postimage hash, OpenRewrite text fixed point and drift refusal are mandatory. No third-party source copied. `m3/collections/pom.xml` and source-vendored JUnit coordinates remain unchanged. The workflow must be tested at its exact updated head.
