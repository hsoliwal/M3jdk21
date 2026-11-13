<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 CPU fourth-wave receiving — exact bounded primitives

This is first-party source output in the EXISTING com.m3.tools.cpu JDK module.
M3JDK21 is the product, not an M3+JDK adaptation layer. Canonical Maven/OpenRewrite
recipes, their source templates, catalogue and donor experiments reside exclusively
in Synexia; no transformation engine or recipe crate belongs in the public product.

The six added classes implement exact complete-DFA state equivalence,
capacity-bounded 0/1 knapsack, CSR dominator masks, integer-ID BM25 ranking,
strict nonoverlapping UTF-16 range merge and four-row approximate count-min-style
frequency signals. No Map.Entry or graph-node object storage is retained by
these algorithms; public result records detach primitive arrays on access.

The two product fixture programs run under a standalone stock Java 21 runner:
`python3 m3/tooling/module-packs/cpu-tools/fixtures/verify_wave4.py
 --owner m3/tooling/module-packs/cpu-tools --out /tmp/wave4-fresh-output`.
The existing M3JDK21 build automatically *compiles* all new classes through
module-source-path traversal, but its legacy fixture inventory has NOT been
modified because that would require a verified live hash-pinned preimage of
`cpu-tools/build.py`. The new fixtures are NOT wired into the full JMOD run.
A separate source-sealed successor must qualify that existing file update.
`WAVE4_COVERAGE.tsv` is additive; the existing COVERAGE.tsv remains unchanged
until its exact live preimage is available to the canonical recipe.

Independent local stock Java 21 `javac --release 21 -Xlint:all -Werror`
plus `java -Xcheck:jni` and `java -Xcheck:jni -Xint` both pass
328,225 automata/planning assertions and 350,224 indexing/merge/cache
assertions per mode; this is scoped correctness, not speedup proof.

PENDING: canonical Maven/OpenRewrite SDK JUnit, linked M3JDK21 image/JMOD,
modified java.base/HotSpot/jtreg, full runtime compatibility, native JNI
acceleration, memory benchmarks and tool-family-complete implementation.
Do not conflate the 91 ledger rows with full product-family compatibility.
