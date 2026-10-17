# M3 Collections SDK — JDK21 sequenced contracts using incumbent slot/link arrays

This is an additive canonical Synexia recipe for the public hsoliwal/M3jdk21 M3CollectionSdk facade. **Not an unrelated new collection backend**. Three new correctly named Java21 JDK-facing factories return `java.util.SequencedMap`, `java.util.SequencedSet`, and `java.util.SequencedCollection`. Concrete owners are the already-present `M3LinkedHashMap`, `M3LinkedHashSet`, `M3LinkedList`; primitive parallel slot/key/value lanes, offset-linked encounter order and JDK-valued null/duplicate rules remain.

The original JDK `java.util.*` API names stay authoritative. Guava BiMap, Eclipse MutableList, Spring MultiValueMap and Apache OrderedMap have different contracts and were **not** reclassified as JDK sequenced implementations. No donor source copied or third-party runtime dependency added. No new JNI ABI or API callback-to-native bridge; optional existing precomputed primitive mask JNI untouched.

## Recipe-first
Named `com.synexia.m3.SequencedSdkJdk21` uses unchanged Maven/OpenRewrite `M3HashPinnedJavaSnapshotRecipe` in exact PlainText source custody mode. One existing `M3CollectionSdk.java` exact SHA256 preimage -> reviewed postimage; one ABSENT new source test. The nine earlier facade source bodies and signatures must remain byte-identical. SHA256 before `9a51f5edad8fd58ec9a7cc4dcd17cec23117de1eba486ba1b7a4c631acd8eff0`, after `8bfaa6ef27ecc9ef892db4586751f05b5485a49a1fb7469ff45fc5a55b75b508`.

## Performed locally
OpenJDK 21.0.12.1, Maven3.9.9, OpenRewrite8.17.1/JUnit5.10.2. **Four actual Maven recipe JUnit methods PASS**, no failures, errors, skips. All four partial source states in both orders, fixed point, absent/drift/duplicate/late edit refusal, previous SDK method byte preservation.

Strict Java21 javac against **real previously compiled first-party M3LinkedHashMap/M3LinkedHashSet/M3LinkedList owners** with their current master Git blobs, and **compile-only scaffolds for unrelated M3CollectionSdk dependencies**, passed. The generated SDK facade plus standalone Java21 contract program executed **592923 assertions and 147456 seeded map/set/list mutations**. JDK LinkedHashMap/LinkedHashSet/LinkedList were the differential authorities. Comparisons cover putFirst/putLast/reversed/poll endpoints, addFirst/addLast/remove endpoints, live reversal, encounters, nulls and original owner invariants. The tested Git blobs match current M3 owners.

## Pending before release
Actual complete M3 current-master Java21 core compile/Maven JUnit, current-head hosted exact SHA checks and C17/-Xcheck:jni (old mask path unchanged), installed JDK/jtreg, all third-party provider method tests, concurrency/serialization/GC/platform/benchmarks, String->arrays->collections promotion remain independent gates. This is three proven factory surfaces, **not every collection API implemented**.

Source and tests are first-party Apache-2.0 ©2026 Hitesh Soliwal and contributors. LeetCode/HackerRank/GeeksforGeeks comparisons in CATALOGUE.tsv supply categories, not copied implementations. Synexia develop never rebased; receiver changes must be recipe-emitted, not hand edited.
