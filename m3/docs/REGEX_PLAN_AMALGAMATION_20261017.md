# M3JDK21 — regex plan amalgamation Phase 1 (2026-10-17)

This receiver is stacked on [M3JDK21 #1003](https://github.com/hsoliwal/M3jdk21/pull/1003), which restores an historically merged composite cursor into the more advanced current regex engine. The canonical source-sealed Maven/OpenRewrite recipe is [Synexia #11975](https://github.com/hsoliwal/com.synexia/pull/11975). It is a **single existing-plan enhancement**, not another Pattern parser/cache or JNI engine.

## Exact before and after

Current code always enters rolling DP or bitset/effect/native execution for any syntactically accepted bounded dot/star pattern. Now `Plan` folds **four metadata facts once per compiled pattern**: minimum UTF16 units from non-star atoms; mandatory ASCII character 64-bit presence mask through `M3StringFacts.codeUnitSignal`; and exact first/last mandatory ASCII units when the initial/final atom is not starred and not dot. `tryMatch` checks these facts before either engine. The gate never prepares a source fact; `M3String.factsIfPrepared()` is used, so missing facts safely do nothing. Precomputed signals are only impossibility filters, not regex accept decisions.

```java
// Before: any accepted bounded grammar enters DP or memoized native effect engine.
if (plan.closure == null) return rolling(source, plan);

// After: reject only provably impossible queries, then preserve exact engine.
if (rejectsByPrecompute(source, plan)) return NO_MATCH;
if (plan.closure == null) return rolling(source, plan);
```

Code is in `src/java.base/share/classes/java/lang/M3StringRegexDp.java`, original Git blob `e93669cc305b53638be241b189dcb5c652c1fac6`, SHA256 `f875210dfb605799b6c8dbee53b28ad6383e000d90da775f7fd285464da7b2d3`, post SHA256 `043c26a0f55ee2a0170bf907ee428dfd133696ce45bf0de44b822dc9e1e5e0e7`. Extended original jtreg source pre SHA256 `108087f9706b1f426b9330eccd4baa2e4e30ed327f0dba644a11e21f3a6eb0ad`, post SHA256 `23b49a26ce30586758afb8cc7a7fecaee5a70833943fd3598c5b703d2a516cd9`.

## Shared facts, not duplicated precompute stores

- `M3StringFacts` already provides canonical bitSignal64 and n-grams; regex may read existing owner/range facts but may never attach new per-String data.
- `M3StringRegexDp.Plan` remains the only cached prepared restricted grammar in this consumer; its atomic plan cache and effect cache are preserved unchanged.
- `M3StringRegexRowPrecompute` continues its own batch posting list for exact ASCII literal shapes using the same M3StringFacts signal algebra. No competing row index is built.
- `M3RegexDpPlan`, `M3StateRelation`, `M3DotStarRelations`, `M3TQ`, `M3RX7` retain their specialized immutable relation/search/DFA roles. Do not merge unlike automaton state numbering or use unverified DFA candidates as Java Pattern truth.
- JDK `Pattern/Matcher` remain authoritative for unsupported grammar, flags, captures, Unicode, regex split/find and mutable match history.

Per plan maximum extra **3 int + 1 long = 20 primitive bytes**, including both the 64 plan slots and up to 256 effect-referenced older plans: +6,400 bytes conservative budget. No new pattern cache, native pointer, strong canonical owner or owned text payload.

## Proof and promotion

Host `.github/workflows/m3-regex-plan-amalgamation.yml` checks exact source seals and compiles the **complete old and new** `M3StringRegexDp` Java source with no algorithm rewrite (only package/class rename), along with the real source-extracted `M3String.CodeUnitCursor` and source-extracted M3StringFacts hash mixers under a small synthetic immutable M3 tuple fixture. This proves candidate Boolean decisions against independent stock Java21 `Pattern.matches` across 12,288 random cases plus deterministic regressions; and records 9-sample warmed same-runner A/B with positive and negative workloads. It does not prove matching modified JDK runtime/JNI or JMH significance. 

Separate exact JDK jtreg adds 8,192 new cases to the existing >5,120 differential tests. Full source-built M3JDK21 JNI, GC/CDS, jtreg/JCK, C1/C2/Xint, actual M3 owner lifetime, stress/OOME, cold/warm CPU/RSS and complete Synexia Maven/OpenRewrite replay are still gates. **Draft; no broad performance claim or merge without measured and compatible receiver.**
