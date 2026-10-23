<!-- SPDX-License-Identifier: Apache-2.0 -->
# Original M3JDK21 five-library interop test-source syntax receiving

This M3JDK21 product receiver retains the original first-party
`M3FiveLibraryIndexViews`, its dynamically indexed arrays, masks and
logical-to-physical offsets. No sidecar M3+JDK framework or alternate
collection implementation is introduced.

The October 22 GitHub interop workflow got beyond the earlier census,
recipe and core module gates but failed Java 21 test compilation:

`M3FiveLibraryArrayificationTest.java:56: 'var' is not allowed in a compound declaration`

The canonical Synexia Maven/OpenRewrite recipe generates one exact
Java source postimage, replacing that **single declaration** with two
consecutive local `var` declarations. All original five-vendor API
differential assertions remain present and unchanged. No production
Java/JNI source, API name, method, native symbol or license changed.

The original full interop test and JDK/vendor/JNI CI gates remain
authoritative. The targeted syntax workflow separately verifies Java 21
rejects the old grammar and admits the corrected grammar, and compares
full original/result source bytes after exactly one statement split.
Source custody is pinned in `source-handoff.tsv`; canonical
Maven/OpenRewrite source and tests belong only in Synexia.

This source is **not qualified** until exact-head product build,
full provider JUnit, canonical Synexia fixed-point/refusal, and applicable
JNI checks have passed. An attached/queued workflow is not proof.
