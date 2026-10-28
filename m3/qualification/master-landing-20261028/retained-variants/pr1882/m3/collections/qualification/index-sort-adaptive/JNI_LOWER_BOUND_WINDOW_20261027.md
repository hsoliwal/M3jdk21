<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 original signed-long lower-bound JNI output-window receiving

Canonical Synexia convergence recipe: hsoliwal/com.synexia PR #13224, source commit c04caf97188b515f0cbac8dcd43532c2862e6216, Java recipe com.synexia.m3.Jdk21LongLowerBoundWindow20261027 and C text recipe com.synexia.m3.Jdk21LongLowerBoundWindowNative20261027. Product owner is hsoliwal/M3jdk21; this is NOT an M3 overlay.

The original M3LongRankBatchNative.java, native m3_long_rank_batch_jni.c and m3rankbatch library remain authoritative. The Java result-window method adds direct primitive output offsets and pre-write validation, reusing the old mode-based boundBatchInto0 JNI export with lower mode 0. Existing upper mode 1 and count mode 2 are retained. No JNI library, callback API, Java public surface, object/payload copy or native pointer escapes.

Java before blob 022cefecdee793a8e6ea91b4a4e9496480ca14ac, after 52f21dbe93b97748fb53f5d5cb7d857d4b04d1cf. C before 1db4c8dbaf28b1aea4315e9c03a2d5ce0c4c7543, after 107d5f86c901042c7fe23c926495aadb568798ca. Standalone test faf32537f1392a1162b610d2382b20bac0217425; JUnit wrapper e768bd5cb92a65973e32b8daf333ab023b4526a4. Validation workflow 38328bb61f92516a8893bc215671bda4d2701996. All source Git blobs received byte-for-byte from Synexia, which retains the algorithms/recipes and candidate comparison catalogue.

Algorithm categories: LeetCode #35 lower-bound insertion and #34 equal boundaries; #88 monotone two-cursor scanning. No challenge code copied. Complexity O(q log n) unsorted, optional cost-admitted O(n + q) monotone query batches, fixed native 256-slot pages. JNI is optional and tests must prove native=true; Java fallback remains authoritative.

Status: SOURCE CANDIDATE / DRAFT / HOLD. Full JNI native parity, module Maven/JUnit reactor, Java installed JDK, jtreg/JCK and performance checks require executed current-head proof. No successful CI asserted here.
