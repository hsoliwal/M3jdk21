# M3JDK21 long-range set — Java21 array/index/mask receiver

Canonical source/reusable OpenRewrite recipe: [Synexia #12159](https://github.com/hsoliwal/com.synexia/pull/12159), SHA b353a2d74552334e3ff323853af9d1b41be0827c. This target contains the same exact three source/test outputs, not an alternative implementation.

## Concrete array-backed sorted and filtered range owner

\`M3LongRangeSet\` has one pair of dynamic parallel \`long[]\` lanes storing sorted, disjoint, non-touching \`[startInclusive,endExclusive)\` intervals. It merges overlaps/touching ranges on insertion, splits on removal, binary-searches membership/intersection, calculates exact BigInteger cardinality, exposes defensive primitive arrays and a fail-fast two-long visitor, and creates detached snapshots. No per-interval Entry/Node or boxed Long storage. The semantics are **signed** long half-open; because the exclusive endpoint must be a long, inclusive \`Long.MAX_VALUE\` cannot be represented. This is not Guava RangeSet/RangeMap, or any provider API clone.

The \`selectedByState(int[] flags,int requiredMask,int forbiddenMask,Backend)\` operation reuses the installed **M3IndexPathMasks** engine and its existing explicit C17/JNI \`NATIVE_REQUIRED\` path. Mask words are 32-bit and aligned to current sorted interval ordinal, not storage slot address. No Java predicates, heap object references or persistent pointers cross JNI. Exact selected intervals are detached. The source's parallel arrays remain its sole authoritative storage.

Time/cost: binary-search membership O(log r), edits O(r) with in-place lane shifts and occasional dynamic growth; selection O(r) and O(selected) additional index/result space; snapshot O(r). This is not a benchmark speedup or a replacement for all collection families.

## Source-first recipe / custody / donation

- Existing target \`M3HashPinnedJavaSnapshotRecipe\` owns Java source generation; named YAML \`com.synexia.m3.Jdk21LongRangeArrays20261017\` and 3 ABSENT-before SHA-256-sealed postimages copied exactly from canonical Synexia.
- Source-first receiver commit mirrors YAML, manifest, 3 Java templates, original Synexia category decisions, canonical recipe JUnit and this product's isolated **real Maven/OpenRewrite** JUnit replay suite. The next commit installs only the three mechanically generated source/postimage blobs, byte-identical with the templates.
- Target verifier \`verify.py\` checks each emitted file byte-for-byte, unchanged production Java/C JNI peers by actual Git-blob identity, sorted manifest/catalgoue and explicit no-promotion receipt.
- Errors are fixed by correcting/admitting new canonical recipes, never patching generated product source. No change to Synexia \`develop\`, preexisting JDK/Guava/Eclipse/Spring/Apache public API names, original JDK classes or the current collection module JNI ABI.
- Problem-category catalogue categorizes interval union/split, order, BigInteger cardinality, native-mask reuse, and LeetCode/HackerRank/GeeksforGeeks approaches; no external challenge source copied. New first-party text retains Apache-2.0 attribution.

## Exact bounded qualification

Synexia donor reports strict Java21 and 411,913 checks across 20,000 seeded edits **using a test-only JNI selector stand-in**. This is useful preliminary source evidence, **not real native/parity evidence**. The receiving GitHub workflow is configured to execute the existing Maven recipe engine against the sealed Java LSTs, all changed Java strict compilation, new standalone oracle in Java and interpreter mode, native C17 strict UBSan build, incumbent M3IndexPathMasks native-required oracle under \`-Xcheck:jni\`, and range-set selftest with the native library actively loaded.

Run:
\`\`\`sh
python3 m3/collections/qualification/long-range-set-20261017/verify.py
mvn -B -ntp -f m3/collections/qualification/long-range-set-20261017/recipe/pom.xml test
mvn -B -ntp -f m3/collections/pom.xml -Dtest=M3LongRangeSetTest test
\`\`\`

Keep DRAFT/HOLD until those exact-head gates pass; product full reactor, source-built JDK, jtreg/JCK, provider/JMM/serialization performance and String → arrays → collections promotion still require separate evidence. A merged or open PR is not release-ready by itself.
