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
| `m3/indexdb/src/main/java/com/m3/indexdb/M3IndexDbChunkSimHash.java` | `2bbe2e12bb76f8fc4805d6600b964842396a1d47bee3fb7c59417193c9fe9653` | `2719c9d67be999ade54e163d79fef48fa9ab7be9` |
| `m3/indexdb/src/test/java/com/m3/indexdb/M3IndexDbChunkSimHashTest.java` | `1ea2dd235aa79af9c7196bc6d85c049e38fb008ae04062021c5f62d2331a06ae` | `0b4dcfcbb5989c7e9a606678f9bac3213bc6d98a` |

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
  m3/indexdb/src/main/java/com/m3/indexdb/M3IndexDbChunkSimHash.java \
  m3/indexdb/src/test/java/com/m3/indexdb/M3IndexDbChunkSimHashTest.java
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
