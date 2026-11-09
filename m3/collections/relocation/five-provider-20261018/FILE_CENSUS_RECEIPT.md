<!-- SPDX-License-Identifier: Apache-2.0 -->
# Canonical Synexia five-provider inventory receipt
- Source: https://github.com/hsoliwal/com.synexia/pull/12293
- Canonical commit: `2f7e3ac2849bdf3e3456be5be76a5d30e5be7cd4`
- Source-file inventory: **3214 Java Git blobs** / 12 pinned recursive source/test/generator roots / 5 providers.
- The five inventory TSV Git blobs in M3JDK21 are identical to those in Synexia.
- Existing five named OpenRewrite source-package recipes, tests and provenance remain unchanged.
- This M3JDK21 receiver commit **does not modify production Java, JNI, OpenJDK public API or donor source files**.
- Apply M3 atom transformations only after source-file full reads, recipe execution and ordered proof: diff → linter → compile → tests → runtime, followed by serial source handoff.
- Guava, Eclipse and Apache are writable forks; Spring is upstream read-only; OpenJDK java.base must not be package-renamed in place.
- No repository-wide source changes, JNI or performance improvements are claimed by this census receipt.
