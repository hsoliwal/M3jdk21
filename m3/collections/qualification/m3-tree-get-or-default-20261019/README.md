<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# Exact getOrDefault callback qualification

This correction is stacked on PR #1442 at
`0228a71901cd8f0abb38bccdc4941da2ec7d1aa9`. The unmodified original corpus now
matches JDK 21 in **640/640 complete case records**, including all **144** previously
corrected empty-map cases. Ten inherited differences are repaired; zero new
differences are introduced. `observations/after-report.json` records strict success.

## Cause and correction

The shared M3SlotMap implements getOrDefault with one findSlot call. JDK TreeMap
inherits Map's conditional containsKey lookup when get returns null. The missing
second search affected four natural-order and six explicit-comparator cases across
root and descending views. Return values, exceptions and state had matched, but
callback multiplicity did not. The second callback can also throw, as the new
regression demonstrates.

M3TreeMap now keeps get on its original single-search path and performs the
conditional second search in eager getOrDefault. Overriding both is necessary
because the inherited get dispatches virtually to getOrDefault. Lazy-value and
key-only extensions retain their existing single-search contract. All storage
fields, dynamic arrays, slot links, height lanes and nested AVL Tree source remain
unchanged; this introduces no entry storage or native dependency.

The full module run also exposed an inherited footprint test expecting one
comparison on an empty comparator map. That disagreed with PR #1442 and the frozen
JDK oracle. The recipe reconciles that assertion against a live JDK TreeMap while
retaining its insertion-exception and natural-null checks. No original oracle
source, stored case record, or prior empty-map regression was modified.

## Executed evidence

| Gate | Result |
| --- | --- |
| Pinned original M3 replay | 630/640; exact reproduction of ten inherited differences |
| Current JDK 21 vs frozen JDK original | 640/640 identical complete case objects |
| Corrected M3 replay | 640/640; 144 empty cases preserved; no introduced differences |
| New regression on original source | Two expected failures: callback count and missing second-search exception |
| Complete collection Maven test suite | 241 tests; zero failures, errors or skips |
| Canonical Maven/OpenRewrite SDK tests | 5 tests; two input orders, exact outputs, fixed point and refusals |
| Effective public method signatures | Same 85 signatures |
| AVL storage declarations and nested source | Unchanged |

OpenJDK 21.0.12.1 was used. Its exact case objects match the original 21.0.9
observations. Runtime/code-source/class hashes are retained in every raw JSONL
identity record. An intermediate attempt using baseline class bytes was rejected;
the final candidate class hash differs and is bound in qualification.json. The
initial proof-POM logging dependency failure and stale footprint expectation remain
in the failure history. No failure was normalized into a pass.

## Canonical recipe and replay

Canonical recipe PR: https://github.com/hsoliwal/com.synexia/pull/12492

Pinned canonical commit: `63c6857c7b57f79a494e950c89d5fbcf5b8c33a7`.
Recipe: `com.synexia.m3.TreeGetOrDefaultCallbacks`, reusing
`com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe`.
Crate: `synexia-openrewrite-recipes/verification/m3-tree-get-or-default-20261019`.
No reusable recipe engine is copied into this target. The included replay.py is an
exact pinned copy of the canonical proof script, with lineage in provenance.json.

With JAVA_HOME pointing to an unmodified JDK 21 and Maven dependencies available
offline, execute the existing target module:

```sh
mvn -o -B -ntp -f m3/collections/pom.xml test
```

For an independent differential replay, build the pinned PR #1442 checkout first:

```sh
python replay.py before --repository /path/to/baseline --output /path/to/evidence
```

Then apply the qualified Synexia SDK outputs to the sealed preimages, build the
candidate, wait for compilation to finish, and run:

```sh
python replay.py after --repository /path/to/candidate --output /path/to/evidence
```

The script invokes the existing EmptyRootLookupOracle and its unchanged JSON
formatter. It validates ordered IDs, every observation field, raw exception
messages, marker identity, callback sequences and map state. It records commands
and complete stdout/stderr; identity records are intentionally outside case-body
comparison. observations/files.sha256 seals the delivered evidence.

This is finite corpus and standalone collection-module qualification. It does not
claim identical AVL/red-black traversal paths for every insertion shape, universal
collection parity, performance gains, or full OpenJDK/Synexia reactor acceptance.
The public inherited signatures remain unchanged; explicit overrides change their
declaration/bridge metadata. Full OpenJDK build/jtreg, whole Synexia reactor and
hosted CI were not run. JNI is unchanged and requires no new native correction.
