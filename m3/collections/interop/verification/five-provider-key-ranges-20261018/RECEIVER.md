<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# Five-provider indexed range receiving evidence

Canonical owner: `hsoliwal/com.synexia`, task
`synexia-openrewrite-recipes/recipes/m3jdk21-five-provider-key-ranges-20261018`.
Product owner: `hsoliwal/M3jdk21`, `m3/collections/interop` optional vendor module.
Java 21 and real API contracts: JDK, Guava, Eclipse Collections, Spring, Apache.

The two Java postimages are exact copies of canonical source-sealed OpenRewrite outputs.
The mirror is only for target qualification and is not a competing recipe authority.
Source SHA-256 and ABSENT preimage gates are in the recipe manifest. The source-first
commit precedes the generated postimages. Native mask operations reuse unchanged
`M3IndexMaskWindow` and `m3_index_mask_window.c`; no new native ABI.

Admission stages: canonical recipe JUnit generation/refusal/fixed point; exact receiver
bytes; strict Java 21 M3 core; real five-provider Maven/JUnit; C17 checked JNI; CPU and
retained-heap benchmarks; installed-JDK/jtreg/JCK, concurrent semantics and multi-OS.
Missing or queued CI is not green. This is not all-five-provider full API completeness.
Preserve String -> arrays -> collections -> AST/compiler promotion order.
