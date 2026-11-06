<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# M3IndexDB composable chunk SimHash — target receiving receipt

**Status: CANDIDATE / limited acceptance, not M3JDK21 runtime or collection-phase promotion.**

Canonical recipe: [hsoliwal/com.synexia#16095](https://github.com/hsoliwal/com.synexia/pull/16095),
commit `353047b445451d95ce83952b48a81931fb63be51`,
named recipe `com.synexia.m3.IndexDbChunkSimHash`.
Target base: `hsoliwal/M3jdk21@172e39d5e69a8f7775c25ad1ce2bfaf2bf5d1246`.

Two additive, ABSENT-before reviewed postimages:

| Target file | SHA-256 exact bytes | Git blob |
| --- | --- | --- |
| `m3/indexdb/src/main/java/jdk/m3/indexdb/M3IndexDbChunkSimHash.java` | `b67c52fb5d88ef53197068315665e913a28a686b27aba3a1c419b9a0f4ecdead` | `2c376ce57f593b68109218164a3ed85bba12c532` |
| `m3/indexdb/src/test/java/jdk/m3/indexdb/M3IndexDbChunkSimHashTest.java` | `1a21b6d5a73a148706ff36e32ee5483234e95b41de97c258b80a0c48e1236ae1` | `b22dcabac0f2bbcdfaf654e8979020b5e02c5295` |

Reuse original `M3IndexDbSemanticFingerprint` ownership: exact SHA-256, ordered
structural/logic identities and existing API do not change. Chunk signed vote
vectors support associative combination and repetition of *feature multisets*,
not exact composition of text bytes or SHA-256 digests. Callers must bind an
explicit token/shingle/hash/weight version to the feature domain; Hamming
distance is approximate candidate discovery only.

## Executed source acceptance

Java 21 `--release 21 -proc:none -Xlint:all -Werror` compiled both new
sources and ran 600 randomized prehashed-feature array windows with 45,015
assertions. Independent signed-vector oracle, offset/weight geometry,
associative regrouping, same-sign/different-vote counterexample, overflow and
no-output-mutation input checks passed. Canonical Synexia's bounded source
adapter verified staged generation, zero-write replay and drift refusal.

Independent target build command (not the canonical recipe):
```sh
javac --release 21 -proc:none -Xlint:all -Werror -d /tmp/m3-simhash-test \
  m3/indexdb/src/main/java/jdk/m3/indexdb/M3IndexDbChunkSimHash.java \
  m3/indexdb/src/test/java/jdk/m3/indexdb/M3IndexDbChunkSimHashTest.java
java -cp /tmp/m3-simhash-test com.m3.indexdb.M3IndexDbChunkSimHashTest
```

**Unexecuted:** Maven/OpenRewrite runtime, complete IndexDB reactor/JUnit,
serialization and persistence integration, installed JDK and jtreg, Java/JNI
comparative CPU/heap/native memory. No new JNI ABI or production native code:
the existing IndexDB module has no suitable admitted SimHash native owner.
No claimed speedup or global similar-document accuracy score.

Follow M3JDK21's mandatory **String -> arrays -> collections -> AST/compiler**
receiving order. A passing candidate oracle is not a product phase advance.
All recipe implementation, source templates, algorithm catalogue and mastery
fixtures remain exclusively in Synexia; this target stores only postimages,
target-authoritative tests and this passive receipt.
