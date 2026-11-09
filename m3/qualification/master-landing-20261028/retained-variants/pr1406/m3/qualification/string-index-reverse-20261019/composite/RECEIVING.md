# M3JDK21 canonical String + strict GVAS convergence (2026-10-19)

**Product**: existing `hsoliwal/M3JDK21` Java21 / HotSpot / JNI / native
runtime. This is NOT an independent M3 layer or adapter. Synexia remains
the **build-time convergence workspace** and source/recipe owner.

The existing fully-qualified **[Synexia #12403](https://github.com/hsoliwal/com.synexia/pull/12403)**
(immutable commit `1124f5b72744b306884beedebafd8558d49d4c4c`)
already owns BOTH separate exact-source Maven/OpenRewrite recipes:
`com.synexia.m3.StringPoolIndex20261018` and
`com.synexia.m3.GvasLookupIndex20261018`.
No new Java/C implementation, manifest engine or recipe class is required.
The source-first artifact here is a **machine-readable cross-recipe composition
record**, not a second rewrite recipe or duplicate runtime object.

The target product branch is created **from [M3JDK21 #1406](https://github.com/hsoliwal/M3jdk21/pull/1406)**
so its canonical `java.lang.M3StringPool` retained owner/generation/packed
coordinate, optional hash index, precomputed reverse coder metadata, bucket
retirement, Java21 jtreg probes, and checked references are preserved exactly.
Do not replace it with the earlier #1390 implementation.

[Target donor M3JDK21 #1407](https://github.com/hsoliwal/M3jdk21/pull/1407)
has **three modified GVAS targets** and **nine additive tests/receipts**.
The three exact product preimage Git blobs have been checked on #1406 and
are identical to #1407's original source (see INPUT_BLOBS.tsv).
All new target paths were checked absent. Product receiving must apply
whole canonical #1407 postimages serially with the exact Git blob check.

The original fixed-address `M3GVAS01` image, UTF-16 exact spelling equality,
row-number and direct reverse address arithmetic remain. Optional hash
precompute never owns text and may refuse without refusing a valid image.
Actual same-address JNI mapping must prove kernel PID identity for each
cooperating process, reject colliding address claims, and retain inode,
permissions and hash proofs.

**Priority-0 qualification boundary:** Do not mistake receipt equality or
an earlier standalone algorithm test for full combined M3JDK21+HotSpot
correctness. The #1407 strict GVAS native CI can rerun on this exact
product stacked branch. The parent #1406 String workflow had historical
source-chain failures that require their own recipe-first successor
repair and matched-JDK jtreg before promotion. No full target merge,
full release, or byte-perfect cross-repo semantic closure is asserted.

Donors: original Synexia code, first-party M3JDK21 #1406/#1407 and the
category-indexed algorithm discussion. LeetCode/HackerRank/GFG are
problem-category references only. Preserve OpenJDK GPLv2 Classpath rights
and Apache-2.0 first-party target rights per file.
