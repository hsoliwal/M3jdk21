<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 typed Map purpose-wrapper and source-seal successor receiving

Owner: established target M3FilteredMaps class, based on
[M3JDK21 #991](https://github.com/hsoliwal/M3jdk21/pull/991).
Canonical first-party recipe and old/new source templates:
[Synexia #11951](https://github.com/hsoliwal/com.synexia/pull/11951),
head `1dc4904e461c1a2069dd21e955b10b770e800fc2`. First-party qualified Java donor:
[Synexia #11920](https://github.com/hsoliwal/com.synexia/pull/11920).

## Additive Java
Three typed factories—unmodifiableMap, checkedMap, synchronizedMap—
each with Map/SortedMap/NavigableMap static overloads (nine).
They delegate to JDK21 Collections wrappers, retain live backing
owner/comparator/range/descending guarantees, and don't create a
parallel Map or retained per-entry object table. Four allocation-light
private read atoms avoid filtered/guarded-entry pipeline construction
on size, isEmpty, containsValue and hashCode; no invented speedup.
Generic public Map.Entry interfaces may allocate adapters where Java
contract requires, but all M3 slot/index resident storage stays
unchanged.

Original Java/public descriptors, source predicate admission, guarded
entry writes, null mappings and comparator-equivalent key matching
must remain compatible. The new standalone Java21 test is borrowed
first-party, uses actual M3LinkedHashMap/M3TreeMap and JDK
oracle/locking cases; optional JNI peer is test-only, not product JNI.

## Exact source transition
Old five-target `generic-filtered-wrappers-postimages-20261017.tsv`
is **not modified or bypassed**. The existing verifier now admits one
reviewed new M3FilteredMaps SHA-256 only when all three current purpose
Map source/test files match exact pinned digests and path/status.
It also pins the complete historical manifest hash and checks
every sibling source unchanged. The text verifier and seven hostile
Python tests are installed byte-identically from Synexia source
recipe; neither is hand patched after generating the Java targets.

The exact-name CI runs every inherited source verifier, two negative
Python suites, all current Java21 production sources with -Werror,
both generic test corpora and the older primitive/filtered/Set algebra
suites. C17 JNI mask kernel is recompiled and executed under
-Xcheck:jni. That is an inherited JNI guard, **not a new native
predicate algorithm**.

Remaining independent gates: Maven/OpenRewrite SDK fixed point,
full M3 collections JUnit/reactor, all five provider API parity,
Map concurrency/weak/identity/serialization, OS/ARM/Windows JNI,
installed java.base/jtreg, performance and reviewer approval.
