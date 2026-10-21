<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# JDK-8371656: concrete Java 21 HashMap before/after — 2026-10-17

## Scope and exact source custody

- Baseline product: \`hsoliwal/M3jdk21 master@970a34350ebdf6a3f1c5ff5acc8445d0f3515f55\`,
  \`java.base/java.util.HashMap\` Git blob \`3ec69cfe9d8715d0125d23b0055d6f487481326f\`,
  SHA256 \`71c8d82247c2736e9fbe3769f366e9bd250bc73ecf390749998e830201aaabf2\`.
- Donor: [OpenJDK JDK-8371656](https://github.com/openjdk/jdk/pull/28243),
  commit \`13c92d0d4d137c7d83a946d1fcd2dfc5686e7b51\`, GPLv2 with Classpath exception;
  no Apache relicensing of the original JDK source.
- Canonical Maven/OpenRewrite source crate: [Synexia #11969](https://github.com/hsoliwal/com.synexia/pull/11969),
  \`com.synexia.m3.Jdk27HashMapBulk20261017\`, exact reviewed three Java targets.
- Target: M3JDK21 [draft receiver #1000](https://github.com/hsoliwal/M3jdk21/pull/1000)
  \`HashMap.java\` SHA256 \`8e5a059db03409858132297dd68811e16c19fc83d91d4449bdeca824f073605f\`,
  \`Collections.java\` SHA256 \`7dcb1bde18923274518b747ad4e65e3019046ca7742714ead678c615d99499ba\`.
  Product readback is **byte-identical** to the two source-sealed canonical recipe templates.

## Before: every map copies through the generic Map API

\`\`\`java
for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
    K key = e.getKey();
    V value = e.getValue();
    putVal(hash(key), key, value, false, evict);
}
\`\`\`

Even a source \`HashMap\` already stores \`Node.hash\`. This older implementation
allocates/traverses an entry iterator and computes \`hash(key)\` again, which
may be costly at megamorphic call sites or with wrapper map sources.

## After: exact-type guarded stored-hash transfer

\`\`\`java
Map<? extends K, ? extends V> hm;
if ((hm = m).getClass() == HashMap.class
    || m instanceof Collections.UnmodifiableMap<? extends K, ? extends V> umap
       && (hm = umap.m).getClass() == HashMap.class) {
    putHashMapEntries((HashMap<? extends K, ? extends V>) hm, evict);
    return;
}
// Existing generic fallback remains unchanged.
for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) { /* original */ }
\`\`\`

The new private helper traverses the exact source table and calls
\`putVal(e.hash, e.key, e.value, false, evict)\`. **LinkedHashMap**, custom
HashMap subclasses and other Map sources retain the original generic code
path. The target Map semantics, target subclass hooks and public API stay
unchanged. HashMap retains all fields and existing node format.

### Essential companion source change found by compiling Java21

Unlike OpenJDK27, OpenJDK21's \`Collections.UnmodifiableMap\` is a
**private** nested class; directly referencing it from \`HashMap\` failed
\`javac --patch-module java.base\`. The actual upstream JDK27 change also
made this type package-private. The **second production file** has precisely
one declaration-token change:

\`\`\`diff
-    private static class UnmodifiableMap<K,V> implements Map<K,V>, Serializable {
+    static class UnmodifiableMap<K,V> implements Map<K,V>, Serializable {
\`\`\`

It does alter the non-public nested class access flag and must be checked
against reflection/jtreg; copying only \`HashMap.java\` is **not** a compatible
Java21 backport. The source crate includes both originals and two generated
postimages with exact SHA-256; the third target is the jtreg Java source.

## Compiled Java21 proof and results

[Actual GitHub Action #38999449773](https://github.com/hsoliwal/M3jdk21/actions/runs/38999449773)
**SUCCESS**, same receiver head, using source-sealed Java21 \`HashMap.java\`
and \`Collections.java\` compiled with \`javac --patch-module java.base\`.
Stock Java21, patched Java21 and patched interpreter (\`-Xint\`) passed
**1,114 contract assertions each**, including hashCode call counts,
exact/wrapped source HashMap, null, self-copy, collision/tree bin,
LinkedHashMap order, subclasses and deterministic randomized copies.
Known pre-existing JDK21 \`this-escape\` and serial warnings are excluded
from otherwise fatal compiler lint; no product source edits were made to
suppress these warnings.

Warmed same-host medians from seven samples in **different** Java21
processes, so JIT/environment noise is plausible. Numbers in **ns/op**:

| Source and operation | Before | Patched | Difference |
| --- | ---: | ---: | ---: |
| Exact HashMap 5, constructor | 95.189 | 60.973 | 35.95% less time |
| Exact HashMap 5, putAll | 49.199 | 52.029 | 5.75% **more** time |
| Exact HashMap 150, constructor | 1,166.304 | 1,242.890 | 6.57% **more** time |
| Exact HashMap 150, putAll | 1,228.678 | 1,190.731 | 3.09% less time |
| Unmodifiable HashMap 150, constructor | 1,809.335 | 1,189.946 | 34.23% less time |
| Unmodifiable HashMap 150, putAll | 1,916.353 | 1,302.657 | 32.02% less time |
| LinkedHashMap 150, constructor, fallback unchanged | 1,693.039 | 1,340.370 | 20.83% less time |
| LinkedHashMap 150, putAll, fallback unchanged | 1,797.587 | 1,425.335 | 20.71% less time |

**Interpretation:** the wrapper optimization is promising and specific,
but not all HashMap cases improved. The unchanged LinkedHashMap fallback
also varied by ~21%; these short two-process probes are not JMH and should
not be used alone to tune thresholds or claim statistically significant
advantages. Re-run in forks across x86/ARM, mono/poly/mega-morphic call
sites, Map sizes and workloads. Profile allocation, CPU, GC and worst-case
collision/tree-bin behavior before choosing the final fast-path guard.

## Gate status

This test **does not build the complete modified M3JDK21 runtime**, does not
run the whole JDK/jtreg/JCK suite, and does not qualify compiler/HotSpot,
GC/JNI/JVMTI/CDS or all Synexia Maven/OpenRewrite fixed-point tests. The
runtime remains owned by M3JDK21; Synexia remains source recipe/convergence
owner, not a Java runtime dependency.

**Keep draft / do not automatically merge or claim global collection wins.**
