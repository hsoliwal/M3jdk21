<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 PMEM two-byte gram index — Java21/JNI recipe-first task crate

Stack this new private receiver **only** on the exact Synexia PMEMSDK source/campaign+selected-search work tree. This is not a standalone repo checkout. The public Java and original native C17/JNI source paths remain unchanged.

1. Inventory source SHA-256 in `FROZEN_INPUT.tsv` and the original Java/JNI ABI.
2. Execute canonical Maven/OpenRewrite `M3HashPinnedJavaSnapshotRecipe` on this crate (exact two `ABSENT` targets, first pass and fixed point, occupied/drift refusal), then materialize its two reviewed output postimages.
3. Run `bash verify-java21-jni.sh` and the full original `m3-pmem-sdk` JNI/JMOD tests.
4. Run real JUnit `PmemCorpusGramIndexJUnitTest` on FILE and POSIX backend, with `pmem.gram.native=true` for mandatory C17 JNI parity. Optional 20k physical-file test requires `pmem.gram.scale=true`.
5. Run external source, JNI header, licence/provenance, full historical loss and target product receiver gates before merging or delivering result-only M3JDK21 patches.

Local Java21/C17 JNI oracle under `proof/` is **only independent differential proof**; it is deliberately not an additional production JNI symbol or second PMEMSDK search implementation. No Chrome model calls and no real 1GB Optane/DAX hardware tests have been run.
