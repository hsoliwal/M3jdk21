<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->

# M3 JNI export join v1: read-only, source-verified, not ABI promotion

Input authority is restricted to two *existing* inventories:

- `M3AtomCensus --jni` writes `JNI_DECLARATIONS_<repo>.tsv` from its single javac parse, with original native atom ID, UTF-16 span, SHA-256 and candidate symbol.
- `M3NativeJniAtomInventory` writes `target/m3-native-jni-atoms.csv` using its existing C/C++/JNI lexical inventory. JNI_OnLoad/Unload remain separately classified.
- `javac -h` generated `.h` sources are independent candidate-name and descriptor witnesses. Their exact SHA-256 is recorded; the header is *not* a runtime ABI attestation.

One new, standalone Java21 tool joins the two inventories; it does not parse Java or C/C++ source a second time. It strictly checks Java UTF-16 slices and whole native source SHA-256 values against approved roots, and requires exact candidate symbol and javac header descriptor matches. Overloaded declarations may only resolve when the parameter types admit an unambiguous JVM descriptor and a long-form `javac -h` symbol matches; simple unresolved object types are refused, never guessed. Orphan native exports, missing native candidates, ambiguous duplicate symbols, JNI lifecycle hooks and unresolved local/anonymous native classes stay explicitly non-admitted.

Statuses: `EXACT_EXPORT_CANDIDATE`, `JAVA_DECLARATION_UNRESOLVED`, `JAVA_DESCRIPTOR_UNRESOLVED`, `HEADER_UNCONFIRMED`, `NATIVE_EXPORT_NOT_OBSERVED`, `DUPLICATE_NATIVE_EXPORT`, `AMBIGUOUS_JAVA_SYMBOL`, `ORPHAN_NATIVE_EXPORT`, `JNI_LIFECYCLE_UNSCOPED`. Every status is **evidence classification only**; none proves RegisterNatives absence, native object linkage, calling conventions, platform ABI, compiler equivalence or JNI runtime correctness.

Output `JNI_EXPORT_JOIN.tsv` is canonically sorted and byte-identical on replay. Errors, source drift, invalid UTF-8, unsafe/symlinked paths, duplicated Java IDs, forged source digests, malformed CSV/header, or path aliases refuse **before publication**. Optional `--require-candidate-match` refuses any declaration/export lacking a unique source-verified header/native candidate, without claiming product admission.

Contracts locked by default: public Java SE APIs, original M3JDK tool CLI, Java/JNI native entrypoints, javac source semantics, and existing output files. The executable crate is additive, has no runtime classpath dependency, and requires a versioned Synexia Maven/OpenRewrite hash-pinned *ABSENT → postimage* recipe before public M3JDK receiver application. LeetCode, HackerRank and GeeksforGeeks are unrelated to this ABI reconciliation leaf, so no challenge/editorial code is copied. Broader category-first algorithm improvements remain owned by the existing donor catalogue.

Stages: inventory → contract → recipe fixture → Java21 lint/compile → self-tests → real javac -h and native C17/`-Xcheck:jni` → deterministic fixed point → Maven/OpenRewrite first apply/fixed point/refusal → M3JDK product/JTreg/JNI/ABI; downstream stages cannot inherit PASS from earlier ones.
