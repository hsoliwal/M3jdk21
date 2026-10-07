# Synexia MIndex collections source-custody intake

Status: SOURCE_CUSTODY_ONLY_DEPENDENCY_FRONTIER_OPEN

## Exact source

- repository: hsoliwal/com.synexia
- source revision: 7d2133f1412a9e7295296c3f86baae577bb3251c
- source module: synexia-mindex/collections
- source module tree: 6df81293f7c514b18910b2b1b9563d08ecb733ac
- source POM blob: f2f75ac51d5501c911d70f7b91967b57d16f03c6
- receiver parent: M3JDK21 PR #334 qualified AST/regex/precompute recovery

## Custody scope

This packet receives the complete Java module source/test corpus, not a cherry-picked class list:

- 46 production Java sources
- 20 JUnit/test Java sources
- 1 Maven POM
- total executable/build custody files: 67

Target custody root: m3/vendor/synexia/synexia-mindex/collections/

All copied files retain their exact Synexia package/source bytes. No renaming, code generation or JDK product wiring occurs in this intake.

## License / authorship

All 66 Java files were individually qualified for SPDX-License-Identifier: Apache-2.0.

- 51 MIndex-named Java files had Apache-2.0 headers recorded in the merged 4,770-path full-family catalogue.
- 14 catalogue rows whose bodies had not been materialized were re-read from the exact pinned source revision and all 14 contain Apache-2.0 headers.
- IdSupport.java, outside the basename-MIndex* catalogue selection, was separately re-read at the same revision and contains Apache-2.0.
- the module POM contains SPDX-License-Identifier: Apache-2.0.

Copyright/provenance follows the canonical Synexia authority:
Copyright 2026 Hitesh Soliwal and contributors for qualified first-party authored expression.

This does not relabel abstract ideas as copyrighted source and does not relicense OpenJDK or third-party bodies.

## Dependency frontier

The source POM depends on com.synexia:synexia-mindex-runtime.

That dependency closure is not admitted by this packet. Therefore:

- target compilation is not claimed;
- the vendor copy is not added to the M3JDK Maven control reactor;
- no java.base/JDK collection implementation is changed;
- no JDK collection contract equivalence is claimed.

The next pass must receive/qualify the required runtime slice or replace it with an independently proved target adapter before compilation.

## Promotion rule

This custody packet grants no automatic JDK mutation or promotion authority.

Before any collection implementation can influence JDK collections, it requires separate contract review for null handling, equality/hash, ordering, iterator/view behavior, serialization, concurrency/JMM where applicable, complexity and source/API compatibility.

## No-loss lineage

This lane composes merged M3JDK full-family invariant PR #309, original Synexia full-family authority PR #9648, current Synexia authority readback recovery PR #9677, and qualified M3JDK AST/regex/precompute recovery PR #334.

The exact source bytes remain canonical in Synexia; M3JDK is a receiver/custody target.
