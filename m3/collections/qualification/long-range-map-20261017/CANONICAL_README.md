<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 primitive long RangeMap — canonical Synexia recipe

**Canonical producer:** \`hsoliwal/com.synexia\`. **Product receiver:** \`hsoliwal/M3jdk21\`, stacked on the existing M3LongRangeSet receiver PR #1184. This is a concrete additional **sorted/filtered long range map**, not a catalogue-only family or a wholesale \`java.util\`/Guava/Spring/Eclipse/Commons replacement.

## Implemented data layout and contract

\`M3LongRangeMap<V>\` uses three aligned dynamic array lanes: \`long[] starts\`, \`long[] ends\`, \`Object[] values\`, plus size/revision scalars. Sorted nonoverlapping half-open signed long intervals are coalesced only as required to avoid overlaps; **adjacent intervals may stay distinct**, even if values compare equal. No per-interval Entry/Node wrapper or second hash/index engine.

\`put(from,to,value)\` replaces the exact half-open coordinate window, preserving partial earlier/later mappings by split and tail shift. \`remove\` deletes that window with similar arithmetic. Staged growth allocates all three arrays before publishing new backing storage; moved old values in truncated tails are cleared. \`containsPoint\` and \`get\` distinguish absent from present-null; comparator order is signed long, binary membership O(log r), updates O(r). An exclusive upper endpoint cannot cover \`Long.MAX_VALUE\`; this limitation is explicit. Single-threaded only; structural edits during callbacks fail after the callback rather than rolling back.

\`snapshot\` copies authoritative parallel lane prefixes, \`forEachRange\` uses a primitive-coordinate callback, and \`selectedByState\` delegates **precomputed range-ordinal 32-bit required/forbidden masks** to the **existing \`M3IndexPathMasks\` JAVA/NATIVE_REQUIRED Java/C17 selector**. No new JNI symbol, native object references, or Java predicate/comparator callbacks.

## Mandatory source-first recipe, not hand-edited generated Java

\`com.synexia.m3.Jdk21LongRangeMap20261017\` composes the existing \`M3HashPinnedJavaSnapshotRecipe\`, standard AST Java LST mode. The sorted manifest marks **three ABSENT-before exact SHA-256 targets**, retains fully reviewed source templates, and rejects nonmatching occupied/duplicate files. All generated Java is taken only from this recipe. Fix the recipe and rerun if the postimage fails, never patch it directly.

First-party Apache-2.0 and donor provenance remain recorded in \`CATEGORY_REVIEW.tsv\`. LeetCode #56/#57, HackerRank Array Manipulation and GeeksforGeeks intervals are algorithm categories, not copied code or shortcuts around public contracts.

## Actual local evidence and open gates

OpenJDK 21.0.12.1, Maven 3.9.9, OpenRewrite 8.17.1, JUnit5.10.2: **4 actual Maven/OpenRewrite tests passed**, zero failures/errors/skips; all 8 partial states, both input orders, fixed point, conflicting/duplicate target refusals. Actual recipe-generated Java strictly compiled \`--release 21 -proc:none -Xlint:all -Werror\` and completed **1,689,303 differential checks across 49,152 seeded operations**, separately under normal JIT and \`-Xint\`.

**Important limit:** local runtime linked only a test-only signature-compatible \`M3IndexPathMasks\` stub for Java-path selection and native-unavailable refusal. The test-only stub must never enter M3JDK21 product. Actual incumbent Java/C17 JNI integration, checked native parity, owning Maven/JUnit reactor, current exact-head GitHub Actions, installed JDK/jtreg/JCK, concurrency/serialization and matched performance remain held. A native speedup is not claimed.

Run the canonical crate JUnit in its Maven repository, and the product receiver verifier + owning module suite against the exact target head; preserve the user's STRING → arrays → collections → AST/compiler receiving order. No \`com.synexia\` runtime dependency is introduced into product APIs.
