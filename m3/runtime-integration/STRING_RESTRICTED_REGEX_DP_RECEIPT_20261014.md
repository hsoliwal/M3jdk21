# M3 String restricted regex rolling-DP receiver — 2026-10-14

Status: DRAFT_CANDIDATE. Do not promote without exact M3JDK21 java.base/jtreg and recipe gates.

Canonical donor: hsoliwal/com.synexia, source develop at 9e975f81b9f937d9f2a4f86ef3e821f917d7938e, synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRestrictedRegex.java. First-party donor Apache-2.0. Master recipe and immutable receiver templates: Synexia draft PR #11284, com.synexia.m3.Jdk21RestrictedRegexDp. Target parent: hsoliwal/M3jdk21@a79b274f2cd2b4d0fe3865ca18b231f1850bf692 (PR #864, compound UTF-16 cursor).

Handoff and hashes:
- src/java.base/share/classes/java/lang/String.java: pre eed88205feda9f64b03515603e5807e3f83961ddeb99dae4ef84bd2034d6f568 -> post 3fe9a39bb6bd457deeabac26c35867a54ab30a64b0fdf39a102d7a43b3f2e624
- src/java.base/share/classes/java/lang/M3StringRegexDp.java: ABSENT -> 8c8ea7b0873a53a39db098ce9dd2bc49249c2d1bad58cced4acad711ac7cd940
- test/jdk/java/lang/String/M3StringRestrictedRegexDpTest.java: ABSENT -> 9f9bae0e529baccec68afe829f53397143ed638666031e9d3378c9107f3511e9
- src/java.base/share/classes/java/lang/M3String.java: UNCHANGED d4dcb3f3320e42a977001254cdd9ac58105cea0a427839ec81d945912476943f

This receiver changes no public JDK names, JNI ABI, native arena ownership, interned payload, String representation, Pattern/Matcher general grammar or VM flags. New target runtime adaptation retains GPLv2 with Classpath exception. Synexia-owned recipe/code catalogue retains its Apache-2.0 notices; neither license silently overwrites the other.

Optimization scope: whole-string String.matches only; lowercase ASCII text and pattern atoms (a-z or dot with optional single postfix star), 8192 max source pattern units, 4 million max DP cells. Reuses existing M3String.CodeUnitCursor over tuple/native atoms; two boolean rows plus operation-local primitive instructions. No source flattening, persistent pattern cache or per-row allocation. Out-of-scope grammar, malformed patterns, Unicode, line terminators, and budget excess delegate to JDK Pattern.matches unchanged. LeetCode 44 wildcard semantics are not admitted.

Executed isolated proof on stock OpenJDK21: strict -Xlint:all -Werror compilation of exact new helper with a clearly separate stub M3 owner, 100,010 matching pairs comparing independently compiled JDK Pattern.matches, bounded top-down memoization and refusal/budget tests, -Xmx32m. Recipe SHA-256 resources checked exactly. These are proof candidates, not successful execution on the experimental VM runtime.

NOT_RUN / required: full Maven/OpenRewrite visitor + actual source-seal replay, product java.base build and HotSpot compile, jtreg with -XX:+UseM3StringStorage, native/GC/JIT and full Java regex regression, cross-platform runtime, cold/warm throughput, retained heap and RSS. Keep draft and do not claim blanket elimination of JDK catastrophic backtracking.
