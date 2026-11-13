<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
# M3 Collections SDK cross-library name receiver

Canonical source/recipe: [Synexia PR #12113](https://github.com/hsoliwal/com.synexia/pull/12113), commit `19509a03e46d4a0ad33cadd08c40bc9b0d86e9e4` based on append-only `develop@892e3f6c51d78bc0948e8a8cf736055b4c70eafa`. Public receiver baseline `M3jdk21 master@4e29dc9d4141261e8763fbe9ffd6a20e017b1aff`.

This is an **SDK family catalogue and facade** receiver, not a package-rename of Guava/Eclipse/Spring/Apache classes or a new collection implementation tree. Existing `M3Collections`, JDK `java.util`, HotSpot/JNI and original source owners are unchanged. Source/scope/legal and String → arrays → collections ordered phases remain as in `AGENTS.md`.

Producer: canonical `M3CollectionSdkNamesRecipe` validates SHA-256 and sorted 104-name catalogue before generating four postimages. The receiver mirror is source-equivalent to Synexia. Two ordered target commits: recipe + proof metadata first; generated source/tests second. If generated code is incorrect, edit original canonical recipe/templates and re-run; never manually patch the output.

Source artifacts are first-party Apache-2.0. Original vendor class/package names are **only external vocabulary** and donor references; no Guava/Eclipse/Spring/Apache runtime dependencies or borrowed code. `M3_BACKEND_ONLY` and `M3_VIEW_ONLY` rows are evidence of a separate M3 structure/view, **not** proof of full source/binary/behavior compatibility. `PENDING` names identify unimplemented roles.

Local receipts: strict Java21 catalogue oracle 521 checks; Maven generator direct-Java selftest fixed-point/missing/drift and four output hash checks. Maven unavailable locally. GitHub CI must run real Maven, Java/JNI existing-kernel parity and full `m3/collections` module tests. JDK/jtreg/JCK, installed-image, varied JNI platforms, memory/perf benchmarks and contract-by-contract external library compatibility are NOT_RUN/NOT_PROVEN. Keep PR DRAFT.
