# M3JDK21 source-bound cursor import repair

Status: candidate-only, proof-gated, not a completed backport or JDK release.

## Origin

JDK image builds for multiple backport lanes failed compiling
`src/java.base/share/classes/java/lang/M3StringSearchPrecompute.java`:
`CursorCharSequence` invokes `Objects.checkIndex` and
`Objects.checkFromToIndex` without importing `java.util.Objects`.

The exact imported postimage is the reusable canonical Synexia OpenRewrite Java-LST recipe
`com.synexia.rewrite.M3Jdk21StringCursorObjectsRecipe`, PR #11559.

## Mechanical delta

Only `import java.util.Objects;` is added after `import java.util.Arrays;`.
No method, API, JNI entry point, parser, string atom, regex plan or internal behavior is changed.

The target source is byte-equivalent to the Synexia recipe template, Git blob
`0a88ed559b23def5d2229bba3316ea2268b4d720`.

The existing copyright header and GPLv2 with Classpath Exception designation remain intact.

## Proof sequence

1. Canonical Synexia Maven/OpenRewrite recipe JUnit and fixed-point proof.
2. Independent receiver SHA-256 and exact-one-import verification.
3. Original OpenJDK configure and `make images` with Java 21 boot JDK.
4. Focused String/indexOf/UTF-16 runtime smoke.
5. Follow-on jtreg and overall backport DAG proof; do not mark release complete here.

This packet neither approves JDK21 compatibility nor promotes itself. Keep original history,
apply only by reviewed PR and retain all unrelated build failures as explicit residue.
