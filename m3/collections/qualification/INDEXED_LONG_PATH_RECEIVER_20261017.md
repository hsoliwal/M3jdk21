<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# M3 indexed 64-bit row paths and stage masks — receiver evidence

**DRAFT / PROMOTION HOLD.** Product receiver for [Synexia canonical PR #12096](https://github.com/hsoliwal/com.synexia/pull/12096) @ `ec972338cf047104246bc50732b6b6063b6ed80c`. Target preimage M3JDK21 `master@4e29dc9d4141261e8763fbe9ffd6a20e017b1aff`. Exact original Synexia first-party code; no external challenge submission, editorial, vendor body or upstream JDK source copied.

## Source and owner

The established `M3IndexPathMasks` Java class and `m3_index_path_masks.c` native JNI owner remain canonical. The existing `select(int[], ...)` and `firstMatchingIndex` entry points remain present; new `selectLong(long[], ...)` and `selectLongNative` accept 64-bit row IDs without truncating sign/extreme values. Physical offsets remain `int[]`; state planes/required/forbidden remain `int[][]`/`int[]` full 32-bit words. Positive/negative strides, duplicates and encounter order are preserved with no entry objects, row-owned lifetime or retained heap pointers.

The Java and text native source transformations, tests, and the installed-source verifier reconciliation are qualified by exactly three **existing** Synexia OpenRewrite hash-pinned recipe engines composed as `com.synexia.m3.Jdk21IndexedLongPathMasks20261017`. No manually patched production postimage. The four-path additive receipt is `m3/collections/recipe/index-path-long-20261017/manifest.tsv`, SHA-256 `422e7650b0781d5496c3b86261c9c50bd3bad19c4647fb12673b69e7d9babd69`. The prior source and receipt overlays remain sealed.

## Provenance and algorithm catalogue

`m3/collections/algorithm-catalog/index-path-long-20261017.tsv` is an exact companion Synexia resource mirror. LeetCode -> HackerRank -> GeeksforGeeks supply separately reviewed category evidence only (two-pointer filtering, offset arrays, bit masking); they do not override the JDK oracle, license any challenge solution, or imply the contracts match. Existing Synexia/Java 21 owners provide the selected code. Apache-2.0 attribution retained; OpenJDK and others remain under their own licenses.

## Focused verification and boundaries

Local Java21 strict compilation and GCC C17 -Werror/-Wconversion/-Wshadow/-pedantic, fatal UBSan, and Java/native -Xcheck:jni passed in isolated source probes: 3,088 Java checks, 6,162 JNI checks over 3,072 seeded long-row families, plus 2,048 original int-path backend parity trials. This is not evidence of complete Maven/OpenRewrite/JUnit application on the actual target tree, full reactor, source-built OpenJDK, jtreg/JCK, installed-JDK packaging, non-Linux JNI, throughput improvement or legal sublicensing of unrelated third-party code.

Focused hosted workflow recompiles the real target files, verifies the bounded Python receipt tests and runs old/new Java/C JNI parity. Check its **head SHA and conclusion** before asserting CI PASS. Promotion remains at the active STRING receiving phase; no Java/HotSpot `java.util` runtime replacement is claimed.

Run locally:
```sh
python3 -m unittest discover -s m3/collections -p 'test_verify_index_path_long.py' -v
mvn -B -f m3/collections/pom.xml -Dtest=M3IndexPathMasksLongTest test
```
