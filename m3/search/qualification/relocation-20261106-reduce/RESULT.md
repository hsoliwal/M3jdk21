# m3/search leaf 5: synexia-fastsearch knowledge reduce/vector kernels (2026-11-06)

Donor: com.synexia develop fd9a233680b4, synexia-fastsearch. Recipe: M3_SEARCH_RELOCATE v4
(`relocate_search_v4.py` + `spec_v4.tsv`). v4 is v3 plus the packages `knowledge.reduce`, `knowledge.util` and
`knowledge.model`. Then `jdk_naming.py apply --only m3/search`: 10 provenance-line rewrites, after which the
second plan finds nothing (fixed point). Per-file blobs: `LEDGER_v4.tsv`.

| donor | M3 owner (jdk.m3.search) |
| --- | --- |
| knowledge/util/Utf8Fnv1a64 | M3Utf8Fnv1a64 |
| knowledge/reduce/SimHash64 | M3SimHash64 |
| knowledge/reduce/LuhnSummarizer | M3LuhnSummarizer |
| knowledge/reduce/DocumentDeduplicator | M3DocumentDeduplicator |
| knowledge/reduce/EvidenceSynthesizer | M3EvidenceSynthesizer |
| knowledge/model/KnowledgeDocument, SourceKind | M3KnowledgeDocument, M3SourceKind |
| knowledge/index/IndexedHit, FeatureHashVectorizer | M3IndexedHit, M3FeatureHashVectorizer |
| test knowledge/reduce/DocumentDeduplicatorConcurrencyTest | DocumentDeduplicatorConcurrencyTest |

All of them depend on java.base only.

## Gates

Run with `leaf5_gate.sh` on master e1595e300940 using stock JDK 21.0.9. The m3/collections, m3/ports/file and m3/file sources are on -sourcepath.

| gate | live | leaf |
| --- | --- | --- |
| javac -Xlint:all,-module -Werror, m3/search main | 25 files, 0 errors, 0 warnings | 34 files, 0 errors, 0 warnings |
| m3/search test sources javac | 0 errors | 0 errors |
| m3/search JUnit (whole suite, natives absent) | 74 passed / 18 failed | 75 passed / 18 failed, identical failing set |
| DocumentDeduplicatorConcurrencyTest | n/a | 1/1 (the donor test is 1/1 too) |
| Leaf5Diff (donor vs relocated in one JVM) | n/a | PASS 20,000 cases (seed 20261106, digest 2255b1c6b4bbc046), PASS 30,000 cases (seed 7, digest f9b0ef65ef62bb2d); same digest against the module build |
| mutant (M3Utf8Fnv1a64 PRIME ...b3 -> ...b5) | n/a | refused at case 0 |

Leaf5Diff covers:
- FNV-1a hash and update over arbitrary [a,b) ranges;
- SimHash64 `of` and `hamming` over near-duplicates;
- FeatureHashVectorizer raw float bits for every power-of-two dimension from 16 to 1024;
- Luhn `rank` and `summarize`;
- dedup decision streams;
- EvidenceSynthesizer summaries and citations.

The text mixes Latin-1, CJK, supplementary characters and unpaired surrogates.

## NEC (not decided, owners kept apart)

- M3SimHash64 vs m3/indexdb IndexDbChunkSimHash: the ties->1 rule is pinned by #2514. These are different contracts.
- M3Utf8Fnv1a64 vs the FNV-1a in the m3-hash-family crate: 0e's m3/indexdb leaf records it as NEC/ALREADY against this owner.
- M3FeatureHashVectorizer vs the jdk.m3.lib vector providers (DefaultVectorizationProvider): those are a different concern (Lucene vector utilities).

Not ported (Lucene-bound, EXCLUDE): LuceneKnowledgeIndex, PreparedSearchQuery, KnowledgeSearchFilter.
