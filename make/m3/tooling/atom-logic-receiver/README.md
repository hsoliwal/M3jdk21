<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# M3JDK21 AtomLogic read-only Maven receiver

**Scope:** receive the existing Synexia normalized SHA-256 / AST-feature SimHash candidate-discovery `M3AtomLogicClusterRecipe` as an **opt-in** Maven control-reactor scan. Do not duplicate the parser, SHA256/SimHash index, persistence, native Hamming kernel, recipe synthesis, or the JDK compiler/GC implementation.

Source owner: [hsoliwal/com.synexia PR #11810](https://github.com/hsoliwal/com.synexia/pull/11810) (class-backed `com.synexia.rewrite.M3AtomLogicClusterRecipe`). Later canonical [#11823](https://github.com/hsoliwal/com.synexia/pull/11823), [#11864](https://github.com/hsoliwal/com.synexia/pull/11864) and [#11944](https://github.com/hsoliwal/com.synexia/pull/11944) own persisted/distributed candidate indexes, Java/JNI feature kernels and attributed call edges. This receiving PR **does not merge** those unqualified branch heads or recreate their functionality. The existing M3JDK21 semantic index/control DAG remains canonical.

## What the profile does

Add one optional profile to `m3/pom.xml` with the existing `rewrite-maven-plugin 5.23.1` and its existing Synexia snapshot coordinates. The profile activates the **read-only** `M3AtomLogicClusterRecipe` with `dryRun` (never `run` or `apply`), enables DataTables and refuses missing active recipes. The runner is an opt-in receipt-only scan of the **sources actually supplied to this Maven reactor**, *not* a claim that Maven automatically analyzes every OpenJDK `src/**/*.java` source or every other Git repository.

It emits the AtomLogic recipe's candidate relationships, occurrence identities, separate normalized logic and occurrence roots and status flags. **SHA-256 equality is only equality of the versioned projection. SimHash64 over ordered logic features is a neighbor signal; SimHash of the completed SHA256 is prohibited.** Neither fingerprint confers semantic equivalence, source mutation, automatic consolidation or API promotion.

## Reproducible recipe-first admission

The new receiver recipe `m3/tooling/atom-logic-receiver/recipe.py` is an exact-source Maven-bound **XML profile transformation only**. Its `before/pom.xml` is an immutable exact Git-blob preimage. It validates unique source anchors, refuses source drift/duplicate profile, parses resulting Maven XML, asserts one read-only canonical recipe, checks that removal of the generated fragment returns byte-for-byte original source, and requires a second application to be a fixed point. Generated target source is committed after the receiver recipe and tests. No Java/JNI production source is changed.

```bash
python3 m3/tooling/atom-logic-receiver/recipe.py self-test
mvn -B -ntp -f m3/tooling/atom-logic-receiver/pom.xml verify
```

To **actually execute** the OpenRewrite profile, first independently qualify and install a class-backed Synexia snapshot containing `M3AtomLogicClusterRecipe`. Merely configuring the profile does not compile or run the underlying canonical recipe:

```bash
mvn -B -ntp -f m3/pom.xml -Pm3-synexia-atomlogic-readonly verify
```

Record the exact Synexia commit/artifact, Java 21 compiler version, parse coverage, source/revision seals and exported DataTables. No silently excluded files may count toward whole-codebase coverage. Distributed fan-in must reconcile full occurrence records and versions before producing any roots or candidate pairs. Original JDK source is untouched.

## Algorithm catalogue and constraints

The receiver selects the existing Synexia category `normalized Java atom logic fingerprint / SimHash candidate discovery`. Compare exact SHA-256, structural signatures, 64-bit indexed Hamming neighborhoods, javac symbol attribution, and native feature distances by category; canonical first-party code wins over recreating a standalone JDK implementation. Guava/Eclipse/Apache and LeetCode/HackerRank/GeeksforGeeks may supply problem taxonomy and challenge fixtures, **not unlicensed source copies**. See the related filtered/sorted wrappers category ledger in [M3JDK21 PR #1059](https://github.com/hsoliwal/M3jdk21/pull/1059).

**HOLD:** The Maven snapshot from Synexia #11810 is not asserted available in this target checkout. No OpenRewrite SDK parse, full OpenJDK scan, Java/JNI performance, behavior equivalence or source-built JDK acceptance result is claimed by a profile+XML admission test. Source rewrite remains with existing source-bound, serial, compiler/test-verified recipe owners.
