# M3JDK21 primitive tombstone reuse — target receiver

**Candidate-only; target phase remains String.** This additive receiver adopts
Synexia's verified recipe/source at [donor PR #12010](https://github.com/hsoliwal/com.synexia/pull/12010),
commit `5ba32cc8c363532c25f615ef65842ac68736aa56`; original target baseline is `master@970a34350ebdf6a3f1c5ff5acc8445d0f3515f55`.

The only runtime edits are `M3LongHashSet.add(long)` and
`M3LongLongHashMap.put(long,long)`, preserving their original Apache-2.0 headers,
public APIs, primitive-array storage and `java.base`-only dependencies.
No `java.util`, HotSpot, JNI/native symbols, GC, M3 String or comparator semantics
are changed. This does not make `M3LongHashSet` a transparent JDK replacement.

## Reproduce

From an exact full target checkout, on a JDK21 host:

```sh
python3 m3/collections/verification/tombstone-reuse-20261017/verify.py
mvn -f m3/collections/verification/tombstone-reuse-20261017/pom.xml verify
mvn -f m3/collections/pom.xml test
```

The standalone verifier compares installed source bytes to sealed POST SHA-256,
uses `git show 970a34350ebdf6a3f1c5ff5acc8445d0f3515f55:path` to recover and authenticate exact PRE,
compiles PRE and POST with `javac --release 21 -proc:none -Xlint:all -Werror`,
requires the original capacity test to fail and the candidate to pass, compares
public `javap -public -s` descriptors and repeat `.class` identities.
It never mutates the checkout and writes receipts outside the source tree.

An independent Synexia selected-source verifier has already executed these
Java contracts in an isolated exact source image: 142,438 checks and a separate
192,768-check algorithm model. That is proof of a bounded Java source unit,
**not** the full target checkout/JDK build/jtreg. The target's verifier and
Maven/OpenRewrite SDK still require execution against the GitHub-installed PR.

Cost example: for the specified 8-slot collision path, retained primitive lane
payload stays 72 bytes (Set) vs old 144, and 136 bytes (Map) vs old 272,
excluding array/object headers and transient allocation. No universal CPU,
throughput, or whole-heap savings claim is made. Before/after JMH and
GC-cost measurements remain open.
