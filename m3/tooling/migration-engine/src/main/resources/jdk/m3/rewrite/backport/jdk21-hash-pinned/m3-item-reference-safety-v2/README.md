# Item reference safety recipe

`com.m3.rewrite.backport.M3ItemReferenceSafety` delegates to the existing `M3Jdk21HashPinnedSnapshotRecipe` with crate `m3-item-reference-safety-v2`. It changes only two local classifier owners and their two existing test/fixture files. All four exact preimages are required; source drift, omissions, duplicate targets and non-Java trees refuse the whole operation. Already-converged outputs replay with no changes.

The repair retains aliases returned by the three exact static `Objects.requireNonNull` overloads and identifies annotated methods by owner, name and descriptor. Existing seeded whitelist keys remain separate. Other helpers remain unknown. It does not provide general method ownership, last-use, open dispatch or GC authority.

Executed on Windows/JDK21 using the provisioned source-built OpenRewrite8.17.1 and JUnit5.10.2 SDK: original classifier plus new witnesses7tests/4failures; repaired classifier same7tests/0failures; YAML-discovered nested recipe6tests/0failures, including exact application, two zero-change replays and ten admission refusals. Production classifier review is clean. Exact source/dependency/proof hashes are in `qualification-windows-jdk21.json`.

This forward safety crate does not replace the older nineteen-row full compiletime crate or claim its current twenty-nine-source custody is complete. General method summaries, runtime reuse, matched completeM3JDK, full Maven reactor, hosted checks and retention/performance qualification remain pending. Keep the continual migration goal active.
