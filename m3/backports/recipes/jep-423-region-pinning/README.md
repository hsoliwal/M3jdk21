# JEP 423 — Region Pinning for G1

Status: inventory / FILE-atom candidate packet. No product source materialization or compatibility
completion claim.

## Upstream authority

- JEP: 423
- issue: JDK-8318706
- implementation commit: `38cfb220ddadbb401cc15f313aadb8234f626210`
- implementation parent: `e44d4b24ed794957c47c140ab6f15544efa2b278`
- target release: JDK 22
- M3JDK21 verbatim baseline: OpenJDK 21 GA
  `890adb6410dab4606a4f26a942aed02fb2f55387`

The initial upstream implementation commit touches **59 paths**.

The mandatory cumulative lineage is:

1. JDK-8318706 / `38cfb220ddadbb401cc15f313aadb8234f626210` — JEP 423 implementation;
2. JDK-8323610 / `8643cc21333c6b51242ed3b9295b25f372244755` — pin-count overflow fix;
3. JDK-8322484 / `0d5f5e15d43f94a79c6133baecd5af217365d176` — pin-cache performance repair.

The cumulative materialization denominator is therefore **64 paths**:

- **47** product/runtime/SA/WhiteBox paths;
- **15** admitted regression/proof paths;
- **2** upstream-deleted JDK21 test paths that M3JDK21 preserves by default.

The five additional product paths are introduced by the mandatory pin-cache repair and are first-class
FILE atoms. A packet that materializes only the initial 59-path implementation is incomplete.

## Compatibility split

JEP 423 is a G1 runtime/GC implementation feature; it does not require Java source grammar or a
new Java SE API. That makes it a compatibility candidate, not an automatic acceptance.

The implementation touches G1 evacuation/collection-set/full-GC policy, heap-region metadata,
WhiteBox/VMStructs and HotSpot Agent mirrors. Those relationships require runtime, JNI/JVMTI/SA,
full-GC and pinned-object regression proof before feature composition.

The upstream commit removes:

- `test/hotspot/jtreg/gc/stress/TestJNIBlockFullGC/TestJNIBlockFullGC.java`
- `test/hotspot/jtreg/gc/stress/TestJNIBlockFullGC/libTestJNIBlockFullGC.c`

M3JDK21 does **not** import those removals during the FILE pass. They remain explicit preserved
Java21 paths unless a later compatibility proof authorizes removal.

## Mechanical pass

The first pass is strictly file-atomic:

1. pin the 64-path cumulative denominator in `PATHS.txt`;
2. exclude the two upstream deletions from source materialization;
3. audit selected-path history from the implementation parent through the two mandatory fixes;
4. require no unreviewed selected-path commit between the cumulative final fix and JDK22 GA;
5. generate one exact Java/text/native FILE atom per 62 admitted path from the proven cumulative donor state;
4. compare every candidate against the JDK21 verbatim preimage;
5. retain per-file JUnit/jtreg/native proof references;
6. only after all FILE atoms reach fixed point may G1 package/module composition be proposed.

Likely later scope:

```text
FILE
  -> PACKAGE (G1 internal collaboration)
  -> MODULE (hotspot + hotspot-agent/test joins)
  -> MULTI_MODULE/runtime proof
```

No LIBRARY_API promotion is implied by this inventory.

## Donor-state rule

JDK22 GA may be used as the physical postimage source only after selected-path lineage proves that
the 62 admitted paths contain no unreviewed intervening changes beyond the three required commits,
and no later JDK22-GA change modifies them after the cumulative final fix. If that audit finds
additional commits, those commits become explicit dependency/review rows before materialization.

## Required proof before promotion

- exact path denominator and JDK22 GA cumulative state;
- no unaccounted upstream path;
- source-bound replay/fixed point for every admitted path;
- HotSpot release build on the Java21 baseline;
- focused G1 pinned-object/full-GC jtreg;
- WhiteBox and HotSpot Agent consistency;
- JNI/JVMTI-sensitive pinning regressions;
- existing JDK21 G1 and preserved TestJNIBlockFullGC regressions;
- runtime stress proof;
- whole-JDK compatibility gates.

Promotion remains `NOT_AUTHORIZED`.
