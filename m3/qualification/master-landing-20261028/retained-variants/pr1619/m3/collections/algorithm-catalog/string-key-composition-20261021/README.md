<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
     SPDX-License-Identifier: Apache-2.0 -->
# String-key composition opportunity catalogue

This additive catalogue evaluates ordinary String keys backed by the existing M3String owner. It contains **39 category decisions**, a finite **312-source inventory**, all **104 existing SDK family records**, and **14 primary source references**. The source epoch is `18213fe70c66567fdfc6ba6f669909e7e9251663`. No replacement String wrapper, dictionary or tree implementation is installed by these metadata files.

`DONOR_DECISIONS.tsv` compares hash maps/sets, ordered and sorted collections, bags/multimaps, bimaps/tables, filtered views, caches, tree/trie alternatives, array indexes/masks, JNI, and consumer categories. `OWNER_INVENTORY.tsv` is produced from exact Git blobs. Its name categories and lexical source signals are **inventory**, not proof that 312 owners were behaviorally reviewed or qualified. `SDK_FAMILY_INVENTORY.tsv` preserves every original owner/proposed-role/disposition cell: PENDING stays PENDING. Primitive-key, enum/class-token, identity and weak/reference domains stay distinct.

## Read-only queries

From this directory:

```sh
python3 query_catalogue.py --where category=string.key.equal
python3 query_catalogue.py --match trie --format json
python3 query_catalogue.py --where status=REJECTED
python3 query_catalogue.py --table owners --match M3PatriciaTrie
python3 query_catalogue.py --table families --where vendor=ECLIPSE
python3 test_catalogue.py -v
```

Exact `--where` filters and substring `--match` terms are ANDed. Queries do not change decisions or grant admission. Use `build_inventory.py --repository /absolute/M3jdk21 --revision 18213fe70c66567fdfc6ba6f669909e7e9251663 --output /absolute/fresh/inventory` to reproduce the finite source inventory. It reads immutable Git blobs and refuses an existing output directory. The two independent inventory generations were byte-identical; five final query/schema/refusal checks pass. The initial overbroad test expectation is retained in canonical evidence.

## Highest-priority owners and boundaries

The selected runtime work reuses M3String's immutable shared ranges, the existing filtered sorted-owner representative lookup, and native array-mask compaction. Shared range identity can skip proven equal content; hashes and routing bits remain candidate filters. A new String comparison fact can help ordinary hash and sorted key consumers through their current Java APIs. Sparse JNI payload spans can reduce transferred elements in qualified fixtures. These mechanisms do not establish workload-wide speedup, allocation reduction or full provider qualification.

M3TreeMap/Set have an existing array AVL owner. The primary M3PatriciaTrie, M3LongPatriciaTrie and separate Apache provider mirror are unreceived scaffolds. The operational relocated Commons trie retains TrieEntry nodes and has different representative-replacement semantics from TreeMap. `TREE_TRIE_FINDINGS.md` records that source review, bounded donor tests, exact catalogue dispositions and the unchanged donor's Java21 source-visibility refusal. Prior-image test observations remain separate from a fresh receiving-product build.

Compressed owner+offset+length edges, primitive child slots, terminal masks, double-array transitions and adaptive radix variants are explicit future candidates. They require empty/mid-edge prefix behavior, unsigned UTF-16 code-unit traversal with signed comparison results, surrogate boundaries, split/merge/removal, live views and original key lifetimes. Arbitrary comparator equivalence does not define lexical prefix order. M3FrozenLexiconIndex is a displacement hash index and M3SignalOrder is radix sorting; neither is evidence of an implemented radix tree.

## Canonical recipe and rights

The canonical Synexia task is `synexia-openrewrite-recipes/recipe-crates/m3-ast-build-reverse-20261017/tasks/m3-string-key-collections-20261021` in [com.synexia](https://github.com/hsoliwal/com.synexia). Its unchanged prepare/Maven/OpenRewrite/receive path seals each target's preimage, postimage and mode; only retained SDK Results are received. Source receipts, actual SDK outcomes, target execution, first-pass refusals and limitations are separate records there. `SOURCE_CUSTODY.json` binds the initial partial canonical snapshot; it does not assert the whole Synexia repository was inventoried.

`DONOR_SOURCES.json` records GitHub, LeetCode, HackerRank and GeeksforGeeks comparisons and rights. Challenge sources nominate operation categories and adversarial cases only. No challenge statement, editorial or solution body is copied. Existing Apache and GPLv2+Classpath target notices remain attached to their own source templates; this Apache metadata does not relicense them. No timing benchmark or whole-repository completion is asserted by the catalogue.
