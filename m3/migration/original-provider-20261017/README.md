# Genuine original mapping validator qualification

The current candidate receives [Synexia PR #12200](https://github.com/hsoliwal/com.synexia/pull/12200), commit `f726d4ce761edf73b04ee2950cbd42790d2370b2`. Its exact 72-file SDK tree is `3c02e3896096753a63654d56650a4561118db2b4`; `source-binding.json` records the source and receiver identities.

Two earlier hosted jobs reached Java 21 compilation and failed because the recipe SDK omitted the donor's provided `org.jetbrains:annotations:24.1.0` dependency. This correction restores it while retaining `-Xlint:all` and `-Werror`. The current mapping fixture preserves every byte outside the migration value, including all 59 top-level mappings and the optional-module declaration. The reviewed migration delta preserves 54 records, explicitly reconciles two and adds four. The existing snapshot engine and workflow are unchanged.

Qualification requires ten SDK methods, including the original nine and a new test of actual recipe output preservation, 64 mutable states in both orders and fixed points, 22 custody negative controls, 38 provider boundaries, 26 original synthetic methods and genuine original CLI PRE/POST executions. Source reviews and 71 nonself pins are clear. Corrected candidate execution and formal autoreview remain **NOT_RUN**. See `annotations-classpath-correction.json` for the actual baseline failures and current source-only limits.

The 60-target recipe preserves 54 current files, prepares one migration-value-only successor and adds five provider/workflow files. The production `m3/docs/name-mapping.json` remains at its current preimage. The original CLI, schema, requirements, licenses and Java/JNI/runtime owners are preserved. After qualification, receive exact emitted output through the recipe.

The baseline must expose its existing M3TQ/missing-recipe errors; POST validate/report must succeed, while complete must retain exit 2 and all 158 completion blockers. Six pinned whole wheels total 642142 bytes. Native provider, payload and Draft 2020-12 checks must execute. The existing 15-minute hosted lane requires preinstalled JDK 21/CPython 3.12, a 512 MiB aggregate output limit and 8 GiB reserve.

Full twelve-pass migration, Routes A/B/C, complete semantic inventory, precompute/regex refinement and heap/native/mapped retention remain ACTIVE. Local storage protection and bulk-output HOLD remain ACTIVE. This source handoff does not establish Java/JNI, complete-JDK, performance or phase acceptance.

Historical evidence remains intact: initial run 39029940546 failed workflow validation before creating jobs; the canonical workflow-context correction is recorded in `workflow-context-correction.json`. Subsequent runs 39030437080 and 39030440322 executed and exposed the annotation-classpath failure. Earlier source-review records describe their own historical candidates.

Copyright 2026 Hitesh Soliwal and contributors. Independent Synexia recipe tooling retains Apache-2.0; embedded fixtures and generated target code retain their original notices and licenses.
