<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 source-time machine hint manufacturing — direct product extension

The JDK 21 annotation processor `SortableProcessor` is the existing javac source/model fact owner. Keep `M3CompilerFacts`, `@Sortable`, all existing generated APIs, source array metadata, and JNI ABI intact. Add compile-time projection of seven candidate-only `M3MachineHintRules` bits from existing 22 facts. Generated companions expose `machinePatternHints()` with **precomputed primitive masks**, not fresh runtime scanning or reflective discovery. A candidate is a review hint, never machine-code lowering authorization.

## Repair only two observed failed boundaries

GitHub Actions `M3JDK21 compiler fact validity columns Java21/JNI` on PR #2308 failed due to an existing `SortableProcessorSelfTest` assertion after the 24,980-coordinate contract passed. Root reproduction: javac trees synthesize `super()` (end position `-1`), and the source-call counter incorrectly includes it. Preserve original six structural token lanes; only type-local declared-source call counts/tokens exclude synthesized calls.

Maven exec plugin similarly fails to locate the processor through `System.getProperty("java.class.path")`; obtain the original `SortableProcessor` class CodeSource directory/JAR and include it on both processor and compiler classpaths. Do not replace the processor, introduce new annotation processing rounds, or weaken expected semantic checks.

## Source seal and gates

Existing `m3-compiler-signal-facts-20261022/SOURCE_MANIFEST.tsv` is immutable historical evidence. The newer source state must be admitted under this qualification's exact successor manifest with before/after blob and SHA-256, and its real `verify.sh` must execute strict compiler/Java runtime proof. JNI/HotSpot/JCK/jtreg remain separately unproven; do not merge this draft until hosted verification is green.
