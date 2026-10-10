<!-- SPDX-License-Identifier: Apache-2.0 -->
# File-read snapshot successor

Copyright 2026 Hitesh Soliwal and contributors.

This stacked intake follows PR #574 at `63b67b8cf1bfeba8032de5d7d4300a748bc1ef52` and
applies the exact Synexia recipe `com.synexia.m3.RegexV7FileRead20261009` from `b37358882b0f25afa9eb9b6cefdfdb15880da332`.
Three file readers reuse their private `Files.readAllBytes` arrays through the
ByteBuffer overload. Caller-owned byte-array decoders retain their snapshots.

The reusable recipe, whole-file matcher closure, protected JUnit regressions,
negative controls, donor notices and failure history stay in Synexia. This target
retains only the existing vendor owner and thin receipts. Public JDK names,
canonical M3 ownership and the existing nested-snapshot improvement survive.

The bound donor proof passes four SDK tests, 29 runtime tests per variant,
427,008 JDK comparisons per variant and two required Linux JNI tests with
`-Xcheck:jni` and explicit minimumRows=1. Five alternating interpreter trials
measure allocation separately. These are donor proof results over byte-identical
codec code, not a receiving JDK build or new native provider adoption.

Full ContextOS ProgramTest closure, canonical serial/reactor review,
java.base cold/runtime split, matched JDK/jtreg, GPU and remaining platform gates
remain open. **NOT_ADMITTED**. Exact pre/post hashes, source artifact root and
source commit are recorded in `qualification.json`.
