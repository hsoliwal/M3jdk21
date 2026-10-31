# M3JDK21 tombstone-reuse receiver — selected-source report

**Status:** DRAFT/HOLD. Exact selected source and Java21 behavior qualified by
Synexia canonical source-sealed recipe PR #12010 at `5ba32cc8c363532c25f615ef65842ac68736aa56`.

**Changed production files:** M3LongHashSet.java and M3LongLongHashMap.java.
Their public JDK-shaped interfaces remain unchanged, no new internal Entry/Node,
all state in existing primitive arrays; `java.util` is not changed.

**Actual executed bounded evidence:** donor source PRE fails targeted capacity
regression, reviewed POST passes 142,438 checks against standard JDK value oracles;
192,768 model checks; 4 source refusals; 4 mutants rejected; repeat Java21 class
hashes for 9 class files; public descriptor identity. See donor proof summary
`evidence/SELECTED_SOURCE_SUMMARY.json` in this target receiver.

**Missing:** target hosted receiver, Maven/OpenRewrite SDK, full collections
reactor, matched JDK image/jtreg, native/GC/CDS, independent review, performance.
The receiving phase is still String. No canonical phase promotion.
